import type { Store } from '@/types/catalog';

// Most of the seeded catalog (books, reviews, inventory) is scoped to Bangalore, so it's the
// default "delivering from" store whenever nothing more specific applies (no signed-in
// preference, no store chosen yet) — /api/v1/stores sorts alphabetically by name, which puts
// Ahmedabad first, so picking stores[0] silently defaulted everyone there instead.
const DEFAULT_STORE_CITY = 'Bangalore';

/** Falls back to the first store if Bangalore isn't in the list, so this never returns nothing. */
export function pickDefaultStore(stores: Store[]): Store | null {
  return stores.find((s) => s.city === DEFAULT_STORE_CITY) ?? stores[0] ?? null;
}
