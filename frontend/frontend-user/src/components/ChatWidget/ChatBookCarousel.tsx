import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, Minus, Plus } from 'lucide-react';
import type { BookDetail } from '@/types/catalog';
import type { DeliveryType } from '@/types/cart';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { addToCart, updateCartItemQty } from '@/redux/slices/cartSlice';
import { useToast } from '@readora/shared-ui';
import { ROUTES } from '@/constants/routes';
import styles from './ChatBookCarousel.module.css';

const MAX_QTY_PER_TITLE = 10;

interface ChatBookCarouselProps {
  books: BookDetail[];
}

/** A vertical list of books the assistant just recommended — thumbnail, title/author, then a cart control. */
export function ChatBookCarousel({ books }: ChatBookCarouselProps) {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const storeId = useAppSelector((state) => state.store.selectedId);
  const cartItems = useAppSelector((state) => state.cart.items);
  const [busyId, setBusyId] = useState<string | null>(null);

  if (books.length === 0) return null;

  const changeQty = async (bookId: string, deliveryType: DeliveryType, nextQty: number) => {
    setBusyId(bookId);
    try {
      await dispatch(updateCartItemQty({ bookId, deliveryType, qty: nextQty })).unwrap();
    } catch {
      showToast('Could not update the cart', 'error');
    } finally {
      setBusyId(null);
    }
  };

  // The assistant's guardrail already restricts recommendations to books that are purchasable
  // exactly one way at this store: in-stock physical, or a virtual edition when there's no store
  // stock — never both at once in this catalogue. So there's nothing to ask the user to choose
  // between; add whichever edition is actually the available one.
  const addDirectly = async (book: BookDetail) => {
    const deliveryType: DeliveryType = book.availability.status === 'IN_STOCK' ? 'PHYSICAL' : 'VIRTUAL';
    if (deliveryType === 'VIRTUAL' && !book.virtualEdition) {
      showToast("This title isn't available right now", 'error');
      return;
    }

    setBusyId(book.id);
    try {
      await dispatch(addToCart({ bookId: book.id, qty: 1, deliveryType, storeId: storeId ?? undefined })).unwrap();
      showToast(`${deliveryType === 'VIRTUAL' ? 'Virtual edition' : 'Added'} — "${book.title}" is in your cart`);
    } catch {
      showToast('Could not add to cart', 'error');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className={styles.list}>
      {books.map((book) => {
        // A book recommended in chat could in principle have lines of both delivery types in
        // cart already (e.g. added elsewhere) — arbitrarily surfaces whichever comes first rather
        // than trying to represent both in this small a row.
        const cartLine = cartItems.find((i) => i.bookId === book.id);
        const busy = busyId === book.id;
        const authorNames = book.authors.map((a) => a.name).join(', ');

        return (
          <div
            className={styles.row}
            key={book.id}
            role="button"
            tabIndex={0}
            onClick={() => navigate(ROUTES.bookDetail(book.id))}
            onKeyDown={(e) => {
              // Guard against a keydown bubbling up from a nested button (Add to cart, qty
              // steppers) — only navigate when the row itself is the focused element.
              if ((e.key === 'Enter' || e.key === ' ') && e.target === e.currentTarget) {
                navigate(ROUTES.bookDetail(book.id));
              }
            }}
          >
            <div className={styles.cover}>
              {book.images[0] ? <img src={book.images[0]} alt={book.title} /> : <BookOpen size={18} />}
            </div>

            <div className={styles.info}>
              <span className={styles.title}>{book.title}</span>
              {authorNames && <span className={styles.author}>{authorNames}</span>}
              <span className={styles.price}>₹{book.listPrice}</span>
            </div>

            {cartLine ? (
              cartLine.deliveryType === 'VIRTUAL' ? (
                <button
                  type="button"
                  className={[styles.qtyStepper, styles.qtyStepperVirtual].join(' ')}
                  onClick={(e) => {
                    e.stopPropagation();
                    changeQty(book.id, 'VIRTUAL', 0);
                  }}
                  disabled={busy}
                  aria-label="Remove virtual edition from cart"
                  title="A virtual edition is a single digital copy"
                >
                  <Minus size={12} />
                  In cart
                </button>
              ) : (
                <div className={styles.qtyStepper} onClick={(e) => e.stopPropagation()}>
                  <button
                    type="button"
                    className={styles.qtyStepperButton}
                    onClick={() => changeQty(book.id, 'PHYSICAL', cartLine.qty - 1)}
                    disabled={busy}
                    aria-label={cartLine.qty === 1 ? 'Remove from cart' : 'Decrease quantity'}
                  >
                    <Minus size={12} />
                  </button>
                  <span className={styles.qtyStepperValue}>{cartLine.qty}</span>
                  <button
                    type="button"
                    className={styles.qtyStepperButton}
                    onClick={() => changeQty(book.id, 'PHYSICAL', cartLine.qty + 1)}
                    disabled={busy || cartLine.qty >= MAX_QTY_PER_TITLE}
                    aria-label="Increase quantity"
                  >
                    <Plus size={12} />
                  </button>
                </div>
              )
            ) : (
              <button
                type="button"
                className={styles.addButton}
                onClick={(e) => {
                  e.stopPropagation();
                  addDirectly(book);
                }}
                disabled={busy}
              >
                {busy ? 'Adding…' : 'Add to cart'}
              </button>
            )}
          </div>
        );
      })}
    </div>
  );
}
