import { Loader2 } from 'lucide-react';
import styles from './Spinner.module.css';

/** A centered loading spinner for a whole page or section — replaces a bare "Loading…" text. */
export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className={styles.wrap} role="status" aria-live="polite">
      <Loader2 size={20} className="spin" aria-hidden="true" />
      {label && <span className={styles.label}>{label}</span>}
    </div>
  );
}
