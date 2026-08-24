export type AddressLabel = 'HOME' | 'WORK' | 'OTHER';

export interface MeResponse {
  userId: string;
  email: string;
  displayName: string | null;
  avatarUrl: string | null;
  locale: string | null;
  wallet: { balance: string; currency: string };
}

export interface Address {
  id: string;
  label: AddressLabel;
  recipientName: string;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  postalCode: string;
  countryCode: string;
  isDefault: boolean;
}

export interface CreateAddressRequest {
  label: AddressLabel;
  recipientName: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  postalCode: string;
  countryCode: string;
  phone?: string;
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
