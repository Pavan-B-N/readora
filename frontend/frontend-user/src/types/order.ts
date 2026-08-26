export type DeliveryType = 'PHYSICAL' | 'VIRTUAL';
export type PaymentMethod = 'WALLET' | 'UPI';

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
  shippingAddress: ShippingAddressInput | null;
  paymentMethod: PaymentMethod;
  upiId?: string;
  items: { bookId: string; qty: number; deliveryType: DeliveryType }[];
}

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
}

export interface CancelOrderResponse {
  orderId: string;
  status: string;
  cancelledAt: string;
}

export interface ReturnOrderResponse {
  orderId: string;
  status: string;
  returnedAt: string;
}
