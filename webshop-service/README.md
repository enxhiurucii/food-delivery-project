# webshop-service

The customer side of the delivery platform: browse restaurants and menus, build a cart, check out for pickup or
delivery with cash or card, and follow the order on a live timeline that updates by itself.

**Stack:** Java 25 (LTS) · Spring Boot 4.1 · Gradle 9 (Kotlin DSL) · MongoDB 8 · RabbitMQ 4 · Keycloak 26 (OAuth2/OIDC) · Caffeine · Spock 2.4 (Groovy 5) · Testcontainers 2

---

## 1. Where this service fits

```
 Browser (React)            Keycloak realm "customers" (login, self-registration)
      │  Bearer JWT (aud=webshop-api)
      ▼
 nginx /api/webshop/** ──► webshop-service ──HTTP (client-credentials token, realm "staff")──► backoffice /internal/**
                               │    ▲
                      MongoDB  │    │ order.status.changed
                     "webshop" │    │
                               └──► RabbitMQ (orders.events) ──order.placed──► backoffice
```

* **Owns:** carts, checkout, the payment choice, and the customer's **projection** of each order, which feeds the tracking timeline.
* **Sync HTTP to the back office** for catalog data (cached for 30 s) and authoritative **prices at checkout**. The
  browser's prices are never trusted.
* **Async to the back office:** `order.placed` goes out through a transactional outbox, and `order.status.changed`
  comes back in, is applied idempotently, and is pushed to the tracking page with SSE.

## 2. Running it

The full stack runs with `docker compose up --build` (see `delivery-platform-infra/docker-compose.yml`).
This service is available at **http://localhost:8082**.

To run only this service from the IDE: start `docker compose up mongo rabbitmq keycloak backoffice`, then run
`./gradlew bootRun`.

Tests: `./gradlew test`. Unit and web-slice specs always run; the Testcontainers spec runs when Docker is available.

### Demo customer

| User | Password |
|---|---|
| `customer@delivery.test` | `customer123` |

Anyone can also **self-register** on the Keycloak login page; new users get the `CUSTOMER` role automatically.

```bash
TOKEN=$(curl -s http://localhost:8180/realms/customers/protocol/openid-connect/token \
  -d grant_type=password -d client_id=webshop-ui \
  -d username=customer@delivery.test -d password=customer123 | jq -r .access_token)

curl -s http://localhost:8082/api/restaurants | jq                       # public
curl -s http://localhost:8082/api/restaurants/napoli/menus | jq          # public; pick a menu item id
curl -s -X POST http://localhost:8082/api/cart/items -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"restaurantId":"napoli","menuItemId":"<id>","quantity":2}' | jq
curl -s -X POST http://localhost:8082/api/checkout -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"fulfillmentType":"PICKUP","paymentMethod":"CASH","paymentTiming":"ON_COMPLETION"}' | jq
```

The password grant is enabled on `webshop-ui` for local testing only. The SPA uses Authorization Code + PKCE.

## 3. Security model

| Concern | How |
|---|---|
| Customer login | **Keycloak** realm `customers` (self-registration on). No passwords in this service. |
| Token validation | OAuth2 resource server: JWKS signature, plus `iss`, `aud=webshop-api` and `exp`. Role `CUSTOMER` comes from `realm_access.roles`. |
| Public vs protected | `GET /api/restaurants/**` is public. Cart, checkout and orders need `ROLE_CUSTOMER`. |
| Ownership | The owner is **always the token's `sub`**, never a request field. Someone else's order id returns **404** (not 403), so ids can't be probed. |
| Calling the back office | **The webshop's own identity**: OAuth2 client credentials (client `webshop-service` in realm `staff`, role `CATALOG_READER`). Tokens are cached and renewed by `OAuth2ClientHttpRequestInterceptor`. The customer's token is **never forwarded**. |
| SSE | One-time ticket (30 s, single use, bound to one order). No JWT in URLs. |
| Staff vs customers | Separate realms mean separate issuers. A customer token is rejected by the back office, and a staff token is rejected here. |

## 4. Connecting the frontend

### 4.1 Login

