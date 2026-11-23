import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { AuthProvider, useAuth } from "react-oidc-context";
import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { oidcConfig } from "../lib/config";
import { ApiError, errorMessage } from "../lib/http";
import type { Cart } from "../lib/types";
import {
  IconBrand,
  IconCart,
  IconHome,
  IconOrders,
  IconSearch,
  IconUser,
} from "../components/icons";
import { Loading, TopProgress, useRouteProgress } from "../components/ui";
import { webshopApi, type WebshopApi } from "./api";
import { RestaurantList } from "./pages/RestaurantList";
import { RestaurantPage } from "./pages/RestaurantPage";
import { CheckoutPage } from "./pages/CheckoutPage";
import { OrderTracking } from "./pages/OrderTracking";
import { MyOrders } from "./pages/MyOrders";

const customerOidc = oidcConfig("customers", "webshop-ui", "/webshop");

export function WebshopApp() {
  return (
    <AuthProvider {...customerOidc}>
      <Shop />
    </AuthProvider>
  );
}

interface ShopContextValue {
  api: WebshopApi;
  isLoggedIn: boolean;
  login: (returnTo?: string) => void;
  cart: Cart | null;
  refreshCart: () => Promise<void>;
  setCart: (cart: Cart | null) => void;
  /** Adds an item; if the cart holds another restaurant's items, offers to start a new cart. */
  addToCart: (restaurantId: string, menuItemId: string) => Promise<void>;
  searchQuery: string;
  setSearchQuery: (q: string) => void;
}

const ShopContext = createContext<ShopContextValue | null>(null);

export function useShop(): ShopContextValue {
  const ctx = useContext(ShopContext);
  if (!ctx) throw new Error("useShop outside WebshopApp");
  return ctx;
}

