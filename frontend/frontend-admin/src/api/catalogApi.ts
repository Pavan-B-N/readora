import { apiClient } from './client';
import type {
  AdminBookDetail,
  Author,
  BookSummary,
  CategoryNode,
  CreateAuthorRequest,
  CreateBookRequest,
  CreateCategoryRequest,
  CreatePublisherRequest,
  PageResponse,
  Publisher,
  UpdateBookRequest,
  UpdateInventoryRequest,
  UpsertVirtualEditionRequest,
} from '@/types/catalog';
import type { IdResponse } from '@/types/api';

export async function listBooks(page: number, size: number, q?: string): Promise<PageResponse<BookSummary>> {
  const response = await apiClient.get<PageResponse<BookSummary>>('/api/v1/books', {
    params: { page, size, q: q || undefined },
  });
  return response.data;
}

export async function getCategoryTree(): Promise<CategoryNode[]> {
  const response = await apiClient.get<CategoryNode[]>('/api/v1/categories');
  return response.data;
}

export async function listPublishers(): Promise<Publisher[]> {
  const response = await apiClient.get<Publisher[]>('/api/v1/publishers');
  return response.data;
}

export async function listAuthors(): Promise<Author[]> {
  const response = await apiClient.get<Author[]>('/api/v1/authors');
  return response.data;
}

export async function createCategory(request: CreateCategoryRequest): Promise<IdResponse> {
  const response = await apiClient.post<IdResponse>('/api/v1/admin/categories', request);
  return response.data;
}

export async function createPublisher(request: CreatePublisherRequest): Promise<IdResponse> {
  const response = await apiClient.post<IdResponse>('/api/v1/admin/publishers', request);
  return response.data;
}

export async function createAuthor(request: CreateAuthorRequest): Promise<IdResponse> {
  const response = await apiClient.post<IdResponse>('/api/v1/admin/authors', request);
  return response.data;
}

export async function getBookForEdit(bookId: string): Promise<AdminBookDetail> {
  const response = await apiClient.get<AdminBookDetail>(`/api/v1/admin/books/${bookId}`);
  return response.data;
}

export async function createBook(request: CreateBookRequest): Promise<IdResponse> {
  const response = await apiClient.post<IdResponse>('/api/v1/admin/books', request);
  return response.data;
}

export async function updateBook(bookId: string, request: UpdateBookRequest): Promise<void> {
  await apiClient.put(`/api/v1/admin/books/${bookId}`, request);
}

export async function updateInventory(bookId: string, request: UpdateInventoryRequest): Promise<void> {
  await apiClient.put(`/api/v1/admin/books/${bookId}/inventory`, request);
}

export async function upsertVirtualEdition(bookId: string, request: UpsertVirtualEditionRequest): Promise<void> {
  await apiClient.put(`/api/v1/admin/books/${bookId}/virtual-edition`, request);
}

export async function deactivateVirtualEdition(bookId: string): Promise<void> {
  await apiClient.delete(`/api/v1/admin/books/${bookId}/virtual-edition`);
}
