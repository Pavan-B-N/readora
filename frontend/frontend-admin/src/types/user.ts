export interface MeResponse {
  userId: string;
  email: string;
  displayName: string | null;
  avatarUrl: string | null;
  phone: string | null;
  locale: string | null;
  preferredStoreId: string | null;
  favoriteCategoryIds: string[];
  wallet: { balance: string; currency: string };
}

export interface UpdateProfileRequest {
  displayName?: string | null;
  phone?: string | null;
  preferredStoreId?: string | null;
}
