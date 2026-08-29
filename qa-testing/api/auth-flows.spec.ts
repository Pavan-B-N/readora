import { expect, test } from '@playwright/test';
import { registerAndLogin, uniqueEmail } from '../support/apiClient';

test.describe('registration validation', () => {
  test('registering the same email twice is rejected with 409', async ({ request }) => {
    const email = uniqueEmail('dup');
    const password = 'correct-horse-battery-staple';

    const first = await request.post('/api/v1/auth/register', { data: { email, password, fullName: 'First' } });
    expect(first.status()).toBe(201);

    const second = await request.post('/api/v1/auth/register', { data: { email, password, fullName: 'Second' } });
    expect(second.status()).toBe(409);
  });

  test('a password below the 10-character minimum is rejected with 400', async ({ request }) => {
    const response = await request.post('/api/v1/auth/register', {
      data: { email: uniqueEmail('shortpw'), password: 'short1', fullName: 'QA' },
    });
    expect(response.status()).toBe(400);
  });

  test('a malformed email is rejected with 400', async ({ request }) => {
    const response = await request.post('/api/v1/auth/register', {
      data: { email: 'not-an-email', password: 'correct-horse-battery-staple', fullName: 'QA' },
    });
    expect(response.status()).toBe(400);
  });

  test('a missing fullName is rejected with 400', async ({ request }) => {
    const response = await request.post('/api/v1/auth/register', {
      data: { email: uniqueEmail('nofullname'), password: 'correct-horse-battery-staple' },
    });
    expect(response.status()).toBe(400);
  });
});

test.describe('login', () => {
  test('an unknown email is rejected with 401', async ({ request }) => {
    const response = await request.post('/api/v1/auth/login', {
      data: { email: uniqueEmail('never-registered'), password: 'whatever-not-real-12345' },
    });
    expect(response.status()).toBe(401);
  });

  test('the wrong password for a real account is rejected with 401', async ({ request }) => {
    const email = uniqueEmail('wrongpw');
    const password = 'correct-horse-battery-staple';
    await request.post('/api/v1/auth/register', { data: { email, password, fullName: 'QA' } });

    const response = await request.post('/api/v1/auth/login', { data: { email, password: 'definitely-wrong-password' } });
    expect(response.status()).toBe(401);
  });
});

test.describe('token lifecycle', () => {
  test('a refresh token exchanges for a new access/refresh pair', async ({ request }) => {
    const user = await registerAndLogin(request, 'refresh');

    const refreshResponse = await request.post('/api/v1/auth/refresh', { data: { refreshToken: user.refreshToken } });
    expect(refreshResponse.status()).toBe(200);

    const refreshed = await refreshResponse.json();
    expect(refreshed.accessToken).toBeTruthy();
    expect(refreshed.refreshToken).toBeTruthy();
    expect(refreshed.refreshToken).not.toBe(user.refreshToken);

    const meResponse = await request.get('/api/v1/users/me', { headers: { Authorization: `Bearer ${refreshed.accessToken}` } });
    expect(meResponse.status()).toBe(200);
  });

  test('reusing an already-rotated refresh token is rejected with 401', async ({ request }) => {
    const user = await registerAndLogin(request, 'reuse');

    const firstRefresh = await request.post('/api/v1/auth/refresh', { data: { refreshToken: user.refreshToken } });
    expect(firstRefresh.status()).toBe(200);

    // The original refresh token was consumed by the exchange above — presenting it again is reuse.
    const replay = await request.post('/api/v1/auth/refresh', { data: { refreshToken: user.refreshToken } });
    expect(replay.status()).toBe(401);
  });

  test('logout revokes the refresh token', async ({ request }) => {
    const user = await registerAndLogin(request, 'logout');

    const logoutResponse = await request.post('/api/v1/auth/logout', { data: { refreshToken: user.refreshToken } });
    expect(logoutResponse.status()).toBe(204);

    const refreshAfterLogout = await request.post('/api/v1/auth/refresh', { data: { refreshToken: user.refreshToken } });
    expect(refreshAfterLogout.status()).toBe(401);
  });

  test('an unknown refresh token is rejected with 401', async ({ request }) => {
    const response = await request.post('/api/v1/auth/refresh', { data: { refreshToken: 'not-a-real-refresh-token' } });
    expect(response.status()).toBe(401);
  });
});

test.describe('protected route access', () => {
  test('a protected route without a token returns 401', async ({ request }) => {
    const response = await request.get('/api/v1/users/me');
    expect(response.status()).toBe(401);
  });

  test('a protected route with a garbage bearer token returns 401', async ({ request }) => {
    const response = await request.get('/api/v1/users/me', { headers: { Authorization: 'Bearer not-a-real-jwt' } });
    expect(response.status()).toBe(401);
  });
});
