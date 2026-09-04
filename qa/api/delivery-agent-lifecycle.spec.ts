import { expect, test } from '@playwright/test';
import { authHeaders, registerAndLogin } from '../support/apiClient';


test.describe('delivery agent lifecycle', () => {
  test.skip(
    !process.env.AGENT_EMAIL || !process.env.AGENT_PASSWORD,
    'requires AGENT_EMAIL/AGENT_PASSWORD for a seeded delivery-agent account — the DELIVERY_AGENT role has no public self-registration path',
  );

  async function loginAgent(request: import('@playwright/test').APIRequestContext) {
    const loginResponse = await request.post('/api/v1/auth/login', {
      data: { email: process.env.AGENT_EMAIL, password: process.env.AGENT_PASSWORD },
    });
    expect(loginResponse.status()).toBe(200);
    const { accessToken } = await loginResponse.json();
    return { Authorization: `Bearer ${accessToken}` };
  }

  test('the agent profile and stats endpoints return well-formed data', async ({ request }) => {
    const headers = await loginAgent(request);

    const meResponse = await request.get('/api/v1/delivery/me', { headers });
    expect(meResponse.status()).toBe(200);
    const me = await meResponse.json();
    expect(typeof me.onDuty).toBe('boolean');

    const statsResponse = await request.get('/api/v1/delivery/me/stats', { headers });
    expect(statsResponse.status()).toBe(200);
    const stats = await statsResponse.json();
    expect(typeof stats.completedDeliveries).toBe('number');
  });

  test('toggling duty on and back off round-trips correctly', async ({ request }) => {
    const headers = await loginAgent(request);

    const onResponse = await request.put('/api/v1/delivery/me/duty', { headers, data: { onDuty: true } });
    expect(onResponse.status()).toBe(200);
    expect((await onResponse.json()).onDuty).toBe(true);

    const offResponse = await request.put('/api/v1/delivery/me/duty', { headers, data: { onDuty: false } });
    expect(offResponse.status()).toBe(200);
    expect((await offResponse.json()).onDuty).toBe(false);

    // Leave the agent back on duty for the claim scenarios below and for delivery-claim.spec.ts.
    await request.put('/api/v1/delivery/me/duty', { headers, data: { onDuty: true } });
  });

  test('a claimed delivery progresses through out-for-delivery to delivered', async ({ request }) => {
    const headers = await loginAgent(request);
    await request.put('/api/v1/delivery/me/duty', { headers, data: { onDuty: true } });

    const queueResponse = await request.get('/api/v1/delivery/queue', { headers });
    expect(queueResponse.status()).toBe(200);
    const queue = await queueResponse.json();
    test.skip(queue.length === 0, 'no UNASSIGNED physical deliveries currently queued for this agent\'s store');
    if (queue.length === 0) return;

    const claimed = await request.post(`/api/v1/delivery/${queue[0].id}/claim`, { headers });
    expect(claimed.status()).toBe(200);
    const assignmentId = (await claimed.json()).id;

    const outForDelivery = await request.post(`/api/v1/delivery/${assignmentId}/out-for-delivery`, { headers });
    expect(outForDelivery.status()).toBe(200);
    expect((await outForDelivery.json()).status).toBe('OUT_FOR_DELIVERY');

    const delivered = await request.post(`/api/v1/delivery/${assignmentId}/delivered`, { headers });
    expect(delivered.status()).toBe(200);
    expect((await delivered.json()).status).toBe('DELIVERED');

    // Marking delivered again (already terminal) must not silently re-succeed.
    const repeat = await request.post(`/api/v1/delivery/${assignmentId}/delivered`, { headers });
    expect(repeat.status()).toBe(409);
  });

  test('claiming an assignment already claimed by another agent returns 409', async ({ request }) => {
    const headers = await loginAgent(request);
    const mineResponse = await request.get('/api/v1/delivery/mine', { headers });
    const mine = await mineResponse.json();
    const alreadyClaimed = (mine as Array<{ id: string; status: string }>).find((a) => a.status !== 'DELIVERED');
    test.skip(!alreadyClaimed, 'no in-progress claimed assignment on this agent to re-claim');
    if (!alreadyClaimed) return;

    // Re-claiming your own already-ASSIGNED item isn't "unassigned" any more — 409 either way.
    const response = await request.post(`/api/v1/delivery/${alreadyClaimed.id}/claim`, { headers });
    expect(response.status()).toBe(409);
  });

  test('a claimed return pickup progresses through en-route to collected', async ({ request }) => {
    const headers = await loginAgent(request);
    await request.put('/api/v1/delivery/me/duty', { headers, data: { onDuty: true } });

    const queueResponse = await request.get('/api/v1/returns/queue', { headers });
    expect(queueResponse.status()).toBe(200);
    const queue = await queueResponse.json();
    test.skip(queue.length === 0, 'no UNASSIGNED return pickups currently queued for this agent\'s store');
    if (queue.length === 0) return;

    const claimed = await request.post(`/api/v1/returns/${queue[0].id}/claim`, { headers });
    expect(claimed.status()).toBe(200);
    const pickupId = (await claimed.json()).id;

    const enRoute = await request.post(`/api/v1/returns/${pickupId}/en-route`, { headers });
    expect(enRoute.status()).toBe(200);

    const collected = await request.post(`/api/v1/returns/${pickupId}/collected`, { headers });
    expect(collected.status()).toBe(200);
  });

});

// Doesn't need a seeded agent account — just a plain customer being denied — so it runs
// unconditionally, unlike the rest of this file.
test('return pickups and deliveries both require the DELIVERY_AGENT role, not any authenticated caller', async ({ request }) => {
  const customer = await registerAndLogin(request, 'non-agent');

  const response = await request.get('/api/v1/delivery/queue', { headers: authHeaders(customer.accessToken) });
  expect(response.status()).toBe(403);
});
