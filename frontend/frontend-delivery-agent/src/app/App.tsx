import { useEffect, useState } from 'react';
import { RouterProvider } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from '@/redux/store';
import { bootstrapSession } from '@/api/client';
import { ToastProvider } from '@readora/shared-ui';
import { Spinner } from '@readora/shared-ui';
import { router } from './router';

/** Waits for a stored refresh token to bootstrap the session before rendering the router, so protected routes don't flash a login redirect first. */
function AppRoutes() {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    bootstrapSession().finally(() => setReady(true));
  }, []);

  if (!ready) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
        <Spinner />
      </div>
    );
  }

  return <RouterProvider router={router} />;
}

/** The delivery agent app's root — wires up the Redux store, toast provider, and router. */
export function App() {
  return (
    <Provider store={store}>
      <ToastProvider direction="bottom">
        <AppRoutes />
      </ToastProvider>
    </Provider>
  );
}
