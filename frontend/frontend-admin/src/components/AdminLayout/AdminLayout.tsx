import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { BookOpen, FolderTree, Building2, Users, Sparkles, Library, Store, Undo2, Bike, Menu, X } from 'lucide-react';
import { useAppSelector } from '@/redux/hooks';
import { getMe } from '@/api/userApi';
import { listStores } from '@/api/catalogApi';
import { NotificationBell } from '@/components/NotificationBell';
import { ROUTES } from '@/constants/routes';
import styles from './AdminLayout.module.css';

const NAV_GROUPS = [
  {
    label: 'Catalogue',
    items: [
      { to: ROUTES.books, label: 'Catalog', icon: BookOpen },
      { to: ROUTES.categories, label: 'Categories', icon: FolderTree },
      { to: ROUTES.publishers, label: 'Publishers', icon: Building2 },
      { to: ROUTES.authors, label: 'Authors', icon: Users },
    ],
  },
  {
    label: 'Operations',
    items: [
      { to: ROUTES.returns, label: 'Returns', icon: Undo2 },
      { to: ROUTES.deliveryAgents, label: 'Delivery agents', icon: Bike },
      { to: ROUTES.embeddings, label: 'Embeddings', icon: Sparkles },
    ],
  },
];

export function AdminLayout() {
  const location = useLocation();
  const email = useAppSelector((state) => state.auth.email);
  const initials = email ? email.slice(0, 2).toUpperCase() : '?';
  const [storeName, setStoreName] = useState<string | null>(null);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    Promise.all([getMe(), listStores()]).then(([me, stores]) => {
      const assigned = stores.find((s) => s.id === me.adminStoreId);
      setStoreName(assigned?.name ?? null);
    });
  }, []);

  useEffect(() => {
    // Close sidebar on route change for mobile
    setMobileMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    // Prevent scrolling when drawer is open
    if (mobileMenuOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [mobileMenuOpen]);

  return (
    <div className={styles.shell}>
      <header className={styles.mobileHeader}>
        <div className={styles.mobileBrand}>
          <span className={styles.brandMark}>
            <Library size={17} />
          </span>
          <span>Readora Admin</span>
        </div>
        <button
          className={styles.mobileMenuBtn}
          onClick={() => setMobileMenuOpen(true)}
          aria-label="Open menu"
        >
          <Menu size={24} />
        </button>
      </header>

      <div
        className={[styles.overlay, mobileMenuOpen && styles.overlayOpen].filter(Boolean).join(' ')}
        onClick={() => setMobileMenuOpen(false)}
        aria-hidden="true"
      />

      <aside className={[styles.sidebar, mobileMenuOpen && styles.sidebarOpen].filter(Boolean).join(' ')}>
        <div className={styles.brand}>
          <span className={styles.brandMark}>
            <Library size={17} />
          </span>
          <span className={styles.brandText}>
            <span className={styles.brandName}>Readora</span>
            <span className={styles.brandRole}>Admin</span>
          </span>
          <button
            className={styles.closeSidebarBtn}
            onClick={() => setMobileMenuOpen(false)}
            aria-label="Close menu"
          >
            <X size={20} />
          </button>
        </div>

        {storeName && (
          <div className={styles.storeBadge}>
            <Store size={13} />
            {storeName}
          </div>
        )}

        <NotificationBell />

        {NAV_GROUPS.map((group) => (
          <nav className={styles.navGroup} key={group.label}>
            <span className={styles.navGroupLabel}>{group.label}</span>
            {group.items.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  [styles.navLink, isActive && styles.navLinkActive].filter(Boolean).join(' ')
                }
              >
                <Icon size={16} />
                {label}
              </NavLink>
            ))}
          </nav>
        ))}

        <NavLink
          to={ROUTES.profile}
          className={({ isActive }) => [styles.footer, isActive && styles.footerActive].filter(Boolean).join(' ')}
        >
          <span className={styles.avatar}>{initials}</span>
          <span className={styles.identity}>
            <span className={styles.email}>{email}</span>
          </span>
        </NavLink>
      </aside>

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
    </div>
  );
}
