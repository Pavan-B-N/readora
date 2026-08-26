import type { ReactNode } from 'react';
import styles from './Badge.module.css';

interface BadgeProps {
  variant?: 'neutral' | 'success' | 'danger' | 'warning' | 'info';
  dot?: boolean;
  children: ReactNode;
}

export function Badge({ variant = 'neutral', dot = false, children }: BadgeProps) {
  return (
    <span className={[styles.badge, styles[variant]].join(' ')}>
      {dot && <span className={styles.dot} />}
      {children}
    </span>
  );
}
