import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAppSelector } from '@/redux/hooks';
import { ROUTES } from '@/constants/routes';

const DELIVERY_AGENT_ROLE = 'DELIVERY_AGENT';

/**
 * Gates every delivery route on a logged-in DELIVERY_AGENT. UX convenience only — the backend's
 * UserContextFilter is the real enforcement, this just avoids flashing the app at the wrong user.
 */
export function ProtectedRoute() {
  const { accessToken, roles } = useAppSelector((state) => state.auth);
  const location = useLocation();

  if (!accessToken) {
    return <Navigate to={ROUTES.login} state={{ from: location }} replace />;
  }

  if (!roles.includes(DELIVERY_AGENT_ROLE)) {
    return <Navigate to={ROUTES.login} replace />;
  }

  return <Outlet />;
}
