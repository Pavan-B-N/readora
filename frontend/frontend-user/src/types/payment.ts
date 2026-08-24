export interface PaymentResponse {
  paymentId: string;
  orderId: string;
  status: 'AUTHORIZED' | 'CAPTURED' | 'FAILED' | 'REFUNDED' | string;
  method: string;
  amount: string;
  walletAmountUsed: string;
  authorizedAt: string | null;
  capturedAt: string | null;
  refund: { status: string; amount: string } | null;
}
