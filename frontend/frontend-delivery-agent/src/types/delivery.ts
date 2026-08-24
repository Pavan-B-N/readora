/** Lifecycle states for a delivery assignment, from unclaimed through delivered. */
export type AssignmentStatus = 'UNASSIGNED' | 'ASSIGNED' | 'OUT_FOR_DELIVERY' | 'DELIVERED';

/** The logged-in delivery agent's own profile, including on-duty status. */
export interface AgentMe {
  userId: string;
  name: string;
  phone: string | null;
  storeId: string;
  onDuty: boolean;
}

/** A line item (title + quantity) within an order shown to the delivery agent. */
export interface JobItem {
  title: string;
  qty: number;
}

/** A delivery job — claimed or unclaimed — assigned to (or available for) an agent. */
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

/** Order and shipping-address detail backing an assignment or return pickup's detail view. */
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

/** Full detail for a single delivery assignment: the assignment plus its order info. */
export interface AssignmentDetail {
  assignment: Assignment;
  order: OrderDeliveryDetail;
}

/** Lifecycle states for a return pickup, from unclaimed through collected. */
export type ReturnPickupStatus = 'UNASSIGNED' | 'ASSIGNED' | 'EN_ROUTE' | 'COLLECTED';

/** A return pickup job — claimed or unclaimed — assigned to (or available for) an agent. */
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

/** Full detail for a single return pickup: the pickup plus its order info. */
export interface ReturnPickupDetail {
  pickup: ReturnPickup;
  order: OrderDeliveryDetail;
}

/** Aggregate performance stats (completed jobs, earnings) for the logged-in agent. */
export interface AgentStats {
  completedDeliveries: number;
  completedReturnPickups: number;
  totalEarnings: string;
  currency: string;
}

/** Client-side merge discriminator — the backend keeps deliveries and return pickups as two separate resources; the UI presents them as one unified "Orders" / "Profile history" list. */
export type JobKind = 'DELIVERY' | 'RETURN_PICKUP';

/** A delivery assignment or return pickup normalized into one shape for the merged Orders/Profile lists. */
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

/** Converts an Assignment into the common UnifiedJob shape for delivery jobs. */
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

/** Converts a ReturnPickup into the common UnifiedJob shape for return jobs. */
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
