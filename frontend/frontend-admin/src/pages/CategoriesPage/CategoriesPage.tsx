import { useEffect, useState } from 'react';
import { Layers, Plus, Wand2 } from 'lucide-react';
import { createCategory, getCategoryTree } from '@/api/catalogApi';
import type { CategoryNode } from '@/types/catalog';
import { slugify } from '@/utils/slugify';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input } from '@/components/Input';
import { Button } from '@/components/Button';
import { Tooltip } from '@/components/Tooltip';
import { Modal } from '@/components/Modal';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@/components/EmptyState';
import styles from './CategoriesPage.module.css';

export function CategoriesPage() {
  const { showToast } = useToast();
  const [categories, setCategories] = useState<CategoryNode[]>([]);
  const [loading, setLoading] = useState(true);

  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [slugTouched, setSlugTouched] = useState(false);
  const [displayOrder, setDisplayOrder] = useState('0');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const reload = () => {
    setLoading(true);
    getCategoryTree()
      .then(setCategories)
      .finally(() => setLoading(false));
  };

  useEffect(reload, []);

  // Auto-derive the slug from the name until the admin edits it themselves.
  useEffect(() => {
    if (!slugTouched) setSlug(slugify(name));
  }, [name, slugTouched]);

  const openDialog = () => {
    setName('');
    setSlug('');
    setSlugTouched(false);
    setDisplayOrder('0');
    setErrors({});
    setOpen(true);
  };

  const onSubmit = async () => {
    const next: Record<string, string> = {};
    if (!name.trim()) next.name = 'Name is required';
    if (!slug.trim()) next.slug = 'Slug is required';
    if (!/^\d+$/.test(displayOrder)) next.displayOrder = 'Whole number';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSaving(true);
    try {
      await createCategory({
        name: name.trim(),
        slug: slug.trim(),
        displayOrder: Number(displayOrder),
      });
      showToast(`Category “${name.trim()}” created`);
      setOpen(false);
      reload();
    } catch {
      showToast('Failed to create category — the slug may already exist', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Categories"
        subtitle="Flat, deliberately not nested — every category sits at the same level."
        actions={
          <Button onClick={openDialog}>
            <Plus size={15} />
            Add category
          </Button>
        }
      />

      <Card>
        <div className={styles.stats}>
          <div className={styles.stat}>
            <span className={styles.statValue}>{categories.length}</span>
            <span className={styles.statLabel}>Categories</span>
          </div>
        </div>

        {loading ? (
          <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>
        ) : categories.length === 0 ? (
          <EmptyState
            icon={Layers}
            title="No categories yet"
            description="Create one, e.g. “Technology” or “Biography & Memoir”."
            action={
              <Button size="sm" onClick={openDialog}>
                <Plus size={14} />
                Add category
              </Button>
            }
          />
        ) : (
          <ul className={styles.list}>
            {categories.map((c) => (
              <li className={styles.item} key={c.id}>
                <span className={styles.itemIcon}>
                  <Layers size={14} />
                </span>
                <span className={styles.itemName}>{c.name}</span>
                <span className={styles.itemSlug}>/{c.slug}</span>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Modal open={open} onClose={() => setOpen(false)} title="New category">
        <div className={styles.form}>
          <Input
            label="Name"
            required
            placeholder="e.g. Science Fiction & Fantasy"
            value={name}
            error={errors.name}
            onChange={(e) => setName(e.target.value)}
          />

          <div className={styles.slugRow}>
            <Input
              label="Slug"
              required
              hint="URL segment"
              placeholder="science-fiction-fantasy"
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
                aria-label="Regenerate slug from name"
                onClick={() => {
                  setSlugTouched(false);
                  setSlug(slugify(name));
                }}
              >
                <Wand2 size={15} />
              </Button>
            </Tooltip>
          </div>

          <Input
            label="Display order"
            hint="Lower shows first"
            value={displayOrder}
            error={errors.displayOrder}
            onChange={(e) => setDisplayOrder(e.target.value)}
          />

          <Button onClick={onSubmit} disabled={saving} block>
            <Plus size={15} />
            {saving ? 'Creating…' : 'Create category'}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
