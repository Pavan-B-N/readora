import { useNavigate } from 'react-router-dom';
import { Compass, Home } from 'lucide-react';
import { Button } from '@readora/shared-ui';
import { ROUTES } from '@/constants/routes';
import styles from './NotFoundPage.module.css';

interface NotFoundPageProps {
  title?: string;
  description?: string;
}

/** The catalogue-wide "nothing matched this URL" page — also reused by RouteErrorBoundary for a thrown 404 response. */
export function NotFoundPage({
  title = "This page doesn't exist",
  description = "The link may be broken, or the page may have moved. Let's get you back to somewhere real.",
}: NotFoundPageProps) {
  const navigate = useNavigate();

  return (
    <div className={styles.wrap}>
      <span className={styles.icon}>
        <Compass size={28} />
      </span>
      <span className={styles.code}>404</span>
      <h1 className={styles.title}>{title}</h1>
      <p className={styles.description}>{description}</p>
      <div className={styles.actions}>
        <Button variant="secondary" onClick={() => navigate(-1)}>
          Go back
        </Button>
        <Button onClick={() => navigate(ROUTES.home)}>
          <Home size={15} />
          Back to home
        </Button>
      </div>
    </div>
  );
}
