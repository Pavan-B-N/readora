import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { ShoppingCart, User, Package, Wallet, LogOut, BookOpen } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { loggedOut } from '@/redux/slices/authSlice';
import { cartCleared } from '@/redux/slices/cartSlice';
import { Tooltip } from '@/components/Tooltip';
import { ChatWidget } from '@/components/ChatWidget';
import { NotificationBell } from '@/components/NotificationBell';
import { StoreSwitcher } from '@/components/StoreSwitcher';
import { SearchBar } from '@/components/SearchBar';
import { ROUTES } from '@/constants/routes';
import styles from './SiteLayout.module.css';

export function SiteLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const { accessToken } = useAppSelector((state) => state.auth);
  const itemCount = useAppSelector((state) => state.cart.itemCount);

  const onLogout = () => {
    dispatch(loggedOut());
    dispatch(cartCleared());
    navigate(ROUTES.home);
  };

  const navClass = ({ isActive }: { isActive: boolean }) =>
    [styles.navLink, isActive && styles.navLinkActive].filter(Boolean).join(' ');

  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link to={ROUTES.home} className={styles.brand}>
            <span className={styles.brandMark}>
              <BookOpen size={17} />
            </span>
            <span className={styles.brandName}>Readora</span>
          </Link>

          <StoreSwitcher />

          <SearchBar />

          <nav className={styles.nav}>
            <NavLink to={ROUTES.cart} className={navClass} aria-label="Cart">
              <span className={styles.navIconWrap}>
                <ShoppingCart size={18} />
                {itemCount > 0 && <span className={styles.cartBadge}>{itemCount}</span>}
              </span>
              <span className={styles.navLabel}>Cart</span>
            </NavLink>

            {accessToken ? (
              <>
                <NotificationBell />
                <NavLink to={ROUTES.orders} className={navClass} aria-label="Orders">
                  <Package size={18} />
                  <span className={styles.navLabel}>Orders</span>
                </NavLink>
                <NavLink to={ROUTES.wallet} className={navClass} aria-label="Wallet">
                  <Wallet size={18} />
                  <span className={styles.navLabel}>Wallet</span>
                </NavLink>
                <NavLink to={ROUTES.profile} className={navClass} aria-label="Profile">
                  <User size={18} />
                  <span className={styles.navLabel}>Profile</span>
                </NavLink>
                <Tooltip label="Log out" placement="bottom">
                  <button type="button" className={styles.navLink} onClick={onLogout} aria-label="Log out">
                    <LogOut size={18} />
                  </button>
                </Tooltip>
              </>
            ) : (
              <Link to={ROUTES.login} className={[styles.navLink, styles.signInLink].join(' ')}>
                Sign in
              </Link>
            )}
          </nav>
        </div>
      </header>

      <main className={styles.main}>
        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.14, ease: 'easeOut' }}
          >
            <Outlet />
          </motion.div>
        </AnimatePresence>
      </main>

      <footer className={styles.footer}>Readora — books, physical and virtual.</footer>

      <ChatWidget />
    </div>
  );
}
