import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { ListChecks, LogOut, Package, Truck } from 'lucide-react';
import { getMe } from '@/api/deliveryApi';
import { useAppDispatch } from '@/redux/hooks';
import { loggedOut } from '@/redux/slices/authSlice';
import { ROUTES } from '@/constants/routes';
import type { AgentMe } from '@/types/delivery';
import styles from './DeliveryLayout.module.css';

export function DeliveryLayout() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [me, setMe] = useState<AgentMe | null>(null);

  useEffect(() => {
    getMe().then(setMe);
  }, []);

  const onLogout = () => {
    dispatch(loggedOut());
    navigate(ROUTES.login, { replace: true });
  };

  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <div className={styles.brand}>
          <span className={styles.brandMark}>
            <Truck size={17} />
          </span>
          <span className={styles.brandText}>
            <span className={styles.brandName}>Readora</span>
            <span className={styles.brandRole}>Delivery</span>
          </span>
        </div>

        <nav className={styles.nav}>
          <NavLink to={ROUTES.queue} className={({ isActive }) => [styles.navLink, isActive && styles.navLinkActive].filter(Boolean).join(' ')}>
            <ListChecks size={15} />
            Queue
          </NavLink>
          <NavLink to={ROUTES.mine} className={({ isActive }) => [styles.navLink, isActive && styles.navLinkActive].filter(Boolean).join(' ')}>
            <Package size={15} />
            My deliveries
          </NavLink>
        </nav>

        <div className={styles.agent}>
          {me && <span className={styles.agentName}>{me.name}</span>}
          <button type="button" className={styles.logoutButton} onClick={onLogout} aria-label="Log out">
            <LogOut size={15} />
          </button>
        </div>
      </header>

      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  );
}
