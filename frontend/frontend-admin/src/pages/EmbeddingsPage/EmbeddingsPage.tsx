import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Sparkles, Info, History, Loader2, ChevronRight } from 'lucide-react';
import { listJobs, queueBackfill } from '@/api/embeddingsApi';
import type { EmbeddingJob } from '@/types/embeddings';
import { formatDuration, statusVariant } from '@/utils/embeddingJob';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Button } from '@/components/Button';
import { Badge } from '@/components/Badge';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@/components/EmptyState';
import { ROUTES } from '@/constants/routes';
import styles from './EmbeddingsPage.module.css';

const POLL_INTERVAL_MS = 1500;

export function EmbeddingsPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [jobs, setJobs] = useState<EmbeddingJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [queueing, setQueueing] = useState(false);
  const pollRef = useRef<number | null>(null);

  const activeJob = jobs.find((j) => j.status === 'QUEUED' || j.status === 'RUNNING') ?? null;

  const refresh = useCallback(async () => {
    const result = await listJobs(20);
    setJobs(result);
    return result;
  }, []);

  useEffect(() => {
    refresh().finally(() => setLoading(false));
  }, [refresh]);

  // Poll only while a job is in flight, so an idle page isn't hammering the API.
  useEffect(() => {
    if (!activeJob) {
      if (pollRef.current) {
        clearInterval(pollRef.current);
        pollRef.current = null;
      }
      return;
    }

    pollRef.current = window.setInterval(() => {
      refresh().then((result) => {
        const stillActive = result.some((j) => j.status === 'QUEUED' || j.status === 'RUNNING');
        if (!stillActive) {
          const latest = result[0];
          if (latest?.status === 'COMPLETED') {
            showToast(
              latest.totalBooks > 0
                ? `Backfill complete — ${latest.totalBooks} book${latest.totalBooks === 1 ? '' : 's'} embedded`
                : 'Backfill complete — everything was already up to date',
            );
          } else if (latest?.status === 'FAILED') {
            showToast('Backfill failed — see the history below', 'error');
          }
        }
      });
    }, POLL_INTERVAL_MS);

    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [activeJob, refresh, showToast]);

  const onQueue = async () => {
    setQueueing(true);
    try {
      await queueBackfill();
      showToast('Backfill queued');
      await refresh();
    } catch (error: unknown) {
      const status = (error as { response?: { status?: number } })?.response?.status;
      showToast(
        status === 409 ? 'A backfill is already running' : 'Could not queue the backfill',
        'error',
      );
      await refresh();
    } finally {
      setQueueing(false);
    }
  };

  const percent =
    activeJob && activeJob.totalBooks > 0
      ? Math.min(100, Math.round((activeJob.processedBooks / activeJob.totalBooks) * 100))
      : null;

  return (
    <div>
      <PageHeader
        title="Embeddings"
        subtitle="Vector index powering semantic search and recommendations."
      />

      <div className={styles.layout}>
        <Card>
          <CardHeader
            title="Vector index backfill"
            actions={
              activeJob && (
                <Badge variant="info" dot pulse>
                  {activeJob.status === 'QUEUED' ? 'Queued' : 'Running'}
                </Badge>
              )
            }
          />

          <div className={styles.explainer}>
            <Info size={16} className={styles.explainerIcon} />
            <span>
              Day-to-day changes re-embed automatically — saving a book publishes a{' '}
              <code>book.upserted</code> event that re-indexes just that title. Running a backfill
              only re-embeds books that are new or changed since their last embedding — it won't
              redo work that's already current. Use it to bootstrap a fresh vector store, recover
              from missed events, or after switching embedding models. The job runs asynchronously
              on a Kafka consumer, so you can leave this page.
            </span>
          </div>

          <div className={styles.runRow}>
            <Button onClick={onQueue} disabled={queueing || Boolean(activeJob)}>
              {activeJob ? <Loader2 size={15} className="spin" /> : <Sparkles size={15} />}
              {activeJob ? 'Backfill in progress…' : queueing ? 'Queueing…' : 'Run backfill'}
            </Button>
            {activeJob && (
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-subtle)' }}>
                Only one backfill can run at a time.
              </span>
            )}
          </div>

          {activeJob && (
            <div className={styles.progressWrap}>
              <div className={styles.progressHead}>
                <span className={styles.progressLabel}>
                  {activeJob.status === 'QUEUED' ? 'Waiting for a worker…' : 'Embedding books'}
                </span>
                <span className={styles.progressCount}>
                  {percent !== null
                    ? `${activeJob.processedBooks} / ${activeJob.totalBooks} · ${percent}%`
                    : `${activeJob.processedBooks} processed`}
                </span>
              </div>

              <div className={styles.progressTrack}>
                <div
                  className={[styles.progressBar, percent === null && styles.progressIndeterminate]
                    .filter(Boolean)
                    .join(' ')}
                  style={percent !== null ? { width: `${percent}%` } : undefined}
                />
              </div>

              {activeJob.currentBookTitle && (
                <div className={styles.currentBook}>
                  Last embedded:
                  <span className={styles.currentBookTitle}>{activeJob.currentBookTitle}</span>
                </div>
              )}
            </div>
          )}
        </Card>

        <Card flush>
          <div style={{ padding: 'var(--space-5) var(--space-5) 0' }}>
            <CardHeader title="Run history" subtitle="The last 20 backfills, newest first." />
          </div>

          {loading ? (
            <p style={{ padding: 'var(--space-5)', color: 'var(--color-text-muted)' }}>Loading…</p>
          ) : jobs.length === 0 ? (
            <EmptyState
              icon={History}
              title="No backfills yet"
              description="Once you run a full re-embed, each run is recorded here with its duration and outcome."
            />
          ) : (
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Status</th>
                  <th>Books</th>
                  <th>Duration</th>
                  <th>Queued</th>
                  <th>Detail</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {jobs.map((job) => (
                  <tr key={job.id} className={styles.row} onClick={() => navigate(ROUTES.embeddingJobDetail(job.id))}>
                    <td>
                      <Badge
                        variant={statusVariant(job.status)}
                        dot
                        pulse={job.status === 'RUNNING' || job.status === 'QUEUED'}
                      >
                        {job.status.charAt(0) + job.status.slice(1).toLowerCase()}
                      </Badge>
                    </td>
                    <td className={styles.numeric}>
                      {job.status === 'RUNNING'
                        ? `${job.processedBooks} / ${job.totalBooks || '?'}`
                        : job.status === 'COMPLETED'
                          ? job.totalBooks
                          : job.totalBooks || '—'}
                    </td>
                    <td className={styles.numeric}>{formatDuration(job)}</td>
                    <td className={styles.mono}>{new Date(job.queuedAt).toLocaleString()}</td>
                    <td>
                      {job.errorMessage ? (
                        <span className={styles.errorCell} title={job.errorMessage}>
                          {job.errorMessage}
                        </span>
                      ) : job.currentBookTitle ? (
                        <span className={styles.mono}>{job.currentBookTitle}</span>
                      ) : (
                        <span className={styles.mono}>—</span>
                      )}
                    </td>
                    <td>
                      <ChevronRight size={15} className={styles.chevron} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>
    </div>
  );
}
