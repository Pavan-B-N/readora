import { Link, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { BookOpen, Clock, Package, X, ChevronLeft, ChevronRight, SlidersHorizontal, Zap, AlertCircle } from 'lucide-react';
import { BookCard } from '@/components/BookCard';
import { Button, EmptyState } from '@readora/shared-ui';
import { Spinner } from '@readora/shared-ui';
import { statusVariant, displayStatus } from '@/utils/orderStatus';
import { ROUTES } from '@/constants/routes';
import { useCatalogFilters } from './hooks/useCatalogFilters';
import { useHomeFeedExtras } from './hooks/useHomeFeedExtras';
import { useCatalogSearch } from './hooks/useCatalogSearch';
import styles from './HomePage.module.css';

const gridVariants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.03 } },
};

const cardVariants = {
  hidden: { opacity: 0, y: 14 },
  show: { opacity: 1, y: 0 },
};

const statusChipClassByVariant: Record<ReturnType<typeof statusVariant>, string> = {
  success: styles.statusSuccess,
  danger: styles.statusDanger,
  warning: styles.statusWarning,
  info: styles.statusInfo,
};

export function HomePage() {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') ?? '';

  const filters = useCatalogFilters();
  const feed = useHomeFeedExtras();
  const search = useCatalogSearch({
    query,
    categoryId: filters.debouncedCategoryId,
    authorId: filters.authorId,
    virtualOnly: filters.virtualOnly,
    recommendedIds: feed.recommendedIds,
  });

  const { categories, categoriesLoading, categoryId, setCategoryId } = filters;
  const { authors, authorId, setAuthorId, virtualOnly, setVirtualOnly } = filters;
  const { filtersOpen, setFiltersOpen, filtersRef, activeCategoryName, activeAuthorName, activeFilterCount } = filters;
  const { orders, ownedVirtualIds, recentlyViewed } = feed;
  const { books, page, setPage, totalPages, totalElements, loading, error, retry, PAGE_SIZE } = search;

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
                      onClick={filters.clearAll}
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
        ) : error ? (
          <EmptyState
            icon={AlertCircle}
            title="Couldn't load books"
            description={error}
            action={
              <Button variant="secondary" size="sm" onClick={retry}>
                Try again
              </Button>
            }
          />
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
                <Button variant="secondary" size="sm" onClick={filters.clearAll}>
                  Clear filters
                </Button>
              ) : undefined
            }
          />
        ) : (
          <>
            <motion.div
              className={styles.grid}
              key={`${page}-${filters.debouncedCategoryId}-${authorId}-${virtualOnly}-${query}`}
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
