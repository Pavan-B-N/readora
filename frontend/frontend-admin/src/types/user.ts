/** The current authenticated user's profile, as returned by the /me endpoint. */
export interface MeResponse {
  userId: string;
  email: string;
  displayName: string | null;
  avatarUrl: string | null;
  phone: string | null;
  locale: string | null;
  preferredStoreId: string | null;
  /** The store this admin is assigned to manage — set only by seed data / backend, never editable here. */
  adminStoreId: string | null;
  favoriteCategoryIds: string[];
  wallet: { balance: string; currency: string };
}

/** Payload to update the current user's editable profile fields. */
export interface UpdateProfileRequest {
  displayName?: string | null;
  phone?: string | null;
}
