export type DeliveryType = 'PHYSICAL' | 'VIRTUAL';

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

export interface CheckoutRequest {
  deliveryType: DeliveryType;
  shippingAddress: ShippingAddressInput | null;
  paymentMethod: string;
  items: { bookId: string; qty: number }[];
}

export interface CheckoutResponse {
  orderId: string;
  orderNumber: string;
  status: string;
  deliveryType: DeliveryType;
  subtotal: string;
  shippingFee: string;
  taxAmount: string;
  grandTotal: string;
  currency: string;
  placedAt: string;
}

export interface OrderSummary {
  orderId: string;
  orderNumber: string;
  status: string;
  grandTotal: string;
  currency: string;
  placedAt: string;
  cancellable: boolean;
}

export interface OrderDetail {
  orderId: string;
  orderNumber: string;
  status: string;
  deliveryType: DeliveryType;
  items: { bookId: string; title: string; isbn13: string; qty: number; unitPrice: string; lineTotal: string }[];
  shippingAddress: { recipientName: string; line1: string; city: string; postalCode: string; countryCode: string } | null;
  history: { toStatus: string; at: string }[];
  cancellable: boolean;
  grandTotal: string;
  currency: string;
  placedAt: string;
}

export interface CancelOrderResponse {
  orderId: string;
  status: string;
  cancelledAt: string;
}
