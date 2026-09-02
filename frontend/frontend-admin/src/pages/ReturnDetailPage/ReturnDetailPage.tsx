import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Check, X as XIcon } from 'lucide-react';
import { getReturn, reviewOrder } from '@/api/orderApi';
import type { AdminOrderSummary } from '@/types/order';
import { Card, CardHeader } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { Input } from '@readora/shared-ui';
import { PageHeader } from '@/components/PageHeader';
import { ReturnChatPanel } from '@/components/ReturnChatPanel';
import { useToast } from '@readora/shared-ui';
import { ROUTES } from '@/constants/routes';
import styles from './ReturnDetailPage.module.css';

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

export function ReturnDetailPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [order, setOrder] = useState<AdminOrderSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [note, setNote] = useState('');
  const [saving, setSaving] = useState(false);

  const reload = () => {
    if (!orderId) return;
    getReturn(orderId).then(setOrder);
  };

  useEffect(() => {
    if (!orderId) return;
    setLoading(true);
    getReturn(orderId)
      .then(setOrder)
      .finally(() => setLoading(false));
  }, [orderId]);

  const onDecide = async (decision: 'APPROVE' | 'REJECT') => {
    if (!orderId) return;
    if (!note.trim()) {
      showToast('Add a note explaining your decision first', 'error');
      return;
    }
    setSaving(true);
    try {
      await reviewOrder(orderId, note.trim(), decision);
      showToast(decision === 'APPROVE' ? 'Return approved — a pickup was queued' : 'Return rejected');
      reload();
    } catch {
      showToast('Could not save this decision', 'error');
    } finally {
      setSaving(false);
    }
  };

  const onMarkReviewed = async () => {
    if (!orderId) return;
    if (!note.trim()) {
      showToast('Add a note before marking this reviewed', 'error');
      return;
    }
    setSaving(true);
    try {
      await reviewOrder(orderId, note.trim());
      showToast('Marked reviewed');
      reload();
    } catch {
      showToast('Could not save this review', 'error');
    } finally {
      setSaving(false);
    }
  };

  if (loading || !order) return <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>;

  const awaitingDecision = order.status === 'RETURN_REQUESTED';
  const canMarkCancelledReviewed = order.status === 'CANCELLED' && !order.adminReviewedAt;

  return (
    <div>
      <PageHeader
        title={order.orderNumber}
        subtitle={`Placed ${new Date(order.placedAt).toLocaleString()}`}
        actions={
          <Button variant="secondary" onClick={() => navigate(ROUTES.returns)}>
            <ArrowLeft size={15} />
            Back to returns
          </Button>
        }
      />

      <div className={styles.layout}>
        <div className={styles.main}>
          <Card>
            <CardHeader title="Case details" />
            <div className={styles.grid}>
              <div className={styles.field}>
                <span className={styles.label}>Status</span>
                <Badge variant={statusVariant(order.status)}>{prettyStatus(order.status)}</Badge>
              </div>
              <div className={styles.field}>
                <span className={styles.label}>Order total</span>
                <span className={styles.value}>
                  ₹{order.grandTotal} {order.currency}
                </span>
              </div>
              <div className={styles.field}>
                <span className={styles.label}>Reason</span>
                <span className={styles.value}>{order.cancelReason ?? '—'}</span>
              </div>
              <div className={styles.field}>
                <span className={styles.label}>Cancelled / requested at</span>
                <span className={styles.value}>{order.cancelledAt ? new Date(order.cancelledAt).toLocaleString() : '—'}</span>
              </div>
              <div className={styles.field}>
                <span className={styles.label}>Refund</span>
                {order.refundStatus ? (
                  <span className={styles.value}>
                    <Badge variant={refundVariant(order.refundStatus)}>{order.refundStatus}</Badge>
                    {order.refundAmount && ` · ₹${order.refundAmount}`}
                  </span>
                ) : (
                  <span className={styles.pending}>Pending</span>
                )}
              </div>
              <div className={styles.field}>
                <span className={styles.label}>Refund completed</span>
                <span className={styles.value}>
                  {order.refundCompletedAt ? new Date(order.refundCompletedAt).toLocaleString() : '—'}
                </span>
              </div>
              {order.adminReviewedAt && (
                <>
                  <div className={styles.field}>
                    <span className={styles.label}>Reviewed at</span>
                    <span className={styles.value}>{new Date(order.adminReviewedAt).toLocaleString()}</span>
                  </div>
                  <div className={styles.field}>
                    <span className={styles.label}>Admin note</span>
                    <span className={styles.value}>{order.adminNote ?? '—'}</span>
                  </div>
                </>
              )}
            </div>
          </Card>

          <Card>
            <CardHeader title="Conversation" subtitle="Messages between you and the customer about this case." />
            <ReturnChatPanel orderId={order.orderId} locked={!awaitingDecision} />
          </Card>
        </div>

        <div className={styles.sidebar}>
          {(awaitingDecision || canMarkCancelledReviewed) && (
            <Card>
              <CardHeader title={awaitingDecision ? 'Decide this return' : 'Review this cancellation'} />
              <div className={styles.decisionForm}>
                <Input
                  label="Note"
                  placeholder="Explain your decision…"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                />
                {awaitingDecision ? (
                  <div className={styles.decisionActions}>
                    <Button variant="danger" onClick={() => onDecide('REJECT')} disabled={saving}>
                      <XIcon size={14} />
                      Reject
                    </Button>
                    <Button onClick={() => onDecide('APPROVE')} disabled={saving}>
                      <Check size={14} />
                      Approve
                    </Button>
                  </div>
                ) : (
                  <Button onClick={onMarkReviewed} disabled={saving} block>
                    {saving ? 'Saving…' : 'Mark reviewed'}
                  </Button>
                )}
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
