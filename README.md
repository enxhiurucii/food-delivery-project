# delivery-platform-infra

Shared local infrastructure for `backoffice-service` and `webshop-service`.

| File | What |
|---|---|
| `docker-compose.yml` | MongoDB (single-node replica set), RabbitMQ, Keycloak, both services, frontend |
| `keycloak/staff-realm.json` | Realm `staff`: roles ADMIN/MANAGER/STAFF/DRIVER/CATALOG_READER, clients `backoffice-ui` (SPA, PKCE) and `webshop-service` (client credentials), demo staff |
| `keycloak/customers-realm.json` | Realm `customers`: role CUSTOMER (default for self-registered users), client `webshop-ui` (SPA, PKCE), demo customer |
| `rabbitmq/definitions.json` | vhost `delivery`, users `admin`, `backoffice`, `webshop` with least-privilege permissions |
| `nginx/nginx.conf` | Reference copy of the frontend's nginx config (the frontend image ships its own copy): `/api/backoffice`, `/api/webshop`, SSE without buffering |

## Run

```
delivery-platform/
├── (contents of this folder)
├── backoffice-service/
├── webshop-service/
└── frontend/
```

```bash
docker compose up --build
```

| Service | URL | Login |
|---|---|---|
| **App: back office** | http://localhost:3000 | `admin` / `admin123` · `napoli.manager` / `manager123` · `napoli.kitchen` / `kitchen123` |
| **App: driver** | http://localhost:3000/driver | realm `staff`: `driver1` / `driver123` |
| **App: webshop** | http://localhost:3000/webshop | realm `customers`: `customer@delivery.test` / `customer123` |
| Back-office API | http://localhost:8081 | Bearer token from realm `staff` |
| Webshop API | http://localhost:8082 | Bearer token from realm `customers` |
| Keycloak | http://localhost:8180 | `admin` / `admin` |
| RabbitMQ | http://localhost:15672 | `admin` / `admin` |

## Notes

* Keycloak runs in **dev mode** with its built-in file database, and the realms are re-imported from JSON. Production
  would use `start` mode with PostgreSQL. Keycloak doesn't support MongoDB.
* `KC_HOSTNAME=http://localhost:8180` makes every token carry the issuer that browsers see. Inside the network, the
  services fetch keys from `http://keycloak:8080/...` (`jwk-set-uri`) but still validate that public issuer.
* The demo passwords are for local use only. To change a RabbitMQ password, regenerate its `password_hash`
  (`rabbitmqctl hash_password <pwd>`) and update the matching `*_RABBIT_PASSWORD` variable.
