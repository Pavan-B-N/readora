import { expect, test } from '@playwright/test';
import { authHeaders, pollUntil, registerAndLogin } from '../support/apiClient';

/** Mirrors WalletEventService.calculateCashback's tier table (services/user-service) exactly. */
function expectedCashback(orderAmount: number): number {
  const rate = orderAmount >= 5000 ? 0.05 : orderAmount >= 3000 ? 0.04 : orderAmount >= 1500 ? 0.03 : orderAmount >= 500 ? 0.02 : 0.01;
  return Math.round(orderAmount * rate * 100) / 100;
}

test('checkout with WALLET pays instantly and credits tiered cashback to the wallet', async ({ request }) => {
  const user = await registerAndLogin(request, 'checkout');
  const headers = authHeaders(user.accessToken);

  // GET /me provisions the wallet + signup bonus (500.00) lazily, on first access.
  const meResponse = await request.get('/api/v1/users/me', { headers });
  const me = await meResponse.json();
  expect(Number(me.wallet.balance)).toBeCloseTo(500.0, 2);

  // A virtual-only book keeps this test independent of any store/shipping-address setup.
  const searchResponse = await request.get('/api/v1/books', { params: { virtualOnly: 'true', page: '0', size: '20' } });
  const page = await searchResponse.json();
  const affordableBook = (page.items as Array<{ id: string; listPrice: number }>).find((b) => Number(b.listPrice) <= 400);
  test.skip(!affordableBook, 'no virtual-only book priced low enough for the signup bonus to cover in this dataset');
  if (!affordableBook) return;

  const checkoutResponse = await request.post('/api/v1/orders/checkout', {
    headers: { ...headers, 'Idempotency-Key': `qa-checkout-${Date.now()}-${Math.random()}` },
    data: {
      paymentMethod: 'WALLET',
      items: [{ bookId: affordableBook.id, qty: 1, deliveryType: 'VIRTUAL' }],
    },
  });
  expect(checkoutResponse.status()).toBe(201);
  const order = await checkoutResponse.json();
  expect(order.walletAmountUsed).toBeTruthy();

  const grandTotal = Number(order.grandTotal);

  // Payment capture is synchronous for WALLET but still travels order->payment-service over
  // Kafka before this GET reflects it — same eventual-consistency shape as everywhere else in
  // this system (e.g. the frontend's own UPI-settlement polling).
  const detail = await pollUntil(async () => {
    const detailResponse = await request.get(`/api/v1/orders/${order.orderId}`, { headers });
    const body = await detailResponse.json();
    return body.payment?.status === 'CAPTURED' ? body : undefined;
  });
  expect(detail.payment.status).toBe('CAPTURED');

  // Cashback is a second, independent Kafka consumer (WalletEventService) reacting to the same
  // PAYMENT_CAPTURED event payment-service just published — it can lag slightly behind the
  // order's own payment view above, so this polls separately rather than checking once.
  const cashbackTransaction = await pollUntil(async () => {
    const walletResponse = await request.get('/api/v1/users/me/wallet', { headers, params: { page: '0', size: '10' } });
    const wallet = await walletResponse.json();
    return (wallet.items as Array<{ type: string; orderId: string; amount: string }>).find(
      (item) => item.type === 'CASHBACK' && item.orderId === order.orderId,
    );
  });

  expect(Number(cashbackTransaction.amount)).toBeCloseTo(expectedCashback(grandTotal), 2);
});
