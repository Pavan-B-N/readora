import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Check, Info } from 'lucide-react';
import {
  createBook,
  getBookForEdit,
  getCategoryTree,
  listAuthors,
  listPublishers,
  updateBook,
} from '@/api/catalogApi';
import type { AdminBookDetail, Author, BookFormat, Publisher } from '@/types/catalog';
import { flattenCategoryTree, type FlatCategory } from '@/utils/flattenCategoryTree';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input, Select, Textarea } from '@/components/Input';
import { Button } from '@/components/Button';
import { Combobox } from '@/components/Combobox';
import { Stepper, type Step } from '@/components/Stepper';
import { PageHeader } from '@/components/PageHeader';
import { TocBuilder, tocSectionsToJson, jsonToTocSections, type TocSection } from '@/components/TocBuilder';
import { InventorySection } from './InventorySection';
import { VirtualEditionSection } from './VirtualEditionSection';
import { ROUTES } from '@/constants/routes';
import styles from './BookFormPage.module.css';

const STEPS: Step[] = [
  { label: 'Details', description: 'Title & identity' },
  { label: 'Classification', description: 'Category & authors' },
  { label: 'Contents', description: 'Format & topics' },
];

interface FormState {
  isbn13: string;
  title: string;
  subtitle: string;
  description: string;
  categoryId: string | null;
  publisherId: string | null;
  authorIds: string[];
  language: string;
  format: BookFormat;
  pageCount: string;
  publishedOn: string;
  listPrice: string;
  currency: string;
  coverImageUrl: string;
  isActive: boolean;
}

const EMPTY_FORM: FormState = {
  isbn13: '',
  title: '',
  subtitle: '',
  description: '',
  categoryId: null,
  publisherId: null,
  authorIds: [],
  language: 'en',
  format: 'PAPERBACK',
  pageCount: '',
  publishedOn: '',
  listPrice: '',
  currency: 'INR',
  coverImageUrl: '',
  isActive: true,
};

function detailToForm(detail: AdminBookDetail): FormState {
  return {
    isbn13: detail.isbn13,
    title: detail.title,
    subtitle: detail.subtitle ?? '',
    description: detail.description ?? '',
    categoryId: detail.categoryId,
    publisherId: detail.publisherId,
    authorIds: detail.authorIds,
    language: detail.language ?? '',
    format: detail.format,
    pageCount: detail.pageCount != null ? String(detail.pageCount) : '',
    publishedOn: detail.publishedOn ?? '',
    listPrice: detail.listPrice,
    currency: detail.currency,
    coverImageUrl: detail.coverImageUrl ?? '',
    isActive: detail.isActive,
  };
}

