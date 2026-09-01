import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { ShoppingCart, User, Package, Wallet, LogOut, BookOpen, Heart, Library } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { loggedOut } from '@/redux/slices/authSlice';
import { cartCleared } from '@/redux/slices/cartSlice';
import { initStore } from '@/redux/slices/storeSlice';
import { fetchWishlist, wishlistCleared } from '@/redux/slices/wishlistSlice';
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
  const wishlistCount = useAppSelector((state) => Object.keys(state.wishlist.ids).length);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMenuOpen(false);
    };
    document.addEventListener('mousedown', onClickOutside);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('mousedown', onClickOutside);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, []);

  // AssistantPage owns switching between conversations itself (via the conversationId route
  // param, without remounting — see its loadConversation guard). Keying the transition on the
  // full pathname would fight that: minting a new conversation navigates from /assistant to
  // /assistant/:id, which — same page, just a param change — would otherwise still be treated as
  // a brand new page by AnimatePresence, unmounting AssistantPage and wiping whatever it had in
  // memory (including any book carousel just attached to a reply) in favor of a fresh instance
  // that re-fetches bare history from the server. Collapsing both to one key sidesteps that.
  const transitionKey = location.pathname.startsWith('/assistant') ? '/assistant' : location.pathname;

  // Auth pages own their whole screen — no site chrome competing with the sign-in/sign-up flow.
  const isAuthPage = location.pathname === ROUTES.login || location.pathname === ROUTES.register;

  // The reader already has its own dedicated reading-assistant panel — the general shopping
  // assistant bubble floating on top of it is redundant and, at the bottom-right, overlaps the
  // reader's own chat composer. It also wants the full viewport width for the PDF + panel split,
  // not the site-wide reading-column max-width every other page shares.
  const isReaderPage = location.pathname.startsWith('/read/');

  // The store we're "delivering from" is resolved once here (preferred store for signed-in
  // callers, first active store otherwise) and shared via Redux — StoreSwitcher and HomePage
  // both read it rather than each independently calling listStores()/getMe().
  useEffect(() => {
    dispatch(initStore());
  }, [accessToken, dispatch]);

  useEffect(() => {
    if (accessToken) dispatch(fetchWishlist());
  }, [accessToken, dispatch]);

  const onLogout = () => {
    dispatch(loggedOut());
    dispatch(cartCleared());
    dispatch(wishlistCleared());
    navigate(ROUTES.home);
  };

  const navClass = ({ isActive }: { isActive: boolean }) =>
    [styles.navLink, isActive && styles.navLinkActive].filter(Boolean).join(' ');

  return (
    <div className={styles.shell}>
      <a href="#main-content" className={styles.skipLink}>
        Skip to main content
      </a>
      {!isAuthPage && (
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
              {accessToken ? (
                <>
                  <NavLink to={ROUTES.cart} className={navClass} aria-label="Cart">
                    <span className={styles.navIconWrap}>
                      <ShoppingCart size={18} />
                      {itemCount > 0 && <span className={styles.cartBadge}>{itemCount}</span>}
                    </span>
                    <span className={styles.navLabel}>Cart</span>
                  </NavLink>
                  <NotificationBell />
                  <NavLink to={ROUTES.wishlist} className={navClass} aria-label="Wishlist">
                    <span className={styles.navIconWrap}>
                      <Heart size={18} />
                      {wishlistCount > 0 && <span className={styles.cartBadge}>{wishlistCount}</span>}
                    </span>
                    <span className={styles.navLabel}>Wishlist</span>
                  </NavLink>
                  <div className={styles.profileMenuWrap} ref={menuRef}>
                    <button
                      type="button"
                      className={[styles.navLink, menuOpen && styles.profileMenuActive].filter(Boolean).join(' ')}
                      onClick={() => setMenuOpen(!menuOpen)}
                      aria-label="Profile menu"
                      aria-haspopup="menu"
                      aria-expanded={menuOpen}
                    >
                      <User size={18} />
                      <span className={styles.navLabel}>Profile</span>
                    </button>
                    <AnimatePresence>
                      {menuOpen && (
                        <motion.div
                          className={styles.profileMenu}
                          role="menu"
                          aria-label="Profile"
                          initial={{ opacity: 0, y: 4, scale: 0.95 }}
                          animate={{ opacity: 1, y: 0, scale: 1 }}
                          exit={{ opacity: 0, scale: 0.95, transition: { duration: 0.1 } }}
                          transition={{ type: 'spring', bounce: 0, duration: 0.2 }}
                        >
                          <Link to={ROUTES.profile} className={styles.menuItem} role="menuitem" onClick={() => setMenuOpen(false)}>
                            <User size={15} />
                            Your Profile
                          </Link>
                          <Link to={ROUTES.orders} className={styles.menuItem} role="menuitem" onClick={() => setMenuOpen(false)}>
                            <Package size={15} />
                            Orders
                          </Link>
                          <Link to={ROUTES.library} className={styles.menuItem} role="menuitem" onClick={() => setMenuOpen(false)}>
                            <Library size={15} />
                            Library
                          </Link>
                          <Link to={ROUTES.wallet} className={styles.menuItem} role="menuitem" onClick={() => setMenuOpen(false)}>
                            <Wallet size={15} />
                            Wallet
                          </Link>
                          <div className={styles.menuDivider} role="separator" />
                          <button type="button" className={styles.menuItem} role="menuitem" onClick={() => { setMenuOpen(false); onLogout(); }}>
                            <LogOut size={15} />
                            Log out
                          </button>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </>
              ) : (
                <Link to={ROUTES.login} className={[styles.navLink, styles.signInLink].join(' ')}>
                  Sign in
                </Link>
              )}
            </nav>
          </div>
        </header>
      )}

      <main
        id="main-content"
        tabIndex={-1}
        className={[styles.main, isAuthPage && styles.mainFullBleed]
          .filter(Boolean)
          .join(' ')}
      >
        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={transitionKey}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.14, ease: 'easeOut' }}
          >
            <Outlet />
          </motion.div>
        </AnimatePresence>
      </main>

      {!isAuthPage && !isReaderPage && <ChatWidget />}
    </div>
  );
}
