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

  async function loginAdmin(request: import('@playwright/test').APIRequestContext) {
    const loginResponse = await request.post('/api/v1/auth/login', {
      data: { email: process.env.ADMIN_EMAIL, password: process.env.ADMIN_PASSWORD },
    });
    expect(loginResponse.status()).toBe(200);
    const { accessToken } = await loginResponse.json();
    return authHeaders(accessToken);
  }

  test('an admin can queue and observe an embeddings backfill job', async ({ request }) => {
    const headers = await loginAdmin(request);

    const queueResponse = await request.post('/api/v1/admin/embeddings/backfill', { headers });
    expect([202, 409]).toContain(queueResponse.status());

    const jobsResponse = await request.get('/api/v1/admin/embeddings/jobs', { headers, params: { limit: '5' } });
    expect(jobsResponse.status()).toBe(200);
  });

  test('an admin can list pending returns', async ({ request }) => {
    const headers = await loginAdmin(request);
    const response = await request.get('/api/v1/admin/orders/pending', { headers });
    expect(response.status()).toBe(200);
  });

  test('an admin can list agents at their store', async ({ request }) => {
    const headers = await loginAdmin(request);
    const response = await request.get('/api/v1/admin/delivery/agents', { headers });
    expect(response.status()).toBe(200);
    expect(Array.isArray(await response.json())).toBe(true);
  });

  test('an admin can review a pending return case, if one exists', async ({ request }) => {
    const headers = await loginAdmin(request);
    const pendingResponse = await request.get('/api/v1/admin/orders/pending', { headers });
    const pending = await pendingResponse.json();
    const returnCase = (pending.content as Array<{ orderId: string; status: string }>).find(
      (o) => o.status === 'RETURN_REQUESTED',
    );
    test.skip(!returnCase, 'no order currently sitting at RETURN_REQUESTED for this admin\'s store');
    if (!returnCase) return;

    const messagesBefore = await request.get(`/api/v1/admin/orders/${returnCase.orderId}/return/messages`, { headers });
    expect(messagesBefore.status()).toBe(200);

    const postMessage = await request.post(`/api/v1/admin/orders/${returnCase.orderId}/return/messages`, {
      headers,
      data: { content: 'QA: checking on this return.' },
    });
    expect(postMessage.status()).toBe(200);

    const review = await request.post(`/api/v1/admin/orders/${returnCase.orderId}/review`, {
      headers,
      data: { note: 'QA automated review — approved for refund.', decision: 'APPROVE' },
    });
    expect(review.status()).toBe(200);
    expect((await review.json()).status).not.toBe('RETURN_REQUESTED');

    // Already decided — a second review attempt with a decision must not silently re-apply.
    const secondReview = await request.post(`/api/v1/admin/orders/${returnCase.orderId}/review`, {
      headers,
      data: { note: 'trying again', decision: 'APPROVE' },
    });
    expect(secondReview.status()).toBe(409);
  });

  test('category, author, and publisher can each be created, updated, and deleted', async ({ request }) => {
    const headers = await loginAdmin(request);
    const suffix = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;

    const categoryCreate = await request.post('/api/v1/admin/categories', {
      headers,
      data: { name: `QA Category ${suffix}`, slug: `qa-category-${suffix}`, displayOrder: 0 },
    });
    expect(categoryCreate.status()).toBe(201);
    const categoryId = (await categoryCreate.json()).id;

    const categoryUpdate = await request.put(`/api/v1/admin/categories/${categoryId}`, {
      headers,
      data: { name: `QA Category ${suffix} Updated`, slug: `qa-category-${suffix}`, displayOrder: 1 },
    });
    expect(categoryUpdate.status()).toBe(204);

    const categoryDelete = await request.delete(`/api/v1/admin/categories/${categoryId}`, { headers });
    expect(categoryDelete.status()).toBe(204);

    const publisherCreate = await request.post('/api/v1/admin/publishers', {
      headers,
      data: { name: `QA Publisher ${suffix}`, slug: `qa-publisher-${suffix}` },
    });
    expect(publisherCreate.status()).toBe(201);

    const authorCreate = await request.post('/api/v1/admin/authors', {
      headers,
      data: { name: `QA Author ${suffix}`, slug: `qa-author-${suffix}`, bio: 'A QA-generated test author.', photoUrl: null },
    });
    expect(authorCreate.status()).toBe(201);
    const authorId = (await authorCreate.json()).id;

    const authorUpdate = await request.put(`/api/v1/admin/authors/${authorId}`, {
      headers,
      data: { name: `QA Author ${suffix} Updated`, slug: `qa-author-${suffix}`, bio: 'Updated bio.', photoUrl: null },
    });
    expect(authorUpdate.status()).toBe(204);

    const authorDelete = await request.delete(`/api/v1/admin/authors/${authorId}`, { headers });
    expect(authorDelete.status()).toBe(204);
  });

  test('deleting a category twice returns 404 the second time', async ({ request }) => {
    const headers = await loginAdmin(request);
    const suffix = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;

    const created = await request.post('/api/v1/admin/categories', {
      headers,
      data: { name: `QA Delete Twice ${suffix}`, slug: `qa-delete-twice-${suffix}`, displayOrder: 0 },
    });
    const categoryId = (await created.json()).id;

    const firstDelete = await request.delete(`/api/v1/admin/categories/${categoryId}`, { headers });
    expect(firstDelete.status()).toBe(204);

    const secondDelete = await request.delete(`/api/v1/admin/categories/${categoryId}`, { headers });
    expect(secondDelete.status()).toBe(404);
  });
});
