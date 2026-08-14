import type { ButtonHTMLAttributes } from 'react';
import styles from './Button.module.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  /** Square padding with no text gap, for a button that only contains an icon. */
  iconOnly?: boolean;
  /** Stretches the button to fill the width of its container. */
  block?: boolean;
}

/** The standard clickable button, styled by variant/size/layout flags instead of raw className. */
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
