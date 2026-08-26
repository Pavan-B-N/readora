import { apiClient } from './client';
import type { BookDetail, BookSummary, CategoryNode, RelatedBook, Store } from '@/types/catalog';
import type { PageResponse } from '@/types/api';

export interface SearchParams {
  q?: string;
  categoryId?: string;
  publisherId?: string;
  format?: string;
  minPrice?: string;
  maxPrice?: string;
  virtualOnly?: boolean;
  page?: number;
  size?: number;
}

export async function searchBooks(params: SearchParams): Promise<PageResponse<BookSummary>> {
  const response = await apiClient.get<PageResponse<BookSummary>>('/api/v1/books', { params });
  return response.data;
}

export async function getBookDetail(bookId: string): Promise<BookDetail> {
  const response = await apiClient.get<BookDetail>(`/api/v1/books/${bookId}`);
  return response.data;
}

export async function getRelatedBooks(bookId: string): Promise<RelatedBook[]> {
  const response = await apiClient.get<RelatedBook[]>(`/api/v1/books/${bookId}/related`);
  return response.data;
}

export async function getCategoryTree(): Promise<CategoryNode[]> {
  const response = await apiClient.get<CategoryNode[]>('/api/v1/categories');
  return response.data;
}

export async function listStores(): Promise<Store[]> {
  const response = await apiClient.get<Store[]>('/api/v1/stores');
  return response.data;
}

export async function getRecommendations(): Promise<BookSummary[]> {
  const response = await apiClient.get<BookSummary[]>('/api/v1/books/recommended');
  return response.data;
}

/** Streams the virtual edition's file for in-app reading — never exposed as a plain downloadable URL. */
export async function getVirtualContent(bookId: string): Promise<Blob> {
  const response = await apiClient.get(`/api/v1/books/${bookId}/read`, { responseType: 'blob' });
  return response.data;
}
