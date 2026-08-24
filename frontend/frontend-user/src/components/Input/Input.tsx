import {
  forwardRef,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react';
import styles from './Input.module.css';

interface FieldWrapperProps {
  label?: string;
  hint?: string;
  error?: string;
  required?: boolean;
  children: ReactNode;
}

export function FieldWrapper({ label, hint, error, required, children }: FieldWrapperProps) {
  return (
    <label className={styles.field}>
      {label && (
        <span className={styles.labelRow}>
          <span className={styles.label}>
            {label}
            {required && <span className={styles.required}> *</span>}
          </span>
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
  ({ label, hint, error, required, className, ...rest }, ref) => (
    <FieldWrapper label={label} hint={hint} error={error} required={required}>
      <input
        ref={ref}
        className={[styles.input, error && styles.invalid, className].filter(Boolean).join(' ')}
        {...rest}
      />
    </FieldWrapper>
  ),
);
Input.displayName = 'Input';

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  hint?: string;
  error?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, hint, error, required, className, ...rest }, ref) => (
    <FieldWrapper label={label} hint={hint} error={error} required={required}>
      <textarea
        ref={ref}
        className={[styles.textarea, error && styles.invalid, className].filter(Boolean).join(' ')}
        {...rest}
      />
    </FieldWrapper>
  ),
);
Textarea.displayName = 'Textarea';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  hint?: string;
  error?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, hint, error, required, className, children, ...rest }, ref) => (
    <FieldWrapper label={label} hint={hint} error={error} required={required}>
      <select
        ref={ref}
        className={[styles.select, error && styles.invalid, className].filter(Boolean).join(' ')}
        {...rest}
      >
        {children}
      </select>
    </FieldWrapper>
  ),
);
Select.displayName = 'Select';
