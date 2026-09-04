import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAppSelector } from '@/redux/hooks';
import { ROUTES } from '@/constants/routes';

const ADMIN_ROLE = 'ADMIN';

export function ProtectedRoute() {
  const { accessToken, roles } = useAppSelector((state) => state.auth);
  const location = useLocation();

  if (!accessToken) {
    return <Navigate to={ROUTES.login} state={{ from: location }} replace />;
  }

  if (!roles.includes(ADMIN_ROLE)) {
    return <Navigate to={ROUTES.login} replace />;
  }

  return <Outlet />;
}
