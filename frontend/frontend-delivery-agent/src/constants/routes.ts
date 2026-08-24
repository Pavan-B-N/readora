/** Central route paths/builders for the delivery agent app, so paths aren't duplicated across router, links, and redirects. */
export const ROUTES = {
  login: '/login',
  orders: '/orders',
  profile: '/profile',
  assignmentDetail: (id: string) => `/assignments/${id}`,
  returnPickupDetail: (id: string) => `/returns/${id}`,
} as const;
