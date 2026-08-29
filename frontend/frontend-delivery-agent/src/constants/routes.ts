export const ROUTES = {
  login: '/login',
  orders: '/orders',
  profile: '/profile',
  assignmentDetail: (id: string) => `/assignments/${id}`,
  returnPickupDetail: (id: string) => `/returns/${id}`,
} as const;
