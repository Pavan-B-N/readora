import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { updateInventory } from '@/api/catalogApi';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input } from '@/components/Input';
import { Button } from '@/components/Button';
import type { AdminBookDetail } from '@/types/catalog';
import styles from './BookFormPage.module.css';

const wholeNumber = z.string().regex(/^\d+$/, 'Must be a whole number, 0 or greater');

const schema = z.object({
  qtyOnHand: wholeNumber,
  reorderThreshold: wholeNumber,
});

type FormValues = z.infer<typeof schema>;

export function InventorySection({ bookId, inventory }: { bookId: string; inventory: AdminBookDetail['inventory'] }) {
  const { showToast } = useToast();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      qtyOnHand: String(inventory?.qtyOnHand ?? 0),
      reorderThreshold: String(inventory?.reorderThreshold ?? 0),
    },
  });

  const onSubmit = async (values: FormValues) => {
    try {
      await updateInventory(bookId, {
        qtyOnHand: Number(values.qtyOnHand),
        reorderThreshold: Number(values.reorderThreshold),
      });
      showToast('Inventory updated');
    } catch {
      showToast('Failed to update inventory', 'error');
    }
  };

  return (
    <Card>
      <h2 className={styles.sectionTitle}>Inventory</h2>
      <p className={styles.sectionSubtitle}>
        {inventory ? `${inventory.qtyReserved} currently reserved by open orders.` : 'No stock record yet — saving creates one.'}
      </p>

      <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
        <div className={styles.row}>
          <Input label="Quantity on hand" type="number" error={errors.qtyOnHand?.message} {...register('qtyOnHand')} />
          <Input label="Reorder threshold" type="number" error={errors.reorderThreshold?.message} {...register('reorderThreshold')} />
        </div>
        <div className={styles.actions}>
          <Button type="submit" disabled={isSubmitting}>
            Save inventory
          </Button>
        </div>
      </form>
    </Card>
  );
}
