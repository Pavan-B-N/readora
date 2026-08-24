import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { createAuthor, listAuthors } from '@/api/catalogApi';
import type { Author } from '@/types/catalog';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input, Textarea } from '@/components/Input';
import { Button } from '@/components/Button';
import styles from '../PublishersPage/PublishersPage.module.css';

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  slug: z.string().min(1, 'Slug is required'),
  bio: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export function AuthorsPage() {
  const { showToast } = useToast();
  const [authors, setAuthors] = useState<Author[]>([]);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const reload = () => {
    listAuthors().then(setAuthors);
  };
  useEffect(() => {
    reload();
  }, []);

  const onSubmit = async (values: FormValues) => {
    try {
      await createAuthor({ name: values.name, slug: values.slug, bio: values.bio || null });
      showToast('Author created');
      reset();
      reload();
    } catch {
      showToast('Failed to create author', 'error');
    }
  };

  return (
    <div className={styles.page}>
      <div>
        <h1>Authors</h1>
        <Card>
          {authors.length === 0 ? (
            <p>No authors yet.</p>
          ) : (
            <ul className={styles.list}>
              {authors.map((a) => (
                <li key={a.id}>{a.name}</li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <Card>
        <h2>New author</h2>
        <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
          <Input label="Name" error={errors.name?.message} {...register('name')} />
          <Input label="Slug" error={errors.slug?.message} {...register('slug')} />
          <Textarea label="Bio" rows={3} {...register('bio')} />
          <Button type="submit" disabled={isSubmitting}>
            Create author
          </Button>
        </form>
      </Card>
    </div>
  );
}
