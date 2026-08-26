import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LogOut, Save, ShieldCheck } from 'lucide-react';
import { getMe, updateProfile } from '@/api/userApi';
import { listStores } from '@/api/catalogApi';
import type { MeResponse } from '@/types/user';
import type { Store } from '@/types/catalog';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { loggedOut } from '@/redux/slices/authSlice';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Input, Select } from '@/components/Input';
import { Button } from '@/components/Button';
import { Badge } from '@/components/Badge';
import { PageHeader } from '@/components/PageHeader';
import { ROUTES } from '@/constants/routes';
import styles from './ProfilePage.module.css';

export function ProfilePage() {
  const { showToast } = useToast();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const roles = useAppSelector((state) => state.auth.roles);

  const [me, setMe] = useState<MeResponse | null>(null);
  const [stores, setStores] = useState<Store[]>([]);
  const [displayName, setDisplayName] = useState('');
  const [phone, setPhone] = useState('');
  const [preferredStoreId, setPreferredStoreId] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    Promise.all([getMe(), listStores()]).then(([meResult, storeList]) => {
      setMe(meResult);
      setStores(storeList);
      setDisplayName(meResult.displayName ?? '');
      setPhone(meResult.phone ?? '');
      setPreferredStoreId(meResult.preferredStoreId ?? '');
    });
  }, []);

  const onSave = async () => {
    setSaving(true);
    try {
      const updated = await updateProfile({
        displayName: displayName.trim() || null,
        phone: phone.trim() || null,
        preferredStoreId: preferredStoreId || null,
      });
      setMe(updated);
      showToast('Profile updated');
    } catch {
      showToast('Failed to update profile', 'error');
    } finally {
      setSaving(false);
    }
  };

  const onLogout = () => {
    dispatch(loggedOut());
    navigate(ROUTES.login, { replace: true });
  };

  const initials = me?.displayName
    ? me.displayName.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()
    : (me?.email.slice(0, 2).toUpperCase() ?? '?');

  if (!me) return <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>;

  return (
    <div>
      <PageHeader title="Profile" subtitle="Your account, store assignment, and session." />

      <div className={styles.layout}>
        <Card>
          <div className={styles.identity}>
            <span className={styles.avatar}>{initials}</span>
            <span className={styles.identityText}>
              <span className={styles.displayName}>{me.displayName ?? me.email}</span>
              <span className={styles.email}>{me.email}</span>
            </span>
          </div>

          <div className={styles.roles}>
            {roles.map((role) => (
              <Badge key={role} variant="info">
                <ShieldCheck size={11} />
                {role}
              </Badge>
            ))}
          </div>

          <Button variant="danger" onClick={onLogout} block>
            <LogOut size={15} />
            Log out
          </Button>
        </Card>

        <Card>
          <CardHeader title="Basic options" subtitle="Shown to other admins in audit trails." />

          <div className={styles.form}>
            <Input
              label="Display name"
              placeholder="e.g. Priya Sharma"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
            <Input
              label="Phone"
              placeholder="Optional"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
            <Select
              label="Assigned store"
              hint="Books you list are scoped to this store"
              value={preferredStoreId}
              onChange={(e) => setPreferredStoreId(e.target.value)}
            >
              <option value="">Unassigned</option>
              {stores.map((store) => (
                <option key={store.id} value={store.id}>
                  {store.name} — {store.city}
                </option>
              ))}
            </Select>

            <Button onClick={onSave} disabled={saving} block>
              <Save size={15} />
              {saving ? 'Saving…' : 'Save changes'}
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
