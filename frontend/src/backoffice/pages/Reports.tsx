import { useEffect, useMemo, useState, type ReactNode } from "react";
import { errorMessage } from "../../lib/http";
import { money, paymentLabel, time, todayIso } from "../../lib/format";
import type { Breakdown, DailyReport, Page, StaffOrder } from "../../lib/types";
import { Badge, Empty, ErrorBanner, Loading } from "../../components/ui";
import { IconCart, IconChart, IconTruck } from "../../components/icons";
import { useBackoffice } from "../BackofficeApp";

const KEY_LABEL: Record<string, string> = { PICKUP: "Pickup", DELIVERY: "Delivery", CASH: "Cash", CARD: "Card" };

/** Daily report (computed by a MongoDB aggregation in the restaurant's timezone) and the receipts of that day. */
export function Reports() {
  const { api, restaurantId, restaurant } = useBackoffice();
  const [date, setDate] = useState(todayIso());
  const [page, setPage] = useState(0);
  const [report, setReport] = useState<DailyReport | null>(null);
  const [receipts, setReceipts] = useState<Page<StaffOrder> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setReport(null);
    api.dailyReport(restaurantId, date).then(setReport).catch((e) => setError(errorMessage(e)));
  }, [api, restaurantId, date]);

  useEffect(() => {
    api.receipts(restaurantId, date, page).then(setReceipts).catch((e) => setError(errorMessage(e)));
  }, [api, restaurantId, date, page]);

  const shiftDay = (days: number) => {
    const d = new Date(`${date}T12:00:00`);
    d.setDate(d.getDate() + days);
    setPage(0);
    setDate(d.toISOString().slice(0, 10));
  };

  const dateLabel = useMemo(() => {
    const d = new Date(`${date}T12:00:00`);
    return d.toLocaleDateString(undefined, { weekday: "long", year: "numeric", month: "long", day: "numeric" });
  }, [date]);

  const isToday = date === todayIso();

  return (
    <section className="reports-page">
      <div className="reports-hero">
        <div className="reports-hero-copy">
          <div className="menu-hero-kicker">
            <IconChart width={18} height={18} />
            Daily insights
          </div>
          <h1>Reports</h1>
          <p className="muted">
            Revenue and closed orders for <strong>{restaurant.name}</strong>
            {isToday ? " · today" : ""}.
          </p>
          <p className="reports-date-label">{dateLabel}</p>
        </div>
        <div className="reports-date-nav card">
          <span className="muted small">Choose day</span>
          <div className="row gap-sm reports-date-controls">
            <button type="button" className="btn btn-sm" onClick={() => shiftDay(-1)} aria-label="Previous day">
              ← Prev
            </button>
            <input
              type="date"
              value={date}
              max={todayIso()}
              onChange={(e) => {
                setPage(0);
                setDate(e.target.value);
              }}
              aria-label="Report date"
            />
            <button type="button" className="btn btn-sm" onClick={() => shiftDay(1)} disabled={isToday} aria-label="Next day">
              Next →
            </button>
          </div>
          {!isToday && (
            <button type="button" className="btn btn-outline btn-sm" onClick={() => { setPage(0); setDate(todayIso()); }}>
              Jump to today
            </button>
          )}
        </div>
      </div>

      <ErrorBanner message={error} onClose={() => setError(null)} />

      {!report ? (
        <Loading variant="section" label="Loading report…" hint="Aggregating today’s numbers" />
      ) : (
        <>
          <div className="kpis">
            <Kpi
              tone="orders"
              icon={<IconCart width={18} height={18} />}
              label="Completed orders"
              value={String(report.orders)}
              hint="Picked up or delivered"
            />
            <Kpi
              tone="revenue"
              icon={<IconChart width={18} height={18} />}
              label="Revenue"
              value={money(report.revenue)}
              hint="Gross for this day"
            />
            <Kpi
              tone="average"
              icon={<IconTruck width={18} height={18} />}
              label="Average order"
              value={money(report.averageOrderValue)}
              hint="Per completed order"
            />
          </div>

          <div className="two-col">
            <BreakdownTable title="By order type" rows={report.byFulfillmentType} total={report.revenue} />
            <BreakdownTable title="By payment method" rows={report.byPaymentMethod} total={report.revenue} />
          </div>

          <p className="muted small reports-note">
            Day computed in {report.timezone}. Only completed orders (picked up / delivered) count toward KPIs.
          </p>
        </>
      )}

      <div className="card reports-receipts">
        <div className="reports-receipts-head">
          <div>
            <h2>Receipts</h2>
            <p className="muted small" style={{ margin: 0 }}>
              Closed orders for {dateLabel}
            </p>
          </div>
          {receipts && receipts.content.length > 0 && (
            <Badge tone="green">{receipts.totalElements} closed</Badge>
          )}
        </div>

        {!receipts ? (
          <Loading variant="section" label="Loading receipts…" hint="Fetching closed orders" />
        ) : receipts.content.length === 0 ? (
          <Empty>No closed orders on this day.</Empty>
        ) : (
          <>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Closed</th>
                    <th>Order</th>
                    <th>Customer</th>
                    <th>Type</th>
                    <th>Items</th>
                    <th>Payment</th>
                    <th className="num">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {receipts.content.map((o) => (
                    <tr key={o.id}>
                      <td>{time(o.closedAt)}</td>
                      <td className="mono">#{o.id.slice(0, 8)}</td>
                      <td>{o.customerName}</td>
                      <td>
                        <Badge tone={o.fulfillmentType === "DELIVERY" ? "blue" : "neutral"}>
                          {o.fulfillmentType === "DELIVERY" ? "Delivery" : "Pickup"}
                        </Badge>
                      </td>
                      <td className="small">{o.lines.map((l) => `${l.quantity}× ${l.name}`).join(", ")}</td>
                      <td className="small">{paymentLabel(o.paymentMethod, o.paymentTiming, o.fulfillmentType)}</td>
                      <td className="num"><strong>{money(o.total)}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {receipts.totalPages > 1 && (
              <div className="row gap-sm end reports-pager">
                <button className="btn btn-sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
                  Previous
                </button>
                <span className="small muted">
                  Page {page + 1} / {receipts.totalPages}
                </span>
                <button className="btn btn-sm" disabled={page + 1 >= receipts.totalPages} onClick={() => setPage(page + 1)}>
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}

function Kpi({
  label,
  value,
  hint,
  icon,
  tone,
}: {
  label: string;
  value: string;
  hint: string;
  icon: ReactNode;
  tone: "orders" | "revenue" | "average";
}) {
  return (
    <div className={`kpi kpi-${tone}`}>
      <div className="kpi-top">
        <span className="kpi-icon">{icon}</span>
        <span>{label}</span>
      </div>
      <strong>{value}</strong>
      <em>{hint}</em>
    </div>
  );
}

function BreakdownTable({ title, rows, total }: { title: string; rows: Breakdown[]; total: number }) {
  return (
    <div className="card breakdown-card">
      <h3>{title}</h3>
      {rows.length === 0 ? (
        <p className="muted small">No data for this day.</p>
      ) : (
        rows.map((row) => {
          const share = total > 0 ? (row.revenue / total) * 100 : 0;
          return (
            <div key={row.key} className="breakdown-row">
              <div className="row space-between small">
                <span>
                  {KEY_LABEL[row.key] ?? row.key}
                  <span className="muted"> · {row.orders} orders</span>
                </span>
                <strong>
                  {money(row.revenue)}
                  <span className="muted" style={{ fontWeight: 600 }}> · {Math.round(share)}%</span>
                </strong>
              </div>
              <div className="bar">
                <div style={{ width: `${share}%` }} />
              </div>
            </div>
          );
        })
      )}
    </div>
  );
}
