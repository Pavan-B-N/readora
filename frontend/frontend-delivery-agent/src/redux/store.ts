import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
  },
});

/** The store's full state shape, derived from the reducers rather than hand-maintained. */
export type RootState = ReturnType<typeof store.getState>;
/** The store's dispatch type, including thunk support. */
export type AppDispatch = typeof store.dispatch;
