import { InMemoryWebStorage, WebStorageStateStore, type User } from "oidc-client-ts";
import type { AuthProviderProps } from "react-oidc-context";

/** Keycloak's public URL (the issuer the services validate). Override at build time with VITE_KEYCLOAK_URL. */
export const KEYCLOAK_URL: string = import.meta.env.VITE_KEYCLOAK_URL ?? "http://localhost:8180";

/** Same-origin API prefixes: nginx (docker) or the Vite proxy (dev) forward them to the services. */
export const BACKOFFICE_API = "/api/backoffice";
export const WEBSHOP_API = "/api/webshop";

/**
 * OIDC settings for a public SPA client (Authorization Code + PKCE).
 * Tokens are kept in memory, never in localStorage; on reload Keycloak's SSO cookie signs the user back in.
 */
export function oidcConfig(realm: string, clientId: string, basePath: string): AuthProviderProps {
  const redirect = `${window.location.origin}${basePath}`;
  return {
    authority: `${KEYCLOAK_URL}/realms/${realm}`,
    client_id: clientId,
    redirect_uri: redirect,
    post_logout_redirect_uri: redirect,
    scope: "openid profile email",
    automaticSilentRenew: true,
    userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
    onSigninCallback: (user: User | void) => {
      // Return to the page the user was on before logging in (passed as OIDC state), and drop ?code=&state=.
      const returnTo = (user && typeof user.state === "string" && user.state) || window.location.pathname;
      window.history.replaceState({}, document.title, returnTo);
      window.dispatchEvent(new PopStateEvent("popstate"));
    },
  };
}
