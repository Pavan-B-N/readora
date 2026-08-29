import { useEffect, useState, type ReactNode } from 'react';
import { motion } from 'framer-motion';
import { Bike, ClipboardList, Library, Store } from 'lucide-react';
import styles from './AuthLayout.module.css';

const MESSAGES = [
  { title: 'Run the show.', subtitle: 'Catalog, inventory, and orders — all in one console.' },
  { title: 'Keep deliveries moving.', subtitle: 'Track every order from checkout to doorstep.' },
  { title: 'Stay in control.', subtitle: 'Manage stores, delivery agents, and returns with confidence.' },
];

const MESSAGE_INTERVAL_MS = 3800;

/** The shared shell behind the admin login page — a messaging panel plus a slot for the form. */
export function AuthLayout({ children }: { children: ReactNode }) {
  const [messageIndex, setMessageIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => setMessageIndex((i) => (i + 1) % MESSAGES.length), MESSAGE_INTERVAL_MS);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className={styles.page}>
      <div className={styles.leftPanel}>
        <div className={styles.leftPanelGlow} aria-hidden="true" />

        <div className={styles.floatIcons} aria-hidden="true">
          <motion.span
            className={styles.floatIcon1}
            animate={{ y: [0, -14, 0] }}
            transition={{ duration: 5, repeat: Infinity, ease: 'easeInOut' }}
          >
            <ClipboardList size={22} />
          </motion.span>
          <motion.span
            className={styles.floatIcon2}
            animate={{ y: [0, 14, 0] }}
            transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
          >
            <Store size={28} />
          </motion.span>
          <motion.span
            className={styles.floatIcon3}
            animate={{ y: [0, -10, 0] }}
            transition={{ duration: 4.5, repeat: Infinity, ease: 'easeInOut' }}
          >
            <Bike size={20} />
          </motion.span>
          <motion.span
            className={styles.floatIcon4}
            animate={{ y: [0, 10, 0] }}
            transition={{ duration: 5.5, repeat: Infinity, ease: 'easeInOut' }}
          >
            <Library size={18} />
          </motion.span>
        </div>

        <span className={styles.brandMark}>
          <Library size={20} />
          Readora Admin
        </span>

        <div className={styles.messageArea}>
          {/* All three frames stay mounted and just crossfade via the `animate` prop — see the
              matching component in frontend-user for why AnimatePresence mount/unmount isn't used. */}
          {MESSAGES.map((m, i) => (
            <motion.div
              key={m.title}
              className={styles.messageSlide}
              animate={{ opacity: i === messageIndex ? 1 : 0 }}
              transition={{ duration: 0.5, ease: 'easeOut' }}
              style={{ pointerEvents: i === messageIndex ? 'auto' : 'none' }}
            >
              <h2 className={styles.messageTitle}>{m.title}</h2>
              <p className={styles.messageSubtitle}>{m.subtitle}</p>
            </motion.div>
          ))}

          <div className={styles.dots}>
            {MESSAGES.map((m, i) => (
              <span
                key={m.title}
                className={[styles.dot, i === messageIndex && styles.dotActive].filter(Boolean).join(' ')}
              />
            ))}
          </div>
        </div>

        <div className={styles.statsRow}>
          <div className={styles.stat}>
            <span className={styles.statValue}>10k+</span>
            <span className={styles.statLabel}>Books</span>
          </div>
          <div className={styles.stat}>
            <span className={styles.statValue}>21</span>
            <span className={styles.statLabel}>Stores</span>
          </div>
          <div className={styles.stat}>
            <span className={styles.statValue}>24/7</span>
            <span className={styles.statLabel}>Operations</span>
          </div>
        </div>
      </div>

      <div className={styles.rightPanel}>
        <div className={styles.formWrap}>{children}</div>
      </div>
    </div>
  );
}
