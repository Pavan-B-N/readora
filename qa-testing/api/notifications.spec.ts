import { expect, test } from '@playwright/test';
import { authHeaders, registerAndLogin } from '../support/apiClient';

test.describe('notifications', () => {
  test('a fresh account has no notifications and an unread count of 0', async ({ request }) => {
    const user = await registerAndLogin(request, 'notif-empty');
    const headers = authHeaders(user.accessToken);

    const listResponse = await request.get('/api/v1/notifications', { headers });
    expect(listResponse.status()).toBe(200);
    const page = await listResponse.json();
    expect(page.content ?? page.items).toEqual([]);

    const unreadResponse = await request.get('/api/v1/notifications/unread-count', { headers });
    expect(unreadResponse.status()).toBe(200);
    const unread = await unreadResponse.json();
    expect(Number(unread.unreadCount)).toBe(0);
  });

  test('marking an unknown notification as read returns 404', async ({ request }) => {
    const user = await registerAndLogin(request, 'notif-marknotfound');
    const response = await request.put(
      '/api/v1/notifications/00000000-0000-0000-0000-000000000000/read',
      { headers: authHeaders(user.accessToken) },
    );
    expect(response.status()).toBe(404);
  });

  test('mark-all-read succeeds even with nothing to mark', async ({ request }) => {
    const user = await registerAndLogin(request, 'notif-markall');
    const response = await request.put('/api/v1/notifications/read-all', { headers: authHeaders(user.accessToken) });
    expect(response.status()).toBeLessThan(300);
  });

  test('notifications require authentication', async ({ request }) => {
    const response = await request.get('/api/v1/notifications');
    expect(response.status()).toBe(401);
  });
});
