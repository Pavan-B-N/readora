import { apiClient } from './client';
import type { AgentMe, AgentStats, Assignment, AssignmentDetail } from '@/types/delivery';

/** Fetches the logged-in delivery agent's own profile. */
export async function getMe(): Promise<AgentMe> {
  const response = await apiClient.get<AgentMe>('/api/v1/delivery/me');
  return response.data;
}

/** Fetches the logged-in agent's delivery performance stats. */
export async function getStats(): Promise<AgentStats> {
  const response = await apiClient.get<AgentStats>('/api/v1/delivery/me/stats');
  return response.data;
}

/** Toggles the agent's on-duty status, controlling whether they receive new assignments. */
export async function setDuty(onDuty: boolean): Promise<AgentMe> {
  const response = await apiClient.put<AgentMe>('/api/v1/delivery/me/duty', { onDuty });
  return response.data;
}

/** Fetches unclaimed delivery assignments available for the agent to pick up. */
export async function getQueue(): Promise<Assignment[]> {
  const response = await apiClient.get<Assignment[]>('/api/v1/delivery/queue');
  return response.data;
}

/** Fetches the assignments the agent has already claimed. */
export async function getMine(): Promise<Assignment[]> {
  const response = await apiClient.get<Assignment[]>('/api/v1/delivery/mine');
  return response.data;
}

/** Fetches full detail for a single assignment, including order and address info. */
export async function getAssignmentDetail(id: string): Promise<AssignmentDetail> {
  const response = await apiClient.get<AssignmentDetail>(`/api/v1/delivery/${id}`);
  return response.data;
}

/** Claims an unassigned delivery from the queue for the current agent. */
export async function claimAssignment(id: string): Promise<Assignment> {
  const response = await apiClient.post<Assignment>(`/api/v1/delivery/${id}/claim`);
  return response.data;
}

/** Marks a claimed assignment as out for delivery. */
export async function markOutForDelivery(id: string): Promise<Assignment> {
  const response = await apiClient.post<Assignment>(`/api/v1/delivery/${id}/out-for-delivery`);
  return response.data;
}

/** Marks an assignment as delivered, completing it. */
export async function markDelivered(id: string): Promise<Assignment> {
  const response = await apiClient.post<Assignment>(`/api/v1/delivery/${id}/delivered`);
  return response.data;
}
