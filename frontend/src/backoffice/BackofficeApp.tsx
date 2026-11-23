import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { AuthProvider, useAuth } from "react-oidc-context";
import { Link, NavLink, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { oidcConfig } from "../lib/config";
import { errorMessage } from "../lib/http";
import type { Me, Restaurant } from "../lib/types";
import { ErrorBanner, Loading, Modal, TopProgress, useRouteProgress } from "../components/ui";
import {
  IconBrand,
  IconChart,
  IconHome,
  IconMenu,
  IconOrders,
  IconSettings,
  IconUser,
  IconUsers,
} from "../components/icons";
import { backofficeApi, type BackofficeApi } from "./api";
import { OrdersBoard } from "./pages/OrdersBoard";
import { MenuEditor } from "./pages/MenuEditor";
import { Settings } from "./pages/Settings";
import { Reports } from "./pages/Reports";
import { StaffManagement } from "./pages/StaffManagement";

const staffOidc = oidcConfig("staff", "backoffice-ui", "/");

export function BackofficeApp() {
  return (
    <AuthProvider {...staffOidc}>
      <Gate />
    </AuthProvider>
  );
}

interface BackofficeContextValue {
  api: BackofficeApi;
  me: Me;
  restaurant: Restaurant;
  restaurantId: string;
  isManager: boolean;
  isAdmin: boolean;
  reloadRestaurant: () => void;
  /** Always the latest token (it is renewed in the background). */
  getToken: () => string;
}

const BackofficeContext = createContext<BackofficeContextValue | null>(null);

export function useBackoffice(): BackofficeContextValue {
  const ctx = useContext(BackofficeContext);
  if (!ctx) throw new Error("useBackoffice outside BackofficeApp");
  return ctx;
}

function Gate() {
  const auth = useAuth();

  if (auth.isLoading) {
    return (
      <>
        <TopProgress active />
        <Loading variant="fullscreen" label="Signing you in…" hint="Connecting securely to TasteHouse" />
      </>
    );
  }
  if (auth.error) {
    return (
      <div className="login-shell">
        <div className="center card narrow">
          <h2>Login problem</h2>
          <p className="muted">{auth.error.message}</p>
          <p className="muted small">Is Keycloak running on http://localhost:8180?</p>
          <button className="btn" onClick={() => void auth.signinRedirect()}>
            Try again
          </button>
        </div>
      </div>
    );
  }
  if (!auth.isAuthenticated) {
    return (
      <div className="login-shell">
        <div className="login-split-container">
          <div className="login-brand-row">
            <IconBrand width={40} height={40} />
            <span className="login-brand-name">TasteHouse</span>
          </div>
          <p className="muted" style={{ textAlign: "center", marginBottom: "2rem" }}>
            Choose how you want to sign in
          </p>
          <div className="login-split">
            <div className="login-split-card">
              <div className="login-split-icon">🍽️</div>
              <h2>Restaurant staff</h2>
              <p className="muted small">Manage orders, menus, and daily revenue for your restaurant.</p>
              <button
                className="btn btn-primary btn-block"
                onClick={() => void auth.signinRedirect({ state: window.location.pathname })}
              >
                Log in as restaurant
              </button>
              <p className="muted small hint" style={{ marginTop: "0.75rem" }}>
                Admin: <code>admin</code> / <code>admin123</code>
                <br />
                Manager: <code>napoli.manager</code> / <code>manager123</code>
                <br />
                Staff: <code>napoli.kitchen</code> / <code>kitchen123</code>
              </p>
            </div>
            <div className="login-split-divider">or</div>
            <div className="login-split-card">
              <div className="login-split-icon">🛵</div>
              <h2>Driver</h2>
              <p className="muted small">Pick up and deliver orders to customers in real time.</p>
              <Link to="/driver" className="btn btn-outline btn-block">
                Log in as driver
              </Link>
              <p className="muted small hint" style={{ marginTop: "0.75rem" }}>
                Demo: <code>driver1</code> / <code>driver123</code>
              </p>
            </div>
          </div>
          <p className="small" style={{ textAlign: "center", marginTop: "1.5rem" }}>
            Hungry? <Link to="/webshop">Go to the webshop →</Link>
          </p>
        </div>
      </div>
    );
  }
  return <Workspace />;
}

interface NewRestaurantForm {
  name: string; description: string; address: string; phone: string; imageUrl: string;
  timezone: string; acceptingOrders: boolean; pickupEnabled: boolean; deliveryEnabled: boolean;
  deliveryFee: number; minimumOrder: number; estimatedPrepMinutes: number;
}

const emptyRestaurantForm = (): NewRestaurantForm => ({
  name: "", description: "", address: "", phone: "", imageUrl: "",
  timezone: "Europe/Tirane", acceptingOrders: false,
  pickupEnabled: true, deliveryEnabled: true,
  deliveryFee: 0, minimumOrder: 0, estimatedPrepMinutes: 20,
});

const NEW_RESTAURANT = "__new__";

function Workspace() {
  const auth = useAuth();
  const location = useLocation();
  const tokenRef = useRef(auth.user!.access_token);
  tokenRef.current = auth.user!.access_token;
  const getToken = () => tokenRef.current;
  const api = useMemo(() => backofficeApi(() => tokenRef.current), []);

  const [me, setMe] = useState<Me | null>(null);
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [restaurant, setRestaurant] = useState<Restaurant | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const [newRestaurantOpen, setNewRestaurantOpen] = useState(false);
  const [newRestaurantForm, setNewRestaurantForm] = useState<NewRestaurantForm>(emptyRestaurantForm());
  const [newRestaurantSaving, setNewRestaurantSaving] = useState(false);
  const [newRestaurantError, setNewRestaurantError] = useState<string | null>(null);

  const setF = useCallback(<K extends keyof NewRestaurantForm>(key: K, value: NewRestaurantForm[K]) =>
    setNewRestaurantForm((f) => ({ ...f, [key]: value })), []);

  const submitNewRestaurant = async (e: FormEvent) => {
    e.preventDefault();
    setNewRestaurantSaving(true);
    setNewRestaurantError(null);
    try {
      const created = await api.createRestaurant(newRestaurantForm);
      const all = await api.restaurants();
      setRestaurants(all);
      setSelectedId(created.id);
      setNewRestaurantOpen(false);
      setNewRestaurantForm(emptyRestaurantForm());
    } catch (err) {
      setNewRestaurantError(errorMessage(err));
    } finally {
      setNewRestaurantSaving(false);
    }
  };
  const bootstrapping = !error && (!me || (!!selectedId && !restaurant));
  const showProgress = useRouteProgress(location.pathname, bootstrapping);

  useEffect(() => {
    api
      .me()
      .then(async (profile) => {
        setMe(profile);
        if (profile.restaurantId) {
          setSelectedId(profile.restaurantId);
        } else if (profile.roles.includes("ADMIN")) {
          const all = await api.restaurants();
          setRestaurants(all);
          setSelectedId(all[0]?.id ?? null);
        }
      })
      .catch((e) => setError(errorMessage(e)));
  }, [api]);

  useEffect(() => {
    if (!selectedId) return;
    api.restaurant(selectedId).then(setRestaurant).catch((e) => setError(errorMessage(e)));
  }, [api, selectedId, reloadKey]);

  useEffect(() => {
    setRestaurant(null);
  }, [selectedId]);

  if (error) {
    return (
      <div className="login-shell">
        <div className="center narrow">
          <ErrorBanner message={error} />
        </div>
      </div>
    );
  }
  if (!me) {
    return (
      <>
        <TopProgress active />
        <Loading variant="fullscreen" label="Loading your workspace…" hint="Fetching your restaurant profile" />
      </>
    );
  }
  if (!selectedId) {
    return (
      <div className="login-shell">
        <div className="center card narrow">
          <h2>No restaurant assigned</h2>
          <p className="muted">
            Your account ({me.username}) is not linked to a restaurant yet. Ask an admin to assign you.
          </p>
          <button className="btn" onClick={() => void auth.signoutRedirect()}>
            Log out
          </button>
        </div>
      </div>
    );
  }
  if (!restaurant) {
    return (
      <>
        <TopProgress active />
        <Loading variant="fullscreen" label="Opening restaurant…" hint="Almost ready" />
      </>
    );
  }

  const isManager = me.roles.includes("MANAGER") || me.roles.includes("ADMIN");
  const isAdmin = me.roles.includes("ADMIN");
  const value: BackofficeContextValue = {
    api,
    me,
    restaurant,
    restaurantId: selectedId,
    isManager,
    isAdmin,
    reloadRestaurant: () => setReloadKey((k) => k + 1),
    getToken,
  };
  const displayName = me.name ?? me.username;

  return (
    <BackofficeContext.Provider value={value}>
      <TopProgress active={showProgress} />
      <div className="app">
        <aside className="side-nav" aria-label="Back office">
          <Link to="/orders" className="side-nav-brand" aria-label="TasteHouse back office">
            <IconBrand />
          </Link>
          <NavLink to="/orders" className={({ isActive }) => `side-link${isActive ? " active" : ""}`} title="Orders">
            <IconOrders />
          </NavLink>
          <NavLink to="/menu" className={({ isActive }) => `side-link${isActive ? " active" : ""}`} title="Menu">
            <IconMenu />
          </NavLink>
          {isManager && (
            <NavLink to="/reports" className={({ isActive }) => `side-link${isActive ? " active" : ""}`} title="Reports">
              <IconChart />
            </NavLink>
          )}
          {isManager && (
            <NavLink to="/settings" className={({ isActive }) => `side-link${isActive ? " active" : ""}`} title="Settings">
              <IconSettings />
            </NavLink>
          )}
          {isAdmin && (
            <NavLink to="/staff" className={({ isActive }) => `side-link${isActive ? " active" : ""}`} title="Staff">
              <IconUsers />
            </NavLink>
          )}
          <div className="side-spacer" />
          <Link to="/webshop" className="side-link" title="Webshop">
            <IconHome />
          </Link>
        </aside>

        <div className="shell-main">
          <header className="topbar topbar-dark">
            <div className="topbar-left">
              <Link to="/orders" className="brand-lockup">
                Taste<span>House</span>
              </Link>
              {isAdmin ? (
                <select
                  className="select-soft"
                  value={selectedId ?? ""}
                  onChange={(e) => {
                    if (e.target.value === NEW_RESTAURANT) setNewRestaurantOpen(true);
                    else setSelectedId(e.target.value);
                  }}
                >
                  {restaurants.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                  <option value={NEW_RESTAURANT}>＋ Add new restaurant…</option>
                </select>
              ) : restaurants.length > 1 ? (
                <select className="select-soft" value={selectedId ?? ""} onChange={(e) => setSelectedId(e.target.value)}>
                  {restaurants.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                </select>
              ) : (
                <span className="restaurant-name">{restaurant.name}</span>
              )}
              <span className={`pill ${restaurant.acceptingOrders ? "pill-green" : "pill-red"}`}>
                {restaurant.acceptingOrders ? "Open" : "Closed"}
              </span>
            </div>
            <nav className="tabs tabs-light">
              <NavLink to="/orders">Orders</NavLink>
              <NavLink to="/menu">Menu</NavLink>
              {isManager && <NavLink to="/reports">Reports</NavLink>}
              {isManager && <NavLink to="/settings">Settings</NavLink>}
              {isAdmin && <NavLink to="/staff">Staff</NavLink>}
            </nav>
            <div className="topbar-right">
              <span className="user-chip">
                <span className="user-avatar">
                  <IconUser width={14} height={14} />
                </span>
                {displayName}
              </span>
              <button className="btn btn-sm" onClick={() => void auth.signoutRedirect()}>
                Log out
              </button>
            </div>
          </header>
          <main className="content" key={selectedId}>
            <Routes>
              <Route path="/" element={<Navigate to="/orders" replace />} />
              <Route path="/orders" element={<OrdersBoard />} />
              <Route path="/menu" element={<MenuEditor />} />
              <Route path="/reports" element={isManager ? <Reports /> : <Navigate to="/orders" />} />
              <Route path="/settings" element={isManager ? <Settings /> : <Navigate to="/orders" />} />
              <Route path="/staff" element={isAdmin ? <StaffManagement /> : <Navigate to="/orders" />} />
              <Route path="*" element={<Navigate to="/orders" replace />} />
            </Routes>
          </main>
        </div>
      </div>
      <Modal open={newRestaurantOpen} title="Add new restaurant" wide
        onClose={() => { if (!newRestaurantSaving) { setNewRestaurantOpen(false); setNewRestaurantForm(emptyRestaurantForm()); setNewRestaurantError(null); } }}>
        <ErrorBanner message={newRestaurantError} onClose={() => setNewRestaurantError(null)} />
        <form className="form-grid" onSubmit={submitNewRestaurant}>
          <label className="span-2">
            Name
            <input required value={newRestaurantForm.name} onChange={(e) => setF("name", e.target.value)} />
          </label>
          <label className="span-2">
            Description
            <input value={newRestaurantForm.description} onChange={(e) => setF("description", e.target.value)} />
          </label>
          <label className="span-2">
            Address
            <input required value={newRestaurantForm.address} onChange={(e) => setF("address", e.target.value)} />
          </label>
          <label>
            Phone
            <input value={newRestaurantForm.phone} onChange={(e) => setF("phone", e.target.value)} />
          </label>
          <label>
            Timezone
            <input required value={newRestaurantForm.timezone} onChange={(e) => setF("timezone", e.target.value)} />
          </label>
          <label>
            Delivery fee (EUR)
            <input type="number" min="0" step="0.01" value={newRestaurantForm.deliveryFee}
              onChange={(e) => setF("deliveryFee", Number(e.target.value))} />
          </label>
          <label>
            Minimum order (EUR)
            <input type="number" min="0" step="0.01" value={newRestaurantForm.minimumOrder}
              onChange={(e) => setF("minimumOrder", Number(e.target.value))} />
          </label>
          <label>
            Prep time (minutes)
            <input type="number" min="1" value={newRestaurantForm.estimatedPrepMinutes}
              onChange={(e) => setF("estimatedPrepMinutes", Number(e.target.value))} />
          </label>
          <label>
            Image URL
            <input value={newRestaurantForm.imageUrl} onChange={(e) => setF("imageUrl", e.target.value)} />
          </label>
          <div className="span-2" style={{ display: "flex", gap: "1.5rem", flexWrap: "wrap" }}>
            <label className="toggle">
              <input type="checkbox" checked={newRestaurantForm.pickupEnabled}
                onChange={(e) => setF("pickupEnabled", e.target.checked)} /> Pickup enabled
            </label>
            <label className="toggle">
              <input type="checkbox" checked={newRestaurantForm.deliveryEnabled}
                onChange={(e) => setF("deliveryEnabled", e.target.checked)} /> Delivery enabled
            </label>
            <label className="toggle">
              <input type="checkbox" checked={newRestaurantForm.acceptingOrders}
                onChange={(e) => setF("acceptingOrders", e.target.checked)} /> Open immediately
            </label>
          </div>
          <div className="modal-actions span-2">
            <button type="button" className="btn" disabled={newRestaurantSaving}
              onClick={() => { setNewRestaurantOpen(false); setNewRestaurantForm(emptyRestaurantForm()); setNewRestaurantError(null); }}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={newRestaurantSaving}>
              {newRestaurantSaving ? "Creating…" : "Create restaurant"}
            </button>
          </div>
        </form>
      </Modal>
    </BackofficeContext.Provider>
  );
}
