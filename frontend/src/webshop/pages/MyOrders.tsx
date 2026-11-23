import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { errorMessage } from "../../lib/http";
import { dateTime, money, STATUS_LABEL } from "../../lib/format";
import type { CustomerOrder } from "../../lib/types";
import { Badge, Empty, ErrorBanner, Loading } from "../../components/ui";
import { RequireLogin, useShop } from "../WebshopApp";

export function MyOrders() {
  return (
    <RequireLogin message="Log in to see your orders.">
      <Orders />
    </RequireLogin>
  );
}

function Orders() {
  const { api } = useShop();
  const [orders, setOrders] = useState<CustomerOrder[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.orders().then(setOrders).catch((e) => setError(errorMessage(e)));
  }, [api]);

  if (error) return <ErrorBanner message={error} />;
  if (!orders) return <Loading variant="page" label="Loading your orders…" hint="Fetching order history" />;

  return (
    <section className="narrow-page">
      <div className="page-head">
        <div>
          <h1>My orders</h1>
          <p className="muted" style={{ margin: 0 }}>Track every TasteHouse order in one place.</p>
        </div>
      </div>
      {orders.length === 0 && <Empty>No orders yet. <Link to="/webshop">Find something tasty →</Link></Empty>}
      {orders.map((o) => (
        <Link key={o.id} to={`/webshop/orders/${o.id}`} className="card order-row">
          <div>
            <strong>{o.restaurantName}</strong>
            <div className="muted small">{dateTime(o.placedAt)} · {o.lines.map((l) => `${l.quantity}× ${l.name}`).join(", ")}</div>
          </div>
          <div className="order-row-right">
            <Badge tone={o.finished ? (o.status === "REJECTED" || o.status === "CANCELLED" ? "red" : "neutral") : "blue"}>
              {STATUS_LABEL[o.status] ?? o.status}
            </Badge>
            <strong>{money(o.total)}</strong>
          </div>
        </Link>
      ))}
    </section>
  );
}
