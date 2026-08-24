import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listOrders } from '@/api/orderApi';
import type { OrderSummary } from '@/types/order';
import { Card } from '@/components/Card';
import { ROUTES } from '@/constants/routes';
import styles from './OrdersPage.module.css';

export function OrdersPage() {
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
        <p>Loading…</p>
      ) : orders.length === 0 ? (
        <p className={styles.empty}>No orders yet.</p>
      ) : (
        <div className={styles.list}>
          {orders.map((order) => (
            <Link key={order.orderId} to={ROUTES.orderDetail(order.orderId)}>
              <Card className={styles.row}>
                <div>
                  <div className={styles.orderNumber}>{order.orderNumber}</div>
                  <div className={styles.meta}>{new Date(order.placedAt).toLocaleDateString()}</div>
                </div>
                <div>
                  <span className={styles.status}>{order.status}</span>
                </div>
                <div>
                  {order.grandTotal} {order.currency}
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
