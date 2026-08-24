import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  BookOpen,
  Check,
  ChevronRight,
  Minus,
  Plus,
  ShoppingCart,
} from 'lucide-react';
import { getBookDetail, getRelatedBooks } from '@/api/catalogApi';
import type { BookDetail, RelatedBook } from '@/types/catalog';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { addToCart, fetchCart, updateCartItemQty } from '@/redux/slices/cartSlice';
import { useToast } from '@/components/Toast';
import { Button } from '@/components/Button';
import { Badge } from '@/components/Badge';
import { ROUTES } from '@/constants/routes';
import styles from './BookDetailPage.module.css';

const MAX_PER_TITLE = 10;

export function BookDetailPage() {
  const { bookId } = useParams<{ bookId: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { showToast } = useToast();
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const cartItems = useAppSelector((state) => state.cart.items);

  const [book, setBook] = useState<BookDetail | null>(null);
  const [related, setRelated] = useState<RelatedBook[]>([]);
  const [qty, setQty] = useState(1);
  const [busy, setBusy] = useState(false);

  const cartLine = cartItems.find((item) => item.bookId === bookId);
  const inCartQty = cartLine?.qty ?? 0;

  useEffect(() => {
    if (!bookId) return;
    setQty(1);
    getBookDetail(bookId).then(setBook);
    getRelatedBooks(bookId).then(setRelated);
  }, [bookId]);

  useEffect(() => {
    if (accessToken) dispatch(fetchCart());
  }, [accessToken, dispatch]);

  if (!book) return <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>;

  const inStock = book.availability.status === 'IN_STOCK';
  const stockCap = Math.min(MAX_PER_TITLE, book.availability.quantityAvailable || 0);
  const remainingAllowance = Math.max(0, stockCap - inCartQty);
  const atLimit = inCartQty >= stockCap;

  const onAddToCart = async () => {
    if (!accessToken) {
      navigate(ROUTES.login, { state: { from: { pathname: ROUTES.bookDetail(book.id) } } });
      return;
    }

    setBusy(true);
    try {
      await dispatch(addToCart({ bookId: book.id, qty })).unwrap();
      showToast(`Added ${qty} × ${book.title} to cart`);
      setQty(1);
    } catch {
      showToast('Could not add to cart', 'error');
    } finally {
      setBusy(false);
    }
  };

  const changeCartQty = async (nextQty: number) => {
    setBusy(true);
    try {
      await dispatch(updateCartItemQty({ bookId: book.id, qty: nextQty })).unwrap();
      showToast(nextQty === 0 ? 'Removed from cart' : 'Cart updated');
    } catch {
      showToast('Could not update the cart', 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <nav className={styles.breadcrumb}>
        <Link to={ROUTES.home}>Home</Link>
        {book.category && (
          <>
            <ChevronRight size={12} />
            <span>{book.category.name}</span>
          </>
        )}
        <ChevronRight size={12} />
        <span>{book.title}</span>
      </nav>

      <div className={styles.layout}>
        <div className={styles.coverColumn}>
          <div className={styles.cover}>
            {book.images[0] ? (
              <img src={book.images[0]} alt={book.title} />
            ) : (
              <span className={styles.coverFallback}>
                <BookOpen size={28} />
                <span className={styles.fallbackTitle}>{book.title}</span>
              </span>
            )}
          </div>
        </div>

        <div>
          <h1 className={styles.title}>{book.title}</h1>
          {book.subtitle && <p className={styles.subtitle}>{book.subtitle}</p>}
          <p className={styles.authors}>
            by <span className={styles.authorName}>{book.authors.map((a) => a.name).join(', ')}</span>
          </p>

          <div className={styles.metaRow}>
            <Badge>{book.format}</Badge>
            {book.pageCount && <Badge>{book.pageCount} pages</Badge>}
            {book.language && <Badge>{book.language.toUpperCase()}</Badge>}
            {book.publisher && <Badge>{book.publisher.name}</Badge>}
          </div>

          <div className={styles.purchaseBox}>
            <div className={styles.priceRow}>
              <span className={styles.price}>₹{book.listPrice}</span>
              <span className={styles.currency}>{book.currency}</span>
            </div>

            <div className={[styles.availability, inStock ? styles.inStock : styles.outOfStock].join(' ')}>
              {inStock ? (
                <>
                  <Check size={14} />
                  In stock — {book.availability.quantityAvailable} available
                </>
              ) : (
                'Currently out of stock'
              )}
            </div>

            {inCartQty > 0 ? (
              <>
                <div className={styles.purchaseRow}>
                  <div className={styles.qtyStepper}>
                    <button
                      type="button"
                      className={styles.qtyButton}
                      onClick={() => changeCartQty(inCartQty - 1)}
                      disabled={busy}
                      aria-label={inCartQty === 1 ? 'Remove from cart' : 'Decrease quantity'}
                    >
                      <Minus size={14} />
                    </button>
                    <span className={styles.qtyValue}>{inCartQty}</span>
                    <button
                      type="button"
                      className={styles.qtyButton}
                      onClick={() => changeCartQty(inCartQty + 1)}
                      disabled={busy || atLimit}
                      aria-label="Increase quantity"
                    >
                      <Plus size={14} />
                    </button>
                  </div>
                  <Button onClick={() => navigate(ROUTES.cart)}>
                    <ShoppingCart size={15} />
                    Go to cart
                  </Button>
                </div>

                <div className={styles.inCartNote}>
                  <Check size={13} />
                  {inCartQty} in your cart
                </div>

                {atLimit && (
                  <p className={styles.limitNote}>
                    {inCartQty >= MAX_PER_TITLE
                      ? `Maximum ${MAX_PER_TITLE} per title.`
                      : 'No more stock available.'}
                  </p>
                )}
              </>
            ) : (
              <>
                <div className={styles.purchaseRow}>
                  <div className={styles.qtyStepper}>
                    <button
                      type="button"
                      className={styles.qtyButton}
                      onClick={() => setQty((q) => Math.max(1, q - 1))}
                      disabled={!inStock || qty <= 1}
                      aria-label="Decrease quantity"
                    >
                      <Minus size={14} />
                    </button>
                    <span className={styles.qtyValue}>{qty}</span>
                    <button
                      type="button"
                      className={styles.qtyButton}
                      onClick={() => setQty((q) => Math.min(remainingAllowance || 1, q + 1))}
                      disabled={!inStock || qty >= remainingAllowance}
                      aria-label="Increase quantity"
                    >
                      <Plus size={14} />
                    </button>
                  </div>
                  <Button onClick={onAddToCart} disabled={!inStock || busy}>
                    <ShoppingCart size={15} />
                    {busy ? 'Adding…' : 'Add to cart'}
                  </Button>
                </div>
                {inStock && <p className={styles.limitNote}>Up to {MAX_PER_TITLE} copies per title.</p>}
              </>
            )}
          </div>

          {book.description && (
            <div className={styles.section}>
              <h2 className={styles.sectionTitle}>About this book</h2>
              <p className={styles.description}>{book.description}</p>
            </div>
          )}

          <div className={styles.specs}>
            <div className={styles.spec}>
              <span className={styles.specLabel}>ISBN-13</span>
              <span className={styles.specValue}>{book.isbn13}</span>
            </div>
            <div className={styles.spec}>
              <span className={styles.specLabel}>Format</span>
              <span className={styles.specValue}>{book.format}</span>
            </div>
            {book.publishedOn && (
              <div className={styles.spec}>
                <span className={styles.specLabel}>Published</span>
                <span className={styles.specValue}>{new Date(book.publishedOn).toLocaleDateString()}</span>
              </div>
            )}
            <div className={styles.spec}>
              <span className={styles.specLabel}>Delivery</span>
              <span className={styles.specValue}>~{book.estimatedDeliveryDays} days</span>
            </div>
          </div>

          {related.length > 0 && (
            <div className={styles.section}>
              <h2 className={styles.sectionTitle}>Related reads</h2>
              <div className={styles.relatedGrid}>
                {related.map((r) => (
                  <Link key={r.id} to={ROUTES.bookDetail(r.id)} className={styles.relatedCard}>
                    <div className={styles.relatedCover}>
                      {r.coverImageUrl ? <img src={r.coverImageUrl} alt="" /> : <BookOpen size={18} />}
                    </div>
                    <span className={styles.relatedTitle}>{r.title}</span>
                    <span className={styles.relatedPrice}>₹{r.listPrice}</span>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
