import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getBookDetail, getRelatedBooks } from '@/api/catalogApi';
import type { BookDetail, RelatedBook } from '@/types/catalog';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { addToCart } from '@/redux/slices/cartSlice';
import { useToast } from '@/components/Toast';
import { Button } from '@/components/Button';
import { ROUTES } from '@/constants/routes';
import styles from './BookDetailPage.module.css';

export function BookDetailPage() {
  const { bookId } = useParams<{ bookId: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { showToast } = useToast();
  const accessToken = useAppSelector((state) => state.auth.accessToken);

  const [book, setBook] = useState<BookDetail | null>(null);
  const [related, setRelated] = useState<RelatedBook[]>([]);
  const [qty, setQty] = useState(1);
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    if (!bookId) return;
    getBookDetail(bookId).then(setBook);
    getRelatedBooks(bookId).then(setRelated);
  }, [bookId]);

  if (!book) {
    return <p>Loading…</p>;
  }

  const inStock = book.availability.status === 'IN_STOCK';

  const onAddToCart = async () => {
    if (!accessToken) {
      navigate(ROUTES.login, { state: { from: { pathname: ROUTES.bookDetail(book.id) } } });
      return;
    }

    setAdding(true);
    try {
      await dispatch(addToCart({ bookId: book.id, qty })).unwrap();
      showToast('Added to cart');
    } catch {
      showToast('Could not add to cart', 'error');
    } finally {
      setAdding(false);
    }
  };

  return (
    <div>
      <div className={styles.layout}>
        <div className={styles.cover}>
          {book.images[0] ? <img src={book.images[0]} alt={book.title} /> : <span>No cover</span>}
        </div>

        <div>
          <h1 className={styles.title}>{book.title}</h1>
          {book.subtitle && <p className={styles.subtitle}>{book.subtitle}</p>}
          <p className={styles.authors}>By {book.authors.map((a) => a.name).join(', ')}</p>

          <div className={styles.meta}>
            <span>{book.format}</span>
            {book.pageCount && <span>{book.pageCount} pages</span>}
            {book.publisher && <span>{book.publisher.name}</span>}
          </div>

          <div className={styles.price}>
            {book.listPrice} {book.currency}
          </div>
          <div className={[styles.availability, inStock ? styles.inStock : styles.outOfStock].join(' ')}>
            {inStock ? `In stock (${book.availability.quantityAvailable} available)` : 'Out of stock'}
          </div>

          <div className={styles.purchaseRow}>
            <select className={styles.qtySelect} value={qty} onChange={(e) => setQty(Number(e.target.value))} disabled={!inStock}>
              {Array.from({ length: Math.min(10, book.availability.quantityAvailable || 1) }, (_, i) => i + 1).map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
            <Button onClick={onAddToCart} disabled={!inStock || adding}>
              {adding ? 'Adding…' : 'Add to cart'}
            </Button>
          </div>

          {book.description && <p className={styles.description}>{book.description}</p>}
        </div>
      </div>

      {related.length > 0 && (
        <div className={styles.relatedSection}>
          <h2 className={styles.sectionTitle}>You might also like</h2>
          <div className={styles.relatedGrid}>
            {related.map((r) => (
              <Link key={r.id} to={ROUTES.bookDetail(r.id)}>
                {r.title}
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
