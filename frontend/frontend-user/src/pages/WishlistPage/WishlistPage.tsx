import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, BookOpen, Heart } from 'lucide-react';
import { getBooksByIds, getLibrary } from '@/api/catalogApi';
import { extractErrorMessage } from '@/api/client';
import type { BookSummary } from '@/types/catalog';
import { useAppSelector } from '@/redux/hooks';
import { BookCard } from '@/components/BookCard';
import { Button } from '@readora/shared-ui';
import { EmptyState } from '@readora/shared-ui';
import { useToast } from '@readora/shared-ui';
import { ROUTES } from '@/constants/routes';
import styles from './WishlistPage.module.css';

export function WishlistPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const wishlistIds = useAppSelector((state) => state.wishlist.ids);
  const idList = useMemo(() => Object.keys(wishlistIds), [wishlistIds]);

  const [books, setBooks] = useState<BookSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [ownedVirtualIds, setOwnedVirtualIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    getLibrary()
      .then((owned) => setOwnedVirtualIds(new Set(owned.map((b) => b.id))))
      .catch((err) => showToast(extractErrorMessage(err, 'Could not load your library'), 'error'));
  }, [showToast]);

  useEffect(() => {
    if (idList.length === 0) {
      setBooks([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    getBooksByIds(idList)
      .then(setBooks)
      .catch((err) => showToast(extractErrorMessage(err, 'Could not load your wishlist'), 'error'))
      .finally(() => setLoading(false));
  }, [idList, showToast]);

  return (
    <div>
      <div style={{ marginBottom: 'var(--space-2)' }}>
        <Button variant="ghost" onClick={() => navigate(-1)}>
          <ArrowLeft size={16} />
          Back
        </Button>
      </div>

      {loading ? (
        <div className={styles.grid}>
          {Array.from({ length: 6 }).map((_, i) => (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }} key={i}>
              <div className="shimmer" style={{ aspectRatio: '2 / 3', borderRadius: 'var(--radius-md)' }} />
              <div className="shimmer" style={{ height: '10px', width: '85%', borderRadius: 'var(--radius-sm)' }} />
              <div className="shimmer" style={{ height: '10px', width: '55%', borderRadius: 'var(--radius-sm)' }} />
            </div>
          ))}
        </div>
      ) : books.length === 0 ? (
        <div style={{ marginTop: 'var(--space-10)' }}>
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
        </div>
      ) : (
        <div className={styles.grid}>
          {books.map((book) => (
            <BookCard book={book} key={book.id} owned={ownedVirtualIds.has(book.id)} />
          ))}
        </div>
      )}
    </div>
  );
}
