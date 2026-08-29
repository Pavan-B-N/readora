import { useState, type MouseEvent, type ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BookOpen, Heart, Minus, Plus, ShoppingCart, Zap } from 'lucide-react';
import type { BookSummary } from '@/types/catalog';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { addWishlistItem, removeWishlistItem } from '@/redux/slices/wishlistSlice';
import { addToCart, updateCartItemQty } from '@/redux/slices/cartSlice';
import { useToast } from '@/components/Toast';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { StarRating } from '@/components/StarRating';
import { ROUTES } from '@/constants/routes';
import styles from './BookCard.module.css';

const MAX_QTY_PER_TITLE = 10;

export function BookCard({
  book,
  footer,
  addLabel = 'Add',
  owned = false,
}: {
  book: BookSummary;
  footer?: ReactNode;
  addLabel?: string;
  /** The caller already owns this book's virtual edition — there's nothing left to buy, so the card offers "Read now" instead of cart controls. No effect on a physical listing (a second copy is a legitimate purchase). */
  owned?: boolean;
}) {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { showToast } = useToast();
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const storeId = useAppSelector((state) => state.store.selectedId);
  const wishlisted = useAppSelector((state) => Boolean(state.wishlist.ids[book.id]));
  const cartLine = useAppSelector((state) =>
    state.cart.items.find((i) => i.bookId === book.id && i.deliveryType === book.deliveryType),
  );
  const [adding, setAdding] = useState(false);
  const [changingQty, setChangingQty] = useState(false);
  const outOfStock = book.availability === 'OUT_OF_STOCK';
  const ownedVirtual = owned && book.deliveryType === 'VIRTUAL';

  const onReadNow = (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    navigate(ROUTES.read(book.id));
  };

  const toggleWishlist = (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!accessToken) {
      navigate(ROUTES.login, { state: { from: { pathname: ROUTES.home } } });
      return;
    }
    dispatch(wishlisted ? removeWishlistItem(book.id) : addWishlistItem(book.id));
  };

  const onAdd = async (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!accessToken) {
      navigate(ROUTES.login, { state: { from: { pathname: ROUTES.home } } });
      return;
    }
    setAdding(true);
    try {
      await dispatch(
        addToCart({
          bookId: book.id,
          qty: 1,
          deliveryType: book.deliveryType,
          storeId: book.deliveryType === 'PHYSICAL' ? (storeId ?? undefined) : undefined,
        }),
      ).unwrap();
      showToast(`Added ${book.deliveryType === 'VIRTUAL' ? 'the virtual edition' : 'a physical copy'} to cart`);
    } catch {
      showToast('Could not add to cart', 'error');
    } finally {
      setAdding(false);
    }
  };

  const changeQty = async (e: MouseEvent, nextQty: number) => {
    e.preventDefault();
    e.stopPropagation();
    setChangingQty(true);
    try {
      await dispatch(updateCartItemQty({ bookId: book.id, deliveryType: book.deliveryType, qty: nextQty })).unwrap();
    } catch {
      showToast('Could not update the cart', 'error');
    } finally {
      setChangingQty(false);
    }
  };

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
        {book.hasVirtualEdition && (
          <span className={styles.virtualChip}>
            <Zap size={11} />
            Virtual edition
          </span>
        )}
        <button
          type="button"
          className={[styles.wishlistButton, wishlisted && styles.wishlistButtonActive].filter(Boolean).join(' ')}
          onClick={toggleWishlist}
          aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
          aria-pressed={wishlisted}
        >
          <Heart size={14} fill={wishlisted ? 'currentColor' : 'none'} />
        </button>
      </div>

      <div className={styles.title}>{book.title}</div>
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
      {ownedVirtual ? (
        <Button variant="primary" size="sm" block onClick={onReadNow}>
          <BookOpen size={13} />
          Read now
        </Button>
      ) : cartLine ? (
        book.deliveryType === 'VIRTUAL' ? (
          <button
            type="button"
            className={[styles.qtyStepper, styles.qtyStepperVirtual].join(' ')}
            onClick={(e) => changeQty(e, 0)}
            disabled={changingQty}
            aria-label="Remove virtual edition from cart"
            title="A virtual edition is a single digital copy"
          >
            <Minus size={13} />
            In cart
          </button>
        ) : (
          <div className={styles.qtyStepper}>
            <button
              type="button"
              className={styles.qtyStepperButton}
              onClick={(e) => changeQty(e, cartLine.qty - 1)}
              disabled={changingQty}
              aria-label={cartLine.qty === 1 ? 'Remove from cart' : 'Decrease quantity'}
            >
              <Minus size={13} />
            </button>
            <span className={styles.qtyStepperValue}>{cartLine.qty}</span>
            <button
              type="button"
              className={styles.qtyStepperButton}
              onClick={(e) => changeQty(e, cartLine.qty + 1)}
              disabled={changingQty || cartLine.qty >= MAX_QTY_PER_TITLE}
              aria-label="Increase quantity"
            >
              <Plus size={13} />
            </button>
          </div>
        )
      ) : (
        <Button variant="primary" size="sm" block onClick={onAdd} disabled={outOfStock || adding}>
          <ShoppingCart size={13} />
          {adding ? 'Adding…' : addLabel}
        </Button>
      )}
      {footer}
    </Link>
  );
}
