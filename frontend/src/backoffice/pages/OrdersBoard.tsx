import { useCallback, useEffect, useState } from "react";
import { errorMessage } from "../../lib/http";
import { subscribeWithTicket } from "../../lib/sse";
import { ACTION_LABEL, money, minutesAgo, paymentLabel, STATUS_LABEL, time } from "../../lib/format";
import type { StaffOrder } from "../../lib/types";
import { Badge, Empty, ErrorBanner, LiveDot, Loading } from "../../components/ui";
import { streamUrl } from "../api";
import { useBackoffice } from "../BackofficeApp";

const COLUMNS: { title: string; statuses: string[] }[] = [
  { title: "New", statuses: ["PLACED"] },
  { title: "Accepted", statuses: ["ACCEPTED"] },
  { title: "Preparing", statuses: ["PREPARING"] },
  { title: "Ready / On the way", statuses: ["READY_FOR_PICKUP", "OUT_FOR_DELIVERY"] },
];

const TERMINAL = ["PICKED_UP", "DELIVERED", "REJECTED", "CANCELLED"];
const NEGATIVE = ["REJECTED", "CANCELLED"];

/** Live kitchen board: initial load over REST, then real-time updates over SSE (fed by RabbitMQ events). */
export function OrdersBoard() {
  const { api, restaurantId } = useBackoffice();
  const [orders, setOrders] = useState<StaffOrder[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [pending, setPending] = useState<{ order: StaffOrder; status: string } | null>(null);

  const upsert = useCallback((order: StaffOrder) => {
    setOrders((current) => {
      const others = (current ?? []).filter((o) => o.id !== order.id);
      return TERMINAL.includes(order.status) ? others : [order, ...others];
    });
  }, []);

  useEffect(() => {
    api.orders(restaurantId).then(setOrders).catch((e) => setError(errorMessage(e)));
    return subscribeWithTicket({
      issueTicket: async () => (await api.streamTicket(restaurantId)).ticket,
      streamUrl: (ticket) => streamUrl(restaurantId, ticket),
      onStatus: setConnected,
      events: {
        "order-placed": (data) => {
          const order = data as StaffOrder;
          upsert(order);
          setToast(`New order from ${order.customerName} · ${money(order.total)}`);
          beep();
        },
        "order-updated": (data) => upsert(data as StaffOrder),
      },
    });
  }, [api, restaurantId, upsert]);

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 6000);
    return () => clearTimeout(timer);
  }, [toast]);

  const move = async (order: StaffOrder, status: string) => {
    if (NEGATIVE.includes(status)) {
      setPending({ order, status });
      return;
    }
    await execute(order, status);
  };

  const execute = async (order: StaffOrder, status: string) => {
    setBusy(order.id);
    try {
      upsert(await api.changeStatus(restaurantId, order.id, status));
    } catch (e) {
      setError(errorMessage(e));
      api.orders(restaurantId).then(setOrders).catch(() => undefined); // resync after a conflict
    } finally {
      setBusy(null);
    }
  };

  return (
    <section>
      {pending && (
        <div className="modal-root">
          <button className="modal-backdrop" onClick={() => setPending(null)} aria-label="Cancel" />
          <div className="modal-panel" role="dialog" aria-modal="true">
            <div className="modal-head">
              <h2>{ACTION_LABEL[pending.status]} order</h2>
              <button className="modal-close" onClick={() => setPending(null)} aria-label="Close">✕</button>
            </div>
            <div className="modal-body">
              <p className="modal-message">
                Are you sure you want to <strong>{ACTION_LABEL[pending.status].toLowerCase()}</strong> the order from <strong>{pending.order.customerName}</strong>? This action cannot be undone.
              </p>
              <div className="modal-actions">
                <button className="btn btn-ghost" onClick={() => setPending(null)}>Keep order</button>
                <button
                  className="btn btn-danger"
                  onClick={() => { const p = pending; setPending(null); void execute(p.order, p.status); }}
                >
                  {ACTION_LABEL[pending.status]}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
      <div className="page-head">
        <div>
          <h1>Live orders</h1>
          <p className="muted" style={{ margin: 0 }}>Kitchen board updates in real time.</p>
        </div>
        <LiveDot connected={connected} />
      </div>
      {toast && <div className="toast">🔔 {toast}</div>}
      <ErrorBanner message={error} onClose={() => setError(null)} />
      {!orders ? (
        <Loading variant="page" label="Loading live orders…" hint="Connecting to the kitchen board" />
      ) : (
        <div className="board">
          {COLUMNS.map((column) => {
            const inColumn = orders
              .filter((o) => column.statuses.includes(o.status))
              .sort((a, b) => a.placedAt.localeCompare(b.placedAt));
            return (
              <div key={column.title} className="column">
                <div className="column-head">
                  <span>{column.title}</span>
                  <span className="count">{inColumn.length}</span>
                </div>
                {inColumn.length === 0 && <Empty>No orders</Empty>}
                {inColumn.map((order) => (
                  <OrderCard key={order.id} order={order} busy={busy === order.id} onMove={(s) => void move(order, s)} />
                ))}
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

function OrderCard({ order, busy, onMove }: { order: StaffOrder; busy: boolean; onMove: (status: string) => void }) {
  return (
    <article className={`order-card ${order.status === "PLACED" ? "order-new" : ""}`}>
      <header className="order-card-head">
        <div>
          <strong>{order.customerName}</strong>
          <div className="muted small">
            #{order.id.slice(0, 8)} · {time(order.placedAt)} · {minutesAgo(order.placedAt)}
          </div>
        </div>
        <Badge tone={order.fulfillmentType === "DELIVERY" ? "blue" : "neutral"}>
          {order.fulfillmentType === "DELIVERY" ? "🛵 Delivery" : "🛍️ Pickup"}
        </Badge>
      </header>
      <ul className="lines">
        {order.lines.map((line) => (
          <li key={line.menuItemId}>
            <span>
              <strong>{line.quantity}×</strong> {line.name}
            </span>
            <span>{money(line.total)}</span>
          </li>
        ))}
      </ul>
      {order.notes && <p className="note">📝 {order.notes}</p>}
      {order.deliveryAddress && (
        <p className="small muted">
          📍 {order.deliveryAddress.street}, {order.deliveryAddress.city} · {order.deliveryAddress.phone}
          {order.deliveryAddress.instructions ? ` · ${order.deliveryAddress.instructions}` : ""}
        </p>
      )}
      <div className="order-card-foot">
        <span className="small">
          <strong>{money(order.total)}</strong> · {paymentLabel(order.paymentMethod, order.paymentTiming, order.fulfillmentType)}{" "}
          <Badge tone={order.paymentStatus === "PAID" ? "green" : "amber"}>{order.paymentStatus === "PAID" ? "Paid" : "To collect"}</Badge>
        </span>
      </div>
      <div className="actions">
        {order.allowedNextStatuses.map((status) => (
          <button
            key={status}
            disabled={busy}
            className={`btn btn-sm ${NEGATIVE.includes(status) ? "btn-danger-ghost" : "btn-primary"}`}
            onClick={() => onMove(status)}
            title={`Move to ${STATUS_LABEL[status]}`}
          >
            {ACTION_LABEL[status] ?? status}
          </button>
        ))}
      </div>
    </article>
  );
}

/** Short notification sound with the Web Audio API (no asset needed). */
function beep() {
  try {
    const ctx = new AudioContext();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.frequency.value = 880;
    gain.gain.setValueAtTime(0.15, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4);
    osc.connect(gain).connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.4);
  } catch {
    // audio not allowed until the user interacted with the page; the toast is enough
  }
}
