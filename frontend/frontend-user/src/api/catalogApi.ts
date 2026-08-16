import { apiClient } from './client';
import type { Author, BookDetail, BookSuggestion, BookSummary, CategoryNode, PurchasedBook, RelatedBook, Review, Store } from '@/types/catalog';
import type { PageResponse } from '@/types/api';

/** Filter/pagination options for a catalog search. */
export interface SearchParams {
  q?: string;
  categoryId?: string;
  publisherId?: string;
  authorId?: string;
  minPrice?: string;
  maxPrice?: string;
  /** Omit for the unified storefront view (physical-at-store + virtual, together). true/false restrict to one or the other. */
  virtualOnly?: boolean;
  storeId?: string;
  page?: number;
  size?: number;
}

/** Runs a paginated catalog search with the given filters. */
export async function searchBooks(params: SearchParams): Promise<PageResponse<BookSummary>> {
  const response = await apiClient.get<PageResponse<BookSummary>>('/api/v1/books', { params });
  return response.data;
}

/** Fetches full detail for one book, optionally scoped to a store's stock/pricing. */
export async function getBookDetail(bookId: string, storeId?: string): Promise<BookDetail> {
  const response = await apiClient.get<BookDetail>(`/api/v1/books/${bookId}`, { params: { storeId } });
  return response.data;
}

/** Fetches books related to the given one, for the "you may also like" rail. */
export async function getRelatedBooks(bookId: string): Promise<RelatedBook[]> {
  const response = await apiClient.get<RelatedBook[]>(`/api/v1/books/${bookId}/related`);
  return response.data;
}

/** Fetches the full category hierarchy for browsing/filtering. */
export async function getCategoryTree(): Promise<CategoryNode[]> {
  const response = await apiClient.get<CategoryNode[]>('/api/v1/categories');
  return response.data;
}

/** Lists every author, for filter dropdowns. */
export async function listAuthors(): Promise<Author[]> {
  const response = await apiClient.get<Author[]>('/api/v1/authors');
  return response.data;
}

/** Lists every physical store, for the store switcher. */
export async function listStores(): Promise<Store[]> {
  const response = await apiClient.get<Store[]>('/api/v1/stores');
  return response.data;
}

/** Fetches title-autocomplete suggestions as the user types a search query. */
export async function suggestBooks(q: string, limit = 8, storeId?: string): Promise<BookSuggestion[]> {
  const response = await apiClient.get<BookSuggestion[]>('/api/v1/books/suggest', { params: { q, limit, storeId } });
  return response.data;
}

/** Fetches the personalized/homepage recommendation rail, optionally scoped to a store. */
export async function getRecommendations(storeId?: string): Promise<BookSummary[]> {
  const response = await apiClient.get<BookSummary[]>('/api/v1/books/recommended', { params: { storeId } });
  return response.data;
}

/** "Your orders" rail — the caller's most recent order line items, each with its order's status. Empty (not an error) for anonymous callers. */
export async function getPurchasedBooks(): Promise<PurchasedBook[]> {
  const response = await apiClient.get<PurchasedBook[]>('/api/v1/books/purchased');
  return response.data;
}

/** "My library" — every virtual edition the caller owns and can open in the in-app reader. Empty (not an error) for anonymous callers. */
export async function getLibrary(): Promise<BookSummary[]> {
  const response = await apiClient.get<BookSummary[]>('/api/v1/books/library');
  return response.data;
}

/** Batch lookup by id, e.g. to render a wishlist — unscoped by store. */
export async function getBooksByIds(ids: string[]): Promise<BookSummary[]> {
  if (ids.length === 0) return [];
  const response = await apiClient.get<BookSummary[]>('/api/v1/books/batch', { params: { ids } });
  return response.data;
}

/** Streams the virtual edition's file for in-app reading — never exposed as a plain downloadable URL. */
export async function getVirtualContent(bookId: string): Promise<Blob> {
  const response = await apiClient.get(`/api/v1/books/${bookId}/read`, { responseType: 'blob' });
  return response.data;
}

/** Fetches a page of reviews for a book. */
export async function getReviews(bookId: string, page = 0, size = 20): Promise<PageResponse<Review>> {
  const response = await apiClient.get<PageResponse<Review>>(`/api/v1/books/${bookId}/reviews`, { params: { page, size } });
  return response.data;
}

/** Creates or replaces the caller's own review for a book. */
export async function upsertReview(bookId: string, rating: number, comment: string | null): Promise<Review> {
  const response = await apiClient.post<Review>(`/api/v1/books/${bookId}/reviews`, { rating, comment });
  return response.data;
}

/** Deletes the caller's own review for a book. */
export async function deleteOwnReview(bookId: string): Promise<void> {
  await apiClient.delete(`/api/v1/books/${bookId}/reviews/me`);
}
