import { useEffect, useState, type ReactNode } from 'react';
import { motion } from 'framer-motion';
import { BookOpen, Compass, GraduationCap, Sparkles } from 'lucide-react';
import styles from './AuthLayout.module.css';

const MESSAGES = [
  { title: 'Grow your knowledge.', subtitle: 'Every book you open plants a seed for tomorrow.' },
  { title: 'Never stop learning.', subtitle: 'Curiosity is the one habit worth keeping for life.' },
  { title: 'One page at a time.', subtitle: 'Small daily reading adds up to a lifetime of insight.' },
];

const MESSAGE_INTERVAL_MS = 3800;

/** The shared shell behind login/register — a messaging panel plus a slot for each page's own form. */
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
            <Sparkles size={22} />
          </motion.span>
          <motion.span
            className={styles.floatIcon2}
            animate={{ y: [0, 14, 0] }}
            transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
          >
            <GraduationCap size={28} />
          </motion.span>
          <motion.span
            className={styles.floatIcon3}
            animate={{ y: [0, -10, 0] }}
            transition={{ duration: 4.5, repeat: Infinity, ease: 'easeInOut' }}
          >
            <Compass size={20} />
          </motion.span>
          <motion.span
            className={styles.floatIcon4}
            animate={{ y: [0, 10, 0] }}
            transition={{ duration: 5.5, repeat: Infinity, ease: 'easeInOut' }}
          >
            <BookOpen size={18} />
          </motion.span>
        </div>

        <span className={styles.brandMark}>
          <BookOpen size={20} />
          Readora
        </span>

        <div className={styles.messageArea}>
          {/* All three frames stay mounted and just crossfade via the `animate` prop, rather than
              the more obvious AnimatePresence mount/unmount-per-key approach — that relies on
              AnimatePresence's exit-completion tracking to remove the outgoing frame, which React
              18 StrictMode's double-invoked effects can leave desynced in dev (the frame never
              formally "exits", so it lingers at its initial pre-animation state instead of
              fading out). Nothing here is ever added or removed, so there's no exit lifecycle to
              get stuck on. */}
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
            <span className={styles.statValue}>~30 min</span>
            <span className={styles.statLabel}>Delivery</span>
          </div>
          <div className={styles.stat}>
            <span className={styles.statValue}>24/7</span>
            <span className={styles.statLabel}>AI assistant</span>
          </div>
        </div>
      </div>

      <div className={styles.rightPanel}>
        <div className={styles.formWrap}>{children}</div>
      </div>
    </div>
  );
}
