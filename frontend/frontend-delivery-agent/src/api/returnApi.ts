import { apiClient } from './client';
import type { ReturnPickup, ReturnPickupDetail } from '@/types/delivery';

export async function getReturnQueue(): Promise<ReturnPickup[]> {
  const response = await apiClient.get<ReturnPickup[]>('/api/v1/returns/queue');
  return response.data;
}

export async function getMyReturns(): Promise<ReturnPickup[]> {
  const response = await apiClient.get<ReturnPickup[]>('/api/v1/returns/mine');
  return response.data;
}

export async function getReturnPickupDetail(id: string): Promise<ReturnPickupDetail> {
  const response = await apiClient.get<ReturnPickupDetail>(`/api/v1/returns/${id}`);
  return response.data;
}

export async function claimReturnPickup(id: string): Promise<ReturnPickup> {
  const response = await apiClient.post<ReturnPickup>(`/api/v1/returns/${id}/claim`);
  return response.data;
}

export async function markReturnEnRoute(id: string): Promise<ReturnPickup> {
  const response = await apiClient.post<ReturnPickup>(`/api/v1/returns/${id}/en-route`);
  return response.data;
}

export async function markReturnCollected(id: string): Promise<ReturnPickup> {
  const response = await apiClient.post<ReturnPickup>(`/api/v1/returns/${id}/collected`);
  return response.data;
}
