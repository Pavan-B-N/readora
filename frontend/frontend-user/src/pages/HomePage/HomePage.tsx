import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { BookOpen, Package, X, ChevronLeft, ChevronRight } from 'lucide-react';
import { searchBooks, getCategoryTree, getRecommendations, getPurchasedBooks } from '@/api/catalogApi';
import type { BookSummary, CategoryNode, PurchasedBook } from '@/types/catalog';
import { useDebounced } from '@/hooks/useDebounced';
import { useAppSelector } from '@/redux/hooks';
import { BookCard } from '@/components/BookCard';
import { Button } from '@/components/Button';
import { EmptyState } from '@/components/EmptyState';
import { statusVariant, displayStatus } from '@/utils/orderStatus';
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
  const [categoryId, setCategoryId] = useState('');
  const [recommendedIds, setRecommendedIds] = useState<Set<string>>(new Set());
  const [orders, setOrders] = useState<PurchasedBook[]>([]);

  const debouncedCategoryId = useDebounced(categoryId, 150);

  useEffect(() => {
    getCategoryTree().then((tree) => setCategories(flatten(tree)));
  }, []);

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
      return;
    }
    getPurchasedBooks().then(setOrders);
  }, [accessToken]);

  useEffect(() => {
    setPage(0);
  }, [query, debouncedCategoryId]);

  useEffect(() => {
    if (!storeResolved) return;

    let cancelled = false;
    setLoading(true);

    searchBooks({
      q: query || undefined,
      categoryId: debouncedCategoryId || undefined,
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
  }, [query, page, debouncedCategoryId, storeId, storeResolved, recommendedIds]);

  const activeCategoryName = categories.find((c) => c.id === categoryId)?.name;

  return (
    <div className={styles.layout}>
      <aside className={styles.sidebar}>
        <span className={styles.sidebarLabel}>Categories</span>
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
      </aside>

      <div className={styles.content}>
        {!query && !categoryId && orders.length > 0 && (
          <div className={styles.rail}>
            <div className={styles.railHeading}>
              <Package size={15} />
              <h2 className={styles.railTitle}>Your orders</h2>
            </div>
            <div className={styles.railRowWrapper}>
              <div className={styles.railRow}>
                {orders.map((item, i) => (
                  <div className={styles.railCard} key={`${item.book.id}-${item.placedAt}-${i}`}>
                    <BookCard
                      book={item.book}
                      addLabel="Buy again"
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

        <div className={styles.railHeading}>
          <BookOpen size={15} />
          <h2 className={styles.railTitle}>
            {query ? `Results for “${query}”` : activeCategoryName ? activeCategoryName : 'Browse books'}
          </h2>
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
          </div>
          <span className={styles.count}>
            {loading ? 'Loading…' : `${totalElements} book${totalElements === 1 ? '' : 's'}`}
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
              categoryId ? (
                <Button variant="secondary" size="sm" onClick={() => setCategoryId('')}>
                  Clear filters
                </Button>
              ) : undefined
            }
          />
        ) : (
          <>
            <motion.div
              className={styles.grid}
              key={`${page}-${debouncedCategoryId}-${query}`}
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
