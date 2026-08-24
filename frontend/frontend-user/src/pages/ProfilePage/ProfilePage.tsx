import { useEffect, useState } from 'react';
import { MapPin, Plus, Trash2 } from 'lucide-react';
import { addAddress, deleteAddress, getMe, listAddresses } from '@/api/userApi';
import type { Address, AddressLabel, MeResponse } from '@/types/user';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Input, Select } from '@/components/Input';
import { Button } from '@/components/Button';
import { Badge } from '@/components/Badge';
import { Tooltip } from '@/components/Tooltip';
import { EmptyState } from '@/components/EmptyState';
import styles from './ProfilePage.module.css';

interface AddressForm {
  label: AddressLabel;
  recipientName: string;
  line1: string;
  line2: string;
  city: string;
  state: string;
  postalCode: string;
  countryCode: string;
  phone: string;
  isDefault: boolean;
}

const EMPTY_ADDRESS: AddressForm = {
  label: 'HOME',
  recipientName: '',
  line1: '',
  line2: '',
  city: '',
  state: '',
  postalCode: '',
  countryCode: '',
  phone: '',
  isDefault: false,
};

export function ProfilePage() {
  const { showToast } = useToast();
  const [me, setMe] = useState<MeResponse | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [form, setForm] = useState<AddressForm>(EMPTY_ADDRESS);
  const [errors, setErrors] = useState<Partial<Record<keyof AddressForm, string>>>({});
  const [saving, setSaving] = useState(false);

  const reloadAddresses = () => listAddresses().then(setAddresses);

  useEffect(() => {
    getMe().then(setMe);
    reloadAddresses();
  }, []);

  const set = (patch: Partial<AddressForm>) => {
    setForm((f) => ({ ...f, ...patch }));
    setErrors((e) => {
      const next = { ...e };
      for (const key of Object.keys(patch)) delete next[key as keyof AddressForm];
      return next;
    });
  };

  const onSubmit = async () => {
    const next: Partial<Record<keyof AddressForm, string>> = {};
    if (!form.recipientName.trim()) next.recipientName = 'Required';
    if (!form.line1.trim()) next.line1 = 'Required';
    if (!form.city.trim()) next.city = 'Required';
    if (!form.state.trim()) next.state = 'Required';
    if (!form.postalCode.trim()) next.postalCode = 'Required';
    if (form.countryCode.trim().length !== 2) next.countryCode = '2 letters';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSaving(true);
    try {
      await addAddress({
        ...form,
        line2: form.line2.trim() || undefined,
        phone: form.phone.trim() || undefined,
        countryCode: form.countryCode.trim().toUpperCase(),
      });
      showToast('Address added');
      setForm(EMPTY_ADDRESS);
      reloadAddresses();
    } catch {
      showToast('Failed to add address', 'error');
    } finally {
      setSaving(false);
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

  const initials = me?.displayName
    ? me.displayName.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()
    : (me?.email.slice(0, 2).toUpperCase() ?? '?');

  return (
    <div>
      <h1>Profile</h1>

      <div className={styles.layout}>
        <div className={styles.stack}>
          <Card>
            {me && (
              <>
                <div className={styles.identity}>
                  <span className={styles.avatar}>{initials}</span>
                  <span className={styles.identityText}>
                    <div className={styles.displayName}>{me.displayName ?? 'Reader'}</div>
                    <div className={styles.email}>{me.email}</div>
                  </span>
                </div>
                <div className={styles.profileRow}>
                  <span className={styles.profileLabel}>Wallet balance</span>
                  <span>
                    ₹{me.wallet.balance} {me.wallet.currency}
                  </span>
                </div>
                {me.locale && (
                  <div className={styles.profileRow}>
                    <span className={styles.profileLabel}>Locale</span>
                    <span>{me.locale}</span>
                  </div>
                )}
              </>
            )}
          </Card>

          <Card>
            <CardHeader title="Addresses" subtitle={`${addresses.length} saved`} />
            {addresses.length === 0 ? (
              <EmptyState
                icon={MapPin}
                title="No saved addresses"
                description="Add one here so checkout is quicker next time."
              />
            ) : (
              addresses.map((address) => (
                <div className={styles.addressCard} key={address.id}>
                  <span className={styles.addressIcon}>
                    <MapPin size={15} />
                  </span>
                  <span className={styles.addressBody}>
                    <span className={styles.addressLabel}>
                      {address.label.charAt(0) + address.label.slice(1).toLowerCase()}
                      {address.isDefault && <Badge variant="info">Default</Badge>}
                    </span>
                    <span className={styles.addressText}>
                      {address.recipientName}
                      <br />
                      {address.line1}
                      {address.line2 ? `, ${address.line2}` : ''}
                      <br />
                      {address.city}, {address.state} {address.postalCode}, {address.countryCode}
                    </span>
                  </span>
                  <Tooltip label="Remove address">
                    <button
                      type="button"
                      className={styles.removeButton}
                      onClick={() => onDelete(address.id)}
                      aria-label={`Remove ${address.label} address`}
                    >
                      <Trash2 size={15} />
                    </button>
                  </Tooltip>
                </div>
              ))
            )}
          </Card>
        </div>

        <Card>
          <CardHeader title="Add address" />
          <div className={styles.form}>
            <Select label="Label" value={form.label} onChange={(e) => set({ label: e.target.value as AddressLabel })}>
              <option value="HOME">Home</option>
              <option value="WORK">Work</option>
              <option value="OTHER">Other</option>
            </Select>
            <Input
              label="Recipient name"
              required
              value={form.recipientName}
              error={errors.recipientName}
              onChange={(e) => set({ recipientName: e.target.value })}
            />
            <Input
              label="Address line 1"
              required
              value={form.line1}
              error={errors.line1}
              onChange={(e) => set({ line1: e.target.value })}
            />
            <Input label="Address line 2" value={form.line2} onChange={(e) => set({ line2: e.target.value })} />
            <div className={styles.row2}>
              <Input
                label="City"
                required
                value={form.city}
                error={errors.city}
                onChange={(e) => set({ city: e.target.value })}
              />
              <Input
                label="State"
                required
                value={form.state}
                error={errors.state}
                onChange={(e) => set({ state: e.target.value })}
              />
            </div>
            <div className={styles.row2}>
              <Input
                label="Postal code"
                required
                value={form.postalCode}
                error={errors.postalCode}
                onChange={(e) => set({ postalCode: e.target.value })}
              />
              <Input
                label="Country"
                required
                hint="e.g. IN"
                value={form.countryCode}
                error={errors.countryCode}
                onChange={(e) => set({ countryCode: e.target.value })}
              />
            </div>
            <Input label="Phone" value={form.phone} onChange={(e) => set({ phone: e.target.value })} />
            <label className={styles.checkboxRow}>
              <input
                type="checkbox"
                checked={form.isDefault}
                onChange={(e) => set({ isDefault: e.target.checked })}
              />
              Set as default
            </label>
            <Button onClick={onSubmit} disabled={saving} block>
              <Plus size={15} />
              {saving ? 'Adding…' : 'Add address'}
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
