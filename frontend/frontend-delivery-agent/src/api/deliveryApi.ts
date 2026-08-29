import { apiClient } from './client';
import type { AgentMe, AgentStats, Assignment, AssignmentDetail } from '@/types/delivery';

export async function getMe(): Promise<AgentMe> {
  const response = await apiClient.get<AgentMe>('/api/v1/delivery/me');
  return response.data;
}

export async function getStats(): Promise<AgentStats> {
  const response = await apiClient.get<AgentStats>('/api/v1/delivery/me/stats');
  return response.data;
}

export async function setDuty(onDuty: boolean): Promise<AgentMe> {
  const response = await apiClient.put<AgentMe>('/api/v1/delivery/me/duty', { onDuty });
  return response.data;
}

export async function getQueue(): Promise<Assignment[]> {
  const response = await apiClient.get<Assignment[]>('/api/v1/delivery/queue');
  return response.data;
}

export async function getMine(): Promise<Assignment[]> {
  const response = await apiClient.get<Assignment[]>('/api/v1/delivery/mine');
  return response.data;
}

export async function getAssignmentDetail(id: string): Promise<AssignmentDetail> {
  const response = await apiClient.get<AssignmentDetail>(`/api/v1/delivery/${id}`);
  return response.data;
}

export async function claimAssignment(id: string): Promise<Assignment> {
  const response = await apiClient.post<Assignment>(`/api/v1/delivery/${id}/claim`);
  return response.data;
}

export async function markOutForDelivery(id: string): Promise<Assignment> {
  const response = await apiClient.post<Assignment>(`/api/v1/delivery/${id}/out-for-delivery`);
  return response.data;
}

export async function markDelivered(id: string): Promise<Assignment> {
  const response = await apiClient.post<Assignment>(`/api/v1/delivery/${id}/delivered`);
  return response.data;
}
