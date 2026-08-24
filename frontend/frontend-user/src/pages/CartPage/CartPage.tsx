import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { X } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { fetchCart, updateCartItemQty } from '@/redux/slices/cartSlice';
import { Card } from '@/components/Card';
import { Button } from '@/components/Button';
import { ROUTES } from '@/constants/routes';
import styles from './CartPage.module.css';

export function CartPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { items, subtotal, currency, status } = useAppSelector((state) => state.cart);

  useEffect(() => {
    dispatch(fetchCart());
  }, [dispatch]);

  if (status === 'loading' && items.length === 0) {
    return <p>Loading…</p>;
  }

  if (items.length === 0) {
    return (
      <div>
        <h1>Your cart</h1>
        <p className={styles.empty}>Your cart is empty.</p>
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
              <div>
                <div className={styles.itemTitle}>{item.title}</div>
                <div className={styles.itemPrice}>
                  {item.unitPrice} {currency} each
                </div>
              </div>

              <div className={styles.qtyControls}>
                <select
                  className={styles.qtySelect}
                  value={item.qty}
                  onChange={(e) => dispatch(updateCartItemQty({ bookId: item.bookId, qty: Number(e.target.value) }))}
                >
                  {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => (
                    <option key={n} value={n}>
                      {n}
                    </option>
                  ))}
                </select>
                <button
                  aria-label="Remove"
                  onClick={() => dispatch(updateCartItemQty({ bookId: item.bookId, qty: 0 }))}
                >
                  <X size={16} />
                </button>
              </div>

              <div className={styles.lineTotal}>
                {item.lineTotal} {currency}
              </div>
            </div>
          ))}
        </Card>

        <Card>
          <div className={styles.summaryRow}>
            <span>Subtotal</span>
            <span>
              {subtotal} {currency}
            </span>
          </div>
          <div className={styles.summaryTotal}>
            <span>Estimated total</span>
            <span>
              {subtotal} {currency}
            </span>
          </div>
          <Button onClick={() => navigate(ROUTES.checkout)}>Proceed to checkout</Button>
        </Card>
      </div>
    </div>
  );
}
