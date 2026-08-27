import { apiClient } from './client';
import type { Address, CreateAddressRequest, MeResponse, UpdateProfileRequest, WalletResponse, WishlistItem } from '@/types/user';

export async function getMe(): Promise<MeResponse> {
  const response = await apiClient.get<MeResponse>('/api/v1/users/me');
  return response.data;
}

export async function updateProfile(request: UpdateProfileRequest): Promise<MeResponse> {
  const response = await apiClient.put<MeResponse>('/api/v1/users/me', request);
  return response.data;
}

export async function listAddresses(): Promise<Address[]> {
  const response = await apiClient.get<Address[]>('/api/v1/users/me/addresses');
  return response.data;
}

export async function addAddress(request: CreateAddressRequest): Promise<{ id: string; isDefault: boolean }> {
  const response = await apiClient.post<{ id: string; isDefault: boolean }>('/api/v1/users/me/addresses', request);
  return response.data;
}

export async function setDefaultAddress(addressId: string): Promise<void> {
  await apiClient.put(`/api/v1/users/me/addresses/${addressId}/default`);
}

export async function deleteAddress(addressId: string): Promise<void> {
  await apiClient.delete(`/api/v1/users/me/addresses/${addressId}`);
}

export async function getWallet(page: number, size: number): Promise<WalletResponse> {
  const response = await apiClient.get<WalletResponse>('/api/v1/users/me/wallet', { params: { page, size } });
  return response.data;
}

export async function topUpWallet(amount: string): Promise<{ balance: string; currency: string }> {
  const response = await apiClient.post<{ balance: string; currency: string }>('/api/v1/users/me/wallet/topup', { amount });
  return response.data;
}

export async function redeemCoupon(code: string): Promise<{ creditedAmount: string; balance: string; currency: string }> {
  const response = await apiClient.post<{ creditedAmount: string; balance: string; currency: string }>(
    '/api/v1/users/me/wallet/redeem-coupon',
    { code },
  );
  return response.data;
}

export async function listWishlist(): Promise<WishlistItem[]> {
  const response = await apiClient.get<WishlistItem[]>('/api/v1/users/me/wishlist');
  return response.data;
}

export async function addToWishlist(bookId: string): Promise<void> {
  await apiClient.put(`/api/v1/users/me/wishlist/${bookId}`);
}

export async function removeFromWishlist(bookId: string): Promise<void> {
  await apiClient.delete(`/api/v1/users/me/wishlist/${bookId}`);
}
