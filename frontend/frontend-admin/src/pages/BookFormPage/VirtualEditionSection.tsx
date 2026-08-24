import { useState } from 'react';
import { Download, PowerOff } from 'lucide-react';
import { deactivateVirtualEdition, upsertVirtualEdition } from '@/api/catalogApi';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Input, Select } from '@/components/Input';
import { Button } from '@/components/Button';
import { Badge } from '@/components/Badge';
import type { AdminBookDetail, VirtualFileFormat } from '@/types/catalog';
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
  const [fileFormat, setFileFormat] = useState<VirtualFileFormat>(virtualEdition?.fileFormat ?? 'PDF');
  const [fileSizeBytes, setFileSizeBytes] = useState(
    virtualEdition?.fileSizeBytes != null ? String(virtualEdition.fileSizeBytes) : '',
  );
  const [price, setPrice] = useState(virtualEdition?.price ?? '');
  const [currency, setCurrency] = useState(virtualEdition?.currency ?? 'INR');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const onSave = async () => {
    const next: Record<string, string> = {};
    if (!fileUrl.trim()) next.fileUrl = 'File URL is required';
    if (!price.trim()) next.price = 'Price is required';
    else if (Number.isNaN(Number(price))) next.price = 'Must be a number';
    if (currency.trim().length !== 3) next.currency = 'Use a 3-letter code';
    if (fileSizeBytes && !/^\d+$/.test(fileSizeBytes)) next.fileSizeBytes = 'Whole number';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSaving(true);
    try {
      await upsertVirtualEdition(bookId, {
        fileUrl: fileUrl.trim(),
        fileFormat,
        fileSizeBytes: fileSizeBytes ? Number(fileSizeBytes) : null,
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

        <div className={styles.row3}>
          <Select
            label="File format"
            value={fileFormat}
            onChange={(e) => setFileFormat(e.target.value as VirtualFileFormat)}
          >
            <option value="PDF">PDF</option>
            <option value="EPUB">EPUB</option>
          </Select>
          <Input
            label="File size"
            hint="bytes"
            placeholder="512000"
            value={fileSizeBytes}
            error={errors.fileSizeBytes}
            onChange={(e) => setFileSizeBytes(e.target.value)}
          />
          <Input
            label="Currency"
            required
            value={currency}
            error={errors.currency}
            onChange={(e) => setCurrency(e.target.value)}
          />
        </div>

        <Input
          label="Price"
          required
          hint="Can differ from the physical list price"
          placeholder="249.00"
          value={price}
          error={errors.price}
          onChange={(e) => setPrice(e.target.value)}
        />

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
