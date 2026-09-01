import { expect, test } from '@playwright/test';
import { authHeaders, registerAndLogin } from '../support/apiClient';

/**
 * Covers the ADMIN-role gate itself (any authenticated non-admin caller must get 403, and an
 * anonymous caller must get 401) without needing seeded admin credentials. The actual admin
 * business flows (approve a return, create a book, queue an embeddings backfill) need a real
 * ADMIN_EMAIL/ADMIN_PASSWORD account and are skipped without one, same pattern as
 * delivery-claim.spec.ts and return-workflow.spec.ts.
 */
test.describe('admin route gate', () => {
  test('an anonymous caller hitting an admin route gets 401', async ({ request }) => {
    const response = await request.get('/api/v1/admin/books/00000000-0000-0000-0000-000000000000');
    expect(response.status()).toBe(401);
  });

  test('an authenticated non-admin caller hitting an admin route gets 403', async ({ request }) => {
    const user = await registerAndLogin(request, 'admin-forbidden');
    const response = await request.get('/api/v1/admin/orders', { headers: authHeaders(user.accessToken) });
    expect(response.status()).toBe(403);
  });

  test('a non-admin caller cannot queue an embeddings backfill', async ({ request }) => {
    const user = await registerAndLogin(request, 'admin-forbidden-embed');
    const response = await request.post('/api/v1/admin/embeddings/backfill', { headers: authHeaders(user.accessToken) });
    expect(response.status()).toBe(403);
  });

  test('a non-admin caller cannot create a book', async ({ request }) => {
    const user = await registerAndLogin(request, 'admin-forbidden-book');
    const response = await request.post('/api/v1/admin/books', {
      headers: authHeaders(user.accessToken),
      data: { title: 'Should never be created' },
    });
    expect(response.status()).toBe(403);
  });
});

test.describe('admin business flows', () => {
  test.skip(
    !process.env.ADMIN_EMAIL || !process.env.ADMIN_PASSWORD,
    'requires ADMIN_EMAIL/ADMIN_PASSWORD for a seeded admin account — not something this suite can self-register',
  );

  test('an admin can queue and observe an embeddings backfill job', async ({ request }) => {
    const loginResponse = await request.post('/api/v1/auth/login', {
      data: { email: process.env.ADMIN_EMAIL, password: process.env.ADMIN_PASSWORD },
    });
    expect(loginResponse.status()).toBe(200);
    const { accessToken } = await loginResponse.json();
    const headers = authHeaders(accessToken);

    const queueResponse = await request.post('/api/v1/admin/embeddings/backfill', { headers });
    expect([202, 409]).toContain(queueResponse.status());

    const jobsResponse = await request.get('/api/v1/admin/embeddings/jobs', { headers, params: { limit: '5' } });
    expect(jobsResponse.status()).toBe(200);
  });

  test('an admin can list pending returns', async ({ request }) => {
    const loginResponse = await request.post('/api/v1/auth/login', {
      data: { email: process.env.ADMIN_EMAIL, password: process.env.ADMIN_PASSWORD },
    });
    const { accessToken } = await loginResponse.json();

    const response = await request.get('/api/v1/admin/orders/pending', { headers: authHeaders(accessToken) });
    expect(response.status()).toBe(200);
  });
});
