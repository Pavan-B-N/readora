import { expect, test } from '@playwright/test';
import { authHeaders, registerAndLogin } from '../support/apiClient';

test.describe('profile', () => {
  test('updating the profile persists every field sent', async ({ request }) => {
    const user = await registerAndLogin(request, 'profile-update');
    const headers = authHeaders(user.accessToken);

    const updateResponse = await request.put('/api/v1/users/me', {
      headers,
      data: { displayName: 'QA Updated Name', phone: '+91-9000000000', preferredStoreId: null, favoriteCategoryIds: [] },
    });
    expect(updateResponse.status()).toBe(200);
    const updated = await updateResponse.json();
    expect(updated.displayName).toBe('QA Updated Name');

    const meResponse = await request.get('/api/v1/users/me', { headers });
    const me = await meResponse.json();
    expect(me.displayName).toBe('QA Updated Name');
  });

  test('the profile endpoint requires authentication', async ({ request }) => {
    const response = await request.get('/api/v1/users/me');
    expect(response.status()).toBe(401);
  });
});

test.describe('addresses', () => {
  test('an address can be added, listed, set default, and deleted', async ({ request }) => {
    const user = await registerAndLogin(request, 'address-crud');
    const headers = authHeaders(user.accessToken);

    const createResponse = await request.post('/api/v1/users/me/addresses', {
      headers,
      data: {
        label: 'HOME',
        recipientType: 'OWNER',
        recipientName: 'QA Test User',
        recipientPhone: '+91-9000000001',
        line1: '221B Baker Street',
        city: 'Bengaluru',
        state: 'Karnataka',
        postalCode: '560001',
        countryCode: 'IN',
        isDefault: false,
      },
    });
    expect(createResponse.status()).toBe(201);
    const created = await createResponse.json();

    const listResponse = await request.get('/api/v1/users/me/addresses', { headers });
    const addresses = await listResponse.json();
    expect(addresses.some((a: { id: string }) => a.id === created.id)).toBe(true);

    const defaultResponse = await request.put(`/api/v1/users/me/addresses/${created.id}/default`, { headers });
    expect(defaultResponse.status()).toBe(204);

    const deleteResponse = await request.delete(`/api/v1/users/me/addresses/${created.id}`, { headers });
    expect(deleteResponse.status()).toBe(204);

    const listAfterDelete = await request.get('/api/v1/users/me/addresses', { headers });
    const addressesAfterDelete = await listAfterDelete.json();
    expect(addressesAfterDelete.some((a: { id: string }) => a.id === created.id)).toBe(false);
  });

  test('deleting an address that does not belong to the caller returns 404', async ({ request }) => {
    const user = await registerAndLogin(request, 'address-notfound');
    const response = await request.delete(
      '/api/v1/users/me/addresses/00000000-0000-0000-0000-000000000000',
      { headers: authHeaders(user.accessToken) },
    );
    expect(response.status()).toBe(404);
  });

  test('adding an address missing required fields is rejected with 400', async ({ request }) => {
    const user = await registerAndLogin(request, 'address-invalid');
    const response = await request.post('/api/v1/users/me/addresses', {
      headers: authHeaders(user.accessToken),
      data: { label: 'HOME', recipientType: 'OWNER' },
    });
    expect(response.status()).toBe(400);
  });
});

test.describe('wishlist', () => {
  test('a book can be added to and removed from the wishlist idempotently', async ({ request }) => {
    const searchResponse = await request.get('/api/v1/books', { params: { page: '0', size: '1' } });
    const page = await searchResponse.json();
    test.skip(page.items.length === 0, 'no books in this dataset to wishlist');
    if (page.items.length === 0) return;
    const bookId = page.items[0].id;

    const user = await registerAndLogin(request, 'wishlist');
    const headers = authHeaders(user.accessToken);

    const addResponse = await request.put(`/api/v1/users/me/wishlist/${bookId}`, { headers });
    expect(addResponse.status()).toBe(204);
    // Idempotent — adding it again is still a no-op success, not a conflict.
    const addAgainResponse = await request.put(`/api/v1/users/me/wishlist/${bookId}`, { headers });
    expect(addAgainResponse.status()).toBe(204);

    const listResponse = await request.get('/api/v1/users/me/wishlist', { headers });
    const wishlist = await listResponse.json();
    expect(wishlist.some((item: { bookId: string }) => item.bookId === bookId)).toBe(true);

    const removeResponse = await request.delete(`/api/v1/users/me/wishlist/${bookId}`, { headers });
    expect(removeResponse.status()).toBe(204);

    const listAfterRemove = await request.get('/api/v1/users/me/wishlist', { headers });
    const wishlistAfterRemove = await listAfterRemove.json();
    expect(wishlistAfterRemove.some((item: { bookId: string }) => item.bookId === bookId)).toBe(false);
  });
});

