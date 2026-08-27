export type AddressLabel = 'HOME' | 'WORK' | 'OTHER';
export type AddressRecipientType = 'OWNER' | 'GUEST';

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

export interface UpdateProfileRequest {
  displayName?: string | null;
  phone?: string | null;
  preferredStoreId?: string | null;
  favoriteCategoryIds?: string[] | null;
}

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

export interface WalletLedgerItem {
  id: string;
  amount: string;
  type: string;
  balanceAfter: string;
  orderId: string | null;
  createdAt: string;
}

export interface WalletResponse {
  balance: string;
  currency: string;
  items: WalletLedgerItem[];
}

export interface WishlistItem {
  bookId: string;
  addedAt: string;
}
