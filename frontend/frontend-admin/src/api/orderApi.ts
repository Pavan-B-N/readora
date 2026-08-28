import { apiClient } from './client';
import type { AdminOrdersPage, AdminOrderSummary, ReturnMessage } from '@/types/order';

export async function listReturns(page = 0, size = 20): Promise<AdminOrdersPage> {
  const response = await apiClient.get<AdminOrdersPage>('/api/v1/admin/orders', { params: { page, size } });
  return response.data;
}

/** decision is "APPROVE"/"REJECT" for a return awaiting review; omit it for a plain cancellation note. */
export async function reviewOrder(orderId: string, note: string, decision?: 'APPROVE' | 'REJECT'): Promise<AdminOrderSummary> {
  const response = await apiClient.post<AdminOrderSummary>(`/api/v1/admin/orders/${orderId}/review`, { note, decision });
  return response.data;
}

export async function getReturnMessages(orderId: string): Promise<ReturnMessage[]> {
  const response = await apiClient.get<ReturnMessage[]>(`/api/v1/admin/orders/${orderId}/return/messages`);
  return response.data;
}

export async function postReturnMessage(orderId: string, content: string): Promise<ReturnMessage> {
  const response = await apiClient.post<ReturnMessage>(`/api/v1/admin/orders/${orderId}/return/messages`, { content });
  return response.data;
}
