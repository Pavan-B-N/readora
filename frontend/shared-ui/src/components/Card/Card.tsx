import type { HTMLAttributes, ReactNode } from 'react';
import styles from './Card.module.css';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** Removes the card's inner padding, for content (like a table) that should reach the edges. */
  flush?: boolean;
}

/** A bordered content container used as the base surface for panels and sections. */
export function Card({ className, flush, ...rest }: CardProps) {
  return (
    <div className={[styles.card, flush && styles.flush, className].filter(Boolean).join(' ')} {...rest} />
  );
}

interface CardHeaderProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
}

/** The title/subtitle/actions row that goes at the top of a Card. */
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
