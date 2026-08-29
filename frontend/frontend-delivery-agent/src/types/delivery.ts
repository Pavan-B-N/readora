export type AssignmentStatus = 'UNASSIGNED' | 'ASSIGNED' | 'OUT_FOR_DELIVERY' | 'DELIVERED';

export interface AgentMe {
  userId: string;
  name: string;
  phone: string | null;
  storeId: string;
  onDuty: boolean;
}

export interface JobItem {
  title: string;
  qty: number;
}

export interface Assignment {
  id: string;
  orderId: string;
  orderNumber: string;
  storeId: string;
  status: AssignmentStatus;
  createdAt: string;
  assignedAt: string | null;
  outForDeliveryAt: string | null;
  deliveredAt: string | null;
  destinationCity: string | null;
  recipientName: string | null;
  recipientPhone: string | null;
  items: JobItem[];
  payoutAmount: string;
}

export interface OrderDeliveryDetail {
  orderId: string;
  orderNumber: string;
  status: string;
  storeId: string | null;
  shippingAddress: {
    recipientName: string;
    line1: string;
    line2: string | null;
    city: string;
    state: string;
    postalCode: string;
    countryCode: string;
    phone: string | null;
  } | null;
  items: JobItem[];
  placedAt: string;
}

export interface AssignmentDetail {
  assignment: Assignment;
  order: OrderDeliveryDetail;
}

export type ReturnPickupStatus = 'UNASSIGNED' | 'ASSIGNED' | 'EN_ROUTE' | 'COLLECTED';

export interface ReturnPickup {
  id: string;
  orderId: string;
  orderNumber: string;
  storeId: string;
  status: ReturnPickupStatus;
  createdAt: string;
  assignedAt: string | null;
  enRouteAt: string | null;
  collectedAt: string | null;
  destinationCity: string | null;
  recipientName: string | null;
  recipientPhone: string | null;
  items: JobItem[];
  payoutAmount: string;
}

export interface ReturnPickupDetail {
  pickup: ReturnPickup;
  order: OrderDeliveryDetail;
}

export interface AgentStats {
  completedDeliveries: number;
  completedReturnPickups: number;
  totalEarnings: string;
  currency: string;
}

/** Client-side merge discriminator — the backend keeps deliveries and return pickups as two
 * separate resources; the UI presents them as one unified "Orders" / "Profile history" list. */
export type JobKind = 'DELIVERY' | 'RETURN_PICKUP';

export interface UnifiedJob {
  kind: JobKind;
  id: string;
  orderId: string;
  orderNumber: string;
  storeId: string;
  status: AssignmentStatus | ReturnPickupStatus;
  createdAt: string;
  destinationCity: string | null;
  recipientName: string | null;
  recipientPhone: string | null;
  items: JobItem[];
  payoutAmount: string;
}

/** Non-terminal statuses — claimed but not yet finished. Used to split Orders into Active vs Available. */
export function isActiveStatus(status: AssignmentStatus | ReturnPickupStatus): boolean {
  return status === 'ASSIGNED' || status === 'OUT_FOR_DELIVERY' || status === 'EN_ROUTE';
}

export function fromAssignment(a: Assignment): UnifiedJob {
  return {
    kind: 'DELIVERY',
    id: a.id,
    orderId: a.orderId,
    orderNumber: a.orderNumber,
    storeId: a.storeId,
    status: a.status,
    createdAt: a.createdAt,
    destinationCity: a.destinationCity,
    recipientName: a.recipientName,
    recipientPhone: a.recipientPhone,
    items: a.items,
    payoutAmount: a.payoutAmount,
  };
}

export function fromReturnPickup(p: ReturnPickup): UnifiedJob {
  return {
    kind: 'RETURN_PICKUP',
    id: p.id,
    orderId: p.orderId,
    orderNumber: p.orderNumber,
    storeId: p.storeId,
    status: p.status,
    createdAt: p.createdAt,
    destinationCity: p.destinationCity,
    recipientName: p.recipientName,
    recipientPhone: p.recipientPhone,
    items: p.items,
    payoutAmount: p.payoutAmount,
  };
}
