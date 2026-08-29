import { apiClient } from './client';

export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  orderId: string | null;
  read: boolean;
  createdAt: string;
}

interface NotificationPage {
  content: NotificationItem[];
  totalPages: number;
}

export async function listNotifications(page = 0, size = 10): Promise<NotificationItem[]> {
  const response = await apiClient.get<NotificationPage>('/api/v1/notifications', { params: { page, size } });
  return response.data.content;
}

export async function getUnreadCount(): Promise<number> {
  const response = await apiClient.get<{ unreadCount: number }>('/api/v1/notifications/unread-count');
  return response.data.unreadCount;
}

export async function markNotificationRead(id: string): Promise<void> {
  await apiClient.put(`/api/v1/notifications/${id}/read`);
}

export async function markAllNotificationsRead(): Promise<void> {
  await apiClient.put('/api/v1/notifications/read-all');
}
