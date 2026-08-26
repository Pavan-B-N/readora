import { Link } from 'react-router-dom';
import { BookOpen } from 'lucide-react';
import type { BookSummary } from '@/types/catalog';
import { Badge } from '@/components/Badge';
import { StarRating } from '@/components/StarRating';
import { ROUTES } from '@/constants/routes';
import styles from './BookCard.module.css';

export function BookCard({ book }: { book: BookSummary }) {
  const outOfStock = book.availability === 'OUT_OF_STOCK';

  return (
    <Link to={ROUTES.bookDetail(book.id)} className={styles.card}>
      <div className={styles.coverWrap}>
        <div className={styles.cover}>
          {book.coverImageUrl ? (
            <img src={book.coverImageUrl} alt="" loading="lazy" />
          ) : (
            <span className={styles.coverFallback}>
              <BookOpen size={20} />
              <span className={styles.fallbackTitle}>{book.title}</span>
            </span>
          )}
        </div>
        {outOfStock && (
          <div className={styles.outOfStockOverlay}>
            <Badge variant="danger">Out of stock</Badge>
          </div>
        )}
      </div>

      <div className={styles.title}>{book.title}</div>
      <div className={styles.authors}>{book.authors.join(', ') || 'Unknown author'}</div>
      {book.reviewCount > 0 && (
        <div className={styles.ratingRow}>
          <StarRating value={book.averageRating ?? 0} size={11} />
          <span className={styles.reviewCount}>({book.reviewCount})</span>
        </div>
      )}
      <div className={styles.priceRow}>
        <span className={styles.price}>₹{book.listPrice}</span>
        <span className={styles.currency}>{book.currency}</span>
      </div>
    </Link>
  );
}
