import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ChevronDown, Check, MapPin, Search } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { switchStore } from '@/redux/slices/storeSlice';
import { pickDefaultStore } from '@/utils/store';
import styles from './StoreSwitcher.module.css';

export function StoreSwitcher() {
  const dispatch = useAppDispatch();
  const { stores, selectedId, switching } = useAppSelector((state) => state.store);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const wrapperRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  useEffect(() => {
    if (open) {
      setQuery('');
      searchRef.current?.focus();
    }
  }, [open]);

  const current = stores.find((s) => s.id === selectedId) ?? pickDefaultStore(stores);
  if (!current) return null;

  const trimmedQuery = query.trim().toLowerCase();
  const filteredStores = trimmedQuery
    ? stores.filter(
        (store) =>
          store.name.toLowerCase().includes(trimmedQuery) ||
          store.city.toLowerCase().includes(trimmedQuery),
      )
    : stores;

  const onSelect = (storeId: string) => {
    setOpen(false);
    if (storeId === selectedId) return;
    dispatch(switchStore(storeId));
  };

  return (
    <div className={styles.wrapper} ref={wrapperRef}>
      <button
        type="button"
        className={styles.trigger}
        onClick={() => setOpen((o) => !o)}
        disabled={switching || stores.length === 0}
        aria-label="Switch delivery store"
      >
        <MapPin size={13} />
        <span className={styles.triggerText}>
          <span className={styles.triggerLabel}>Delivering from</span>
          <span className={styles.triggerStore}>{current.name}</span>
        </span>
        <ChevronDown size={13} />
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            className={styles.menu}
            initial={{ opacity: 0, y: -6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.98 }}
            transition={{ duration: 0.15 }}
          >
            <div className={styles.searchBox}>
              <Search size={13} className={styles.searchIcon} />
              <input
                ref={searchRef}
                type="text"
                className={styles.searchInput}
                placeholder="Search city or store..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
            <div className={styles.optionList}>
              {filteredStores.length === 0 && (
                <p className={styles.noResults}>No stores match &quot;{query}&quot;</p>
              )}
              {filteredStores.map((store) => (
                <button
                  type="button"
                  key={store.id}
                  className={styles.option}
                  onClick={() => onSelect(store.id)}
                >
                  <span className={styles.optionText}>
                    <span className={styles.optionName}>{store.name}</span>
                    <span className={styles.optionCity}>{store.city}</span>
                  </span>
                  {store.id === selectedId && <Check size={14} />}
                </button>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
