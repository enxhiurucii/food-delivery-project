import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { errorMessage } from "../../lib/http";
import { money } from "../../lib/format";
import type { Menu, ShopRestaurant } from "../../lib/types";
import { Badge, ErrorBanner, Loading, Spinner } from "../../components/ui";
import { IconClock, IconPlus, IconStar, IconTruck } from "../../components/icons";
import { useShop } from "../WebshopApp";

export function RestaurantPage() {
  const { restaurantId = "" } = useParams();
  const { api, cart, addToCart, isLoggedIn } = useShop();
  const [restaurant, setRestaurant] = useState<ShopRestaurant | null>(null);
  const [menus, setMenus] = useState<Menu[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState<string | null>(null);
  const [activeMenu, setActiveMenu] = useState<string | null>(null);

  useEffect(() => {
    api.restaurant(restaurantId).then(setRestaurant).catch((e) => setError(errorMessage(e)));
    api.menus(restaurantId).then(setMenus).catch((e) => setError(errorMessage(e)));
  }, [api, restaurantId]);

  useEffect(() => {
    if (menus && menus.length > 0 && !activeMenu) setActiveMenu(menus[0].id);
  }, [menus, activeMenu]);

  const add = async (itemId: string) => {
    setAdding(itemId);
    await addToCart(restaurantId, itemId);
    setAdding(null);
  };

  const quantityInCart = (itemId: string) =>
    cart?.restaurantId === restaurantId ? (cart.items.find((i) => i.menuItemId === itemId)?.quantity ?? 0) : 0;
  const cartHere = cart?.restaurantId === restaurantId && cart.items.length > 0;
  const cartCount = cartHere ? cart!.items.reduce((sum, i) => sum + i.quantity, 0) : 0;

  const visibleMenus = useMemo(() => {
    if (!menus) return [];
    if (!activeMenu || activeMenu === "all") return menus;
    return menus.filter((m) => m.id === activeMenu);
  }, [menus, activeMenu]);

  if (error) return <ErrorBanner message={error} />;
  if (!restaurant || !menus) {
    return <Loading variant="page" label="Loading menu…" hint="Fetching dishes from the kitchen" />;
  }

  return (
    <section className="restaurant-page with-sidebar">
      <div className="restaurant-main">
        <nav className="restaurant-crumb small">
          <Link to="/webshop">Restaurants</Link>
          <span aria-hidden>/</span>
          <span>{restaurant.name}</span>
        </nav>

        <div className="restaurant-hero">
          <div className="restaurant-hero-media">
            {restaurant.imageUrl ? (
              <img src={restaurant.imageUrl} alt="" />
            ) : (
              <div className="img-placeholder tall restaurant-hero-fallback">🍽️</div>
            )}
            <div className="restaurant-hero-fade" />
          </div>
          <div className="restaurant-hero-panel">
            <div className="restaurant-hero-top">
              <Badge tone={restaurant.open ? "green" : "red"}>{restaurant.open ? "Open now" : "Closed"}</Badge>
              <span className="rating">
                <IconStar /> 4.8
              </span>
            </div>
            <h1>{restaurant.name}</h1>
            {restaurant.description && <p className="restaurant-desc">{restaurant.description}</p>}
            <div className="restaurant-chips">
              <span className="info-chip">
                <IconClock width={16} height={16} /> {restaurant.estimatedPrepMinutes} min
              </span>
              {restaurant.deliveryAvailable && (
                <span className="info-chip">
                  <IconTruck width={16} height={16} /> Delivery {money(restaurant.deliveryFee)}
                </span>
              )}
              {restaurant.pickupAvailable && <span className="info-chip">Pickup available</span>}
              <span className="info-chip">Min. {money(restaurant.minimumOrder)}</span>
            </div>
            <p className="muted small restaurant-address">{restaurant.address}</p>
            {!restaurant.open && (
              <div className="restaurant-closed-note">
                <Badge tone="red">Not accepting orders right now</Badge>
              </div>
            )}
          </div>
        </div>

        {menus.length > 1 && (
          <div className="menu-tabs" role="tablist" aria-label="Menu sections">
            <button
              type="button"
              className={`menu-tab${activeMenu === "all" ? " active" : ""}`}
              onClick={() => setActiveMenu("all")}
            >
              All
            </button>
            {menus.map((menu) => (
              <button
                key={menu.id}
                type="button"
                className={`menu-tab${activeMenu === menu.id ? " active" : ""}`}
                onClick={() => setActiveMenu(menu.id)}
              >
                {menu.name}
                <span className="menu-tab-count">{menu.items.length}</span>
              </button>
            ))}
          </div>
        )}

        {visibleMenus.map((menu) => (
          <div key={menu.id} className="menu-section" id={`menu-${menu.id}`}>
            <div className="section-head">
              <div>
                <h2>{menu.name}</h2>
                {menu.description ? <p>{menu.description}</p> : <p>Fresh picks from this section.</p>}
              </div>
            </div>

            {menu.items.length === 0 ? (
              <div className="empty">No dishes in this section yet.</div>
            ) : (
              <div className="dish-grid">
                {menu.items.map((item, index) => {
                  const qty = quantityInCart(item.id);
                  const busy = adding === item.id;
                  return (
                    <article key={item.id} className={`dish-card ${item.available ? "" : "dish-unavailable"}`}>
                      <div className="dish-card-media">
                        {index === 0 && item.available && (
                          <span className="product-badge">
                            <Badge tone="green">Bestseller</Badge>
                          </span>
                        )}
                        {!item.available && (
                          <span className="product-badge">
                            <Badge tone="red">Sold out</Badge>
                          </span>
                        )}
                        {item.imageUrl ? <img src={item.imageUrl} alt="" /> : <div className="img-placeholder tall">🍴</div>}
                      </div>
                      <div className="dish-card-body">
                        <div className="dish-card-copy">
                          <h3>{item.name}</h3>
                          {item.description ? (
                            <p className="muted small">{item.description}</p>
                          ) : (
                            <p className="muted small">Chef’s specialty.</p>
                          )}
                        </div>
                        <div className="dish-card-foot">
                          <div>
                            <span className="price">{money(item.price)}</span>
                            <span className="rating">
                              <IconStar /> 4.{8 - (index % 3)}
                            </span>
                          </div>
                          {item.available ? (
                            <button
                              className={`btn-add${qty > 0 ? " btn-add-count" : ""}`}
                              disabled={busy || !restaurant.open}
                              onClick={() => void add(item.id)}
                              title={
                                isLoggedIn
                                  ? qty > 0
                                    ? `${qty} in cart · add more`
                                    : "Add to cart"
                                  : "Log in to order"
                              }
                              aria-label="Add to cart"
                            >
                              {busy ? <Spinner size={16} /> : qty > 0 ? <span>{qty}</span> : <IconPlus />}
                            </button>
                          ) : (
                            <Badge tone="red">Sold out</Badge>
                          )}
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </div>
        ))}
      </div>

      <aside className={`sidebar card cart-panel${cartHere ? " cart-panel-active" : ""}`}>
        <div className="cart-panel-head">
          <div>
            <h3>Your order</h3>
            <p className="muted small" style={{ margin: 0 }}>
              {cartHere ? `${cartCount} item${cartCount === 1 ? "" : "s"}` : "Ready when you are"}
            </p>
          </div>
          {cartHere && <Badge tone="green">Live</Badge>}
        </div>

        {!cartHere ? (
          <div className="cart-empty">
            <div className="cart-empty-icon" aria-hidden>
              🛒
            </div>
            <p className="muted small">
              {isLoggedIn ? "Add dishes to start an order." : "Log in to start an order."}
            </p>
          </div>
        ) : (
          <>
            <ul className="lines cart-lines">
              {cart!.items.map((i) => (
                <li key={i.menuItemId}>
                  <span>
                    <strong>{i.quantity}×</strong> {i.name}
                  </span>
                  <span>{money(i.total)}</span>
                </li>
              ))}
            </ul>
            <div className="row space-between total-row">
              <span>Subtotal</span>
              <strong>{money(cart!.subtotal)}</strong>
            </div>
            {restaurant.minimumOrder > cart!.subtotal && (
              <p className="small warn">Add {money(restaurant.minimumOrder - cart!.subtotal)} more to reach the minimum.</p>
            )}
            <Link to="/webshop/checkout" className="btn btn-primary btn-block btn-lg">
              Go to checkout
            </Link>
          </>
        )}
      </aside>
    </section>
  );
}
