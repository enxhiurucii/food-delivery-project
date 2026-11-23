# backoffice-service

Restaurant management for the delivery platform: restaurants and their settings, the menu editor, the live
order board with its status workflow, receipts and daily reports.

**Stack:** Java 25 (LTS) · Spring Boot 4.1 · Gradle 9 (Kotlin DSL) · MongoDB 8 · RabbitMQ 4 · Keycloak 26 (OAuth2/OIDC) · Spock 2.4 (Groovy 5) · Testcontainers 2

---

## 1. Where this service fits

```
 Browser (React)            Keycloak realm "staff" (login, tokens)
      │  Bearer JWT (aud=backoffice-api)                 ▲ client credentials
      ▼                                                  │
 nginx /api/backoffice/** ──► backoffice-service ◄── HTTP /internal/** ── webshop-service
                                   │    ▲                                       │
                          MongoDB  │    │ order.placed        order.status.changed
                       "backoffice"│    └──────── RabbitMQ (orders.events) ◄────┘
```

* **Owns:** restaurants, menus and menu items, staff-to-restaurant assignments, and the **authoritative order lifecycle**.
* **Receives orders** asynchronously: the webshop publishes `order.placed` and this service consumes it idempotently.
* **Publishes** `order.status.changed` through a **transactional outbox** whenever staff move an order forward.
* **Serves the webshop** a read-only internal catalog API plus authoritative pricing for checkout (`/internal/**`).
* **Pushes live updates** to open dashboards using Server-Sent Events, e.g. a "New order!" notification.

## 2. Running it

### Everything with Docker (recommended)

Unzip `backoffice-service`, `webshop-service`, `frontend` and `delivery-platform-infra` next to each other. The
contents of `delivery-platform-infra` go in the root folder:

```
delivery-platform/
├── docker-compose.yml  keycloak/  rabbitmq/  nginx/
├── backoffice-service/
├── webshop-service/
└── frontend/
```

```bash
docker compose up --build
```

| What | URL | Credentials |
|---|---|---|
| **Web app** (back office / webshop) | http://localhost:3000 · http://localhost:3000/webshop | see below |
| Back-office API | http://localhost:8081 | Bearer token from realm `staff` |
| Keycloak admin | http://localhost:8180 | admin / admin |
| RabbitMQ management | http://localhost:15672 | admin / admin |

### Only this service, from the IDE

Start the infrastructure with `docker compose up mongo rabbitmq keycloak`, then run `./gradlew bootRun`. The
defaults in `application.yml` already point at `localhost`.

### Tests

```bash
./gradlew test            # unit + web-slice specs always run; Testcontainers specs run when Docker is available
open build/reports/tests/test/index.html
open build/reports/jacoco/test/html/index.html
```

## 3. Demo data and users

Restaurants are seeded on the first start (`app.seed.enabled=true`). Users live in Keycloak (`keycloak/staff-realm.json`).

| User | Password | Roles | Works at |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | all restaurants (platform operator) |
| `napoli.manager` | `manager123` | MANAGER, STAFF | `napoli` (Pizzeria Napoli) |
| `napoli.kitchen` | `kitchen123` | STAFF | `napoli` |
| `sushi.manager` | `manager123` | MANAGER, STAFF | `sushi-zen` (Sushi Zen), handy for testing tenant isolation |
| `driver1` | `driver123` | DRIVER | platform-wide (not tied to a restaurant) |

The admin can create new managers and drivers without leaving the app: log in as `admin`, go to the **Staff** page (people icon in the sidebar), or add a restaurant from the dropdown in the topbar.

Quick check with curl. The password grant is enabled on `backoffice-ui` **for local testing only**; the SPA uses
Authorization Code + PKCE.

```bash
TOKEN=$(curl -s http://localhost:8180/realms/staff/protocol/openid-connect/token \
  -d grant_type=password -d client_id=backoffice-ui \
  -d username=napoli.manager -d password=manager123 | jq -r .access_token)

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/me | jq
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/restaurants/napoli/orders | jq
```

