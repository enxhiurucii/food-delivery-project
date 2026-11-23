import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { DriverOrder } from "../../lib/types";
import { money, minutesAgo, time } from "../../lib/format";
import { errorMessage } from "../../lib/http";
import { ErrorBanner, Loading } from "../../components/ui";
import { IconClock, IconTruck } from "../../components/icons";
import { useDriver } from "../DriverApp";

export function AvailableDeliveries() {
  const { api } = useDriver();
  const navigate = useNavigate();
  const [orders, setOrders] = useState<DriverOrder[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [claiming, setClaiming] = useState<string | null>(null);
  const [tick, setTick] = useState(15);

  const load = () => api.available().then(setOrders).catch((e) => setError(errorMessage(e)));

  useEffect(() => {
    void load();
    const dataInterval = setInterval(() => { void load(); setTick(15); }, 15000);
    const tickInterval = setInterval(() => setTick((t) => Math.max(0, t - 1)), 1000);
    return () => { clearInterval(dataInterval); clearInterval(tickInterval); };
  }, [api]);

  const claim = async (orderId: string) => {
    setClaiming(orderId);
    try {
      await api.claim(orderId);
      navigate("/driver/active");
    } catch (e) {
      setError(errorMessage(e));
      void load();
    } finally {
      setClaiming(null);
    }
  };

  const count = orders?.length ?? 0;

  return (
    <section className="driver-available">
      <div className="driver-available-head">
        <div className="driver-available-title">
          <h1>Available deliveries</h1>
          <p className="muted">
            {orders
              ? count === 0
                ? "No open jobs right now — this list refreshes automatically"
                : `${count} open job${count !== 1 ? "s" : ""} · auto-refresh in ${tick}s`
              : "Loading open jobs…"}
          </p>
        </div>
        <div className="driver-available-actions">
          {orders && (
            <div className="driver-live-pill" aria-live="polite">
              <span className="driver-live-dot" />
              Live · {tick}s
            </div>
          )}
          <button
            type="button"
            className="btn btn-sm"
            onClick={() => { void load(); setTick(15); }}
          >
            Refresh
          </button>
        </div>
      </div>

      <ErrorBanner message={error} onClose={() => setError(null)} />

      {!orders ? (
        <Loading variant="page" label="Loading available deliveries…" />
      ) : orders.length === 0 ? (
        <div className="delivery-empty">
          <div className="delivery-empty-art" aria-hidden>
            <IconTruck width={36} height={36} />
          </div>
          <h2>No deliveries right now</h2>
          <p className="muted">New orders will appear here automatically.</p>
        </div>
      ) : (
        <div className="delivery-grid">
          {orders.map((order) => {
            const isClaiming = claiming === order.id;
            const paymentPaid = order.paymentStatus === "PAID";
            const paymentCash = order.paymentMethod === "CASH";
            const itemCount = order.lines.reduce((sum, line) => sum + line.quantity, 0);

            return (
              <article key={order.id} className="delivery-card">
                <header className="delivery-card-top">
                  <div className="delivery-card-id">
                    <span className="delivery-card-hash">#{order.id.slice(0, 8)}</span>
                    <span className="delivery-card-age">
                      <IconClock width={14} height={14} />
                      {minutesAgo(order.placedAt)}
                    </span>
                  </div>
                  <span className="delivery-total">{money(order.total)}</span>
                </header>

                <div className="delivery-card-route">
                  <div className="delivery-route-point">
                    <div className="delivery-route-dot pickup" />
                    <div className="delivery-route-copy">
                      <div className="delivery-route-label">Pick up</div>
                      <div className="delivery-route-name">{order.restaurantName}</div>
                      <div className="delivery-route-address">{order.restaurantAddress}</div>
                    </div>
                  </div>
                  <div className="delivery-route-line" aria-hidden />
                  <div className="delivery-route-point">
                    <div className="delivery-route-dot dropoff" />
                    <div className="delivery-route-copy">
                      <div className="delivery-route-label">Drop off</div>
                      <div className="delivery-route-name">{order.customerName}</div>
                      <div className="delivery-route-address">
                        {order.deliveryAddress.street}, {order.deliveryAddress.city}
                        {order.deliveryAddress.instructions && (
                          <span className="muted"> · {order.deliveryAddress.instructions}</span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>

                <div className="delivery-card-items">
                  <span className="delivery-items-count">
                    {itemCount} item{itemCount !== 1 ? "s" : ""}
                  </span>
                  {order.lines.map((line, i) => (
                    <span key={i} className="delivery-item-pill">
                      {line.quantity}× {line.name}
                    </span>
                  ))}
                  {order.notes && (
                    <span className="delivery-item-pill delivery-item-note">{order.notes}</span>
                  )}
                </div>

                <footer className="delivery-card-footer">
                  <div className="delivery-card-meta">
                    <span
                      className={`delivery-payment-badge ${
                        paymentPaid ? "paid" : paymentCash ? "cash" : "card"
                      }`}
                    >
                      {paymentPaid ? "Paid online" : paymentCash ? "Cash on delivery" : "Card on delivery"}
                    </span>
                    <span className="muted small">{time(order.placedAt)}</span>
                  </div>
                  <button
                    type="button"
                    className="btn btn-primary delivery-claim-btn"
                    disabled={isClaiming || claiming !== null}
                    onClick={() => void claim(order.id)}
                  >
                    {isClaiming ? (
                      <>
                        <span className="spinner-xs" /> Claiming…
                      </>
                    ) : (
                      <>
                        <IconTruck width={16} height={16} /> Claim
                      </>
                    )}
                  </button>
                </footer>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
