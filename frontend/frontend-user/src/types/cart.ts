export interface CartItem {
  bookId: string;
  title: string;
  qty: number;
  unitPrice: string;
  lineTotal: string;
}

export interface CartResponse {
  items: CartItem[];
  subtotal: string;
  currency: string;
  itemCount: number;
}

export interface CartSummaryResponse {
  itemCount: number;
  subtotal: string;
  currency: string;
}

export interface AddCartItemRequest {
  bookId: string;
  qty: number;
}
