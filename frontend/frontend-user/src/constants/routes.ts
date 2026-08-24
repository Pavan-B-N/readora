export const ROUTES = {
  home: '/',
  login: '/login',
  register: '/register',
  bookDetail: (bookId: string) => `/books/${bookId}`,
  cart: '/cart',
  checkout: '/checkout',
  orders: '/orders',
  orderDetail: (orderId: string) => `/orders/${orderId}`,
  wallet: '/wallet',
  profile: '/profile',
} as const;
