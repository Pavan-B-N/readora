import { createBrowserRouter } from 'react-router-dom';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { SiteLayout } from '@/components/SiteLayout';
import { LoginPage } from '@/pages/LoginPage/LoginPage';
import { RegisterPage } from '@/pages/RegisterPage/RegisterPage';
import { HomePage } from '@/pages/HomePage/HomePage';
import { BookDetailPage } from '@/pages/BookDetailPage/BookDetailPage';
import { CartPage } from '@/pages/CartPage/CartPage';
import { WishlistPage } from '@/pages/WishlistPage/WishlistPage';
import { CheckoutPage } from '@/pages/CheckoutPage/CheckoutPage';
import { OrdersPage } from '@/pages/OrdersPage/OrdersPage';
import { OrderDetailPage } from '@/pages/OrderDetailPage/OrderDetailPage';
import { WalletPage } from '@/pages/WalletPage/WalletPage';
import { ProfilePage } from '@/pages/ProfilePage/ProfilePage';
import { VirtualReaderPage } from '@/pages/VirtualReaderPage/VirtualReaderPage';
import { AssistantPage } from '@/pages/AssistantPage/AssistantPage';
import { ROUTES } from '@/constants/routes';

export const router = createBrowserRouter([
  {
    element: <SiteLayout />,
    children: [
      { path: ROUTES.home, element: <HomePage /> },
      { path: '/books/:bookId', element: <BookDetailPage /> },
      { path: ROUTES.login, element: <LoginPage /> },
      { path: ROUTES.register, element: <RegisterPage /> },
      {
        element: <ProtectedRoute />,
        children: [
          { path: ROUTES.cart, element: <CartPage /> },
          { path: ROUTES.wishlist, element: <WishlistPage /> },
          { path: ROUTES.checkout, element: <CheckoutPage /> },
          { path: ROUTES.orders, element: <OrdersPage /> },
          { path: '/orders/:orderId', element: <OrderDetailPage /> },
          { path: ROUTES.wallet, element: <WalletPage /> },
          { path: ROUTES.profile, element: <ProfilePage /> },
          { path: '/read/:bookId', element: <VirtualReaderPage /> },
          { path: '/assistant/:conversationId?', element: <AssistantPage /> },
        ],
      },
    ],
  },
]);
