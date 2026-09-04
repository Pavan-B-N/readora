import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCheck, MapPin, Phone, Truck } from 'lucide-react';
import { getAssignmentDetail, markDelivered, markOutForDelivery } from '@/api/deliveryApi';
import { extractErrorMessage } from '@/api/client';
import type { Assignment, AssignmentDetail } from '@/types/delivery';
import { Card, CardHeader } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { useToast } from '@readora/shared-ui';
import { Spinner } from '@readora/shared-ui';
import styles from './AssignmentDetailPage.module.css';

export function AssignmentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [detail, setDetail] = useState<AssignmentDetail | null>(null);
  const [updating, setUpdating] = useState(false);

  const reload = () => {
    if (!id) return;
    getAssignmentDetail(id).then(setDetail);
  };

  useEffect(reload, [id]);

  const runAction = async (action: (id: string) => Promise<Assignment>, successMessage: string) => {
    if (!id) return;
    setUpdating(true);
    try {
      await action(id);
      showToast(successMessage);
      reload();
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not update this order'), 'error');
    } finally {
      setUpdating(false);
    }
  };

  if (!detail) return <Spinner />;

  const { assignment, order } = detail;
  const address = order.shippingAddress;

  return (
    <div>
      <button type="button" className={styles.back} onClick={() => navigate(-1)}>
        <ArrowLeft size={15} />
        Back
      </button>

      <div className={styles.header}>
        <h1>{assignment.orderNumber}</h1>
        <Badge variant={assignment.status === 'DELIVERED' ? 'success' : 'info'} dot>
          {assignment.status === 'OUT_FOR_DELIVERY' ? 'Out for delivery' : assignment.status}
        </Badge>
        <span className={styles.payout}>You'll earn ₹{assignment.payoutAmount}</span>
      </div>

      <div className={styles.layout}>
        <Card>
          <CardHeader title="Delivery address" />
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
        {assignment.status === 'ASSIGNED' && (
          <Button onClick={() => runAction(markOutForDelivery, 'Marked out for delivery')} disabled={updating}>
            <Truck size={15} />
            Mark out for delivery
          </Button>
        )}
        {assignment.status === 'OUT_FOR_DELIVERY' && (
          <Button onClick={() => runAction(markDelivered, 'Marked delivered')} disabled={updating}>
            <CheckCheck size={15} />
            Mark delivered
          </Button>
        )}
      </div>
    </div>
  );
}
