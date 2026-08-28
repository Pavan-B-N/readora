export interface AdminOrderSummary {
  orderId: string;
  orderNumber: string;
  status: string;
  grandTotal: string;
  currency: string;
  placedAt: string;
  cancelledAt: string | null;
  cancelReason: string | null;
  refundStatus: string | null;
  refundAmount: string | null;
  refundCompletedAt: string | null;
  adminReviewedAt: string | null;
  adminNote: string | null;
}

export interface AdminOrdersPage {
  content: AdminOrderSummary[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** One message in the small admin<->customer chat that opens while a return sits at RETURN_REQUESTED. */
export interface ReturnMessage {
  id: string;
  senderUserId: string;
  senderRole: 'CUSTOMER' | 'ADMIN';
  content: string;
  createdAt: string;
}
