import { useEffect, useState } from 'react';
import { Layers, Pencil, Plus, Trash2, Wand2 } from 'lucide-react';
import { createCategory, deleteCategory, getCategoryTree, updateCategory } from '@/api/catalogApi';
import { extractErrorMessage } from '@/api/client';
import type { CategoryNode } from '@/types/catalog';
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
import styles from './CategoriesPage.module.css';

export function CategoriesPage() {
  const { showToast } = useToast();
  const [categories, setCategories] = useState<CategoryNode[]>([]);
  const [loading, setLoading] = useState(true);

  const [open, setOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [slugTouched, setSlugTouched] = useState(false);
  const [displayOrder, setDisplayOrder] = useState('0');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

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

  const openCreateDialog = () => {
    setEditingId(null);
    setName('');
    setSlug('');
    setSlugTouched(false);
    setDisplayOrder('0');
    setErrors({});
    setOpen(true);
  };

  const openEditDialog = (category: CategoryNode) => {
    setEditingId(category.id);
    setName(category.name);
    setSlug(category.slug);
    setSlugTouched(true);
    setDisplayOrder(String(category.displayOrder));
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

    const payload = { name: name.trim(), slug: slug.trim(), displayOrder: Number(displayOrder) };
    setSaving(true);
    try {
      if (editingId) {
        await updateCategory(editingId, payload);
        showToast(`Category “${payload.name}” updated`);
      } else {
        await createCategory(payload);
        showToast(`Category “${payload.name}” created`);
      }
      setOpen(false);
      reload();
    } catch {
      showToast(
        editingId ? 'Failed to update category — the slug may already exist' : 'Failed to create category — the slug may already exist',
        'error',
      );
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async (category: CategoryNode) => {
    if (!window.confirm(`Delete "${category.name}"? This can't be undone.`)) return;
    setDeletingId(category.id);
    try {
      await deleteCategory(category.id);
      showToast(`Category “${category.name}” deleted`);
      reload();
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not delete this category'), 'error');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div>
      <PageHeader
        title="Categories"
        subtitle="Flat, deliberately not nested — every category sits at the same level."
        actions={
          <Button onClick={openCreateDialog}>
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
          <Spinner />
        ) : categories.length === 0 ? (
          <EmptyState
            icon={Layers}
            title="No categories yet"
            description="Create one, e.g. “Technology” or “Biography & Memoir”."
            action={
              <Button size="sm" onClick={openCreateDialog}>
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
                <span className={styles.itemActions}>
                  <Tooltip label="Edit">
                    <Button variant="ghost" size="sm" iconOnly aria-label="Edit category" onClick={() => openEditDialog(c)}>
                      <Pencil size={14} />
                    </Button>
                  </Tooltip>
                  <Tooltip label="Delete">
                    <Button
                      variant="ghost"
                      size="sm"
                      iconOnly
                      aria-label="Delete category"
                      onClick={() => onDelete(c)}
                      disabled={deletingId === c.id}
                    >
                      <Trash2 size={14} />
                    </Button>
                  </Tooltip>
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Modal open={open} onClose={() => setOpen(false)} title={editingId ? 'Edit category' : 'New category'}>
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
            {saving ? (editingId ? 'Saving…' : 'Creating…') : editingId ? 'Save changes' : 'Create category'}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
