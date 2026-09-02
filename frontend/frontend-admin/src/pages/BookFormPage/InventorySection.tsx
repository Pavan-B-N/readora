import { useState } from 'react';
import { Package, PackagePlus } from 'lucide-react';
import { updateInventory } from '@/api/catalogApi';
import { useToast } from '@readora/shared-ui';
import { Card, CardHeader } from '@readora/shared-ui';
import { Input } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import type { AdminBookDetail } from '@/types/catalog';
import styles from './BookFormPage.module.css';

export function InventorySection({
  bookId,
  inventory,
}: {
  bookId: string;
  inventory: AdminBookDetail['inventory'];
}) {
  const { showToast } = useToast();
  const [qtyOnHand, setQtyOnHand] = useState(String(inventory?.qtyOnHand ?? 0));
  const [reorderThreshold, setReorderThreshold] = useState(String(inventory?.reorderThreshold ?? 0));
  const [errors, setErrors] = useState<{ qtyOnHand?: string; reorderThreshold?: string }>({});
  const [saving, setSaving] = useState(false);

  const [restockQty, setRestockQty] = useState('');
  const [restockError, setRestockError] = useState<string | undefined>();
  const [restocking, setRestocking] = useState(false);

  const available = (Number(qtyOnHand) || 0) - (inventory?.qtyReserved ?? 0);

  const onSave = async () => {
    const next: typeof errors = {};
    if (!/^\d+$/.test(qtyOnHand)) next.qtyOnHand = 'Whole number, 0 or more';
    if (!/^\d+$/.test(reorderThreshold)) next.reorderThreshold = 'Whole number, 0 or more';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSaving(true);
    try {
      await updateInventory(bookId, {
        qtyOnHand: Number(qtyOnHand),
        reorderThreshold: Number(reorderThreshold),
      });
      showToast('Inventory updated');
    } catch {
      showToast('Failed to update inventory', 'error');
    } finally {
      setSaving(false);
    }
  };

  /** For when a shipment arrives — adds to the current count instead of asking the admin to do the math. */
  const onAddStock = async () => {
    if (!/^\d+$/.test(restockQty) || Number(restockQty) === 0) {
      setRestockError('Enter how many arrived');
      return;
    }
    setRestockError(undefined);

    const newQty = (Number(qtyOnHand) || 0) + Number(restockQty);
    setRestocking(true);
    try {
      await updateInventory(bookId, {
        qtyOnHand: newQty,
        reorderThreshold: Number(reorderThreshold) || 0,
      });
      setQtyOnHand(String(newQty));
      showToast(`Added ${restockQty} — now ${newQty} on hand`);
      setRestockQty('');
    } catch {
      showToast('Failed to add stock', 'error');
    } finally {
      setRestocking(false);
    }
  };

  return (
    <Card>
      <CardHeader
        title="Inventory"
        subtitle={
          inventory
            ? `${inventory.qtyReserved} reserved by open orders · ${available} available to sell`
            : 'No stock record yet — saving creates one.'
        }
        actions={
          <Badge variant={available > 0 ? 'success' : 'danger'} dot>
            {available > 0 ? 'In stock' : 'Out of stock'}
          </Badge>
        }
      />

      <div className={styles.restockRow}>
        <Input
          label="Stock arrived"
          hint="Units to add to the current count"
          placeholder="e.g. 20"
          value={restockQty}
          error={restockError}
          onChange={(e) => setRestockQty(e.target.value)}
        />
        <Button onClick={onAddStock} disabled={restocking}>
          <PackagePlus size={15} />
          {restocking ? 'Adding…' : 'Add stock'}
        </Button>
      </div>

      <div className={styles.divider} />

      <span className={styles.fileMeta}>Or set the exact totals directly:</span>

      <div className={styles.row2}>
        <Input
          label="Quantity on hand"
          hint="Physical count"
          value={qtyOnHand}
          error={errors.qtyOnHand}
          onChange={(e) => setQtyOnHand(e.target.value)}
        />
        <Input
          label="Reorder threshold"
          hint="Low-stock alert level"
          value={reorderThreshold}
          error={errors.reorderThreshold}
          onChange={(e) => setReorderThreshold(e.target.value)}
        />
      </div>

      <div style={{ marginTop: 'var(--space-4)' }}>
        <Button variant="secondary" onClick={onSave} disabled={saving}>
          <Package size={15} />
          {saving ? 'Saving…' : 'Save inventory'}
        </Button>
      </div>
    </Card>
  );
}
