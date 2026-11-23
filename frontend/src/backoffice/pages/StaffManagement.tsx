import { useEffect, useId, useState, type FormEvent } from "react";
import { errorMessage } from "../../lib/http";
import type { Restaurant, StaffUser } from "../../lib/types";
import { ErrorBanner, Loading, Spinner } from "../../components/ui";
import { IconShield, IconTruck, IconUsers } from "../../components/icons";
import { useBackoffice } from "../BackofficeApp";

type Role = "manager" | "driver";

interface ManagerForm {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  restaurantId: string;
}

interface DriverForm {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
}

const emptyManager = (restaurantId = ""): ManagerForm => ({
  firstName: "", lastName: "", username: "", email: "", password: "", restaurantId,
});

const emptyDriver = (): DriverForm => ({
  firstName: "", lastName: "", username: "", email: "", password: "",
});

export function StaffManagement() {
  const { api } = useBackoffice();
  const formId = useId();

  const [restaurants, setRestaurants] = useState<Restaurant[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [role, setRole] = useState<Role>("manager");

  const [managerForm, setManagerForm] = useState<ManagerForm>(emptyManager());
  const [driverForm, setDriverForm] = useState<DriverForm>(emptyDriver());
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<{ role: Role; user: StaffUser } | null>(null);

  useEffect(() => {
    api.restaurants().then((all) => {
      setRestaurants(all);
      if (all.length > 0) setManagerForm((f) => f.restaurantId ? f : { ...f, restaurantId: all[0].id });
    }).catch((e) => setLoadError(errorMessage(e)));
  }, [api]);

  const setM = <K extends keyof ManagerForm>(key: K, value: ManagerForm[K]) =>
    setManagerForm((f) => ({ ...f, [key]: value }));
  const setD = <K extends keyof DriverForm>(key: K, value: DriverForm[K]) =>
    setDriverForm((f) => ({ ...f, [key]: value }));

  const switchRole = (next: Role) => {
    if (next === role) return;
    setRole(next);
    setError(null);
    setCreated(null);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setCreated(null);
    try {
      if (role === "manager") {
        const user = await api.createManager(managerForm);
        setCreated({ role: "manager", user });
        setManagerForm((f) => ({ ...emptyManager(), restaurantId: f.restaurantId }));
      } else {
        const user = await api.createDriver(driverForm);
        setCreated({ role: "driver", user });
        setDriverForm(emptyDriver());
      }
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  if (loadError) return <div className="staff-page"><ErrorBanner message={loadError} /></div>;
  if (!restaurants) return <Loading variant="page" label="Loading staff tools…" hint="Fetching restaurants" />;

  const isManager = role === "manager";
  const firstName = isManager ? managerForm.firstName : driverForm.firstName;
  const lastName = isManager ? managerForm.lastName : driverForm.lastName;
  const username = isManager ? managerForm.username : driverForm.username;
  const email = isManager ? managerForm.email : driverForm.email;
  const password = isManager ? managerForm.password : driverForm.password;

  return (
    <section className="staff-page">
      <header className="staff-hero">
        <div className="staff-hero-copy">
          <p className="staff-hero-kicker">
            <IconUsers width={18} height={18} /> Team access
          </p>
          <h1>Add staff &amp; drivers</h1>
          <p>
            Create restaurant managers or delivery drivers. Credentials are set here —
            people can change them later from their account.
          </p>
        </div>
        <div className="staff-hero-aside" aria-hidden>
          <div className="staff-hero-orb staff-hero-orb-a" />
          <div className="staff-hero-orb staff-hero-orb-b" />
          <div className="staff-hero-badge">
            <IconUsers width={28} height={28} />
          </div>
        </div>
      </header>

      <div className="staff-role-switch" role="tablist" aria-label="Account type">
        <button
          type="button"
          role="tab"
          aria-selected={isManager}
          className={`staff-role-btn${isManager ? " active" : ""}`}
          onClick={() => switchRole("manager")}
        >
          <span className="staff-role-icon" aria-hidden>
            <IconShield width={20} height={20} />
          </span>
          <span className="staff-role-copy">
            <strong>Manager</strong>
            <span>Menu, settings &amp; reports</span>
          </span>
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={!isManager}
          className={`staff-role-btn${!isManager ? " active" : ""}`}
          onClick={() => switchRole("driver")}
        >
          <span className="staff-role-icon" aria-hidden>
            <IconTruck width={20} height={20} />
          </span>
          <span className="staff-role-copy">
            <strong>Driver</strong>
            <span>Claim &amp; deliver orders</span>
          </span>
        </button>
      </div>

      <div className="staff-create card" key={role}>
        <div className="staff-create-head">
          <div className={`staff-create-mark ${isManager ? "mark-manager" : "mark-driver"}`} aria-hidden>
            {isManager ? <IconShield width={22} height={22} /> : <IconTruck width={22} height={22} />}
          </div>
          <div>
            <h2>{isManager ? "New manager" : "New driver"}</h2>
            <p className="muted small" style={{ margin: 0 }}>
              {isManager
                ? "Tied to one restaurant — they can run the day-to-day for that location."
                : "Platform-wide access — they can pick up deliveries from any restaurant."}
            </p>
          </div>
        </div>

        <ErrorBanner message={error} onClose={() => setError(null)} />

        {created && (
          <div className="staff-success" role="status">
            <div className="staff-success-check" aria-hidden>✓</div>
            <div>
              <strong>
                {created.role === "manager" ? "Manager" : "Driver"}{" "}
                {created.user.firstName} {created.user.lastName} is ready.
              </strong>
              <p>
                Username <code>{created.user.username}</code>
                {created.user.email ? <> · {created.user.email}</> : null}
              </p>
            </div>
            <button type="button" className="link staff-success-dismiss" onClick={() => setCreated(null)} aria-label="Dismiss">
              ✕
            </button>
          </div>
        )}

        <form className="staff-form" onSubmit={submit} aria-labelledby={`${formId}-title`}>
          <h2 id={`${formId}-title`} className="sr-only">
            {isManager ? "Create manager" : "Create driver"}
          </h2>

          <fieldset className="staff-fieldset">
            <legend>Profile</legend>
            <div className="staff-fields staff-fields-2">
              <label>
                First name
                <input
                  required
                  autoComplete="given-name"
                  placeholder="Alex"
                  value={firstName}
                  onChange={(e) => (isManager ? setM("firstName", e.target.value) : setD("firstName", e.target.value))}
                />
              </label>
              <label>
                Last name
                <input
                  required
                  autoComplete="family-name"
                  placeholder="Nguyen"
                  value={lastName}
                  onChange={(e) => (isManager ? setM("lastName", e.target.value) : setD("lastName", e.target.value))}
                />
              </label>
            </div>
          </fieldset>

          <fieldset className="staff-fieldset">
            <legend>Sign-in</legend>
            <div className="staff-fields">
              <label>
                Username
                <input
                  required
                  minLength={3}
                  maxLength={50}
                  autoComplete="username"
                  placeholder="alex.manager"
                  value={username}
                  onChange={(e) => (isManager ? setM("username", e.target.value) : setD("username", e.target.value))}
                />
              </label>
              <label>
                Email
                <input
                  required
                  type="email"
                  autoComplete="email"
                  placeholder="alex@restaurant.test"
                  value={email}
                  onChange={(e) => (isManager ? setM("email", e.target.value) : setD("email", e.target.value))}
                />
              </label>
              <label>
                Temporary password
                <input
                  required
                  type="password"
                  minLength={6}
                  autoComplete="new-password"
                  placeholder="At least 6 characters"
                  value={password}
                  onChange={(e) => (isManager ? setM("password", e.target.value) : setD("password", e.target.value))}
                />
              </label>
            </div>
          </fieldset>

          {isManager && (
            <fieldset className="staff-fieldset">
              <legend>Assignment</legend>
              <div className="staff-fields">
                <label>
                  Restaurant
                  <select
                    required
                    value={managerForm.restaurantId}
                    onChange={(e) => setM("restaurantId", e.target.value)}
                  >
                    {restaurants.map((r) => (
                      <option key={r.id} value={r.id}>{r.name}</option>
                    ))}
                  </select>
                </label>
              </div>
            </fieldset>
          )}

          <div className="staff-form-actions">
            <p className="muted small staff-form-hint">
              {isManager
                ? "They’ll sign in on the back office with the staff realm."
                : "They’ll sign in at /driver with the staff realm."}
            </p>
            <button className="btn btn-primary btn-lg" type="submit" disabled={saving || (isManager && restaurants.length === 0)}>
              {saving ? (
                <>
                  <Spinner size={16} /> Creating…
                </>
              ) : isManager ? (
                "Create manager"
              ) : (
                "Create driver"
              )}
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
