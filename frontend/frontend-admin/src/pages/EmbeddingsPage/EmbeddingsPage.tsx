import { useState } from 'react';
import { triggerBackfill } from '@/api/embeddingsApi';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Button } from '@/components/Button';
import styles from './EmbeddingsPage.module.css';

export function EmbeddingsPage() {
  const { showToast } = useToast();
  const [running, setRunning] = useState(false);

  const onBackfill = async () => {
    setRunning(true);
    try {
      await triggerBackfill();
      showToast('Backfill complete');
    } catch {
      showToast('Backfill failed', 'error');
    } finally {
      setRunning(false);
    }
  };

  return (
    <div>
      <h1>Embeddings</h1>
      <Card className={styles.card}>
        <p className={styles.description}>
          Day-to-day book changes re-embed automatically via the book.upserted event. Use this only to bootstrap a
          fresh vector store or recover from missed events — it re-embeds the entire catalogue and may take a while
          for a large one.
        </p>
        <Button onClick={onBackfill} disabled={running}>
          {running ? 'Running backfill…' : 'Re-embed entire catalogue'}
        </Button>
      </Card>
    </div>
  );
}
