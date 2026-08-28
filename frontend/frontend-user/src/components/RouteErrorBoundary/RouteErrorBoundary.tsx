import { isRouteErrorResponse, useNavigate, useRouteError } from 'react-router-dom';
import { AlertTriangle } from 'lucide-react';
import { NotFoundPage } from '@/pages/NotFoundPage/NotFoundPage';
import { Button } from '@/components/Button';
import { ROUTES } from '@/constants/routes';
import styles from './RouteErrorBoundary.module.css';

/**
 * The router's top-level errorElement. Router-level errors fall into two shapes:
 * - isRouteErrorResponse (thrown Response, e.g. a loader's 404) — reuse the same NotFoundPage
 *   copy, since to the user it's indistinguishable from an unmatched URL.
 * - anything else (a render exception, a thrown Error) — a real bug, shown separately so it's
 *   not miscategorized as "page doesn't exist".
 */
export function RouteErrorBoundary() {
  const error = useRouteError();
  const navigate = useNavigate();

  if (isRouteErrorResponse(error) && error.status === 404) {
    return <NotFoundPage />;
  }

  return (
    <div className={styles.wrap}>
      <span className={styles.icon}>
        <AlertTriangle size={28} />
      </span>
      <h1 className={styles.title}>Something went wrong</h1>
      <p className={styles.description}>
        An unexpected error occurred while loading this page. Try reloading, or head back to home.
      </p>
      <div className={styles.actions}>
        <Button variant="secondary" onClick={() => window.location.reload()}>
          Reload
        </Button>
        <Button onClick={() => navigate(ROUTES.home)}>Back to home</Button>
      </div>
    </div>
  );
}
