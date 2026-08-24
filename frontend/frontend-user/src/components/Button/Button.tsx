import type { ButtonHTMLAttributes } from 'react';
import styles from './Button.module.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  iconOnly?: boolean;
  block?: boolean;
}

export function Button({
  variant = 'primary',
  size = 'md',
  iconOnly = false,
  block = false,
  className,
  ...rest
}: ButtonProps) {
  const classes = [
    styles.button,
    styles[variant],
    size !== 'md' && styles[size],
    iconOnly && styles.iconOnly,
    block && styles.block,
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return <button className={classes} {...rest} />;
}
