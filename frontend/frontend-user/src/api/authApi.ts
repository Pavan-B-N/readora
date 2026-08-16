import { apiClient } from './client';
import type { LoginRequest, LoginResponse, RegisterRequest, RegisterResponse } from '@/types/auth';

/** Authenticates with email/password and returns the issued access/refresh tokens. */
export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>('/api/v1/auth/login', request);
  return response.data;
}

/** Creates a new account and returns the issued access/refresh tokens. */
export async function register(request: RegisterRequest): Promise<RegisterResponse> {
  const response = await apiClient.post<RegisterResponse>('/api/v1/auth/register', request);
  return response.data;
}
