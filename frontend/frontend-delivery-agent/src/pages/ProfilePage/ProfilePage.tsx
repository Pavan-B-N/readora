import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  BookOpen,
  CheckCheck,
  ChevronRight,
  MapPin,
  Navigation,
  Package,
  PackageCheck,
  Phone,
  RotateCcw,
  Truck,
  User,
  Wallet,
} from 'lucide-react';
import { getMe, getMine, getStats, markDelivered, markOutForDelivery } from '@/api/deliveryApi';
import { getMyReturns, markReturnCollected, markReturnEnRoute } from '@/api/returnApi';
import { extractErrorMessage } from '@/api/client';
import { fromAssignment, fromReturnPickup } from '@/types/delivery';
import type { AgentMe, AgentStats, UnifiedJob } from '@/types/delivery';
import { Card } from '@/components/Card';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { useToast } from '@/components/Toast';
import { ROUTES } from '@/constants/routes';
import styles from './ProfilePage.module.css';

function initials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');
}

function statusVariant(job: UnifiedJob) {
  if (job.status === 'DELIVERED' || job.status === 'COLLECTED') return 'success' as const;
  if (job.status === 'OUT_FOR_DELIVERY' || job.status === 'EN_ROUTE') return 'info' as const;
  return 'warning' as const;
}

function statusLabel(job: UnifiedJob) {
  if (job.status === 'OUT_FOR_DELIVERY') return 'Out for delivery';
  if (job.status === 'EN_ROUTE') return 'On the way';
  return job.status.charAt(0) + job.status.slice(1).toLowerCase();
}

function detailRoute(job: UnifiedJob) {
  return job.kind === 'DELIVERY' ? ROUTES.assignmentDetail(job.id) : ROUTES.returnPickupDetail(job.id);
}

