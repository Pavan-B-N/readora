import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import * as pdfjsLib from 'pdfjs-dist';
import type { PDFDocumentProxy } from 'pdfjs-dist';
import pdfWorkerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url';
import { ArrowLeft, ChevronLeft, ChevronRight, Lock, Loader2, Send, Sparkles } from 'lucide-react';
import { getVirtualContent } from '@/api/catalogApi';
import {
  getReaderHistory,
  getReaderStatus,
  initializeReader,
  sendReaderMessage,
  type ReaderIndexStatus,
  type ReaderMessage,
} from '@/api/aiApi';
import { Button } from '@/components/Button';
import { Spinner } from '@/components/Spinner';
import { RichText } from '@/components/ChatWidget/RichText';
import styles from './VirtualReaderPage.module.css';

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorkerUrl;

export function VirtualReaderPage() {
  const { bookId } = useParams<{ bookId: string }>();
  const navigate = useNavigate();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const viewerRef = useRef<HTMLDivElement>(null);
  const pdfRef = useRef<PDFDocumentProxy | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const [numPages, setNumPages] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewerWidth, setViewerWidth] = useState(0);

  const [readerStatus, setReaderStatus] = useState<ReaderIndexStatus>(null);
  const [initializing, setInitializing] = useState(false);
  const [messages, setMessages] = useState<ReaderMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);

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

  // Tracks the viewer column's actual width so the page render below can fill it, rather than
  // rendering at a fixed scale that leaves the extra room this page now has (full viewport width,
  // no site-wide reading-column cap) unused.
  useEffect(() => {
    const el = viewerRef.current;
    if (!el) return;
    const observer = new ResizeObserver((entries) => {
      const width = entries[0]?.contentRect.width;
      if (width) setViewerWidth(width);
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!pdfRef.current || !canvasRef.current || !viewerWidth) return;
    let cancelled = false;

    pdfRef.current.getPage(page).then(async (pdfPage) => {
      if (cancelled || !canvasRef.current) return;
      const naturalWidth = pdfPage.getViewport({ scale: 1 }).width;
      // Leaves a little breathing room either side rather than touching the card's own padding
      // exactly, and never upscales a narrow page into a blurry giant on a very wide monitor.
      const scale = Math.min(Math.max(viewerWidth - 48, 1) / naturalWidth, 2.2);
      const viewport = pdfPage.getViewport({ scale });
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
  }, [page, numPages, viewerWidth]);

  useEffect(() => {
    if (!bookId) return;
    getReaderStatus(bookId).then(setReaderStatus);
  }, [bookId]);

  useEffect(() => {
    if (!bookId || readerStatus !== 'READY') return;
    getReaderHistory(bookId).then(setMessages);
  }, [bookId, readerStatus]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const onInitialize = async () => {
    if (!bookId) return;
    setInitializing(true);
    try {
      const status = await initializeReader(bookId);
      setReaderStatus(status);
    } catch {
      setReaderStatus('FAILED');
    } finally {
      setInitializing(false);
    }
  };

  const onSend = async (e: FormEvent) => {
    e.preventDefault();
    const question = draft.trim();
    if (!bookId || !question || sending) return;

    setMessages((prev) => [...prev, { role: 'USER', content: question }]);
    setDraft('');
    setSending(true);
    try {
      const reply = await sendReaderMessage(bookId, question);
      setMessages((prev) => [...prev, { role: 'ASSISTANT', content: reply }]);
    } catch {
      setMessages((prev) => [
        ...prev,
        { role: 'ASSISTANT', content: "Sorry, I couldn't answer that just now — try again." },
      ]);
    } finally {
      setSending(false);
    }
  };

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

      <div className={styles.layout}>
        <div className={styles.viewer} ref={viewerRef}>
          {loading && <Spinner />}
          {error && <p className={styles.status}>{error}</p>}
          {!loading && !error && (
            <canvas ref={canvasRef} className={styles.canvas} onContextMenu={(e) => e.preventDefault()} />
          )}
        </div>

        {!loading && !error && (
          <div className={styles.assistantPanel}>
            <div className={styles.assistantHeader}>
              <Sparkles size={15} />
              Reading assistant
            </div>

            {readerStatus === 'READY' ? (
              <>
                <div className={styles.messages}>
                  {messages.length === 0 && (
                    <p className={styles.assistantHint}>Ask a question about this book — I'll answer from its contents.</p>
                  )}
                  {messages.map((m, i) => (
                    <div key={i} className={[styles.bubble, m.role === 'USER' ? styles.bubbleUser : styles.bubbleAssistant].join(' ')}>
                      <RichText text={m.content} />
                    </div>
                  ))}
                  {sending && (
                    <div className={[styles.bubble, styles.bubbleAssistant].join(' ')}>
                      <Loader2 size={14} className="spin" />
                    </div>
                  )}
                  <div ref={messagesEndRef} />
                </div>
                <form className={styles.composer} onSubmit={onSend}>
                  <input
                    className={styles.composerInput}
                    placeholder="Ask about this book…"
                    value={draft}
                    onChange={(e) => setDraft(e.target.value)}
                    disabled={sending}
                  />
                  <Button type="submit" size="sm" iconOnly aria-label="Send" disabled={sending || !draft.trim()}>
                    <Send size={14} />
                  </Button>
                </form>
              </>
            ) : (
              <div className={styles.assistantSetup}>
                {readerStatus === 'FAILED' && (
                  <p className={styles.assistantError}>Couldn't set up the assistant last time.</p>
                )}
                <p className={styles.assistantHint}>
                  Set up the reading assistant to ask questions and get answers straight from this book's content.
                </p>
                <Button onClick={onInitialize} disabled={initializing}>
                  {initializing ? <Loader2 size={15} className="spin" /> : <Sparkles size={15} />}
                  {initializing ? 'Preparing…' : readerStatus === 'FAILED' ? 'Try again' : 'Set up assistant'}
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
