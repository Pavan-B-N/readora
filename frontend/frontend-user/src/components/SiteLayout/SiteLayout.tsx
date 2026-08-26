import { useState, type FormEvent } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { ShoppingCart, User, Package, Wallet, LogOut, Search, BookOpen } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { loggedOut } from '@/redux/slices/authSlice';
import { cartCleared } from '@/redux/slices/cartSlice';
import { Tooltip } from '@/components/Tooltip';
import { ChatWidget } from '@/components/ChatWidget';
import { NotificationBell } from '@/components/NotificationBell';
import { StoreSwitcher } from '@/components/StoreSwitcher';
import { ROUTES } from '@/constants/routes';
import styles from './SiteLayout.module.css';

export function SiteLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const [searchParams] = useSearchParams();
  const [query, setQuery] = useState(searchParams.get('q') ?? '');
  const { accessToken } = useAppSelector((state) => state.auth);
  const itemCount = useAppSelector((state) => state.cart.itemCount);

  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    navigate(query ? `${ROUTES.home}?q=${encodeURIComponent(query)}` : ROUTES.home);
  };

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

          <form className={styles.searchForm} onSubmit={onSearch} role="search">
            <Search size={15} className={styles.searchIcon} />
            <input
              className={styles.searchInput}
              placeholder="Search books, authors, topics…"
              aria-label="Search books"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </form>

          <nav className={styles.nav}>
            <Tooltip label="Cart" placement="bottom">
              <NavLink to={ROUTES.cart} className={navClass} aria-label="Cart">
                <ShoppingCart size={18} />
                {itemCount > 0 && <span className={styles.cartBadge}>{itemCount}</span>}
              </NavLink>
            </Tooltip>

            {accessToken ? (
              <>
                <NotificationBell />
                <Tooltip label="Orders" placement="bottom">
                  <NavLink to={ROUTES.orders} className={navClass} aria-label="Orders">
                    <Package size={18} />
                  </NavLink>
                </Tooltip>
                <Tooltip label="Wallet" placement="bottom">
                  <NavLink to={ROUTES.wallet} className={navClass} aria-label="Wallet">
                    <Wallet size={18} />
                  </NavLink>
                </Tooltip>
                <Tooltip label="Profile" placement="bottom">
                  <NavLink to={ROUTES.profile} className={navClass} aria-label="Profile">
                    <User size={18} />
                  </NavLink>
                </Tooltip>
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
