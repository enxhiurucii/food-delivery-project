/**
 * Server-Sent Events with one-time tickets.
 * The browser's EventSource cannot send an Authorization header, so we:
 *   1. POST .../stream-ticket with the bearer token -> { ticket } (valid ~30 s, single use)
 *   2. open EventSource(.../stream?ticket=...)
 * Because a ticket is single-use, EventSource's built-in reconnect would fail; on error we close the stream
 * and subscribe again with a fresh ticket (with a small backoff).
 */
export function subscribeWithTicket(options: {
  issueTicket: () => Promise<string>;
  streamUrl: (ticket: string) => string;
  events: Record<string, (data: unknown) => void>;
  onStatus?: (connected: boolean) => void;
}): () => void {
  let source: EventSource | null = null;
  let stopped = false;
  let retryTimer: ReturnType<typeof setTimeout> | undefined;
  let attempt = 0;

  const connect = async () => {
    if (stopped) return;
    try {
      const ticket = await options.issueTicket();
      if (stopped) return;
      source = new EventSource(options.streamUrl(ticket));
      source.addEventListener("connected", () => {
        attempt = 0;
        options.onStatus?.(true);
      });
      for (const [name, handler] of Object.entries(options.events)) {
        source.addEventListener(name, (event) => handler(JSON.parse((event as MessageEvent<string>).data)));
      }
      source.onerror = () => {
        source?.close();
        scheduleReconnect();
      };
    } catch {
      scheduleReconnect();
    }
  };

  const scheduleReconnect = () => {
    options.onStatus?.(false);
    if (stopped) return;
    attempt += 1;
    retryTimer = setTimeout(connect, Math.min(1000 * 2 ** attempt, 15000));
  };

  void connect();

  return () => {
    stopped = true;
    clearTimeout(retryTimer);
    source?.close();
  };
}
