import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { DeliveryLayout } from '@/components/DeliveryLayout';
import { LoginPage } from '@/pages/LoginPage/LoginPage';
import { OrdersPage } from '@/pages/OrdersPage/OrdersPage';
import { ProfilePage } from '@/pages/ProfilePage/ProfilePage';
import { AssignmentDetailPage } from '@/pages/AssignmentDetailPage/AssignmentDetailPage';
import { ReturnPickupDetailPage } from '@/pages/ReturnPickupDetailPage/ReturnPickupDetailPage';
import { ROUTES } from '@/constants/routes';

export const router = createBrowserRouter([
  { path: ROUTES.login, element: <LoginPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <DeliveryLayout />,
        children: [
          { path: '/', element: <Navigate to={ROUTES.orders} replace /> },
          { path: ROUTES.orders, element: <OrdersPage /> },
          { path: ROUTES.profile, element: <ProfilePage /> },
          { path: '/assignments/:id', element: <AssignmentDetailPage /> },
          { path: '/returns/:id', element: <ReturnPickupDetailPage /> },
          // Catches stale bookmarks/tabs pointed at removed routes (e.g. the old /queue,
          // /returns/queue, /returns/mine, from before Orders/My Jobs were merged) — sends them
          // to Orders instead of react-router's default "Unexpected Application Error" page.
          // ProtectedRoute itself still redirects to /login first if the caller isn't authenticated.
          { path: '*', element: <Navigate to={ROUTES.orders} replace /> },
        ],
      },
    ],
  },
]);
