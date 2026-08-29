import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, LogIn } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { login } from '@/redux/slices/authSlice';
import { Input } from '@/components/Input';
import { Button } from '@/components/Button';
import { AuthLayout } from '@/components/AuthLayout';
import { ROUTES } from '@/constants/routes';
import styles from '@/components/AuthLayout/AuthLayout.module.css';

const loginSchema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export function LoginPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { status, error } = useAppSelector((state) => state.auth);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (values: LoginFormValues) => {
    const result = await dispatch(login(values));
    if (login.fulfilled.match(result)) {
      navigate(ROUTES.books, { replace: true });
    }
  };

  return (
    <AuthLayout>
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
