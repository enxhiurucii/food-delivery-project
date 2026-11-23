import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { errorMessage } from "../../lib/http";
import { money } from "../../lib/format";
import type { Menu, MenuItem } from "../../lib/types";
import { Badge, ConfirmModal, Empty, ErrorBanner, Loading, Modal, Spinner } from "../../components/ui";
import { IconMenu, IconPlus } from "../../components/icons";
import { useBackoffice } from "../BackofficeApp";

type ItemDraft = Omit<MenuItem, "id" | "currency">;
const EMPTY_ITEM: ItemDraft = { name: "", description: "", price: 0, imageUrl: "", available: true };

type DishModalState = { menuId: string; itemId: string | null };
type DeleteTarget =
  | { kind: "section"; menuId: string; name: string }
  | { kind: "dish"; menuId: string; itemId: string; name: string };

/** Simple menu editor: menus (e.g. "Lunch menu") with items (image, name, description, price). */
export function MenuEditor() {
  const { api, restaurantId, isManager, restaurant } = useBackoffice();
  const [menus, setMenus] = useState<Menu[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [newMenuName, setNewMenuName] = useState("");
  const [dishModal, setDishModal] = useState<DishModalState | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<DeleteTarget | null>(null);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(() => {
    api.menus(restaurantId).then(setMenus).catch((e) => setError(errorMessage(e)));
  }, [api, restaurantId]);

  useEffect(load, [load]);

  const run = async (action: () => Promise<unknown>) => {
    try {
      await action();
      setDishModal(null);
      load();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const addMenu = (e: FormEvent) => {
    e.preventDefault();
    if (!newMenuName.trim()) return;
    void run(() => api.createMenu(restaurantId, { name: newMenuName.trim(), position: (menus?.length ?? 0) + 1 })).then(() =>
      setNewMenuName(""),
    );
  };

  const editingItem = useMemo(() => {
    if (!menus || !dishModal?.itemId) return null;
    const menu = menus.find((m) => m.id === dishModal.menuId);
    return menu?.items.find((i) => i.id === dishModal.itemId) ?? null;
  }, [menus, dishModal]);

  const editingMenuName = useMemo(() => {
    if (!menus || !dishModal) return "";
    return menus.find((m) => m.id === dishModal.menuId)?.name ?? "";
  }, [menus, dishModal]);

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      if (deleteTarget.kind === "section") {
        await api.deleteMenu(restaurantId, deleteTarget.menuId);
      } else {
        await api.deleteItem(restaurantId, deleteTarget.menuId, deleteTarget.itemId);
      }
      setDeleteTarget(null);
      load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setDeleting(false);
    }
  };

  if (!menus) return <Loading variant="page" label="Loading menu studio…" hint="Fetching sections and dishes" />;

  const totalItems = menus.reduce((sum, m) => sum + m.items.length, 0);
  const availableItems = menus.reduce((sum, m) => sum + m.items.filter((i) => i.available).length, 0);
  const dishModalOpen = dishModal !== null && (dishModal.itemId === null || editingItem !== null);

  return (
    <section className="menu-page">
      <div className="menu-hero">
        <div className="menu-hero-copy">
          <div className="menu-hero-kicker">
            <IconMenu width={18} height={18} />
            Menu studio
          </div>
          <h1>Shape your menu</h1>
          <p className="muted">
            Build sections and dishes for <strong>{restaurant.name}</strong>. Customers see these items instantly in the webshop.
          </p>
          <div className="menu-stats">
            <div className="menu-stat">
              <strong>{menus.length}</strong>
              <span>Sections</span>
            </div>
            <div className="menu-stat">
              <strong>{totalItems}</strong>
              <span>Dishes</span>
            </div>
            <div className="menu-stat">
              <strong>{availableItems}</strong>
              <span>Available</span>
            </div>
          </div>
        </div>
        {!isManager && (
          <div className="menu-hero-aside">
            <Badge tone="amber">Read only — managers can edit</Badge>
          </div>
        )}
      </div>

      <ErrorBanner message={error} onClose={() => setError(null)} />

      {isManager && (
        <form className="menu-create card" onSubmit={addMenu}>
          <div className="menu-create-copy">
            <h3>New menu section</h3>
            <p className="muted small">Group dishes like “Lunch”, “Mains”, or “Drinks”.</p>
          </div>
          <div className="menu-create-fields">
            <input
              placeholder='e.g. “Lunch menu”'
              value={newMenuName}
              onChange={(e) => setNewMenuName(e.target.value)}
              aria-label="New menu name"
            />
            <button className="btn btn-primary">
              <IconPlus width={16} height={16} /> Add section
            </button>
          </div>
        </form>
      )}

      {menus.length === 0 && (
        <Empty>
          No menus yet.
          {isManager ? " Create your first section above." : ""}
        </Empty>
      )}

      <div className="menu-sections">
        {menus.map((menu) => (
          <div key={menu.id} className="card menu-block">
            <div className="menu-block-head">
              <div>
                <h2>{menu.name}</h2>
                <p className="muted small" style={{ margin: 0 }}>
                  {menu.items.length === 0
                    ? "Empty section — add your first dish"
                    : `${menu.items.length} dish${menu.items.length === 1 ? "" : "es"} · ${menu.items.filter((i) => i.available).length} available`}
                </p>
              </div>
              {isManager && (
                <div className="row gap-sm">
                  <button className="btn btn-primary btn-sm" onClick={() => setDishModal({ menuId: menu.id, itemId: null })}>
                    <IconPlus width={14} height={14} /> Add dish
                  </button>
                  <button
                    className="btn btn-sm btn-danger-ghost"
                    onClick={() => setDeleteTarget({ kind: "section", menuId: menu.id, name: menu.name })}
                  >
                    Delete section
                  </button>
                </div>
              )}
            </div>

            {menu.items.length === 0 && <div className="empty menu-empty">No dishes in this section yet.</div>}

            <div className="menu-item-grid">
              {menu.items.map((item) => (
                <article key={item.id} className={`menu-dish-card ${item.available ? "" : "item-unavailable"}`}>
                  <div className="menu-dish-media">
                    {!item.available && (
                      <span className="product-badge">
                        <Badge tone="red">Sold out</Badge>
                      </span>
                    )}
                    {item.available && (
                      <span className="product-badge">
                        <Badge tone="green">On menu</Badge>
                      </span>
                    )}
                    {item.imageUrl ? <img src={item.imageUrl} alt="" /> : <div className="img-placeholder tall">🍴</div>}
                  </div>
                  <div className="menu-dish-body">
                    <div className="row space-between gap-sm">
                      <h3>{item.name}</h3>
                      <span className="price">{money(item.price)}</span>
                    </div>
                    {item.description ? (
                      <p className="muted small">{item.description}</p>
                    ) : (
                      <p className="muted small">No description yet.</p>
                    )}
                    {isManager && (
                      <div className="menu-dish-actions">
                        <button className="btn btn-sm" onClick={() => setDishModal({ menuId: menu.id, itemId: item.id })}>
                          Edit
                        </button>
                        <button
                          className="btn btn-sm"
                          onClick={() =>
                            void run(() =>
                              api.updateItem(restaurantId, menu.id, item.id, { ...item, available: !item.available }),
                            )
                          }
                        >
                          {item.available ? "Mark sold out" : "Mark available"}
                        </button>
                        <button
                          className="btn btn-sm btn-danger-ghost"
                          onClick={() =>
                            setDeleteTarget({ kind: "dish", menuId: menu.id, itemId: item.id, name: item.name })
                          }
                        >
                          Delete
                        </button>
                      </div>
                    )}
                  </div>
                </article>
              ))}
            </div>
          </div>
        ))}
      </div>

      <Modal
        open={dishModalOpen}
        title={dishModal?.itemId ? "Edit dish" : "Add dish"}
        onClose={() => setDishModal(null)}
        wide
      >
        {dishModal && (
          <>
            <p className="muted small modal-subtitle">
              {dishModal.itemId ? "Update details for" : "Create a new dish in"} <strong>{editingMenuName}</strong>.
            </p>
            <ItemForm
              key={`${dishModal.menuId}:${dishModal.itemId ?? "new"}`}
              initial={editingItem ?? EMPTY_ITEM}
              onCancel={() => setDishModal(null)}
              onSave={(draft) =>
                dishModal.itemId
                  ? run(() => api.updateItem(restaurantId, dishModal.menuId, dishModal.itemId!, draft))
                  : run(() => api.addItem(restaurantId, dishModal.menuId, draft))
              }
            />
          </>
        )}
      </Modal>

      <ConfirmModal
        open={deleteTarget !== null}
        title={deleteTarget?.kind === "section" ? "Delete section?" : "Delete dish?"}
        message={
          deleteTarget?.kind === "section"
            ? `Delete “${deleteTarget.name}” and all dishes in it? This can’t be undone.`
            : `Delete “${deleteTarget?.name ?? ""}” from the menu? This can’t be undone.`
        }
        confirmLabel="Delete"
        busy={deleting}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void confirmDelete()}
      />
    </section>
  );
}

