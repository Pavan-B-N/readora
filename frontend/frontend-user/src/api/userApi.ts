import { apiClient } from './client';
import type {
  Address,
  BrowsingHistoryItem,
  CreateAddressRequest,
  MeResponse,
  SearchHistoryItem,
  UpdateProfileRequest,
  WalletResponse,
  WishlistItem,
} from '@/types/user';

/** Fetches the caller's own profile. */
export async function getMe(): Promise<MeResponse> {
  const response = await apiClient.get<MeResponse>('/api/v1/users/me');
  return response.data;
}

/** Updates the caller's own profile fields. */
export async function updateProfile(request: UpdateProfileRequest): Promise<MeResponse> {
  const response = await apiClient.put<MeResponse>('/api/v1/users/me', request);
  return response.data;
}

/** Lists the caller's saved addresses. */
export async function listAddresses(): Promise<Address[]> {
  const response = await apiClient.get<Address[]>('/api/v1/users/me/addresses');
  return response.data;
}

/** Adds a new saved address for the caller. */
export async function addAddress(request: CreateAddressRequest): Promise<{ id: string; isDefault: boolean }> {
  const response = await apiClient.post<{ id: string; isDefault: boolean }>('/api/v1/users/me/addresses', request);
  return response.data;
}

/** Marks a saved address as the caller's default. */
export async function setDefaultAddress(addressId: string): Promise<void> {
  await apiClient.put(`/api/v1/users/me/addresses/${addressId}/default`);
}

/** Deletes one of the caller's saved addresses. */
export async function deleteAddress(addressId: string): Promise<void> {
  await apiClient.delete(`/api/v1/users/me/addresses/${addressId}`);
}

/** Fetches a page of the caller's wallet transaction history. */
export async function getWallet(page: number, size: number): Promise<WalletResponse> {
  const response = await apiClient.get<WalletResponse>('/api/v1/users/me/wallet', { params: { page, size } });
  return response.data;
}

/** Adds funds to the caller's wallet balance. */
export async function topUpWallet(amount: string): Promise<{ balance: string; currency: string }> {
  const response = await apiClient.post<{ balance: string; currency: string }>('/api/v1/users/me/wallet/topup', { amount });
  return response.data;
}

/** Redeems a coupon code for wallet credit. */
export async function redeemCoupon(code: string): Promise<{ creditedAmount: string; balance: string; currency: string }> {
  const response = await apiClient.post<{ creditedAmount: string; balance: string; currency: string }>(
    '/api/v1/users/me/wallet/redeem-coupon',
    { code },
  );
  return response.data;
}

/** Lists the caller's wishlisted books. */
export async function listWishlist(): Promise<WishlistItem[]> {
  const response = await apiClient.get<WishlistItem[]>('/api/v1/users/me/wishlist');
  return response.data;
}

/** Adds a book to the caller's wishlist. */
export async function addToWishlist(bookId: string): Promise<void> {
  await apiClient.put(`/api/v1/users/me/wishlist/${bookId}`);
}

/** Removes a book from the caller's wishlist. */
export async function removeFromWishlist(bookId: string): Promise<void> {
  await apiClient.delete(`/api/v1/users/me/wishlist/${bookId}`);
}

/** Fetches the caller's recently-viewed books. */
export async function getBrowsingHistory(): Promise<BrowsingHistoryItem[]> {
  const response = await apiClient.get<BrowsingHistoryItem[]>('/api/v1/users/me/history');
  return response.data;
}

/** Records a book view into the caller's browsing history. */
export async function recordBookView(bookId: string): Promise<void> {
  await apiClient.put(`/api/v1/users/me/history/${bookId}`);
}

/** Fetches the caller's past search queries. */
export async function getSearchHistory(): Promise<SearchHistoryItem[]> {
  const response = await apiClient.get<SearchHistoryItem[]>('/api/v1/users/me/search-history');
  return response.data;
}

/** Records a search query into the caller's search history. */
export async function recordSearch(query: string): Promise<void> {
  await apiClient.post('/api/v1/users/me/search-history', { query });
}
