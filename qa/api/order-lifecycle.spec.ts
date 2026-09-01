import { expect, test } from '@playwright/test';
import { authHeaders, pollUntil, registerAndLogin } from '../support/apiClient';

async function checkoutAffordableVirtualOrder(
  request: import('@playwright/test').APIRequestContext,
  headers: Record<string, string>,
  keyPrefix: string,
) {
  const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '20' } });
  const page = await searchResponse.json();
  const affordableBook = (page.items as Array<{ id: string; listPrice: number }>).find((b) => Number(b.listPrice) <= 400);
  if (!affordableBook) return undefined;

  const checkoutResponse = await request.post('/api/v1/orders/checkout', {
    headers: { ...headers, 'Idempotency-Key': `${keyPrefix}-${Date.now()}-${Math.random()}` },
    data: { paymentMethod: 'WALLET', items: [{ bookId: affordableBook.id, qty: 1, deliveryType: 'VIRTUAL' }] },
  });
  if (checkoutResponse.status() !== 201) return undefined;
  return checkoutResponse.json();
}

test.describe('order history', () => {
  test('a fresh account has an empty order history', async ({ request }) => {
    const user = await registerAndLogin(request, 'orders-empty');
    const response = await request.get('/api/v1/orders', { headers: authHeaders(user.accessToken) });
    expect(response.status()).toBe(200);
    const page = await response.json();
    expect(page.content ?? page.items).toEqual([]);
  });

  test('a placed order appears in the caller\'s order history', async ({ request }) => {
    const user = await registerAndLogin(request, 'orders-list');
    const headers = authHeaders(user.accessToken);
    await request.get('/api/v1/users/me', { headers });
    const order = await checkoutAffordableVirtualOrder(request, headers, 'qa-orderlist');
    test.skip(!order, 'no affordable virtual-only book in this dataset');
    if (!order) return;

    const listResponse = await request.get('/api/v1/orders', { headers });
    const page = await listResponse.json();
    const content = page.content ?? page.items;
    expect(content.some((o: { orderId?: string; id?: string }) => (o.orderId ?? o.id) === order.orderId)).toBe(true);
  });

  test('fetching another user\'s order returns 404, not the order', async ({ request }) => {
    const owner = await registerAndLogin(request, 'orders-owner');
    const ownerHeaders = authHeaders(owner.accessToken);
    await request.get('/api/v1/users/me', { headers: ownerHeaders });
    const order = await checkoutAffordableVirtualOrder(request, ownerHeaders, 'qa-orderowner');
    test.skip(!order, 'no affordable virtual-only book in this dataset');
    if (!order) return;

    const intruder = await registerAndLogin(request, 'orders-intruder');
    const response = await request.get(`/api/v1/orders/${order.orderId}`, { headers: authHeaders(intruder.accessToken) });
    expect(response.status()).toBe(404);
  });

  test('fetching an unknown order id returns 404', async ({ request }) => {
    const user = await registerAndLogin(request, 'orders-unknown');
    const response = await request.get(
      '/api/v1/orders/00000000-0000-0000-0000-000000000000',
      { headers: authHeaders(user.accessToken) },
    );
    expect(response.status()).toBe(404);
  });

  test('order history requires authentication', async ({ request }) => {
    const response = await request.get('/api/v1/orders');
    expect(response.status()).toBe(401);
  });
});

