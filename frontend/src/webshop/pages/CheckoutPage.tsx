import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { errorMessage } from "../../lib/http";
import { money } from "../../lib/format";
import type { Address, ShopRestaurant } from "../../lib/types";
import { Empty, ErrorBanner, Loading, Spinner } from "../../components/ui";
import type { CheckoutRequest } from "../api";
import { RequireLogin, useShop } from "../WebshopApp";

type Fulfillment = CheckoutRequest["fulfillmentType"];
interface PaymentOption {
  id: string;
  label: string;
  hint: string;
  method: CheckoutRequest["paymentMethod"];
  timing: CheckoutRequest["paymentTiming"];
}

/** Payment options per fulfillment type (mirrors the webshop's PaymentPolicy). */
const PAYMENT_OPTIONS: Record<Fulfillment, PaymentOption[]> = {
  PICKUP: [
    { id: "card-online", label: "Pay now by card", hint: "Charged at checkout", method: "CARD", timing: "ONLINE" },
    { id: "cash-restaurant", label: "Pay at the restaurant · cash", hint: "Pay when you pick up", method: "CASH", timing: "ON_COMPLETION" },
    { id: "card-restaurant", label: "Pay at the restaurant · card", hint: "Pay when you pick up", method: "CARD", timing: "ON_COMPLETION" },
  ],
  DELIVERY: [
    { id: "card-online", label: "Pay now by card", hint: "Charged at checkout", method: "CARD", timing: "ONLINE" },
    { id: "cash-delivery", label: "Cash on delivery", hint: "Pay the courier", method: "CASH", timing: "ON_COMPLETION" },
  ],
};

const EMPTY_ADDRESS: Address = { street: "", city: "Tirana", postalCode: "", phone: "", instructions: "" };

export function CheckoutPage() {
  return (
    <RequireLogin message="Log in to see your cart and order.">
      <Checkout />
    </RequireLogin>
  );
}