function Shop() {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const tokenRef = useRef<string | undefined>(undefined);
  tokenRef.current = auth.user?.access_token;
  const api = useMemo(() => webshopApi(() => tokenRef.current), []);
  const [cart, setCart] = useState<Cart | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const searchRef = useRef<HTMLInputElement>(null);

  const login = useCallback(
    (returnTo?: string) => void auth.signinRedirect({ state: returnTo ?? window.location.pathname }),
    [auth],
  );

  const refreshCart = useCallback(async () => {
    if (!tokenRef.current) return setCart(null);
    setCart(await api.cart());
  }, [api]);

  useEffect(() => {
    if (auth.isAuthenticated) void refreshCart().catch(() => undefined);
    else setCart(null);
  }, [auth.isAuthenticated, refreshCart]);

  const addToCart = useCallback(
    async (restaurantId: string, menuItemId: string) => {
      if (!auth.isAuthenticated) return login(location.pathname);
      try {
        setCart(await api.addToCart(restaurantId, menuItemId));
      } catch (e) {
        if (e instanceof ApiError && e.code === "CART_RESTAURANT_MISMATCH") {
          if (window.confirm("Your cart has items from another restaurant. Empty it and start a new order here?")) {
            await api.clearCart();
            setCart(await api.addToCart(restaurantId, menuItemId));
          }
          return;
        }
        window.alert(errorMessage(e));
      }
    },
    [api, auth.isAuthenticated, login, location.pathname],
  );

  const itemCount = cart?.items.reduce((sum, i) => sum + i.quantity, 0) ?? 0;
  const displayName = auth.user?.profile.given_name ?? auth.user?.profile.preferred_username ?? "Guest";
  const showProgress = useRouteProgress(location.pathname, auth.isLoading);
  const value: ShopContextValue = {
    api,
    isLoggedIn: auth.isAuthenticated,
    login,
    cart,
    refreshCart,
    setCart,
    addToCart,
    searchQuery,
    setSearchQuery,
  };

  if (auth.isLoading) {
    return (
      <>
        <TopProgress active />
        <Loading variant="fullscreen" label="Warming up TasteHouse…" hint="Checking your session" />
      </>
    );
  }

  return (
    <ShopContext.Provider value={value}>
      <TopProgress active={showProgress} />
      <div className="app shop">
        <aside className="side-nav" aria-label="Main">
          <Link to="/webshop" className="side-nav-brand" aria-label="TasteHouse home">
            <IconBrand />
          </Link>
          <NavLink to="/webshop" end className={({ isActive }) => `side-link${isActive ? " active" : ""}`} title="Home">
            <IconHome />
          </NavLink>
          <button
            type="button"
            className="side-link"
            title="Search"
            onClick={() => {
              if (location.pathname !== "/webshop") navigate("/webshop");
              queueMicrotask(() => searchRef.current?.focus());
            }}
          >
            <IconSearch />
          </button>
          {auth.isAuthenticated && (
            <NavLink to="/webshop/orders" className={({ isActive }) => `side-link${isActive ? " active" : ""}`} title="My orders">
              <IconOrders />
            </NavLink>
          )}
          <NavLink to="/webshop/checkout" className={({ isActive }) => `side-link${isActive ? " active" : ""}`} title="Cart">
            <IconCart />
          </NavLink>
          <div className="side-spacer" />
          <button
            type="button"
            className="side-link"
            title={auth.isAuthenticated ? "Account" : "Log in"}
            onClick={() => (auth.isAuthenticated ? undefined : login())}
          >
            <IconUser />
          </button>
        </aside>

        <div className="shell-main">
          <header className="topbar">
            <div className="topbar-left">
              <Link to="/webshop" className="brand-lockup">
                Taste<span>House</span>
              </Link>
            </div>
            <div className="search-bar" role="search">
              <IconSearch width={18} height={18} aria-hidden />
              <input
                ref={searchRef}
                type="search"
                placeholder="Search for restaurants or dishes…"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                aria-label="Search restaurants or dishes"
              />
            </div>
            <div className="topbar-right">
              {auth.isAuthenticated && (
                <Link to="/webshop/orders" className="btn-icon" title="My orders" aria-label="My orders">
                  <IconOrders width={18} height={18} />
                </Link>
              )}
              <Link to="/webshop/checkout" className="cart-button" aria-label="Cart">
                <IconCart width={18} height={18} />
                Cart
                {itemCount > 0 && <span className="cart-count">{itemCount}</span>}
              </Link>
              {auth.isAuthenticated ? (
                <>
                  <span className="user-chip">
                    <span className="user-avatar">{String(displayName).slice(0, 1).toUpperCase()}</span>
                    {displayName}
                  </span>
                  <button className="btn btn-sm" onClick={() => void auth.signoutRedirect()}>
                    Log out
                  </button>
                </>
              ) : (
                <button className="btn btn-primary btn-sm" onClick={() => login()}>
                  Log in / Sign up
                </button>
              )}
            </div>
          </header>

          <main className="content">
            <Routes>
              <Route index element={<RestaurantList />} />
              <Route path="restaurants/:restaurantId" element={<RestaurantPage />} />
              <Route path="checkout" element={<CheckoutPage />} />
              <Route path="orders" element={<MyOrders />} />
              <Route path="orders/:orderId" element={<OrderTracking />} />
              <Route path="*" element={<Navigate to="/webshop" replace />} />
            </Routes>
          </main>

          <footer className="footer">
            <div className="footer-grid">
              <div>
                <div className="brand-lockup" style={{ marginBottom: "0.5rem" }}>
                  <IconBrand width={24} height={24} />
                  Taste<span>House</span>
                </div>
                <p className="muted small">Delicious food delivered fast across Tirana. Order pickup or delivery from your favorite kitchens.</p>
                <div className="footer-social" aria-hidden>
                  <span>in</span>
                  <span>ig</span>
                  <span>x</span>
                </div>
              </div>
              <div>
                <h4>Company</h4>
                <Link to="/webshop">Restaurants</Link>
                <Link to="/">Back office</Link>
                <a href="#features">About</a>
              </div>
              <div>
                <h4>Support</h4>
                <a href="#features">Help center</a>
                <Link to="/webshop/orders">Track order</Link>
                <a href="#partners">Partners</a>
              </div>
              <div>
                <h4>Legal</h4>
                <a href="#partners">Privacy</a>
                <a href="#partners">Terms</a>
                <a href="#partners">Cookies</a>
              </div>
              <div>
                <h4>Newsletter</h4>
                <p className="muted small">Get deals and new restaurants in your inbox.</p>
                <form
                  className="newsletter"
                  onSubmit={(e) => {
                    e.preventDefault();
                    window.alert("Thanks! Newsletter signup is a demo only.");
                  }}
                >
                  <input type="email" required placeholder="Email address" aria-label="Email" />
                  <button className="btn btn-primary btn-sm" type="submit">
                    Subscribe
                  </button>
                </form>
              </div>
            </div>
            <p className="footer-note">
              Demo customer: <code>customer@delivery.test</code> / <code>customer123</code>
            </p>
          </footer>
        </div>
      </div>
    </ShopContext.Provider>
  );
}

/** Renders children only for logged-in customers; otherwise a login prompt. */
export function RequireLogin({ children, message }: { children: React.ReactNode; message: string }) {
  const auth = useAuth();
  const { login } = useShop();
  if (auth.isLoading) {
    return <Loading variant="page" label="Checking your account…" hint="Hang tight for a moment" />;
  }
  if (!auth.isAuthenticated) {
    return (
      <div className="card narrow center-text">
        <p>{message}</p>
        <button className="btn btn-primary" onClick={() => login()}>
          Log in / Sign up
        </button>
      </div>
    );
  }
  return <>{children}</>;
}
