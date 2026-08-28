export const ROUTES = {
  login: '/login',
  books: '/books',
  newPhysicalBook: '/books/new/physical',
  newVirtualBook: '/books/new/virtual',
  editBook: (bookId: string) => `/books/${bookId}/edit`,
  categories: '/categories',
  publishers: '/publishers',
  authors: '/authors',
  embeddings: '/embeddings',
  returns: '/returns',
  deliveryAgents: '/delivery-agents',
  profile: '/profile',
} as const;