## 4. Security model

| Concern | How |
|---|---|
| Who issues tokens | **Keycloak**, realm `staff`. This service stores **no passwords** and has no login endpoint. |
| Token validation | OAuth2 resource server: signature via JWKS, plus `iss`, `aud=backoffice-api` and `exp`. |
| Roles | Keycloak realm roles (`realm_access.roles`) are mapped to `ROLE_*` by `KeycloakJwtAuthenticationConverter`. |
| Tenant isolation | `@PreAuthorize("@tenantGuard.canAccess(#restaurantId, authentication)")`. The token's `sub` is looked up in `staff_members` (the user ↔ restaurant link is back-office domain data). Only ADMIN bypasses it. |
| Service-to-service | `/internal/**` requires role `CATALOG_READER`, which only the webshop's service account has (client-credentials token). nginx does not route `/internal`. |
| SSE | `EventSource` can't send headers, so the client first gets a **one-time, 30-second ticket** with its bearer token. No JWT ever appears in a URL. |
| CSRF / sessions | Stateless bearer API, no cookies, so CSRF protection is disabled on purpose. |

Role matrix:

| | STAFF | MANAGER | ADMIN | DRIVER |
|---|---|---|---|---|
| Order board, change status | own restaurant | own restaurant | all | – |
| Read menus / restaurant | own | own | all | – |
| Edit menus / settings | – | own | all | – |
| Reports, receipts, staff list | – | own | all | – |
| Create restaurants, assign staff, create users | – | – | ✓ | – |
| Claim and deliver orders | – | – | – | ✓ |

## 5. Connecting the frontend

### 5.1 Login (React + `react-oidc-context`)

```bash
npm i oidc-client-ts react-oidc-context
```

```tsx
// src/auth.tsx
import { AuthProvider } from "react-oidc-context";
import { WebStorageStateStore, InMemoryWebStorage } from "oidc-client-ts";

export const backofficeOidc = {
  authority: "http://localhost:8180/realms/staff",
  client_id: "backoffice-ui",                   // public client, Authorization Code + PKCE
  redirect_uri: `${window.location.origin}/`,
  post_logout_redirect_uri: `${window.location.origin}/`,
  scope: "openid profile email",
  automaticSilentRenew: true,
  // Tokens in memory, not localStorage: XSS can't read them from storage. Keycloak's SSO cookie re-logs in on reload.
  userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
  onSigninCallback: () => window.history.replaceState({}, document.title, window.location.pathname),
};

// main.tsx
// <AuthProvider {...backofficeOidc}><App /></AuthProvider>

// In a component:
// const auth = useAuth();
// if (!auth.isAuthenticated) return <button onClick={() => auth.signinRedirect()}>Log in</button>;
// const token = auth.user!.access_token;
```

After login, call `GET /api/me` to learn the user's `restaurantId` and `roles`, then route to that restaurant's dashboard.

### 5.2 Calling the API

Behind nginx the base path is `/api/backoffice` (same origin, no CORS). With the Vite dev server, use a proxy:

```ts
// vite.config.ts
server: {
  port: 5173,
  proxy: {
    "/api/backoffice": { target: "http://localhost:8081", rewrite: p => p.replace(/^\/api\/backoffice/, "/api") },
    "/api/webshop":    { target: "http://localhost:8082", rewrite: p => p.replace(/^\/api\/webshop/, "/api") },
  },
},
```

```ts
// src/api/backoffice.ts
export async function api<T>(token: string, path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(`/api/backoffice${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}`, ...init.headers },
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));       // RFC 9457 problem detail
    throw Object.assign(new Error(problem.detail ?? res.statusText), { status: res.status, code: problem.code });
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

// await api(token, `/restaurants/napoli/orders/${id}/status`, { method: "POST", body: JSON.stringify({ status: "ACCEPTED" }) });
```

Errors always look like this:

```json
{ "type": "about:blank", "title": "Conflict", "status": 409, "code": "INVALID_STATUS_TRANSITION",
  "detail": "Order 7f3c... (PICKUP) cannot move from PLACED to DELIVERED" }
```

