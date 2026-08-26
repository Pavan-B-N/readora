import { apiClient } from './client';
import type { NotificationPage } from '@/types/notification';

export async function listNotifications(page: number, size: number): Promise<NotificationPage> {
  const response = await apiClient.get<NotificationPage>('/api/v1/notifications', { params: { page, size } });
  return response.data;
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
