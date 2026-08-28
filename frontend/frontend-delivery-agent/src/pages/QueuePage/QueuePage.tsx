import { useEffect, useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { MapPin, MoonStar, Package, CheckCircle2 } from 'lucide-react';
import { claimAssignment, getQueue } from '@/api/deliveryApi';
import { extractErrorMessage } from '@/api/client';
import type { Assignment } from '@/types/delivery';
import type { DeliveryLayoutContext } from '@/components/DeliveryLayout/DeliveryLayout';
import { Card } from '@/components/Card';
import { Button } from '@/components/Button';
import { useToast } from '@/components/Toast';
import { ROUTES } from '@/constants/routes';
import styles from './QueuePage.module.css';

export function QueuePage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { me } = useOutletContext<DeliveryLayoutContext>();
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [loading, setLoading] = useState(true);
  const [claimingId, setClaimingId] = useState<string | null>(null);

  const reload = () => {
    setLoading(true);
    getQueue()
      .then(setAssignments)
      .finally(() => setLoading(false));
  };

  useEffect(reload, [me?.onDuty]);

  const onClaim = async (id: string) => {
    setClaimingId(id);
    try {
      await claimAssignment(id);
      showToast('Order claimed');
      navigate(ROUTES.assignmentDetail(id));
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not claim this order — it may already be taken'), 'error');
      reload();
    } finally {
      setClaimingId(null);
    }
  };

  return (
    <div>
      <h1>Available orders</h1>
      <p className={styles.subtitle}>Physical orders ready for pickup at your store — first come, first served.</p>

      {loading ? (
        <p className={styles.loading}>Loading…</p>
      ) : me && !me.onDuty ? (
        <Card className={styles.empty}>
          <MoonStar size={28} className={styles.emptyIcon} />
          <p>You're off duty — go on duty to see available orders.</p>
        </Card>
      ) : assignments.length === 0 ? (
        <Card className={styles.empty}>
          <Package size={28} className={styles.emptyIcon} />
          <p>No orders waiting right now — check back soon.</p>
        </Card>
      ) : (
        <div className={styles.list}>
          {assignments.map((a) => (
            <Card key={a.id} className={styles.card}>
              <div className={styles.info}>
                <span className={styles.orderNumber}>{a.orderNumber}</span>
                <span className={styles.meta}>
                  {a.destinationCity && (
                    <span className={styles.destination}>
                      <MapPin size={11} />
                      {a.destinationCity}
                    </span>
                  )}
                  Queued {new Date(a.createdAt).toLocaleString()}
                </span>
              </div>
              <span className={styles.payout}>₹{a.payoutAmount}</span>
              <Button onClick={() => onClaim(a.id)} disabled={claimingId === a.id}>
                <CheckCircle2 size={15} />
                {claimingId === a.id ? 'Claiming…' : 'Accept'}
              </Button>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