| Status | Meaning |
|---|---|
| 400 | validation error |
| 401 | missing or expired token, or bad SSE ticket |
| 403 | wrong role or another restaurant |
| 404 | not found |
| 409 | invalid transition or concurrent edit |
| 422 | business rule (`code` says which) |

### 5.3 Live order board (SSE with a one-time ticket)

```ts
export async function subscribeToOrders(
  token: string, restaurantId: string,
  onEvent: (type: "order-placed" | "order-updated", order: Order) => void,
): Promise<() => void> {
  const { ticket } = await api<{ ticket: string }>(token, `/restaurants/${restaurantId}/orders/stream-ticket`, { method: "POST" });
  const es = new EventSource(`/api/backoffice/restaurants/${restaurantId}/orders/stream?ticket=${encodeURIComponent(ticket)}`);
  es.addEventListener("order-placed",  e => onEvent("order-placed",  JSON.parse((e as MessageEvent).data)));  // show "New order!" + sound
  es.addEventListener("order-updated", e => onEvent("order-updated", JSON.parse((e as MessageEvent).data)));
  // A ticket is single-use: on any error, close and re-subscribe with a fresh ticket.
  es.onerror = () => { es.close(); setTimeout(() => subscribeToOrders(token, restaurantId, onEvent), 3000); };
  return () => es.close();
}
```

Every order in a response contains `allowedNextStatuses`, so the UI can render exactly the buttons that are
valid right now (e.g. `["ACCEPTED", "REJECTED", "CANCELLED"]` for a new order).

## 6. API reference

All paths below are relative to `/api` (nginx: `/api/backoffice`). Money is a decimal with 2 places, currency `EUR`.

| Method | Path | Who | Purpose |
|---|---|---|---|
| GET | `/me` | any staff | subject, username, roles, `restaurantId` |
| GET | `/restaurants` | ADMIN | all restaurants |
| POST | `/restaurants` | ADMIN | create restaurant |
| GET | `/restaurants/{id}` | team | restaurant + settings |
| PUT | `/restaurants/{id}` | MANAGER | update name, address, timezone, open/closed, pickup/delivery, fee, minimum order, prep time |
| GET / POST | `/restaurants/{id}/staff` | MANAGER / ADMIN | list / assign a Keycloak user (`subject`) to the restaurant |
| GET | `/restaurants/{id}/menus` | team | menus with items |
| POST / PUT / DELETE | `/restaurants/{id}/menus[/{menuId}]` | MANAGER | menu CRUD |
| POST / PUT / DELETE | `/restaurants/{id}/menus/{menuId}/items[/{itemId}]` | MANAGER | item CRUD (`name, description, price, imageUrl, available`) |
| GET | `/restaurants/{id}/orders?status=PLACED,ACCEPTED` | team | order board (default: all active orders) |
| GET | `/restaurants/{id}/orders/{orderId}` | team | one order |
| POST | `/restaurants/{id}/orders/{orderId}/status` | team | `{ "status": "ACCEPTED" }` |
| POST | `/restaurants/{id}/orders/stream-ticket` | team | one-time SSE ticket |
| GET | `/restaurants/{id}/orders/stream?ticket=` | ticket | SSE: `order-placed`, `order-updated` |
| GET | `/restaurants/{id}/reports/daily?date=2026-11-23` | MANAGER | orders, revenue, average, breakdown by fulfillment type and payment method |
| GET | `/restaurants/{id}/receipts?date=2026-11-23&page=0&size=20` | MANAGER | closed (completed) orders of that day |
| POST | `/admin/users/managers` | ADMIN | create a Keycloak user with role MANAGER and link them to a restaurant |
| POST | `/admin/users/drivers` | ADMIN | create a Keycloak user with role DRIVER |
| GET | `/driver/deliveries/available` | DRIVER | orders with status READY_FOR_PICKUP not yet claimed |
| GET | `/driver/deliveries/active` | DRIVER | the driver's current in-progress delivery |
| POST | `/driver/deliveries/{orderId}/claim` | DRIVER | claim a delivery |
| POST | `/driver/deliveries/{orderId}/status` | DRIVER | advance delivery status (PICKED_UP → OUT_FOR_DELIVERY → DELIVERED) |