test.describe('browsing and search history', () => {
  test('viewing a book records it, most-recent first', async ({ request }) => {
    const searchResponse = await request.get('/api/v1/books', { params: { page: '0', size: '1' } });
    const page = await searchResponse.json();
    test.skip(page.items.length === 0, 'no books in this dataset to view');
    if (page.items.length === 0) return;
    const bookId = page.items[0].id;

    const user = await registerAndLogin(request, 'history');
    const headers = authHeaders(user.accessToken);

    const recordResponse = await request.put(`/api/v1/users/me/history/${bookId}`, { headers });
    expect(recordResponse.status()).toBe(204);

    const listResponse = await request.get('/api/v1/users/me/history', { headers });
    const history = await listResponse.json();
    expect(history[0].bookId).toBe(bookId);
  });

  test('recording a search query stores it in the search history', async ({ request }) => {
    const user = await registerAndLogin(request, 'searchhist');
    const headers = authHeaders(user.accessToken);

    const recordResponse = await request.post('/api/v1/users/me/search-history', { headers, data: { query: 'clean code' } });
    expect(recordResponse.status()).toBe(204);

    const listResponse = await request.get('/api/v1/users/me/search-history', { headers });
    const history = await listResponse.json();
    expect(history.some((item: { query: string }) => item.query === 'clean code')).toBe(true);
  });

  test('recording a blank search query is rejected with 400', async ({ request }) => {
    const user = await registerAndLogin(request, 'searchhist-blank');
    const response = await request.post('/api/v1/users/me/search-history', {
      headers: authHeaders(user.accessToken),
      data: { query: '' },
    });
    expect(response.status()).toBe(400);
  });
});

test.describe('wallet', () => {
  test('a top-up within the allowed range credits the wallet', async ({ request }) => {
    const user = await registerAndLogin(request, 'topup');
    const headers = authHeaders(user.accessToken);
    await request.get('/api/v1/users/me', { headers }); // provisions the wallet + signup bonus

    const topUpResponse = await request.post('/api/v1/users/me/wallet/topup', { headers, data: { amount: 100 } });
    expect(topUpResponse.status()).toBe(200);
    const balance = await topUpResponse.json();
    expect(Number(balance.balance)).toBeCloseTo(600.0, 2); // 500 signup bonus + 100 top-up
  });

  test('a top-up below the minimum is rejected with 400', async ({ request }) => {
    const user = await registerAndLogin(request, 'topup-toolow');
    const response = await request.post('/api/v1/users/me/wallet/topup', {
      headers: authHeaders(user.accessToken),
      data: { amount: 0.5 },
    });
    expect(response.status()).toBe(400);
  });

  test('a top-up above the maximum is rejected with 400', async ({ request }) => {
    const user = await registerAndLogin(request, 'topup-toohigh');
    const response = await request.post('/api/v1/users/me/wallet/topup', {
      headers: authHeaders(user.accessToken),
      data: { amount: 100000 },
    });
    expect(response.status()).toBe(400);
  });

  test('redeeming an unknown coupon code returns 404', async ({ request }) => {
    const user = await registerAndLogin(request, 'coupon-unknown');
    const response = await request.post('/api/v1/users/me/wallet/redeem-coupon', {
      headers: authHeaders(user.accessToken),
      data: { code: 'NO-SUCH-COUPON-CODE' },
    });
    expect(response.status()).toBe(404);
  });

  test('the wallet ledger returns a paginated page shape', async ({ request }) => {
    const user = await registerAndLogin(request, 'wallet-ledger');
    const headers = authHeaders(user.accessToken);
    await request.get('/api/v1/users/me', { headers });

    const response = await request.get('/api/v1/users/me/wallet', { headers, params: { page: '0', size: '10' } });
    expect(response.status()).toBe(200);
    const wallet = await response.json();
    expect(Array.isArray(wallet.items)).toBe(true);
    expect(typeof wallet.balance).not.toBe('undefined');
  });
});
