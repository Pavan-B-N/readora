import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { addItem, getCart, setItemQty } from '@/api/cartApi';
import { extractErrorMessage } from '@/api/client';
import type { AddCartItemRequest, CartResponse, DeliveryType } from '@/types/cart';

interface CartState {
  items: CartResponse['items'];
  subtotal: string;
  currency: string;
  itemCount: number;
  requiresShippingAddress: boolean;
  status: 'idle' | 'loading';
  error: string | null;
}

const initialState: CartState = {
  items: [],
  subtotal: '0',
  currency: 'USD',
  itemCount: 0,
  requiresShippingAddress: false,
  status: 'idle',
  error: null,
};

/** Loads the caller's server-side cart into state. */
export const fetchCart = createAsyncThunk<CartResponse, void, { rejectValue: string }>(
  'cart/fetch',
  async (_, { rejectWithValue }) => {
    try {
      return await getCart();
    } catch (error) {
      return rejectWithValue(extractErrorMessage(error, 'Could not load your cart'));
    }
  },
);

/** Adds a line to the cart, then refetches the full cart so totals stay in sync. */
export const addToCart = createAsyncThunk<CartResponse, AddCartItemRequest, { rejectValue: string }>(
  'cart/addItem',
  async (request, { rejectWithValue }) => {
    try {
      await addItem(request);
      return await getCart();
    } catch (error) {
      return rejectWithValue(extractErrorMessage(error, 'Could not add item to cart'));
    }
  },
);

/** Sets a line's quantity (0 removes it), then refetches the full cart so totals stay in sync. */
export const updateCartItemQty = createAsyncThunk<
  CartResponse,
  { bookId: string; deliveryType: DeliveryType; qty: number },
  { rejectValue: string }
>('cart/setQty', async ({ bookId, deliveryType, qty }, { rejectWithValue }) => {
  try {
    await setItemQty(bookId, deliveryType, qty);
    return await getCart();
  } catch (error) {
    return rejectWithValue(extractErrorMessage(error, 'Could not update cart'));
  }
});

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    /** Resets to an empty cart locally (e.g. on logout or after checkout) without a server round-trip. */
    cartCleared(state) {
      state.items = [];
      state.subtotal = '0';
      state.itemCount = 0;
      state.requiresShippingAddress = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCart.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addMatcher(
        (action): action is ReturnType<typeof fetchCart.fulfilled> =>
          [fetchCart.fulfilled.type, addToCart.fulfilled.type, updateCartItemQty.fulfilled.type].includes(action.type),
        (state, action) => {
          state.status = 'idle';
          state.error = null;
          state.items = action.payload.items;
          state.subtotal = action.payload.subtotal;
          state.currency = action.payload.currency;
          state.itemCount = action.payload.itemCount;
          state.requiresShippingAddress = action.payload.requiresShippingAddress;
        },
      )
      .addMatcher(
        (action): action is { type: string; payload?: string; error: { message?: string } } =>
          [fetchCart.rejected.type, addToCart.rejected.type, updateCartItemQty.rejected.type].includes(action.type),
        (state, action) => {
          state.status = 'idle';
          state.error = action.payload ?? action.error.message ?? 'Something went wrong with your cart';
        },
      );
  },
});

export const { cartCleared } = cartSlice.actions;
export default cartSlice.reducer;
