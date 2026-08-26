import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Check, Download, ShoppingCart, Truck } from 'lucide-react';
import type { BookDetail } from '@/types/catalog';
import type { DeliveryType } from '@/types/cart';
import { useAppDispatch } from '@/redux/hooks';
import { addToCart } from '@/redux/slices/cartSlice';
import { Modal } from '@/components/Modal';
import { Button } from '@/components/Button';
import { ROUTES } from '@/constants/routes';
import styles from './ChatBookPicker.module.css';

interface ChatBookPickerProps {
  book: BookDetail | null;
  onClose: () => void;
}

export function ChatBookPicker({ book, onClose }: ChatBookPickerProps) {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [added, setAdded] = useState<DeliveryType | null>(null);

  const close = () => {
    setAdded(null);
    onClose();
  };

  if (!book) return null;

  const inStock = book.availability.status === 'IN_STOCK';

  const onAdd = async (deliveryType: DeliveryType) => {
    setBusy(true);
    try {
      await dispatch(addToCart({ bookId: book.id, qty: 1, deliveryType })).unwrap();
      setAdded(deliveryType);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal open={Boolean(book)} onClose={close} title={added ? 'Added to cart' : 'Choose an edition'} width={420}>
      {added ? (
        <div className={styles.confirmed}>
          <span className={styles.confirmedIcon}>
            <Check size={18} />
          </span>
          <p className={styles.confirmedText}>
            {added === 'VIRTUAL' ? 'The virtual edition' : 'A physical copy'} of "{book.title}" is in your cart.
          </p>
          <div className={styles.confirmedActions}>
            <Button variant="secondary" onClick={close}>
              Keep chatting
            </Button>
            <Button onClick={() => navigate(ROUTES.checkout)}>Go to checkout</Button>
          </div>
        </div>
      ) : (
        <div className={styles.choices}>
          {inStock && (
            <button type="button" className={styles.choiceCard} onClick={() => onAdd('PHYSICAL')} disabled={busy}>
              <span className={styles.choiceIcon}>
                <Truck size={18} />
              </span>
              <span className={styles.choiceText}>
                <span className={styles.choiceName}>Physical copy</span>
                <span className={styles.choiceHint}>Delivered in ~30 min from your store</span>
              </span>
              <span className={styles.choicePrice}>₹{book.listPrice}</span>
            </button>
          )}
          {book.virtualEdition && (
            <button type="button" className={styles.choiceCard} onClick={() => onAdd('VIRTUAL')} disabled={busy}>
              <span className={styles.choiceIcon}>
                <Download size={18} />
              </span>
              <span className={styles.choiceText}>
                <span className={styles.choiceName}>Virtual edition</span>
                <span className={styles.choiceHint}>Instant access, read in-app</span>
              </span>
              <span className={styles.choicePrice}>₹{book.virtualEdition.price}</span>
            </button>
          )}
          {!inStock && !book.virtualEdition && (
            <p className={styles.unavailable}>
              <ShoppingCart size={14} />
              This title isn't available right now.
            </p>
          )}
        </div>
      )}
    </Modal>
  );
}
