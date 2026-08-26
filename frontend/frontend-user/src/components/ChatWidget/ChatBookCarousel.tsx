import { BookOpen } from 'lucide-react';
import type { BookDetail } from '@/types/catalog';
import styles from './ChatBookCarousel.module.css';

interface ChatBookCarouselProps {
  books: BookDetail[];
  onSelect: (book: BookDetail) => void;
}

export function ChatBookCarousel({ books, onSelect }: ChatBookCarouselProps) {
  if (books.length === 0) return null;

  return (
    <div className={styles.row}>
      {books.map((book) => (
        <div className={styles.card} key={book.id}>
          <div className={styles.cover}>
            {book.images[0] ? (
              <img src={book.images[0]} alt={book.title} />
            ) : (
              <BookOpen size={20} />
            )}
          </div>
          <span className={styles.title}>{book.title}</span>
          <span className={styles.price}>₹{book.listPrice}</span>
          <button type="button" className={styles.selectButton} onClick={() => onSelect(book)}>
            Select
          </button>
        </div>
      ))}
    </div>
  );
}