export function BookFormPage() {
  const { bookId } = useParams<{ bookId: string }>();
  const isEditMode = Boolean(bookId);
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [step, setStep] = useState(0);
  const [furthest, setFurthest] = useState(0);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [toc, setToc] = useState<TocSection[]>([]);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(isEditMode);

  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [publishers, setPublishers] = useState<Publisher[]>([]);
  const [authors, setAuthors] = useState<Author[]>([]);
  const [detail, setDetail] = useState<AdminBookDetail | null>(null);

  useEffect(() => {
    Promise.all([getCategoryTree(), listPublishers(), listAuthors()]).then(([tree, pubs, auths]) => {
      setCategories(flattenCategoryTree(tree));
      setPublishers(pubs);
      setAuthors(auths);
    });
  }, []);

  useEffect(() => {
    if (!bookId) return;
    getBookForEdit(bookId)
      .then((result) => {
        setDetail(result);
        setForm(detailToForm(result));
        setToc(jsonToTocSections(result.tableOfContents));
        setFurthest(STEPS.length - 1);
      })
      .finally(() => setLoading(false));
  }, [bookId]);

  const set = (patch: Partial<FormState>) => {
    setForm((f) => ({ ...f, ...patch }));
    setErrors((e) => {
      const next = { ...e };
      for (const key of Object.keys(patch)) delete next[key as keyof FormState];
      return next;
    });
  };

  const categoryOptions = useMemo(
    () => categories.map((c) => ({ value: c.id, label: c.label.replace(/^(—\s*)+/, ''), meta: c.depth > 0 ? 'sub' : undefined })),
    [categories],
  );
  const publisherOptions = useMemo(() => publishers.map((p) => ({ value: p.id, label: p.name })), [publishers]);
  const authorOptions = useMemo(() => authors.map((a) => ({ value: a.id, label: a.name })), [authors]);

  const validateStep = (index: number): boolean => {
    const next: Partial<Record<keyof FormState, string>> = {};

    if (index === 0) {
      if (!isEditMode && form.isbn13.trim().length !== 13) next.isbn13 = 'ISBN-13 must be exactly 13 characters';
      if (!form.title.trim()) next.title = 'Title is required';
      if (!form.listPrice.trim()) next.listPrice = 'Price is required';
      else if (Number.isNaN(Number(form.listPrice))) next.listPrice = 'Price must be a number';
      if (form.currency.trim().length !== 3) next.currency = 'Use a 3-letter currency code';
    }

    if (index === 1) {
      if (form.authorIds.length === 0) next.authorIds = 'Select at least one author';
    }

    if (index === 2) {
      if (form.pageCount && !/^\d+$/.test(form.pageCount)) next.pageCount = 'Must be a whole number';
    }

    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const goNext = () => {
    if (!validateStep(step)) return;
    const next = Math.min(step + 1, STEPS.length - 1);
    setStep(next);
    setFurthest((f) => Math.max(f, next));
  };

  const submit = async () => {
    for (let i = 0; i < STEPS.length; i++) {
      if (!validateStep(i)) {
        setStep(i);
        return;
      }
    }

    const shared = {
      title: form.title.trim(),
      subtitle: form.subtitle.trim() || null,
      description: form.description.trim() || null,
      tableOfContents: tocSectionsToJson(toc),
      categoryId: form.categoryId,
      publisherId: form.publisherId,
      language: form.language.trim() || null,
      format: form.format,
      pageCount: form.pageCount ? Number(form.pageCount) : null,
      publishedOn: form.publishedOn || null,
      listPrice: form.listPrice.trim(),
      currency: form.currency.trim().toUpperCase(),
      coverImageUrl: form.coverImageUrl.trim() || null,
    };

    setSubmitting(true);
    try {
      if (isEditMode && bookId) {
        await updateBook(bookId, { ...shared, authorIds: form.authorIds, isActive: form.isActive });
        showToast('Book updated');
      } else {
        const result = await createBook({ ...shared, isbn13: form.isbn13.trim(), authorIds: form.authorIds });
        showToast('Book created — now set stock and virtual edition');
        navigate(ROUTES.editBook(result.id), { replace: true });
      }
    } catch {
      showToast('Failed to save book', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <p>Loading…</p>;

  const labelFor = (list: { value: string; label: string }[], id: string | null) =>
    id ? (list.find((o) => o.value === id)?.label ?? '—') : null;

  return (
    <div className={styles.page}>
      <PageHeader
        title={isEditMode ? form.title || 'Edit book' : 'New book'}
        subtitle={isEditMode ? `ISBN ${form.isbn13}` : 'Add a title to the catalogue in three steps.'}
        actions={
          <Button variant="secondary" onClick={() => navigate(ROUTES.books)}>
            <ArrowLeft size={15} />
            Back to books
          </Button>
        }
      />

      <Card>
        <Stepper steps={STEPS} current={step} furthestReached={furthest} onStepClick={setStep} />

        <div className={styles.stepBody}>
          {step === 0 && (
            <div className={styles.form}>
              <h3 className={styles.sectionTitle}>Book details</h3>
              <p className={styles.sectionSubtitle}>The identity and price of this title.</p>

              <div className={styles.row2}>
                <Input
                  label="ISBN-13"
                  required={!isEditMode}
                  disabled={isEditMode}
                  hint={isEditMode ? 'Cannot be changed' : '13 digits'}
                  placeholder="9780451524935"
                  value={form.isbn13}
                  error={errors.isbn13}
                  onChange={(e) => set({ isbn13: e.target.value })}
                />
                <Input
                  label="Title"
                  required
                  placeholder="e.g. Clean Code"
                  value={form.title}
                  error={errors.title}
                  onChange={(e) => set({ title: e.target.value })}
                />
              </div>

              <Input
                label="Subtitle"
                placeholder="e.g. A Handbook of Agile Software Craftsmanship"
                value={form.subtitle}
                onChange={(e) => set({ subtitle: e.target.value })}
              />

              <Textarea
                label="Description"
                hint="Also powers semantic search"
                rows={4}
                placeholder="What is this book about?"
                value={form.description}
                onChange={(e) => set({ description: e.target.value })}
              />

              <div className={styles.row3}>
                <Input
                  label="List price"
                  required
                  placeholder="499.00"
                  value={form.listPrice}
                  error={errors.listPrice}
                  onChange={(e) => set({ listPrice: e.target.value })}
                />
                <Input
                  label="Currency"
                  required
                  placeholder="INR"
                  value={form.currency}
                  error={errors.currency}
                  onChange={(e) => set({ currency: e.target.value })}
                />
                <Input
                  label="Published on"
                  type="date"
                  value={form.publishedOn}
                  onChange={(e) => set({ publishedOn: e.target.value })}
                />
              </div>
            </div>
          )}

          {step === 1 && (
            <div className={styles.form}>
              <h3 className={styles.sectionTitle}>Classification</h3>
              <p className={styles.sectionSubtitle}>
                Where this book sits in the catalogue. Start typing to search — the top 5 matches appear.
              </p>

              <div className={styles.row2}>
                <Combobox
                  label="Category"
                  placeholder="Search categories…"
                  options={categoryOptions}
                  value={form.categoryId}
                  onChange={(v) => set({ categoryId: v })}
                />
                <Combobox
                  label="Publisher"
                  placeholder="Search publishers…"
                  options={publisherOptions}
                  value={form.publisherId}
                  onChange={(v) => set({ publisherId: v })}
                />
              </div>

              <Combobox
                multiple
                label="Authors"
                required
                hint="One or more"
                placeholder="Search authors…"
                options={authorOptions}
                value={form.authorIds}
                error={errors.authorIds}
                onChange={(v) => set({ authorIds: v })}
              />

              <Input
                label="Cover image URL"
                placeholder="https://…"
                value={form.coverImageUrl}
                onChange={(e) => set({ coverImageUrl: e.target.value })}
              />

              {isEditMode && (
                <label className={styles.toggleRow}>
                  <input
                    type="checkbox"
                    checked={form.isActive}
                    onChange={(e) => set({ isActive: e.target.checked })}
                  />
                  <span className={styles.toggleText}>
                    <span className={styles.toggleLabel}>Active</span>
                    <span className={styles.toggleHint}>
                      Inactive books are hidden from customers and can't be purchased.
                    </span>
                  </span>
                </label>
              )}
            </div>
          )}

          {step === 2 && (
            <div className={styles.form}>
              <h3 className={styles.sectionTitle}>Format &amp; contents</h3>
              <p className={styles.sectionSubtitle}>
                Physical format, and the topics this book covers.
              </p>

              <div className={styles.row3}>
                <Select label="Format" value={form.format} onChange={(e) => set({ format: e.target.value as BookFormat })}>
                  <option value="HARDCOVER">Hardcover</option>
                  <option value="PAPERBACK">Paperback</option>
                  <option value="EBOOK">Ebook</option>
                </Select>
                <Input
                  label="Language"
                  placeholder="en"
                  value={form.language}
                  onChange={(e) => set({ language: e.target.value })}
                />
                <Input
                  label="Page count"
                  placeholder="320"
                  value={form.pageCount}
                  error={errors.pageCount}
                  onChange={(e) => set({ pageCount: e.target.value })}
                />
              </div>

              <div>
                <span
                  style={{
                    fontSize: 'var(--font-size-xs)',
                    fontWeight: 500,
                    color: 'var(--color-text-muted)',
                    display: 'block',
                    marginBottom: 'var(--space-2)',
                  }}
                >
                  Table of contents
                  <span style={{ color: 'var(--color-text-subtle)', fontWeight: 400 }}>
                    {' '}
                    — optional, improves search quality for non-fiction
                  </span>
                </span>
                <TocBuilder value={toc} onChange={setToc} />
              </div>

              <div className={styles.divider} />

              <h3 className={styles.sectionTitle}>Review</h3>
              <div className={styles.reviewGrid}>
                <ReviewItem label="Title" value={form.title} />
                <ReviewItem label="ISBN-13" value={form.isbn13} />
                <ReviewItem label="Price" value={`${form.listPrice} ${form.currency}`} />
                <ReviewItem label="Category" value={labelFor(categoryOptions, form.categoryId)} />
                <ReviewItem label="Publisher" value={labelFor(publisherOptions, form.publisherId)} />
                <ReviewItem
                  label="Authors"
                  value={
                    form.authorIds.map((id) => authorOptions.find((a) => a.value === id)?.label).filter(Boolean).join(', ') ||
                    null
                  }
                />
                <ReviewItem label="Format" value={form.format} />
                <ReviewItem label="Sections" value={toc.length ? `${toc.length}` : null} />
              </div>

              {!isEditMode && (
                <div className={styles.postCreateNote}>
                  <Info size={15} style={{ flexShrink: 0, marginTop: 2 }} />
                  <span>
                    Stock levels and the virtual edition are set after the book exists — you'll land on the edit
                    page for that next.
                  </span>
                </div>
              )}
            </div>
          )}
        </div>

        <div className={styles.stepFooter}>
          {step > 0 && (
            <Button variant="secondary" onClick={() => setStep(step - 1)}>
              <ArrowLeft size={15} />
              Back
            </Button>
          )}
          <div className={styles.footerRight}>
            {isEditMode && (
              <Button variant="ghost" onClick={() => navigate(ROUTES.books)}>
                Cancel
              </Button>
            )}
            {step < STEPS.length - 1 ? (
              <Button onClick={goNext}>
                Continue
                <ArrowRight size={15} />
              </Button>
            ) : (
              <Button onClick={submit} disabled={submitting}>
                <Check size={15} />
                {submitting ? 'Saving…' : isEditMode ? 'Save changes' : 'Create book'}
              </Button>
            )}
          </div>
        </div>
      </Card>

      {isEditMode && bookId && detail && (
        <>
          <InventorySection bookId={bookId} inventory={detail.inventory} />
          <VirtualEditionSection
            bookId={bookId}
            virtualEdition={detail.virtualEdition}
            onChanged={() => getBookForEdit(bookId).then(setDetail)}
          />
        </>
      )}
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
