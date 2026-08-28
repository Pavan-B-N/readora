import { useEffect, useRef } from 'react';
import { useAppSelector } from '@/redux/hooks';

/**
 * Fires `onMatch` whenever a new order-status push notification arrives over NotificationBell's
 * WebSocket subscription while this component is mounted (that subscription already dispatches
 * every arrival into the notifications slice — this just watches for one relevant to the caller,
 * rather than opening a second connection). Skips whatever notifications are already in state at
 * mount — from the initial history fetch, or a previous page visit — since those aren't new
 * arrivals; only a genuinely fresh one should trigger a refetch.
 */
export function useOrderStatusNotifications(shouldRefresh: (orderId: string | null) => boolean, onMatch: () => void) {
  const items = useAppSelector((state) => state.notifications.items);
  const isFirstRun = useRef(true);
  const shouldRefreshRef = useRef(shouldRefresh);
  const onMatchRef = useRef(onMatch);
  shouldRefreshRef.current = shouldRefresh;
  onMatchRef.current = onMatch;

  useEffect(() => {
    if (isFirstRun.current) {
      isFirstRun.current = false;
      return;
    }
    const latest = items[0];
    if (latest && shouldRefreshRef.current(latest.orderId)) onMatchRef.current();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items]);
}
