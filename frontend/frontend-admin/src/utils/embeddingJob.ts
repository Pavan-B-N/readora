import type { EmbeddingJob } from '@/types/embeddings';

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

export function formatDuration(job: EmbeddingJob): string {
  if (!job.startedAt) return '—';
  const end = job.finishedAt ? new Date(job.finishedAt) : new Date();
  const seconds = Math.round((end.getTime() - new Date(job.startedAt).getTime()) / 1000);
  if (seconds < 60) return `${seconds}s`;
  return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
}
