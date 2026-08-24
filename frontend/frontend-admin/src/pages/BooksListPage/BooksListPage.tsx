import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { listBooks } from '@/api/catalogApi';
import type { BookSummary } from '@/types/catalog';
import { Card } from '@/components/Card';
import { Button } from '@/components/Button';
import { Input } from '@/components/Input';
import { ROUTES } from '@/constants/routes';
import styles from './BooksListPage.module.css';

const PAGE_SIZE = 20;

export function BooksListPage() {
  const navigate = useNavigate();
  const [books, setBooks] = useState<BookSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    listBooks(page, PAGE_SIZE, query)
      .then((result) => {
        if (cancelled) return;
        setBooks(result.items);
        setTotalPages(result.totalPages);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [page, query]);

  return (
    <div>
      <div className={styles.header}>
        <h1>Books</h1>
        <Button onClick={() => navigate(ROUTES.newBook)}>
          <Plus size={16} />
          New book
        </Button>
      </div>

      <div className={styles.searchBar}>
        <Input
          label="Search"
          placeholder="Title, author, ISBN…"
          value={query}
          onChange={(e) => {
            setPage(0);
            setQuery(e.target.value);
          }}
        />
      </div>

      <Card>
        {loading ? (
          <p>Loading…</p>
        ) : books.length === 0 ? (
          <p className={styles.empty}>No books found.</p>
        ) : (
          <>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Authors</th>
                  <th>Format</th>
                  <th>Price</th>
                  <th>Availability</th>
                </tr>
              </thead>
              <tbody>
                {books.map((book) => (
                  <tr key={book.id} className={styles.row} onClick={() => navigate(ROUTES.editBook(book.id))}>
                    <td>{book.title}</td>
                    <td>{book.authors.join(', ')}</td>
                    <td>{book.format}</td>
                    <td>
                      {book.listPrice} {book.currency}
                    </td>
                    <td>
                      <span className={book.availability === 'IN_STOCK' ? styles.badgeInStock : styles.badgeOutOfStock}>
                        {book.availability === 'IN_STOCK' ? 'In stock' : 'Out of stock'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

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
      </Card>
    </div>
  );
}
