import { useState, type FormEvent } from "react";
import { errorMessage } from "../../lib/http";
import type { Restaurant } from "../../lib/types";
import { ErrorBanner } from "../../components/ui";
import { useBackoffice } from "../BackofficeApp";

/** "How the restaurant works": open/closed, pickup/delivery, fees, minimum order, prep time. */
export function Settings() {
  const { api, restaurant, restaurantId, reloadRestaurant } = useBackoffice();
  const { id: _id, currency: _currency, ...initial } = restaurant;
  const [form, setForm] = useState<Omit<Restaurant, "id" | "currency">>(initial);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);

  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) => {
    setSaved(false);
    setForm({ ...form, [key]: value });
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.updateRestaurant(restaurantId, form);
      setSaved(true);
      reloadRestaurant();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="narrow-page">
      <div className="page-head">
        <div>
          <h1>Restaurant settings</h1>
          <p className="muted" style={{ margin: 0 }}>Hours, delivery, and profile details.</p>
        </div>
      </div>
      <ErrorBanner message={error} onClose={() => setError(null)} />
      {saved && <div className="banner banner-ok">Saved.</div>}

      <form className="card form-grid" onSubmit={submit}>
        <h3 className="span-2">Ordering</h3>
        <label className="toggle span-2">
          <input type="checkbox" checked={form.acceptingOrders} onChange={(e) => set("acceptingOrders", e.target.checked)} />
          <span>
            <strong>Accepting orders</strong>
            <span className="muted small"> · customers can only check out while this is on</span>
          </span>
        </label>
        <label className="toggle">
          <input type="checkbox" checked={form.pickupEnabled} onChange={(e) => set("pickupEnabled", e.target.checked)} /> Pickup
        </label>
        <label className="toggle">
          <input type="checkbox" checked={form.deliveryEnabled} onChange={(e) => set("deliveryEnabled", e.target.checked)} /> Delivery
        </label>
        <label>
          Delivery fee (EUR)
          <input type="number" min="0" step="0.01" value={form.deliveryFee} onChange={(e) => set("deliveryFee", Number(e.target.value))} />
        </label>
        <label>
          Minimum order (EUR)
          <input type="number" min="0" step="0.01" value={form.minimumOrder} onChange={(e) => set("minimumOrder", Number(e.target.value))} />
        </label>
        <label>
          Preparation time (minutes)
          <input type="number" min="1" value={form.estimatedPrepMinutes} onChange={(e) => set("estimatedPrepMinutes", Number(e.target.value))} />
        </label>
        <label>
          Timezone
          <input required value={form.timezone} onChange={(e) => set("timezone", e.target.value)} />
        </label>

        <h3 className="span-2">Profile</h3>
        <label>
          Name
          <input required value={form.name} onChange={(e) => set("name", e.target.value)} />
        </label>
        <label>
          Phone
          <input value={form.phone ?? ""} onChange={(e) => set("phone", e.target.value)} />
        </label>
        <label className="span-2">
          Address
          <input required value={form.address} onChange={(e) => set("address", e.target.value)} />
        </label>
        <label className="span-2">
          Description
          <input value={form.description ?? ""} onChange={(e) => set("description", e.target.value)} />
        </label>
        <label className="span-2">
          Image URL
          <input type="url" value={form.imageUrl ?? ""} onChange={(e) => set("imageUrl", e.target.value)} />
        </label>
        <div className="span-2 row end">
          <button className="btn btn-primary" disabled={saving}>{saving ? "Saving…" : "Save settings"}</button>
        </div>
      </form>
    </section>
  );
}
