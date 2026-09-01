import { forwardRef, useId, type InputHTMLAttributes, type ReactNode } from 'react';
import styles from './Input.module.css';

interface FieldWrapperProps {
  label?: string;
  hint?: string;
  error?: string;
  errorId?: string;
  children: ReactNode;
}

export function FieldWrapper({ label, hint, error, errorId, children }: FieldWrapperProps) {
  return (
    <label className={styles.field}>
      {label && (
        <span className={styles.labelRow}>
          <span className={styles.label}>{label}</span>
          {hint && <span className={styles.hint}>{hint}</span>}
        </span>
      )}
      {children}
      {error && (
        <span className={styles.errorText} id={errorId} role="alert">
          {error}
        </span>
      )}
    </label>
  );
}

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  hint?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, hint, error, className, id, ...rest }, ref) => {
    const generatedId = useId();
    const errorId = error ? `${id ?? generatedId}-error` : undefined;
    return (
      <FieldWrapper label={label} hint={hint} error={error} errorId={errorId}>
        <input
          ref={ref}
          id={id}
          className={[styles.input, error && styles.invalid, className].filter(Boolean).join(' ')}
          aria-invalid={!!error}
          aria-describedby={errorId}
          {...rest}
        />
      </FieldWrapper>
    );
  },
);
Input.displayName = 'Input';
