export const ROUTES = {
  login: '/login',
  queue: '/queue',
  mine: '/mine',
  assignmentDetail: (id: string) => `/assignments/${id}`,
  returnQueue: '/returns/queue',
  returnMine: '/returns/mine',
  returnPickupDetail: (id: string) => `/returns/${id}`,
} as const;
