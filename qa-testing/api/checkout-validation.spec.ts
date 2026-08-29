import { expect, test } from '@playwright/test';
import { authHeaders, registerAndLogin } from '../support/apiClient';

async function affordableVirtualBook(request: import('@playwright/test').APIRequestContext, maxPrice = 400) {
  const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '20' } });
  const page = await searchResponse.json();
  return (page.items as Array<{ id: string; listPrice: number }>).find((b) => Number(b.listPrice) <= maxPrice);
}

test.describe('checkout validation', () => {
  test('checkout without an Idempotency-Key header is rejected with 400', async ({ request }) => {
    const user = await registerAndLogin(request, 'checkout-noidem');
    const response = await request.post('/api/v1/orders/checkout', {
      headers: authHeaders(user.accessToken),
      data: { paymentMethod: 'WALLET', items: [] },
    });
    expect(response.status()).toBe(400);
  });

  test('checkout with an empty item list is rejected', async ({ request }) => {
    const user = await registerAndLogin(request, 'checkout-empty');
    const response = await request.post('/api/v1/orders/checkout', {
      headers: { ...authHeaders(user.accessToken), 'Idempotency-Key': `qa-empty-${Date.now()}` },
      data: { paymentMethod: 'WALLET', items: [] },
    });
    expect(response.status()).toBeGreaterThanOrEqual(400);
    expect(response.status()).toBeLessThan(500);
  });

  test('checkout referencing a nonexistent book returns 404', async ({ request }) => {
    const user = await registerAndLogin(request, 'checkout-badbook');
    const response = await request.post('/api/v1/orders/checkout', {
      headers: { ...authHeaders(user.accessToken), 'Idempotency-Key': `qa-badbook-${Date.now()}` },
      data: {
        paymentMethod: 'WALLET',
        items: [{ bookId: '00000000-0000-0000-0000-000000000000', qty: 1, deliveryType: 'VIRTUAL' }],
      },
    });
    expect(response.status()).toBe(404);
  });

  test('an unsupported payment method is rejected with 400', async ({ request }) => {
    const book = await affordableVirtualBook(request);
    test.skip(!book, 'no affordable virtual-only book in this dataset');
    if (!book) return;

    const user = await registerAndLogin(request, 'checkout-badpayment');
    const response = await request.post('/api/v1/orders/checkout', {
      headers: { ...authHeaders(user.accessToken), 'Idempotency-Key': `qa-badpayment-${Date.now()}` },
      data: { paymentMethod: 'BITCOIN', items: [{ bookId: book.id, qty: 1, deliveryType: 'VIRTUAL' }] },
    });
    expect(response.status()).toBe(400);
  });

  test('a WALLET checkout that exceeds the wallet balance is rejected with 402', async ({ request }) => {
    // The signup bonus is 500.00 — a virtual book priced well above that will always outrun it.
    const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '20' } });
    const page = await searchResponse.json();
    const expensiveBook = (page.items as Array<{ id: string; listPrice: number }>).find((b) => Number(b.listPrice) > 500);
    test.skip(!expensiveBook, 'no virtual-only book priced above the signup bonus in this dataset');
    if (!expensiveBook) return;

    const user = await registerAndLogin(request, 'checkout-insufficient');
    await request.get('/api/v1/users/me', { headers: authHeaders(user.accessToken) }); // provisions wallet

    const response = await request.post('/api/v1/orders/checkout', {
      headers: { ...authHeaders(user.accessToken), 'Idempotency-Key': `qa-insufficient-${Date.now()}` },
      data: { paymentMethod: 'WALLET', items: [{ bookId: expensiveBook.id, qty: 1, deliveryType: 'VIRTUAL' }] },
    });
    expect(response.status()).toBe(402);
  });

  test('a PHYSICAL item without a shippingAddress is rejected with 400', async ({ request }) => {
    const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'false', page: '0', size: '20' } });
    const page = await searchResponse.json();
    const physicalBook = (page.items as Array<{ id: string }>)[0];
    test.skip(!physicalBook, 'no book in this dataset to check out physically');
    if (!physicalBook) return;

    const user = await registerAndLogin(request, 'checkout-noaddress');
    const response = await request.post('/api/v1/orders/checkout', {
      headers: { ...authHeaders(user.accessToken), 'Idempotency-Key': `qa-noaddress-${Date.now()}` },
      data: { paymentMethod: 'WALLET', items: [{ bookId: physicalBook.id, qty: 1, deliveryType: 'PHYSICAL' }] },
    });
    expect(response.status()).toBe(400);
  });

  test('replaying the same Idempotency-Key returns the original order rather than creating a duplicate', async ({ request }) => {
    const book = await affordableVirtualBook(request);
    test.skip(!book, 'no affordable virtual-only book in this dataset');
    if (!book) return;

    const user = await registerAndLogin(request, 'checkout-idempotent');
    const headers = authHeaders(user.accessToken);
    await request.get('/api/v1/users/me', { headers }); // provisions wallet
    const idempotencyKey = `qa-idempotent-${Date.now()}-${Math.random()}`;
    const body = { paymentMethod: 'WALLET', items: [{ bookId: book.id, qty: 1, deliveryType: 'VIRTUAL' }] };

    const first = await request.post('/api/v1/orders/checkout', { headers: { ...headers, 'Idempotency-Key': idempotencyKey }, data: body });
    expect(first.status()).toBe(201);
    const firstOrder = await first.json();

    const replay = await request.post('/api/v1/orders/checkout', { headers: { ...headers, 'Idempotency-Key': idempotencyKey }, data: body });
    expect(replay.status()).toBe(201);
    const replayOrder = await replay.json();

    expect(replayOrder.orderId).toBe(firstOrder.orderId);
  });

  test('checkout requires authentication', async ({ request }) => {
    const response = await request.post('/api/v1/orders/checkout', {
      headers: { 'Idempotency-Key': `qa-noauth-${Date.now()}` },
      data: { paymentMethod: 'WALLET', items: [] },
    });
    expect(response.status()).toBe(401);
  });
});
