import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useLocation, useNavigate } from 'react-router-dom';
import { AlertCircle, Bike, LogIn, MapPin, Package, Wallet } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { login } from '@/redux/slices/authSlice';
import { Input } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { AuthLayout } from '@readora/shared-ui';
import { ROUTES } from '@/constants/routes';
import styles from '@/styles/authForm.module.css';

const AUTH_MESSAGES = [
  { title: 'Deliver, earn, repeat.', subtitle: 'Claim orders at your store and get moving in seconds.' },
  { title: 'Know before you go.', subtitle: 'Customer, address, and items — all up front, before you accept.' },
  { title: 'Every job counts.', subtitle: 'Track your earnings and completed jobs from one profile.' },
];

const AUTH_STATS = [
  { value: '₹40', label: 'Per job' },
  { value: '~30 min', label: 'Avg delivery' },
  { value: '24/7', label: 'Go on duty' },
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
      const from = (location.state as { from?: { pathname: string } })?.from?.pathname ?? ROUTES.orders;
      navigate(from, { replace: true });
    }
  };

  return (
    <AuthLayout
      brandIcon={Bike}
      brandName="Readora Delivery"
      floatIcons={[Package, MapPin, Bike, Wallet]}
      messages={AUTH_MESSAGES}
      stats={AUTH_STATS}
    >
      <div>
        <h1 className={styles.title}>Readora Delivery</h1>
        <p className={styles.subtitle}>Sign in to see your assigned deliveries</p>
      </div>

      <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
        <Input
          label="Email"
          type="email"
          autoComplete="username"
          placeholder="agent.bangalore@readora.dev"
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

      <p className={styles.footer}>Delivery agents only — ask your store manager for access</p>
    </AuthLayout>
  );
}
