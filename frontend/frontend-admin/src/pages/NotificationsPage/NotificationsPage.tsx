import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, CheckCheck, ExternalLink, Inbox } from 'lucide-react';
import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationItem,
} from '@/api/notificationApi';
import { ROUTES } from '@/constants/routes';
import { Button } from '@readora/shared-ui';
import styles from './NotificationsPage.module.css';

function timeAgo(iso: string): string {
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

const PAGE_SIZE = 20;

export function NotificationsPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [filter, setFilter] = useState<'all' | 'unread'>('all');
  const [markingAll, setMarkingAll] = useState(false);

  const load = async (pageNum: number) => {
    setLoading(true);
    try {
      const data = await listNotifications(pageNum, PAGE_SIZE);
      if (pageNum === 0) {
        setItems(data);
      } else {
        setItems((prev) => [...prev, ...data]);
      }
      setHasMore(data.length === PAGE_SIZE);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(0);
  }, []);

  const onLoadMore = () => {
    const next = page + 1;
    setPage(next);
    load(next);
  };

  const onItemClick = (item: NotificationItem) => {
    if (!item.read) {
      markNotificationRead(item.id).then(() => {
        setItems((prev) => prev.map((i) => (i.id === item.id ? { ...i, read: true } : i)));
      });
    }
    if (item.orderId) {
      navigate(ROUTES.returnDetail(item.orderId));
    }
  };

  const onMarkAllRead = async () => {
    setMarkingAll(true);
    try {
      await markAllNotificationsRead();
      setItems((prev) => prev.map((i) => ({ ...i, read: true })));
    } finally {
      setMarkingAll(false);
    }
  };

  const displayed = filter === 'unread' ? items.filter((i) => !i.read) : items;
  const unreadCount = items.filter((i) => !i.read).length;

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.titleRow}>
          <Bell size={20} />
          <h1 className={styles.title}>Notifications</h1>
          {unreadCount > 0 && <span className={styles.badge}>{unreadCount}</span>}
        </div>
        <div className={styles.actions}>
          <div className={styles.filterTabs}>
            <button
              type="button"
              className={[styles.tab, filter === 'all' && styles.tabActive].filter(Boolean).join(' ')}
              onClick={() => setFilter('all')}
            >
              All
            </button>
            <button
              type="button"
              className={[styles.tab, filter === 'unread' && styles.tabActive].filter(Boolean).join(' ')}
              onClick={() => setFilter('unread')}
            >
              Unread {unreadCount > 0 && `(${unreadCount})`}
            </button>
          </div>
          {unreadCount > 0 && (
            <Button variant="secondary" size="sm" onClick={onMarkAllRead} disabled={markingAll}>
              <CheckCheck size={14} />
              {markingAll ? 'Marking…' : 'Mark all read'}
            </Button>
          )}
        </div>
      </div>

      <div className={styles.list}>
        {loading && items.length === 0 ? (
          <div className={styles.empty}>
            <div className={styles.skeletonList}>
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className={styles.skeletonItem}>
                  <div className="shimmer" style={{ width: 8, height: 8, borderRadius: '50%', flexShrink: 0 }} />
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
                    <div className="shimmer" style={{ height: 14, width: '40%', borderRadius: 4 }} />
                    <div className="shimmer" style={{ height: 12, width: '70%', borderRadius: 4 }} />
                    <div className="shimmer" style={{ height: 10, width: '20%', borderRadius: 4 }} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : displayed.length === 0 ? (
          <div className={styles.empty}>
            <Inbox size={36} />
            <p>{filter === 'unread' ? 'No unread notifications.' : 'No notifications yet.'}</p>
          </div>
        ) : (
          displayed.map((item) => (
            <button
              key={item.id}
              type="button"
              className={[styles.item, !item.read && styles.itemUnread].filter(Boolean).join(' ')}
              onClick={() => onItemClick(item)}
            >
              <span className={styles.dot}>{!item.read && <span className={styles.unreadDot} />}</span>
              <span className={styles.itemBody}>
                <span className={styles.itemTitle}>{item.title}</span>
                <span className={styles.itemMessage}>{item.message}</span>
                <span className={styles.itemMeta}>
                  <span className={styles.itemTime}>{timeAgo(item.createdAt)}</span>
                  {item.orderId && (
                    <span className={styles.itemLink}>
                      <ExternalLink size={11} />
                      View return
                    </span>
                  )}
                </span>
              </span>
            </button>
          ))
        )}
      </div>

      {hasMore && !loading && (
        <div className={styles.loadMore}>
          <Button variant="secondary" size="sm" onClick={onLoadMore}>
            Load more
          </Button>
        </div>
      )}
    </div>
  );
}
