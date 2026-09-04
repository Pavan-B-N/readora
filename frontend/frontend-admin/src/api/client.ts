import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { store } from '@/redux/store';
import { loggedOut, tokensReceived } from '@/redux/slices/authSlice';
import type { LoginResponse } from '@/types/auth';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: BASE_URL,
});

export function extractErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError<{ message?: string }>(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

apiClient.interceptors.request.use((config) => {
  const { accessToken } = store.getState().auth;

  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`);
  }
  config.headers.set('X-Correlation-Id', crypto.randomUUID());

  return config;
});

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
}

async function refreshAccessToken(): Promise<string | null> {
  const { refreshToken } = store.getState().auth;
  if (!refreshToken) {
    return null;
  }

  try {
    const response = await axios.post<LoginResponse>(`${BASE_URL}/api/v1/auth/refresh`, { refreshToken });
    store.dispatch(tokensReceived({
      accessToken: response.data.accessToken,
      refreshToken: response.data.refreshToken,
    }));
    return response.data.accessToken;
  } catch {
    return null;
  }
}

let refreshPromise: Promise<string | null> | null = null;

function dedupedRefresh(): Promise<string | null> {
  refreshPromise ??= refreshAccessToken().finally(() => {
    refreshPromise = null;
  });
  return refreshPromise;
}

export async function bootstrapSession(): Promise<void> {
  if (store.getState().auth.refreshToken) {
    await dedupedRefresh();
  }
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined;

    if (error.response?.status !== 401 || !originalRequest || originalRequest._retried) {
      return Promise.reject(error);
    }

    originalRequest._retried = true;

    const newAccessToken = await dedupedRefresh();

    if (!newAccessToken) {
      store.dispatch(loggedOut());
      return Promise.reject(error);
    }

    originalRequest.headers.set('Authorization', `Bearer ${newAccessToken}`);
    return apiClient(originalRequest);
  },
);
