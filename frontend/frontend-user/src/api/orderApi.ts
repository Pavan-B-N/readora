import { apiClient, correlationId } from './client';
import type {
  CancelOrderResponse,
  CheckoutRequest,
  CheckoutResponse,
  OrderDetail,
  OrderSummary,
  ReturnMessage,
  ReturnOrderResponse,
} from '@/types/order';

/** Places an order from the cart; carries an idempotency key so a retried request can't double-charge/double-create. */
export async function checkout(request: CheckoutRequest): Promise<CheckoutResponse> {
  const response = await apiClient.post<CheckoutResponse>('/api/v1/orders/checkout', request, {
    headers: { 'Idempotency-Key': correlationId() },
  });
  return response.data;
}

/** Fetches a page of the caller's past orders. */
export async function listOrders(page: number, size: number): Promise<{ content: OrderSummary[]; totalPages: number }> {
  const response = await apiClient.get<{ content: OrderSummary[]; totalPages: number }>('/api/v1/orders', {
    params: { page, size },
  });
  return response.data;
}

/** Fetches full detail for one order. */
export async function getOrderDetail(orderId: string): Promise<OrderDetail> {
  const response = await apiClient.get<OrderDetail>(`/api/v1/orders/${orderId}`);
  return response.data;
}

/** Cancels an order, optionally with a reason. */
export async function cancelOrder(orderId: string, reason?: string): Promise<CancelOrderResponse> {
  const response = await apiClient.post<CancelOrderResponse>(`/api/v1/orders/${orderId}/cancel`, { reason });
  return response.data;
}

/** Requests a return for an already-delivered order. */
export async function returnOrder(orderId: string, reason: string): Promise<ReturnOrderResponse> {
  const response = await apiClient.post<ReturnOrderResponse>(`/api/v1/orders/${orderId}/return`, { reason });
  return response.data;
}

/** Fetches the return-conversation thread for an order. */
export async function getReturnMessages(orderId: string): Promise<ReturnMessage[]> {
  const response = await apiClient.get<ReturnMessage[]>(`/api/v1/orders/${orderId}/return/messages`);
  return response.data;
}

/** Posts a message into an order's return conversation. */
export async function postReturnMessage(orderId: string, content: string): Promise<ReturnMessage> {
  const response = await apiClient.post<ReturnMessage>(`/api/v1/orders/${orderId}/return/messages`, { content });
  return response.data;
}
