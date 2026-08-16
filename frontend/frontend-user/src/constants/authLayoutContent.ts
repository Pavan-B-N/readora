import { BookOpen, Compass, GraduationCap, Sparkles } from 'lucide-react';
import type { AuthLayoutMessage, AuthLayoutStat } from '@readora/shared-ui';

export const AUTH_BRAND_ICON = BookOpen;
export const AUTH_BRAND_NAME = 'Readora';
export const AUTH_FLOAT_ICONS: [typeof Sparkles, typeof GraduationCap, typeof Compass, typeof BookOpen] = [
  Sparkles,
  GraduationCap,
  Compass,
  BookOpen,
];

export const AUTH_MESSAGES: AuthLayoutMessage[] = [
  { title: 'Grow your knowledge.', subtitle: 'Every book you open plants a seed for tomorrow.' },
  { title: 'Never stop learning.', subtitle: 'Curiosity is the one habit worth keeping for life.' },
  { title: 'One page at a time.', subtitle: 'Small daily reading adds up to a lifetime of insight.' },
];

export const AUTH_STATS: AuthLayoutStat[] = [
  { value: '10k+', label: 'Books' },
  { value: '~30 min', label: 'Delivery' },
  { value: '24/7', label: 'AI assistant' },
];
