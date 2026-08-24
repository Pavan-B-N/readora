import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { deactivateVirtualEdition, upsertVirtualEdition } from '@/api/catalogApi';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input, Select } from '@/components/Input';
import { Button } from '@/components/Button';
import type { AdminBookDetail } from '@/types/catalog';
import styles from './BookFormPage.module.css';

const schema = z.object({
  fileUrl: z.string().min(1, 'File URL is required'),
  fileFormat: z.enum(['PDF', 'EPUB']),
  fileSizeBytes: z.string().regex(/^\d*$/, 'Must be a whole number').optional(),
  price: z.string().min(1, 'Price is required'),
  currency: z.string().length(3, 'Use a 3-letter currency code'),
});

type FormValues = z.infer<typeof schema>;

export function VirtualEditionSection({
  bookId,
  virtualEdition,
  onChanged,
}: {
  bookId: string;
  virtualEdition: AdminBookDetail['virtualEdition'];
  onChanged: () => void;
}) {
  const { showToast } = useToast();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      fileUrl: virtualEdition?.fileUrl ?? '',
      fileFormat: virtualEdition?.fileFormat ?? 'PDF',
      fileSizeBytes: virtualEdition?.fileSizeBytes != null ? String(virtualEdition.fileSizeBytes) : '',
      price: virtualEdition?.price ?? '',
      currency: virtualEdition?.currency ?? 'USD',
    },
  });

  const onSubmit = async (values: FormValues) => {
    try {
      await upsertVirtualEdition(bookId, {
        fileUrl: values.fileUrl,
        fileFormat: values.fileFormat,
        fileSizeBytes: !values.fileSizeBytes ? null : Number(values.fileSizeBytes),
        price: values.price,
        currency: values.currency,
      });
      showToast('Virtual edition saved');
      onChanged();
    } catch {
      showToast('Failed to save virtual edition', 'error');
    }
  };

  const onDeactivate = async () => {
    try {
      await deactivateVirtualEdition(bookId);
      showToast('Virtual edition deactivated');
      onChanged();
    } catch {
      showToast('Failed to deactivate virtual edition', 'error');
    }
  };

  return (
    <Card>
      <h2 className={styles.sectionTitle}>Virtual edition</h2>
      <p className={styles.sectionSubtitle}>
        {virtualEdition
          ? virtualEdition.isActive
            ? 'Active — customers can buy the digital edition.'
            : 'Deactivated — saving below reactivates it.'
          : 'No virtual edition yet.'}
      </p>

      <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
        <Input label="File URL" error={errors.fileUrl?.message} {...register('fileUrl')} />

        <div className={styles.row3}>
          <Select label="File format" error={errors.fileFormat?.message} {...register('fileFormat')}>
            <option value="PDF">PDF</option>
            <option value="EPUB">EPUB</option>
          </Select>
          <Input label="File size (bytes)" type="number" error={errors.fileSizeBytes?.message} {...register('fileSizeBytes')} />
          <Input label="Currency" error={errors.currency?.message} {...register('currency')} />
        </div>

        <Input label="Price" error={errors.price?.message} {...register('price')} />

        <div className={styles.actions}>
          <Button type="submit" disabled={isSubmitting}>
            Save virtual edition
          </Button>
          {virtualEdition?.isActive && (
            <Button type="button" variant="danger" onClick={onDeactivate}>
              Deactivate
            </Button>
          )}
        </div>
      </form>
    </Card>
  );
}
