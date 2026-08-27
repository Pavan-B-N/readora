export interface BookSummary {
  id: string;
  isbn13: string;
  title: string;
  authors: string[];
  publisher: string | null;
  category: string | null;
  listPrice: string;
  currency: string;
  coverImageUrl: string | null;
  availability: 'IN_STOCK' | 'OUT_OF_STOCK';
  hasVirtualEdition: boolean;
  /** Which edition this particular listing represents — distinct from hasVirtualEdition, which just says a virtual edition also exists. */
  deliveryType: 'PHYSICAL' | 'VIRTUAL';
  averageRating: number | null;
  reviewCount: number;
}

/** One "Your orders" rail entry — a book plus the status of the order it came from. */
export interface PurchasedBook {
  book: BookSummary;
  orderStatus: string;
  placedAt: string;
}

export interface BookDetail {
  id: string;
  isbn13: string;
  title: string;
  subtitle: string | null;
  description: string | null;
  authors: { id: string; name: string; bio: string | null; photoUrl: string | null }[];
  category: { id: string; name: string } | null;
  publisher: { id: string; name: string } | null;
  store: { id: string; name: string; city: string } | null;
  pageCount: number | null;
  language: string | null;
  publishedOn: string | null;
  listPrice: string;
  currency: string;
  images: string[];
  availability: {
    status: 'IN_STOCK' | 'OUT_OF_STOCK' | 'NOT_AVAILABLE_AT_STORE' | 'NO_PHYSICAL_EDITION';
    quantityAvailable: number;
  };
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
