import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { createPublisher, listPublishers } from '@/api/catalogApi';
import type { Publisher } from '@/types/catalog';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input } from '@/components/Input';
import { Button } from '@/components/Button';
import styles from './PublishersPage.module.css';

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  slug: z.string().min(1, 'Slug is required'),
});

type FormValues = z.infer<typeof schema>;

export function PublishersPage() {
  const { showToast } = useToast();
  const [publishers, setPublishers] = useState<Publisher[]>([]);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const reload = () => {
    listPublishers().then(setPublishers);
  };
  useEffect(() => {
    reload();
  }, []);

  const onSubmit = async (values: FormValues) => {
    try {
      await createPublisher(values);
      showToast('Publisher created');
      reset();
      reload();
    } catch {
      showToast('Failed to create publisher', 'error');
    }
  };

  return (
    <div className={styles.page}>
      <div>
        <h1>Publishers</h1>
        <Card>
          {publishers.length === 0 ? (
            <p>No publishers yet.</p>
          ) : (
            <ul className={styles.list}>
              {publishers.map((p) => (
                <li key={p.id}>{p.name}</li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <Card>
        <h2>New publisher</h2>
        <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
          <Input label="Name" error={errors.name?.message} {...register('name')} />
          <Input label="Slug" error={errors.slug?.message} {...register('slug')} />
          <Button type="submit" disabled={isSubmitting}>
            Create publisher
          </Button>
        </form>
      </Card>
    </div>
  );
}
