import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPin, RotateCcw, Navigation, PackageCheck, ChevronRight } from 'lucide-react';
import { getMyReturns, markReturnCollected, markReturnEnRoute } from '@/api/returnApi';
import { extractErrorMessage } from '@/api/client';
import type { ReturnPickup, ReturnPickupStatus } from '@/types/delivery';
import { Card } from '@/components/Card';
import { Badge } from '@/components/Badge';
import { Button } from '@/components/Button';
import { useToast } from '@/components/Toast';
import { ROUTES } from '@/constants/routes';
import styles from './MyReturnsPage.module.css';

function statusVariant(status: ReturnPickupStatus) {
  if (status === 'COLLECTED') return 'success' as const;
  if (status === 'EN_ROUTE') return 'info' as const;
  return 'warning' as const;
}

function statusLabel(status: ReturnPickupStatus) {
  if (status === 'EN_ROUTE') return 'On the way';
  return status.charAt(0) + status.slice(1).toLowerCase();
}

export function MyReturnsPage() {
  const { showToast } = useToast();
  const [pickups, setPickups] = useState<ReturnPickup[]>([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const reload = () => {
    setLoading(true);
    getMyReturns()
      .then(setPickups)
      .finally(() => setLoading(false));
  };

  useEffect(reload, []);

  const runAction = async (id: string, action: (id: string) => Promise<ReturnPickup>, successMessage: string) => {
    setUpdatingId(id);
    try {
      await action(id);
      showToast(successMessage);
      reload();
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not update this pickup'), 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div>
      <h1>My return pickups</h1>
      <p className={styles.subtitle}>Everything you've claimed, most recent first.</p>

      {loading ? (
        <p className={styles.loading}>Loading…</p>
      ) : pickups.length === 0 ? (
        <Card className={styles.empty}>
          <RotateCcw size={28} className={styles.emptyIcon} />
          <p>You haven't claimed any pickups yet — check the queue.</p>
        </Card>
      ) : (
        <div className={styles.list}>
          {pickups.map((p) => (
            <Card key={p.id} className={styles.card}>
              <Link to={ROUTES.returnPickupDetail(p.id)} className={styles.info}>
                <span className={styles.orderNumber}>
                  {p.orderNumber}
                  <ChevronRight size={14} className={styles.chevron} />
                </span>
                <span className={styles.metaRow}>
                  <Badge variant={statusVariant(p.status)} dot>
                    {statusLabel(p.status)}
                  </Badge>
                  {p.destinationCity && (
                    <span className={styles.destination}>
                      <MapPin size={11} />
                      {p.destinationCity}
                    </span>
                  )}
                </span>
              </Link>

              <span className={styles.payout}>₹{p.payoutAmount}</span>

              <div className={styles.actions}>
                {p.status === 'ASSIGNED' && (
                  <Button size="sm" onClick={() => runAction(p.id, markReturnEnRoute, 'Marked on the way')} disabled={updatingId === p.id}>
                    <Navigation size={14} />
                    On the way
                  </Button>
                )}
                {p.status === 'EN_ROUTE' && (
                  <Button size="sm" onClick={() => runAction(p.id, markReturnCollected, 'Marked collected')} disabled={updatingId === p.id}>
                    <PackageCheck size={14} />
                    Collected
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