Internal (service-to-service only, amounts in cents): `GET /internal/restaurants`, `GET /internal/restaurants/{id}`,
`GET /internal/restaurants/{id}/menus`, `POST /internal/restaurants/{id}/pricing`.

## 7. Order lifecycle

```
PLACED ──► ACCEPTED ──► PREPARING ──► READY_FOR_PICKUP ──► PICKED_UP     (pickup)
   │           │              └─────► OUT_FOR_DELIVERY ──► DELIVERED     (delivery)
   ├──► REJECTED                       (restaurant declines)
   └───────────┴──► CANCELLED          (only before PREPARING)
```

* The rules live in the aggregate (`Order.changeStatus` and `OrderStatus.canTransitionTo`). An invalid move returns 409.
* Every change is appended to `statusHistory`, and the webshop's timeline is built from it.
* Terminal states set `closedAt`. `PICKED_UP` and `DELIVERED` count as revenue and also settle deferred payments
  (cash or card at the counter, cash to the courier).
* `@Version` optimistic locking: if two people click at the same moment, one gets a 409 instead of a lost update.

## 8. Messaging

| | |
|---|---|
| Exchange | `orders.events` (topic, durable), dead letters to `orders.events.dlx` |
| Consumes | `order.placed` → queue `backoffice.order-placed` (3 attempts, then `backoffice.order-placed.dlq`) |
| Publishes | `order.status.changed` `{eventId, orderId, restaurantId, status, sequence, paymentStatus, changedAt}` |
| Delivery | at-least-once; consumers are idempotent (order id = webshop's id; status events carry a `sequence`) |
| Reliability | transactional outbox: the order update and the event are written in one MongoDB transaction, and `OutboxRelay` publishes with publisher confirms |
| Trust | each service has its own RabbitMQ user with least-privilege permissions; the AMQP `user-id` is set and validated by the broker |

## 9. Code structure

Package by feature. Inside each feature: `api` (controllers, DTOs) → `application` (use cases, transactions)
→ `domain` (aggregates and rules, no Spring web/AMQP) ← `infrastructure` (Mongo, Rabbit).

```
com.delivery.backoffice
├── restaurant/   menu/   order/   catalog/ (internal API)   report/   staff/   driver/
├── admin/   (user management: creates Keycloak users + assigns staff via admin REST API)
└── shared/  config · error (ProblemDetail) · money · messaging (Rabbit, events, outbox) · security · sse · seed
```

## 10. Decisions and assumptions

* **Identity is delegated to Keycloak.** No passwords live in this service. It only validates tokens, so authentication
  really is "just configuration". Staff ↔ restaurant is domain data, keyed by the token `sub` rather than the email.
* **Money is stored as `long` cents.** It is exact and works with `$sum` in aggregations; the API shows decimals.
* **Menu items are embedded in their menu document.** They are always read and edited together, and the list is small.
* **The order id comes from the webshop**, which makes the consumer naturally idempotent.
* **Reports use the restaurant's timezone.** An order closed at 23:30 in Tirana counts for that day.
* **Event contracts are duplicated, not shared**, so each service can deploy on its own: enums travel as strings and
  unknown fields are ignored.
* **SSE connections and tickets are kept in memory**, which is fine for one instance. With several instances, move tickets
  to Redis and give each instance its own exclusive queue so every dashboard still gets every event.

**Production next steps:**
* a BFF (Spring Cloud Gateway + `oauth2Login` + `TokenRelay`), so browsers hold only an HttpOnly cookie (RFC 10017);
* Keycloak in production mode on PostgreSQL;
* mTLS or a service mesh between services;
* a scheduler lock for `OutboxRelay` (ShedLock) when running several instances;
* OpenAPI docs.
