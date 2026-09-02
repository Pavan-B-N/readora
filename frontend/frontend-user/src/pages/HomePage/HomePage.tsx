import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { BookOpen, Clock, Package, X, ChevronLeft, ChevronRight, SlidersHorizontal, Zap } from 'lucide-react';
import {
  searchBooks,
  getCategoryTree,
  getRecommendations,
  getPurchasedBooks,
  getLibrary,
  getBooksByIds,
  listAuthors,
} from '@/api/catalogApi';
import { getBrowsingHistory } from '@/api/userApi';
import type { Author, BookSummary, CategoryNode, PurchasedBook } from '@/types/catalog';
import { useDebounced } from '@/hooks/useDebounced';
import { useAppSelector } from '@/redux/hooks';
import { BookCard } from '@/components/BookCard';
import { Button } from '@readora/shared-ui';
import { EmptyState } from '@readora/shared-ui';
import { Spinner } from '@/components/Spinner';
import { statusVariant, displayStatus } from '@/utils/orderStatus';
import { ROUTES } from '@/constants/routes';
import styles from './HomePage.module.css';

const PAGE_SIZE = 18;

const gridVariants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.03 } },
};

const cardVariants = {
  hidden: { opacity: 0, y: 14 },
  show: { opacity: 1, y: 0 },
};

interface FlatCategory {
  id: string;
  name: string;
  depth: number;
}

function flatten(nodes: CategoryNode[], depth = 0): FlatCategory[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, depth },
    ...flatten(node.children, depth + 1),
  ]);
}

const statusChipClassByVariant: Record<ReturnType<typeof statusVariant>, string> = {
  success: styles.statusSuccess,
  danger: styles.statusDanger,
  warning: styles.statusWarning,
  info: styles.statusInfo,
};

