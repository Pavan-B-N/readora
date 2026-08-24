import { apiClient } from './client';
import type { BookDetail, BookSummary, CategoryNode, RelatedBook } from '@/types/catalog';
import type { PageResponse } from '@/types/api';

export interface SearchParams {
  q?: string;
  categoryId?: string;
  publisherId?: string;
  format?: string;
  minPrice?: string;
  maxPrice?: string;
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
