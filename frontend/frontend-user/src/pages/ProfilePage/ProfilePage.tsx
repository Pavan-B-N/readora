import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Check, MapPin, Pencil, Plus, Star, Trash2, X } from 'lucide-react';
import { addAddress, deleteAddress, getMe, listAddresses, setDefaultAddress, updateProfile } from '@/api/userApi';
import { listStores } from '@/api/catalogApi';
import type { Address, AddressLabel, AddressRecipientType, MeResponse } from '@/types/user';
import type { Store } from '@/types/catalog';
import { pickDefaultStore } from '@/utils/store';
import { useToast } from '@readora/shared-ui';
import { Card, CardHeader } from '@readora/shared-ui';
import { Input, Select } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { Badge } from '@readora/shared-ui';
import { Tooltip } from '@readora/shared-ui';
import { EmptyState } from '@readora/shared-ui';
import styles from './ProfilePage.module.css';

interface AddressForm {
  label: AddressLabel;
  recipientType: AddressRecipientType;
  recipientName: string;
  recipientPhone: string;
  line1: string;
  line2: string;
  isDefault: boolean;
}

function emptyForm(recipientName: string, recipientPhone: string): AddressForm {
  return {
    label: 'HOME',
    recipientType: 'OWNER',
    recipientName,
    recipientPhone,
    line1: '',
    line2: '',
    isDefault: false,
  };
}

