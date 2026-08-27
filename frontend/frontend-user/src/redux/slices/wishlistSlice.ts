import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { addToWishlist, listWishlist, removeFromWishlist } from '@/api/userApi';

interface WishlistState {
  ids: Record<string, boolean>;
  resolved: boolean;
}

const initialState: WishlistState = {
  ids: {},
  resolved: false,
};

export const fetchWishlist = createAsyncThunk('wishlist/fetch', async () => listWishlist());

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
      })
      .addCase(fetchWishlist.rejected, (state) => {
        state.resolved = true;
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
