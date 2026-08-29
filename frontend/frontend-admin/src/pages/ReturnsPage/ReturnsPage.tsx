import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, ChevronLeft, ChevronRight, PackageSearch } from 'lucide-react';
import { listPendingReturns, listReviewedReturns } from '@/api/orderApi';
import type { AdminOrderSummary } from '@/types/order';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { PageHeader } from '@/components/PageHeader';
import { ROUTES } from '@/constants/routes';
import styles from './ReturnsPage.module.css';

type Tab = 'pending' | 'reviewed';

function prettyStatus(status: string) {
  return status
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

function statusVariant(status: string) {
  if (status === 'CANCELLED' || status === 'RETURN_REJECTED') return 'danger' as const;
  if (status === 'RETURNED') return 'neutral' as const;
  return 'warning' as const;
}

function refundVariant(status: string | null) {
  if (status === 'COMPLETED') return 'success' as const;
  if (status === 'FAILED') return 'danger' as const;
  return 'warning' as const;
}

function needsReview(order: AdminOrderSummary): boolean {
  return order.status === 'RETURN_REQUESTED' || (order.status === 'CANCELLED' && !order.adminReviewedAt);
}

function ReturnRow({ order, onClick }: { order: AdminOrderSummary; onClick: () => void }) {
  return (
    <button
      type="button"
      className={[styles.row, needsReview(order) && styles.rowNeedsReview].filter(Boolean).join(' ')}
      onClick={onClick}
    >
      <div className={styles.rowLeft}>
        <div className={styles.rowMeta}>
          <span className={styles.orderNumber}>{order.orderNumber}</span>
          <span className={styles.dot}>·</span>
          <span className={styles.date}>
            {new Date(order.placedAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
          </span>
          {order.grandTotal && (
            <>
              <span className={styles.dot}>·</span>
              <span className={styles.amount}>₹{order.grandTotal}</span>
            </>
          )}
        </div>
        <div className={styles.rowBadges}>
          <Badge variant={statusVariant(order.status)}>{prettyStatus(order.status)}</Badge>
          {order.refundStatus && (
            <Badge variant={refundVariant(order.refundStatus)}>
              Refund: {order.refundStatus.toLowerCase()}
              {order.refundAmount ? ` · ₹${order.refundAmount}` : ''}
            </Badge>
          )}
          {needsReview(order) && <Badge variant="warning">Needs review</Badge>}
          {order.adminReviewedAt && !needsReview(order) && <Badge variant="neutral">Reviewed</Badge>}
        </div>
        {order.cancelReason && <p className={styles.reason}>{order.cancelReason}</p>}
        {order.adminNote && order.adminReviewedAt && <p className={styles.note}>{order.adminNote}</p>}
      </div>
      <ArrowRight size={15} className={styles.arrow} />
    </button>
  );
}

export function ReturnsPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>('pending');
  const [page, setPage] = useState(0);
  const [orders, setOrders] = useState<AdminOrderSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  const reload = () => {
    setLoading(true);
    const fetcher = tab === 'pending' ? listPendingReturns : listReviewedReturns;
    fetcher(page, 20)
      .then((result) => {
        setOrders(result.content);
        setTotalPages(result.totalPages);
      })
      .finally(() => setLoading(false));
  };

  // Reset page when tab changes
  useEffect(() => {
    setPage(0);
    setOrders([]);
  }, [tab]);

  useEffect(() => {
    reload();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, page]);

  const switchTab = (next: Tab) => {
    if (next !== tab) setTab(next);
  };

  return (
    <div>
      <PageHeader
        title="Returns & cancellations"
        subtitle="Cancelled and returned orders at your store."
      />

      <div className={styles.tabs}>
        <button
          type="button"
          className={[styles.tab, tab === 'pending' && styles.tabActive].filter(Boolean).join(' ')}
          onClick={() => switchTab('pending')}
        >
          Pending review
        </button>
        <button
          type="button"
          className={[styles.tab, tab === 'reviewed' && styles.tabActive].filter(Boolean).join(' ')}
          onClick={() => switchTab('reviewed')}
        >
          Reviewed
        </button>
      </div>

      <div className={styles.list}>
        {loading ? (
          Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className={styles.skeletonRow}>
              <div className={styles.skeletonMain}>
                <div className="shimmer" style={{ width: 120, height: 14, borderRadius: 4 }} />
                <div className="shimmer" style={{ width: 72, height: 14, borderRadius: 4 }} />
              </div>
              <div className={styles.skeletonRight}>
                <div className="shimmer" style={{ width: 80, height: 20, borderRadius: 20 }} />
                <div className="shimmer" style={{ width: 60, height: 20, borderRadius: 20 }} />
              </div>
            </div>
          ))
        ) : orders.length === 0 ? (
          <div className={styles.empty}>
            <PackageSearch size={36} />
            <p>
              {tab === 'pending'
                ? 'No cases pending review — all caught up!'
                : 'No reviewed cases yet.'}
            </p>
          </div>
        ) : (
          orders.map((order) => (
            <ReturnRow
              key={order.orderId}
              order={order}
              onClick={() => navigate(ROUTES.returnDetail(order.orderId))}
            />
          ))
        )}
      </div>

      {totalPages > 1 && (
        <div className={styles.pagination}>
          <span className={styles.pageLabel}>Page {page + 1} of {totalPages}</span>
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
    </div>
  );
}
