/** Whether a cart/order line is a shipped physical copy or an instantly-available digital edition. */
export type DeliveryType = 'PHYSICAL' | 'VIRTUAL';

/** A single line in the cart. */
export interface CartItem {
  bookId: string;
  title: string;
  qty: number;
  unitPrice: string;
  lineTotal: string;
  deliveryType: DeliveryType;
}

/** Full cart contents plus derived totals. */
export interface CartResponse {
  items: CartItem[];
  subtotal: string;
  currency: string;
  itemCount: number;
  /** Whether any line is a physical item, so checkout must collect a shipping address. */
  requiresShippingAddress: boolean;
}

/** Lightweight cart totals without the full line items — e.g. for a header badge. */
export interface CartSummaryResponse {
  itemCount: number;
  subtotal: string;
  currency: string;
}

/** Body for adding (or increasing) a line in the cart. */
export interface AddCartItemRequest {
  bookId: string;
  qty: number;
  deliveryType: DeliveryType;
  /** Required only for a PHYSICAL item, to check store-local stock. */
  storeId?: string;
}
