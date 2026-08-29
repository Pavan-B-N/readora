import { expect, test } from '@playwright/test';
import { authHeaders, registerAndLogin } from '../support/apiClient';

test('a new customer can register, log in, and fetch their own profile', async ({ request }) => {
  const user = await registerAndLogin(request, 'smoke');
  expect(user.accessToken).toBeTruthy();

  const meResponse = await request.get('/api/v1/users/me', { headers: authHeaders(user.accessToken) });
  expect(meResponse.status()).toBe(200);

  const me = await meResponse.json();
  expect(me.email).toBe(user.email);
});
