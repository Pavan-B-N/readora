import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Download, Truck, XCircle } from 'lucide-react';
import { cancelOrder, getOrderDetail } from '@/api/orderApi';
import type { OrderDetail } from '@/types/order';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Button } from '@/components/Button';
import { Badge } from '@/components/Badge';
import styles from './OrderDetailPage.module.css';

function statusVariant(status: string) {
  if (status === 'DELIVERED' || status === 'CONFIRMED' || status === 'PAID') return 'success' as const;
  if (status === 'CANCELLED' || status === 'PAYMENT_FAILED') return 'danger' as const;
  if (status === 'SHIPPED') return 'info' as const;
  return 'warning' as const;
}

function prettyStatus(status: string) {
  return status
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

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
      showToast('Order cancelled — a refund is on its way');
      reload();
    } catch {
      showToast('Could not cancel this order', 'error');
    } finally {
      setCancelling(false);
    }
  };

  if (!order) return <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>;

  return (
    <div>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <h1 className={styles.orderNumber}>{order.orderNumber}</h1>
          <span className={styles.placedAt}>
            Placed {new Date(order.placedAt).toLocaleString()}
          </span>
          <div className={styles.deliveryBadgeRow}>
            <Badge variant={statusVariant(order.status)} dot>
              {prettyStatus(order.status)}
            </Badge>
            <Badge variant="neutral">
              {order.deliveryType === 'VIRTUAL' ? <Download size={11} /> : <Truck size={11} />}
              {order.deliveryType === 'VIRTUAL' ? 'Virtual' : 'Physical'}
            </Badge>
          </div>
        </div>
        {order.cancellable && (
          <Button variant="danger" onClick={onCancel} disabled={cancelling}>
            <XCircle size={15} />
            {cancelling ? 'Cancelling…' : 'Cancel order'}
          </Button>
        )}
      </div>

      <div className={styles.layout}>
        <div className={styles.stack}>
          <Card>
            <CardHeader title="Items" />
            {order.items.map((item) => (
              <div className={styles.item} key={item.bookId}>
                <span className={styles.itemName}>
                  {item.title}
                  <div className={styles.itemMeta}>
                    ₹{item.unitPrice} × {item.qty}
                    {item.isbn13 ? ` · ${item.isbn13}` : ''}
                  </div>
                </span>
                <span className={styles.itemTotal}>₹{item.lineTotal}</span>
              </div>
            ))}
          </Card>

          {order.shippingAddress && (
            <Card>
              <CardHeader title="Shipping address" />
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

          <Card>
            <CardHeader title="Status history" />
            <div className={styles.timeline}>
              {order.history.map((entry, i) => (
                <div className={styles.timelineEntry} key={i}>
                  <span
                    className={[styles.timelineDot, i === order.history.length - 1 && styles.timelineDotLatest]
                      .filter(Boolean)
                      .join(' ')}
                  />
                  <span className={styles.timelineText}>
                    <span className={styles.timelineStatus}>{prettyStatus(entry.toStatus)}</span>
                    <span className={styles.timelineTime}>{new Date(entry.at).toLocaleString()}</span>
                  </span>
                </div>
              ))}
            </div>
          </Card>
        </div>

        <Card>
          <div className={styles.summaryTotal}>
            <span>Total</span>
            <span>₹{order.grandTotal}</span>
          </div>
          <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-subtle)' }}>
            {order.deliveryType === 'VIRTUAL'
              ? 'Digital delivery — no shipping required.'
              : 'Cancellable within 48 hours, before shipping.'}
          </p>
        </Card>
      </div>
    </div>
  );
}
