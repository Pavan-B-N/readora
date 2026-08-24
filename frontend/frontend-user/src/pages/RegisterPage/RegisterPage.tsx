import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, UserPlus } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { register as registerUser } from '@/redux/slices/authSlice';
import { Card } from '@/components/Card';
import { Input } from '@/components/Input';
import { Button } from '@/components/Button';
import { ROUTES } from '@/constants/routes';
import styles from '../LoginPage/LoginPage.module.css';

const registerSchema = z.object({
  fullName: z.string().min(1, 'Full name is required'),
  email: z.string().email('Enter a valid email'),
  password: z.string().min(10, 'Password must be at least 10 characters'),
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export function RegisterPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { status, error } = useAppSelector((state) => state.auth);

  const {
    register: registerField,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) });

  const onSubmit = async (values: RegisterFormValues) => {
    const result = await dispatch(registerUser(values));
    if (registerUser.fulfilled.match(result)) {
      navigate(ROUTES.home, { replace: true });
    }
  };

  return (
    <div className={styles.page}>
      <Card className={styles.card}>
        <div>
          <h1 className={styles.title}>Create your account</h1>
          <p className={styles.subtitle}>Get ₹100 in your wallet when you sign up</p>
        </div>

        <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
          <Input
            label="Full name"
            placeholder="Alice Nguyen"
            error={errors.fullName?.message}
            {...registerField('fullName')}
          />
          <Input
            label="Email"
            type="email"
            autoComplete="username"
            placeholder="you@example.com"
            error={errors.email?.message}
            {...registerField('email')}
          />
          <Input
            label="Password"
            type="password"
            autoComplete="new-password"
            hint="At least 10 characters"
            placeholder="••••••••••"
            error={errors.password?.message}
            {...registerField('password')}
          />

          {error && (
            <div className={styles.formError}>
              <AlertCircle size={15} />
              {error}
            </div>
          )}

          <Button type="submit" disabled={status === 'loading'} block>
            <UserPlus size={15} />
            {status === 'loading' ? 'Creating account…' : 'Create account'}
          </Button>
        </form>

        <div className={styles.altAction}>
          Already have an account? <Link to={ROUTES.login}>Sign in</Link>
        </div>
      </Card>
    </div>
  );
}
