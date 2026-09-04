import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, MapPin, Navigation, PackageCheck, Phone } from 'lucide-react';
import { getReturnPickupDetail, markReturnCollected, markReturnEnRoute } from '@/api/returnApi';
import { extractErrorMessage } from '@/api/client';
import type { ReturnPickup, ReturnPickupDetail } from '@/types/delivery';
import { Card, CardHeader } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { useToast } from '@readora/shared-ui';
import { Spinner } from '@readora/shared-ui';
import styles from './ReturnPickupDetailPage.module.css';

export function ReturnPickupDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [detail, setDetail] = useState<ReturnPickupDetail | null>(null);
  const [updating, setUpdating] = useState(false);

  const reload = () => {
    if (!id) return;
    getReturnPickupDetail(id).then(setDetail);
  };

  useEffect(reload, [id]);

  const runAction = async (action: (id: string) => Promise<ReturnPickup>, successMessage: string) => {
    if (!id) return;
    setUpdating(true);
    try {
      await action(id);
      showToast(successMessage);
      reload();
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not update this pickup'), 'error');
    } finally {
      setUpdating(false);
    }
  };

  if (!detail) return <Spinner />;

  const { pickup, order } = detail;
  const address = order.shippingAddress;

  return (
    <div>
      <button type="button" className={styles.back} onClick={() => navigate(-1)}>
        <ArrowLeft size={15} />
        Back
      </button>

      <div className={styles.header}>
        <h1>{pickup.orderNumber}</h1>
        <Badge variant={pickup.status === 'COLLECTED' ? 'success' : 'info'} dot>
          {pickup.status === 'EN_ROUTE' ? 'On the way' : pickup.status}
        </Badge>
        <span className={styles.payout}>You'll earn ₹{pickup.payoutAmount}</span>
      </div>

      <div className={styles.layout}>
        <Card>
          <CardHeader title="Pickup address" />
          {address ? (
            <div className={styles.address}>
              <span className={styles.addressName}>{address.recipientName}</span>
              <span className={styles.addressLine}>
                <MapPin size={14} />
                {address.line1}
                {address.line2 ? `, ${address.line2}` : ''}, {address.city}, {address.state} {address.postalCode}
              </span>
              {address.phone && (
                <a href={`tel:${address.phone}`} className={styles.addressLine}>
                  <Phone size={14} />
                  {address.phone}
                </a>
              )}
            </div>
          ) : (
            <p className={styles.loading}>No address on file</p>
          )}
        </Card>

        <Card>
          <CardHeader title="Items" />
          <div className={styles.items}>
            {order.items.map((item, i) => (
              <div className={styles.item} key={i}>
                <span>{item.title}</span>
                <span className={styles.qty}>× {item.qty}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <div className={styles.actions}>
        {pickup.status === 'ASSIGNED' && (
          <Button onClick={() => runAction(markReturnEnRoute, 'Marked on the way')} disabled={updating}>
            <Navigation size={15} />
            Mark on the way
          </Button>
        )}
        {pickup.status === 'EN_ROUTE' && (
          <Button onClick={() => runAction(markReturnCollected, 'Marked collected')} disabled={updating}>
            <PackageCheck size={15} />
            Mark collected
          </Button>
        )}
      </div>
    </div>
  );
}