export function ProfilePage() {
  const { showToast } = useToast();
  const [me, setMe] = useState<AgentMe | null>(null);
  const [stats, setStats] = useState<AgentStats | null>(null);
  const [jobs, setJobs] = useState<UnifiedJob[]>([]);
  const [jobsLoading, setJobsLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  useEffect(() => {
    getMe().then(setMe);
    getStats().then(setStats);
  }, []);

  const reloadJobs = () => {
    setJobsLoading(true);
    Promise.all([getMine(), getMyReturns()])
      .then(([deliveries, pickups]) => {
        const merged = [...deliveries.map(fromAssignment), ...pickups.map(fromReturnPickup)];
        merged.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        setJobs(merged);
      })
      .finally(() => setJobsLoading(false));
  };

  useEffect(reloadJobs, []);

  const runAction = async (id: string, action: (id: string) => Promise<unknown>, successMessage: string) => {
    setUpdatingId(id);
    try {
      await action(id);
      showToast(successMessage);
      reloadJobs();
      getStats().then(setStats);
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not update this'), 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  if (!me || !stats) return <p className={styles.loading}>Loading…</p>;

  const totalCompleted = stats.completedDeliveries + stats.completedReturnPickups;

  return (
    <div>
      <h1>Profile</h1>

      <Card className={styles.identityCard}>
        <span className={styles.avatar}>{initials(me.name)}</span>
        <div className={styles.identity}>
          <span className={styles.name}>{me.name}</span>
          {me.phone && (
            <span className={styles.phone}>
              <Phone size={13} />
              {me.phone}
            </span>
          )}
        </div>
        <Badge variant={me.onDuty ? 'success' : 'neutral'} dot>
          {me.onDuty ? 'On duty' : 'Off duty'}
        </Badge>
      </Card>

      <div className={styles.statsGrid}>
        <Card className={styles.statCard}>
          <span className={[styles.statIcon, styles.earningsIcon].join(' ')}>
            <Wallet size={18} />
          </span>
          <span className={styles.statValue}>₹{stats.totalEarnings}</span>
          <span className={styles.statLabel}>Total earnings</span>
        </Card>

        <Card className={styles.statCard}>
          <span className={[styles.statIcon, styles.deliveriesIcon].join(' ')}>
            <Truck size={18} />
          </span>
          <span className={styles.statValue}>{stats.completedDeliveries}</span>
          <span className={styles.statLabel}>Deliveries completed</span>
        </Card>

        <Card className={styles.statCard}>
          <span className={[styles.statIcon, styles.returnsIcon].join(' ')}>
            <RotateCcw size={18} />
          </span>
          <span className={styles.statValue}>{stats.completedReturnPickups}</span>
          <span className={styles.statLabel}>Return pickups completed</span>
        </Card>

        <Card className={styles.statCard}>
          <span className={[styles.statIcon, styles.totalIcon].join(' ')}>
            <Package size={18} />
          </span>
          <span className={styles.statValue}>{totalCompleted}</span>
          <span className={styles.statLabel}>Total jobs completed</span>
        </Card>
      </div>

      <p className={styles.note}>Earnings are a flat ₹40 per completed delivery or pickup — a portfolio simulation, not a real payout engine.</p>

      <h2 className={styles.sectionTitle}>Job history</h2>
      <p className={styles.sectionSubtitle}>Everything you've claimed, most recent first.</p>

      {jobsLoading ? (
        <p className={styles.loading}>Loading…</p>
      ) : jobs.length === 0 ? (
        <Card className={styles.empty}>
          <Package size={28} className={styles.emptyIcon} />
          <p>You haven't claimed any orders yet — check Orders.</p>
        </Card>
      ) : (
        <div className={styles.jobList}>
          {jobs.map((job) => (
            <Card key={`${job.kind}-${job.id}`} className={styles.jobCard}>
              <Link to={detailRoute(job)} className={styles.jobLink}>
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
                  <ChevronRight size={14} className={styles.chevron} />
                </div>

                <div className={styles.metaRow}>
                  <Badge variant={statusVariant(job)} dot>
                    {statusLabel(job)}
                  </Badge>
                  {job.destinationCity && (
                    <span className={styles.metaItem}>
                      <MapPin size={12} />
                      {job.destinationCity}
                    </span>
                  )}
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
                      <span className={styles.metaItem}>
                        <Phone size={12} />
                        {job.recipientPhone}
                      </span>
                    )}
                  </div>
                )}

                {job.itemsSummary && (
                  <div className={styles.itemsRow}>
                    <BookOpen size={12} />
                    <span className={styles.itemsSummary}>{job.itemsSummary}</span>
                  </div>
                )}
              </Link>

              <div className={styles.jobFooter}>
                <span className={styles.payout}>₹{job.payoutAmount}</span>
                <div className={styles.actions}>
                  {job.kind === 'DELIVERY' && job.status === 'ASSIGNED' && (
                    <Button size="sm" onClick={() => runAction(job.id, markOutForDelivery, 'Marked out for delivery')} disabled={updatingId === job.id}>
                      <Truck size={14} />
                      Out for delivery
                    </Button>
                  )}
                  {job.kind === 'DELIVERY' && job.status === 'OUT_FOR_DELIVERY' && (
                    <Button size="sm" onClick={() => runAction(job.id, markDelivered, 'Marked delivered')} disabled={updatingId === job.id}>
                      <CheckCheck size={14} />
                      Delivered
                    </Button>
                  )}
                  {job.kind === 'RETURN_PICKUP' && job.status === 'ASSIGNED' && (
                    <Button size="sm" onClick={() => runAction(job.id, markReturnEnRoute, 'Marked on the way')} disabled={updatingId === job.id}>
                      <Navigation size={14} />
                      On the way
                    </Button>
                  )}
                  {job.kind === 'RETURN_PICKUP' && job.status === 'EN_ROUTE' && (
                    <Button size="sm" onClick={() => runAction(job.id, markReturnCollected, 'Marked collected')} disabled={updatingId === job.id}>
                      <PackageCheck size={14} />
                      Collected
                    </Button>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
