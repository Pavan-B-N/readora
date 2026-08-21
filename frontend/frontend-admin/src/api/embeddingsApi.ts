import { apiClient } from './client';
import type { EmbeddingJob, EmbeddingJobBookLog } from '@/types/embeddings';

/** Queues a backfill. Returns the created job — the work runs asynchronously off Kafka. */
export async function queueBackfill(): Promise<EmbeddingJob> {
  const response = await apiClient.post<EmbeddingJob>('/api/v1/admin/embeddings/backfill');
  return response.data;
}

/** Fetches the most recent embedding backfill jobs, newest first. */
export async function listJobs(limit = 20): Promise<EmbeddingJob[]> {
  const response = await apiClient.get<EmbeddingJob[]>('/api/v1/admin/embeddings/jobs', {
    params: { limit },
  });
  return response.data;
}

/** Fetches a single embedding job's status. */
export async function getJob(jobId: string): Promise<EmbeddingJob> {
  const response = await apiClient.get<EmbeddingJob>(`/api/v1/admin/embeddings/jobs/${jobId}`);
  return response.data;
}

/** Fetches the per-book processing log for an embedding job. */
export async function listJobBooks(jobId: string): Promise<EmbeddingJobBookLog[]> {
  const response = await apiClient.get<EmbeddingJobBookLog[]>(`/api/v1/admin/embeddings/jobs/${jobId}/books`);
  return response.data;
}
