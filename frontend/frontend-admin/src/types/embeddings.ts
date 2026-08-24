export type EmbeddingJobStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface EmbeddingJob {
  id: string;
  status: EmbeddingJobStatus;
  totalBooks: number;
  processedBooks: number;
  currentBookTitle: string | null;
  errorMessage: string | null;
  queuedAt: string;
  startedAt: string | null;
  finishedAt: string | null;
}
