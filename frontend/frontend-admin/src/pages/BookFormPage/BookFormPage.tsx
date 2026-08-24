import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { createBook, getBookForEdit, getCategoryTree, listAuthors, listPublishers, updateBook } from '@/api/catalogApi';
import type { Author, Publisher } from '@/types/catalog';
import { flattenCategoryTree, type FlatCategory } from '@/utils/flattenCategoryTree';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input, Select, Textarea } from '@/components/Input';
import { Button } from '@/components/Button';
import { ROUTES } from '@/constants/routes';
import { InventorySection } from './InventorySection';
import { VirtualEditionSection } from './VirtualEditionSection';
import type { AdminBookDetail } from '@/types/catalog';
import styles from './BookFormPage.module.css';

const bookSchema = z.object({
  isbn13: z.string().length(13, 'ISBN-13 must be 13 characters'),
  title: z.string().min(1, 'Title is required'),
  subtitle: z.string().optional(),
  description: z.string().optional(),
  tableOfContents: z.string().optional(),
  categoryId: z.string().optional(),
  publisherId: z.string().optional(),
  authorIds: z.array(z.string()).min(1, 'Select at least one author'),
  language: z.string().optional(),
  format: z.enum(['HARDCOVER', 'PAPERBACK', 'EBOOK']),
  pageCount: z.string().regex(/^\d*$/, 'Must be a whole number').optional(),
  publishedOn: z.string().optional(),
  listPrice: z.string().min(1, 'Price is required'),
  currency: z.string().length(3, 'Use a 3-letter currency code'),
  coverImageUrl: z.string().optional(),
  isActive: z.boolean(),
});

type BookFormValues = z.infer<typeof bookSchema>;

const emptyDefaults: BookFormValues = {
  isbn13: '',
  title: '',
  subtitle: '',
  description: '',
  tableOfContents: '',
  categoryId: '',
  publisherId: '',
  authorIds: [],
  language: '',
  format: 'PAPERBACK',
  pageCount: '',
  publishedOn: '',
  listPrice: '',
  currency: 'USD',
  coverImageUrl: '',
  isActive: true,
};

function detailToFormValues(detail: AdminBookDetail): BookFormValues {
  return {
    isbn13: detail.isbn13,
    title: detail.title,
    subtitle: detail.subtitle ?? '',
    description: detail.description ?? '',
    tableOfContents: detail.tableOfContents ?? '',
    categoryId: detail.categoryId ?? '',
    publisherId: detail.publisherId ?? '',
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

  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [publishers, setPublishers] = useState<Publisher[]>([]);
  const [authors, setAuthors] = useState<Author[]>([]);
  const [detail, setDetail] = useState<AdminBookDetail | null>(null);
  const [loading, setLoading] = useState(isEditMode);

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<BookFormValues>({ resolver: zodResolver(bookSchema), defaultValues: emptyDefaults });

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
        reset(detailToFormValues(result));
      })
      .finally(() => setLoading(false));
  }, [bookId, reset]);

  const onSubmit = async (values: BookFormValues) => {
    const shared = {
      title: values.title,
      subtitle: values.subtitle || null,
      description: values.description || null,
      tableOfContents: values.tableOfContents || null,
      categoryId: values.categoryId || null,
      publisherId: values.publisherId || null,
      language: values.language || null,
      format: values.format,
      pageCount: !values.pageCount ? null : Number(values.pageCount),
      publishedOn: values.publishedOn || null,
      listPrice: values.listPrice,
      currency: values.currency,
      coverImageUrl: values.coverImageUrl || null,
    };

    try {
      if (isEditMode && bookId) {
        await updateBook(bookId, { ...shared, authorIds: values.authorIds, isActive: values.isActive });
        showToast('Book updated');
      } else {
        const result = await createBook({ ...shared, isbn13: values.isbn13, authorIds: values.authorIds });
        showToast('Book created');
        navigate(ROUTES.editBook(result.id), { replace: true });
        return;
      }
    } catch {
      showToast('Failed to save book', 'error');
    }
  };

  if (loading) {
    return <p>Loading…</p>;
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1>{isEditMode ? 'Edit book' : 'New book'}</h1>
      </div>

      <Card>
        <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
          <div className={styles.row}>
            <Input
              label="ISBN-13"
              disabled={isEditMode}
              error={errors.isbn13?.message}
              {...register('isbn13')}
            />
            <Input label="Title" error={errors.title?.message} {...register('title')} />
          </div>

          <Input label="Subtitle" {...register('subtitle')} />
          <Textarea label="Description" rows={4} {...register('description')} />
          <Textarea
            label="Table of contents (JSON)"
            rows={4}
            placeholder='{"Basics": ["Variables", "Loops"]}'
            {...register('tableOfContents')}
          />

          <div className={styles.row}>
            <Select label="Category" {...register('categoryId')}>
              <option value="">— None —</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.label}
                </option>
              ))}
            </Select>
            <Select label="Publisher" {...register('publisherId')}>
              <option value="">— None —</option>
              {publishers.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </Select>
          </div>

          <Controller
            name="authorIds"
            control={control}
            render={({ field }) => (
              <div>
                <span className={styles.sectionSubtitle}>Authors</span>
                <div className={styles.checkboxGrid}>
                  {authors.map((author) => (
                    <label key={author.id} className={styles.checkboxLabel}>
                      <input
                        type="checkbox"
                        checked={field.value.includes(author.id)}
                        onChange={(e) => {
                          field.onChange(
                            e.target.checked
                              ? [...field.value, author.id]
                              : field.value.filter((id) => id !== author.id),
                          );
                        }}
                      />
                      {author.name}
                    </label>
                  ))}
                </div>
                {errors.authorIds && <span>{errors.authorIds.message}</span>}
              </div>
            )}
          />

          <div className={styles.row3}>
            <Select label="Format" error={errors.format?.message} {...register('format')}>
              <option value="HARDCOVER">Hardcover</option>
              <option value="PAPERBACK">Paperback</option>
              <option value="EBOOK">Ebook</option>
            </Select>
            <Input label="Language" {...register('language')} />
            <Input label="Page count" type="number" {...register('pageCount')} />
          </div>

          <div className={styles.row3}>
            <Input label="Published on" type="date" {...register('publishedOn')} />
            <Input label="List price" error={errors.listPrice?.message} {...register('listPrice')} />
            <Input label="Currency" error={errors.currency?.message} {...register('currency')} />
          </div>

          <Input label="Cover image URL" {...register('coverImageUrl')} />

          {isEditMode && (
            <label className={styles.toggleRow}>
              <input type="checkbox" {...register('isActive')} />
              Active (visible to customers)
            </label>
          )}

          <div className={styles.actions}>
            <Button type="submit" disabled={isSubmitting}>
              {isEditMode ? 'Save changes' : 'Create book'}
            </Button>
            <Button type="button" variant="secondary" onClick={() => navigate(ROUTES.books)}>
              Cancel
            </Button>
          </div>
        </form>
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
