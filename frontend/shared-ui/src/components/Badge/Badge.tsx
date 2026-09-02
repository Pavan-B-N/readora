import type { ReactNode } from 'react';
import styles from './Badge.module.css';

interface BadgeProps {
  variant?: 'neutral' | 'success' | 'danger' | 'warning' | 'info';
  dot?: boolean;
  pulse?: boolean;
  children: ReactNode;
}

export function Badge({ variant = 'neutral', dot = false, pulse = false, children }: BadgeProps) {
  return (
    <span className={[styles.badge, styles[variant]].join(' ')}>
      {dot && <span className={[styles.dot, pulse && styles.pulse].filter(Boolean).join(' ')} />}
      {children}
    </span>
  );
}
