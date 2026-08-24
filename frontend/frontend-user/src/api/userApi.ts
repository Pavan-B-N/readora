import { apiClient } from './client';
import type { Address, CreateAddressRequest, MeResponse, WalletResponse } from '@/types/user';

export async function getMe(): Promise<MeResponse> {
  const response = await apiClient.get<MeResponse>('/api/v1/users/me');
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

export async function deleteAddress(addressId: string): Promise<void> {
  await apiClient.delete(`/api/v1/users/me/addresses/${addressId}`);
}

export async function getWallet(page: number, size: number): Promise<WalletResponse> {
  const response = await apiClient.get<WalletResponse>('/api/v1/users/me/wallet', { params: { page, size } });
  return response.data;
}
