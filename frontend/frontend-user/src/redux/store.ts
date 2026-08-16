import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import cartReducer from './slices/cartSlice';
import notificationReducer from './slices/notificationSlice';
import storeReducer from './slices/storeSlice';
import wishlistReducer from './slices/wishlistSlice';

/** The app's single Redux store, combining every feature slice. */
export const store = configureStore({
  reducer: {
    auth: authReducer,
    cart: cartReducer,
    notifications: notificationReducer,
    store: storeReducer,
    wishlist: wishlistReducer,
  },
});

/** The shape of the whole Redux state tree, inferred from the store itself. */
export type RootState = ReturnType<typeof store.getState>;
/** The store's dispatch type, including thunk support — used to type useAppDispatch. */
export type AppDispatch = typeof store.dispatch;
