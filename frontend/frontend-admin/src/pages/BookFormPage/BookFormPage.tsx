import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Check, Info, X } from 'lucide-react';
import { createAuthor, createBook, getCategoryTree, listAuthors, listPublishers, listStores } from '@/api/catalogApi';
import type { Author, Publisher, Store } from '@/types/catalog';
import { flattenCategoryTree, type FlatCategory } from '@/utils/flattenCategoryTree';
import { slugify } from '@/utils/slugify';
import { getMe } from '@/api/userApi';
import { useToast } from '@readora/shared-ui';
import { Card } from '@readora/shared-ui';
import { Input, Textarea } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { Combobox } from '@/components/Combobox';
import { Stepper, type Step } from '@/components/Stepper';
import { TocBuilder, topicsToJson } from '@/components/TocBuilder';
import { ROUTES } from '@/constants/routes';
import styles from './BookFormPage.module.css';

const STEPS: Step[] = [
  { label: 'Details', description: 'Title & identity' },
  { label: 'Classification', description: 'Category & authors' },
  { label: 'Contents', description: 'Language & topics' },
];

interface FormState {
  isbn13: string;
  title: string;
  description: string;
  categoryId: string | null;
  publisherId: string | null;
  storeId: string;
  authorIds: string[];
  language: string;
  pageCount: string;
  publishedOn: string;
  listPrice: string;
  currency: string;
  coverImageUrl: string;
}

const EMPTY_FORM: FormState = {
  isbn13: '',
  title: '',
  description: '',
  categoryId: null,
  publisherId: null,
  storeId: '',
  authorIds: [],
  language: 'en',
  pageCount: '',
  publishedOn: '',
  listPrice: '',
  currency: 'INR',
  coverImageUrl: '',
};

/** Clamps whatever's in the URL's ?step= to a valid step index, defaulting to 0. */
function readStepParam(searchParams: URLSearchParams): number {
  const raw = Number(searchParams.get('step'));
  return Number.isInteger(raw) && raw >= 0 && raw < STEPS.length ? raw : 0;
}

