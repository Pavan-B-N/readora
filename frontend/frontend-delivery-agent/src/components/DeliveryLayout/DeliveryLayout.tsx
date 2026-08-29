import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { ListChecks, LogOut, Truck, UserCircle } from 'lucide-react';
import { getMe, setDuty } from '@/api/deliveryApi';
import { useAppDispatch } from '@/redux/hooks';
import { loggedOut } from '@/redux/slices/authSlice';
import { ROUTES } from '@/constants/routes';
import type { AgentMe } from '@/types/delivery';
import styles from './DeliveryLayout.module.css';

/** Shared with child route pages via <Outlet context> — lets the queue pages show "you're offline" instead of a generic empty state. */
export interface DeliveryLayoutContext {
  me: AgentMe | null;
  reloadMe: () => void;
}

export function DeliveryLayout() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [me, setMe] = useState<AgentMe | null>(null);
  const [togglingDuty, setTogglingDuty] = useState(false);

  const reloadMe = () => {
    getMe().then(setMe);
  };

  useEffect(reloadMe, []);

  const onToggleDuty = async () => {
    if (!me) return;
    setTogglingDuty(true);
    try {
      const updated = await setDuty(!me.onDuty);
      setMe(updated);
    } finally {
      setTogglingDuty(false);
    }
  };

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
          <NavLink to={ROUTES.orders} className={({ isActive }) => [styles.navLink, isActive && styles.navLinkActive].filter(Boolean).join(' ')}>
            <ListChecks size={15} />
            Orders
          </NavLink>
          <NavLink to={ROUTES.profile} className={({ isActive }) => [styles.navLink, isActive && styles.navLinkActive].filter(Boolean).join(' ')}>
            <UserCircle size={15} />
            Profile
          </NavLink>
        </nav>

        <div className={styles.agent}>
          {me && (
            <button
              type="button"
              className={[styles.dutyToggle, me.onDuty && styles.dutyToggleOn].filter(Boolean).join(' ')}
              onClick={onToggleDuty}
              disabled={togglingDuty}
              aria-pressed={me.onDuty}
            >
              <span className={styles.dutyToggleDot} />
              {me.onDuty ? 'On duty' : 'Off duty'}
            </button>
          )}
          {me && <span className={styles.agentName}>{me.name}</span>}
          <button type="button" className={styles.logoutButton} onClick={onLogout} aria-label="Log out">
            <LogOut size={15} />
          </button>
        </div>
      </header>

      <main className={styles.content}>
        <Outlet context={{ me, reloadMe } satisfies DeliveryLayoutContext} />
      </main>
    </div>
  );
}
