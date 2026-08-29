import { expect, test } from '@playwright/test';
import { authHeaders, registerAndLogin } from '../support/apiClient';

async function firstVirtualBook(request: import('@playwright/test').APIRequestContext) {
  const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '1' } });
  const page = await searchResponse.json();
  return page.items[0] as { id: string; listPrice: number } | undefined;
}

test.describe('cart', () => {
  test('a fresh account starts with an empty cart', async ({ request }) => {
    const user = await registerAndLogin(request, 'cart-empty');
    const response = await request.get('/api/v1/cart', { headers: authHeaders(user.accessToken) });
    expect(response.status()).toBe(200);

    const cart = await response.json();
    expect(cart.items).toEqual([]);
  });

  test('adding an item is reflected in the cart summary and full cart', async ({ request }) => {
    const book = await firstVirtualBook(request);
    test.skip(!book, 'no virtual-only book in this dataset to add to the cart');
    if (!book) return;

    const user = await registerAndLogin(request, 'cart-add');
    const headers = authHeaders(user.accessToken);

    const addResponse = await request.post('/api/v1/cart/items', {
      headers,
      data: { bookId: book.id, qty: 1, deliveryType: 'VIRTUAL' },
    });
    expect(addResponse.status()).toBe(200);
    const summary = await addResponse.json();
    expect(summary.itemCount).toBeGreaterThanOrEqual(1);

    const cartResponse = await request.get('/api/v1/cart', { headers });
    const cart = await cartResponse.json();
    expect(cart.items.some((item: { bookId: string }) => item.bookId === book.id)).toBe(true);
  });

  test('adding the same item twice increments its quantity rather than duplicating the line', async ({ request }) => {
    const book = await firstVirtualBook(request);
    test.skip(!book, 'no virtual-only book in this dataset to add to the cart');
    if (!book) return;

    const user = await registerAndLogin(request, 'cart-increment');
    const headers = authHeaders(user.accessToken);

    await request.post('/api/v1/cart/items', { headers, data: { bookId: book.id, qty: 1, deliveryType: 'VIRTUAL' } });
    await request.post('/api/v1/cart/items', { headers, data: { bookId: book.id, qty: 2, deliveryType: 'VIRTUAL' } });

    const cartResponse = await request.get('/api/v1/cart', { headers });
    const cart = await cartResponse.json();
    const line = cart.items.find((item: { bookId: string }) => item.bookId === book.id);
    expect(line.qty).toBe(3);
  });

  test('setting a line item to qty 0 removes it from the cart', async ({ request }) => {
    const book = await firstVirtualBook(request);
    test.skip(!book, 'no virtual-only book in this dataset to add to the cart');
    if (!book) return;

    const user = await registerAndLogin(request, 'cart-remove');
    const headers = authHeaders(user.accessToken);

    await request.post('/api/v1/cart/items', { headers, data: { bookId: book.id, qty: 1, deliveryType: 'VIRTUAL' } });

    const removeResponse = await request.put(`/api/v1/cart/items/${book.id}/VIRTUAL`, { headers, data: { qty: 0 } });
    expect(removeResponse.status()).toBe(200);

    const cartResponse = await request.get('/api/v1/cart', { headers });
    const cart = await cartResponse.json();
    expect(cart.items.some((item: { bookId: string }) => item.bookId === book.id)).toBe(false);
  });

  test('adding a nonexistent book returns 404, not a 500', async ({ request }) => {
    const user = await registerAndLogin(request, 'cart-badbook');
    const response = await request.post('/api/v1/cart/items', {
      headers: authHeaders(user.accessToken),
      data: { bookId: '00000000-0000-0000-0000-000000000000', qty: 1, deliveryType: 'VIRTUAL' },
    });
    expect(response.status()).toBe(404);
  });

  test('setting the quantity of a line item not in the cart returns 404', async ({ request }) => {
    const user = await registerAndLogin(request, 'cart-setmissing');
    const response = await request.put(
      '/api/v1/cart/items/00000000-0000-0000-0000-000000000000/VIRTUAL',
      { headers: authHeaders(user.accessToken), data: { qty: 1 } },
    );
    expect(response.status()).toBe(404);
  });

  test('the cart requires authentication', async ({ request }) => {
    const response = await request.get('/api/v1/cart');
    expect(response.status()).toBe(401);
  });
});
