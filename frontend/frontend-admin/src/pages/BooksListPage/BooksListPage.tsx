import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Plus, Search, BookOpen, Truck, Download, X, ChevronLeft, ChevronRight, ImageOff } from 'lucide-react';
import { listBooks, getCategoryTree } from '@/api/catalogApi';
import { getMe } from '@/api/userApi';
import type { BookSummary } from '@/types/catalog';
import { flattenCategoryTree, type FlatCategory } from '@/utils/flattenCategoryTree';
import { useDebounced } from '@/hooks/useDebounced';
import { Card } from '@/components/Card';
import { Button } from '@/components/Button';
import { Select, FieldWrapper } from '@/components/Input';
import { Badge } from '@/components/Badge';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@/components/EmptyState';
import { ROUTES } from '@/constants/routes';
import styles from './BooksListPage.module.css';

const PAGE_SIZE = 15;

interface Filters {
  q: string;
  categoryId: string;
}

const EMPTY_FILTERS: Filters = {
  q: '',
  categoryId: '',
};

export function BooksListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [books, setBooks] = useState<BookSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);
  const edition: 'physical' | 'virtual' = searchParams.get('tab') === 'virtual' ? 'virtual' : 'physical';
  const setEdition = (next: 'physical' | 'virtual') =>
    setSearchParams(
      (prev) => {
        const merged = new URLSearchParams(prev);
        merged.set('tab', next);
        return merged;
      },
      { replace: true },
    );
  const debouncedFilters = useDebounced(filters);

  const [categories, setCategories] = useState<FlatCategory[]>([]);
  // The physical catalogue is store-scoped (a customer shops one store at a time — see
  // catalog-service's STORE_ID_REQUIRED), so the physical tab needs the admin's own assigned
  // store before it can search at all. null-but-loaded means "no store assigned."
  const [adminStoreId, setAdminStoreId] = useState<string | null>(null);
  const [storeLoaded, setStoreLoaded] = useState(false);

  useEffect(() => {
    getCategoryTree().then((tree) => setCategories(flattenCategoryTree(tree)));
  }, []);

  useEffect(() => {
    getMe().then((me) => {
      setAdminStoreId(me.adminStoreId);
      setStoreLoaded(true);
    });
  }, []);

  // Keep the tab reflected in the URL even on a bare /books visit, so it's always shareable/refreshable.
  useEffect(() => {
    if (!searchParams.get('tab')) setEdition('physical');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    setPage(0);
  }, [debouncedFilters, edition]);

  useEffect(() => {
    // The physical tab can't search without a store — wait for getMe() to resolve rather than
    // firing a request that catalog-service will just reject with STORE_ID_REQUIRED. The virtual
    // tab ignores store entirely, so it doesn't need to wait.
    if (edition === 'physical' && !storeLoaded) return;

    // A genuinely unassigned admin has no physical catalogue to show — stop here rather than
    // repeating the same STORE_ID_REQUIRED request with an undefined storeId.
    if (edition === 'physical' && storeLoaded && !adminStoreId) {
      setBooks([]);
      setTotalPages(0);
      setTotalElements(0);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);

    listBooks({
      page,
      size: PAGE_SIZE,
      q: debouncedFilters.q || undefined,
      categoryId: debouncedFilters.categoryId || undefined,
      virtualOnly: edition === 'virtual',
      storeId: edition === 'physical' ? (adminStoreId ?? undefined) : undefined,
    })
      .then((result) => {
        if (cancelled) return;
        setBooks(result.items ?? []);
        setTotalPages(result.totalPages);
        setTotalElements(result.totalElements);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [page, debouncedFilters, edition, storeLoaded, adminStoreId]);

  const set = (patch: Partial<Filters>) => setFilters((f) => ({ ...f, ...patch }));

  const activeChips = useMemo(() => {
    const chips: { key: keyof Filters; label: string }[] = [];
    if (filters.q) chips.push({ key: 'q', label: `“${filters.q}”` });
    if (filters.categoryId) {
      chips.push({
        key: 'categoryId',
        label: categories.find((c) => c.id === filters.categoryId)?.label.replace(/^—\s*/, '') ?? 'Category',
      });
    }
    return chips;
  }, [filters, categories]);

  return (
    <div>
      <PageHeader
        title="Catalog"
        subtitle="Manage the catalogue — stock levels, pricing, and virtual editions."
        actions={
          edition === 'physical' ? (
            <Button onClick={() => navigate(ROUTES.newPhysicalBook)}>
              <Plus size={15} />
              New physical book
            </Button>
          ) : (
            <Button onClick={() => navigate(ROUTES.newVirtualBook)}>
              <Plus size={15} />
              New virtual edition
            </Button>
          )
        }
      />

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

      <div className={styles.filters}>
        <FieldWrapper label="Search">
          <div className={styles.searchWrap}>
            <Search size={15} className={styles.searchIcon} />
            <input
              className={styles.searchInput}
              placeholder="Title, author, ISBN…"
              value={filters.q}
              onChange={(e) => set({ q: e.target.value })}
            />
          </div>
        </FieldWrapper>

        <Select label="Category" value={filters.categoryId} onChange={(e) => set({ categoryId: e.target.value })}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.label}
            </option>
          ))}
        </Select>
      </div>

      {(activeChips.length > 0 || !loading) && (
        <div className={styles.activeFilters}>
          {activeChips.map((chip) => (
            <span className={styles.filterChip} key={chip.key}>
              {chip.label}
              <button
                type="button"
                aria-label={`Clear ${chip.key} filter`}
                onClick={() => set({ [chip.key]: '' } as Partial<Filters>)}
              >
                <X size={11} />
              </button>
            </span>
          ))}
          {activeChips.length > 0 && (
            <Button variant="ghost" size="sm" onClick={() => setFilters(EMPTY_FILTERS)}>
              Clear all
            </Button>
          )}
          <span className={styles.resultCount}>
            {loading ? 'Loading…' : `${totalElements} book${totalElements === 1 ? '' : 's'}`}
          </span>
        </div>
      )}

      <Card flush>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Title</th>
              <th>Authors</th>
              <th>Price</th>
              <th>Availability</th>
            </tr>
          </thead>
          <tbody>
            {loading
              ? Array.from({ length: 6 }).map((_, i) => (
                  <tr className={styles.skeletonRow} key={i}>
                    {Array.from({ length: 4 }).map((__, j) => (
                      <td key={j}>
                        <div className={styles.skeleton} style={{ width: j === 0 ? '70%' : '50%' }} />
                      </td>
                    ))}
                  </tr>
                ))
              : books.map((book) => (
                  <tr key={book.id} className={styles.row} onClick={() => navigate(ROUTES.editBook(book.id))}>
                    <td>
                      <div className={styles.titleCell}>
                        {book.coverImageUrl ? (
                          <img className={styles.cover} src={book.coverImageUrl} alt="" />
                        ) : (
                          <span className={styles.cover}>
                            <ImageOff size={13} />
                          </span>
                        )}
                        <span className={styles.titleText}>
                          <span className={styles.bookTitle}>{book.title}</span>
                          <span className={styles.bookMeta}>{book.isbn13}</span>
                        </span>
                      </div>
                    </td>
                    <td>{book.authors.join(', ') || '—'}</td>
                    <td className={styles.price}>
                      {book.listPrice} {book.currency}
                    </td>
                    <td>
                      <Badge variant={book.availability === 'IN_STOCK' ? 'success' : 'danger'} dot>
                        {book.availability === 'IN_STOCK' ? 'In stock' : 'Out of stock'}
                      </Badge>
                    </td>
                  </tr>
                ))}
          </tbody>
        </table>

        {!loading && edition === 'physical' && storeLoaded && !adminStoreId && (
          <EmptyState
            icon={BookOpen}
            title="No store assigned"
            description="Your admin account isn't assigned to a store, so there's no physical catalogue to show. Contact an owner to get one assigned."
          />
        )}

        {!loading && books.length === 0 && !(edition === 'physical' && storeLoaded && !adminStoreId) && (
          <EmptyState
            icon={BookOpen}
            title="No books match these filters"
            description="Try a different search, or clear the category filter."
            action={
              activeChips.length > 0 ? (
                <Button variant="secondary" size="sm" onClick={() => setFilters(EMPTY_FILTERS)}>
                  Clear filters
                </Button>
              ) : (
                <Button
                  size="sm"
                  onClick={() => navigate(edition === 'physical' ? ROUTES.newPhysicalBook : ROUTES.newVirtualBook)}
                >
                  <Plus size={14} />
                  {edition === 'physical' ? 'Add the first physical book' : 'Add the first virtual edition'}
                </Button>
              )
            }
          />
        )}

        {!loading && books.length > 0 && (
          <div className={styles.pagination}>
            <span>
              Page {page + 1} of {Math.max(totalPages, 1)}
            </span>
            <div className={styles.paginationActions}>
              <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                <ChevronLeft size={14} />
                Previous
              </Button>
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
          </div>
        )}
      </Card>
    </div>
  );
}
