# frontend

One React app (Vite + TypeScript) containing both user interfaces of the delivery platform:

| Path | App | Keycloak realm / client | Talks to |
|---|---|---|---|
| `/` | **Restaurant back office**: live order board, menu editor, settings, reports | `staff` / `backoffice-ui` | `/api/backoffice` → backoffice-service |
| `/webshop` | **Customer webshop**: restaurants, cart, checkout, live order tracking | `customers` / `webshop-ui` | `/api/webshop` → webshop-service |

The backend is what this assignment is judged on, so the UI deliberately stays small: plain React, React Router,
`react-oidc-context` for login, and hand-written CSS with no UI framework.

## Run

**With Docker (whole platform):** put `frontend/` next to the services and run `docker compose up --build`
(see `delivery-platform-infra`). Then open **http://localhost:3000**.

**Dev server (hot reload):**

```bash
npm install
npm run dev        # http://localhost:5173 ; vite.config.ts proxies /api/backoffice -> :8081 and /api/webshop -> :8082
```

The infrastructure and services need to be running: `docker compose up mongo rabbitmq keycloak backoffice webshop`.

## Try the whole flow (two browser windows)

1. **Window A**: http://localhost:3000 → log in as `napoli.manager` / `manager123`. The **Orders** board opens and
   shows a green "Live" indicator.
2. **Window B** (private window, so it gets its own Keycloak session): http://localhost:3000/webshop
   1. Open **Pizzeria Napoli** and add a *Burger menu*. You'll be asked to log in as `customer@delivery.test` /
      `customer123`, or you can sign up.
   2. Go to the cart, choose pickup or delivery and a payment option, then **Place order**.
3. Window A shows **"New order from Alice"** with a sound, and the card appears in *New*. Click **Accept** →
   **Start preparing** → **Ready for pickup** → **Picked up**.
4. Window B's timeline moves forward with each click, without a page reload: SSE fed by RabbitMQ events.
5. In Window A, open **Reports** to see today's revenue, the pickup/delivery and cash/card breakdowns, and the receipt.

Other things to try:
* log in as `napoli.kitchen` / `kitchen123` (staff role: no Reports, Settings, or menu editing);
* log in as `admin` / `admin123` — a restaurant switcher appears in the topbar with **"＋ Add new restaurant…"**, and a **Staff** page (sidebar) lets you create managers and drivers without touching Keycloak;
* log in as `driver1` / `driver123` via the **Driver** option on the login screen to claim and deliver orders;
* close the restaurant in **Settings**, then try to check out in the webshop.

## How it connects to the backend

* **Login:** OIDC Authorization Code + PKCE with Keycloak (`src/lib/config.ts`). Access tokens are kept **in
  memory** and renewed silently. Every API call sends `Authorization: Bearer <token>` (`src/lib/http.ts`).
* **Same origin:** the browser only calls `/api/backoffice/**` and `/api/webshop/**`. nginx (`nginx.conf`) or the
  Vite proxy forwards them, so no CORS is needed. `/internal/**` is never exposed.
* **Errors:** the services return RFC 9457 problem details. `ApiError` exposes `status`, `code` and `message`.
  For example, the webshop reacts to `CART_RESTAURANT_MISMATCH` by offering to start a new cart.
* **Live updates (SSE):** `src/lib/sse.ts` gets a one-time ticket (`POST …/stream-ticket` with the bearer token) and
  opens an `EventSource` with `?ticket=`. Because tickets are single-use, it re-subscribes with a fresh ticket after any
  disconnect, with backoff.
  * Back office: events `order-placed` and `order-updated` on `/restaurants/{id}/orders/stream`.
  * Webshop: event `order-updated` on `/orders/{id}/stream`, carrying the full order with a ready-made `timeline`.
* **Keycloak URL:** the default is `http://localhost:8180`. Override it at build time with `VITE_KEYCLOAK_URL`
  (Docker build arg).

## Structure

```
src/
├── main.tsx                 routes: /webshop/* -> WebshopApp, /* -> BackofficeApp
├── lib/                     config (OIDC), http (fetch + ApiError), sse (ticket-based EventSource), format, types
├── components/ui.tsx        Badge, ErrorBanner, LiveDot, ...
├── backoffice/              BackofficeApp (AuthProvider, /api/me, restaurant context), api.ts, pages/
│   └── pages/               OrdersBoard, MenuEditor, Settings, Reports, StaffManagement (admin only)
└── webshop/                 WebshopApp (AuthProvider, cart context), api.ts, pages/
    └── pages/               RestaurantList, RestaurantPage, CheckoutPage, OrderTracking, MyOrders
```
