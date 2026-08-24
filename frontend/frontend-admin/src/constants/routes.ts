export const ROUTES = {
  login: '/login',
  books: '/books',
  newBook: '/books/new',
  editBook: (bookId: string) => `/books/${bookId}/edit`,
  categories: '/categories',
  publishers: '/publishers',
  authors: '/authors',
  embeddings: '/embeddings',
} as const;
