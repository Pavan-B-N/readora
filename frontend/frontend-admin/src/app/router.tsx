import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { AdminLayout } from '@/components/AdminLayout';
import { LoginPage } from '@/pages/LoginPage/LoginPage';
import { BooksListPage } from '@/pages/BooksListPage/BooksListPage';
import { BookFormPage } from '@/pages/BookFormPage/BookFormPage';
import { VirtualBookFormPage } from '@/pages/VirtualBookFormPage/VirtualBookFormPage';
import { BookDetailPage } from '@/pages/BookDetailPage/BookDetailPage';
import { CategoriesPage } from '@/pages/CategoriesPage/CategoriesPage';
import { PublishersPage } from '@/pages/PublishersPage/PublishersPage';
import { AuthorsPage } from '@/pages/AuthorsPage/AuthorsPage';
import { EmbeddingsPage } from '@/pages/EmbeddingsPage/EmbeddingsPage';
import { EmbeddingJobDetailPage } from '@/pages/EmbeddingJobDetailPage/EmbeddingJobDetailPage';
import { ReturnsPage } from '@/pages/ReturnsPage/ReturnsPage';
import { ReturnDetailPage } from '@/pages/ReturnDetailPage/ReturnDetailPage';
import { DeliveryAgentsPage } from '@/pages/DeliveryAgentsPage/DeliveryAgentsPage';
import { NotificationsPage } from '@/pages/NotificationsPage/NotificationsPage';
import { ProfilePage } from '@/pages/ProfilePage/ProfilePage';
import { NotFoundPage } from '@/pages/NotFoundPage/NotFoundPage';
import { ROUTES } from '@/constants/routes';

export const router = createBrowserRouter([
  { path: ROUTES.login, element: <LoginPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { path: '/', element: <Navigate to={ROUTES.books} replace /> },
          { path: ROUTES.books, element: <BooksListPage /> },
          { path: ROUTES.newPhysicalBook, element: <BookFormPage /> },
          { path: ROUTES.newVirtualBook, element: <VirtualBookFormPage /> },
          { path: '/catalog/:bookId/edit', element: <BookDetailPage /> },
          { path: ROUTES.categories, element: <CategoriesPage /> },
          { path: ROUTES.publishers, element: <PublishersPage /> },
          { path: ROUTES.authors, element: <AuthorsPage /> },
          { path: ROUTES.embeddings, element: <EmbeddingsPage /> },
          { path: '/embeddings/:jobId', element: <EmbeddingJobDetailPage /> },
          { path: ROUTES.returns, element: <ReturnsPage /> },
          { path: '/returns/:orderId', element: <ReturnDetailPage /> },
          { path: ROUTES.deliveryAgents, element: <DeliveryAgentsPage /> },
          { path: ROUTES.notifications, element: <NotificationsPage /> },
          { path: ROUTES.profile, element: <ProfilePage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]);

