import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import styles from './EmptyState.module.css';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <div className={styles.empty}>
      <span className={styles.icon}>
        <Icon size={20} />
      </span>
      <span className={styles.title}>{title}</span>
      {description && <p className={styles.description}>{description}</p>}
      {action}
    </div>
  );
}
