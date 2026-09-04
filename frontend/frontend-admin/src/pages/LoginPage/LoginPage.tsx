import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useLocation, useNavigate } from 'react-router-dom';
import { AlertCircle, Bike, ClipboardList, Library, LogIn, Store } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { login } from '@/redux/slices/authSlice';
import { Input } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { AuthLayout } from '@readora/shared-ui';
import { ROUTES } from '@/constants/routes';
import styles from '@/styles/authForm.module.css';

const AUTH_MESSAGES = [
  { title: 'Run the show.', subtitle: 'Catalog, inventory, and orders — all in one console.' },
  { title: 'Keep deliveries moving.', subtitle: 'Track every order from checkout to doorstep.' },
  { title: 'Stay in control.', subtitle: 'Manage stores, delivery agents, and returns with confidence.' },
];

const AUTH_STATS = [
  { value: '10k+', label: 'Books' },
  { value: '21', label: 'Stores' },
  { value: '24/7', label: 'Operations' },
];

const loginSchema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export function LoginPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { status, error } = useAppSelector((state) => state.auth);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (values: LoginFormValues) => {
    const result = await dispatch(login(values));
    if (login.fulfilled.match(result)) {
      const from = (location.state as { from?: { pathname: string } })?.from?.pathname ?? ROUTES.books;
      navigate(from, { replace: true });
    }
  };

  return (
    <AuthLayout
      brandIcon={Library}
      brandName="Readora Admin"
      floatIcons={[ClipboardList, Store, Bike, Library]}
      messages={AUTH_MESSAGES}
      stats={AUTH_STATS}
    >
      <div>
        <h1 className={styles.title}>Readora Admin</h1>
        <p className={styles.subtitle}>Sign in to manage the catalogue, orders, and stores</p>
      </div>

      <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
        <Input
          label="Email"
          type="email"
          autoComplete="username"
          placeholder="admin@readora.dev"
          error={errors.email?.message}
          {...register('email')}
        />
        <Input
          label="Password"
          type="password"
          autoComplete="current-password"
          placeholder="••••••••"
          error={errors.password?.message}
          {...register('password')}
        />

        {error && (
          <div className={styles.formError}>
            <AlertCircle size={15} />
            {error}
          </div>
        )}

        <Button type="submit" disabled={status === 'loading'} block>
          <LogIn size={15} />
          {status === 'loading' ? 'Signing in…' : 'Sign in'}
        </Button>
      </form>

      <p className={styles.footer}>Restricted access — Readora staff only</p>
    </AuthLayout>
  );
}
