import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notificationApi';
import type { NotificationItem } from '@/types/notification';

interface NotificationState {
  items: NotificationItem[];
  unreadCount: number;
  status: 'idle' | 'loading';
}

const initialState: NotificationState = {
  items: [],
  unreadCount: 0,
  status: 'idle',
};

export const fetchNotifications = createAsyncThunk('notifications/fetch', async () => {
  const [page, unreadCount] = await Promise.all([listNotifications(0, 20), getUnreadCount()]);
  return { items: page.content, unreadCount };
});

export const markRead = createAsyncThunk('notifications/markRead', async (id: string) => {
  await markNotificationRead(id);
  return id;
});

export const markAllRead = createAsyncThunk('notifications/markAllRead', async () => {
  await markAllNotificationsRead();
});

const notificationSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    notificationReceived(state, action: { payload: NotificationItem }) {
      state.items.unshift(action.payload);
      state.items = state.items.slice(0, 20);
      if (!action.payload.read) state.unreadCount += 1;
    },
    notificationsCleared(state) {
      state.items = [];
      state.unreadCount = 0;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchNotifications.pending, (state) => {
        state.status = 'loading';
      })
      .addCase(fetchNotifications.fulfilled, (state, action) => {
        state.status = 'idle';
        state.items = action.payload.items;
        state.unreadCount = action.payload.unreadCount;
      })
      .addCase(fetchNotifications.rejected, (state) => {
        state.status = 'idle';
      })
      .addCase(markRead.fulfilled, (state, action) => {
        const item = state.items.find((n) => n.id === action.payload);
        if (item && !item.read) {
          item.read = true;
          state.unreadCount = Math.max(0, state.unreadCount - 1);
        }
      })
      .addCase(markAllRead.fulfilled, (state) => {
        state.items.forEach((n) => (n.read = true));
        state.unreadCount = 0;
      });
  },
});

export const { notificationReceived, notificationsCleared } = notificationSlice.actions;
export default notificationSlice.reducer;
