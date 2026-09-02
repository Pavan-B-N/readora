import { useState } from 'react';
import { Download, PowerOff } from 'lucide-react';
import { deactivateVirtualEdition, upsertVirtualEdition } from '@/api/catalogApi';
import { useToast } from '@readora/shared-ui';
import { Card, CardHeader } from '@readora/shared-ui';
import { Input } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import type { AdminBookDetail } from '@/types/catalog';
import styles from './BookFormPage.module.css';

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
  const [fileUrl, setFileUrl] = useState(virtualEdition?.fileUrl ?? '');
  const [price, setPrice] = useState(virtualEdition?.price != null ? String(virtualEdition.price) : '');
  const [currency, setCurrency] = useState(virtualEdition?.currency ?? 'INR');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const onSave = async () => {
    const next: Record<string, string> = {};
    if (!fileUrl.trim()) next.fileUrl = 'File URL is required';
    if (!price.trim()) next.price = 'Price is required';
    else if (Number.isNaN(Number(price))) next.price = 'Must be a number';
    if (currency.trim().length !== 3) next.currency = 'Use a 3-letter code';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSaving(true);
    try {
      await upsertVirtualEdition(bookId, {
        fileUrl: fileUrl.trim(),
        fileFormat: 'PDF',
        fileSizeBytes: null,
        price: price.trim(),
        currency: currency.trim().toUpperCase(),
      });
      showToast('Virtual edition saved');
      onChanged();
    } catch {
      showToast('Failed to save virtual edition', 'error');
    } finally {
      setSaving(false);
    }
  };

  const onDeactivate = async () => {
    try {
      await deactivateVirtualEdition(bookId);
      showToast('Virtual edition deactivated');
      onChanged();
    } catch {
      showToast('Failed to deactivate', 'error');
    }
  };

  return (
    <Card>
      <CardHeader
        title="Virtual edition"
        subtitle={
          virtualEdition
            ? virtualEdition.isActive
              ? 'Customers can buy the digital edition.'
              : 'Deactivated — saving below reactivates it.'
            : 'No virtual edition yet. Add one to sell this book digitally.'
        }
        actions={
          virtualEdition && (
            <Badge variant={virtualEdition.isActive ? 'success' : 'neutral'} dot>
              {virtualEdition.isActive ? 'Active' : 'Inactive'}
            </Badge>
          )
        }
      />

      <div className={styles.form}>
        <Input
          label="File URL"
          required
          hint="Storage key — a signed URL is generated at delivery"
          placeholder="s3://readora-virtual-editions/9780451524935.epub"
          value={fileUrl}
          error={errors.fileUrl}
          onChange={(e) => setFileUrl(e.target.value)}
        />

        <div className={styles.row2}>
          <Input
            label="Price"
            required
            hint="Can differ from the physical list price"
            placeholder="249.00"
            value={price}
            error={errors.price}
            onChange={(e) => setPrice(e.target.value)}
          />
          <Input
            label="Currency"
            required
            value={currency}
            error={errors.currency}
            onChange={(e) => setCurrency(e.target.value)}
          />
        </div>

        <p className={styles.fileMeta}>
          PDF only for now. File size is detected automatically when saved.
        </p>

        <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
          <Button onClick={onSave} disabled={saving}>
            <Download size={15} />
            {saving ? 'Saving…' : 'Save virtual edition'}
          </Button>
          {virtualEdition?.isActive && (
            <Button variant="danger" onClick={onDeactivate}>
              <PowerOff size={15} />
              Deactivate
            </Button>
          )}
        </div>
      </div>
    </Card>
  );
}
