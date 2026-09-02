import { useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  BookOpen,
  CheckCheck,
  CheckCircle2,
  ChevronDown,
  MapPin,
  MoonStar,
  Navigation,
  Package,
  PackageCheck,
  Phone,
  RotateCcw,
  Truck,
  User,
} from 'lucide-react';
import { claimAssignment, getMine, getQueue, markDelivered, markOutForDelivery } from '@/api/deliveryApi';
import { claimReturnPickup, getMyReturns, getReturnQueue, markReturnCollected, markReturnEnRoute } from '@/api/returnApi';
import { extractErrorMessage } from '@/api/client';
import { fromAssignment, fromReturnPickup, isActiveStatus } from '@/types/delivery';
import type { UnifiedJob } from '@/types/delivery';
import type { DeliveryLayoutContext } from '@/components/DeliveryLayout/DeliveryLayout';
import { Card } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { useToast } from '@readora/shared-ui';
import styles from './OrdersPage.module.css';

function statusVariant(job: UnifiedJob) {
  if (job.status === 'OUT_FOR_DELIVERY' || job.status === 'EN_ROUTE') return 'info' as const;
  return 'warning' as const;
}

function statusLabel(job: UnifiedJob) {
  if (job.status === 'OUT_FOR_DELIVERY') return 'Out for delivery';
  if (job.status === 'EN_ROUTE') return 'On the way';
  return job.status.charAt(0) + job.status.slice(1).toLowerCase();
}

function jobKey(job: UnifiedJob) {
  return `${job.kind}-${job.id}`;
}

/** The "home page" — a currently-in-progress section, plus everything available to claim, merged across deliveries and return pickups. */
export function OrdersPage() {
  const { showToast } = useToast();
  const { me } = useOutletContext<DeliveryLayoutContext>();
  const [activeJobs, setActiveJobs] = useState<UnifiedJob[]>([]);
  const [availableJobs, setAvailableJobs] = useState<UnifiedJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [claimingId, setClaimingId] = useState<string | null>(null);
  const [updatingId, setUpdatingId] = useState<string | null>(null);
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  const reload = () => {
    setLoading(true);
    Promise.all([getQueue(), getReturnQueue(), getMine(), getMyReturns()])
      .then(([deliveries, pickups, myDeliveries, myPickups]) => {
        const available = [...deliveries.map(fromAssignment), ...pickups.map(fromReturnPickup)];
        available.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
        setAvailableJobs(available);

        const active = [...myDeliveries.map(fromAssignment), ...myPickups.map(fromReturnPickup)].filter((j) => isActiveStatus(j.status));
        active.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
        setActiveJobs(active);
      })
      .finally(() => setLoading(false));
  };

  useEffect(reload, [me?.onDuty]);

  const toggleExpanded = (key: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const onClaim = async (job: UnifiedJob) => {
    setClaimingId(job.id);
    try {
      if (job.kind === 'DELIVERY') {
        await claimAssignment(job.id);
        showToast('Order claimed');
      } else {
        await claimReturnPickup(job.id);
        showToast('Pickup claimed');
      }
      reload();
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not claim this — it may already be taken'), 'error');
      reload();
    } finally {
      setClaimingId(null);
    }
  };

  const runAction = async (job: UnifiedJob, action: (id: string) => Promise<unknown>, successMessage: string) => {
    setUpdatingId(job.id);
    try {
      await action(job.id);
      showToast(successMessage);
      reload();
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not update this'), 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  const renderDetails = (job: UnifiedJob) => (
    <>
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

      {job.items && job.items.length > 0 && (
        <div className={styles.itemsBlock}>
          <span className={styles.itemsLabel}>
            <BookOpen size={12} />
            Items
          </span>
          <ul className={styles.itemsList}>
            {job.items.slice(0, 2).map((item, i) => (
              <li key={i} className={styles.itemRow}>
                <span>{item.title}</span>
                <span className={styles.itemQty}>× {item.qty}</span>
              </li>
            ))}
            {job.items.length > 2 && (
              <li className={styles.itemsMore}>+{job.items.length - 2} more</li>
            )}
          </ul>
        </div>
      )}
    </>
  );

  return (
    <div>
      <h1>Orders</h1>
      <p className={styles.subtitle}>What you're working on, and what's ready at your store.</p>

      {loading ? (
        <p className={styles.loading}>Loading…</p>
      ) : (
        <>
          {activeJobs.length > 0 && (
            <div className={styles.section}>
              <h2 className={styles.sectionTitle}>Active</h2>
              <div className={styles.activeList}>
                {activeJobs.map((job) => (
                  <Card key={jobKey(job)} className={styles.card}>
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
                      <Badge variant={statusVariant(job)} dot>
                        {statusLabel(job)}
                      </Badge>
                      <span className={styles.payout}>₹{job.payoutAmount}</span>
                    </div>

                    {renderDetails(job)}

                    <div className={styles.actions}>
                      {job.kind === 'DELIVERY' && job.status === 'ASSIGNED' && (
                        <Button onClick={() => runAction(job, markOutForDelivery, 'Marked out for delivery')} disabled={updatingId === job.id}>
                          <Truck size={15} />
                          Out for delivery
                        </Button>
                      )}
                      {job.kind === 'DELIVERY' && job.status === 'OUT_FOR_DELIVERY' && (
                        <Button onClick={() => runAction(job, markDelivered, 'Marked delivered')} disabled={updatingId === job.id}>
                          <CheckCheck size={15} />
                          Delivered
                        </Button>
                      )}
                      {job.kind === 'RETURN_PICKUP' && job.status === 'ASSIGNED' && (
                        <Button onClick={() => runAction(job, markReturnEnRoute, 'Marked on the way')} disabled={updatingId === job.id}>
                          <Navigation size={15} />
                          On the way
                        </Button>
                      )}
                      {job.kind === 'RETURN_PICKUP' && job.status === 'EN_ROUTE' && (
                        <Button onClick={() => runAction(job, markReturnCollected, 'Marked collected')} disabled={updatingId === job.id}>
                          <PackageCheck size={15} />
                          Collected
                        </Button>
                      )}
                    </div>
                  </Card>
                ))}
              </div>
            </div>
          )}

          <div className={styles.section}>
            <h2 className={styles.sectionTitle}>Available</h2>

            {me && !me.onDuty ? (
              <Card className={styles.empty}>
                <MoonStar size={28} className={styles.emptyIcon} />
                <p>You're off duty — go on duty to see available orders.</p>
              </Card>
            ) : availableJobs.length === 0 ? (
              <Card className={styles.empty}>
                <Package size={28} className={styles.emptyIcon} />
                <p>No orders waiting right now — check back soon.</p>
              </Card>
            ) : (
              <div className={styles.list}>
                {availableJobs.map((job) => {
                  const key = jobKey(job);
                  const expanded = expandedIds.has(key);
                  return (
                    <Card key={key} className={styles.card}>
                      <button type="button" className={styles.topRowButton} onClick={() => toggleExpanded(key)}>
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
                        <ChevronDown size={16} className={[styles.chevron, expanded && styles.chevronOpen].filter(Boolean).join(' ')} />
                      </button>

                      {expanded && renderDetails(job)}

                      <Button onClick={() => onClaim(job)} disabled={claimingId === job.id} block>
                        <CheckCircle2 size={15} />
                        {claimingId === job.id ? 'Claiming…' : 'Accept'}
                      </Button>
                    </Card>
                  );
                })}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
