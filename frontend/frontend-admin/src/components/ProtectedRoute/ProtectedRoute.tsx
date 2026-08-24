import { Navigate, Outlet } from 'react-router-dom';
import { useAppSelector } from '@/redux/hooks';
import { ROUTES } from '@/constants/routes';

const ADMIN_ROLE = 'ADMIN';

/**
 * Gates every admin route on a logged-in ADMIN. This is a UX convenience only — the backend's
 * UserContextFilter is the real enforcement, this just avoids flashing admin UI at the wrong user.
 */
export function ProtectedRoute() {
  const { accessToken, roles } = useAppSelector((state) => state.auth);

  if (!accessToken) {
    return <Navigate to={ROUTES.login} replace />;
  }

  if (!roles.includes(ADMIN_ROLE)) {
    return <Navigate to={ROUTES.login} replace />;
  }

  return <Outlet />;
}
