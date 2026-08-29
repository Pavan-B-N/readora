import { expect, test } from '@playwright/test';
import { authHeaders, registerAndLogin } from '../support/apiClient';

const SEEDED_COUPON_CODE = 'WELCOME50';
const SEEDED_COUPON_AMOUNT = 50.0;

test.describe('coupon redemption', () => {
  test('redeeming a valid coupon credits the wallet by its amount', async ({ request }) => {
    const user = await registerAndLogin(request, 'coupon-redeem');
    const headers = authHeaders(user.accessToken);
    const meResponse = await request.get('/api/v1/users/me', { headers });
    const me = await meResponse.json();
    const balanceBefore = Number(me.wallet.balance);

    const response = await request.post('/api/v1/users/me/wallet/redeem-coupon', { headers, data: { code: SEEDED_COUPON_CODE } });
    test.skip(response.status() === 404, `seeded coupon ${SEEDED_COUPON_CODE} not present in this dataset`);
    expect(response.status()).toBe(200);

    const redemption = await response.json();
    expect(Number(redemption.creditedAmount)).toBeCloseTo(SEEDED_COUPON_AMOUNT, 2);
    expect(Number(redemption.balance)).toBeCloseTo(balanceBefore + SEEDED_COUPON_AMOUNT, 2);

    const walletResponse = await request.get('/api/v1/users/me/wallet', { headers, params: { page: '0', size: '5' } });
    const wallet = await walletResponse.json();
    expect(Number(wallet.balance)).toBeCloseTo(balanceBefore + SEEDED_COUPON_AMOUNT, 2);
    expect(wallet.items.some((item: { type: string }) => item.type === 'COUPON_REDEEMED')).toBe(true);
  });

  test('redeeming the same coupon twice from the same account is rejected with 409', async ({ request }) => {
    const user = await registerAndLogin(request, 'coupon-redeem-twice');
    const headers = authHeaders(user.accessToken);
    await request.get('/api/v1/users/me', { headers });

    const first = await request.post('/api/v1/users/me/wallet/redeem-coupon', { headers, data: { code: SEEDED_COUPON_CODE } });
    test.skip(first.status() === 404, `seeded coupon ${SEEDED_COUPON_CODE} not present in this dataset`);
    expect(first.status()).toBe(200);

    const second = await request.post('/api/v1/users/me/wallet/redeem-coupon', { headers, data: { code: SEEDED_COUPON_CODE } });
    expect(second.status()).toBe(409);
  });

  test('a coupon code is redeemable independently per account', async ({ request }) => {
    const userA = await registerAndLogin(request, 'coupon-a');
    const userB = await registerAndLogin(request, 'coupon-b');

    const first = await request.post('/api/v1/users/me/wallet/redeem-coupon', {
      headers: authHeaders(userA.accessToken),
      data: { code: 'READORA100' },
    });
    test.skip(first.status() === 404, 'seeded coupon READORA100 not present in this dataset');
    expect(first.status()).toBe(200);

    const second = await request.post('/api/v1/users/me/wallet/redeem-coupon', {
      headers: authHeaders(userB.accessToken),
      data: { code: 'READORA100' },
    });
    expect(second.status()).toBe(200);
  });
});
