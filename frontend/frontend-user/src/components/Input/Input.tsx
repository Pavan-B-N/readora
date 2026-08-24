import { forwardRef, type InputHTMLAttributes, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react';
import styles from './Input.module.css';

interface FieldWrapperProps {
  label: string;
  error?: string;
  children: React.ReactNode;
}

function FieldWrapper({ label, error, children }: FieldWrapperProps) {
  return (
    <label className={styles.field}>
      <span className={styles.label}>{label}</span>
      {children}
      {error && <span className={styles.errorText}>{error}</span>}
    </label>
  );
}

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(({ label, error, className, ...rest }, ref) => (
  <FieldWrapper label={label} error={error}>
    <input
      ref={ref}
      className={[styles.input, error && styles.invalid, className].filter(Boolean).join(' ')}
      {...rest}
    />
  </FieldWrapper>
));
Input.displayName = 'Input';

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string;
  error?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(({ label, error, className, ...rest }, ref) => (
  <FieldWrapper label={label} error={error}>
    <textarea
      ref={ref}
      className={[styles.textarea, error && styles.invalid, className].filter(Boolean).join(' ')}
      {...rest}
    />
  </FieldWrapper>
));
Textarea.displayName = 'Textarea';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
  error?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(({ label, error, className, children, ...rest }, ref) => (
  <FieldWrapper label={label} error={error}>
    <select
      ref={ref}
      className={[styles.select, error && styles.invalid, className].filter(Boolean).join(' ')}
      {...rest}
    >
      {children}
    </select>
  </FieldWrapper>
));
Select.displayName = 'Select';
