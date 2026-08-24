import { Link } from 'react-router-dom';
import type { BookSummary } from '@/types/catalog';
import { ROUTES } from '@/constants/routes';
import styles from './BookCard.module.css';

export function BookCard({ book }: { book: BookSummary }) {
  return (
    <Link to={ROUTES.bookDetail(book.id)} className={styles.card}>
      <div className={styles.cover}>
        {book.coverImageUrl ? <img src={book.coverImageUrl} alt={book.title} /> : <span>No cover</span>}
      </div>
      <div className={styles.title}>{book.title}</div>
      <div className={styles.authors}>{book.authors.join(', ')}</div>
      <div className={styles.price}>
        {book.listPrice} {book.currency}
      </div>
      {book.availability === 'OUT_OF_STOCK' && <div className={styles.outOfStock}>Out of stock</div>}
    </Link>
  );
}
