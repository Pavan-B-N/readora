import { expect, test } from '@playwright/test';

test.describe('delivery agent claim flow', () => {
  test.skip(
    !process.env.AGENT_EMAIL || !process.env.AGENT_PASSWORD,
    'requires AGENT_EMAIL/AGENT_PASSWORD for a seeded delivery-agent account — the DELIVERY_AGENT role has no public self-registration path',
  );

  test('an on-duty agent can see and claim an unassigned delivery from their store queue', async ({ request }) => {
    const loginResponse = await request.post('/api/v1/auth/login', {
      data: { email: process.env.AGENT_EMAIL, password: process.env.AGENT_PASSWORD },
    });
    expect(loginResponse.status()).toBe(200);
    const { accessToken } = await loginResponse.json();
    const headers = { Authorization: `Bearer ${accessToken}` };

    await request.put('/api/v1/delivery/me/duty', { headers, data: { onDuty: true } });

    const queueResponse = await request.get('/api/v1/delivery/queue', { headers });
    expect(queueResponse.status()).toBe(200);
    const queue = await queueResponse.json();

    test.skip(queue.length === 0, 'no UNASSIGNED physical deliveries currently queued for this agent\'s store');
    if (queue.length === 0) return;

    const claimResponse = await request.post(`/api/v1/delivery/${queue[0].id}/claim`, { headers });
    expect(claimResponse.status()).toBe(200);

    const claimed = await claimResponse.json();
    expect(claimed.status).toBe('ASSIGNED');
  });
});
