import { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { BookOpen, Loader2, RotateCcw, XCircle } from 'lucide-react';
import { cancelOrder, getOrderDetail, returnOrder } from '@/api/orderApi';
import type { OrderDetail } from '@/types/order';
import { useOrderStatusNotifications } from '@/hooks/useOrderStatusNotifications';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Button } from '@/components/Button';
import { Badge } from '@/components/Badge';
import { Spinner } from '@/components/Spinner';
import { ReturnChatPanel } from '@/components/ReturnChatPanel';
import { Modal } from '@/components/Modal';
import { Textarea } from '@/components/Input';
import { ROUTES } from '@/constants/routes';
import styles from './OrderDetailPage.module.css';

// RETURN_REQUESTED is included here (not just the pre-return statuses) so the ebook "Read now"
// button stays visible while a return is pending admin review, matching the backend — which also
// doesn't revoke access until RETURN_APPROVED (see OrderItemRepository.findDistinctBookIdsByUserId).
const READABLE_STATUSES = new Set(['PAID', 'CONFIRMED', 'ASSIGNED', 'SHIPPED', 'DELIVERED', 'RETURN_REQUESTED']);

const RETURN_FAMILY_STATUSES = new Set([
  'RETURN_REQUESTED',
  'RETURN_REJECTED',
  'RETURN_APPROVED',
  'RETURN_ASSIGNED',
  'RETURN_EN_ROUTE',
  'RETURN_COLLECTED',
  'REFUND_INITIATED',
  'RETURNED',
]);

const POLL_INTERVAL_MS = 2000;

const TRACKER_STEPS = ['Order placed', 'Assigned to agent', 'Out for delivery', 'Delivered'];

const RETURN_TRACKER_STEPS = ['Requested', 'Approved', 'Agent assigned', 'On the way', 'Collected', 'Refunded'];

/** null for statuses the tracker doesn't apply to (pre-confirmation, cancelled, payment failed). */
function trackerStepIndex(status: string): number | null {
  switch (status) {
    case 'CONFIRMED':
      return 0;
    case 'ASSIGNED':
      return 1;
    case 'SHIPPED':
      return 2;
    case 'DELIVERED':
    case 'RETURNED':
      return 3;
    default:
      return null;
  }
}

/** null for statuses the return tracker doesn't apply to (order was never returned, or the return was rejected). */
function returnTrackerStepIndex(status: string): number | null {
  switch (status) {
    case 'RETURN_REQUESTED':
      return 0;
    case 'RETURN_APPROVED':
      return 1;
    case 'RETURN_ASSIGNED':
      return 2;
    case 'RETURN_EN_ROUTE':
      return 3;
    case 'RETURN_COLLECTED':
      return 4;
    case 'REFUND_INITIATED':
    case 'RETURNED':
      return 5;
    default:
      return null;
  }
}

function statusVariant(status: string) {
  if (status === 'DELIVERED' || status === 'CONFIRMED' || status === 'PAID') return 'success' as const;
  if (status === 'CANCELLED' || status === 'PAYMENT_FAILED' || status === 'RETURNED' || status === 'RETURN_REJECTED') {
    return 'danger' as const;
  }
  if (status === 'ASSIGNED' || status === 'SHIPPED') return 'info' as const;
  return 'warning' as const;
}

function paymentStatusVariant(status: string) {
  if (status === 'CAPTURED') return 'success' as const;
  if (status === 'FAILED') return 'danger' as const;
  if (status === 'REFUNDED') return 'info' as const;
  return 'warning' as const;
}

