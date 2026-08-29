import { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { getUnreadCount } from '@/api/notificationApi';
import { ROUTES } from '@/constants/routes';
import styles from './NotificationBell.module.css';

const POLL_INTERVAL_MS = 20_000;

export function NotificationBell({ navLinkClass }: { navLinkClass: (args: { isActive: boolean }) => string }) {
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    getUnreadCount().then(setUnreadCount);
    const interval = window.setInterval(() => getUnreadCount().then(setUnreadCount), POLL_INTERVAL_MS);
    return () => window.clearInterval(interval);
  }, []);

  return (
    <NavLink
      to={ROUTES.notifications}
      className={navLinkClass}
      aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications'}
    >
      <Bell size={16} />
      Notifications
      {unreadCount > 0 && <span className={styles.badge}>{unreadCount > 9 ? '9+' : unreadCount}</span>}
    </NavLink>
  );
}
