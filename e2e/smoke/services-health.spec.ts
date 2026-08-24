import { test, expect, request as playwrightRequest, type APIRequestContext } from '@playwright/test';
import { GATEWAY_BASE_URL, MCP_SERVER_BASE_URL, NIL_UUID, uniqueTestEmail } from '../support/config';

/**
 * Reachability + wiring smoke test, not a functional test suite — every assertion here checks
 * "did the right service answer with a sensible status", not specific business outcomes. Runs a
 * real register→login once, then reuses that token to prove genuine pass-through to each backend
 * service through api-gateway (a bare 401 from the gateway itself would prove nothing about the
 * service behind it).
 */
test.describe.configure({ mode: 'serial' });

let api: APIRequestContext;
let accessToken: string;

test.beforeAll(async () => {
  api = await playwrightRequest.newContext({ baseURL: GATEWAY_BASE_URL });

  const email = uniqueTestEmail();
  const password = 'smoke-test-password-123';

  const registerRes = await api.post('/api/v1/auth/register', {
    data: { email, password, fullName: 'Smoke Test' },
  });
  expect(registerRes.status(), 'auth-service: register').toBe(201);

  const loginRes = await api.post('/api/v1/auth/login', { data: { email, password } });
  expect(loginRes.status(), 'auth-service: login').toBe(200);

  accessToken = (await loginRes.json()).accessToken;
});

test.afterAll(async () => {
  await api.dispose();
});

function authHeaders() {
  return { Authorization: `Bearer ${accessToken}` };
}

test.describe('public catalog routes — through the gateway', () => {
  test('catalog-service: GET /api/v1/books', async () => {
    expect((await api.get('/api/v1/books')).status()).toBe(200);
  });

  test('catalog-service: GET /api/v1/categories', async () => {
    expect((await api.get('/api/v1/categories')).status()).toBe(200);
  });

  test('catalog-service: GET /api/v1/publishers', async () => {
    expect((await api.get('/api/v1/publishers')).status()).toBe(200);
  });

  test('catalog-service: GET /api/v1/authors', async () => {
    expect((await api.get('/api/v1/authors')).status()).toBe(200);
  });
});

test.describe('the gateway itself', () => {
  test('unknown route returns 404, not a silent pass-through', async () => {
    expect((await api.get('/api/v1/does-not-exist')).status()).toBe(404);
  });

  test('protected route without a token is rejected at the gateway', async () => {
    expect((await api.get('/api/v1/cart')).status()).toBe(401);
  });
});

test.describe('authenticated pass-through to each backend service', () => {
  test('user-service: GET /api/v1/users/me', async () => {
    expect((await api.get('/api/v1/users/me', { headers: authHeaders() })).status()).toBe(200);
  });

  test('user-service: GET /api/v1/users/me/wallet', async () => {
    expect((await api.get('/api/v1/users/me/wallet', { headers: authHeaders() })).status()).toBe(200);
  });

  test('commerce-service: GET /api/v1/cart', async () => {
    expect((await api.get('/api/v1/cart', { headers: authHeaders() })).status()).toBe(200);
  });

  test('commerce-service: GET /api/v1/orders', async () => {
    expect((await api.get('/api/v1/orders', { headers: authHeaders() })).status()).toBe(200);
  });

  test('payment-service: GET /api/v1/payments/:orderId returns a real 404, not an auth error', async () => {
    const res = await api.get(`/api/v1/payments/${NIL_UUID}`, { headers: authHeaders() });
    expect(res.status()).toBe(404);
  });
});

test.describe('admin gate — authenticated but not ADMIN', () => {
  test('catalog-service: admin route is forbidden for a plain customer', async () => {
    const res = await api.get(`/api/v1/admin/books/${NIL_UUID}`, { headers: authHeaders() });
    expect(res.status()).toBe(403);
  });

  test('ai-service: admin route is forbidden for a plain customer', async () => {
    const res = await api.post('/api/v1/admin/embeddings/backfill', { headers: authHeaders() });
    expect(res.status()).toBe(403);
  });
});

test.describe('mcp-server — not routed through the gateway at all', () => {
  test('direct request without the shared gateway secret is rejected', async () => {
    const direct = await playwrightRequest.newContext({ baseURL: MCP_SERVER_BASE_URL });
    try {
      expect((await direct.get('/')).status()).toBe(403);
    } finally {
      await direct.dispose();
    }
  });
});
