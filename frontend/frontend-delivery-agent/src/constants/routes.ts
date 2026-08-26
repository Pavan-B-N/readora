export const ROUTES = {
  login: '/login',
  queue: '/queue',
  mine: '/mine',
  assignmentDetail: (id: string) => `/assignments/${id}`,
} as const;
