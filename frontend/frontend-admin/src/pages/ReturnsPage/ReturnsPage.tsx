import { useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight, MessageSquarePlus } from 'lucide-react';
import { listReturns, reviewOrder } from '@/api/orderApi';
import type { AdminOrderSummary } from '@/types/order';
import { Card } from '@/components/Card';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { Input } from '@/components/Input';
import { PageHeader } from '@/components/PageHeader';
import { useToast } from '@/components/Toast';
import styles from './ReturnsPage.module.css';

function statusVariant(status: string) {
  if (status === 'CANCELLED') return 'danger' as const;
  return 'warning' as const;
}

function refundVariant(status: string | null) {
  if (status === 'COMPLETED') return 'success' as const;
  if (status === 'FAILED') return 'danger' as const;
  return 'warning' as const;
}

export function ReturnsPage() {
  const { showToast } = useToast();
  const [page, setPage] = useState(0);
  const [orders, setOrders] = useState<AdminOrderSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [noteDrafts, setNoteDrafts] = useState<Record<string, string>>({});
  const [savingId, setSavingId] = useState<string | null>(null);

  const reload = () => {
    setLoading(true);
    listReturns(page, 20)
      .then((result) => {
        setOrders(result.content);
        setTotalPages(result.totalPages);
      })
      .finally(() => setLoading(false));
  };

  useEffect(reload, [page]);

  const onReview = async (orderId: string) => {
    const note = (noteDrafts[orderId] ?? '').trim();
    if (!note) {
      showToast('Add a note before marking this reviewed', 'error');
      return;
    }
    setSavingId(orderId);
    try {
      await reviewOrder(orderId, note);
      showToast('Marked reviewed');
      reload();
    } catch {
      showToast('Could not save this review', 'error');
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div>
      <PageHeader
        title="Returns & cancellations"
        subtitle="Cancelled and returned orders at your store, with refund status from payment-service."
      />

      <Card flush>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Order</th>
              <th>Status</th>
              <th>Refund</th>
              <th>Reason</th>
              <th>Review</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr className={styles.skeletonRow} key={i}>
                  {Array.from({ length: 5 }).map((__, j) => (
                    <td key={j}>
                      <div className={styles.skeleton} style={{ width: j === 0 ? '70%' : '50%' }} />
                    </td>
                  ))}
                </tr>
              ))
            ) : orders.length === 0 ? (
              <tr>
                <td colSpan={5} className={styles.empty}>
                  No cancellations or returns at your store.
                </td>
              </tr>
            ) : (
              orders.map((order) => (
                <tr key={order.orderId}>
                  <td>
                    <div className={styles.orderCell}>
                      <span className={styles.orderNumber}>{order.orderNumber}</span>
                      <span className={styles.orderMeta}>{new Date(order.placedAt).toLocaleDateString()}</span>
                    </div>
                  </td>
                  <td>
                    <Badge variant={statusVariant(order.status)}>{order.status}</Badge>
                  </td>
                  <td>
                    {order.refundStatus ? (
                      <div className={styles.refundCell}>
                        <Badge variant={refundVariant(order.refundStatus)}>{order.refundStatus}</Badge>
                        {order.refundAmount && <span className={styles.refundAmount}>₹{order.refundAmount}</span>}
                      </div>
                    ) : (
                      <span className={styles.pending}>Pending</span>
                    )}
                  </td>
                  <td className={styles.reason}>{order.cancelReason ?? '—'}</td>
                  <td>
                    {order.adminReviewedAt ? (
                      <div className={styles.reviewed}>
                        <span className={styles.reviewedLabel}>Reviewed</span>
                        <span className={styles.reviewedNote}>{order.adminNote}</span>
                      </div>
                    ) : (
                      <div className={styles.reviewForm}>
                        <Input
                          placeholder="Add a note…"
                          value={noteDrafts[order.orderId] ?? ''}
                          onChange={(e) => setNoteDrafts((d) => ({ ...d, [order.orderId]: e.target.value }))}
                        />
                        <Button size="sm" variant="secondary" onClick={() => onReview(order.orderId)} disabled={savingId === order.orderId}>
                          <MessageSquarePlus size={14} />
                          {savingId === order.orderId ? 'Saving…' : 'Mark reviewed'}
                        </Button>
                      </div>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {totalPages > 1 && (
          <div className={styles.pagination}>
            <span>
              Page {page + 1} of {totalPages}
            </span>
            <div className={styles.paginationActions}>
              <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                <ChevronLeft size={14} />
                Previous
              </Button>
              <Button variant="secondary" size="sm" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
                Next
                <ChevronRight size={14} />
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
