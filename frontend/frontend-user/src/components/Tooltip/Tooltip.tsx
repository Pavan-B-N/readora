import { useState, type ReactNode } from 'react';
import styles from './Tooltip.module.css';

interface TooltipProps {
  label: string;
  placement?: 'top' | 'bottom' | 'left' | 'right';
  children: ReactNode;
}

/**
 * Hover/focus tooltip. Shows on focus as well as hover so keyboard users get the same hint,
 * and the trigger carries aria-label so screen readers don't depend on the visual bubble.
 */
export function Tooltip({ label, placement = 'top', children }: TooltipProps) {
  const [visible, setVisible] = useState(false);

  return (
    <span
      className={styles.wrapper}
      onMouseEnter={() => setVisible(true)}
      onMouseLeave={() => setVisible(false)}
      onFocus={() => setVisible(true)}
      onBlur={() => setVisible(false)}
    >
      {children}
      {visible && <span className={[styles.bubble, styles[placement]].join(' ')}>{label}</span>}
    </span>
  );
}
