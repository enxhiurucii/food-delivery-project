import { BACKOFFICE_API } from "../lib/config";
import { apiFetch } from "../lib/http";
import type { DailyReport, Me, Menu, MenuItem, Page, Restaurant, StaffOrder, StaffUser } from "../lib/types";

/** Typed client for backoffice-service. getToken() returns the current (auto-renewed) staff access token. */
export function backofficeApi(getToken: () => string) {
  const call = <T,>(path: string, init?: RequestInit) => apiFetch<T>(BACKOFFICE_API, path, getToken(), init);
  const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) });

  return {
    me: () => call<Me>("/me"),
    restaurants: () => call<Restaurant[]>("/restaurants"),
    restaurant: (id: string) => call<Restaurant>(`/restaurants/${id}`),
    updateRestaurant: (id: string, body: Omit<Restaurant, "id" | "currency">) => call<Restaurant>(`/restaurants/${id}`, json("PUT", body)),

    menus: (rid: string) => call<Menu[]>(`/restaurants/${rid}/menus`),
    createMenu: (rid: string, body: { name: string; description?: string; position: number }) =>
      call<Menu>(`/restaurants/${rid}/menus`, json("POST", body)),
    deleteMenu: (rid: string, menuId: string) => call<void>(`/restaurants/${rid}/menus/${menuId}`, { method: "DELETE" }),
    addItem: (rid: string, menuId: string, body: Omit<MenuItem, "id" | "currency">) =>
      call<MenuItem>(`/restaurants/${rid}/menus/${menuId}/items`, json("POST", body)),
    updateItem: (rid: string, menuId: string, itemId: string, body: Omit<MenuItem, "id" | "currency">) =>
      call<MenuItem>(`/restaurants/${rid}/menus/${menuId}/items/${itemId}`, json("PUT", body)),
    deleteItem: (rid: string, menuId: string, itemId: string) =>
      call<void>(`/restaurants/${rid}/menus/${menuId}/items/${itemId}`, { method: "DELETE" }),

    orders: (rid: string) => call<StaffOrder[]>(`/restaurants/${rid}/orders`),
    changeStatus: (rid: string, orderId: string, status: string) =>
      call<StaffOrder>(`/restaurants/${rid}/orders/${orderId}/status`, json("POST", { status })),
    streamTicket: (rid: string) => call<{ ticket: string }>(`/restaurants/${rid}/orders/stream-ticket`, { method: "POST" }),

    dailyReport: (rid: string, date: string) => call<DailyReport>(`/restaurants/${rid}/reports/daily?date=${date}`),
    receipts: (rid: string, date: string, page = 0) =>
      call<Page<StaffOrder>>(`/restaurants/${rid}/receipts?date=${date}&page=${page}&size=20`),

    createRestaurant: (body: Omit<Restaurant, "id" | "currency">) => call<Restaurant>("/restaurants", json("POST", body)),

    createManager: (body: { firstName: string; lastName: string; username: string; email: string; password: string; restaurantId: string }) =>
      call<StaffUser>("/admin/users/managers", json("POST", body)),
    createDriver: (body: { firstName: string; lastName: string; username: string; email: string; password: string }) =>
      call<StaffUser>("/admin/users/drivers", json("POST", body)),
    restaurantStaff: (rid: string) => call<{ subject: string; displayName: string; restaurantId: string; createdAt: string }[]>(`/restaurants/${rid}/staff`),
  };
}

export type BackofficeApi = ReturnType<typeof backofficeApi>;

export const streamUrl = (rid: string, ticket: string) =>
  `${BACKOFFICE_API}/restaurants/${rid}/orders/stream?ticket=${encodeURIComponent(ticket)}`;
