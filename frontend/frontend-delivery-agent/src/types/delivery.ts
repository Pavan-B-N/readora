export type AssignmentStatus = 'UNASSIGNED' | 'ASSIGNED' | 'OUT_FOR_DELIVERY' | 'DELIVERED';

export interface AgentMe {
  userId: string;
  name: string;
  phone: string | null;
  storeId: string;
  onDuty: boolean;
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
  items: { title: string; qty: number }[];
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
  payoutAmount: string;
}

export interface ReturnPickupDetail {
  pickup: ReturnPickup;
  order: OrderDeliveryDetail;
}
