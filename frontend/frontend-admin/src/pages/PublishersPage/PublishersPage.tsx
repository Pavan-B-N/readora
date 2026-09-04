import { useEffect, useMemo, useState } from 'react';
import { Building2, Plus, Search, Wand2 } from 'lucide-react';
import { createPublisher, listPublishers } from '@/api/catalogApi';
import type { Publisher } from '@/types/catalog';
import { slugify } from '@/utils/slugify';
import { useToast } from '@readora/shared-ui';
import { Card } from '@readora/shared-ui';
import { Input } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { Tooltip } from '@readora/shared-ui';
import { Modal } from '@readora/shared-ui';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@readora/shared-ui';
import { Spinner } from '@readora/shared-ui';
import styles from './PublishersPage.module.css';

export function PublishersPage() {
  const { showToast } = useToast();
  const [publishers, setPublishers] = useState<Publisher[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');

  const [open, setOpen] = useState(false);
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

  const openDialog = () => {
    setName('');
    setSlug('');
    setSlugTouched(false);
    setErrors({});
    setOpen(true);
  };

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
      setOpen(false);
      reload();
    } catch {
      showToast('Failed to create publisher — the slug may already exist', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Publishers"
        subtitle={`${publishers.length} publisher${publishers.length === 1 ? '' : 's'} in the catalogue.`}
        actions={
          <Button onClick={openDialog}>
            <Plus size={15} />
            Add publisher
          </Button>
        }
      />

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
        <Spinner />
      ) : filtered.length === 0 ? (
        <Card>
          <EmptyState
            icon={Building2}
            title={query ? 'No matches' : 'No publishers yet'}
            description={query ? `Nothing matches “${query}”.` : 'Add the first publisher to get started.'}
            action={
              !query ? (
                <Button size="sm" onClick={openDialog}>
                  <Plus size={14} />
                  Add publisher
                </Button>
              ) : undefined
            }
          />
        </Card>
      ) : (
        <div className={styles.grid}>
          {filtered.map((p) => (
            <Card key={p.id} className={styles.publisherCard}>
              <span className={styles.publisherIcon}>
                <Building2 size={20} />
              </span>
              <span className={styles.publisherName}>{p.name}</span>
              <span className={styles.publisherSlug}>/{p.slug}</span>
            </Card>
          ))}
        </div>
      )}

      <Modal open={open} onClose={() => setOpen(false)} title="New publisher">
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
      </Modal>
    </div>
  );
}
