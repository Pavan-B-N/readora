import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { ListChecks, LogOut, Truck, UserCircle } from 'lucide-react';
import { getMe, setDuty } from '@/api/deliveryApi';
import { extractErrorMessage } from '@/api/client';
import { useAppDispatch } from '@/redux/hooks';
import { loggedOut } from '@/redux/slices/authSlice';
import { useToast } from '@readora/shared-ui';
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
  const { showToast } = useToast();
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
    } catch (error) {
      // e.g. trying to go off duty with an active delivery/pickup still in progress — the
      // backend is the source of truth here, this just surfaces its 409 message.
      showToast(extractErrorMessage(error, 'Could not update duty status'), 'error');
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
      <a href="#main-content" className={styles.skipLink}>
        Skip to main content
      </a>
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
            <span className={styles.navLabel}>Orders</span>
          </NavLink>
          <NavLink to={ROUTES.profile} className={({ isActive }) => [styles.navLink, isActive && styles.navLinkActive].filter(Boolean).join(' ')}>
            <UserCircle size={15} />
            <span className={styles.navLabel}>Profile</span>
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

      <main id="main-content" tabIndex={-1} className={styles.content}>
        <Outlet context={{ me, reloadMe } satisfies DeliveryLayoutContext} />
      </main>
    </div>
  );
}
