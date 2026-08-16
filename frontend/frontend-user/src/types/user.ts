/** Free-form category tag for a saved address, shown as a chip in the UI. */
export type AddressLabel = 'HOME' | 'WORK' | 'OTHER';
/** Whether an address's recipient is the account owner (defaults name/phone from the profile) or someone else. */
export type AddressRecipientType = 'OWNER' | 'GUEST';

/** The signed-in caller's own profile — display fields, preferences, and wallet balance. */
export interface MeResponse {
  userId: string;
  email: string;
  displayName: string | null;
  avatarUrl: string | null;
  phone: string | null;
  locale: string | null;
  preferredStoreId: string | null;
  favoriteCategoryIds: string[];
  wallet: { balance: string; currency: string };
}

/** Patch body for profile updates — every field is optional/nullable since callers only send what changed. */
export interface UpdateProfileRequest {
  displayName?: string | null;
  phone?: string | null;
  preferredStoreId?: string | null;
  favoriteCategoryIds?: string[] | null;
}

/** A saved shipping address. */
export interface Address {
  id: string;
  label: AddressLabel;
  recipientType: AddressRecipientType;
  recipientName: string;
  recipientPhone: string | null;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  postalCode: string;
  countryCode: string;
  storeId: string | null;
  isDefault: boolean;
}

/** Body for saving a new address. */
export interface CreateAddressRequest {
  label: AddressLabel;
  recipientType: AddressRecipientType;
  recipientName: string;
  recipientPhone: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  postalCode: string;
  countryCode: string;
  storeId?: string;
  isDefault: boolean;
}

/** A single wallet transaction (top-up, refund, payment, etc.). */
export interface WalletLedgerItem {
  id: string;
  amount: string;
  type: string;
  balanceAfter: string;
  orderId: string | null;
  createdAt: string;
}

/** Current wallet balance plus recent transaction history. */
export interface WalletResponse {
  balance: string;
  currency: string;
  items: WalletLedgerItem[];
}

/** A wishlisted book id with when it was added. */
export interface WishlistItem {
  bookId: string;
  addedAt: string;
}

/** A viewed-book entry, used to drive the "Recently viewed" rail. */
export interface BrowsingHistoryItem {
  bookId: string;
  viewedAt: string;
}

/** A past search query, used to drive the search bar's recent-searches dropdown. */
export interface SearchHistoryItem {
  query: string;
  searchedAt: string;
}
