import { apiClient } from './client';
import type { AddCartItemRequest, CartResponse, CartSummaryResponse } from '@/types/cart';

export async function getCart(): Promise<CartResponse> {
  const response = await apiClient.get<CartResponse>('/api/v1/cart');
  return response.data;
}

export async function addItem(request: AddCartItemRequest): Promise<CartSummaryResponse> {
  const response = await apiClient.post<CartSummaryResponse>('/api/v1/cart/items', request);
  return response.data;
}

export async function setItemQty(bookId: string, qty: number): Promise<CartSummaryResponse> {
  const response = await apiClient.put<CartSummaryResponse>(`/api/v1/cart/items/${bookId}`, { qty });
  return response.data;
}
