import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Check, Download, ImageOff, Pencil, Truck, X } from 'lucide-react';
import {
  createAuthor,
  getBookForEdit,
  getCategoryTree,
  listAuthors,
  listPublishers,
  updateBook,
} from '@/api/catalogApi';
import type { AdminBookDetail, Author, Publisher } from '@/types/catalog';
import { flattenCategoryTree, type FlatCategory } from '@/utils/flattenCategoryTree';
import { slugify } from '@/utils/slugify';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Badge } from '@/components/Badge';
import { Input, Textarea } from '@/components/Input';
import { Button } from '@/components/Button';
import { Combobox } from '@/components/Combobox';
import { PageHeader } from '@/components/PageHeader';
import { TocBuilder, tocSectionsToJson, jsonToTocSections, type TocSection } from '@/components/TocBuilder';
import { InventorySection } from '../BookFormPage/InventorySection';
import { VirtualEditionSection } from '../BookFormPage/VirtualEditionSection';
import { ReviewsSection } from '../BookFormPage/ReviewsSection';
import { ROUTES } from '@/constants/routes';
import styles from './BookDetailPage.module.css';

type Section = 'details' | 'classification' | 'pricing' | null;

interface FormState {
  title: string;
  subtitle: string;
  description: string;
  coverImageUrl: string;
  isActive: boolean;
  categoryId: string | null;
  publisherId: string | null;
  authorIds: string[];
  language: string;
  pageCount: string;
  publishedOn: string;
  listPrice: string;
  currency: string;
}

function detailToForm(detail: AdminBookDetail): FormState {
  return {
    title: detail.title,
    subtitle: detail.subtitle ?? '',
    description: detail.description ?? '',
    coverImageUrl: detail.coverImageUrl ?? '',
    isActive: detail.isActive,
    categoryId: detail.categoryId,
    publisherId: detail.publisherId,
    authorIds: detail.authorIds,
    language: detail.language ?? '',
    pageCount: detail.pageCount != null ? String(detail.pageCount) : '',
    publishedOn: detail.publishedOn ?? '',
    listPrice: String(detail.listPrice),
    currency: detail.currency,
  };
}

