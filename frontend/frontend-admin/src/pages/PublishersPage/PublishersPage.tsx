import { useEffect, useMemo, useState } from 'react';
import { Building2, Plus, Search, Wand2 } from 'lucide-react';
import { createPublisher, listPublishers } from '@/api/catalogApi';
import type { Publisher } from '@/types/catalog';
import { slugify } from '@/utils/slugify';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Input } from '@/components/Input';
import { Button } from '@/components/Button';
import { Tooltip } from '@/components/Tooltip';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@/components/EmptyState';
import styles from './PublishersPage.module.css';

export function PublishersPage() {
  const { showToast } = useToast();
  const [publishers, setPublishers] = useState<Publisher[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');

  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [slugTouched, setSlugTouched] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const reload = () => {
    setLoading(true);
    listPublishers()
      .then(setPublishers)
      .finally(() => setLoading(false));
  };

  useEffect(reload, []);

  useEffect(() => {
    if (!slugTouched) setSlug(slugify(name));
  }, [name, slugTouched]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return q ? publishers.filter((p) => p.name.toLowerCase().includes(q)) : publishers;
  }, [publishers, query]);

  const onSubmit = async () => {
    const next: Record<string, string> = {};
    if (!name.trim()) next.name = 'Name is required';
    if (!slug.trim()) next.slug = 'Slug is required';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSaving(true);
    try {
      await createPublisher({ name: name.trim(), slug: slug.trim() });
      showToast(`Publisher “${name.trim()}” created`);
      setName('');
      setSlug('');
      setSlugTouched(false);
      reload();
    } catch {
      showToast('Failed to create publisher — the slug may already exist', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader title="Publishers" subtitle={`${publishers.length} publisher${publishers.length === 1 ? '' : 's'} in the catalogue.`} />

      <div className={styles.layout}>
        <Card>
          <div className={styles.searchWrap}>
            <Search size={15} className={styles.searchIcon} />
            <input
              className={styles.searchInput}
              placeholder="Filter publishers…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>

          {loading ? (
            <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>
          ) : filtered.length === 0 ? (
            <EmptyState
              icon={Building2}
              title={query ? 'No matches' : 'No publishers yet'}
              description={query ? `Nothing matches “${query}”.` : 'Add the first publisher using the form.'}
            />
          ) : (
            <ul className={styles.list}>
              {filtered.map((p) => (
                <li className={styles.item} key={p.id}>
                  <span className={styles.itemIcon}>
                    <Building2 size={15} />
                  </span>
                  <span className={styles.itemText}>
                    <span className={styles.itemName}>{p.name}</span>
                    <span className={styles.itemSlug}>/{p.slug}</span>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardHeader title="New publisher" />
          <div className={styles.form}>
            <Input
              label="Name"
              required
              placeholder="e.g. Penguin Classics"
              value={name}
              error={errors.name}
              onChange={(e) => setName(e.target.value)}
            />
            <div className={styles.slugRow}>
              <Input
                label="Slug"
                required
                hint="URL segment"
                placeholder="penguin-classics"
                value={slug}
                error={errors.slug}
                onChange={(e) => {
                  setSlugTouched(true);
                  setSlug(e.target.value);
                }}
              />
              <Tooltip label="Regenerate from name">
                <Button
                  variant="secondary"
                  iconOnly
                  aria-label="Regenerate slug"
                  onClick={() => {
                    setSlugTouched(false);
                    setSlug(slugify(name));
                  }}
                >
                  <Wand2 size={15} />
                </Button>
              </Tooltip>
            </div>
            <Button onClick={onSubmit} disabled={saving} block>
              <Plus size={15} />
              {saving ? 'Creating…' : 'Create publisher'}
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
