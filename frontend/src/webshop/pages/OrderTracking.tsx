import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { errorMessage } from "../../lib/http";
import { subscribeWithTicket } from "../../lib/sse";
import { money, paymentLabel, STATUS_LABEL, time } from "../../lib/format";
import type { CustomerOrder } from "../../lib/types";
import { Badge, ErrorBanner, LiveDot, Loading } from "../../components/ui";
import { IconClock, IconHome, IconTruck } from "../../components/icons";
import { orderStreamUrl } from "../api";
import { RequireLogin, useShop } from "../WebshopApp";

const HEADLINE: Record<string, string> = {
  PLACED: "Your order was received!",
  ACCEPTED: "The restaurant accepted your order",
  PREPARING: "Your food is being prepared",
  READY_FOR_PICKUP: "Your order is ready — come pick it up!",
  OUT_FOR_DELIVERY: "Your order is on the way",
  PICKED_UP: "Enjoy your meal!",
  DELIVERED: "Delivered. Enjoy your meal!",
  REJECTED: "Sorry, the restaurant could not take your order",
  CANCELLED: "Your order was cancelled",
};

const STATUS_HINT: Record<string, string> = {
  PLACED: "We’re waiting for the kitchen to confirm.",
  ACCEPTED: "Great news — the kitchen has your order.",
  PREPARING: "Chefs are cooking right now.",
  READY_FOR_PICKUP: "Head over when you’re ready.",
  OUT_FOR_DELIVERY: "A courier is bringing it to you.",
  PICKED_UP: "Thanks for ordering with TasteHouse.",
  DELIVERED: "Thanks for ordering with TasteHouse.",
  REJECTED: "You weren’t charged for this order.",
  CANCELLED: "This order is no longer active.",
};

export function OrderTracking() {
  return (
    <RequireLogin message="Log in to follow your order.">
      <Tracking />
    </RequireLogin>
  );
}

