import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, Heart } from 'lucide-react';
import { getBooksByIds } from '@/api/catalogApi';
import type { BookSummary } from '@/types/catalog';
import { useAppSelector } from '@/redux/hooks';
import { BookCard } from '@/components/BookCard';
import { Button } from '@/components/Button';
import { Card } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import { Spinner } from '@/components/Spinner';
import { ROUTES } from '@/constants/routes';
import styles from './WishlistPage.module.css';

export function WishlistPage() {
  const navigate = useNavigate();
  const wishlistIds = useAppSelector((state) => state.wishlist.ids);
  const idList = useMemo(() => Object.keys(wishlistIds), [wishlistIds]);

  const [books, setBooks] = useState<BookSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (idList.length === 0) {
      setBooks([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    getBooksByIds(idList)
      .then(setBooks)
      .finally(() => setLoading(false));
  }, [idList]);

  return (
    <div>
      <h1>Your wishlist</h1>

      {loading ? (
        <Spinner />
      ) : books.length === 0 ? (
        <Card style={{ marginTop: 'var(--space-5)' }}>
          <EmptyState
            icon={Heart}
            title="Your wishlist is empty"
            description="Tap the heart on any book to save it here for later."
            action={
              <Button onClick={() => navigate(ROUTES.home)}>
                <BookOpen size={15} />
                Browse books
              </Button>
            }
          />
        </Card>
      ) : (
        <div className={styles.grid}>
          {books.map((book) => (
            <BookCard book={book} key={book.id} />
          ))}
        </div>
      )}
    </div>
  );
}
