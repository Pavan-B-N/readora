import type { EmbeddingJob } from '@/types/embeddings';

/** Maps an embedding job status to the badge color variant used to display it. */
export function statusVariant(status: EmbeddingJob['status']) {
  switch (status) {
    case 'COMPLETED':
      return 'success' as const;
    case 'FAILED':
      return 'danger' as const;
    case 'RUNNING':
      return 'info' as const;
    default:
      return 'warning' as const;
  }
}

/** Formats how long a job has run (or ran) as a human-readable "Xm Ys" / "Xs" string. */
export function formatDuration(job: EmbeddingJob): string {
  if (!job.startedAt) return '—';
  const end = job.finishedAt ? new Date(job.finishedAt) : new Date();
  const seconds = Math.round((end.getTime() - new Date(job.startedAt).getTime()) / 1000);
  if (seconds < 60) return `${seconds}s`;
  return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
}
