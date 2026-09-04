import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { addToWishlist, listWishlist, removeFromWishlist } from '@/api/userApi';
import { extractErrorMessage } from '@/api/client';

interface WishlistState {
  ids: Record<string, boolean>;
  resolved: boolean;
  error: string | null;
}

const initialState: WishlistState = {
  ids: {},
  resolved: false,
  error: null,
};

export const fetchWishlist = createAsyncThunk<Awaited<ReturnType<typeof listWishlist>>, void, { rejectValue: string }>(
  'wishlist/fetch',
  async (_, { rejectWithValue }) => {
    try {
      return await listWishlist();
    } catch (error) {
      return rejectWithValue(extractErrorMessage(error, 'Could not load your wishlist'));
    }
  },
);

export const addWishlistItem = createAsyncThunk('wishlist/add', async (bookId: string) => {
  await addToWishlist(bookId);
  return bookId;
});

export const removeWishlistItem = createAsyncThunk('wishlist/remove', async (bookId: string) => {
  await removeFromWishlist(bookId);
  return bookId;
});

const wishlistSlice = createSlice({
  name: 'wishlist',
  initialState,
  reducers: {
    wishlistCleared(state) {
      state.ids = {};
      state.resolved = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchWishlist.fulfilled, (state, action) => {
        state.ids = Object.fromEntries(action.payload.map((item) => [item.bookId, true]));
        state.resolved = true;
        state.error = null;
      })
      .addCase(fetchWishlist.rejected, (state, action) => {
        state.resolved = true;
        state.error = action.payload ?? action.error.message ?? 'Could not load your wishlist';
      })
      .addCase(addWishlistItem.fulfilled, (state, action) => {
        state.ids[action.payload] = true;
      })
      .addCase(removeWishlistItem.fulfilled, (state, action) => {
        delete state.ids[action.payload];
      });
  },
});

export const { wishlistCleared } = wishlistSlice.actions;
export default wishlistSlice.reducer;
