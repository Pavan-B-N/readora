import { apiClient } from './client';

export async function triggerBackfill(): Promise<void> {
  await apiClient.post('/api/v1/admin/embeddings/backfill');
}
