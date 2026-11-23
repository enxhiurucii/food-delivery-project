import { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";
import { AuthProvider, useAuth } from "react-oidc-context";
import { Link, NavLink, Navigate, Route, Routes } from "react-router-dom";
import { oidcConfig } from "../lib/config";
import { errorMessage } from "../lib/http";
import { backofficeApi } from "../backoffice/api";
import { driverApi, type DriverApi } from "./api";
import type { Me } from "../lib/types";
import { ErrorBanner, Loading } from "../components/ui";
import { IconUser } from "../components/icons";
import { AvailableDeliveries } from "./pages/AvailableDeliveries";
import { ActiveDelivery } from "./pages/ActiveDelivery";

const driverOidc = oidcConfig("staff", "backoffice-ui", "/driver");

interface DriverContextValue {
  api: DriverApi;
  me: Me;
}
const DriverContext = createContext<DriverContextValue | null>(null);

export function useDriver(): DriverContextValue {
  const ctx = useContext(DriverContext);
  if (!ctx) throw new Error("useDriver outside DriverApp");
  return ctx;
}

export function DriverApp() {
  return (
    <AuthProvider {...driverOidc}>
      <DriverGate />
    </AuthProvider>
  );
}

function DriverGate() {
  const auth = useAuth();

  if (auth.isLoading) {
    return <Loading variant="fullscreen" label="Signing you in…" />;
  }
  if (auth.error) {
    return (
      <div className="login-shell">
        <div className="center card narrow">
          <h2>Login problem</h2>
          <p className="muted">{auth.error.message}</p>
          <button className="btn" onClick={() => void auth.signinRedirect()}>Try again</button>
        </div>
      </div>
    );
  }
  if (!auth.isAuthenticated) {
    void auth.signinRedirect();
    return <Loading variant="fullscreen" label="Redirecting to login…" />;
  }
  return <DriverWorkspace />;
}

function DriverWorkspace() {
  const auth = useAuth();
  const tokenRef = useRef(auth.user!.access_token);
  tokenRef.current = auth.user!.access_token;

  const boApi = useMemo(() => backofficeApi(() => tokenRef.current), []);
  const api = useMemo(() => driverApi(() => tokenRef.current), []);

  const [me, setMe] = useState<Me | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    boApi.me().then(setMe).catch((e) => setError(errorMessage(e)));
  }, [boApi]);

  if (error) {
    return (
      <div className="login-shell">
        <div className="center narrow"><ErrorBanner message={error} /></div>
      </div>
    );
  }
  if (!me) {
    return <Loading variant="fullscreen" label="Loading your profile…" />;
  }
  if (!me.roles.includes("DRIVER")) {
    return (
      <div className="login-shell">
        <div className="center card narrow">
          <h2>Not a driver account</h2>
          <p className="muted">Your account ({me.username}) does not have driver access.</p>
          <button className="btn" onClick={() => void auth.signoutRedirect()}>Log out</button>
        </div>
      </div>
    );
  }

  const displayName = me.name ?? me.username;

  return (
    <DriverContext.Provider value={{ api, me }}>
      <div className="app driver-app">
        <div className="shell-main">
          <header className="topbar topbar-dark">
            <div className="topbar-left">
              <Link to="/driver" className="brand-lockup">Taste<span>House</span> <span className="muted small">· Driver</span></Link>
            </div>
            <nav className="tabs tabs-light">
              <NavLink to="/driver/available">Available</NavLink>
              <NavLink to="/driver/active">Active delivery</NavLink>
            </nav>
            <div className="topbar-right">
              <span className="user-chip">
                <span className="user-avatar"><IconUser width={14} height={14} /></span>
                {displayName}
              </span>
              <button className="btn btn-sm" onClick={() => void auth.signoutRedirect()}>Log out</button>
            </div>
          </header>
          <main className="content driver-content">
            <Routes>
              <Route path="/" element={<Navigate to="/driver/available" replace />} />
              <Route path="/available" element={<AvailableDeliveries />} />
              <Route path="/active" element={<ActiveDelivery />} />
              <Route path="*" element={<Navigate to="/driver/available" replace />} />
            </Routes>
          </main>
        </div>
      </div>
    </DriverContext.Provider>
  );
}
