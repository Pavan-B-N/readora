import { useEffect, useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { MapPin, MoonStar, RotateCcw, CheckCircle2 } from 'lucide-react';
import { claimReturnPickup, getReturnQueue } from '@/api/returnApi';
import { extractErrorMessage } from '@/api/client';
import type { ReturnPickup } from '@/types/delivery';
import type { DeliveryLayoutContext } from '@/components/DeliveryLayout/DeliveryLayout';
import { Card } from '@/components/Card';
import { Button } from '@/components/Button';
import { useToast } from '@/components/Toast';
import { ROUTES } from '@/constants/routes';
import styles from './ReturnQueuePage.module.css';

export function ReturnQueuePage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { me } = useOutletContext<DeliveryLayoutContext>();
  const [pickups, setPickups] = useState<ReturnPickup[]>([]);
  const [loading, setLoading] = useState(true);
  const [claimingId, setClaimingId] = useState<string | null>(null);

  const reload = () => {
    setLoading(true);
    getReturnQueue()
      .then(setPickups)
      .finally(() => setLoading(false));
  };

  useEffect(reload, [me?.onDuty]);

  const onClaim = async (id: string) => {
    setClaimingId(id);
    try {
      await claimReturnPickup(id);
      showToast('Pickup claimed');
      navigate(ROUTES.returnPickupDetail(id));
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not claim this pickup — it may already be taken'), 'error');
      reload();
    } finally {
      setClaimingId(null);
    }
  };

  return (
    <div>
      <h1>Available pickups</h1>
      <p className={styles.subtitle}>Approved returns ready for pickup at your store — first come, first served.</p>

      {loading ? (
        <p className={styles.loading}>Loading…</p>
      ) : me && !me.onDuty ? (
        <Card className={styles.empty}>
          <MoonStar size={28} className={styles.emptyIcon} />
          <p>You're off duty — go on duty to see available pickups.</p>
        </Card>
      ) : pickups.length === 0 ? (
        <Card className={styles.empty}>
          <RotateCcw size={28} className={styles.emptyIcon} />
          <p>No pickups waiting right now — check back soon.</p>
        </Card>
      ) : (
        <div className={styles.list}>
          {pickups.map((p) => (
            <Card key={p.id} className={styles.card}>
              <div className={styles.info}>
                <span className={styles.orderNumber}>{p.orderNumber}</span>
                <span className={styles.meta}>
                  {p.destinationCity && (
                    <span className={styles.destination}>
                      <MapPin size={11} />
                      {p.destinationCity}
                    </span>
                  )}
                  Queued {new Date(p.createdAt).toLocaleString()}
                </span>
              </div>
              <span className={styles.payout}>₹{p.payoutAmount}</span>
              <Button onClick={() => onClaim(p.id)} disabled={claimingId === p.id}>
                <CheckCircle2 size={15} />
                {claimingId === p.id ? 'Claiming…' : 'Accept'}
              </Button>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
