import { apiClient } from './client';
import type { MeResponse, UpdateProfileRequest } from '@/types/user';

export async function getMe(): Promise<MeResponse> {
  const response = await apiClient.get<MeResponse>('/api/v1/users/me');
  return response.data;
}

export async function updateProfile(request: UpdateProfileRequest): Promise<MeResponse> {
  const response = await apiClient.put<MeResponse>('/api/v1/users/me', request);
  return response.data;
}
