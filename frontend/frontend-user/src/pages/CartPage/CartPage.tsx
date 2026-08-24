import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BookOpen, Minus, Plus, ShoppingCart, Trash2 } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { fetchCart, updateCartItemQty } from '@/redux/slices/cartSlice';
import { Card } from '@/components/Card';
import { Button } from '@/components/Button';
import { Tooltip } from '@/components/Tooltip';
import { EmptyState } from '@/components/EmptyState';
import { ROUTES } from '@/constants/routes';
import styles from './CartPage.module.css';

const MAX_PER_TITLE = 10;

export function CartPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { items, subtotal, currency, status } = useAppSelector((state) => state.cart);

  useEffect(() => {
    dispatch(fetchCart());
  }, [dispatch]);

  if (status === 'loading' && items.length === 0) {
    return <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>;
  }

  if (items.length === 0) {
    return (
      <div>
        <h1>Your cart</h1>
        <Card style={{ marginTop: 'var(--space-5)' }}>
          <EmptyState
            icon={ShoppingCart}
            title="Your cart is empty"
            description="Browse the catalogue and add a few books to get started."
            action={
              <Button onClick={() => navigate(ROUTES.home)}>
                <BookOpen size={15} />
                Browse books
              </Button>
            }
          />
        </Card>
      </div>
    );
  }

  return (
    <div>
      <h1>Your cart</h1>
      <div className={styles.layout}>
        <Card>
          {items.map((item) => (
            <div className={styles.item} key={item.bookId}>
              <Link to={ROUTES.bookDetail(item.bookId)} className={styles.cover}>
                <BookOpen size={17} />
              </Link>

              <div className={styles.itemInfo}>
                <Link to={ROUTES.bookDetail(item.bookId)} className={styles.itemTitle}>
                  {item.title}
                </Link>
                <div className={styles.itemPrice}>
                  ₹{item.unitPrice} {currency} each
                </div>
              </div>

              <div className={styles.qtyStepper}>
                <button
                  type="button"
                  className={styles.qtyButton}
                  onClick={() => dispatch(updateCartItemQty({ bookId: item.bookId, qty: item.qty - 1 }))}
                  disabled={item.qty <= 1}
                  aria-label="Decrease quantity"
                >
                  <Minus size={13} />
                </button>
                <span className={styles.qtyValue}>{item.qty}</span>
                <button
                  type="button"
                  className={styles.qtyButton}
                  onClick={() => dispatch(updateCartItemQty({ bookId: item.bookId, qty: item.qty + 1 }))}
                  disabled={item.qty >= MAX_PER_TITLE}
                  aria-label="Increase quantity"
                >
                  <Plus size={13} />
                </button>
              </div>

              <div className={styles.lineTotal}>₹{item.lineTotal}</div>

              <Tooltip label="Remove">
                <button
                  type="button"
                  className={styles.removeButton}
                  onClick={() => dispatch(updateCartItemQty({ bookId: item.bookId, qty: 0 }))}
                  aria-label={`Remove ${item.title} from cart`}
                >
                  <Trash2 size={15} />
                </button>
              </Tooltip>
            </div>
          ))}
        </Card>

        <Card>
          <div className={styles.summaryRow}>
            <span>
              Subtotal ({items.reduce((sum, i) => sum + i.qty, 0)} item
              {items.reduce((sum, i) => sum + i.qty, 0) === 1 ? '' : 's'})
            </span>
            <span>₹{subtotal}</span>
          </div>
          <div className={styles.summaryRow}>
            <span>Shipping</span>
            <span>Free</span>
          </div>
          <div className={styles.summaryTotal}>
            <span>Subtotal</span>
            <span>
              ₹{subtotal} <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 400 }}>{currency}</span>
            </span>
          </div>
          <Button onClick={() => navigate(ROUTES.checkout)} block>
            Proceed to checkout
          </Button>
          <p className={styles.taxNote}>Tax is calculated at checkout.</p>
        </Card>
      </div>
    </div>
  );
}
