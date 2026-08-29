import { useEffect, useState, type ReactNode } from 'react';
import { motion } from 'framer-motion';
import { Bike, MapPin, Package, Wallet } from 'lucide-react';
import styles from './AuthLayout.module.css';

const MESSAGES = [
  { title: 'Deliver, earn, repeat.', subtitle: 'Claim orders at your store and get moving in seconds.' },
  { title: 'Know before you go.', subtitle: 'Customer, address, and items — all up front, before you accept.' },
  { title: 'Every job counts.', subtitle: 'Track your earnings and completed jobs from one profile.' },
];

const MESSAGE_INTERVAL_MS = 3800;

/** The shared shell behind the delivery agent's login page — matches frontend-admin's AuthLayout. */
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
            <Package size={22} />
          </motion.span>
          <motion.span
            className={styles.floatIcon2}
            animate={{ y: [0, 14, 0] }}
            transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
          >
            <MapPin size={26} />
          </motion.span>
          <motion.span
            className={styles.floatIcon3}
            animate={{ y: [0, -10, 0] }}
            transition={{ duration: 4.5, repeat: Infinity, ease: 'easeInOut' }}
          >
            <Bike size={22} />
          </motion.span>
          <motion.span
            className={styles.floatIcon4}
            animate={{ y: [0, 10, 0] }}
            transition={{ duration: 5.5, repeat: Infinity, ease: 'easeInOut' }}
          >
            <Wallet size={18} />
          </motion.span>
        </div>

        <span className={styles.brandMark}>
          <Bike size={20} />
          Readora Delivery
        </span>

        <div className={styles.messageArea}>
          {/* All three frames stay mounted and just crossfade via the `animate` prop — matches
              the identical component in frontend-admin/frontend-user. */}
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
            <span className={styles.statValue}>₹40</span>
            <span className={styles.statLabel}>Per job</span>
          </div>
          <div className={styles.stat}>
            <span className={styles.statValue}>~30 min</span>
            <span className={styles.statLabel}>Avg delivery</span>
          </div>
          <div className={styles.stat}>
            <span className={styles.statValue}>24/7</span>
            <span className={styles.statLabel}>Go on duty</span>
          </div>
        </div>
      </div>

      <div className={styles.rightPanel}>
        <div className={styles.formWrap}>{children}</div>
      </div>
    </div>
  );
}
