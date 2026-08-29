import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, CheckCheck, Inbox } from 'lucide-react';
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationItem,
} from '@/api/notificationApi';
import { ROUTES } from '@/constants/routes';
import styles from './NotificationBell.module.css';

/** No live WebSocket push here (unlike the customer app's bell) — a short poll is enough for an admin console that's usually open in a background tab. */
const POLL_INTERVAL_MS = 20_000;

function timeAgo(iso: string): string {
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

export function NotificationBell() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const refresh = () => {
    getUnreadCount().then(setUnreadCount);
    listNotifications(0, 10).then(setItems);
  };

  useEffect(() => {
    refresh();
    const interval = window.setInterval(refresh, POLL_INTERVAL_MS);
    return () => window.clearInterval(interval);
  }, []);

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const onItemClick = (item: NotificationItem) => {
    if (!item.read) {
      markNotificationRead(item.id).then(() => {
        setItems((prev) => prev.map((i) => (i.id === item.id ? { ...i, read: true } : i)));
        setUnreadCount((c) => Math.max(0, c - 1));
      });
    }
    if (item.orderId) {
      navigate(ROUTES.returnDetail(item.orderId));
      setOpen(false);
    }
  };

  const onMarkAllRead = () => {
    markAllNotificationsRead().then(() => {
      setItems((prev) => prev.map((i) => ({ ...i, read: true })));
      setUnreadCount(0);
    });
  };

  return (
    <div className={styles.wrapper} ref={wrapperRef}>
      <button
        type="button"
        className={styles.bellButton}
        onClick={() => setOpen((o) => !o)}
        aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications'}
      >
        <Bell size={15} />
        Notifications
        {unreadCount > 0 && <span className={styles.badge}>{unreadCount > 9 ? '9+' : unreadCount}</span>}
      </button>

      {open && (
        <div className={styles.panel}>
          <div className={styles.header}>
            <span className={styles.headerTitle}>Notifications</span>
            {unreadCount > 0 && (
              <button type="button" className={styles.markAllButton} onClick={onMarkAllRead}>
                <CheckCheck size={13} />
                Mark all read
              </button>
            )}
          </div>

          {items.length === 0 ? (
            <div className={styles.empty}>
              <Inbox size={20} />
              <span>Nothing yet — return alerts will show up here.</span>
            </div>
          ) : (
            <div className={styles.list}>
              {items.map((item) => (
                <button
                  type="button"
                  key={item.id}
                  className={[styles.item, !item.read && styles.itemUnread].filter(Boolean).join(' ')}
                  onClick={() => onItemClick(item)}
                >
                  {!item.read && <span className={styles.unreadDot} />}
                  <span className={styles.itemBody}>
                    <span className={styles.itemTitle}>{item.title}</span>
                    <span className={styles.itemMessage}>{item.message}</span>
                    <span className={styles.itemTime}>{timeAgo(item.createdAt)}</span>
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
