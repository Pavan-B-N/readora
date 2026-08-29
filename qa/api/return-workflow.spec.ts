import { expect, test } from '@playwright/test';
import { authHeaders, pollUntil, registerAndLogin } from '../support/apiClient';

test('a virtual order can be returned and auto-approves straight through to a refund', async ({ request }) => {
  const user = await registerAndLogin(request, 'return');
  const headers = authHeaders(user.accessToken);

  await request.get('/api/v1/users/me', { headers }); // provisions the wallet + signup bonus

  const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '20' } });
  const page = await searchResponse.json();
  const affordableBook = (page.items as Array<{ id: string; listPrice: number }>).find((b) => Number(b.listPrice) <= 400);
  test.skip(!affordableBook, 'no virtual-only book priced low enough for the signup bonus to cover in this dataset');
  if (!affordableBook) return;

  const checkoutResponse = await request.post('/api/v1/orders/checkout', {
    headers: { ...headers, 'Idempotency-Key': `qa-return-${Date.now()}-${Math.random()}` },
    data: { paymentMethod: 'WALLET', items: [{ bookId: affordableBook.id, qty: 1, deliveryType: 'VIRTUAL' }] },
  });
  expect(checkoutResponse.status()).toBe(201);
  const order = await checkoutResponse.json();

  // A virtual order is auto-marked DELIVERED the instant its payment captures — no shipping step.
  await pollUntil(async () => {
    const detailResponse = await request.get(`/api/v1/orders/${order.orderId}`, { headers });
    const detail = await detailResponse.json();
    return detail.status === 'DELIVERED' ? detail : undefined;
  });

  const returnResponse = await request.post(`/api/v1/orders/${order.orderId}/return`, {
    headers,
    data: { reason: 'QA automated test — no longer needed' },
  });
  expect(returnResponse.status()).toBe(200);

  // Virtual-only orders skip admin review entirely and go straight to a refund — the only wait
  // here is payment-service completing the refund and commerce-service confirming it over Kafka.
  const finalOrder = await pollUntil(async () => {
    const detailResponse = await request.get(`/api/v1/orders/${order.orderId}`, { headers });
    const detail = await detailResponse.json();
    return detail.status === 'RETURNED' ? detail : undefined;
  });

  expect(finalOrder.status).toBe('RETURNED');
});

test.describe('physical return admin-review path', () => {
  test.skip(
    !process.env.ADMIN_EMAIL || !process.env.ADMIN_PASSWORD,
    'requires ADMIN_EMAIL/ADMIN_PASSWORD for a seeded admin account — not something this suite can self-register',
  );

  test('a physical return requires admin approval before refunding', async () => {
    // Deliberately not implemented: exercising this path also needs a delivered PHYSICAL order,
    // which itself needs a delivery agent (see delivery-claim.spec.ts) assigned to the same
    // store the order resolves to. Wire this up once seeded ADMIN_EMAIL/AGENT_EMAIL credentials
    // for a matching store are available — see qa/README or ask for the seeded test accounts.
    test.fixme();
  });
});
