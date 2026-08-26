import type { HTMLAttributes, ReactNode } from 'react';
import styles from './Card.module.css';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  flush?: boolean;
}

export function Card({ className, flush, ...rest }: CardProps) {
  return <div className={[styles.card, flush && styles.flush, className].filter(Boolean).join(' ')} {...rest} />;
}

interface CardHeaderProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
}

export function CardHeader({ title, subtitle, actions }: CardHeaderProps) {
  return (
    <div className={styles.header}>
      <div>
        <h2 className={styles.title}>{title}</h2>
        {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
      </div>
      {actions}
    </div>
  );
}
