import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPin, Package, Truck, CheckCheck, ChevronRight } from 'lucide-react';
import { getMine, markDelivered, markOutForDelivery } from '@/api/deliveryApi';
import { extractErrorMessage } from '@/api/client';
import type { Assignment, AssignmentStatus } from '@/types/delivery';
import { Card } from '@/components/Card';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { useToast } from '@/components/Toast';
import { ROUTES } from '@/constants/routes';
import styles from './MyDeliveriesPage.module.css';

function statusVariant(status: AssignmentStatus) {
  if (status === 'DELIVERED') return 'success' as const;
  if (status === 'OUT_FOR_DELIVERY') return 'info' as const;
  return 'warning' as const;
}

function statusLabel(status: AssignmentStatus) {
  if (status === 'OUT_FOR_DELIVERY') return 'Out for delivery';
  return status.charAt(0) + status.slice(1).toLowerCase();
}

export function MyDeliveriesPage() {
  const { showToast } = useToast();
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const reload = () => {
    setLoading(true);
    getMine()
      .then(setAssignments)
      .finally(() => setLoading(false));
  };

  useEffect(reload, []);

  const runAction = async (id: string, action: (id: string) => Promise<Assignment>, successMessage: string) => {
    setUpdatingId(id);
    try {
      await action(id);
      showToast(successMessage);
      reload();
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not update this order'), 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div>
      <h1>My deliveries</h1>
      <p className={styles.subtitle}>Everything you've claimed, most recent first.</p>

      {loading ? (
        <p className={styles.loading}>Loading…</p>
      ) : assignments.length === 0 ? (
        <Card className={styles.empty}>
          <Package size={28} className={styles.emptyIcon} />
          <p>You haven't claimed any orders yet — check the queue.</p>
        </Card>
      ) : (
        <div className={styles.list}>
          {assignments.map((a) => (
            <Card key={a.id} className={styles.card}>
              <Link to={ROUTES.assignmentDetail(a.id)} className={styles.info}>
                <span className={styles.orderNumber}>
                  {a.orderNumber}
                  <ChevronRight size={14} className={styles.chevron} />
                </span>
                <span className={styles.metaRow}>
                  <Badge variant={statusVariant(a.status)} dot>
                    {statusLabel(a.status)}
                  </Badge>
                  {a.destinationCity && (
                    <span className={styles.destination}>
                      <MapPin size={11} />
                      {a.destinationCity}
                    </span>
                  )}
                </span>
              </Link>

              <span className={styles.payout}>₹{a.payoutAmount}</span>

              <div className={styles.actions}>
                {a.status === 'ASSIGNED' && (
                  <Button
                    size="sm"
                    onClick={() => runAction(a.id, markOutForDelivery, 'Marked out for delivery')}
                    disabled={updatingId === a.id}
                  >
                    <Truck size={14} />
                    Out for delivery
                  </Button>
                )}
                {a.status === 'OUT_FOR_DELIVERY' && (
                  <Button
                    size="sm"
                    onClick={() => runAction(a.id, markDelivered, 'Marked delivered')}
                    disabled={updatingId === a.id}
                  >
                    <CheckCheck size={14} />
                    Delivered
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
