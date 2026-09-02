import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, BookOpen, Download, Minus, Plus, ShoppingCart, Trash2, Truck } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { fetchCart, updateCartItemQty } from '@/redux/slices/cartSlice';
import { Card } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { Tooltip } from '@readora/shared-ui';
import { EmptyState } from '@readora/shared-ui';
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

  const backButton = (
    <div style={{ marginBottom: 'var(--space-2)' }}>
      <Button variant="ghost" onClick={() => navigate(-1)}>
        <ArrowLeft size={16} />
        Back
      </Button>
    </div>
  );

  if (status === 'loading' && items.length === 0) {
    return (
      <div>
        {backButton}
        <div className={styles.layout}>
          <Card>
            {Array.from({ length: 2 }).map((_, i) => (
              <div key={i} className={styles.item}>
                <div className="skeletonPulse" style={{ width: 46, height: 66, borderRadius: 'var(--radius-sm)' }} />
                <div className={styles.itemInfo}>
                  <div className="skeletonPulse" style={{ width: '40%', height: 16, marginBottom: 8, borderRadius: 4 }} />
                  <div className="skeletonPulse" style={{ width: '20%', height: 14, marginBottom: 12, borderRadius: 4 }} />
                  <div className="skeletonPulse" style={{ width: 70, height: 20, borderRadius: 10 }} />
                </div>
                <div className="skeletonPulse" style={{ width: 80, height: 32, borderRadius: 16 }} />
                <div className="skeletonPulse" style={{ width: 32, height: 32, borderRadius: 8 }} />
              </div>
            ))}
          </Card>
          <div className="skeletonPulse" style={{ height: 220, borderRadius: 'var(--radius-lg)' }} />
        </div>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div>
        {backButton}
        <div style={{ marginTop: 'var(--space-10)' }}>
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
        </div>
      </div>
    );
  }

  return (
    <div>
      {backButton}
      <div className={styles.layout}>
        <Card>
          {items.map((item) => (
            <div className={styles.item} key={`${item.bookId}:${item.deliveryType}`}>
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
                <Badge variant="neutral">
                  {item.deliveryType === 'VIRTUAL' ? <Download size={11} /> : <Truck size={11} />}
                  {item.deliveryType === 'VIRTUAL' ? 'Virtual' : 'Physical'}
                </Badge>
              </div>

              {item.deliveryType === 'VIRTUAL' ? (
                <span className={styles.qtyFixed} title="A virtual edition is a single digital copy">
                  Qty 1
                </span>
              ) : (
                <div className={styles.qtyStepper}>
                  <button
                    type="button"
                    className={styles.qtyButton}
                    onClick={() =>
                      dispatch(updateCartItemQty({ bookId: item.bookId, deliveryType: item.deliveryType, qty: item.qty - 1 }))
                    }
                    disabled={item.qty <= 1}
                    aria-label="Decrease quantity"
                  >
                    <Minus size={13} />
                  </button>
                  <span className={styles.qtyValue}>{item.qty}</span>
                  <button
                    type="button"
                    className={styles.qtyButton}
                    onClick={() =>
                      dispatch(updateCartItemQty({ bookId: item.bookId, deliveryType: item.deliveryType, qty: item.qty + 1 }))
                    }
                    disabled={item.qty >= MAX_PER_TITLE}
                    aria-label="Increase quantity"
                  >
                    <Plus size={13} />
                  </button>
                </div>
              )}

              <div className={styles.lineTotal}>₹{item.lineTotal}</div>

              <Tooltip label="Remove">
                <button
                  type="button"
                  className={styles.removeButton}
                  onClick={() => dispatch(updateCartItemQty({ bookId: item.bookId, deliveryType: item.deliveryType, qty: 0 }))}
                  aria-label={`Remove ${item.title} from cart`}
                >
                  <Trash2 size={15} />
                </button>
              </Tooltip>
            </div>
          ))}
        </Card>

        <Card className={styles.summary}>
          <div className={styles.summaryRow}>
            <span>
              Subtotal ({items.reduce((sum, i) => sum + i.qty, 0)} item
              {items.reduce((sum, i) => sum + i.qty, 0) === 1 ? '' : 's'})
            </span>
            <span>₹{subtotal}</span>
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
          <p className={styles.taxNote}>Shipping, packaging & GST are calculated at checkout.</p>
        </Card>
      </div>
    </div>
  );
}
