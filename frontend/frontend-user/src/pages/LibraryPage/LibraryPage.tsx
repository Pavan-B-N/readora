import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BookOpen, Library } from 'lucide-react';
import { getLibrary } from '@/api/catalogApi';
import type { BookSummary } from '@/types/catalog';
import { Button } from '@readora/shared-ui';
import { EmptyState } from '@readora/shared-ui';
import { Spinner } from '@/components/Spinner';
import { ROUTES } from '@/constants/routes';
import styles from './LibraryPage.module.css';

export function LibraryPage() {
  const navigate = useNavigate();
  const [books, setBooks] = useState<BookSummary[] | null>(null);

  useEffect(() => {
    getLibrary().then(setBooks);
  }, []);

  if (books === null) {
    return (
      <div>
        <h1>My library</h1>
        <p className={styles.subtitle}>Every virtual edition you own, ready to read in-app.</p>
        <div className={styles.grid}>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className={styles.card}>
              <div className="shimmer" style={{ aspectRatio: '2 / 3', width: '100%', borderRadius: 'var(--radius-md)' }} />
              <div className="shimmer" style={{ height: 14, width: '80%', borderRadius: 4, marginTop: 12 }} />
              <div className="shimmer" style={{ height: 12, width: '60%', borderRadius: 4, marginTop: 4 }} />
              <div className="shimmer" style={{ height: 12, width: '40%', borderRadius: 4, marginTop: 8 }} />
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div>
      <h1>My library</h1>
      <p className={styles.subtitle}>Every virtual edition you own, ready to read in-app.</p>

      {books.length === 0 ? (
        <EmptyState
          icon={Library}
          title="Your library is empty"
          description="Buy a virtual edition to read it here, with an AI assistant that can answer questions about it."
          action={
            <Button onClick={() => navigate(ROUTES.home)}>
              <BookOpen size={15} />
              Browse books
            </Button>
          }
        />
      ) : (
        <div className={styles.grid}>
          {books.map((book) => (
            <Link key={book.id} to={ROUTES.read(book.id)} className={styles.card}>
              <div className={styles.cover}>
                {book.coverImageUrl ? (
                  <img src={book.coverImageUrl} alt="" loading="lazy" />
                ) : (
                  <span className={styles.coverFallback}>
                    <BookOpen size={22} />
                  </span>
                )}
              </div>
              <span className={styles.title}>{book.title}</span>
              <span className={styles.authors}>{book.authors.join(', ')}</span>
              <span className={styles.readNow}>
                <BookOpen size={13} />
                Read now
              </span>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
