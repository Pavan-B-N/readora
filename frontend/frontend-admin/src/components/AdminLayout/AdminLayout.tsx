import { NavLink, Outlet } from 'react-router-dom';
import { BookOpen, FolderTree, Building2, Users, Sparkles } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { loggedOut } from '@/redux/slices/authSlice';
import { Button } from '@/components/Button';
import { ROUTES } from '@/constants/routes';
import styles from './AdminLayout.module.css';

const NAV_ITEMS = [
  { to: ROUTES.books, label: 'Books', icon: BookOpen },
  { to: ROUTES.categories, label: 'Categories', icon: FolderTree },
  { to: ROUTES.publishers, label: 'Publishers', icon: Building2 },
  { to: ROUTES.authors, label: 'Authors', icon: Users },
  { to: ROUTES.embeddings, label: 'Embeddings', icon: Sparkles },
];

export function AdminLayout() {
  const dispatch = useAppDispatch();
  const email = useAppSelector((state) => state.auth.email);

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>Readora Admin</div>

        <nav className={styles.nav}>
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) => [styles.navLink, isActive && styles.navLinkActive].filter(Boolean).join(' ')}
            >
              <Icon size={16} />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className={styles.footer}>
          {email && <span className={styles.email}>{email}</span>}
          <Button variant="secondary" onClick={() => dispatch(loggedOut())}>
            Log out
          </Button>
        </div>
      </aside>

      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  );
}
