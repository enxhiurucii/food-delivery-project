export function money(amount: number | string | null | undefined, currency = "EUR"): string {
  const value = typeof amount === "string" ? Number(amount) : (amount ?? 0);
  return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(value);
}

export function time(iso: string | null | undefined): string {
  if (!iso) return "";
  return new Date(iso).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
}

export function dateTime(iso: string | null | undefined): string {
  if (!iso) return "";
  return new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

export function minutesAgo(iso: string): string {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
  return minutes === 0 ? "just now" : `${minutes} min ago`;
}

export function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export const STATUS_LABEL: Record<string, string> = {
  PLACED: "Order placed",
  ACCEPTED: "Accepted",
  PREPARING: "Preparing",
  READY_FOR_PICKUP: "Ready for pickup",
  OUT_FOR_DELIVERY: "On the way",
  PICKED_UP: "Picked up",
  DELIVERED: "Delivered",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
};

/** Button labels for staff actions, keyed by the target status. */
export const ACTION_LABEL: Record<string, string> = {
  ACCEPTED: "Accept",
  REJECTED: "Reject",
  CANCELLED: "Cancel",
  PREPARING: "Start preparing",
  READY_FOR_PICKUP: "Ready for pickup",
  OUT_FOR_DELIVERY: "Out for delivery",
  PICKED_UP: "Picked up",
  DELIVERED: "Delivered",
};

export const PAYMENT_LABEL: Record<string, string> = {
  "CARD/ONLINE": "Paid online by card",
  "CASH/ON_COMPLETION": "Cash",
  "CARD/ON_COMPLETION": "Card at the counter",
};

export function paymentLabel(method: string, timing: string, fulfillment?: string): string {
  if (method === "CASH" && timing === "ON_COMPLETION") return fulfillment === "DELIVERY" ? "Cash on delivery" : "Pay at the restaurant (cash)";
  if (method === "CARD" && timing === "ON_COMPLETION") return "Pay at the restaurant (card)";
  return PAYMENT_LABEL[`${method}/${timing}`] ?? `${method} ${timing}`;
}
