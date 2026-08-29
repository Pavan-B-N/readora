import { useEffect, useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { BookOpen, CheckCircle2, MapPin, MoonStar, Package, Phone, RotateCcw, Truck, User } from 'lucide-react';
import { claimAssignment, getQueue } from '@/api/deliveryApi';
import { claimReturnPickup, getReturnQueue } from '@/api/returnApi';
import { extractErrorMessage } from '@/api/client';
import { fromAssignment, fromReturnPickup } from '@/types/delivery';
import type { UnifiedJob } from '@/types/delivery';
import type { DeliveryLayoutContext } from '@/components/DeliveryLayout/DeliveryLayout';
import { Card } from '@/components/Card';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { useToast } from '@/components/Toast';
import { ROUTES } from '@/constants/routes';
import styles from './OrdersPage.module.css';

/** The "home page" — every unassigned delivery and return pickup at the agent's store, merged into one list. */
export function OrdersPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { me } = useOutletContext<DeliveryLayoutContext>();
  const [jobs, setJobs] = useState<UnifiedJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [claimingId, setClaimingId] = useState<string | null>(null);

  const reload = () => {
    setLoading(true);
    Promise.all([getQueue(), getReturnQueue()])
      .then(([deliveries, pickups]) => {
        const merged = [...deliveries.map(fromAssignment), ...pickups.map(fromReturnPickup)];
        merged.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
        setJobs(merged);
      })
      .finally(() => setLoading(false));
  };

  useEffect(reload, [me?.onDuty]);

  const onClaim = async (job: UnifiedJob) => {
    setClaimingId(job.id);
    try {
      if (job.kind === 'DELIVERY') {
        await claimAssignment(job.id);
        showToast('Order claimed');
        navigate(ROUTES.assignmentDetail(job.id));
      } else {
        await claimReturnPickup(job.id);
        showToast('Pickup claimed');
        navigate(ROUTES.returnPickupDetail(job.id));
      }
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not claim this — it may already be taken'), 'error');
      reload();
    } finally {
      setClaimingId(null);
    }
  };

  return (
    <div>
      <h1>Orders</h1>
      <p className={styles.subtitle}>Deliveries and return pickups ready at your store — first come, first served.</p>

      {loading ? (
        <p className={styles.loading}>Loading…</p>
      ) : me && !me.onDuty ? (
        <Card className={styles.empty}>
          <MoonStar size={28} className={styles.emptyIcon} />
          <p>You're off duty — go on duty to see available orders.</p>
        </Card>
      ) : jobs.length === 0 ? (
        <Card className={styles.empty}>
          <Package size={28} className={styles.emptyIcon} />
          <p>No orders waiting right now — check back soon.</p>
        </Card>
      ) : (
        <div className={styles.list}>
          {jobs.map((job) => (
            <Card key={`${job.kind}-${job.id}`} className={styles.card}>
              <div className={styles.topRow}>
                <Badge variant={job.kind === 'DELIVERY' ? 'info' : 'warning'}>
                  {job.kind === 'DELIVERY' ? (
                    <>
                      <Truck size={11} /> Delivery
                    </>
                  ) : (
                    <>
                      <RotateCcw size={11} /> Return pickup
                    </>
                  )}
                </Badge>
                <span className={styles.orderNumber}>{job.orderNumber}</span>
                <span className={styles.payout}>₹{job.payoutAmount}</span>
              </div>

              <div className={styles.metaRow}>
                {job.destinationCity && (
                  <span className={styles.metaItem}>
                    <MapPin size={12} />
                    {job.destinationCity}
                  </span>
                )}
                <span className={styles.metaItem}>Queued {new Date(job.createdAt).toLocaleString()}</span>
              </div>

              {(job.recipientName || job.recipientPhone) && (
                <div className={styles.metaRow}>
                  {job.recipientName && (
                    <span className={styles.metaItem}>
                      <User size={12} />
                      {job.recipientName}
                    </span>
                  )}
                  {job.recipientPhone && (
                    <a href={`tel:${job.recipientPhone}`} className={styles.contactLink}>
                      <Phone size={12} />
                      {job.recipientPhone}
                    </a>
                  )}
                </div>
              )}

              {job.itemsSummary && (
                <div className={styles.itemsRow}>
                  <BookOpen size={12} />
                  <span className={styles.itemsSummary}>{job.itemsSummary}</span>
                </div>
              )}

              <Button onClick={() => onClaim(job)} disabled={claimingId === job.id} block>
                <CheckCircle2 size={15} />
                {claimingId === job.id ? 'Claiming…' : 'Accept'}
              </Button>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
