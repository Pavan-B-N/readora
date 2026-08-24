export type BookFormat = 'HARDCOVER' | 'PAPERBACK' | 'EBOOK';
export type VirtualFileFormat = 'PDF' | 'EPUB';

export interface CategoryNode {
  id: string;
  name: string;
  slug: string;
  children: CategoryNode[];
}

export interface Publisher {
  id: string;
  name: string;
  slug: string;
}

export interface Author {
  id: string;
  name: string;
  slug: string;
  bio: string | null;
}

export interface BookSummary {
  id: string;
  isbn13: string;
  title: string;
  authors: string[];
  publisher: string | null;
  format: BookFormat;
  listPrice: string;
  currency: string;
  coverImageUrl: string | null;
  availability: 'IN_STOCK' | 'OUT_OF_STOCK';
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateCategoryRequest {
  name: string;
  slug: string;
  parentId: string | null;
  displayOrder: number;
}

export interface CreatePublisherRequest {
  name: string;
  slug: string;
}

export interface CreateAuthorRequest {
  name: string;
  slug: string;
  bio: string | null;
}

export interface CreateBookRequest {
  isbn13: string;
  title: string;
  subtitle: string | null;
  description: string | null;
  tableOfContents: string | null;
  categoryId: string | null;
  publisherId: string | null;
  authorIds: string[];
  language: string | null;
  format: BookFormat;
  pageCount: number | null;
  publishedOn: string | null;
  listPrice: string;
  currency: string;
  coverImageUrl: string | null;
}

export interface UpdateBookRequest {
  title: string;
  subtitle: string | null;
  description: string | null;
  tableOfContents: string | null;
  categoryId: string | null;
  publisherId: string | null;
  authorIds: string[] | null;
  language: string | null;
  format: BookFormat;
  pageCount: number | null;
  publishedOn: string | null;
  listPrice: string;
  currency: string;
  coverImageUrl: string | null;
  isActive: boolean;
}

export interface UpdateInventoryRequest {
  qtyOnHand: number;
  reorderThreshold: number;
}

export interface UpsertVirtualEditionRequest {
  fileUrl: string;
  fileFormat: VirtualFileFormat;
  fileSizeBytes: number | null;
  price: string;
  currency: string;
}

export interface AdminBookDetail {
  id: string;
  isbn13: string;
  title: string;
  subtitle: string | null;
  description: string | null;
  tableOfContents: string | null;
  categoryId: string | null;
  publisherId: string | null;
  authorIds: string[];
  language: string | null;
  format: BookFormat;
  pageCount: number | null;
  publishedOn: string | null;
  listPrice: string;
  currency: string;
  coverImageUrl: string | null;
  isActive: boolean;
  inventory: {
    qtyOnHand: number;
    qtyReserved: number;
    reorderThreshold: number;
  } | null;
  virtualEdition: {
    fileUrl: string;
    fileFormat: VirtualFileFormat;
    fileSizeBytes: number | null;
    price: string;
    currency: string;
    isActive: boolean;
  } | null;
}
