import { useEffect, useState } from 'react';
import { useToast } from '@readora/shared-ui';
import { getBooksByIds, getLibrary, getPurchasedBooks, getRecommendations } from '@/api/catalogApi';
import { getBrowsingHistory } from '@/api/userApi';
import { extractErrorMessage } from '@/api/client';
import type { BookSummary, PurchasedBook } from '@/types/catalog';
import { useAppSelector } from '@/redux/hooks';

/** The signed-in-only rails on the home page: recommendations, past orders, owned titles, recently viewed. */
export function useHomeFeedExtras() {
  const { showToast } = useToast();
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const { selectedId: storeId, resolved: storeResolved } = useAppSelector((state) => state.store);

  const [recommendedIds, setRecommendedIds] = useState<Set<string>>(new Set());
  const [orders, setOrders] = useState<PurchasedBook[]>([]);
  const [ownedVirtualIds, setOwnedVirtualIds] = useState<Set<string>>(new Set());
  const [recentlyViewed, setRecentlyViewed] = useState<BookSummary[]>([]);

  // Recommended items aren't a separate rail — they're just sorted first in the main feed below.
  useEffect(() => {
    if (!accessToken || !storeResolved) {
      setRecommendedIds(new Set());
      return;
    }
    getRecommendations(storeId ?? undefined)
      .then((items) => setRecommendedIds(new Set(items.map((b) => b.id))))
      .catch((err) => showToast(extractErrorMessage(err, 'Could not load recommendations'), 'error'));
  }, [accessToken, storeId, storeResolved, showToast]);

  useEffect(() => {
    if (!accessToken) {
      setOrders([]);
      setOwnedVirtualIds(new Set());
      return;
    }
    getPurchasedBooks()
      .then(setOrders)
      .catch((err) => showToast(extractErrorMessage(err, 'Could not load your orders'), 'error'));
    getLibrary()
      .then((books) => setOwnedVirtualIds(new Set(books.map((b) => b.id))))
      .catch((err) => showToast(extractErrorMessage(err, 'Could not load your library'), 'error'));
  }, [accessToken, showToast]);

  useEffect(() => {
    if (!accessToken) {
      setRecentlyViewed([]);
      return;
    }
    getBrowsingHistory()
      .then((history) => {
        const ids = history.map((item) => item.bookId);
        if (ids.length === 0) {
          setRecentlyViewed([]);
          return;
        }
        // The batch lookup doesn't guarantee it preserves order, so re-sort by view recency here.
        return getBooksByIds(ids).then((books) => {
          const byId = new Map(books.map((book) => [book.id, book]));
          setRecentlyViewed(ids.map((id) => byId.get(id)).filter((book): book is BookSummary => Boolean(book)));
        });
      })
      .catch((err) => showToast(extractErrorMessage(err, 'Could not load recently viewed books'), 'error'));
  }, [accessToken, showToast]);

  return { recommendedIds, orders, ownedVirtualIds, recentlyViewed };
}
