import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ChevronDown, Check, MapPin } from 'lucide-react';
import { listStores } from '@/api/catalogApi';
import { getMe, updateProfile } from '@/api/userApi';
import { useAppSelector } from '@/redux/hooks';
import type { Store } from '@/types/catalog';
import styles from './StoreSwitcher.module.css';

export function StoreSwitcher() {
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const [stores, setStores] = useState<Store[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [switching, setSwitching] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    listStores().then(setStores);
  }, []);

  useEffect(() => {
    if (!accessToken) {
      setSelectedId(null);
      return;
    }
    getMe().then((me) => setSelectedId(me.preferredStoreId));
  }, [accessToken]);

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const current = stores.find((s) => s.id === selectedId) ?? stores[0];
  if (!current) return null;

  const onSelect = async (storeId: string) => {
    setOpen(false);
    if (storeId === selectedId || !accessToken) return;
    setSwitching(true);
    try {
      await updateProfile({ preferredStoreId: storeId });
      setSelectedId(storeId);
    } finally {
      setSwitching(false);
    }
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
            {stores.map((store) => (
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
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
