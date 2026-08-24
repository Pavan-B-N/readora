import { useState, type FormEvent } from 'react';
import { Link, Outlet, useNavigate, useSearchParams } from 'react-router-dom';
import { ShoppingCart, User, Package, Wallet } from 'lucide-react';
import { useAppSelector } from '@/redux/hooks';
import { ROUTES } from '@/constants/routes';
import styles from './SiteLayout.module.css';

export function SiteLayout() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [query, setQuery] = useState(searchParams.get('q') ?? '');
  const { accessToken } = useAppSelector((state) => state.auth);
  const itemCount = useAppSelector((state) => state.cart.itemCount);

  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    navigate(query ? `${ROUTES.home}?q=${encodeURIComponent(query)}` : ROUTES.home);
  };

  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link to={ROUTES.home} className={styles.brand}>
            Readora
          </Link>

          <form className={styles.searchForm} onSubmit={onSearch}>
            <input
              className={styles.searchInput}
              placeholder="Search books, authors…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </form>

          <nav className={styles.nav}>
            <Link to={ROUTES.cart} className={styles.navLink}>
              <ShoppingCart size={18} />
              {itemCount > 0 && <span className={styles.cartBadge}>{itemCount}</span>}
            </Link>
            {accessToken ? (
              <>
                <Link to={ROUTES.orders} className={styles.navLink}>
                  <Package size={18} />
                </Link>
                <Link to={ROUTES.wallet} className={styles.navLink}>
                  <Wallet size={18} />
                </Link>
                <Link to={ROUTES.profile} className={styles.navLink}>
                  <User size={18} />
                </Link>
              </>
            ) : (
              <Link to={ROUTES.login} className={styles.navLink}>
                Sign in
              </Link>
            )}
          </nav>
        </div>
      </header>

      <main className={styles.main}>
        <Outlet />
      </main>

      <footer className={styles.footer}>Readora — books, physical and virtual.</footer>
    </div>
  );
}
