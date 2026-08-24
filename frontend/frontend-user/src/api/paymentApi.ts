import { apiClient } from './client';
import type { PaymentResponse } from '@/types/payment';

export async function getPaymentByOrder(orderId: string): Promise<PaymentResponse> {
  const response = await apiClient.get<PaymentResponse>(`/api/v1/payments/${orderId}`);
  return response.data;
}