function prettyStatus(status: string) {
  return status
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

/** A few statuses read awkwardly as plain title-cased enum names — everything else falls through to prettyStatus(). */
function displayStatus(status: string) {
  if (status === 'SHIPPED') return 'Out for delivery';
  if (status === 'RETURN_EN_ROUTE') return 'Agent on the way';
  if (status === 'REFUND_INITIATED') return 'Refund in progress';
  if (status === 'RETURN_ASSIGNED') return 'Agent assigned';
  return prettyStatus(status);
}

function Tracker({ steps, currentIndex }: { steps: string[]; currentIndex: number }) {
  return (
    <div className={styles.tracker}>
      {steps.map((label, i) => {
        const done = i < currentIndex;
        const current = i === currentIndex;
        return (
          <div className={styles.trackerStep} key={label}>
            <span className={[styles.trackerLine, done && styles.trackerLineDone].filter(Boolean).join(' ')} />
            <span
              className={[styles.trackerDot, done && styles.trackerDotDone, current && styles.trackerDotCurrent]
                .filter(Boolean)
                .join(' ')}
            />
            <span className={[styles.trackerLabel, (done || current) && styles.trackerLabelActive].filter(Boolean).join(' ')}>
              {label}
            </span>
          </div>
        );
      })}
    </div>
  );
}

function DeliveryTracker({ status }: { status: string }) {
  const currentIndex = trackerStepIndex(status);
  if (currentIndex === null) return null;
  return <Tracker steps={TRACKER_STEPS} currentIndex={currentIndex} />;
}

/** Only meaningful once a physical return has actually started (RETURN_REQUESTED or later) — null otherwise, e.g. RETURN_REJECTED has nothing further to track. */
function ReturnTracker({ status }: { status: string }) {
  const currentIndex = returnTrackerStepIndex(status);
  if (currentIndex === null) return null;
  return <Tracker steps={RETURN_TRACKER_STEPS} currentIndex={currentIndex} />;
}

export function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [returning, setReturning] = useState(false);
  const [returnModalOpen, setReturnModalOpen] = useState(false);
  const [returnReason, setReturnReason] = useState('');
  const [returnReasonError, setReturnReasonError] = useState('');
  const pollRef = useRef<number | null>(null);

  const reload = () => {
    if (!orderId) return;
    getOrderDetail(orderId).then(setOrder);
  };

  useEffect(reload, [orderId]);

  // While a UPI payment is settling server-side, poll so the status flips without a manual refresh.
  useEffect(() => {
    if (order?.status !== 'PENDING_PAYMENT') {
      if (pollRef.current) {
        clearInterval(pollRef.current);
        pollRef.current = null;
      }
      return;
    }

    pollRef.current = window.setInterval(reload, POLL_INTERVAL_MS);
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [order?.status]);

  // A delivery-status push (e.g. "Out for delivery") should update this page immediately if it's
  // already open, not just tick up the bell's unread count.
  useOrderStatusNotifications((notificationOrderId) => notificationOrderId === orderId, reload);

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

  const onOpenReturnModal = () => {
    setReturnReason('');
    setReturnReasonError('');
    setReturnModalOpen(true);
  };

  const onSubmitReturn = async () => {
    if (!orderId) return;
    const trimmedReason = returnReason.trim();
    if (!trimmedReason) {
      setReturnReasonError('Tell us why you’re returning this order');
      return;
    }
    // Physical items need a human to sign off before anything happens; a virtual-only order has
    // nothing to inspect, so it auto-approves and starts refunding right away — see
    // OrderService.returnOrder()'s javadoc.
    const needsReview = order?.items.some((item) => item.deliveryType === 'PHYSICAL') ?? false;
    setReturning(true);
    try {
      await returnOrder(orderId, trimmedReason);
      setReturnModalOpen(false);
      showToast(needsReview ? "Return requested — we'll review it shortly" : 'Return started — a refund is on its way');
      reload();
    } catch {
      showToast('Could not return this order', 'error');
    } finally {
      setReturning(false);
    }
  };

  if (!order) {
    return (
      <div>
        <div className={styles.header}>
          <div className={styles.headerLeft}>
            <div className="skeletonPulse" style={{ width: 140, height: 28, marginBottom: 8, borderRadius: 4 }} />
            <div className="skeletonPulse" style={{ width: 220, height: 16, marginBottom: 12, borderRadius: 4 }} />
            <div className="skeletonPulse" style={{ width: 100, height: 24, borderRadius: 12 }} />
          </div>
        </div>
        <div className={styles.layout}>
          <div className={styles.stack}>
            <Card>
              <div className="skeletonPulse" style={{ width: 160, height: 20, marginBottom: 24, borderRadius: 4 }} />
              {Array.from({ length: 2 }).map((_, i) => (
                <div key={i} style={{ display: 'flex', gap: 16, marginBottom: i === 0 ? 16 : 0, paddingBottom: i === 0 ? 16 : 0, borderBottom: i === 0 ? '1px solid var(--color-border)' : 'none' }}>
                  <div className="skeletonPulse" style={{ width: 46, height: 66, borderRadius: 4 }} />
                  <div style={{ flex: 1 }}>
                    <div className="skeletonPulse" style={{ width: '40%', height: 16, marginBottom: 8, borderRadius: 4 }} />
                    <div className="skeletonPulse" style={{ width: '20%', height: 14, marginBottom: 8, borderRadius: 4 }} />
                    <div className="skeletonPulse" style={{ width: 60, height: 20, borderRadius: 10 }} />
                  </div>
                </div>
              ))}
            </Card>
          </div>
          <div className={styles.stack}>
            <Card>
              <div className="skeletonPulse" style={{ width: 120, height: 20, marginBottom: 16, borderRadius: 4 }} />
              <div className="skeletonPulse" style={{ width: '100%', height: 14, marginBottom: 8, borderRadius: 4 }} />
              <div className="skeletonPulse" style={{ width: '100%', height: 14, marginBottom: 8, borderRadius: 4 }} />
              <div className="skeletonPulse" style={{ width: '60%', height: 14, borderRadius: 4 }} />
            </Card>
          </div>
        </div>
      </div>
    );
  }

  const isSettlingUpi = order.status === 'PENDING_PAYMENT' && order.paymentMethod === 'UPI';
  // An order's items can mix PHYSICAL and VIRTUAL editions — there's no single delivery type for
  // the order as a whole, so every check below looks at the actual item lines instead of relying
  // on order.deliveryType (which only ever reflects "has at least one physical item").
  const hasPhysicalItem = order.items.some((item) => item.deliveryType === 'PHYSICAL');
  const isInReturnFlow = RETURN_FAMILY_STATUSES.has(order.status);

  return (
    <div>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <h1 className={styles.orderNumber}>{order.orderNumber}</h1>
          <span className={styles.placedAt}>
            Placed {new Date(order.placedAt).toLocaleString()}
          </span>
          <div className={styles.deliveryBadgeRow}>
            <Badge variant={statusVariant(order.status)} dot pulse={isSettlingUpi}>
              {displayStatus(order.status)}
            </Badge>
            {order.deliveryAgentName && !isInReturnFlow && (
              <Badge variant="neutral">Agent: {order.deliveryAgentName}</Badge>
            )}
            {order.returnAgentName && (
              <Badge variant="neutral">Pickup agent: {order.returnAgentName}</Badge>
            )}
          </div>
          {isSettlingUpi && (
            <span className={styles.upiPending}>
              <Loader2 size={13} className="spin" />
              Waiting for your UPI approval — this confirms automatically.
            </span>
          )}
        </div>
        <div className={styles.headerActions}>
          {order.returnable && (
            <Button variant="secondary" onClick={onOpenReturnModal} disabled={returning}>
              <RotateCcw size={15} />
              {returning ? 'Starting return…' : 'Return order'}
            </Button>
          )}
          {order.cancellable && (
            <Button variant="danger" onClick={onCancel} disabled={cancelling}>
              <XCircle size={15} />
              {cancelling ? 'Cancelling…' : 'Cancel order'}
            </Button>
          )}
        </div>
      </div>

      <div className={styles.layout}>
        <div className={styles.stack}>
          {hasPhysicalItem && !isInReturnFlow && (
            <Card>
              <CardHeader title="Delivery tracking" />
              <DeliveryTracker status={order.status} />
              {order.deliveredAt && (
                <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-subtle)' }}>
                  Delivered {new Date(order.deliveredAt).toLocaleString()}
                </p>
              )}
            </Card>
          )}

          {hasPhysicalItem && isInReturnFlow && order.status !== 'RETURN_REJECTED' && (
            <Card>
              <CardHeader title="Return tracking" />
              <ReturnTracker status={order.status} />
            </Card>
          )}

          {hasPhysicalItem && isInReturnFlow && (
            <Card>
              <CardHeader title="Return conversation" />
              <ReturnChatPanel orderId={order.orderId} locked={order.status !== 'RETURN_REQUESTED'} />
            </Card>
          )}

          <Card>
            <CardHeader title="Items" />
            {order.items.map((item) => (
              <div
                className={[styles.item, styles.itemClickable].join(' ')}
                key={`${item.bookId}:${item.deliveryType}`}
                onClick={() => navigate(ROUTES.bookDetail(item.bookId))}
              >
                <span className={styles.itemName}>
                  {item.title}
                  <div className={styles.itemMeta}>
                    ₹{item.unitPrice} × {item.qty}
                    {item.isbn13 ? ` · ${item.isbn13}` : ''} · {item.deliveryType === 'VIRTUAL' ? 'Virtual' : 'Physical'}
                  </div>
                </span>
                <span className={styles.itemTrailing}>
                  <span className={styles.itemTotal}>₹{item.lineTotal}</span>
                  {item.deliveryType === 'VIRTUAL' && READABLE_STATUSES.has(order.status) && (
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate(ROUTES.read(item.bookId));
                      }}
                    >
                      <BookOpen size={14} />
                      Read now
                    </Button>
                  )}
                </span>
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
                    <span className={styles.timelineStatus}>{displayStatus(entry.toStatus)}</span>
                    <span className={styles.timelineTime}>{new Date(entry.at).toLocaleString()}</span>
                  </span>
                </div>
              ))}
            </div>
          </Card>
        </div>

        <Card>
          <CardHeader title="Payment" />
          <div className={styles.summaryRow}>
            <span>Subtotal</span>
            <span>₹{order.subtotal}</span>
          </div>
          <div className={styles.summaryRow}>
            <span>Shipping</span>
            <span>{Number(order.shippingFee) === 0 ? 'Free' : `₹${order.shippingFee}`}</span>
          </div>
          {Number(order.packagingFee) > 0 && (
            <div className={styles.summaryRow}>
              <span>Packaging</span>
              <span>₹{order.packagingFee}</span>
            </div>
          )}
          <div className={styles.summaryRow}>
            <span>GST</span>
            <span>₹{order.taxAmount}</span>
          </div>
          <div className={styles.summaryTotal}>
            <span>Total</span>
            <span>₹{order.grandTotal}</span>
          </div>
          <div className={styles.paymentMethodRow}>
            Paid via {order.paymentMethod === 'UPI' ? 'UPI' : 'Wallet'}
            {Number(order.walletAmountUsed) > 0 && ` · ₹${order.walletAmountUsed} from wallet`}
          </div>

          {order.payment && (
            <div className={styles.transactionBox}>
              <div className={styles.summaryRow}>
                <span>Payment status</span>
                <Badge variant={paymentStatusVariant(order.payment.status)} dot>
                  {displayStatus(order.payment.status)}
                </Badge>
              </div>
              <div className={styles.summaryRow}>
                <span>Transaction ID</span>
                <span className={styles.transactionId}>{order.payment.transactionId}</span>
              </div>
              {order.payment.capturedAt && (
                <div className={styles.summaryRow}>
                  <span>Paid on</span>
                  <span>{new Date(order.payment.capturedAt).toLocaleString()}</span>
                </div>
              )}
            </div>
          )}

          <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-subtle)' }}>
            {hasPhysicalItem
              ? 'Cancellable within 48 hours, before a delivery agent is assigned.'
              : 'Digital delivery — no shipping required.'}
          </p>
        </Card>
      </div>

      <Modal open={returnModalOpen} onClose={() => setReturnModalOpen(false)} title="Return this order" width={420}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          <Textarea
            label="Reason for return"
            required
            rows={3}
            placeholder="e.g. Item arrived damaged, wrong book, changed my mind…"
            value={returnReason}
            error={returnReasonError}
            onChange={(e) => {
              setReturnReason(e.target.value);
              if (returnReasonError) setReturnReasonError('');
            }}
          />
          <Button onClick={onSubmitReturn} disabled={returning} block>
            {returning ? 'Starting return…' : 'Submit return request'}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
