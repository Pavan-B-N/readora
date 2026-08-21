import { apiClient } from './client';
import type { MeResponse, UpdateProfileRequest } from '@/types/user';

/** Fetches the currently authenticated admin's profile. */
export async function getMe(): Promise<MeResponse> {
  const response = await apiClient.get<MeResponse>('/api/v1/users/me');
  return response.data;
}

/** Updates the currently authenticated admin's profile. */
export async function updateProfile(request: UpdateProfileRequest): Promise<MeResponse> {
  const response = await apiClient.put<MeResponse>('/api/v1/users/me', request);
  return response.data;
}
