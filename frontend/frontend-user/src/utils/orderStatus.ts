export function statusVariant(status: string) {
  if (status === 'DELIVERED' || status === 'CONFIRMED' || status === 'PAID') return 'success' as const;
  if (status === 'CANCELLED' || status === 'PAYMENT_FAILED') return 'danger' as const;
  if (status === 'ASSIGNED' || status === 'SHIPPED') return 'info' as const;
  return 'warning' as const;
}

function prettyStatus(status: string) {
  return status
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

/** SHIPPED is stored as-is (see backend's OrderStatus javadoc) but reads as "Out for delivery" here. */
export function displayStatus(status: string) {
  if (status === 'SHIPPED') return 'Out for delivery';
  return prettyStatus(status);
}
