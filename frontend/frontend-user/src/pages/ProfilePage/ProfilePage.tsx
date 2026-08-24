import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { X } from 'lucide-react';
import { addAddress, deleteAddress, getMe, listAddresses } from '@/api/userApi';
import type { Address, MeResponse } from '@/types/user';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input, Select } from '@/components/Input';
import { Button } from '@/components/Button';
import styles from './ProfilePage.module.css';

const addressSchema = z.object({
  label: z.enum(['HOME', 'WORK', 'OTHER']),
  recipientName: z.string().min(1, 'Required'),
  line1: z.string().min(1, 'Required'),
  line2: z.string().optional(),
  city: z.string().min(1, 'Required'),
  state: z.string().min(1, 'Required'),
  postalCode: z.string().min(1, 'Required'),
  countryCode: z.string().length(2, 'Use a 2-letter country code'),
  phone: z.string().optional(),
  isDefault: z.boolean(),
});

type AddressFormValues = z.infer<typeof addressSchema>;

export function ProfilePage() {
  const { showToast } = useToast();
  const [me, setMe] = useState<MeResponse | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<AddressFormValues>({ resolver: zodResolver(addressSchema), defaultValues: { label: 'HOME', isDefault: false } });

  const reloadAddresses = () => listAddresses().then(setAddresses);

  useEffect(() => {
    getMe().then(setMe);
    reloadAddresses();
  }, []);

  const onSubmit = async (values: AddressFormValues) => {
    try {
      await addAddress(values);
      showToast('Address added');
      reset({ label: 'HOME', isDefault: false, recipientName: '', line1: '', line2: '', city: '', state: '', postalCode: '', countryCode: '', phone: '' });
      reloadAddresses();
    } catch {
      showToast('Failed to add address', 'error');
    }
  };

  const onDelete = async (id: string) => {
    try {
      await deleteAddress(id);
      showToast('Address removed');
      reloadAddresses();
    } catch {
      showToast('Failed to remove address', 'error');
    }
  };

  return (
    <div>
      <h1>Profile</h1>
      <div className={styles.layout}>
        <div>
          <Card>
            <h2>Account</h2>
            {me && (
              <>
                <div className={styles.profileRow}>
                  <span className={styles.profileLabel}>Name:</span>
                  {me.displayName ?? '—'}
                </div>
                <div className={styles.profileRow}>
                  <span className={styles.profileLabel}>Email:</span>
                  {me.email}
                </div>
                <div className={styles.profileRow}>
                  <span className={styles.profileLabel}>Wallet balance:</span>
                  {me.wallet.balance} {me.wallet.currency}
                </div>
              </>
            )}
          </Card>

          <Card>
            <h2>Addresses</h2>
            {addresses.length === 0 ? (
              <p>No saved addresses.</p>
            ) : (
              addresses.map((address) => (
                <div className={styles.addressCard} key={address.id}>
                  <button className={styles.removeButton} onClick={() => onDelete(address.id)} aria-label="Remove address">
                    <X size={14} />
                  </button>
                  <div className={styles.addressLabel}>
                    {address.label}
                    {address.isDefault && <span className={styles.defaultBadge}>Default</span>}
                  </div>
                  {address.recipientName}, {address.line1}, {address.city}, {address.postalCode}, {address.countryCode}
                </div>
              ))
            )}
          </Card>
        </div>

        <Card>
          <h2>Add address</h2>
          <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
            <Select label="Label" {...register('label')}>
              <option value="HOME">Home</option>
              <option value="WORK">Work</option>
              <option value="OTHER">Other</option>
            </Select>
            <Input label="Recipient name" error={errors.recipientName?.message} {...register('recipientName')} />
            <Input label="Address line 1" error={errors.line1?.message} {...register('line1')} />
            <Input label="Address line 2" {...register('line2')} />
            <Input label="City" error={errors.city?.message} {...register('city')} />
            <Input label="State" error={errors.state?.message} {...register('state')} />
            <Input label="Postal code" error={errors.postalCode?.message} {...register('postalCode')} />
            <Input label="Country code (e.g. US)" error={errors.countryCode?.message} {...register('countryCode')} />
            <Input label="Phone" {...register('phone')} />
            <label>
              <input type="checkbox" {...register('isDefault')} /> Set as default
            </label>
            <Button type="submit" disabled={isSubmitting}>
              Add address
            </Button>
          </form>
        </Card>
      </div>
    </div>
  );
}
