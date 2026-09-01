import { expect, test } from '@playwright/test';
import { authHeaders, pollUntil, registerAndLogin } from '../support/apiClient';

test('checkout with UPI authorizes immediately then settles to CAPTURED a few seconds later', async ({ request }) => {
  const user = await registerAndLogin(request, 'upi');
  const headers = authHeaders(user.accessToken);
  await request.get('/api/v1/users/me', { headers });

  const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '20' } });
  const page = await searchResponse.json();
  const affordableBook = (page.items as Array<{ id: string; listPrice: number }>).find((b) => Number(b.listPrice) <= 400);
  test.skip(!affordableBook, 'no virtual-only book priced low enough in this dataset');
  if (!affordableBook) return;

  const checkoutResponse = await request.post('/api/v1/orders/checkout', {
    headers: { ...headers, 'Idempotency-Key': `qa-upi-${Date.now()}-${Math.random()}` },
    data: {
      paymentMethod: 'UPI',
      upiId: 'qa-test@upi',
      items: [{ bookId: affordableBook.id, qty: 1, deliveryType: 'VIRTUAL' }],
    },
  });
  expect(checkoutResponse.status()).toBe(201);
  const order = await checkoutResponse.json();

  // UPI is authorized synchronously at checkout, then a server-side job (UpiSettlementJob)
  // captures it ~3s later — same simulated-provider delay the frontend's own "waiting for
  // payment" spinner is built around.
  const authorized = await pollUntil(async () => {
    const detailResponse = await request.get(`/api/v1/orders/${order.orderId}`, { headers });
    const detail = await detailResponse.json();
    return detail.payment?.status ? detail : undefined;
  });
  expect(['AUTHORIZED', 'CAPTURED']).toContain(authorized.payment.status);

  const captured = await pollUntil(
    async () => {
      const detailResponse = await request.get(`/api/v1/orders/${order.orderId}`, { headers });
      const detail = await detailResponse.json();
      return detail.payment?.status === 'CAPTURED' ? detail : undefined;
    },
    { attempts: 20, delayMs: 500 },
  );
  expect(captured.payment.status).toBe('CAPTURED');
});

test('checkout with UPI but no upiId is still accepted — upiId is a display nicety, not validated', async ({ request }) => {
  // Confirmed against OrderService: upiId is carried on the request DTO but never read by
  // checkout() — there's no @NotBlank on it and no cross-field check, unlike shippingAddress for
  // PHYSICAL items. This documents that actual (lenient) behavior rather than an assumed spec.
  const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '20' } });
  const page = await searchResponse.json();
  const affordableBook = (page.items as Array<{ id: string; listPrice: number }>).find((b) => Number(b.listPrice) <= 400);
  test.skip(!affordableBook, 'no virtual-only book priced low enough in this dataset');
  if (!affordableBook) return;

  const user = await registerAndLogin(request, 'upi-noid');
  const headers = authHeaders(user.accessToken);

  const response = await request.post('/api/v1/orders/checkout', {
    headers: { ...headers, 'Idempotency-Key': `qa-upi-noid-${Date.now()}` },
    data: { paymentMethod: 'UPI', items: [{ bookId: affordableBook.id, qty: 1, deliveryType: 'VIRTUAL' }] },
  });
  expect(response.status()).toBe(201);
});