function ItemForm({ initial, onSave, onCancel }: { initial: ItemDraft; onSave: (d: ItemDraft) => Promise<unknown>; onCancel: () => void }) {
  const [draft, setDraft] = useState<ItemDraft>({
    name: initial.name,
    description: initial.description ?? "",
    price: initial.price,
    imageUrl: initial.imageUrl ?? "",
    available: initial.available,
  });
  const [saving, setSaving] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await onSave({ ...draft, description: draft.description || null, imageUrl: draft.imageUrl || null });
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="item-form menu-item-form menu-item-form-modal" onSubmit={submit}>
      <label>
        Name
        <input required autoFocus value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
      </label>
      <label>
        Price (EUR)
        <input
          required
          type="number"
          min="0"
          step="0.01"
          value={draft.price}
          onChange={(e) => setDraft({ ...draft, price: Number(e.target.value) })}
        />
      </label>
      <label className="span-2">
        Description
        <input value={draft.description ?? ""} onChange={(e) => setDraft({ ...draft, description: e.target.value })} />
      </label>
      <label className="span-2">
        Image URL
        <input
          type="url"
          placeholder="https://…"
          value={draft.imageUrl ?? ""}
          onChange={(e) => setDraft({ ...draft, imageUrl: e.target.value })}
        />
      </label>
      {draft.imageUrl ? (
        <div className="menu-form-preview span-2">
          <img src={draft.imageUrl} alt="" onError={(e) => ((e.currentTarget.style.display = "none"))} />
        </div>
      ) : null}
      <label className="checkbox span-2">
        <input type="checkbox" checked={draft.available} onChange={(e) => setDraft({ ...draft, available: e.target.checked })} />{" "}
        Available on the menu
      </label>
      <div className="row gap-sm end span-2 modal-actions">
        <button type="button" className="btn" onClick={onCancel} disabled={saving}>
          Cancel
        </button>
        <button className="btn btn-primary" disabled={saving}>
          {saving ? (
            <>
              <Spinner size={16} /> Saving…
            </>
          ) : (
            "Save dish"
          )}
        </button>
      </div>
    </form>
  );
}
