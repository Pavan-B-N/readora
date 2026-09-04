import { useEffect, useState } from 'react';
import { Bike, MapPin, Phone, Truck, Undo2 } from 'lucide-react';
import { listDeliveryAgents } from '@/api/deliveryApi';
import type { AdminAgent } from '@/types/delivery';
import { Card } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@readora/shared-ui';
import { Spinner } from '@readora/shared-ui';
import styles from './DeliveryAgentsPage.module.css';

/** Live-ish view, not a real push feed — a short poll is enough for a store-sized agent roster. */
const POLL_INTERVAL_MS = 5000;

function prettyStatus(status: string) {
  return status
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

function workStatusVariant(status: string) {
  if (status === 'DELIVERED' || status === 'COLLECTED') return 'success' as const;
  if (status === 'OUT_FOR_DELIVERY' || status === 'RETURN_EN_ROUTE') return 'info' as const;
  return 'warning' as const;
}

export function DeliveryAgentsPage() {
  const [agents, setAgents] = useState<AdminAgent[]>([]);
  const [loading, setLoading] = useState(true);

  const reload = () => listDeliveryAgents().then(setAgents);

  useEffect(() => {
    reload().finally(() => setLoading(false));
    const interval = window.setInterval(reload, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onDutyCount = agents.filter((a) => a.onDuty).length;

  return (
    <div>
      <PageHeader
        title="Delivery agents"
        subtitle={
          loading
            ? 'Loading…'
            : `${onDutyCount} of ${agents.length} agent${agents.length === 1 ? '' : 's'} on duty right now.`
        }
      />

      {loading ? (
        <Spinner />
      ) : agents.length === 0 ? (
        <Card>
          <EmptyState icon={Bike} title="No delivery agents yet" description="Agents assigned to your store will show up here." />
        </Card>
      ) : (
        <div className={styles.list}>
          {agents.map((agent) => (
            <Card key={agent.userId} className={styles.card}>
              <span className={[styles.dutyDot, agent.onDuty && styles.dutyDotOn].filter(Boolean).join(' ')} />

              <span className={styles.info}>
                <span className={styles.name}>{agent.name}</span>
                {agent.phone && (
                  <span className={styles.phone}>
                    <Phone size={11} />
                    {agent.phone}
                  </span>
                )}
              </span>

              <Badge variant={agent.onDuty ? 'success' : 'neutral'}>{agent.onDuty ? 'On duty' : 'Off duty'}</Badge>

              {agent.activeWork ? (
                <span className={styles.work}>
                  <span className={styles.workIcon}>
                    {agent.activeWork.type === 'DELIVERY' ? <Truck size={13} /> : <Undo2 size={13} />}
                  </span>
                  <span className={styles.workText}>
                    <span className={styles.workOrder}>{agent.activeWork.orderNumber}</span>
                    {agent.activeWork.destinationCity && (
                      <span className={styles.workCity}>
                        <MapPin size={10} />
                        {agent.activeWork.destinationCity}
                      </span>
                    )}
                  </span>
                  <Badge variant={workStatusVariant(agent.activeWork.status)} dot>
                    {prettyStatus(agent.activeWork.status)}
                  </Badge>
                </span>
              ) : (
                <span className={styles.idle}>Idle</span>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
