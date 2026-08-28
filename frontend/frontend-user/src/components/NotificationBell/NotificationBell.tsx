import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AnimatePresence, motion } from 'framer-motion';
import { Bell, CheckCheck, Package } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { fetchNotifications, markAllRead, markRead, notificationReceived, notificationsCleared } from '@/redux/slices/notificationSlice';
import type { NotificationItem } from '@/types/notification';
import { playNotificationSound } from '@/utils/notificationSound';
import { ROUTES } from '@/constants/routes';
import styles from './NotificationBell.module.css';

const WS_URL = import.meta.env.VITE_NOTIFICATION_WS_URL ?? 'http://localhost:8086/ws';

/** How long a live popup stays on screen before auto-dismissing. */
const POPUP_LIFETIME_MS = 6000;

function timeAgo(iso: string): string {
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

export function NotificationBell() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const { items, unreadCount } = useAppSelector((state) => state.notifications);
  const [open, setOpen] = useState(false);
  const [livePopups, setLivePopups] = useState<NotificationItem[]>([]);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const clientRef = useRef<Client | null>(null);

  const dismissPopup = (id: string) => {
    setLivePopups((current) => current.filter((n) => n.id !== id));
  };

  useEffect(() => {
    if (!accessToken) {
      dispatch(notificationsCleared());
      setLivePopups([]);
      return;
    }

    dispatch(fetchNotifications());

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (message) => {
          try {
            const payload = JSON.parse(message.body) as { data: NotificationItem };
            dispatch(notificationReceived(payload.data));

            // Surfacing this only via the bell's badge count is easy to miss entirely — a live
            // popup plus a sound is what actually gets a user's attention while they're doing
            // something else on the page.
            setLivePopups((current) => [...current, payload.data]);
            window.setTimeout(() => dismissPopup(payload.data.id), POPUP_LIFETIME_MS);
            playNotificationSound();
          } catch {
            // ignore malformed frames
          }
        });
      },
    });
    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken, dispatch]);

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  if (!accessToken) return null;

  const onItemClick = (item: NotificationItem) => {
    if (!item.read) dispatch(markRead(item.id));
    if (item.orderId) {
      navigate(ROUTES.orderDetail(item.orderId));
      setOpen(false);
    }
  };

  const onPopupClick = (item: NotificationItem) => {
    dismissPopup(item.id);
    onItemClick(item);
  };

  return (
    <div className={styles.wrapper} ref={wrapperRef}>
      <button
        type="button"
        className={styles.bellButton}
        onClick={() => setOpen((o) => !o)}
        aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications'}
      >
        <Bell size={18} />
        {unreadCount > 0 && <span className={styles.badge}>{unreadCount > 9 ? '9+' : unreadCount}</span>}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            className={styles.panel}
            initial={{ opacity: 0, y: -6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.98 }}
            transition={{ duration: 0.15 }}
          >
            <div className={styles.header}>
              <span className={styles.headerTitle}>Notifications</span>
              {unreadCount > 0 && (
                <button type="button" className={styles.markAllButton} onClick={() => dispatch(markAllRead())}>
                  <CheckCheck size={13} />
                  Mark all read
                </button>
              )}
            </div>

            {items.length === 0 ? (
              <div className={styles.empty}>
                <Package size={20} />
                <span>Nothing yet — order updates will show up here.</span>
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
          </motion.div>
        )}
      </AnimatePresence>

      <div className={styles.livePopupStack}>
        <AnimatePresence>
          {livePopups.map((item) => (
            <motion.button
              type="button"
              key={item.id}
              className={styles.livePopup}
              onClick={() => onPopupClick(item)}
              initial={{ opacity: 0, y: -16, x: 24 }}
              animate={{ opacity: 1, y: 0, x: 0 }}
              exit={{ opacity: 0, x: 40, transition: { duration: 0.15 } }}
              transition={{ type: 'spring', stiffness: 400, damping: 32 }}
            >
              <span className={styles.livePopupIcon}>
                <Package size={16} />
              </span>
              <span className={styles.itemBody}>
                <span className={styles.itemTitle}>{item.title}</span>
                <span className={styles.itemMessage}>{item.message}</span>
              </span>
              <button
                type="button"
                className={styles.livePopupClose}
                onClick={(e) => {
                  e.stopPropagation();
                  dismissPopup(item.id);
                }}
                aria-label="Dismiss notification"
              >
                ×
              </button>
            </motion.button>
          ))}
        </AnimatePresence>
      </div>
    </div>
  );
}
