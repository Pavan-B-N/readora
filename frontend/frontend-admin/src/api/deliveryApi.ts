import { apiClient } from './client';
import type { AdminAgent } from '@/types/delivery';

export async function listDeliveryAgents(): Promise<AdminAgent[]> {
  const response = await apiClient.get<AdminAgent[]>('/api/v1/admin/delivery/agents');
  return response.data;
}
