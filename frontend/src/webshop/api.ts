import { WEBSHOP_API } from "../lib/config";
import { apiFetch } from "../lib/http";
import type { Address, Cart, CustomerOrder, Menu, ShopRestaurant } from "../lib/types";

export interface CheckoutRequest {
  fulfillmentType: "PICKUP" | "DELIVERY";
  deliveryAddress: Address | null;
  paymentMethod: "CASH" | "CARD";
  paymentTiming: "ONLINE" | "ON_COMPLETION";
  notes: string | null;
}

/** Typed client for webshop-service. Catalog calls are public; the rest need the customer token. */
export function webshopApi(getToken: () => string | undefined) {
  const call = <T,>(path: string, init?: RequestInit) => apiFetch<T>(WEBSHOP_API, path, getToken(), init);
  const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) });

  return {
    restaurants: () => call<ShopRestaurant[]>("/restaurants"),
    restaurant: (id: string) => call<ShopRestaurant>(`/restaurants/${id}`),
    menus: (id: string) => call<Menu[]>(`/restaurants/${id}/menus`),

    cart: () => call<Cart>("/cart"),
    addToCart: (restaurantId: string, menuItemId: string, quantity = 1) =>
      call<Cart>("/cart/items", json("POST", { restaurantId, menuItemId, quantity })),
    changeQuantity: (menuItemId: string, quantity: number) => call<Cart>(`/cart/items/${menuItemId}`, json("PUT", { quantity })),
    clearCart: () => call<void>("/cart", { method: "DELETE" }),

    checkout: (body: CheckoutRequest) => call<{ order: CustomerOrder; pricesChanged: boolean }>("/checkout", json("POST", body)),
    orders: () => call<CustomerOrder[]>("/orders"),
    order: (id: string) => call<CustomerOrder>(`/orders/${id}`),
    streamTicket: (id: string) => call<{ ticket: string }>(`/orders/${id}/stream-ticket`, { method: "POST" }),
  };
}

export type WebshopApi = ReturnType<typeof webshopApi>;

export const orderStreamUrl = (orderId: string, ticket: string) =>
  `${WEBSHOP_API}/orders/${orderId}/stream?ticket=${encodeURIComponent(ticket)}`;