export function BookDetailPage() {
  const { bookId } = useParams<{ bookId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [detail, setDetail] = useState<AdminBookDetail | null>(null);
  const [form, setForm] = useState<FormState | null>(null);
  const [toc, setToc] = useState<TocSection[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<Section>(null);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [publishers, setPublishers] = useState<Publisher[]>([]);
  const [authors, setAuthors] = useState<Author[]>([]);
  const [creatingAuthor, setCreatingAuthor] = useState(false);

  const reload = () => {
    if (!bookId) return Promise.resolve();
    return getBookForEdit(bookId).then((result) => {
      setDetail(result);
      setForm(detailToForm(result));
      setToc(jsonToTocSections(result.tableOfContents));
    });
  };

  useEffect(() => {
    Promise.all([reload(), getCategoryTree(), listPublishers(), listAuthors()]).then(([, tree, pubs, auths]) => {
      setCategories(flattenCategoryTree(tree));
      setPublishers(pubs);
      setAuthors(auths);
      setLoading(false);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bookId]);

  const set = (patch: Partial<FormState>) => {
    setForm((f) => (f ? { ...f, ...patch } : f));
    setErrors((e) => {
      const next = { ...e };
      for (const key of Object.keys(patch)) delete next[key];
      return next;
    });
  };

  const handleCreateAuthor = async (name: string) => {
    setCreatingAuthor(true);
    try {
      const result = await createAuthor({ name, slug: slugify(name), bio: null, photoUrl: null });
      const newAuthor: Author = { id: result.id, name, slug: slugify(name), bio: null, photoUrl: null };
      setAuthors((prev) => [...prev, newAuthor]);
      set({ authorIds: [...(form?.authorIds ?? []), result.id] });
      showToast(`Author “${name}” added`);
    } catch {
      showToast('Failed to create author — the name may already exist', 'error');
    } finally {
      setCreatingAuthor(false);
    }
  };

  const categoryOptions = useMemo(
    () => categories.map((c) => ({ value: c.id, label: c.label.replace(/^(—\s*)+/, ''), meta: c.depth > 0 ? 'sub' : undefined })),
    [categories],
  );
  const publisherOptions = useMemo(() => publishers.map((p) => ({ value: p.id, label: p.name })), [publishers]);
  const authorOptions = useMemo(() => authors.map((a) => ({ value: a.id, label: a.name })), [authors]);

  const startEdit = (section: Section) => setEditing(section);

  const cancelEdit = () => {
    if (detail) {
      setForm(detailToForm(detail));
      setToc(jsonToTocSections(detail.tableOfContents));
    }
    setErrors({});
    setEditing(null);
  };

  const saveSection = async (section: Section) => {
    if (!bookId || !form) return;

    const next: Record<string, string> = {};
    if (section === 'details' && !form.title.trim()) next.title = 'Title is required';
    if (section === 'classification' && form.authorIds.length === 0) next.authorIds = 'Select at least one author';
    if (section === 'pricing') {
      if (!form.listPrice.trim()) next.listPrice = 'Price is required';
      else if (Number.isNaN(Number(form.listPrice))) next.listPrice = 'Price must be a number';
      if (form.currency.trim().length !== 3) next.currency = 'Use a 3-letter currency code';
      if (form.pageCount && !/^\d+$/.test(form.pageCount)) next.pageCount = 'Must be a whole number';
    }
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSaving(true);
    try {
      await updateBook(bookId, {
        title: form.title.trim(),
        subtitle: form.subtitle.trim() || null,
        description: form.description.trim() || null,
        tableOfContents: tocSectionsToJson(toc),
        categoryId: form.categoryId,
        publisherId: form.publisherId,
        authorIds: form.authorIds,
        language: form.language.trim() || null,
        pageCount: form.pageCount ? Number(form.pageCount) : null,
        publishedOn: form.publishedOn || null,
        listPrice: form.listPrice.trim(),
        currency: form.currency.trim().toUpperCase(),
        coverImageUrl: form.coverImageUrl.trim() || null,
        isActive: form.isActive,
      });
      showToast('Book updated');
      setEditing(null);
      await reload();
    } catch {
      showToast('Failed to save changes', 'error');
    } finally {
      setSaving(false);
    }
  };

  if (loading || !detail || !form) return <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>;

  const isPhysical = Boolean(detail.storeId);
  const isVirtual = Boolean(detail.virtualEdition?.isActive);
  const categoryLabel = categoryOptions.find((c) => c.value === form.categoryId)?.label ?? null;
  const publisherLabel = publisherOptions.find((p) => p.value === form.publisherId)?.label ?? null;
  const authorNames = form.authorIds.map((id) => authorOptions.find((a) => a.value === id)?.label).filter(Boolean).join(', ');

  return (
    <div className={styles.page}>
      <PageHeader
        title={detail.title}
        subtitle={`ISBN ${detail.isbn13}`}
        actions={
          <Button variant="secondary" onClick={() => navigate(ROUTES.books)}>
            <ArrowLeft size={15} />
            Back to catalog
          </Button>
        }
      />

      <div className={styles.hero}>
        {form.coverImageUrl ? (
          <img className={styles.cover} src={form.coverImageUrl} alt="" />
        ) : (
          <span className={styles.coverPlaceholder}>
            <ImageOff size={22} />
          </span>
        )}
        <div className={styles.heroBadges}>
          {isPhysical && (
            <Badge variant="neutral">
              <Truck size={11} />
              Physical
            </Badge>
          )}
          {isVirtual && (
            <Badge variant="neutral">
              <Download size={11} />
              Virtual
            </Badge>
          )}
          <Badge variant={detail.isActive ? 'success' : 'danger'} dot>
            {detail.isActive ? 'Active' : 'Inactive'}
          </Badge>
        </div>
      </div>

      <Card>
        <CardHeader
          title="Details"
          actions={
            editing === 'details' ? (
              <SectionActions saving={saving} onSave={() => saveSection('details')} onCancel={cancelEdit} />
            ) : (
              <EditButton onClick={() => startEdit('details')} />
            )
          }
        />

        {editing === 'details' ? (
          <div className={styles.form}>
            <div className={styles.row2}>
              <Input label="Title" required value={form.title} error={errors.title} onChange={(e) => set({ title: e.target.value })} />
              <Input label="Subtitle" value={form.subtitle} onChange={(e) => set({ subtitle: e.target.value })} />
            </div>
            <Textarea
              label="Description"
              hint="Also powers semantic search"
              rows={4}
              value={form.description}
              onChange={(e) => set({ description: e.target.value })}
            />
            <Input label="Cover image URL" value={form.coverImageUrl} onChange={(e) => set({ coverImageUrl: e.target.value })} />
            <label className={styles.toggleRow}>
              <input type="checkbox" checked={form.isActive} onChange={(e) => set({ isActive: e.target.checked })} />
              <span className={styles.toggleText}>
                <span className={styles.toggleLabel}>Active</span>
                <span className={styles.toggleHint}>Inactive books are hidden from customers and can't be purchased.</span>
              </span>
            </label>
          </div>
        ) : (
          <div className={styles.reviewGrid}>
            <ReviewItem label="Subtitle" value={form.subtitle || null} />
            <ReviewItem label="Description" value={form.description || null} />
            <ReviewItem label="Cover image URL" value={form.coverImageUrl || null} />
          </div>
        )}
      </Card>

      <Card>
        <CardHeader
          title="Classification"
          actions={
            editing === 'classification' ? (
              <SectionActions saving={saving} onSave={() => saveSection('classification')} onCancel={cancelEdit} />
            ) : (
              <EditButton onClick={() => startEdit('classification')} />
            )
          }
        />

        {editing === 'classification' ? (
          <div className={styles.form}>
            <div className={styles.row2}>
              <Combobox label="Category" placeholder="Search categories…" options={categoryOptions} value={form.categoryId} onChange={(v) => set({ categoryId: v })} />
              <Combobox label="Publisher" placeholder="Search publishers…" options={publisherOptions} value={form.publisherId} onChange={(v) => set({ publisherId: v })} />
            </div>
            <Combobox
              multiple
              label="Authors"
              required
              hint="Type a new name to add an author that isn't in the catalogue yet"
              placeholder="Search authors…"
              options={authorOptions}
              value={form.authorIds}
              error={errors.authorIds}
              onChange={(v) => set({ authorIds: v })}
              onCreate={handleCreateAuthor}
              creating={creatingAuthor}
            />
          </div>
        ) : (
          <div className={styles.reviewGrid}>
            <ReviewItem label="Category" value={categoryLabel} />
            <ReviewItem label="Publisher" value={publisherLabel} />
            <ReviewItem label="Authors" value={authorNames || null} />
          </div>
        )}
      </Card>

      {isPhysical && (
        <Card>
          <CardHeader
            title="Pricing & contents"
            subtitle="Physical-edition specific — hidden for virtual-only titles."
            actions={
              editing === 'pricing' ? (
                <SectionActions saving={saving} onSave={() => saveSection('pricing')} onCancel={cancelEdit} />
              ) : (
                <EditButton onClick={() => startEdit('pricing')} />
              )
            }
          />

          {editing === 'pricing' ? (
            <div className={styles.form}>
              <div className={styles.row3}>
                <Input label="List price" required value={form.listPrice} error={errors.listPrice} onChange={(e) => set({ listPrice: e.target.value })} />
                <Input label="Currency" required value={form.currency} error={errors.currency} onChange={(e) => set({ currency: e.target.value })} />
                <Input label="Language" value={form.language} onChange={(e) => set({ language: e.target.value })} />
              </div>
              <div className={styles.row3}>
                <Input label="Page count" value={form.pageCount} error={errors.pageCount} onChange={(e) => set({ pageCount: e.target.value })} />
                <Input label="Published on" type="date" value={form.publishedOn} onChange={(e) => set({ publishedOn: e.target.value })} />
              </div>
              <div>
                <span className={styles.tocLabel}>
                  Table of contents
                  <span className={styles.tocLabelHint}> — optional, improves search quality for non-fiction</span>
                </span>
                <TocBuilder value={toc} onChange={setToc} />
              </div>
            </div>
          ) : (
            <div className={styles.reviewGrid}>
              <ReviewItem label="Price" value={`${form.listPrice} ${form.currency}`} />
              <ReviewItem label="Language" value={form.language || null} />
              <ReviewItem label="Page count" value={form.pageCount || null} />
              <ReviewItem label="Published on" value={form.publishedOn || null} />
              <ReviewItem label="Contents sections" value={toc.length ? String(toc.length) : null} />
            </div>
          )}
        </Card>
      )}

      {isPhysical && <InventorySection bookId={bookId!} inventory={detail.inventory} />}

      <VirtualEditionSection bookId={bookId!} virtualEdition={detail.virtualEdition} onChanged={reload} />

      <ReviewsSection bookId={bookId!} />

      <Card>
        <CardHeader title="Audit" subtitle="Who listed this book, and whether its search index is current." />
        <div className={styles.reviewGrid}>
          <ReviewItem label="Added on" value={new Date(detail.createdAt).toLocaleString()} />
          <ReviewItem label="Added by" value={detail.createdByUserId ? `${detail.createdByUserId.slice(0, 8)}…` : null} />
          <div className={styles.reviewItem}>
            <span className={styles.reviewLabel}>Search index</span>
            <span className={styles.reviewValue}>
              <Badge variant={detail.needsReembedding ? 'warning' : 'success'} dot>
                {detail.needsReembedding ? 'Needs re-embedding' : 'Up to date'}
              </Badge>
            </span>
          </div>
          <ReviewItem label="Last embedded" value={detail.embeddedAt ? new Date(detail.embeddedAt).toLocaleString() : 'Never'} />
        </div>
      </Card>
    </div>
  );
}

function EditButton({ onClick }: { onClick: () => void }) {
  return (
    <Button variant="ghost" size="sm" iconOnly aria-label="Edit section" onClick={onClick}>
      <Pencil size={14} />
    </Button>
  );
}

function SectionActions({ saving, onSave, onCancel }: { saving: boolean; onSave: () => void; onCancel: () => void }) {
  return (
    <div className={styles.sectionActions}>
      <Button variant="ghost" size="sm" iconOnly aria-label="Cancel" onClick={onCancel} disabled={saving}>
        <X size={14} />
      </Button>
      <Button size="sm" iconOnly aria-label="Save" onClick={onSave} disabled={saving}>
        <Check size={14} />
      </Button>
    </div>
  );
}

function ReviewItem({ label, value }: { label: string; value: string | null }) {
  return (
    <div className={styles.reviewItem}>
      <span className={styles.reviewLabel}>{label}</span>
      <span className={[styles.reviewValue, !value && styles.reviewEmpty].filter(Boolean).join(' ')}>
        {value || 'Not set'}
      </span>
    </div>
  );
}
