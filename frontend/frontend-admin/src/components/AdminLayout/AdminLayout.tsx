import { NavLink, Outlet } from 'react-router-dom';
import { BookOpen, FolderTree, Building2, Users, Sparkles, LogOut, Library } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { loggedOut } from '@/redux/slices/authSlice';
import { Tooltip } from '@/components/Tooltip';
import { ROUTES } from '@/constants/routes';
import styles from './AdminLayout.module.css';

const NAV_GROUPS = [
  {
    label: 'Catalogue',
    items: [
      { to: ROUTES.books, label: 'Books', icon: BookOpen },
      { to: ROUTES.categories, label: 'Categories', icon: FolderTree },
      { to: ROUTES.publishers, label: 'Publishers', icon: Building2 },
      { to: ROUTES.authors, label: 'Authors', icon: Users },
    ],
  },
  {
    label: 'Operations',
    items: [{ to: ROUTES.embeddings, label: 'Embeddings', icon: Sparkles }],
  },
];

export function AdminLayout() {
  const dispatch = useAppDispatch();
  const email = useAppSelector((state) => state.auth.email);
  const initials = email ? email.slice(0, 2).toUpperCase() : '?';

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

        <div className={styles.footer}>
          <span className={styles.avatar}>{initials}</span>
          <span className={styles.identity}>
            <span className={styles.email}>{email}</span>
          </span>
          <Tooltip label="Log out" placement="top">
            <button
              type="button"
              className={styles.navLink}
              onClick={() => dispatch(loggedOut())}
              aria-label="Log out"
            >
              <LogOut size={16} />
            </button>
          </Tooltip>
        </div>
      </aside>

      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  );
}
