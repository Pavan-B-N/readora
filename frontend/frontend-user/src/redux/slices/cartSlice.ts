import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { addItem, getCart, setItemQty } from '@/api/cartApi';
import type { AddCartItemRequest, CartResponse, DeliveryType } from '@/types/cart';

interface CartState {
  items: CartResponse['items'];
  subtotal: string;
  currency: string;
  itemCount: number;
  requiresShippingAddress: boolean;
  status: 'idle' | 'loading';
}

const initialState: CartState = {
  items: [],
  subtotal: '0',
  currency: 'USD',
  itemCount: 0,
  requiresShippingAddress: false,
  status: 'idle',
};

export const fetchCart = createAsyncThunk('cart/fetch', async () => getCart());

export const addToCart = createAsyncThunk('cart/addItem', async (request: AddCartItemRequest) => {
  await addItem(request);
  return getCart();
});

export const updateCartItemQty = createAsyncThunk(
  'cart/setQty',
  async ({ bookId, deliveryType, qty }: { bookId: string; deliveryType: DeliveryType; qty: number }) => {
    await setItemQty(bookId, deliveryType, qty);
    return getCart();
  },
);

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
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
      })
      .addMatcher(
        (action): action is ReturnType<typeof fetchCart.fulfilled> =>
          [fetchCart.fulfilled.type, addToCart.fulfilled.type, updateCartItemQty.fulfilled.type].includes(action.type),
        (state, action) => {
          state.status = 'idle';
          state.items = action.payload.items;
          state.subtotal = action.payload.subtotal;
          state.currency = action.payload.currency;
          state.itemCount = action.payload.itemCount;
          state.requiresShippingAddress = action.payload.requiresShippingAddress;
        },
      );
  },
});

export const { cartCleared } = cartSlice.actions;
export default cartSlice.reducer;