/** "Your order was received" page with a timeline that refreshes itself over SSE. */
function Tracking() {
  const { orderId = "" } = useParams();
  const { api } = useShop();
  const location = useLocation();
  const navState = (location.state ?? {}) as { justPlaced?: boolean; pricesChanged?: boolean };
  const [order, setOrder] = useState<CustomerOrder | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    api.order(orderId).then(setOrder).catch((e) => setError(errorMessage(e)));
  }, [api, orderId]);

  const finished = order?.finished ?? false;
  useEffect(() => {
    if (finished) return;
    return subscribeWithTicket({
      issueTicket: async () => (await api.streamTicket(orderId)).ticket,
      streamUrl: (ticket) => orderStreamUrl(orderId, ticket),
      onStatus: setConnected,
      events: { "order-updated": (data) => setOrder(data as CustomerOrder) },
    });
  }, [api, orderId, finished]);

  if (error) return <ErrorBanner message={error} />;
  if (!order) return <Loading variant="page" label="Loading order…" hint="Getting the latest status" />;

  const derailed = order.status === "REJECTED" || order.status === "CANCELLED";
  const success = order.status === "DELIVERED" || order.status === "PICKED_UP";
  const shortId = order.id.slice(0, 8);

  return (
    <section className="tracking tracking-page">
      <nav className="restaurant-crumb small">
        <Link to="/webshop">Home</Link>
        <span aria-hidden>/</span>
        <Link to="/webshop/orders">My orders</Link>
        <span aria-hidden>/</span>
        <span>#{shortId}</span>
      </nav>

      {navState.justPlaced && !derailed && (
        <div className="banner banner-ok">Order placed successfully. We’ll keep this page updated live.</div>
      )}
      {navState.pricesChanged && (
        <div className="banner banner-warn">
          Some prices changed since you added the items. Your order uses the current menu prices.
        </div>
      )}

      <div className={`tracking-hero card ${derailed ? "derailed" : ""} ${success ? "success" : ""}`}>
        <div className="tracking-hero-copy">
          <div className="tracking-kicker">
            <Badge tone={derailed ? "red" : success ? "green" : "blue"}>
              {STATUS_LABEL[order.status] ?? order.status}
            </Badge>
            {!order.finished && <LiveDot connected={connected} />}
          </div>
          <h1>{HEADLINE[order.status] ?? STATUS_LABEL[order.status]}</h1>
          <p className="tracking-sub">
            {STATUS_HINT[order.status] ?? "Follow every step as the kitchen works on your order."}
          </p>
          <div className="restaurant-chips">
            <span className="info-chip">{order.restaurantName}</span>
            <span className="info-chip">#{shortId}</span>
            <span className="info-chip">
              <IconClock width={16} height={16} /> {time(order.placedAt)}
            </span>
            <span className="info-chip">
              {order.fulfillmentType === "DELIVERY" ? (
                <>
                  <IconTruck width={16} height={16} /> Delivery
                </>
              ) : (
                "Pickup"
              )}
            </span>
          </div>
          {!order.finished && order.estimatedReadyAt && (
            <p className="tracking-eta">
              Estimated {order.fulfillmentType === "DELIVERY" ? "ready for delivery" : "ready for pickup"} around{" "}
              <strong>{time(order.estimatedReadyAt)}</strong>
            </p>
          )}
        </div>
        <div className={`tracking-hero-art ${derailed ? "art-bad" : success ? "art-good" : "art-live"}`} aria-hidden>
          <span>{derailed ? "!" : success ? "✓" : "◉"}</span>
        </div>
      </div>

      <div className="two-col tracking-grid">
        <div className="card tracking-status-card">
          <div className="tracking-card-head">
            <h3>Status timeline</h3>
            <p className="muted small">Updates arrive live from the kitchen.</p>
          </div>
          <ol className="timeline">
            {order.timeline.map((step) => (
              <li
                key={step.status}
                className={[
                  "timeline-step",
                  step.reached ? "reached" : "",
                  step.current ? "current" : "",
                  step.status === "REJECTED" || step.status === "CANCELLED" ? "failed" : "",
                ].join(" ")}
              >
                <span className="timeline-dot" />
                <div className="timeline-copy">
                  <strong>{STATUS_LABEL[step.status] ?? step.status}</strong>
                  <div className="muted small">
                    {step.at ? time(step.at) : step.current ? "In progress now" : "Pending"}
                  </div>
                </div>
              </li>
            ))}
          </ol>
        </div>

        <div className="card tracking-details-card">
          <div className="tracking-card-head">
            <h3>Order details</h3>
            <p className="muted small">What you ordered and how you’ll pay.</p>
          </div>

          <ul className="lines tracking-lines">
            {order.lines.map((l, i) => (
              <li key={i}>
                <span>
                  <strong>{l.quantity}×</strong> {l.name}
                </span>
                <span>{money(l.total)}</span>
              </li>
            ))}
          </ul>

          <div className="tracking-totals">
            <div className="row space-between">
              <span>Subtotal</span>
              <span>{money(order.subtotal)}</span>
            </div>
            <div className="row space-between">
              <span>Delivery fee</span>
              <span>{money(order.deliveryFee)}</span>
            </div>
            <div className="row space-between total-row">
              <span>Total</span>
              <strong>{money(order.total)}</strong>
            </div>
          </div>

          <div className="tracking-meta">
            <div className="tracking-meta-row">
              <span className="muted small">Fulfillment</span>
              <strong>{order.fulfillmentType === "DELIVERY" ? "Delivery" : "Pickup"}</strong>
            </div>
            <div className="tracking-meta-row">
              <span className="muted small">Payment</span>
              <span className="row gap-sm">
                <strong>{paymentLabel(order.paymentMethod, order.paymentTiming, order.fulfillmentType)}</strong>
                <Badge tone={order.paymentStatus === "PAID" ? "green" : order.paymentStatus === "REFUNDED" ? "blue" : "amber"}>
                  {order.paymentStatus === "PAID" ? "Paid" : order.paymentStatus === "REFUNDED" ? "Refunded" : "Pending"}
                </Badge>
              </span>
            </div>
            {order.deliveryAddress && (
              <div className="tracking-meta-row">
                <span className="muted small">Deliver to</span>
                <strong>
                  {order.deliveryAddress.street}, {order.deliveryAddress.city}
                </strong>
              </div>
            )}
            {order.notes && (
              <div className="tracking-meta-row">
                <span className="muted small">Notes</span>
                <strong>{order.notes}</strong>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="tracking-actions">
        <Link to="/webshop/orders" className="btn">
          ← All my orders
        </Link>
        <Link to="/webshop" className="btn btn-primary">
          <IconHome width={16} height={16} /> Order something else
        </Link>
      </div>
    </section>
  );
}
