export type AssignmentStatus = 'UNASSIGNED' | 'ASSIGNED' | 'OUT_FOR_DELIVERY' | 'DELIVERED';

export interface AgentMe {
  userId: string;
  name: string;
  phone: string | null;
  storeId: string;
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
