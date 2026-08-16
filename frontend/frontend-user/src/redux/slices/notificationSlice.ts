import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notificationApi';
import { extractErrorMessage } from '@/api/client';
import type { NotificationItem } from '@/types/notification';

interface NotificationState {
  items: NotificationItem[];
  unreadCount: number;
  status: 'idle' | 'loading';
  error: string | null;
}

const initialState: NotificationState = {
  items: [],
  unreadCount: 0,
  status: 'idle',
  error: null,
};

/** Loads the latest notification page and unread count together. */
export const fetchNotifications = createAsyncThunk<
  { items: NotificationItem[]; unreadCount: number },
  void,
  { rejectValue: string }
>('notifications/fetch', async (_, { rejectWithValue }) => {
  try {
    const [page, unreadCount] = await Promise.all([listNotifications(0, 20), getUnreadCount()]);
    return { items: page.content, unreadCount };
  } catch (error) {
    return rejectWithValue(extractErrorMessage(error, 'Could not load notifications'));
  }
});

/** Marks one notification read on the server and locally. */
export const markRead = createAsyncThunk('notifications/markRead', async (id: string) => {
  await markNotificationRead(id);
  return id;
});

/** Marks every notification read on the server and locally. */
export const markAllRead = createAsyncThunk('notifications/markAllRead', async () => {
  await markAllNotificationsRead();
});

const notificationSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    /** Prepends a live WebSocket-pushed notification, capping the kept list at 20 and bumping the unread count if it arrived unread. */
    notificationReceived(state, action: { payload: NotificationItem }) {
      state.items.unshift(action.payload);
      state.items = state.items.slice(0, 20);
      if (!action.payload.read) state.unreadCount += 1;
    },
    /** Resets to no notifications locally (e.g. on logout) without a server round-trip. */
    notificationsCleared(state) {
      state.items = [];
      state.unreadCount = 0;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchNotifications.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchNotifications.fulfilled, (state, action) => {
        state.status = 'idle';
        state.items = action.payload.items;
        state.unreadCount = action.payload.unreadCount;
      })
      .addCase(fetchNotifications.rejected, (state, action) => {
        state.status = 'idle';
        state.error = action.payload ?? action.error.message ?? 'Could not load notifications';
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