```tsx
import { WebStorageStateStore, InMemoryWebStorage } from "oidc-client-ts";

export const webshopOidc = {
  authority: "http://localhost:8180/realms/customers",
  client_id: "webshop-ui",                       // public client, Authorization Code + PKCE
  redirect_uri: `${window.location.origin}/webshop`,
  post_logout_redirect_uri: `${window.location.origin}/webshop`,
  scope: "openid profile email",
  automaticSilentRenew: true,
  userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),   // tokens in memory
  onSigninCallback: () => window.history.replaceState({}, document.title, window.location.pathname),
};
// <AuthProvider {...webshopOidc}> around the /webshop routes.
// "Sign up" button: auth.signinRedirect({ extraQueryParams: { kc_action: "register" } })  (or link to the Keycloak registration page)
```

Browsing doesn't need a login, so only ask the customer to log in when they add something to the cart.

### 4.2 API helper

Same pattern as the back office, with the base path `/api/webshop`. Errors are RFC 9457 problem details with a
machine-readable `code`:

| `code` | When |
|---|---|
| `CART_RESTAURANT_MISMATCH` | adding an item from a second restaurant (offer "empty cart and continue") |
| `CART_EMPTY`, `ITEM_NOT_IN_CART`, `INVALID_QUANTITY` | cart problems |
| `ITEM_NOT_FOUND`, `ITEM_UNAVAILABLE` | the menu changed |
| `RESTAURANT_CLOSED`, `PICKUP_NOT_AVAILABLE`, `DELIVERY_NOT_AVAILABLE` | restaurant settings |
| `ADDRESS_REQUIRED`, `MINIMUM_ORDER_NOT_REACHED` | checkout rules |
| `PAYMENT_OPTION_NOT_ALLOWED`, `PAYMENT_REQUIRED`, `PAYMENT_DECLINED` | payment rules |
| `RESTAURANT_SERVICE_UNAVAILABLE` (503) | the back office is down; safe to retry |

### 4.3 Checkout form: payment options

| Fulfillment | Option shown to the customer | `paymentMethod` | `paymentTiming` | Status after checkout |
|---|---|---|---|---|
| Pickup | Pay now by card | `CARD` | `ONLINE` | `PAID` |
| Pickup | Pay at the restaurant (cash) | `CASH` | `ON_COMPLETION` | `PENDING` → `PAID` when picked up |
| Pickup | Pay at the restaurant (card) | `CARD` | `ON_COMPLETION` | `PENDING` → `PAID` when picked up |
| Delivery | Pay now by card | `CARD` | `ONLINE` | `PAID` |
| Delivery | Cash on delivery | `CASH` | `ON_COMPLETION` | `PENDING` → `PAID` when delivered |

```json
POST /api/checkout
{
  "fulfillmentType": "DELIVERY",
  "deliveryAddress": { "street": "Rruga Myslym Shyri 10", "city": "Tirana", "postalCode": "1001",
                       "phone": "+355 69 123 4567", "instructions": "3rd floor" },
  "paymentMethod": "CASH",
  "paymentTiming": "ON_COMPLETION",
  "notes": "No onions"
}
```

The response is `201 { "order": OrderView, "pricesChanged": false }`. If `pricesChanged` is true, tell the customer
that the menu prices changed and the order uses the current prices. Then redirect to `/webshop/orders/{id}`.

### 4.4 "Your order was received": live timeline

`GET /api/orders/{id}` returns an `OrderView` with a `timeline` that is ready to draw:

```json
"status": "PREPARING",
"finished": false,
"estimatedReadyAt": "2026-11-23T12:20:00Z",
"timeline": [
  { "status": "PLACED",           "reached": true,  "current": false, "at": "2026-11-23T12:00:00Z" },
  { "status": "ACCEPTED",         "reached": true,  "current": false, "at": "2026-11-23T12:01:10Z" },
  { "status": "PREPARING",        "reached": true,  "current": true,  "at": "2026-11-23T12:03:00Z" },
  { "status": "READY_FOR_PICKUP", "reached": false, "current": false, "at": null },
  { "status": "PICKED_UP",        "reached": false, "current": false, "at": null }
]
```

* A delivery order shows `OUT_FOR_DELIVERY` → `DELIVERED` instead of the pickup steps.
* A rejected or cancelled order shows only the steps it really reached, then `REJECTED` or `CANCELLED`.

Live updates:

```ts
export async function followOrder(token: string, orderId: string, onUpdate: (o: OrderView) => void) {
  const { ticket } = await api<{ ticket: string }>(token, `/orders/${orderId}/stream-ticket`, { method: "POST" });
  const es = new EventSource(`/api/webshop/orders/${orderId}/stream?ticket=${encodeURIComponent(ticket)}`);
  es.addEventListener("order-updated", e => {
    const order: OrderView = JSON.parse((e as MessageEvent).data);
    onUpdate(order);
    if (order.finished) es.close();                       // picked up / delivered / rejected: stop listening
  });
  es.onerror = () => { es.close(); setTimeout(() => followOrder(token, orderId, onUpdate), 3000); };  // tickets are single-use
  return () => es.close();
}
```

## 5. API reference

Paths are relative to `/api` (nginx: `/api/webshop`). Money is a decimal with 2 places, currency `EUR`.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/restaurants` | public | restaurants (`open`, `pickupAvailable`, `deliveryAvailable`, fee, minimum) |
| GET | `/restaurants/{id}` | public | one restaurant |
| GET | `/restaurants/{id}/menus` | public | menus and items |
| GET | `/cart` | CUSTOMER | current cart |
| POST | `/cart/items` | CUSTOMER | `{restaurantId, menuItemId, quantity}`; validated and priced by the back office |
| PUT | `/cart/items/{menuItemId}` | CUSTOMER | `{quantity}` (0 removes the item) |
| DELETE | `/cart` | CUSTOMER | empty the cart |
| POST | `/checkout` | CUSTOMER | place the order (see 4.3) |
| GET | `/orders` | CUSTOMER | my last 50 orders |
| GET | `/orders/{id}` | CUSTOMER (owner) | order and timeline |
| POST | `/orders/{id}/stream-ticket` | CUSTOMER (owner) | one-time SSE ticket |
| GET | `/orders/{id}/stream?ticket=` | ticket | SSE event `order-updated` with the full `OrderView` |

## 6. How checkout works

1. Load the customer's cart; it must not be empty.
2. Make a **sync HTTP call** to `POST backoffice /internal/restaurants/{id}/pricing`. This returns the current prices and
   availability, plus the restaurant's rules: open, pickup/delivery, fee, minimum order and prep time.
3. Check the rules: restaurant open → fulfillment type offered → address present → minimum order → payment option allowed.
4. Charge `CARD + ONLINE` payments through `PaymentProcessor` (a mock that always approves; a real PSP would plug in here).
5. **In one MongoDB transaction:** save the `CustomerOrder`, write the `order.placed` outbox event and delete the cart.
6. `OutboxRelay` publishes to RabbitMQ (publisher confirms). The back office receives the order and its dashboard shows
   "New order!".

## 7. Code structure

```
com.delivery.webshop
├── catalog/   api (public browsing) · infrastructure (BackofficeClient HTTP interface, gateway with cache + error mapping)
├── cart/      api · application · domain (Cart: one restaurant per cart, TTL 7 days) · infrastructure
├── checkout/  api · application (CheckoutService) · infrastructure (PaymentProcessor port + mock)
├── order/     api (tracking, OrderView/timeline) · application (status-change handler) · domain (CustomerOrder, PaymentPolicy) · infrastructure (Rabbit listener)
└── shared/    config · error · money · messaging (Rabbit, events, outbox) · security · sse
```

## 8. Decisions and assumptions

* **Guest checkout is not supported**: you need to log in to order. Browsing is public.
* **Carts are stored on the server**, one per customer and tied to one restaurant (like Wolt). Carts expire after 7 days
  through a MongoDB TTL index.
* **Prices are snapshotted twice.** The cart keeps a display snapshot; checkout re-prices and the order keeps the final
  snapshot, so later menu edits never change an order.
* **Payments are simplified, as agreed.** Card online payments are always approved; deferred payments become `PAID`
  when the back office completes the order.
* **The webshop doesn't own the lifecycle.** It mirrors the back office's statuses and drops duplicate or stale events
  using `sequence`.
* **Calls to the back office time out** after 2 s to connect and 5 s to read, and failures become a clear 503.
  Catalog reads are cached; pricing never is.

**Production next steps:**
* a BFF, so the browser holds only an HttpOnly session cookie;
* idempotency keys on `POST /checkout` to protect against double-clicks;
* customer address book and profile;
* refund/compensation when a charge succeeds but saving the order fails;
* move SSE tickets to Redis when running several instances.
