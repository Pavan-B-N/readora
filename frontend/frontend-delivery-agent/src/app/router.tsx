import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { DeliveryLayout } from '@/components/DeliveryLayout';
import { LoginPage } from '@/pages/LoginPage/LoginPage';
import { QueuePage } from '@/pages/QueuePage/QueuePage';
import { MyDeliveriesPage } from '@/pages/MyDeliveriesPage/MyDeliveriesPage';
import { AssignmentDetailPage } from '@/pages/AssignmentDetailPage/AssignmentDetailPage';
import { ReturnQueuePage } from '@/pages/ReturnQueuePage/ReturnQueuePage';
import { MyReturnsPage } from '@/pages/MyReturnsPage/MyReturnsPage';
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
          { path: '/', element: <Navigate to={ROUTES.queue} replace /> },
          { path: ROUTES.queue, element: <QueuePage /> },
          { path: ROUTES.mine, element: <MyDeliveriesPage /> },
          { path: '/assignments/:id', element: <AssignmentDetailPage /> },
          { path: ROUTES.returnQueue, element: <ReturnQueuePage /> },
          { path: ROUTES.returnMine, element: <MyReturnsPage /> },
          { path: '/returns/:id', element: <ReturnPickupDetailPage /> },
        ],
      },
    ],
  },
]);
