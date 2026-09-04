import { useEffect, useState, type ReactNode } from 'react';
import { motion } from 'framer-motion';
import type { LucideIcon } from 'lucide-react';
import styles from './AuthLayout.module.css';

const MESSAGE_INTERVAL_MS = 3800;

export interface AuthLayoutMessage {
  title: string;
  subtitle: string;
}

export interface AuthLayoutStat {
  value: string;
  label: string;
}

interface AuthLayoutProps {
  children: ReactNode;
  brandIcon: LucideIcon;
  brandName: string;
  /** The four decorative floating icons, largest (28px) to smallest (18px) at fixed positions. */
  floatIcons: [LucideIcon, LucideIcon, LucideIcon, LucideIcon];
  messages: AuthLayoutMessage[];
  stats: AuthLayoutStat[];
}

/** The shared shell behind every app's login/register pages — a messaging panel plus a slot for the form. */
export function AuthLayout({ children, brandIcon: BrandIcon, brandName, floatIcons, messages, stats }: AuthLayoutProps) {
  const [FloatIcon1, FloatIcon2, FloatIcon3, FloatIcon4] = floatIcons;
  const [messageIndex, setMessageIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => setMessageIndex((i) => (i + 1) % messages.length), MESSAGE_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [messages.length]);

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
            <FloatIcon1 size={22} />
          </motion.span>
          <motion.span
            className={styles.floatIcon2}
            animate={{ y: [0, 14, 0] }}
            transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
          >
            <FloatIcon2 size={28} />
          </motion.span>
          <motion.span
            className={styles.floatIcon3}
            animate={{ y: [0, -10, 0] }}
            transition={{ duration: 4.5, repeat: Infinity, ease: 'easeInOut' }}
          >
            <FloatIcon3 size={20} />
          </motion.span>
          <motion.span
            className={styles.floatIcon4}
            animate={{ y: [0, 10, 0] }}
            transition={{ duration: 5.5, repeat: Infinity, ease: 'easeInOut' }}
          >
            <FloatIcon4 size={18} />
          </motion.span>
        </div>

        <span className={styles.brandMark}>
          <BrandIcon size={20} />
          {brandName}
        </span>

        <div className={styles.messageArea}>
          {/* All frames stay mounted and just crossfade via the `animate` prop, rather than the
              more obvious AnimatePresence mount/unmount-per-key approach — that relies on
              AnimatePresence's exit-completion tracking to remove the outgoing frame, which React
              18 StrictMode's double-invoked effects can leave desynced in dev (the frame never
              formally "exits", so it lingers at its initial pre-animation state instead of
              fading out). Nothing here is ever added or removed, so there's no exit lifecycle to
              get stuck on. */}
          {messages.map((m, i) => (
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
            {messages.map((m, i) => (
              <span
                key={m.title}
                className={[styles.dot, i === messageIndex && styles.dotActive].filter(Boolean).join(' ')}
              />
            ))}
          </div>
        </div>

        <div className={styles.statsRow}>
          {stats.map((s) => (
            <div className={styles.stat} key={s.label}>
              <span className={styles.statValue}>{s.value}</span>
              <span className={styles.statLabel}>{s.label}</span>
            </div>
          ))}
        </div>
      </div>

      <div className={styles.rightPanel}>
        <div className={styles.formWrap}>{children}</div>
      </div>
    </div>
  );
}
