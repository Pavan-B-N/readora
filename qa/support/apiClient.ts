import type { APIRequestContext } from '@playwright/test';

/**
 * A throwaway customer account, registered fresh per test run — never a shared fixture, so tests
 * can run in parallel without fighting over the same email or wallet balance.
 */
export interface TestUser {
  email: string;
  password: string;
  accessToken: string;
  refreshToken: string;
}

export function uniqueEmail(prefix: string): string {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1_000_000)}@example.com`;
}

/** Registers a new customer and logs in, returning a ready-to-use access token. */
export async function registerAndLogin(request: APIRequestContext, emailPrefix = 'qa'): Promise<TestUser> {
  const email = uniqueEmail(emailPrefix);
  const password = 'correct-horse-battery-staple';

  const registerResponse = await request.post('/api/v1/auth/register', {
    data: { email, password, fullName: 'QA Test User' },
  });
  if (!registerResponse.ok()) {
    throw new Error(`Register failed (${registerResponse.status()}): ${await registerResponse.text()}`);
  }

  const loginResponse = await request.post('/api/v1/auth/login', { data: { email, password } });
  if (!loginResponse.ok()) {
    throw new Error(`Login failed (${loginResponse.status()}): ${await loginResponse.text()}`);
  }
  const { accessToken, refreshToken } = await loginResponse.json();

  return { email, password, accessToken, refreshToken };
}

export function authHeaders(accessToken: string): Record<string, string> {
  return { Authorization: `Bearer ${accessToken}` };
}

export async function pollUntil<T>(
  check: () => Promise<T | undefined>,
  options: { attempts?: number; delayMs?: number } = {},
): Promise<T> {
  const { attempts = 20, delayMs = 500 } = options;
  for (let attempt = 0; attempt < attempts; attempt++) {
    const result = await check();
    if (result !== undefined) return result;
    await new Promise((resolve) => setTimeout(resolve, delayMs));
  }
  throw new Error(`pollUntil: condition never became true after ${attempts} attempts (${attempts * delayMs}ms)`);
}
