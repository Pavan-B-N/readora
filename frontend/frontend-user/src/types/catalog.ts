export type BookFormat = 'HARDCOVER' | 'PAPERBACK' | 'EBOOK';

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

export interface BookDetail {
  id: string;
  isbn13: string;
  title: string;
  subtitle: string | null;
  description: string | null;
  authors: { id: string; name: string }[];
  category: { id: string; name: string } | null;
  publisher: { id: string; name: string } | null;
  format: BookFormat;
  pageCount: number | null;
  language: string | null;
  publishedOn: string | null;
  listPrice: string;
  currency: string;
  images: string[];
  availability: { status: 'IN_STOCK' | 'OUT_OF_STOCK'; quantityAvailable: number };
  estimatedDeliveryDays: number;
}

export interface RelatedBook {
  id: string;
  title: string;
  listPrice: string;
  coverImageUrl: string | null;
}

export interface CategoryNode {
  id: string;
  name: string;
  slug: string;
  children: CategoryNode[];
}
