import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { ArrowLeft, BookOpen, Sparkles } from 'lucide-react';
import { getJob, listJobBooks } from '@/api/embeddingsApi';
import type { EmbeddingJob, EmbeddingJobBookLog } from '@/types/embeddings';
import { formatDuration, statusVariant } from '@/utils/embeddingJob';
import { Card, CardHeader } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import { Spinner } from '@readora/shared-ui';
import styles from './EmbeddingJobDetailPage.module.css';

const POLL_INTERVAL_MS = 1500;

const ACTIVE_STATUSES = new Set(['QUEUED', 'RUNNING']);

export function EmbeddingJobDetailPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();
  const [job, setJob] = useState<EmbeddingJob | null>(null);
  const [books, setBooks] = useState<EmbeddingJobBookLog[]>([]);
  const [loading, setLoading] = useState(true);
  const pollRef = useRef<number | null>(null);

  const reload = async () => {
    if (!jobId) return;
    const [jobResult, booksResult] = await Promise.all([getJob(jobId), listJobBooks(jobId)]);
    setJob(jobResult);
    setBooks(booksResult);
    return jobResult;
  };

  useEffect(() => {
    reload().finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobId]);

  // Keep polling live while the run is still in flight; a finished run's book log is already complete.
  useEffect(() => {
    if (!job || !ACTIVE_STATUSES.has(job.status)) {
      if (pollRef.current) {
        clearInterval(pollRef.current);
        pollRef.current = null;
      }
      return;
    }

    pollRef.current = window.setInterval(reload, POLL_INTERVAL_MS);
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [job?.status]);

  if (loading) {
    return <Spinner />;
  }

  if (!job) {
    return <p style={{ color: 'var(--color-text-muted)' }}>No such backfill run.</p>;
  }

  const isActive = ACTIVE_STATUSES.has(job.status);
  const percent = job.totalBooks > 0 ? Math.min(100, Math.round((job.processedBooks / job.totalBooks) * 100)) : null;

  return (
    <div>
      <button type="button" className={styles.back} onClick={() => navigate(-1)}>
        <ArrowLeft size={15} />
        Back to run history
      </button>

      <div className={styles.header}>
        <h1 className={styles.title}>Backfill run</h1>
        <Badge variant={statusVariant(job.status)} dot pulse={isActive}>
          {job.status.charAt(0) + job.status.slice(1).toLowerCase()}
        </Badge>
        <span className={styles.jobId}>{job.id}</span>
      </div>

      <div className={styles.layout}>
        <Card>
          <CardHeader title="Summary" />
          <div className={styles.statsGrid}>
            <div className={styles.stat}>
              <span className={styles.statLabel}>Queued</span>
              <span className={styles.statValue}>{new Date(job.queuedAt).toLocaleString()}</span>
            </div>
            <div className={styles.stat}>
              <span className={styles.statLabel}>Duration</span>
              <span className={styles.statValue}>{formatDuration(job)}</span>
            </div>
            <div className={styles.stat}>
              <span className={styles.statLabel}>Books embedded</span>
              <span className={styles.statValue}>
                {job.processedBooks}
                {job.totalBooks > 0 ? ` / ${job.totalBooks}` : ''}
              </span>
            </div>
          </div>

          {isActive && (
            <div className={styles.progressWrap}>
              <div className={styles.progressTrack}>
                <div
                  className={[styles.progressBar, percent === null && styles.progressIndeterminate].filter(Boolean).join(' ')}
                  style={percent !== null ? { width: `${percent}%` } : undefined}
                />
              </div>
              <span className={styles.progressLabel}>
                {job.status === 'QUEUED' ? 'Waiting for a worker…' : percent !== null ? `${percent}%` : 'Embedding…'}
              </span>
            </div>
          )}

          {job.errorMessage && (
            <div className={styles.errorBox}>
              <strong>Failed:</strong> {job.errorMessage}
            </div>
          )}
        </Card>

        <Card flush>
          <div style={{ padding: 'var(--space-5) var(--space-5) 0' }}>
            <CardHeader
              title="Books processed"
              subtitle={isActive ? 'Live — updates as each batch finishes.' : `${books.length} book${books.length === 1 ? '' : 's'} total.`}
              actions={isActive && <Sparkles size={15} className={styles.liveIcon} />}
            />
          </div>

          {books.length === 0 ? (
            <p style={{ padding: 'var(--space-5)', color: 'var(--color-text-muted)' }}>
              {isActive ? 'Waiting for the first batch…' : 'No books were embedded in this run.'}
            </p>
          ) : (
            <div className={styles.bookList}>
              <AnimatePresence initial={false}>
                {books.map((book) => (
                  <motion.div
                    key={book.bookId}
                    className={styles.bookRow}
                    initial={{ opacity: 0, y: -6 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.18 }}
                  >
                    <span className={styles.bookIcon}>
                      <BookOpen size={13} />
                    </span>
                    <span className={styles.bookTitle}>{book.title}</span>
                    <span className={styles.bookTime}>{new Date(book.processedAt).toLocaleTimeString()}</span>
                  </motion.div>
                ))}
              </AnimatePresence>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
