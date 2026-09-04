import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { listStores } from '@/api/catalogApi';
import { getMe, updateProfile } from '@/api/userApi';
import { extractErrorMessage } from '@/api/client';
import type { Store } from '@/types/catalog';
import { pickDefaultStore } from '@/utils/store';
import type { RootState } from '../store';

interface StoreState {
  stores: Store[];
  selectedId: string | null;
  resolved: boolean;
  switching: boolean;
  error: string | null;
}

const initialState: StoreState = {
  stores: [],
  selectedId: null,
  resolved: false,
  switching: false,
  error: null,
};

// Resolves "the store we're delivering from" — the signed-in caller's preferred store, or the
// default store (see pickDefaultStore) for anonymous callers or ones with no preference set yet.
// Every physical-book search requires this id, so callers (StoreSwitcher, HomePage) must wait for
// `resolved` before searching the physical tab.
export const initStore = createAsyncThunk<
  { stores: Store[]; selectedId: string | null },
  void,
  { state: RootState; rejectValue: string }
>('store/init', async (_, { getState, rejectWithValue }) => {
  try {
    const stores = await listStores();
    const { accessToken } = getState().auth;
    if (!accessToken) {
      return { stores, selectedId: pickDefaultStore(stores)?.id ?? null };
    }
    const me = await getMe();
    return { stores, selectedId: me.preferredStoreId ?? pickDefaultStore(stores)?.id ?? null };
  } catch (error) {
    return rejectWithValue(extractErrorMessage(error, 'Could not load stores'));
  }
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
        state.error = null;
      })
      .addCase(initStore.fulfilled, (state, action) => {
        state.stores = action.payload.stores;
        state.selectedId = action.payload.selectedId;
        state.resolved = true;
      })
      .addCase(initStore.rejected, (state, action) => {
        state.resolved = true;
        state.error = action.payload ?? action.error.message ?? 'Could not load stores';
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
