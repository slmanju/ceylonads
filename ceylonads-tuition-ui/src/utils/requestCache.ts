// Minimal in-memory memoization for master data (categories, locations, per-category filter
// metadata) so repeated mounts across navigation reuse one in-flight/resolved request instead of
// refetching. Not persisted; cleared on full page reload.
const cache = new Map<string, Promise<unknown>>();

export function cachedRequest<T>(key: string, fetcher: () => Promise<T>): Promise<T> {
  const existing = cache.get(key);
  if (existing) return existing as Promise<T>;

  const promise = fetcher().catch((err) => {
    cache.delete(key);
    throw err;
  });
  cache.set(key, promise);
  return promise;
}