export function HomePage() {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') ?? '';
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const { selectedId: storeId, resolved: storeResolved } = useAppSelector((state) => state.store);

  const [books, setBooks] = useState<BookSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [categoriesLoading, setCategoriesLoading] = useState(true);
  const [categoryId, setCategoryId] = useState('');
  const [recommendedIds, setRecommendedIds] = useState<Set<string>>(new Set());
  const [orders, setOrders] = useState<PurchasedBook[]>([]);
  const [ownedVirtualIds, setOwnedVirtualIds] = useState<Set<string>>(new Set());
  const [recentlyViewed, setRecentlyViewed] = useState<BookSummary[]>([]);

  const [authors, setAuthors] = useState<Author[]>([]);
  const [authorId, setAuthorId] = useState('');
  const [virtualOnly, setVirtualOnly] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const filtersRef = useRef<HTMLDivElement>(null);

  const debouncedCategoryId = useDebounced(categoryId, 150);

  useEffect(() => {
    getCategoryTree().then((tree) => {
      setCategories(flatten(tree));
      setCategoriesLoading(false);
    });
    listAuthors().then(setAuthors);
  }, []);

  useEffect(() => {
    if (!filtersOpen) return;
    const onClickOutside = (e: MouseEvent) => {
      if (filtersRef.current && !filtersRef.current.contains(e.target as Node)) setFiltersOpen(false);
    };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, [filtersOpen]);

  // Recommended items aren't a separate rail — they're just sorted first in the main feed below.
  useEffect(() => {
    if (!accessToken || !storeResolved) {
      setRecommendedIds(new Set());
      return;
    }
    getRecommendations(storeId ?? undefined).then((items) => setRecommendedIds(new Set(items.map((b) => b.id))));
  }, [accessToken, storeId, storeResolved]);

  useEffect(() => {
    if (!accessToken) {
      setOrders([]);
      setOwnedVirtualIds(new Set());
      return;
    }
    getPurchasedBooks().then(setOrders);
    getLibrary().then((books) => setOwnedVirtualIds(new Set(books.map((b) => b.id))));
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) {
      setRecentlyViewed([]);
      return;
    }
    getBrowsingHistory().then((history) => {
      const ids = history.map((item) => item.bookId);
      if (ids.length === 0) {
        setRecentlyViewed([]);
        return;
      }
      // The batch lookup doesn't guarantee it preserves order, so re-sort by view recency here.
      getBooksByIds(ids).then((books) => {
        const byId = new Map(books.map((book) => [book.id, book]));
        setRecentlyViewed(ids.map((id) => byId.get(id)).filter((book): book is BookSummary => Boolean(book)));
      });
    });
  }, [accessToken]);

  useEffect(() => {
    setPage(0);
  }, [query, debouncedCategoryId, authorId, virtualOnly]);

  useEffect(() => {
    if (!storeResolved) return;

    let cancelled = false;
    setLoading(true);

    searchBooks({
      q: query || undefined,
      categoryId: debouncedCategoryId || undefined,
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
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [query, page, debouncedCategoryId, authorId, virtualOnly, storeId, storeResolved, recommendedIds]);

  const activeCategoryName = categories.find((c) => c.id === categoryId)?.name;
  const activeAuthorName = authors.find((a) => a.id === authorId)?.name;
  const activeFilterCount = (authorId ? 1 : 0) + (virtualOnly ? 1 : 0);

  return (
    <div className={styles.layout}>
      <aside className={styles.sidebar}>
        <span className={styles.sidebarLabel}>Categories</span>
        {categoriesLoading ? (
          <div style={{ padding: 'var(--space-2)' }}>
            <Spinner />
          </div>
        ) : (
          <>
            <button
              type="button"
              className={[styles.categoryButton, !categoryId && styles.categoryActive].filter(Boolean).join(' ')}
              onClick={() => setCategoryId('')}
            >
              All books
            </button>
            {categories.map((c) => (
              <button
                type="button"
                key={c.id}
                className={[
                  styles.categoryButton,
                  c.depth > 0 && styles.categoryChild,
                  categoryId === c.id && styles.categoryActive,
                ]
                  .filter(Boolean)
                  .join(' ')}
                onClick={() => setCategoryId(c.id === categoryId ? '' : c.id)}
              >
                {c.name}
              </button>
            ))}
          </>
        )}
      </aside>

      <div className={styles.content}>
        {!query && !categoryId && orders.length > 0 && (
          <div className={styles.rail}>
            <div className={styles.railHeaderRow}>
              <div className={styles.railHeading}>
                <Package size={15} />
                <h2 className={styles.railTitle}>Your orders</h2>
              </div>
              {orders.length > 5 && (
                <Link to={ROUTES.orders} className={styles.railAction}>
                  View all <ChevronRight size={14} />
                </Link>
              )}
            </div>
            <div className={styles.railRowWrapper}>
              <div className={styles.railRow}>
                {orders.slice(0, 5).map((item, i) => (
                  <div className={styles.railCard} key={`${item.book.id}-${item.placedAt}-${i}`}>
                    <BookCard
                      book={item.book}
                      addLabel="Buy again"
                      owned={ownedVirtualIds.has(item.book.id)}
                      footer={
                        <span className={[styles.statusChip, statusChipClassByVariant[statusVariant(item.orderStatus)]].join(' ')}>
                          {displayStatus(item.orderStatus)}
                        </span>
                      }
                    />
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {!query && !categoryId && recentlyViewed.length > 0 && (
          <div className={styles.rail}>
            <div className={styles.railHeaderRow}>
              <div className={styles.railHeading}>
                <Clock size={15} />
                <h2 className={styles.railTitle}>Recently viewed</h2>
              </div>
            </div>
            <div className={styles.railRowWrapper}>
              <div className={styles.railRow}>
                {recentlyViewed.map((book) => (
                  <div className={styles.railCard} key={book.id}>
                    <BookCard book={book} owned={ownedVirtualIds.has(book.id)} />
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        <div className={styles.railHeaderRow}>
          <div className={styles.railHeading}>
            <BookOpen size={15} />
            <h2 className={styles.railTitle}>
              {query ? `Results for “${query}”` : activeCategoryName ? activeCategoryName : 'Browse books'}
            </h2>
          </div>
        </div>

        <div className={styles.resultsBar}>
          <div className={styles.chips}>
            {activeCategoryName && (
              <span className={styles.chip}>
                {activeCategoryName}
                <button type="button" onClick={() => setCategoryId('')} aria-label="Clear category filter">
                  <X size={11} />
                </button>
              </span>
            )}
            {activeAuthorName && (
              <span className={styles.chip}>
                {activeAuthorName}
                <button type="button" onClick={() => setAuthorId('')} aria-label="Clear author filter">
                  <X size={11} />
                </button>
              </span>
            )}
            {virtualOnly && (
              <span className={styles.chip}>
                <Zap size={11} />
                Virtual only
                <button type="button" onClick={() => setVirtualOnly(false)} aria-label="Clear virtual-only filter">
                  <X size={11} />
                </button>
              </span>
            )}

            <div className={styles.filterWrap} ref={filtersRef}>
              <button
                type="button"
                className={[styles.filterButton, activeFilterCount > 0 && styles.filterButtonActive].filter(Boolean).join(' ')}
                onClick={() => setFiltersOpen((o) => !o)}
                aria-label="Filters"
              >
                <SlidersHorizontal size={14} />
                Filters
                {activeFilterCount > 0 && <span className={styles.filterCount}>{activeFilterCount}</span>}
              </button>

              {filtersOpen && (
                <div className={styles.filterPanel}>
                  <label className={styles.filterCheckboxRow}>
                    <input
                      type="checkbox"
                      checked={virtualOnly}
                      onChange={(e) => setVirtualOnly(e.target.checked)}
                    />
                    <Zap size={13} />
                    Virtual editions only
                  </label>

                  <div className={styles.filterField}>
                    <span className={styles.filterLabel}>Category</span>
                    <select
                      className={styles.filterSelect}
                      value={categoryId}
                      onChange={(e) => setCategoryId(e.target.value)}
                    >
                      <option value="">All categories</option>
                      {categories.map((c) => (
                        <option key={c.id} value={c.id}>
                          {'—'.repeat(c.depth)} {c.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className={styles.filterField}>
                    <span className={styles.filterLabel}>Author</span>
                    <select
                      className={styles.filterSelect}
                      value={authorId}
                      onChange={(e) => setAuthorId(e.target.value)}
                    >
                      <option value="">All authors</option>
                      {authors.map((a) => (
                        <option key={a.id} value={a.id}>
                          {a.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  {(activeFilterCount > 0 || categoryId) && (
                    <button
                      type="button"
                      className={styles.filterClear}
                      onClick={() => {
                        setAuthorId('');
                        setVirtualOnly(false);
                        setCategoryId('');
                      }}
                    >
                      Clear all filters
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>
          <span className={styles.count}>
            {loading ? null : `${totalElements} book${totalElements === 1 ? '' : 's'}`}
          </span>
        </div>

        {loading ? (
          <div className={styles.grid}>
            {Array.from({ length: PAGE_SIZE }).map((_, i) => (
              <div className={styles.skeletonCard} key={i}>
                <div className={[styles.skeletonCover, styles.shimmer].join(' ')} />
                <div className={[styles.skeletonLine, styles.shimmer].join(' ')} style={{ width: '85%' }} />
                <div className={[styles.skeletonLine, styles.shimmer].join(' ')} style={{ width: '55%' }} />
              </div>
            ))}
          </div>
        ) : books.length === 0 ? (
          <EmptyState
            icon={BookOpen}
            title="No books found"
            description={
              query
                ? `Nothing matches “${query}”. Try a different search, or ask the assistant in the corner.`
                : 'Try clearing a filter to see more of the catalogue.'
            }
            action={
              categoryId || authorId || virtualOnly ? (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setCategoryId('');
                    setAuthorId('');
                    setVirtualOnly(false);
                  }}
                >
                  Clear filters
                </Button>
              ) : undefined
            }
          />
        ) : (
          <>
            <motion.div
              className={styles.grid}
              key={`${page}-${debouncedCategoryId}-${authorId}-${virtualOnly}-${query}`}
              variants={gridVariants}
              initial="hidden"
              animate="show"
            >
              {books.map((book) => (
                <motion.div variants={cardVariants} key={book.id}>
                  <BookCard book={book} />
                </motion.div>
              ))}
            </motion.div>

            {totalPages > 1 && (
              <div className={styles.pagination}>
                <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                  <ChevronLeft size={14} />
                  Previous
                </Button>
                <span>
                  Page {page + 1} of {totalPages}
                </span>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                  <ChevronRight size={14} />
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