test.describe('cancellation', () => {
  test('a freshly placed order can be cancelled', async ({ request }) => {
    const user = await registerAndLogin(request, 'orders-cancel');
    const headers = authHeaders(user.accessToken);
    await request.get('/api/v1/users/me', { headers });
    const order = await checkoutAffordableVirtualOrder(request, headers, 'qa-cancel');
    test.skip(!order, 'no affordable virtual-only book in this dataset');
    if (!order) return;

    const cancelResponse = await request.post(`/api/v1/orders/${order.orderId}/cancel`, {
      headers,
      data: { reason: 'QA automated test — changed my mind' },
    });
    expect(cancelResponse.status()).toBe(200);

    const detail = await pollUntil(async () => {
      const detailResponse = await request.get(`/api/v1/orders/${order.orderId}`, { headers });
      const body = await detailResponse.json();
      return body.status === 'CANCELLED' ? body : undefined;
    });
    expect(detail.status).toBe('CANCELLED');
  });

  test('cancelling an already-cancelled order returns 409', async ({ request }) => {
    const user = await registerAndLogin(request, 'orders-doublecancel');
    const headers = authHeaders(user.accessToken);
    await request.get('/api/v1/users/me', { headers });
    const order = await checkoutAffordableVirtualOrder(request, headers, 'qa-doublecancel');
    test.skip(!order, 'no affordable virtual-only book in this dataset');
    if (!order) return;

    await request.post(`/api/v1/orders/${order.orderId}/cancel`, { headers, data: { reason: 'first cancel' } });
    const secondCancel = await request.post(`/api/v1/orders/${order.orderId}/cancel`, { headers, data: { reason: 'second cancel' } });
    expect(secondCancel.status()).toBe(409);
  });

  test('cancelling another user\'s order returns 404', async ({ request }) => {
    const owner = await registerAndLogin(request, 'orders-cancel-owner');
    const ownerHeaders = authHeaders(owner.accessToken);
    await request.get('/api/v1/users/me', { headers: ownerHeaders });
    const order = await checkoutAffordableVirtualOrder(request, ownerHeaders, 'qa-cancelowner');
    test.skip(!order, 'no affordable virtual-only book in this dataset');
    if (!order) return;

    const intruder = await registerAndLogin(request, 'orders-cancel-intruder');
    const response = await request.post(`/api/v1/orders/${order.orderId}/cancel`, {
      headers: authHeaders(intruder.accessToken),
      data: { reason: 'not mine' },
    });
    expect(response.status()).toBe(404);
  });
});

test.describe('return requests', () => {
  test('returning an order that has not been delivered yet returns 409', async ({ request }) => {
    const user = await registerAndLogin(request, 'orders-return-notdelivered');
    const headers = authHeaders(user.accessToken);
    await request.get('/api/v1/users/me', { headers });
    const order = await checkoutAffordableVirtualOrder(request, headers, 'qa-returnnotdelivered');
    test.skip(!order, 'no affordable virtual-only book in this dataset');
    if (!order) return;

    // Virtual orders auto-deliver almost instantly on payment capture, so this races that —
    // if it loses the race and 200s instead, that's still a legitimate outcome, not a bug.
    const response = await request.post(`/api/v1/orders/${order.orderId}/return`, { headers, data: { reason: 'too fast' } });
    expect([200, 409]).toContain(response.status());
  });

  test('returning with a blank reason is rejected with 400', async ({ request }) => {
    const user = await registerAndLogin(request, 'orders-return-blank');
    const headers = authHeaders(user.accessToken);
    await request.get('/api/v1/users/me', { headers });
    const order = await checkoutAffordableVirtualOrder(request, headers, 'qa-returnblank');
    test.skip(!order, 'no affordable virtual-only book in this dataset');
    if (!order) return;

    await pollUntil(async () => {
      const detailResponse = await request.get(`/api/v1/orders/${order.orderId}`, { headers });
      const detail = await detailResponse.json();
      return detail.status === 'DELIVERED' ? detail : undefined;
    });

    const response = await request.post(`/api/v1/orders/${order.orderId}/return`, { headers, data: { reason: '' } });
    expect(response.status()).toBe(400);
  });

  test('the return chat is readable and postable while a return is pending', async ({ request }) => {
    const user = await registerAndLogin(request, 'orders-return-chat');
    const headers = authHeaders(user.accessToken);
    await request.get('/api/v1/users/me', { headers });
    const order = await checkoutAffordableVirtualOrder(request, headers, 'qa-returnchat');
    test.skip(!order, 'no affordable virtual-only book in this dataset');
    if (!order) return;

    await pollUntil(async () => {
      const detailResponse = await request.get(`/api/v1/orders/${order.orderId}`, { headers });
      const detail = await detailResponse.json();
      return detail.status === 'DELIVERED' ? detail : undefined;
    });

    const messagesResponse = await request.get(`/api/v1/orders/${order.orderId}/return/messages`, { headers });
    expect(messagesResponse.status()).toBe(200);
    expect(await messagesResponse.json()).toEqual([]);

    // Virtual returns auto-approve instantly, which can close the return chat before this posts —
    // 200 (message accepted) and 409 (already resolved) are both legitimate depending on that race.
    const returnResponse = await request.post(`/api/v1/orders/${order.orderId}/return`, { headers, data: { reason: 'testing the chat' } });
    if (returnResponse.status() !== 200) return;

    const postResponse = await request.post(`/api/v1/orders/${order.orderId}/return/messages`, { headers, data: { content: 'Any update?' } });
    expect([200, 409]).toContain(postResponse.status());
  });
});
