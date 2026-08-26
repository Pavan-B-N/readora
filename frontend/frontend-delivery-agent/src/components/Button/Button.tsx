import type { ButtonHTMLAttributes } from 'react';
import styles from './Button.module.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  block?: boolean;
}

export function Button({ variant = 'primary', size = 'md', block = false, className, ...rest }: ButtonProps) {
  const classes = [styles.button, styles[variant], size !== 'md' && styles[size], block && styles.block, className]
    .filter(Boolean)
    .join(' ');

  return <button className={classes} {...rest} />;
}
