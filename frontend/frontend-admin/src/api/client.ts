import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { store } from '@/redux/store';
import { loggedOut, tokensReceived } from '@/redux/slices/authSlice';
import type { LoginResponse } from '@/types/auth';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: BASE_URL,
});

/** A request that never got a response at all (connection refused, DNS failure, timeout, offline) is called out by name rather than folded into the caller's generic fallback. */
export function extractErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    if (typeof error.response?.data?.message === 'string') {
      return error.response.data.message;
    }
    if (!error.response) {
      return "Can't reach the server. Check your connection and try again.";
    }
  }
  return fallback;
}

/**
 * crypto.randomUUID() requires a secure context (HTTPS, or the special localhost exception) —
 * it's simply undefined otherwise, which is exactly the case for a plain-HTTP deployment (no
 * TLS yet). This is just a request-tracing id, not security-sensitive, so a plain pseudo-random
 * fallback is fine when the real thing isn't available.
 */
function correlationId(): string {
  if (typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

apiClient.interceptors.request.use((config) => {
  const { accessToken } = store.getState().auth;

  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`);
  }
  config.headers.set('X-Correlation-Id', correlationId());

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
