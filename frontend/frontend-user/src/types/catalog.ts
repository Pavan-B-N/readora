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
  averageRating: number | null;
  reviewCount: number;
}

export interface BookDetail {
  id: string;
  isbn13: string;
  title: string;
  subtitle: string | null;
  description: string | null;
  authors: { id: string; name: string; bio: string | null }[];
  category: { id: string; name: string } | null;
  publisher: { id: string; name: string } | null;
  pageCount: number | null;
  language: string | null;
  publishedOn: string | null;
  listPrice: string;
  currency: string;
  images: string[];
  availability: { status: 'IN_STOCK' | 'OUT_OF_STOCK'; quantityAvailable: number };
  estimatedDeliveryDays: number;
  virtualEdition: { price: string; currency: string } | null;
  topics: string[];
  averageRating: number | null;
  reviewCount: number;
}

export interface BookSuggestion {
  id: string;
  title: string;
  authors: string[];
  listPrice: string;
  currency: string;
  coverImageUrl: string | null;
}

export interface Review {
  id: string;
  userId: string;
  authorDisplayName: string;
  rating: number;
  comment: string | null;
  verifiedPurchase: boolean;
  createdAt: string;
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
