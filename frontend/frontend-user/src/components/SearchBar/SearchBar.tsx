import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { BookOpen, Search } from 'lucide-react';
import { suggestBooks } from '@/api/catalogApi';
import { useDebounced } from '@/hooks/useDebounced';
import { useAppSelector } from '@/redux/hooks';
import type { BookSuggestion } from '@/types/catalog';
import { ROUTES } from '@/constants/routes';
import styles from './SearchBar.module.css';

const MIN_QUERY_LENGTH = 2;

export function SearchBar() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [query, setQuery] = useState(searchParams.get('q') ?? '');
  const [suggestions, setSuggestions] = useState<BookSuggestion[]>([]);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const debouncedQuery = useDebounced(query, 250);
  const { selectedId: storeId, resolved: storeResolved } = useAppSelector((state) => state.store);

  useEffect(() => {
    const trimmed = debouncedQuery.trim();
    if (trimmed.length < MIN_QUERY_LENGTH || !storeResolved) {
      setSuggestions([]);
      return;
    }
    let cancelled = false;
    suggestBooks(trimmed, 8, storeId ?? undefined).then((results) => {
      if (!cancelled) setSuggestions(results);
    });
    return () => {
      cancelled = true;
    };
  }, [debouncedQuery, storeId, storeResolved]);

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const goToBook = (book: BookSuggestion) => {
    setOpen(false);
    setQuery('');
    navigate(ROUTES.bookDetail(book.id));
  };

  const goToSearchResults = (q: string) => {
    setOpen(false);
    navigate(q ? `${ROUTES.home}?q=${encodeURIComponent(q)}` : ROUTES.home);
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (activeIndex >= 0 && suggestions[activeIndex]) {
      goToBook(suggestions[activeIndex]);
    } else {
      goToSearchResults(query);
    }
  };

  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (!open || suggestions.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex((i) => Math.min(i + 1, suggestions.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex((i) => Math.max(i - 1, -1));
    } else if (e.key === 'Escape') {
      setOpen(false);
      setActiveIndex(-1);
    }
  };

  return (
    <div className={styles.wrapper} ref={wrapperRef}>
      <form className={styles.searchForm} onSubmit={onSubmit} role="search">
        <Search size={15} className={styles.searchIcon} />
        <input
          className={styles.searchInput}
          placeholder="Search books, authors, topics…"
          aria-label="Search books"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
            setActiveIndex(-1);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={onKeyDown}
        />
      </form>

      <AnimatePresence>
        {open && suggestions.length > 0 && (
          <motion.div
            className={styles.dropdown}
            initial={{ opacity: 0, y: -6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.98 }}
            transition={{ duration: 0.12 }}
          >
            {suggestions.map((book, i) => (
              <button
                type="button"
                key={book.id}
                className={[styles.suggestion, i === activeIndex && styles.suggestionActive].filter(Boolean).join(' ')}
                onMouseEnter={() => setActiveIndex(i)}
                onClick={() => goToBook(book)}
              >
                <span className={styles.suggestionCover}>
                  {book.coverImageUrl ? <img src={book.coverImageUrl} alt="" /> : <BookOpen size={14} />}
                </span>
                <span className={styles.suggestionText}>
                  <span className={styles.suggestionTitle}>{book.title}</span>
                  <span className={styles.suggestionAuthors}>{book.authors.join(', ') || 'Unknown author'}</span>
                </span>
                <span className={styles.suggestionPrice}>₹{book.listPrice}</span>
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
