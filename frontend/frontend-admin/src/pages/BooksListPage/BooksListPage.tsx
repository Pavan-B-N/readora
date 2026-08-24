import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, BookOpen, X, ChevronLeft, ChevronRight, ImageOff } from 'lucide-react';
import { listBooks, getCategoryTree, listPublishers } from '@/api/catalogApi';
import type { BookSummary, Publisher } from '@/types/catalog';
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
  publisherId: string;
  format: string;
  minPrice: string;
  maxPrice: string;
}

const EMPTY_FILTERS: Filters = {
  q: '',
  categoryId: '',
  publisherId: '',
  format: '',
  minPrice: '',
  maxPrice: '',
};

export function BooksListPage() {
  const navigate = useNavigate();
  const [books, setBooks] = useState<BookSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);
  const debouncedFilters = useDebounced(filters);

  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [publishers, setPublishers] = useState<Publisher[]>([]);

  useEffect(() => {
    getCategoryTree().then((tree) => setCategories(flattenCategoryTree(tree)));
    listPublishers().then(setPublishers);
  }, []);

  useEffect(() => {
    setPage(0);
  }, [debouncedFilters]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    listBooks({
      page,
      size: PAGE_SIZE,
      q: debouncedFilters.q || undefined,
      categoryId: debouncedFilters.categoryId || undefined,
      publisherId: debouncedFilters.publisherId || undefined,
      format: debouncedFilters.format || undefined,
      minPrice: debouncedFilters.minPrice || undefined,
      maxPrice: debouncedFilters.maxPrice || undefined,
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
  }, [page, debouncedFilters]);

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
    if (filters.publisherId) {
      chips.push({
        key: 'publisherId',
        label: publishers.find((p) => p.id === filters.publisherId)?.name ?? 'Publisher',
      });
    }
    if (filters.format) chips.push({ key: 'format', label: filters.format });
    if (filters.minPrice) chips.push({ key: 'minPrice', label: `min ₹${filters.minPrice}` });
    if (filters.maxPrice) chips.push({ key: 'maxPrice', label: `max ₹${filters.maxPrice}` });
    return chips;
  }, [filters, categories, publishers]);

  return (
    <div>
      <PageHeader
        title="Books"
        subtitle="Manage the catalogue — stock levels, pricing, and virtual editions."
        actions={
          <Button onClick={() => navigate(ROUTES.newBook)}>
            <Plus size={15} />
            New book
          </Button>
        }
      />

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

        <Select label="Publisher" value={filters.publisherId} onChange={(e) => set({ publisherId: e.target.value })}>
          <option value="">All publishers</option>
          {publishers.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </Select>

        <Select label="Format" value={filters.format} onChange={(e) => set({ format: e.target.value })}>
          <option value="">All formats</option>
          <option value="HARDCOVER">Hardcover</option>
          <option value="PAPERBACK">Paperback</option>
          <option value="EBOOK">Ebook</option>
        </Select>

        <FieldWrapper label="Price range">
          <div className={styles.priceRange}>
            <input
              className={styles.priceInput}
              type="number"
              placeholder="Min"
              value={filters.minPrice}
              onChange={(e) => set({ minPrice: e.target.value })}
            />
            <span style={{ color: 'var(--color-text-subtle)' }}>–</span>
            <input
              className={styles.priceInput}
              type="number"
              placeholder="Max"
              value={filters.maxPrice}
              onChange={(e) => set({ maxPrice: e.target.value })}
            />
          </div>
        </FieldWrapper>
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
              <th>Format</th>
              <th>Price</th>
              <th>Availability</th>
            </tr>
          </thead>
          <tbody>
            {loading
              ? Array.from({ length: 6 }).map((_, i) => (
                  <tr className={styles.skeletonRow} key={i}>
                    {Array.from({ length: 5 }).map((__, j) => (
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
                    <td>{book.format}</td>
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

        {!loading && books.length === 0 && (
          <EmptyState
            icon={BookOpen}
            title="No books match these filters"
            description="Try widening the price range or clearing a filter."
            action={
              activeChips.length > 0 ? (
                <Button variant="secondary" size="sm" onClick={() => setFilters(EMPTY_FILTERS)}>
                  Clear filters
                </Button>
              ) : (
                <Button size="sm" onClick={() => navigate(ROUTES.newBook)}>
                  <Plus size={14} />
                  Add the first book
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
