import { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import * as pdfjsLib from 'pdfjs-dist';
import type { PDFDocumentProxy } from 'pdfjs-dist';
import pdfWorkerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url';
import { ArrowLeft, ChevronLeft, ChevronRight, Lock } from 'lucide-react';
import { getVirtualContent } from '@/api/catalogApi';
import { Button } from '@/components/Button';
import styles from './VirtualReaderPage.module.css';

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorkerUrl;

export function VirtualReaderPage() {
  const { bookId } = useParams<{ bookId: string }>();
  const navigate = useNavigate();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const pdfRef = useRef<PDFDocumentProxy | null>(null);

  const [numPages, setNumPages] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!bookId) return;
    let cancelled = false;

    getVirtualContent(bookId)
      .then(async (blob) => {
        const buffer = await blob.arrayBuffer();
        if (cancelled) return;
        const pdf = await pdfjsLib.getDocument({ data: buffer }).promise;
        if (cancelled) return;
        pdfRef.current = pdf;
        setNumPages(pdf.numPages);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        const status = (err as { response?: { status?: number } })?.response?.status;
        setError(
          status === 403
            ? "You haven't purchased this book."
            : status === 404
              ? 'No virtual edition is available for this book.'
              : 'Could not load this book right now.',
        );
        setLoading(false);
      });

    return () => {
      cancelled = true;
      pdfRef.current?.loadingTask.destroy();
    };
  }, [bookId]);

  useEffect(() => {
    if (!pdfRef.current || !canvasRef.current) return;
    let cancelled = false;

    pdfRef.current.getPage(page).then(async (pdfPage) => {
      if (cancelled || !canvasRef.current) return;
      const viewport = pdfPage.getViewport({ scale: 1.4 });
      const canvas = canvasRef.current;
      const context = canvas.getContext('2d');
      if (!context) return;
      canvas.width = viewport.width;
      canvas.height = viewport.height;
      await pdfPage.render({ canvasContext: context, viewport, canvas }).promise;
    });

    return () => {
      cancelled = true;
    };
  }, [page, numPages]);

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
          <ArrowLeft size={15} />
          Back
        </Button>
        <span className={styles.lockNote}>
          <Lock size={12} />
          In-app reading only
        </span>
        {numPages > 0 && (
          <div className={styles.pager}>
            <Button
              variant="secondary"
              size="sm"
              iconOnly
              aria-label="Previous page"
              disabled={page <= 1}
              onClick={() => setPage((p) => Math.max(1, p - 1))}
            >
              <ChevronLeft size={15} />
            </Button>
            <span className={styles.pageIndicator}>
              Page {page} of {numPages}
            </span>
            <Button
              variant="secondary"
              size="sm"
              iconOnly
              aria-label="Next page"
              disabled={page >= numPages}
              onClick={() => setPage((p) => Math.min(numPages, p + 1))}
            >
              <ChevronRight size={15} />
            </Button>
          </div>
        )}
      </div>

      <div className={styles.viewer}>
        {loading && <p className={styles.status}>Loading…</p>}
        {error && <p className={styles.status}>{error}</p>}
        {!loading && !error && (
          <canvas ref={canvasRef} className={styles.canvas} onContextMenu={(e) => e.preventDefault()} />
        )}
      </div>
    </div>
  );
}
