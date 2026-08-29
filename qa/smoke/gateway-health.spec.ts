import { expect, test } from '@playwright/test';

/**
 * Each of these hits a route this specific service owns, through the real gateway. For routes
 * that require auth, a 401 is the "healthy" result — it proves the request reached the service's
 * own security filter and got a real answer, rather than a gateway-level 502/503/timeout, which
 * is all this suite is trying to catch. No accounts, no state, nothing to clean up afterward.
 */

test('catalog-service is reachable through the gateway', async ({ request }) => {
  const response = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '1' } });
  expect(response.status()).toBe(200);
});

test('ai-service is reachable through the gateway', async ({ request }) => {
  const response = await request.get('/api/v1/ai/search', { params: { q: 'smoke-test', limit: '1' } });
  expect(response.status()).toBeLessThan(500);
});

test('auth-service is reachable through the gateway', async ({ request }) => {
  const response = await request.post('/api/v1/auth/login', {
    data: { email: 'no-such-user@example.com', password: 'whatever-not-real' },
  });
  expect(response.status()).toBe(401);
});

test('user-service is reachable through the gateway', async ({ request }) => {
  const response = await request.get('/api/v1/users/me/wallet');
  expect(response.status()).toBe(401);
});

test('commerce-service is reachable through the gateway', async ({ request }) => {
  const response = await request.get('/api/v1/cart');
  expect(response.status()).toBe(401);
});

test('payment-service is reachable through the gateway', async ({ request }) => {
  const response = await request.get('/api/v1/payments/00000000-0000-0000-0000-000000000000');
  expect(response.status()).toBe(401);
});

test('notification-service is reachable through the gateway', async ({ request }) => {
  const response = await request.get('/api/v1/notifications');
  expect(response.status()).toBe(401);
});

test('delivery-agent-service is reachable through the gateway', async ({ request }) => {
  const response = await request.get('/api/v1/delivery/me');
  expect(response.status()).toBe(401);
});