export function BookFormPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [searchParams, setSearchParams] = useSearchParams();

  const [step, setStepState] = useState(() => readStepParam(searchParams));
  const [furthest, setFurthest] = useState(step);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [toc, setToc] = useState<string[]>([]);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [submitting, setSubmitting] = useState(false);

  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [publishers, setPublishers] = useState<Publisher[]>([]);
  const [authors, setAuthors] = useState<Author[]>([]);
  const [stores, setStores] = useState<Store[]>([]);
  const [creatingAuthor, setCreatingAuthor] = useState(false);
  const [noStoreAssigned, setNoStoreAssigned] = useState(false);

  const setStep = (next: number) => {
    setStepState(next);
    setSearchParams((prev) => {
      const merged = new URLSearchParams(prev);
      merged.set('step', String(next));
      return merged;
    }, { replace: true });
  };

  useEffect(() => {
    Promise.all([getCategoryTree(), listPublishers(), listAuthors(), listStores(), getMe()]).then(
      ([tree, pubs, auths, storeList, me]) => {
        setCategories(flattenCategoryTree(tree));
        setPublishers(pubs);
        setAuthors(auths);
        setStores(storeList);
        // Admins are locked to their assigned store — the backend enforces this too, so there's
        // no client-side fallback to "just pick the first store" if unassigned.
        if (me.adminStoreId) {
          set({ storeId: me.adminStoreId });
        } else {
          setNoStoreAssigned(true);
        }
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const set = (patch: Partial<FormState>) => {
    setForm((f) => ({ ...f, ...patch }));
    setErrors((e) => {
      const next = { ...e };
      for (const key of Object.keys(patch)) delete next[key as keyof FormState];
      return next;
    });
  };

  const handleCreateAuthor = async (name: string) => {
    setCreatingAuthor(true);
    try {
      const result = await createAuthor({ name, slug: slugify(name), bio: null, photoUrl: null });
      const newAuthor: Author = { id: result.id, name, slug: slugify(name), bio: null, photoUrl: null };
      setAuthors((prev) => [...prev, newAuthor]);
      set({ authorIds: [...form.authorIds, result.id] });
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

  const [isbnChecking, setIsbnChecking] = useState(false);

  useEffect(() => {
    if (form.isbn13.trim().length === 13) {
      setIsbnChecking(true);
      checkIsbnExists(form.isbn13.trim())
        .then(exists => {
          if (exists) setErrors(e => ({ ...e, isbn13: 'A book with this ISBN already exists' }));
        })
        .finally(() => setIsbnChecking(false));
    }
  }, [form.isbn13]);

  const validateStep = (index: number): boolean => {
    const next: Partial<Record<keyof FormState | 'tableOfContents', string>> = {};

    if (index === 0) {
      if (form.isbn13.trim().length !== 13) next.isbn13 = 'ISBN-13 must be exactly 13 characters';
      if (!form.title.trim()) next.title = 'Title is required';
      if (!form.description.trim()) next.description = 'Description is required';
      if (!form.listPrice.trim()) next.listPrice = 'Price is required';
      else if (Number.isNaN(Number(form.listPrice))) next.listPrice = 'Price must be a number';
      if (form.currency.trim().length !== 3) next.currency = 'Use a 3-letter currency code';
      if (!form.publishedOn) next.publishedOn = 'Published date is required';
      
      if (errors.isbn13 && form.isbn13.trim().length === 13) {
        next.isbn13 = errors.isbn13;
      }
    }

    if (index === 1) {
      if (!form.storeId) {
        next.storeId = noStoreAssigned
          ? "Your account isn't assigned to a store — ask a super-admin to assign one"
          : 'Select a store';
      }
      if (!form.categoryId) next.categoryId = 'Select a category';
      if (!form.publisherId) next.publisherId = 'Select a publisher';
      if (form.authorIds.length === 0) next.authorIds = 'Select at least one author';
      if (form.coverImageUrl && !/^https?:\/\//.test(form.coverImageUrl)) next.coverImageUrl = 'Must be a valid URL starting with http:// or https://';
      else if (!form.coverImageUrl.trim()) next.coverImageUrl = 'Cover image URL is required';
    }

    if (index === 2) {
      if (!form.pageCount.trim()) next.pageCount = 'Page count is required';
      else if (!/^\d+$/.test(form.pageCount)) next.pageCount = 'Must be a whole number';
      if (toc.length === 0) (next as any).tableOfContents = 'Table of contents is required';
    }

    setErrors(next as any);
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

    setSubmitting(true);
    try {
      const result = await createBook({
        isbn13: form.isbn13.trim(),
        title: form.title.trim(),
        description: form.description.trim() || null,
        tableOfContents: topicsToJson(toc),
        categoryId: form.categoryId,
        publisherId: form.publisherId,
        storeId: form.storeId,
        authorIds: form.authorIds,
        language: form.language.trim() || null,
        pageCount: form.pageCount ? Number(form.pageCount) : null,
        publishedOn: form.publishedOn || null,
        listPrice: form.listPrice.trim(),
        currency: form.currency.trim().toUpperCase(),
        coverImageUrl: form.coverImageUrl.trim() || null,
      });
      showToast('Book created — now set stock and virtual edition');
      navigate(ROUTES.editBook(result.id), { replace: true });
    } catch (error: unknown) {
      const errorCode = (error as { response?: { data?: { errorCode?: string } } })?.response?.data?.errorCode;
      if (errorCode === 'ISBN_ALREADY_EXISTS') {
        setStep(0);
        setErrors((e) => ({ ...e, isbn13: 'A book with this ISBN already exists — increase its stock instead' }));
        showToast('That ISBN is already in the catalogue', 'error');
      } else {
        showToast('Failed to save book', 'error');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const labelFor = (list: { value: string; label: string }[], id: string | null) =>
    id ? (list.find((o) => o.value === id)?.label ?? '—') : null;

  return (
    <div className={styles.page}>
      <div className={styles.formContainer}>

        <Stepper steps={STEPS} current={step} furthestReached={furthest} onStepClick={setStep} />

        <div className={styles.stepBody}>
          {step === 0 && (
            <div className={styles.form}>
              <h3 className={styles.sectionTitle}>Book details</h3>
              <p className={styles.sectionSubtitle}>The identity and price of this title.</p>

              <div className={styles.row2}>
                <Input
                  label="ISBN-13"
                  required
                  hint="13 digits"
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
                required
                hint="Also powers semantic search"
                rows={4}
                value={form.description}
                error={errors.description}
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
                  required
                  value={form.publishedOn}
                  error={errors.publishedOn}
                  onChange={(e) => set({ publishedOn: e.target.value })}
                />
              </div>
            </div>
          )}

          {step === 1 && (
            <div className={styles.form}>
              <h3 className={styles.sectionTitle}>Classification</h3>
              <p className={styles.sectionSubtitle}>
                Where this book sits in the catalogue. Start typing to search — the top 20 matches appear.
              </p>

              <Input
                label="Store"
                hint={
                  noStoreAssigned
                    ? "Your account has no store assignment — you can't create books until one is set"
                    : 'You can only list books under your own assigned store'
                }
                error={errors.storeId}
                disabled
                value={
                  stores.length === 0
                    ? 'Loading…'
                    : noStoreAssigned
                      ? 'Not assigned'
                      : (stores.find((s) => s.id === form.storeId)?.name ?? 'None')
                }
                onChange={() => {}}
              />

              <div className={styles.row2}>
                <Combobox
                  label="Category"
                  required
                  placeholder="Search categories…"
                  options={categoryOptions}
                  value={form.categoryId}
                  error={errors.categoryId}
                  onChange={(v) => set({ categoryId: v })}
                />
                <Combobox
                  label="Publisher"
                  required
                  placeholder="Search publishers…"
                  options={publisherOptions}
                  value={form.publisherId}
                  error={errors.publisherId}
                  onChange={(v) => set({ publisherId: v })}
                />
              </div>

              <Combobox
                multiple
                label="Authors"
                required
                hint="One or more — type a new name to add an author that isn't in the catalogue yet"
                placeholder="Search authors…"
                options={authorOptions}
                value={form.authorIds}
                error={errors.authorIds}
                onChange={(v) => set({ authorIds: v })}
                onCreate={handleCreateAuthor}
                creating={creatingAuthor}
              />

              <Input
                label="Cover image URL"
                required
                placeholder="https://…"
                value={form.coverImageUrl}
                error={errors.coverImageUrl}
                onChange={(e) => set({ coverImageUrl: e.target.value })}
              />
            </div>
          )}

          {step === 2 && (
            <div className={styles.form}>
              <h3 className={styles.sectionTitle}>Contents</h3>
              <p className={styles.sectionSubtitle}>
                Language, length, and the topics this book covers.
              </p>

              <div className={styles.row3}>
                <Input
                  label="Language"
                  placeholder="en"
                  value={form.language}
                  onChange={(e) => set({ language: e.target.value })}
                />
                <Input
                  label="Page count"
                  required
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
                  Table of contents <span style={{ color: 'var(--color-danger)' }}>*</span>
                  <span style={{ color: 'var(--color-text-subtle)', fontWeight: 400 }}>
                    {' '}
                    — improves search quality for non-fiction
                  </span>
                </span>
                <TocBuilder value={toc} onChange={setToc} />
                {(errors as any).tableOfContents && (
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-danger)', marginTop: '4px' }}>
                    {(errors as any).tableOfContents}
                  </div>
                )}
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
                <ReviewItem label="Topics" value={toc.length ? `${toc.length}` : null} />
              </div>

              <div className={styles.postCreateNote}>
                <Info size={15} style={{ flexShrink: 0, marginTop: 2 }} />
                <span>
                  Stock levels and the virtual edition are set after the book exists — you'll land on its detail
                  page for that next.
                </span>
              </div>
            </div>
          )}
        </div>

        <div className={styles.stepFooter}>
          {step > 0 ? (
            <Button variant="secondary" onClick={() => setStep(step - 1)}>
              <ArrowLeft size={15} />
              Back
            </Button>
          ) : (
            <Button variant="secondary" onClick={() => navigate(ROUTES.books)}>
              Cancel
            </Button>
          )}
          <div className={styles.footerRight}>
            {step < STEPS.length - 1 ? (
              <Button onClick={goNext}>
                Continue
                <ArrowRight size={15} />
              </Button>
            ) : (
              <Button onClick={submit} disabled={submitting}>
                <Check size={15} />
                {submitting ? 'Saving…' : 'Create book'}
              </Button>
            )}
          </div>
        </div>
      </div>
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
