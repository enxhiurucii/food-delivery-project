import { BACKOFFICE_API } from "../lib/config";
import { apiFetch } from "../lib/http";
import type { DriverOrder } from "../lib/types";

export function driverApi(getToken: () => string) {
  const call = <T,>(path: string, init?: RequestInit) =>
    apiFetch<T>(BACKOFFICE_API, path, getToken(), init);
  const json = (method: string, body: unknown): RequestInit => ({
    method,
    body: JSON.stringify(body),
  });

  return {
    available: () => call<DriverOrder[]>("/driver/deliveries/available"),
    active: () => call<DriverOrder | null>("/driver/deliveries/active"),
    claim: (orderId: string) =>
      call<DriverOrder>(`/driver/deliveries/${orderId}/claim`, { method: "POST" }),
    updateStatus: (orderId: string, status: string) =>
      call<DriverOrder>(`/driver/deliveries/${orderId}/status`, json("POST", { status })),
  };
}

export type DriverApi = ReturnType<typeof driverApi>;
