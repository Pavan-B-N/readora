import { expect, test } from '@playwright/test';
import { authHeaders, registerAndLogin } from '../support/apiClient';

test.describe('semantic search', () => {
  test('a normal query returns a well-formed result without authentication', async ({ request }) => {
    const response = await request.get('/api/v1/ai/search', { params: { q: 'a mystery novel', limit: '5' } });
    expect(response.status()).toBeLessThan(500);
    const body = await response.json();
    expect(Array.isArray(body.items)).toBe(true);
  });

  test('a limit above the server cap is clamped rather than erroring', async ({ request }) => {
    const response = await request.get('/api/v1/ai/search', { params: { q: 'fiction', limit: '500' } });
    expect(response.status()).toBeLessThan(500);
  });

  test('a missing query is rejected with 400', async ({ request }) => {
    const response = await request.get('/api/v1/ai/search', { params: { limit: '5' } });
    expect(response.status()).toBe(400);
  });
});

test.describe('shopping assistant chat', () => {
  test('the chat endpoint requires authentication', async ({ request }) => {
    const response = await request.post('/api/v1/ai/chat', { data: { message: 'recommend me a book' } });
    expect(response.status()).toBe(401);
  });

  test('an empty message is rejected with 400', async ({ request }) => {
    const user = await registerAndLogin(request, 'ai-chat-empty');
    const response = await request.post('/api/v1/ai/chat', {
      headers: authHeaders(user.accessToken),
      data: { message: '' },
    });
    expect(response.status()).toBe(400);
  });

  test('a message over the character limit is rejected with 400', async ({ request }) => {
    const user = await registerAndLogin(request, 'ai-chat-toolong');
    const response = await request.post('/api/v1/ai/chat', {
      headers: authHeaders(user.accessToken),
      data: { message: 'x'.repeat(5000) },
    });
    expect(response.status()).toBe(400);
  });
});

test.describe('conversations', () => {
  test('a fresh account has no saved conversations', async ({ request }) => {
    const user = await registerAndLogin(request, 'ai-conversations-empty');
    const response = await request.get('/api/v1/ai/conversations', { headers: authHeaders(user.accessToken) });
    expect(response.status()).toBe(200);
    const page = await response.json();
    expect((page.content ?? page.items).length).toBe(0);
  });

  test('fetching messages for an unknown conversation returns 404', async ({ request }) => {
    const user = await registerAndLogin(request, 'ai-conversations-unknown');
    const response = await request.get(
      '/api/v1/ai/conversations/00000000-0000-0000-0000-000000000000/messages',
      { headers: authHeaders(user.accessToken) },
    );
    expect(response.status()).toBe(404);
  });

  test('listing conversations requires authentication', async ({ request }) => {
    const response = await request.get('/api/v1/ai/conversations');
    expect(response.status()).toBe(401);
  });
});
