import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { searchBooks } from '@/api/catalogApi';
import type { BookSummary } from '@/types/catalog';
import { BookCard } from '@/components/BookCard';
import { Button } from '@/components/Button';
import styles from './HomePage.module.css';

const PAGE_SIZE = 24;

export function HomePage() {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') ?? '';
  const [books, setBooks] = useState<BookSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setPage(0);
  }, [query]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    searchBooks({ q: query || undefined, page, size: PAGE_SIZE })
      .then((result) => {
        if (cancelled) return;
        setBooks(result.items ?? []);
        setTotalPages(result.totalPages);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [query, page]);

  return (
    <div>
      <div className={styles.header}>
        <h1 className={styles.title}>{query ? `Results for “${query}”` : 'Browse books'}</h1>
        <p className={styles.subtitle}>Physical and virtual editions, all in one place.</p>
      </div>

      {loading ? (
        <p>Loading…</p>
      ) : books.length === 0 ? (
        <p className={styles.empty}>No books found.</p>
      ) : (
        <>
          <div className={styles.grid}>
            {books.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>

          <div className={styles.pagination}>
            <Button variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Previous
            </Button>
            <span>
              Page {page + 1} of {Math.max(totalPages, 1)}
            </span>
            <Button variant="secondary" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
              Next
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
