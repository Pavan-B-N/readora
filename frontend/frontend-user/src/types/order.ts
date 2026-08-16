/** Whether an order line is a shipped physical copy or an instantly-available digital edition. */
export type DeliveryType = 'PHYSICAL' | 'VIRTUAL';
/** Which of the two demo payment rails checkout used. */
export type PaymentMethod = 'WALLET' | 'UPI';

/** Address submitted with a checkout request — a flattened, request-shaped subset of the saved Address type. */
export interface ShippingAddressInput {
  recipientName: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  postalCode: string;
  countryCode: string;
  phone?: string;
}

/** Body posted to place an order from the current cart. */
export interface CheckoutRequest {
  shippingAddress: ShippingAddressInput | null;
  paymentMethod: PaymentMethod;
  upiId?: string;
  items: { bookId: string; qty: number; deliveryType: DeliveryType }[];
}

/** Confirmation returned right after placing an order. */
export interface CheckoutResponse {
  orderId: string;
  orderNumber: string;
  status: string;
  deliveryType: DeliveryType;
  subtotal: string;
  shippingFee: string;
  packagingFee: string;
  taxAmount: string;
  grandTotal: string;
  walletAmountUsed: string;
  paymentMethod: string;
  currency: string;
  placedAt: string;
}

/** Capped preview of an order's line items, for the order list's cover collage — see itemCount on OrderSummary for the true total. */
export interface OrderItemPreview {
  bookId: string;
  title: string;
  coverImageUrl: string | null;
}

/** Row shape for the orders list page. */
export interface OrderSummary {
  orderId: string;
  orderNumber: string;
  status: string;
  grandTotal: string;
  currency: string;
  placedAt: string;
  cancellable: boolean;
  deliveredAt: string | null;
  itemPreviews: OrderItemPreview[];
  itemCount: number;
}

/** transactionId is the payment provider's own reference — there's no real external gateway behind this dummy provider. */
export interface OrderPaymentInfo {
  transactionId: string;
  status: string;
  amount: string;
  walletAmountUsed: string;
  authorizedAt: string | null;
  capturedAt: string | null;
}

/** Full order record shown on the order detail page — items, shipping, payment, status history, and return tracking. */
export interface OrderDetail {
  orderId: string;
  orderNumber: string;
  status: string;
  deliveryType: DeliveryType;
  items: {
    bookId: string;
    title: string;
    isbn13: string | null;
    qty: number;
    unitPrice: string;
    lineTotal: string;
    deliveryType: DeliveryType;
  }[];
  shippingAddress: { recipientName: string; line1: string; city: string; postalCode: string; countryCode: string } | null;
  history: { toStatus: string; at: string }[];
  cancellable: boolean;
  returnable: boolean;
  subtotal: string;
  shippingFee: string;
  packagingFee: string;
  taxAmount: string;
  grandTotal: string;
  walletAmountUsed: string;
  paymentMethod: string;
  currency: string;
  placedAt: string;
  deliveryAgentName: string | null;
  deliveredAt: string | null;
  /** null if payment-service hasn't recorded a payment for this order yet (e.g. right after checkout), or was unreachable. */
  payment: OrderPaymentInfo | null;
  /** Set once a return reaches RETURN_ASSIGNED or later — the agent collecting the return, distinct from deliveryAgentName above. */
  returnAgentName: string | null;
}

/** Confirmation returned after cancelling an order. */
export interface CancelOrderResponse {
  orderId: string;
  status: string;
  cancelledAt: string;
}

/** Confirmation returned after starting a return. */
export interface ReturnOrderResponse {
  orderId: string;
  status: string;
  returnedAt: string;
}

/** One message in the small admin<->customer chat that opens while a return sits at RETURN_REQUESTED. */
export interface ReturnMessage {
  id: string;
  senderUserId: string;
  senderRole: 'CUSTOMER' | 'ADMIN';
  content: string;
  createdAt: string;
}
