export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  orderId: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationPage {
  content: NotificationItem[];
  totalPages: number;
}
