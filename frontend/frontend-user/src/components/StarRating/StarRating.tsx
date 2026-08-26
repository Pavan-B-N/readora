import { useState } from 'react';
import { Star } from 'lucide-react';
import styles from './StarRating.module.css';

interface StarRatingProps {
  value: number;
  size?: number;
  /** Presence makes the component an interactive 1-5 picker instead of a read-only display. */
  onChange?: (rating: number) => void;
}

export function StarRating({ value, size = 14, onChange }: StarRatingProps) {
  const [hovered, setHovered] = useState<number | null>(null);
  const interactive = Boolean(onChange);
  const displayValue = hovered ?? value;

  return (
    <span
      className={[styles.stars, interactive && styles.interactive].filter(Boolean).join(' ')}
      onMouseLeave={() => setHovered(null)}
      role={interactive ? 'radiogroup' : undefined}
      aria-label={interactive ? 'Rating' : `${value.toFixed(1)} out of 5 stars`}
    >
      {[1, 2, 3, 4, 5].map((star) => (
        <Star
          key={star}
          size={size}
          className={styles.star}
          fill={star <= displayValue ? 'currentColor' : 'none'}
          onMouseEnter={interactive ? () => setHovered(star) : undefined}
          onClick={interactive ? () => onChange?.(star) : undefined}
        />
      ))}
    </span>
  );
}
