import { apiClient } from './client';
import type { EmbeddingJob } from '@/types/embeddings';

/** Queues a backfill. Returns the created job — the work runs asynchronously off Kafka. */
export async function queueBackfill(): Promise<EmbeddingJob> {
  const response = await apiClient.post<EmbeddingJob>('/api/v1/admin/embeddings/backfill');
  return response.data;
}

export async function listJobs(limit = 20): Promise<EmbeddingJob[]> {
  const response = await apiClient.get<EmbeddingJob[]>('/api/v1/admin/embeddings/jobs', {
    params: { limit },
  });
  return response.data;
}

export async function getJob(jobId: string): Promise<EmbeddingJob> {
  const response = await apiClient.get<EmbeddingJob>(`/api/v1/admin/embeddings/jobs/${jobId}`);
  return response.data;
}
