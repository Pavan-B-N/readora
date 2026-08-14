import {
  forwardRef,
  useId,
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
  errorId?: string;
  children: ReactNode;
}

/** The label/hint/error-text chrome shared by Input, Textarea, and Select, wrapped around whatever field element is passed as children. */
export function FieldWrapper({ label, hint, error, required, errorId, children }: FieldWrapperProps) {
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

/** A single-line text input with an attached label/hint/error, wired for a11y (aria-invalid/aria-describedby). */
export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, hint, error, required, className, id, ...rest }, ref) => {
    const generatedId = useId();
    const errorId = error ? `${id ?? generatedId}-error` : undefined;
    return (
      <FieldWrapper label={label} hint={hint} error={error} required={required} errorId={errorId}>
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

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  hint?: string;
  error?: string;
}

/** A multi-line text input with an attached label/hint/error, wired for a11y (aria-invalid/aria-describedby). */
export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, hint, error, required, className, id, ...rest }, ref) => {
    const generatedId = useId();
    const errorId = error ? `${id ?? generatedId}-error` : undefined;
    return (
      <FieldWrapper label={label} hint={hint} error={error} required={required} errorId={errorId}>
        <textarea
          ref={ref}
          id={id}
          className={[styles.textarea, error && styles.invalid, className].filter(Boolean).join(' ')}
          aria-invalid={!!error}
          aria-describedby={errorId}
          {...rest}
        />
      </FieldWrapper>
    );
  },
);
Textarea.displayName = 'Textarea';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  hint?: string;
  error?: string;
}

/** A native select dropdown with an attached label/hint/error, wired for a11y (aria-invalid/aria-describedby). */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, hint, error, required, className, children, id, ...rest }, ref) => {
    const generatedId = useId();
    const errorId = error ? `${id ?? generatedId}-error` : undefined;
    return (
      <FieldWrapper label={label} hint={hint} error={error} required={required} errorId={errorId}>
        <select
          ref={ref}
          id={id}
          className={[styles.select, error && styles.invalid, className].filter(Boolean).join(' ')}
          aria-invalid={!!error}
          aria-describedby={errorId}
          {...rest}
        >
          {children}
        </select>
      </FieldWrapper>
    );
  },
);
Select.displayName = 'Select';
