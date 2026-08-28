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

/** One book this job has embedded — see EmbeddingJob for the run-level aggregate. */
export interface EmbeddingJobBookLog {
  bookId: string;
  title: string;
  processedAt: string;
}
