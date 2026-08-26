import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Check } from 'lucide-react';
import {
  createAuthor,
  createBook,
  getCategoryTree,
  listAuthors,
  upsertVirtualEdition,
} from '@/api/catalogApi';
import type { Author, VirtualFileFormat } from '@/types/catalog';
import { flattenCategoryTree, type FlatCategory } from '@/utils/flattenCategoryTree';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Input, Select, Textarea } from '@/components/Input';
import { Button } from '@/components/Button';
import { Combobox } from '@/components/Combobox';
import { PageHeader } from '@/components/PageHeader';
import { ROUTES } from '@/constants/routes';
import styles from '../BookFormPage/BookFormPage.module.css';

interface FormState {
  isbn13: string;
  title: string;
  subtitle: string;
  description: string;
  categoryId: string | null;
  authorIds: string[];
  language: string;
  coverImageUrl: string;
  fileUrl: string;
  fileFormat: VirtualFileFormat;
  fileSizeBytes: string;
  price: string;
  currency: string;
}

const EMPTY_FORM: FormState = {
  isbn13: '',
  title: '',
  subtitle: '',
  description: '',
  categoryId: null,
  authorIds: [],
  language: 'en',
  coverImageUrl: '',
  fileUrl: '',
  fileFormat: 'PDF',
  fileSizeBytes: '',
  price: '',
  currency: 'INR',
};

export function VirtualBookFormPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [submitting, setSubmitting] = useState(false);

  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [authors, setAuthors] = useState<Author[]>([]);
  const [creatingAuthor, setCreatingAuthor] = useState(false);

  useEffect(() => {
    Promise.all([getCategoryTree(), listAuthors()]).then(([tree, auths]) => {
      setCategories(flattenCategoryTree(tree));
      setAuthors(auths);
    });
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
      const result = await createAuthor({ name, slug: name.toLowerCase().replace(/[^a-z0-9]+/g, '-'), bio: null });
      const newAuthor: Author = { id: result.id, name, slug: '', bio: null };
      setAuthors((prev) => [...prev, newAuthor]);
      set({ authorIds: [...form.authorIds, result.id] });
      showToast(`Author "${name}" added`);
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
  const authorOptions = useMemo(() => authors.map((a) => ({ value: a.id, label: a.name })), [authors]);

  const validate = (): boolean => {
    const next: Partial<Record<keyof FormState, string>> = {};
    if (form.isbn13.trim().length !== 13) next.isbn13 = 'ISBN-13 must be exactly 13 characters';
    if (!form.title.trim()) next.title = 'Title is required';
    if (form.authorIds.length === 0) next.authorIds = 'Select at least one author';
    if (!form.fileUrl.trim()) next.fileUrl = 'File URL is required';
    if (!form.price.trim()) next.price = 'Price is required';
    else if (Number.isNaN(Number(form.price))) next.price = 'Price must be a number';
    if (form.currency.trim().length !== 3) next.currency = 'Use a 3-letter currency code';
    if (form.fileSizeBytes && !/^\d+$/.test(form.fileSizeBytes)) next.fileSizeBytes = 'Whole number';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async () => {
    if (!validate()) return;

    setSubmitting(true);
    try {
      const book = await createBook({
        isbn13: form.isbn13.trim(),
        title: form.title.trim(),
        subtitle: form.subtitle.trim() || null,
        description: form.description.trim() || null,
        tableOfContents: null,
        categoryId: form.categoryId,
        publisherId: null,
        storeId: null,
        authorIds: form.authorIds,
        language: form.language.trim() || null,
        pageCount: null,
        publishedOn: null,
        listPrice: form.price.trim(),
        currency: form.currency.trim().toUpperCase(),
        coverImageUrl: form.coverImageUrl.trim() || null,
      });

      await upsertVirtualEdition(book.id, {
        fileUrl: form.fileUrl.trim(),
        fileFormat: form.fileFormat,
        fileSizeBytes: form.fileSizeBytes ? Number(form.fileSizeBytes) : null,
        price: form.price.trim(),
        currency: form.currency.trim().toUpperCase(),
      });

      showToast('Virtual edition created');
      navigate(ROUTES.editBook(book.id), { replace: true });
    } catch {
      showToast('Failed to create virtual edition', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.page}>
      <PageHeader
        title="New virtual edition"
        subtitle="Universally available — no physical store or stock involved."
        actions={
          <Button variant="secondary" onClick={() => navigate(ROUTES.books)}>
            <ArrowLeft size={15} />
            Back to catalog
          </Button>
        }
      />

      <Card>
        <CardHeader title="Book details" />
        <div className={styles.form}>
          <div className={styles.row2}>
            <Input
              label="ISBN-13"
              required
              placeholder="9780451524935"
              hint="13 digits"
              value={form.isbn13}
              error={errors.isbn13}
              onChange={(e) => set({ isbn13: e.target.value })}
            />
            <Input
              label="Title"
              required
              placeholder="e.g. Java & Spring"
              value={form.title}
              error={errors.title}
              onChange={(e) => set({ title: e.target.value })}
            />
          </div>

          <Input
            label="Subtitle"
            placeholder="Optional"
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

          <div className={styles.row2}>
            <Combobox
              label="Category"
              placeholder="Search categories…"
              options={categoryOptions}
              value={form.categoryId}
              onChange={(v) => set({ categoryId: v })}
            />
            <Input
              label="Language"
              placeholder="en"
              value={form.language}
              onChange={(e) => set({ language: e.target.value })}
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
            placeholder="https://…"
            value={form.coverImageUrl}
            onChange={(e) => set({ coverImageUrl: e.target.value })}
          />
        </div>
      </Card>

      <Card>
        <CardHeader title="Virtual file & price" />
        <div className={styles.form}>
          <Input
            label="File URL"
            required
            hint="Storage key — a signed URL is generated at delivery"
            placeholder="java-and-spring.pdf"
            value={form.fileUrl}
            error={errors.fileUrl}
            onChange={(e) => set({ fileUrl: e.target.value })}
          />

          <div className={styles.row3}>
            <Select label="File format" value={form.fileFormat} onChange={(e) => set({ fileFormat: e.target.value as VirtualFileFormat })}>
              <option value="PDF">PDF</option>
              <option value="EPUB">EPUB</option>
            </Select>
            <Input
              label="File size"
              hint="bytes, optional"
              placeholder="512000"
              value={form.fileSizeBytes}
              error={errors.fileSizeBytes}
              onChange={(e) => set({ fileSizeBytes: e.target.value })}
            />
            <Input
              label="Currency"
              required
              value={form.currency}
              error={errors.currency}
              onChange={(e) => set({ currency: e.target.value })}
            />
          </div>

          <Input
            label="Price"
            required
            placeholder="249.00"
            value={form.price}
            error={errors.price}
            onChange={(e) => set({ price: e.target.value })}
          />

          <Button onClick={submit} disabled={submitting} block>
            <Check size={15} />
            {submitting ? 'Creating…' : 'Create virtual edition'}
          </Button>
        </div>
      </Card>
    </div>
  );
}
