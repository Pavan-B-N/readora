import { apiClient } from './client';
import type { AddCartItemRequest, CartResponse, CartSummaryResponse, DeliveryType } from '@/types/cart';

/** Fetches the caller's current cart. */
export async function getCart(): Promise<CartResponse> {
  const response = await apiClient.get<CartResponse>('/api/v1/cart');
  return response.data;
}

/** Adds a book/delivery-type combination to the cart, or increments it if already present. */
export async function addItem(request: AddCartItemRequest): Promise<CartSummaryResponse> {
  const response = await apiClient.post<CartSummaryResponse>('/api/v1/cart/items', request);
  return response.data;
}

/** Sets the quantity of one cart line to an exact value (0 removes it). */
export async function setItemQty(bookId: string, deliveryType: DeliveryType, qty: number): Promise<CartSummaryResponse> {
  const response = await apiClient.put<CartSummaryResponse>(`/api/v1/cart/items/${bookId}/${deliveryType}`, { qty });
  return response.data;
}
