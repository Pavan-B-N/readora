import { useEffect, useState } from 'react';
import { RouterProvider } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from '@/redux/store';
import { bootstrapSession } from '@/api/client';
import { fetchCart } from '@/redux/slices/cartSlice';
import { ToastProvider } from '@readora/shared-ui';
import { router } from './router';

function AppRoutes() {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    bootstrapSession()
      .then(() => {
        // The cart badge in the header otherwise only rehydrates once the user visits a page
        // that happens to dispatch fetchCart itself — a plain refresh would show 0 items even
        // though the server-side cart (Redis, 30-day TTL) is untouched.
        if (store.getState().auth.accessToken) store.dispatch(fetchCart());
      })
      .finally(() => setReady(true));
  }, []);

  if (!ready) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
        <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>
      </div>
    );
  }

  return <RouterProvider router={router} />;
}

export function App() {
  return (
    <Provider store={store}>
      <ToastProvider>
        <AppRoutes />
      </ToastProvider>
    </Provider>
  );
}
