import { useEffect, useState } from 'react';
import { searchBooks } from '@/api/catalogApi';
import { extractErrorMessage } from '@/api/client';
import type { BookSummary } from '@/types/catalog';
import { useAppSelector } from '@/redux/hooks';

const PAGE_SIZE = 18;

interface UseCatalogSearchParams {
  query: string;
  categoryId: string;
  authorId: string;
  virtualOnly: boolean;
  recommendedIds: Set<string>;
}

/** Drives the main paginated book grid; resets to page 0 whenever a search input changes. */
export function useCatalogSearch({ query, categoryId, authorId, virtualOnly, recommendedIds }: UseCatalogSearchParams) {
  const { selectedId: storeId, resolved: storeResolved } = useAppSelector((state) => state.store);

  const [books, setBooks] = useState<BookSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryToken, setRetryToken] = useState(0);

  useEffect(() => {
    setPage(0);
  }, [query, categoryId, authorId, virtualOnly]);

  useEffect(() => {
    if (!storeResolved) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    searchBooks({
      q: query || undefined,
      categoryId: categoryId || undefined,
      authorId: authorId || undefined,
      virtualOnly: virtualOnly || undefined,
      storeId: storeId ?? undefined,
      page,
      size: PAGE_SIZE,
    })
      .then((result) => {
        if (cancelled) return;
        const items = result.items ?? [];
        // Stable partition: recommended-for-you titles surface first, everything else follows
        // in the order the backend returned it — no separate "Recommended" rail, this is it.
        const sorted = [...items].sort((a, b) => {
          const aRec = recommendedIds.has(a.id) ? 0 : 1;
          const bRec = recommendedIds.has(b.id) ? 0 : 1;
          return aRec - bRec;
        });
        setBooks(sorted);
        setTotalPages(result.totalPages);
        setTotalElements(result.totalElements);
      })
      .catch((err) => {
        if (!cancelled) setError(extractErrorMessage(err, "Something went wrong while fetching the catalogue."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [query, page, categoryId, authorId, virtualOnly, storeId, storeResolved, recommendedIds, retryToken]);

  const retry = () => setRetryToken((t) => t + 1);

  return { books, page, setPage, totalPages, totalElements, loading, error, retry, PAGE_SIZE };
}
