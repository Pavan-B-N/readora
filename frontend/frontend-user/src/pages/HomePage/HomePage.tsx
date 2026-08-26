import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { BookOpen, Download, Sparkles, Truck, X, ChevronLeft, ChevronRight } from 'lucide-react';
import { searchBooks, getCategoryTree, getRecommendations } from '@/api/catalogApi';
import type { BookSummary, CategoryNode } from '@/types/catalog';
import { useDebounced } from '@/hooks/useDebounced';
import { useAppSelector } from '@/redux/hooks';
import { BookCard } from '@/components/BookCard';
import { Button } from '@/components/Button';
import { Select } from '@/components/Input';
import { EmptyState } from '@/components/EmptyState';
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

export function HomePage() {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') ?? '';
  const accessToken = useAppSelector((state) => state.auth.accessToken);

  const [books, setBooks] = useState<BookSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [categoryId, setCategoryId] = useState('');
  const [format, setFormat] = useState('');
  const [priceBand, setPriceBand] = useState('');
  const [sort, setSort] = useState('relevance');
  const [edition, setEdition] = useState<'physical' | 'virtual'>('physical');
  const [recommendations, setRecommendations] = useState<BookSummary[]>([]);

  const filters = useMemo(
    () => ({ categoryId, format, priceBand, edition }),
    [categoryId, format, priceBand, edition],
  );
  const debouncedFilters = useDebounced(filters, 150);

  useEffect(() => {
    getCategoryTree().then((tree) => setCategories(flatten(tree)));
  }, []);

  useEffect(() => {
    if (!accessToken) {
      setRecommendations([]);
      return;
    }
    getRecommendations().then(setRecommendations);
  }, [accessToken]);

  useEffect(() => {
    setPage(0);
  }, [query, debouncedFilters]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    const [minPrice, maxPrice] = debouncedFilters.priceBand
      ? debouncedFilters.priceBand.split('-')
      : ['', ''];

    searchBooks({
      q: query || undefined,
      categoryId: debouncedFilters.categoryId || undefined,
      format: debouncedFilters.format || undefined,
      minPrice: minPrice || undefined,
      maxPrice: maxPrice || undefined,
      virtualOnly: debouncedFilters.edition === 'virtual',
      page,
      size: PAGE_SIZE,
    })
      .then((result) => {
        if (cancelled) return;
        const items = result.items ?? [];
        setBooks(
          sort === 'price-asc'
            ? [...items].sort((a, b) => Number(a.listPrice) - Number(b.listPrice))
            : sort === 'price-desc'
              ? [...items].sort((a, b) => Number(b.listPrice) - Number(a.listPrice))
              : sort === 'title'
                ? [...items].sort((a, b) => a.title.localeCompare(b.title))
                : items,
        );
        setTotalPages(result.totalPages);
        setTotalElements(result.totalElements);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [query, page, debouncedFilters, sort]);

  const activeCategoryName = categories.find((c) => c.id === categoryId)?.name;
  const hasFilters = Boolean(categoryId || format || priceBand);

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

      <div>
        {!query && !categoryId && recommendations.length > 0 && (
          <div className={styles.recommendations}>
            <div className={styles.recommendationsHeading}>
              <Sparkles size={15} />
              <h2 className={styles.recommendationsTitle}>Recommended for you</h2>
            </div>
            <div className={styles.recommendationsRow}>
              {recommendations.map((book) => (
                <div className={styles.recommendationCard} key={book.id}>
                  <BookCard book={book} />
                </div>
              ))}
            </div>
          </div>
        )}

        <div className={styles.heading}>
          <h1 className={styles.title}>
            {query ? `Results for “${query}”` : activeCategoryName ? activeCategoryName : 'Browse books'}
          </h1>
          <p className={styles.subtitle}>Physical and virtual editions, all in one place.</p>
        </div>

        <div className={styles.editionTabs} role="tablist" aria-label="Edition type">
          <button
            type="button"
            role="tab"
            aria-selected={edition === 'physical'}
            className={[styles.editionTab, edition === 'physical' && styles.editionTabActive].filter(Boolean).join(' ')}
            onClick={() => setEdition('physical')}
          >
            <Truck size={14} />
            Physical
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={edition === 'virtual'}
            className={[styles.editionTab, edition === 'virtual' && styles.editionTabActive].filter(Boolean).join(' ')}
            onClick={() => setEdition('virtual')}
          >
            <Download size={14} />
            Virtual editions
          </button>
        </div>

        <div className={styles.filterBar}>
          <Select label="Format" value={format} onChange={(e) => setFormat(e.target.value)}>
            <option value="">All formats</option>
            <option value="HARDCOVER">Hardcover</option>
            <option value="PAPERBACK">Paperback</option>
            <option value="EBOOK">Ebook</option>
          </Select>

          <Select label="Price range" value={priceBand} onChange={(e) => setPriceBand(e.target.value)}>
            <option value="">Any price</option>
            <option value="-299">Under ₹299</option>
            <option value="300-599">₹300 – ₹599</option>
            <option value="600-999">₹600 – ₹999</option>
            <option value="1000-">₹1000 and above</option>
          </Select>

          <Select label="Sort by" value={sort} onChange={(e) => setSort(e.target.value)}>
            <option value="relevance">Relevance</option>
            <option value="price-asc">Price: low to high</option>
            <option value="price-desc">Price: high to low</option>
            <option value="title">Title A–Z</option>
          </Select>
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
            {format && (
              <span className={styles.chip}>
                {format}
                <button type="button" onClick={() => setFormat('')} aria-label="Clear format filter">
                  <X size={11} />
                </button>
              </span>
            )}
            {priceBand && (
              <span className={styles.chip}>
                {priceBand.startsWith('-')
                  ? `Under ₹${priceBand.slice(1)}`
                  : priceBand.endsWith('-')
                    ? `₹${priceBand.slice(0, -1)}+`
                    : `₹${priceBand.replace('-', ' – ₹')}`}
                <button type="button" onClick={() => setPriceBand('')} aria-label="Clear price filter">
                  <X size={11} />
                </button>
              </span>
            )}
            {hasFilters && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setCategoryId('');
                  setFormat('');
                  setPriceBand('');
                }}
              >
                Clear all
              </Button>
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
              hasFilters ? (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setCategoryId('');
                    setFormat('');
                    setPriceBand('');
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
              key={`${page}-${edition}-${debouncedFilters.categoryId}-${debouncedFilters.format}-${debouncedFilters.priceBand}-${sort}-${query}`}
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
