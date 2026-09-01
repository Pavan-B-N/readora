import { expect, test } from '@playwright/test';
import { authHeaders, registerAndLogin } from '../support/apiClient';

test.describe('reference data', () => {
  test('categories are listed and public', async ({ request }) => {
    const response = await request.get('/api/v1/categories');
    expect(response.status()).toBe(200);
    expect(Array.isArray(await response.json())).toBe(true);
  });

  test('authors are listed and public', async ({ request }) => {
    const response = await request.get('/api/v1/authors');
    expect(response.status()).toBe(200);
    expect(Array.isArray(await response.json())).toBe(true);
  });

  test('publishers are listed and public', async ({ request }) => {
    const response = await request.get('/api/v1/publishers');
    expect(response.status()).toBe(200);
    expect(Array.isArray(await response.json())).toBe(true);
  });
});

test.describe('search-adjacent endpoints', () => {
  test('typeahead suggestions return an array for a virtual-scoped query', async ({ request }) => {
    const response = await request.get('/api/v1/books/suggest', { params: { q: 'the', limit: '5' } });
    expect(response.status()).toBe(200);
    expect(Array.isArray(await response.json())).toBe(true);
  });

  test('batch lookup returns matches and silently skips unknown ids', async ({ request }) => {
    const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '1' } });
    const page = await searchResponse.json();
    test.skip(page.items.length === 0, 'no book in this dataset to batch-lookup');
    if (page.items.length === 0) return;
    const knownId = page.items[0].id;
    const unknownId = '00000000-0000-0000-0000-000000000000';

    const response = await request.get('/api/v1/books/batch', { params: { ids: `${knownId},${unknownId}` } });
    expect(response.status()).toBe(200);
    const results = await response.json();
    expect(results.some((b: { id: string }) => b.id === knownId)).toBe(true);
    expect(results.some((b: { id: string }) => b.id === unknownId)).toBe(false);
  });

  test('check-isbn reports true for an ISBN already in the catalogue', async ({ request }) => {
    const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '20' } });
    const page = await searchResponse.json();
    const bookWithIsbn = (page.items as Array<{ id: string; isbn13?: string }>).find((b) => b.isbn13);
    test.skip(!bookWithIsbn, 'no book with a known isbn13 in the search summary to test against');
    if (!bookWithIsbn) return;

    const response = await request.get('/api/v1/books/check-isbn', { params: { isbn: bookWithIsbn.isbn13! } });
    expect(response.status()).toBe(200);
    expect(await response.json()).toBe(true);
  });

  test('check-isbn reports false for an ISBN nobody has used', async ({ request }) => {
    const response = await request.get('/api/v1/books/check-isbn', { params: { isbn: `978-0-00-000000-${Date.now() % 10}` } });
    expect(response.status()).toBe(200);
    expect(await response.json()).toBe(false);
  });

  test('related titles for an unknown book id returns 404', async ({ request }) => {
    const response = await request.get('/api/v1/books/00000000-0000-0000-0000-000000000000/related');
    expect(response.status()).toBe(404);
  });

  test('anonymous callers get an empty purchased/library/recommended rail rather than a 401', async ({ request }) => {
    const purchased = await request.get('/api/v1/books/purchased');
    expect(purchased.status()).toBe(200);
    expect(await purchased.json()).toEqual([]);

    const library = await request.get('/api/v1/books/library');
    expect(library.status()).toBe(200);
    expect(await library.json()).toEqual([]);

    const recommended = await request.get('/api/v1/books/recommended');
    expect(recommended.status()).toBe(200);
    expect(await recommended.json()).toEqual([]);
  });
});

test.describe('reviews', () => {
  test('a review can be added, appears in the list, then deleted', async ({ request }) => {
    const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '1' } });
    const page = await searchResponse.json();
    test.skip(page.items.length === 0, 'no book in this dataset to review');
    if (page.items.length === 0) return;
    const bookId = page.items[0].id;

    const user = await registerAndLogin(request, 'review');
    const headers = authHeaders(user.accessToken);

    const upsertResponse = await request.post(`/api/v1/books/${bookId}/reviews`, {
      headers,
      data: { rating: 4, comment: 'Solid read, QA-generated review.' },
    });
    expect(upsertResponse.status()).toBe(200);

    const listResponse = await request.get(`/api/v1/books/${bookId}/reviews`, { params: { page: '0', size: '20' } });
    const reviewsPage = await listResponse.json();
    const reviews = reviewsPage.content ?? reviewsPage.items;
    expect(reviews.some((r: { rating: number }) => r.rating === 4)).toBe(true);

    const deleteResponse = await request.delete(`/api/v1/books/${bookId}/reviews/me`, { headers });
    expect(deleteResponse.status()).toBe(204);
  });

  test('an out-of-range rating is rejected with 400', async ({ request }) => {
    const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '1' } });
    const page = await searchResponse.json();
    test.skip(page.items.length === 0, 'no book in this dataset to review');
    if (page.items.length === 0) return;
    const bookId = page.items[0].id;

    const user = await registerAndLogin(request, 'review-invalid');
    const response = await request.post(`/api/v1/books/${bookId}/reviews`, {
      headers: authHeaders(user.accessToken),
      data: { rating: 10, comment: 'invalid rating' },
    });
    expect(response.status()).toBe(400);
  });

  test('reviewing requires authentication', async ({ request }) => {
    const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '1' } });
    const page = await searchResponse.json();
    test.skip(page.items.length === 0, 'no book in this dataset to review');
    if (page.items.length === 0) return;
    const bookId = page.items[0].id;

    const response = await request.post(`/api/v1/books/${bookId}/reviews`, { data: { rating: 5, comment: 'anon' } });
    expect(response.status()).toBe(401);
  });
});
