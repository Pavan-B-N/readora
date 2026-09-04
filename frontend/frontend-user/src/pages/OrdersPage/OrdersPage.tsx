import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BookOpen, ChevronRight, Package } from 'lucide-react';
import { listOrders } from '@/api/orderApi';
import { extractErrorMessage } from '@/api/client';
import type { OrderItemPreview, OrderSummary } from '@/types/order';
import { useOrderStatusNotifications } from '@/hooks/useOrderStatusNotifications';
import { Card } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { EmptyState } from '@readora/shared-ui';
import { Spinner } from '@readora/shared-ui';
import { useToast } from '@readora/shared-ui';
import { ROUTES } from '@/constants/routes';
import { statusVariant, displayStatus } from '@/utils/orderStatus';
import styles from './OrdersPage.module.css';

/** How many covers the collage shows before collapsing the rest into a "+N" chip. */
const MAX_VISIBLE_COVERS = 3;

function CoverCollage({ previews, itemCount }: { previews: OrderItemPreview[]; itemCount: number }) {
  const visible = previews.slice(0, MAX_VISIBLE_COVERS);
  const hiddenCount = itemCount - visible.length;

  return (
    <div className={styles.collage}>
      {visible.map((item, i) => (
        <span className={styles.cover} key={item.bookId} style={{ zIndex: visible.length - i }}>
          {item.coverImageUrl ? <img src={item.coverImageUrl} alt="" /> : <BookOpen size={16} />}
        </span>
      ))}
      {hiddenCount > 0 && (
        <span className={styles.coverMore} style={{ zIndex: 0 }}>
          +{hiddenCount}
        </span>
      )}
    </div>
  );
}

export function OrdersPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState(true);

  const refresh = () =>
    listOrders(0, 20)
      .then((result) => setOrders(result.content))
      .catch((err) => showToast(extractErrorMessage(err, 'Could not load your orders'), 'error'));

  useEffect(() => {
    refresh().finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // A status push for any order on this page should update its badge immediately, not just tick
  // up the bell's unread count.
  useOrderStatusNotifications(
    (notificationOrderId) => orders.some((o) => o.orderId === notificationOrderId),
    refresh,
  );

  return (
    <div>
      <h1>Your orders</h1>

      {loading ? (
        <div className={styles.list} style={{ marginTop: 'var(--space-5)' }}>
          {Array.from({ length: 4 }).map((_, i) => (
            <Card className={styles.card} key={i}>
              <div className={styles.cardHeader}>
                <div className="skeletonPulse" style={{ width: 40, height: 52, borderRadius: 'var(--radius-sm)' }} />
                <div className="skeletonPulse" style={{ width: 80, height: 24, borderRadius: 12 }} />
              </div>
              <span className={styles.info}>
                <div className="skeletonPulse" style={{ width: '80%', height: 18, marginBottom: 8, borderRadius: 4 }} />
                <div className="skeletonPulse" style={{ width: '40%', height: 14, borderRadius: 4 }} />
              </span>
              <div className={styles.cardFooter}>
                <div className="skeletonPulse" style={{ width: 60, height: 20, borderRadius: 4 }} />
                <div className="skeletonPulse" style={{ width: 50, height: 16, borderRadius: 4 }} />
              </div>
            </Card>
          ))}
        </div>
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
          {orders.map((order) => {
            const primaryTitle = order.itemPreviews[0]?.title ?? order.orderNumber;
            const extraCount = order.itemCount - 1;

            return (
              <Link key={order.orderId} to={ROUTES.orderDetail(order.orderId)}>
                <Card className={styles.card}>
                  <div className={styles.cardHeader}>
                    <CoverCollage previews={order.itemPreviews} itemCount={order.itemCount} />
                    <Badge variant={statusVariant(order.status)} dot>
                      {displayStatus(order.status)}
                    </Badge>
                  </div>

                  <span className={styles.info}>
                    <div className={styles.title}>
                      {primaryTitle}
                      {extraCount > 0 && <span className={styles.extraCount}> + {extraCount} more</span>}
                    </div>
                    <div className={styles.meta}>
                      {order.orderNumber} ·{' '}
                      {new Date(order.placedAt).toLocaleDateString(undefined, {
                        day: 'numeric',
                        month: 'short',
                        year: 'numeric',
                      })}
                    </div>
                  </span>

                  <div className={styles.cardFooter}>
                    <span className={styles.total}>₹{order.grandTotal}</span>
                    <span className={styles.viewDetails}>
                      View <ChevronRight size={14} />
                    </span>
                  </div>
                </Card>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
