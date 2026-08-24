import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { cancelOrder, getOrderDetail } from '@/api/orderApi';
import type { OrderDetail } from '@/types/order';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Button } from '@/components/Button';
import styles from './OrderDetailPage.module.css';

export function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const { showToast } = useToast();
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [cancelling, setCancelling] = useState(false);

  const reload = () => {
    if (!orderId) return;
    getOrderDetail(orderId).then(setOrder);
  };

  useEffect(reload, [orderId]);

  const onCancel = async () => {
    if (!orderId) return;
    setCancelling(true);
    try {
      await cancelOrder(orderId);
      showToast('Order cancelled');
      reload();
    } catch {
      showToast('Could not cancel order', 'error');
    } finally {
      setCancelling(false);
    }
  };

  if (!order) {
    return <p>Loading…</p>;
  }

  return (
    <div>
      <div className={styles.header}>
        <div>
          <h1>{order.orderNumber}</h1>
          <span className={styles.status}>{order.status}</span>
        </div>
        {order.cancellable && (
          <Button variant="danger" onClick={onCancel} disabled={cancelling}>
            {cancelling ? 'Cancelling…' : 'Cancel order'}
          </Button>
        )}
      </div>

      <div className={styles.layout}>
        <div>
          <Card>
            <h2 className={styles.sectionTitle}>Items</h2>
            {order.items.map((item) => (
              <div className={styles.item} key={item.bookId}>
                <span>
                  {item.title} × {item.qty}
                </span>
                <span>
                  {item.lineTotal} {order.currency}
                </span>
              </div>
            ))}
          </Card>

          {order.shippingAddress && (
            <Card className={styles.spaced}>
              <h2 className={styles.sectionTitle}>Shipping address</h2>
              <div className={styles.address}>
                {order.shippingAddress.recipientName}
                <br />
                {order.shippingAddress.line1}
                <br />
                {order.shippingAddress.city}, {order.shippingAddress.postalCode}
                <br />
                {order.shippingAddress.countryCode}
              </div>
            </Card>
          )}

          <Card className={styles.spaced}>
            <h2 className={styles.sectionTitle}>Status history</h2>
            {order.history.map((entry, i) => (
              <div className={styles.historyEntry} key={i}>
                <span>{entry.toStatus}</span>
                <span>{new Date(entry.at).toLocaleString()}</span>
              </div>
            ))}
          </Card>
        </div>

        <Card>
          <div className={styles.summaryTotal}>
            <span>Total</span>
            <span>
              {order.grandTotal} {order.currency}
            </span>
          </div>
          <p className={styles.address}>Placed {new Date(order.placedAt).toLocaleString()}</p>
        </Card>
      </div>
    </div>
  );
}
