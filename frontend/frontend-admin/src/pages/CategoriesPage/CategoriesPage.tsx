import { useEffect, useMemo, useState } from 'react';
import { ChevronDown, ChevronRight, Folder, FolderTree, Plus, Wand2 } from 'lucide-react';
import { createCategory, getCategoryTree } from '@/api/catalogApi';
import type { CategoryNode } from '@/types/catalog';
import { flattenCategoryTree } from '@/utils/flattenCategoryTree';
import { slugify } from '@/utils/slugify';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Input } from '@/components/Input';
import { Button } from '@/components/Button';
import { Combobox } from '@/components/Combobox';
import { Tooltip } from '@/components/Tooltip';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@/components/EmptyState';
import styles from './CategoriesPage.module.css';

function CategoryTree({ nodes }: { nodes: CategoryNode[] }) {
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});

  return (
    <ul className={styles.tree}>
      {nodes.map((node) => {
        const hasChildren = node.children.length > 0;
        const isCollapsed = collapsed[node.id];

        return (
          <li key={node.id}>
            <div className={styles.node}>
              {hasChildren ? (
                <button
                  type="button"
                  className={styles.chevron}
                  onClick={() => setCollapsed({ ...collapsed, [node.id]: !isCollapsed })}
                  aria-label={isCollapsed ? `Expand ${node.name}` : `Collapse ${node.name}`}
                >
                  {isCollapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
                </button>
              ) : (
                <span className={styles.chevronSpacer} />
              )}
              <span className={styles.folderIcon}>
                <Folder size={14} />
              </span>
              <span className={styles.nodeName}>{node.name}</span>
              <span className={styles.nodeSlug}>/{node.slug}</span>
              {hasChildren && (
                <span className={styles.nodeCount}>
                  {node.children.length} sub{node.children.length === 1 ? '' : 's'}
                </span>
              )}
            </div>
            {hasChildren && !isCollapsed && <CategoryTree nodes={node.children} />}
          </li>
        );
      })}
    </ul>
  );
}

export function CategoriesPage() {
  const { showToast } = useToast();
  const [tree, setTree] = useState<CategoryNode[]>([]);
  const [loading, setLoading] = useState(true);

  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [slugTouched, setSlugTouched] = useState(false);
  const [parentId, setParentId] = useState<string | null>(null);
  const [displayOrder, setDisplayOrder] = useState('0');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const flat = useMemo(() => flattenCategoryTree(tree), [tree]);
  const topLevelCount = tree.length;
  const totalCount = flat.length;

  const reload = () => {
    setLoading(true);
    getCategoryTree()
      .then(setTree)
      .finally(() => setLoading(false));
  };

  useEffect(reload, []);

  // Auto-derive the slug from the name until the admin edits it themselves.
  useEffect(() => {
    if (!slugTouched) setSlug(slugify(name));
  }, [name, slugTouched]);

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
        parentId,
        displayOrder: Number(displayOrder),
      });
      showToast(`Category “${name.trim()}” created`);
      setName('');
      setSlug('');
      setSlugTouched(false);
      setParentId(null);
      setDisplayOrder('0');
      reload();
    } catch {
      showToast('Failed to create category — the slug may already exist', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader title="Categories" subtitle="The browsing hierarchy customers use to find books." />

      <div className={styles.layout}>
        <Card>
          <div className={styles.stats}>
            <div className={styles.stat}>
              <span className={styles.statValue}>{totalCount}</span>
              <span className={styles.statLabel}>Total categories</span>
            </div>
            <div className={styles.stat}>
              <span className={styles.statValue}>{topLevelCount}</span>
              <span className={styles.statLabel}>Top level</span>
            </div>
            <div className={styles.stat}>
              <span className={styles.statValue}>{totalCount - topLevelCount}</span>
              <span className={styles.statLabel}>Nested</span>
            </div>
          </div>

          {loading ? (
            <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>
          ) : tree.length === 0 ? (
            <EmptyState
              icon={FolderTree}
              title="No categories yet"
              description="Create a top-level category like “Fiction”, then nest sub-categories under it."
            />
          ) : (
            <CategoryTree nodes={tree} />
          )}
        </Card>

        <Card>
          <CardHeader title="New category" subtitle="Nest it under a parent, or leave it top level." />

          <div className={styles.form}>
            <Input
              label="Name"
              required
              placeholder="e.g. Science Fiction"
              value={name}
              error={errors.name}
              onChange={(e) => setName(e.target.value)}
            />

            <div className={styles.slugRow}>
              <Input
                label="Slug"
                required
                hint="URL segment"
                placeholder="science-fiction"
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

            <Combobox
              label="Parent category"
              placeholder="Search — leave empty for top level"
              options={flat.map((c) => ({ value: c.id, label: c.name, meta: c.depth > 0 ? `level ${c.depth + 1}` : 'top' }))}
              value={parentId}
              onChange={setParentId}
            />

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
        </Card>
      </div>
    </div>
  );
}
