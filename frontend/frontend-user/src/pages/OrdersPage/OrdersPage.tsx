import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BookOpen, ChevronRight, Package } from 'lucide-react';
import { listOrders } from '@/api/orderApi';
import type { OrderSummary } from '@/types/order';
import { Card } from '@/components/Card';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { EmptyState } from '@/components/EmptyState';
import { ROUTES } from '@/constants/routes';
import styles from './OrdersPage.module.css';

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

export function OrdersPage() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listOrders(0, 20)
      .then((result) => setOrders(result.content))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h1>Your orders</h1>

      {loading ? (
        <p style={{ color: 'var(--color-text-muted)', marginTop: 'var(--space-4)' }}>Loading…</p>
      ) : orders.length === 0 ? (
        <Card style={{ marginTop: 'var(--space-5)' }}>
          <EmptyState
            icon={Package}
            title="No orders yet"
            description="Once you place an order, it'll show up here with its live status."
            action={
              <Button onClick={() => navigate(ROUTES.home)}>
                <BookOpen size={15} />
                Browse books
              </Button>
            }
          />
        </Card>
      ) : (
        <div className={styles.list}>
          {orders.map((order) => (
            <Link key={order.orderId} to={ROUTES.orderDetail(order.orderId)}>
              <Card className={styles.card}>
                <span className={styles.icon}>
                  <Package size={17} />
                </span>
                <span className={styles.info}>
                  <div className={styles.orderNumber}>{order.orderNumber}</div>
                  <div className={styles.meta}>
                    {new Date(order.placedAt).toLocaleDateString(undefined, {
                      day: 'numeric',
                      month: 'short',
                      year: 'numeric',
                    })}
                  </div>
                </span>
                <Badge variant={statusVariant(order.status)} dot>
                  {prettyStatus(order.status)}
                </Badge>
                <span className={styles.total}>₹{order.grandTotal}</span>
                <ChevronRight size={16} className={styles.chevron} />
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