function Checkout() {
  const { api, cart, setCart, refreshCart } = useShop();
  const navigate = useNavigate();
  const [restaurant, setRestaurant] = useState<ShopRestaurant | null>(null);
  const [fulfillment, setFulfillment] = useState<Fulfillment>("PICKUP");
  const [paymentId, setPaymentId] = useState("card-online");
  const [address, setAddress] = useState<Address>(EMPTY_ADDRESS);
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [placing, setPlacing] = useState(false);

  useEffect(() => {
    void refreshCart().catch((e) => setError(errorMessage(e)));
  }, [refreshCart]);

  useEffect(() => {
    if (!cart?.restaurantId) return;
    api.restaurant(cart.restaurantId).then((r) => {
      setRestaurant(r);
      if (!r.pickupAvailable && r.deliveryAvailable) setFulfillment("DELIVERY");
    }).catch((e) => setError(errorMessage(e)));
  }, [api, cart?.restaurantId]);

  const options = PAYMENT_OPTIONS[fulfillment];
  const payment = options.find((o) => o.id === paymentId) ?? options[0];
  const deliveryFee = fulfillment === "DELIVERY" ? (restaurant?.deliveryFee ?? 0) : 0;
  const total = (cart?.subtotal ?? 0) + deliveryFee;
  const belowMinimum = restaurant ? (cart?.subtotal ?? 0) < restaurant.minimumOrder : false;

  const changeQuantity = async (menuItemId: string, quantity: number) => {
    try {
      setCart(await api.changeQuantity(menuItemId, quantity));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const place = async (e: FormEvent) => {
    e.preventDefault();
    setPlacing(true);
    setError(null);
    try {
      const { order, pricesChanged } = await api.checkout({
        fulfillmentType: fulfillment,
        deliveryAddress: fulfillment === "DELIVERY" ? { ...address, postalCode: address.postalCode || null, instructions: address.instructions || null } : null,
        paymentMethod: payment.method,
        paymentTiming: payment.timing,
        notes: notes.trim() || null,
      });
      setCart(null);
      navigate(`/webshop/orders/${order.id}`, { state: { justPlaced: true, pricesChanged } });
    } catch (err) {
      setError(errorMessage(err));
      setPlacing(false);
    }
  };

  const fulfillmentChoices = useMemo(
    () => [
      { id: "PICKUP" as const, label: "🛍️ Pickup", enabled: restaurant?.pickupAvailable ?? true },
      { id: "DELIVERY" as const, label: `🛵 Delivery (${money(restaurant?.deliveryFee ?? 0)})`, enabled: restaurant?.deliveryAvailable ?? true },
    ],
    [restaurant],
  );

  if (!cart) return <Loading variant="page" label="Loading your cart…" hint="Syncing items with the server" />;
  if (cart.items.length === 0) {
    return (
      <Empty>
        Your cart is empty. <Link to="/webshop">Browse restaurants →</Link>
      </Empty>
    );
  }

  return (
    <form className="checkout" onSubmit={place}>
      <div className="checkout-main">
        <div className="page-head">
          <div>
            <h1>Checkout</h1>
            {restaurant && <p className="muted" style={{ margin: 0 }}>{restaurant.name}</p>}
          </div>
        </div>
        <ErrorBanner message={error} onClose={() => setError(null)} />

        <div className="card">
          <h3>1. Your items</h3>
          <ul className="lines editable">
            {cart.items.map((item) => (
              <li key={item.menuItemId}>
                <span>{item.name}</span>
                <span className="stepper">
                  <button type="button" onClick={() => void changeQuantity(item.menuItemId, item.quantity - 1)}>−</button>
                  <span>{item.quantity}</span>
                  <button type="button" onClick={() => void changeQuantity(item.menuItemId, item.quantity + 1)}>+</button>
                </span>
                <span>{money(item.total)}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="card">
          <h3>2. Pickup or delivery?</h3>
          <div className="choice-row">
            {fulfillmentChoices.map((c) => (
              <label key={c.id} className={`choice ${fulfillment === c.id ? "selected" : ""} ${c.enabled ? "" : "disabled"}`}>
                <input
                  type="radio"
                  name="fulfillment"
                  disabled={!c.enabled}
                  checked={fulfillment === c.id}
                  onChange={() => { setFulfillment(c.id); setPaymentId("card-online"); }}
                />
                {c.label}
              </label>
            ))}
          </div>
          {fulfillment === "DELIVERY" && (
            <div className="form-grid">
              <label className="span-2">Street and number<input required value={address.street} onChange={(e) => setAddress({ ...address, street: e.target.value })} /></label>
              <label>City<input required value={address.city} onChange={(e) => setAddress({ ...address, city: e.target.value })} /></label>
              <label>Postal code<input value={address.postalCode ?? ""} onChange={(e) => setAddress({ ...address, postalCode: e.target.value })} /></label>
              <label>Phone<input required type="tel" value={address.phone} onChange={(e) => setAddress({ ...address, phone: e.target.value })} /></label>
              <label>Instructions for the courier<input value={address.instructions ?? ""} onChange={(e) => setAddress({ ...address, instructions: e.target.value })} /></label>
            </div>
          )}
        </div>

        <div className="card">
          <h3>3. Payment</h3>
          <div className="choice-col">
            {options.map((o) => (
              <label key={o.id} className={`choice ${payment.id === o.id ? "selected" : ""}`}>
                <input type="radio" name="payment" checked={payment.id === o.id} onChange={() => setPaymentId(o.id)} />
                <span>
                  <strong>{o.label}</strong> <span className="muted small">· {o.hint}</span>
                </span>
              </label>
            ))}
          </div>
          {payment.timing === "ONLINE" && <p className="muted small">Demo: card payments are simulated and always approved.</p>}
          <label className="full">
            Notes for the restaurant
            <input maxLength={500} placeholder="e.g. no onions" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </label>
        </div>
      </div>

      <aside className="sidebar card">
        <h3>Summary</h3>
        <div className="row space-between"><span>Subtotal</span><span>{money(cart.subtotal)}</span></div>
        <div className="row space-between"><span>Delivery fee</span><span>{money(deliveryFee)}</span></div>
        <div className="row space-between total-row"><span>Total</span><strong>{money(total)}</strong></div>
        {belowMinimum && restaurant && <p className="small warn">Minimum order is {money(restaurant.minimumOrder)}.</p>}
        {restaurant && !restaurant.open && <p className="small warn">This restaurant is not accepting orders right now.</p>}
        <button className="btn btn-primary btn-block btn-lg" disabled={placing || belowMinimum || (restaurant ? !restaurant.open : false)}>
          {placing ? (
            <>
              <Spinner size={18} /> Placing order…
            </>
          ) : (
            `Place order · ${money(total)}`
          )}
        </button>
        <p className="muted small">Prices are confirmed by the restaurant when you place the order.</p>
      </aside>
    </form>
  );
}
