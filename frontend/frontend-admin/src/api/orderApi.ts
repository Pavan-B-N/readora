import { apiClient } from './client';
import type { AdminOrdersPage, AdminOrderSummary } from '@/types/order';

export async function listReturns(page = 0, size = 20): Promise<AdminOrdersPage> {
  const response = await apiClient.get<AdminOrdersPage>('/api/v1/admin/orders', { params: { page, size } });
  return response.data;
}

export async function reviewOrder(orderId: string, note: string): Promise<AdminOrderSummary> {
  const response = await apiClient.post<AdminOrderSummary>(`/api/v1/admin/orders/${orderId}/review`, { note });
  return response.data;
}
