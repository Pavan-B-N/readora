import { apiClient } from './client';
import type { CancelOrderResponse, CheckoutRequest, CheckoutResponse, OrderDetail, OrderSummary } from '@/types/order';

export async function checkout(request: CheckoutRequest): Promise<CheckoutResponse> {
  const response = await apiClient.post<CheckoutResponse>('/api/v1/orders/checkout', request, {
    headers: { 'Idempotency-Key': crypto.randomUUID() },
  });
  return response.data;
}

export async function listOrders(page: number, size: number): Promise<{ content: OrderSummary[]; totalPages: number }> {
  const response = await apiClient.get<{ content: OrderSummary[]; totalPages: number }>('/api/v1/orders', {
    params: { page, size },
  });
  return response.data;
}

export async function getOrderDetail(orderId: string): Promise<OrderDetail> {
  const response = await apiClient.get<OrderDetail>(`/api/v1/orders/${orderId}`);
  return response.data;
}

export async function cancelOrder(orderId: string, reason?: string): Promise<CancelOrderResponse> {
  const response = await apiClient.post<CancelOrderResponse>(`/api/v1/orders/${orderId}/cancel`, { reason });
  return response.data;
}
