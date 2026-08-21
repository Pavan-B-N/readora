/** Supported file formats for a book's virtual (digital) edition. */
export type VirtualFileFormat = 'PDF' | 'EPUB';

/** A customer review left on a book. */
export interface Review {
  id: string;
  userId: string;
  authorDisplayName: string;
  rating: number;
  comment: string | null;
  verifiedPurchase: boolean;
  createdAt: string;
}

/** A category in the hierarchical category tree, with its nested subcategories. */
export interface CategoryNode {
  id: string;
  name: string;
  slug: string;
  displayOrder: number;
  children: CategoryNode[];
}

/** A physical store location that can be assigned as a book's stock source. */
export interface Store {
  id: string;
  name: string;
  city: string;
  line1: string;
  line2: string | null;
  state: string;
  postalCode: string;
  countryCode: string;
}

/** A book publisher. */
export interface Publisher {
  id: string;
  name: string;
  slug: string;
}

/** A book author. */
export interface Author {
  id: string;
  name: string;
  slug: string;
  bio: string | null;
  photoUrl: string | null;
}

/** Condensed book fields used in list/table views. */
export interface BookSummary {
  id: string;
  isbn13: string;
  title: string;
  authors: string[];
  publisher: string | null;
  listPrice: string;
  currency: string;
  coverImageUrl: string | null;
  availability: 'IN_STOCK' | 'OUT_OF_STOCK';
}

/** Generic paginated response envelope for list endpoints. */
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Payload to create a new category. */
export interface CreateCategoryRequest {
  name: string;
  slug: string;
  displayOrder: number;
}

/** Payload to update a category; same shape as creation. */
export type UpdateCategoryRequest = CreateCategoryRequest;

/** Payload to create a new publisher. */
export interface CreatePublisherRequest {
  name: string;
  slug: string;
}

/** Payload to create a new author. */
export interface CreateAuthorRequest {
  name: string;
  slug: string;
  bio: string | null;
  photoUrl: string | null;
}

/** Payload to update an existing author. */
export interface UpdateAuthorRequest {
  name: string;
  slug: string;
  bio: string | null;
  photoUrl: string | null;
}

/** Payload to create a new book. */
export interface CreateBookRequest {
  isbn13: string;
  title: string;
  subtitle: string | null;
  description: string | null;
  tableOfContents: string | null;
  categoryId: string | null;
  publisherId: string | null;
  storeId: string | null;
  authorIds: string[];
  language: string | null;
  pageCount: number | null;
  publishedOn: string | null;
  listPrice: string;
  currency: string;
  coverImageUrl: string | null;
}

/** Payload to update an existing book's metadata. */
export interface UpdateBookRequest {
  title: string;
  subtitle: string | null;
  description: string | null;
  tableOfContents: string | null;
  categoryId: string | null;
  publisherId: string | null;
  authorIds: string[] | null;
  language: string | null;
  pageCount: number | null;
  publishedOn: string | null;
  listPrice: string;
  currency: string;
  coverImageUrl: string | null;
  isActive: boolean;
}

/** Payload to update a book's stock levels. */
export interface UpdateInventoryRequest {
  qtyOnHand: number;
  reorderThreshold: number;
}

/** Payload to create or replace a book's virtual (digital) edition. */
export interface UpsertVirtualEditionRequest {
  fileUrl: string;
  fileFormat: VirtualFileFormat;
  fileSizeBytes: number | null;
  price: string;
  currency: string;
}

/** Full book record as returned to the admin, including inventory and virtual edition. */
export interface AdminBookDetail {
  id: string;
  isbn13: string;
  title: string;
  subtitle: string | null;
  description: string | null;
  tableOfContents: string | null;
  categoryId: string | null;
  publisherId: string | null;
  storeId: string | null;
  authorIds: string[];
  language: string | null;
  pageCount: number | null;
  publishedOn: string | null;
  listPrice: string;
  currency: string;
  coverImageUrl: string | null;
  isActive: boolean;
  createdByUserId: string | null;
  createdAt: string;
  embeddedAt: string | null;
  needsReembedding: boolean;
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
