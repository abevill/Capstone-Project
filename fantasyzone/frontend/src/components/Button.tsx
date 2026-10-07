import type { ButtonHTMLAttributes } from 'react';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger';
  loading?: boolean;
}

export default function Button({ variant = 'primary', loading = false, type = 'button', disabled, children, ...rest }: Props) {
  return (
    <button type={type} className={`btn btn-${variant}`} disabled={disabled || loading} aria-busy={loading} {...rest}>
      {loading ? 'Please wait...' : children}
    </button>
  );
}
