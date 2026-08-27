import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { listStores } from '@/api/catalogApi';
import { getMe, updateProfile } from '@/api/userApi';
import type { Store } from '@/types/catalog';
import type { RootState } from '../store';

interface StoreState {
  stores: Store[];
  selectedId: string | null;
  resolved: boolean;
  switching: boolean;
}

const initialState: StoreState = {
  stores: [],
  selectedId: null,
  resolved: false,
  switching: false,
};

// Resolves "the store we're delivering from" — the signed-in caller's preferred store, or the
// first active store for anonymous callers. Every physical-book search requires this id, so
// callers (StoreSwitcher, HomePage) must wait for `resolved` before searching the physical tab.
export const initStore = createAsyncThunk<
  { stores: Store[]; selectedId: string | null },
  void,
  { state: RootState }
>('store/init', async (_, { getState }) => {
  const stores = await listStores();
  const { accessToken } = getState().auth;
  if (!accessToken) {
    return { stores, selectedId: stores[0]?.id ?? null };
  }
  const me = await getMe();
  return { stores, selectedId: me.preferredStoreId ?? stores[0]?.id ?? null };
});

export const switchStore = createAsyncThunk<string, string, { state: RootState }>(
  'store/switch',
  async (storeId, { getState }) => {
    const { accessToken } = getState().auth;
    if (accessToken) {
      await updateProfile({ preferredStoreId: storeId });
    }
    return storeId;
  },
);

const storeSlice = createSlice({
  name: 'store',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(initStore.pending, (state) => {
        state.resolved = false;
      })
      .addCase(initStore.fulfilled, (state, action) => {
        state.stores = action.payload.stores;
        state.selectedId = action.payload.selectedId;
        state.resolved = true;
      })
      .addCase(initStore.rejected, (state) => {
        state.resolved = true;
      })
      .addCase(switchStore.pending, (state) => {
        state.switching = true;
      })
      .addCase(switchStore.fulfilled, (state, action) => {
        state.selectedId = action.payload;
        state.switching = false;
      })
      .addCase(switchStore.rejected, (state) => {
        state.switching = false;
      });
  },
});

export default storeSlice.reducer;
