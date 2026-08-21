import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';

/** The app's single Redux store — currently just the auth slice. */
export const store = configureStore({
  reducer: {
    auth: authReducer,
  },
});

/** The shape of the whole Redux state tree, inferred from the store. */
export type RootState = ReturnType<typeof store.getState>;
/** The store's dispatch function type, including thunk support. */
export type AppDispatch = typeof store.dispatch;