export function ProfilePage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [me, setMe] = useState<MeResponse | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [store, setStore] = useState<Store | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState<AddressForm>(emptyForm('', ''));
  const [errors, setErrors] = useState<Partial<Record<keyof AddressForm, string>>>({});
  const [saving, setSaving] = useState(false);

  const [editingProfile, setEditingProfile] = useState(false);
  const [profileForm, setProfileForm] = useState({ displayName: '', phone: '' });
  const [savingProfile, setSavingProfile] = useState(false);

  const reloadAddresses = () => listAddresses().then(setAddresses);

  useEffect(() => {
    getMe().then(setMe);
    reloadAddresses();
    listStores().then((stores) => setStore(pickDefaultStore(stores)));
  }, []);

  const set = (patch: Partial<AddressForm>) => {
    setForm((f) => ({ ...f, ...patch }));
    setErrors((e) => {
      const next = { ...e };
      for (const key of Object.keys(patch)) delete next[key as keyof AddressForm];
      return next;
    });
  };

  const openForm = () => {
    setForm(emptyForm(me?.displayName ?? '', me?.phone ?? ''));
    setFormOpen(true);
  };

  const openProfileEdit = () => {
    setProfileForm({ displayName: me?.displayName ?? '', phone: me?.phone ?? '' });
    setEditingProfile(true);
  };

  const onSaveProfile = async () => {
    setSavingProfile(true);
    try {
      const updated = await updateProfile({
        displayName: profileForm.displayName.trim() || null,
        phone: profileForm.phone.trim() || null,
      });
      setMe(updated);
      showToast('Profile updated');
      setEditingProfile(false);
    } catch {
      showToast('Could not update your profile', 'error');
    } finally {
      setSavingProfile(false);
    }
  };

  const onRecipientTypeChange = (recipientType: AddressRecipientType) => {
    if (recipientType === 'OWNER') {
      set({ recipientType, recipientName: me?.displayName ?? '', recipientPhone: me?.phone ?? '' });
    } else {
      set({ recipientType, recipientName: '', recipientPhone: '' });
    }
  };

  const onSubmit = async () => {
    const next: Partial<Record<keyof AddressForm, string>> = {};
    if (!form.recipientName.trim()) next.recipientName = 'Required';
    if (!form.recipientPhone.trim()) next.recipientPhone = 'Required';
    if (!form.line1.trim()) next.line1 = 'Required';
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    if (!store) {
      showToast('No store available yet — try again shortly', 'error');
      return;
    }

    setSaving(true);
    try {
      await addAddress({
        label: form.label,
        recipientType: form.recipientType,
        recipientName: form.recipientName.trim(),
        recipientPhone: form.recipientPhone.trim(),
        line1: form.line1.trim(),
        line2: form.line2.trim() || undefined,
        city: store.city,
        state: store.state,
        postalCode: store.postalCode,
        countryCode: store.countryCode,
        storeId: store.id,
        isDefault: form.isDefault,
      });
      showToast('Address added');
      setFormOpen(false);
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

  const onSetDefault = async (id: string) => {
    try {
      await setDefaultAddress(id);
      reloadAddresses();
    } catch {
      showToast('Failed to set default address', 'error');
    }
  };

  const initials = me?.displayName
    ? me.displayName.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()
    : (me?.email.slice(0, 2).toUpperCase() ?? '?');

  return (
    <div>
      <div style={{ marginBottom: 'var(--space-2)' }}>
        <Button variant="ghost" onClick={() => navigate(-1)}>
          <ArrowLeft size={16} />
          Back
        </Button>
      </div>

      <div className={styles.layout}>
        <div className={styles.stack}>
          <div className={styles.section}>
            {me && (
              <>
                <div className={styles.sectionHeader}>
                  <h2 className={styles.sectionTitle}>Personal info</h2>
                  {!editingProfile && (
                    <Button variant="ghost" size="sm" iconOnly aria-label="Edit profile" onClick={openProfileEdit}>
                      <Pencil size={14} />
                    </Button>
                  )}
                </div>
                <div className={styles.identity}>
                  <span className={styles.avatar}>{initials}</span>
                  <span className={styles.identityText}>
                    <div className={styles.displayName}>{me.displayName ?? 'Reader'}</div>
                    <div className={styles.email}>{me.email}</div>
                  </span>
                </div>

                {editingProfile ? (
                  <div className={styles.form}>
                    <Input
                      label="Display name"
                      value={profileForm.displayName}
                      onChange={(e) => setProfileForm((f) => ({ ...f, displayName: e.target.value }))}
                    />
                    <Input
                      label="Phone"
                      value={profileForm.phone}
                      onChange={(e) => setProfileForm((f) => ({ ...f, phone: e.target.value }))}
                    />
                    <div className={styles.profileEditActions}>
                      <Button variant="secondary" onClick={() => setEditingProfile(false)} disabled={savingProfile}>
                        Cancel
                      </Button>
                      <Button onClick={onSaveProfile} disabled={savingProfile}>
                        {savingProfile ? 'Saving…' : 'Save'}
                      </Button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className={styles.profileRow}>
                      <span className={styles.profileLabel}>Phone</span>
                      <span>{me.phone ?? <span className={styles.missingValue}>Not set</span>}</span>
                    </div>
                    <div className={styles.profileRow}>
                      <span className={styles.profileLabel}>Wallet balance</span>
                      <span>
                        ₹{me.wallet.balance} {me.wallet.currency}
                      </span>
                    </div>
                    {store && (
                      <div className={styles.profileRow}>
                        <span className={styles.profileLabel}>Shopping from</span>
                        <span>{store.name}</span>
                      </div>
                    )}
                    {me.locale && (
                      <div className={styles.profileRow}>
                        <span className={styles.profileLabel}>Locale</span>
                        <span>{me.locale}</span>
                      </div>
                    )}
                  </>
                )}
              </>
            )}
          </div>

          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <div>
                <h2 className={styles.sectionTitle}>Addresses</h2>
                <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)' }}>{addresses.length} saved</span>
              </div>
              {!formOpen && (
                <Button variant="secondary" size="sm" onClick={openForm}>
                  <Plus size={14} />
                  Add new
                </Button>
              )}
            </div>
            {addresses.length === 0 && !formOpen ? (
              <EmptyState
                icon={MapPin}
                title="No saved addresses"
                description="Add one so checkout only ever asks for it once."
                action={
                  <Button size="sm" onClick={openForm}>
                    <Plus size={14} />
                    Add address
                  </Button>
                }
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
                      <Badge variant="neutral">{address.recipientType === 'OWNER' ? 'You' : 'Guest'}</Badge>
                      {address.isDefault && <Badge variant="info">Default</Badge>}
                    </span>
                    <span className={styles.addressText}>
                      {address.recipientName}
                      {address.recipientPhone ? ` · ${address.recipientPhone}` : ''}
                      <br />
                      {address.line1}
                      {address.line2 ? `, ${address.line2}` : ''}
                      <br />
                      {address.city}, {address.state} {address.postalCode}
                    </span>
                  </span>
                  {!address.isDefault && (
                    <Tooltip label="Set as default">
                      <button
                        type="button"
                        className={styles.defaultButton}
                        onClick={() => onSetDefault(address.id)}
                        aria-label={`Set ${address.label} address as default`}
                      >
                        <Star size={15} />
                      </button>
                    </Tooltip>
                  )}
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
          </div>
        </div>

        {formOpen && (
          <div className={styles.section}>
            <div className={styles.sectionHeader}>
              <h2 className={styles.sectionTitle}>Add address</h2>
              <Button variant="ghost" size="sm" iconOnly aria-label="Cancel" onClick={() => setFormOpen(false)}>
                <X size={15} />
              </Button>
            </div>
            <div className={styles.form}>
              {store && (
                <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-subtle)' }}>
                  Delivering from {store.name}, {store.city}. Just add the street-level details below.
                </p>
              )}

              <Select label="Label" value={form.label} onChange={(e) => set({ label: e.target.value as AddressLabel })}>
                <option value="HOME">Home</option>
                <option value="WORK">Work</option>
                <option value="OTHER">Other</option>
              </Select>

              <div className={styles.recipientTypeRow}>
                <button
                  type="button"
                  className={[styles.recipientTypeButton, form.recipientType === 'OWNER' && styles.recipientTypeActive]
                    .filter(Boolean)
                    .join(' ')}
                  onClick={() => onRecipientTypeChange('OWNER')}
                >
                  {form.recipientType === 'OWNER' && <Check size={13} />}
                  For me
                </button>
                <button
                  type="button"
                  className={[styles.recipientTypeButton, form.recipientType === 'GUEST' && styles.recipientTypeActive]
                    .filter(Boolean)
                    .join(' ')}
                  onClick={() => onRecipientTypeChange('GUEST')}
                >
                  {form.recipientType === 'GUEST' && <Check size={13} />}
                  For someone else
                </button>
              </div>

              <div className={styles.row2}>
                <Input
                  label="Recipient name"
                  required
                  disabled={form.recipientType === 'OWNER' && Boolean(me?.displayName)}
                  value={form.recipientName}
                  error={errors.recipientName}
                  onChange={(e) => set({ recipientName: e.target.value })}
                />
                <Input
                  label="Recipient phone"
                  required
                  disabled={form.recipientType === 'OWNER' && Boolean(me?.phone)}
                  value={form.recipientPhone}
                  error={errors.recipientPhone}
                  onChange={(e) => set({ recipientPhone: e.target.value })}
                />
              </div>

              <Input
                label="Street / door no."
                required
                placeholder="e.g. Flat 4B, 221 Residency Road"
                value={form.line1}
                error={errors.line1}
                onChange={(e) => set({ line1: e.target.value })}
              />
              <Input
                label="Landmark"
                placeholder="Optional"
                value={form.line2}
                onChange={(e) => set({ line2: e.target.value })}
              />

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
          </div>
        )}
      </div>
    </div>
  );
}
