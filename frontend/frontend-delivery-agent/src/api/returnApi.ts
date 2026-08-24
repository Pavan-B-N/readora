import { apiClient } from './client';
import type { ReturnPickup, ReturnPickupDetail } from '@/types/delivery';

/** Fetches unclaimed return pickups available for the agent to pick up. */
export async function getReturnQueue(): Promise<ReturnPickup[]> {
  const response = await apiClient.get<ReturnPickup[]>('/api/v1/returns/queue');
  return response.data;
}

/** Fetches the return pickups the agent has already claimed. */
export async function getMyReturns(): Promise<ReturnPickup[]> {
  const response = await apiClient.get<ReturnPickup[]>('/api/v1/returns/mine');
  return response.data;
}

/** Fetches full detail for a single return pickup, including the chat thread and pickup address. */
export async function getReturnPickupDetail(id: string): Promise<ReturnPickupDetail> {
  const response = await apiClient.get<ReturnPickupDetail>(`/api/v1/returns/${id}`);
  return response.data;
}

/** Claims an unassigned return pickup from the queue for the current agent. */
export async function claimReturnPickup(id: string): Promise<ReturnPickup> {
  const response = await apiClient.post<ReturnPickup>(`/api/v1/returns/${id}/claim`);
  return response.data;
}

/** Marks a claimed return pickup as en route to the customer. */
export async function markReturnEnRoute(id: string): Promise<ReturnPickup> {
  const response = await apiClient.post<ReturnPickup>(`/api/v1/returns/${id}/en-route`);
  return response.data;
}

/** Marks a return pickup as collected from the customer, completing it. */
export async function markReturnCollected(id: string): Promise<ReturnPickup> {
  const response = await apiClient.post<ReturnPickup>(`/api/v1/returns/${id}/collected`);
  return response.data;
}
