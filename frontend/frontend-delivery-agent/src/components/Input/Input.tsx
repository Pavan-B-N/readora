import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react';
import styles from './Input.module.css';

interface FieldWrapperProps {
  label?: string;
  hint?: string;
  error?: string;
  children: ReactNode;
}

export function FieldWrapper({ label, hint, error, children }: FieldWrapperProps) {
  return (
    <label className={styles.field}>
      {label && (
        <span className={styles.labelRow}>
          <span className={styles.label}>{label}</span>
          {hint && <span className={styles.hint}>{hint}</span>}
        </span>
      )}
      {children}
      {error && <span className={styles.errorText}>{error}</span>}
    </label>
  );
}

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  hint?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, hint, error, className, ...rest }, ref) => (
    <FieldWrapper label={label} hint={hint} error={error}>
      <input
        ref={ref}
        className={[styles.input, error && styles.invalid, className].filter(Boolean).join(' ')}
        {...rest}
      />
    </FieldWrapper>
  ),
);
Input.displayName = 'Input';
