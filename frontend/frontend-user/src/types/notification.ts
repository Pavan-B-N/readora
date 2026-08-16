/** A single notification — orderId is set when it's tied to an order update, used to deep-link the bell's item click. */
export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  orderId: string | null;
  read: boolean;
  createdAt: string;
}

/** One page of the notification history endpoint. */
export interface NotificationPage {
  content: NotificationItem[];
  totalPages: number;
}
