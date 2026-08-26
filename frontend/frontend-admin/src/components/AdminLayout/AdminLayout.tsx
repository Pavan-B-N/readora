import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { BookOpen, FolderTree, Building2, Users, Sparkles, Library, Store, Undo2 } from 'lucide-react';
import { useAppSelector } from '@/redux/hooks';
import { getMe } from '@/api/userApi';
import { listStores } from '@/api/catalogApi';
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
      { to: ROUTES.embeddings, label: 'Embeddings', icon: Sparkles },
    ],
  },
];

export function AdminLayout() {
  const location = useLocation();
  const email = useAppSelector((state) => state.auth.email);
  const initials = email ? email.slice(0, 2).toUpperCase() : '?';
  const [storeName, setStoreName] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([getMe(), listStores()]).then(([me, stores]) => {
      const assigned = stores.find((s) => s.id === me.adminStoreId);
      setStoreName(assigned?.name ?? null);
    });
  }, []);

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.brandMark}>
            <Library size={17} />
          </span>
          <span className={styles.brandText}>
            <span className={styles.brandName}>Readora</span>
            <span className={styles.brandRole}>Admin</span>
          </span>
        </div>

        {storeName && (
          <div className={styles.storeBadge}>
            <Store size={13} />
            {storeName}
          </div>
        )}

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
