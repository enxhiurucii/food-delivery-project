import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { DriverOrder } from "../../lib/types";
import { money, minutesAgo, time, STATUS_LABEL } from "../../lib/format";
import { errorMessage } from "../../lib/http";
import { Badge, ErrorBanner, Loading } from "../../components/ui";
import { useDriver } from "../DriverApp";

const DRIVER_ACTION_LABEL: Record<string, string> = {
  OUT_FOR_DELIVERY: "Picked up from restaurant",
  DELIVERED: "Mark as delivered",
};

export function ActiveDelivery() {
  const { api } = useDriver();
  const [order, setOrder] = useState<DriverOrder | null | undefined>(undefined); // undefined = loading
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = () =>
    api.active().then((o) => setOrder(o ?? null)).catch((e) => setError(errorMessage(e)));

  useEffect(() => {
    void load();
    const interval = setInterval(() => void load(), 10000);
    return () => clearInterval(interval);
  }, [api]);

  const updateStatus = async (status: string) => {
    if (!order) return;
    setBusy(true);
    try {
      const updated = await api.updateStatus(order.id, status);
      setOrder(updated);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  if (order === undefined) {
    return <Loading variant="page" label="Loading your active delivery…" />;
  }

  if (order === null) {
    return (
      <section>
        <div className="page-head"><h1>Active delivery</h1></div>
        <div className="center" style={{ padding: "3rem 1rem", textAlign: "center" }}>
          <p className="muted">You have no active delivery.</p>
          <Link to="/driver/available" className="btn btn-primary">Browse available deliveries</Link>
        </div>
      </section>
    );
  }

  const isInTransit = order.status === "OUT_FOR_DELIVERY";
  const isDone = order.status === "DELIVERED";

  return (
    <section>
      <div className="page-head">
        <h1>Active delivery</h1>
        <Badge tone={isInTransit ? "blue" : isDone ? "green" : "amber"}>{STATUS_LABEL[order.status] ?? order.status}</Badge>
      </div>
      <ErrorBanner message={error} onClose={() => setError(null)} />

      <article className="order-card" style={{ maxWidth: 520 }}>
        <header className="order-card-head">
          <div>
            <strong>{order.customerName}</strong>
            <div className="muted small">#{order.id.slice(0, 8)} · {time(order.placedAt)} · {minutesAgo(order.placedAt)}</div>
          </div>
        </header>

        <div style={{ marginBottom: "0.75rem" }}>
          <p className="small muted" style={{ margin: "0 0 0.25rem" }}>
            <strong>Pick up from:</strong> {order.restaurantName}
          </p>
          <p className="small muted" style={{ margin: 0 }}>📍 {order.restaurantAddress}</p>
        </div>

        <ul className="lines">
          {order.lines.map((line, i) => (
            <li key={i}>
              <span><strong>{line.quantity}×</strong> {line.name}</span>
              <span>{money(line.total)}</span>
            </li>
          ))}
        </ul>
        {order.notes && <p className="note">📝 {order.notes}</p>}

        <div style={{ marginBottom: "0.75rem" }}>
          <p className="small muted" style={{ margin: "0 0 0.25rem" }}>
            <strong>Deliver to:</strong> {order.deliveryAddress.street}, {order.deliveryAddress.city}
          </p>
          {order.deliveryAddress.instructions && (
            <p className="small muted" style={{ margin: 0 }}>💬 {order.deliveryAddress.instructions}</p>
          )}
          {order.deliveryAddress.phone && (
            <p className="small muted" style={{ margin: 0 }}>📞 {order.deliveryAddress.phone}</p>
          )}
        </div>

        <div className="order-card-foot">
          <span className="small">
            <strong>{money(order.total)}</strong>
            {order.paymentMethod === "CASH" ? (
              <Badge tone="amber"> Collect cash</Badge>
            ) : order.paymentStatus === "PAID" ? (
              <Badge tone="green"> Already paid</Badge>
            ) : (
              <Badge tone="amber"> Collect by card</Badge>
            )}
          </span>
        </div>

        {!isDone && (
          <div className="actions">
            {order.allowedNextStatuses.map((status) => (
              <button
                key={status}
                className="btn btn-sm btn-primary"
                disabled={busy}
                onClick={() => void updateStatus(status)}
              >
                {DRIVER_ACTION_LABEL[status] ?? status}
              </button>
            ))}
          </div>
        )}

        {isDone && (
          <div className="actions">
            <Link to="/driver/available" className="btn btn-sm btn-primary">Back to available deliveries</Link>
          </div>
        )}
      </article>
    </section>
  );
}
