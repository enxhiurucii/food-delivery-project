import { useEffect, useRef, useState, type ReactNode } from "react";
import { IconBrand } from "./icons";

export function Badge({ tone = "neutral", children }: { tone?: "neutral" | "green" | "amber" | "red" | "blue"; children: ReactNode }) {
  return <span className={`badge badge-${tone === "neutral" ? "neutral" : tone}`}>{children}</span>;
}

export function ErrorBanner({ message, onClose }: { message: string | null; onClose?: () => void }) {
  if (!message) return null;
  return (
    <div className="banner banner-error" role="alert">
      <span>{message}</span>
      {onClose && (
        <button className="link" onClick={onClose} aria-label="Dismiss">
          ✕
        </button>
      )}
    </div>
  );
}

export function Spinner({ size = 22 }: { size?: number }) {
  return (
    <span className="spinner" style={{ width: size, height: size }} aria-hidden>
      <span className="spinner-ring" />
    </span>
  );
}

/** Branded loader used while waiting on auth, navigation, or backend responses. */
export function Loading({
  label = "Loading…",
  hint,
  variant = "page",
}: {
  label?: string;
  hint?: string;
  variant?: "fullscreen" | "page" | "section" | "inline";
}) {
  if (variant === "inline") {
    return (
      <span className="loading-inline" role="status" aria-live="polite">
        <Spinner size={16} />
        <span>{label}</span>
      </span>
    );
  }

  const logoSize = variant === "section" ? 40 : 52;

  return (
    <div className={`loading-screen loading-${variant}`} role="status" aria-live="polite" aria-busy="true">
      <div className="loading-card">
        <div className="loading-brand">
          <IconBrand width={logoSize} height={logoSize} />
        </div>
        <p className="loading-label">{label}</p>
        {hint ? <p className="loading-hint">{hint}</p> : null}
        <div className="loading-track" aria-hidden>
          <span className="loading-track-fill" />
        </div>
      </div>
    </div>
  );
}

/** Lightweight top progress strip for in-app waiting states. */
export function TopProgress({ active }: { active: boolean }) {
  if (!active) return null;
  return <div className="top-progress" aria-hidden />;
}

/** Shows a brief top progress bar on every client-side route change. */
export function useRouteProgress(pathname: string, activeExtra = false) {
  const [routing, setRouting] = useState(false);
  const first = useRef(true);

  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    setRouting(true);
    const timer = window.setTimeout(() => setRouting(false), 450);
    return () => window.clearTimeout(timer);
  }, [pathname]);

  return routing || activeExtra;
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>;
}

export function LiveDot({ connected }: { connected: boolean }) {
  return (
    <span className={`live ${connected ? "live-on" : "live-off"}`} title={connected ? "Live updates connected" : "Reconnecting…"}>
      <span className="live-dot" /> {connected ? "Live" : "Reconnecting"}
    </span>
  );
}

export function Modal({
  open,
  title,
  onClose,
  children,
  wide,
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  wide?: boolean;
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="modal-root" role="presentation">
      <button type="button" className="modal-backdrop" aria-label="Close dialog" onClick={onClose} />
      <div className={`modal-panel${wide ? " modal-panel-wide" : ""}`} role="dialog" aria-modal="true" aria-label={title}>
        <header className="modal-head">
          <h2>{title}</h2>
          <button type="button" className="btn-icon modal-close" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </header>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}

export function ConfirmModal({
  open,
  title,
  message,
  confirmLabel = "Delete",
  cancelLabel = "Cancel",
  busy,
  onConfirm,
  onClose,
}: {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  busy?: boolean;
  onConfirm: () => void;
  onClose: () => void;
}) {
  return (
    <Modal open={open} title={title} onClose={busy ? () => undefined : onClose}>
      <p className="modal-message">{message}</p>
      <div className="modal-actions">
        <button type="button" className="btn" disabled={busy} onClick={onClose}>
          {cancelLabel}
        </button>
        <button type="button" className="btn btn-danger" disabled={busy} onClick={onConfirm}>
          {busy ? (
            <>
              <Spinner size={16} /> Working…
            </>
          ) : (
            confirmLabel
          )}
        </button>
      </div>
    </Modal>
  );
}
