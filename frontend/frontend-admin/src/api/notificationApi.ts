import { apiClient } from './client';

/** A single in-app notification shown to the admin. */
export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  orderId: string | null;
  read: boolean;
  createdAt: string;
}

/** Raw paginated response shape returned by the notifications endpoint. */
interface NotificationPage {
  content: NotificationItem[];
  totalPages: number;
}

/** Fetches a page of the admin's notifications. */
export async function listNotifications(page = 0, size = 10): Promise<NotificationItem[]> {
  const response = await apiClient.get<NotificationPage>('/api/v1/notifications', { params: { page, size } });
  return response.data.content;
}

/** Fetches the count of unread notifications, used for the notification bell badge. */
export async function getUnreadCount(): Promise<number> {
  const response = await apiClient.get<{ unreadCount: number }>('/api/v1/notifications/unread-count');
  return response.data.unreadCount;
}

/** Marks a single notification as read. */
export async function markNotificationRead(id: string): Promise<void> {
  await apiClient.put(`/api/v1/notifications/${id}/read`);
}

/** Marks all of the admin's notifications as read. */
export async function markAllNotificationsRead(): Promise<void> {
  await apiClient.put('/api/v1/notifications/read-all');
}
