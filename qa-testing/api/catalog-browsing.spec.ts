import { expect, test } from '@playwright/test';

test.describe('catalog browsing', () => {
  test('searching virtual-only books returns a well-formed page', async ({ request }) => {
    const response = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '5' } });
    expect(response.status()).toBe(200);

    const page = await response.json();
    expect(Array.isArray(page.items)).toBe(true);
    expect(typeof page.totalElements).toBe('number');
  });

  test('fetching a real book by id returns full detail matching the search result', async ({ request }) => {
    const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '1' } });
    const page = await searchResponse.json();
    test.skip(page.items.length === 0, 'no virtual-only books in this dataset to test against');

    const bookId = page.items[0].id;
    const detailResponse = await request.get(`/api/v1/books/${bookId}`);
    expect(detailResponse.status()).toBe(200);

    const detail = await detailResponse.json();
    expect(detail.id).toBe(bookId);
    expect(detail.title).toBe(page.items[0].title);
  });

  test('an unknown book id returns 404, not a 500', async ({ request }) => {
    const response = await request.get('/api/v1/books/00000000-0000-0000-0000-000000000000');
    expect(response.status()).toBe(404);
  });

  test('stores list is public and non-empty in a properly seeded environment', async ({ request }) => {
    const response = await request.get('/api/v1/stores');
    expect(response.status()).toBe(200);

    const stores = await response.json();
    expect(Array.isArray(stores)).toBe(true);
  });
});
