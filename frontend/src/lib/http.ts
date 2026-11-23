/** Error carrying the RFC 9457 problem detail returned by the services ({status, code, detail}). */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string | undefined,
    message: string,
  ) {
    super(message);
  }
}

export async function apiFetch<T>(
  base: string,
  path: string,
  token: string | undefined,
  init: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (init.body) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${base}${path}`, { ...init, headers: { ...headers, ...(init.headers as Record<string, string>) } });
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}) as Record<string, unknown>);
    const message =
      (problem.detail as string | undefined) ??
      (res.status === 401 ? "Please log in again." : res.status === 403 ? "You are not allowed to do this." : res.statusText);
    throw new ApiError(res.status, problem.code as string | undefined, message);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "Something went wrong";
}
