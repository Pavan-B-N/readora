import { useEffect, useMemo, useState } from 'react';
import { Plus, Search, Users, Wand2 } from 'lucide-react';
import { createAuthor, listAuthors } from '@/api/catalogApi';
import type { Author } from '@/types/catalog';
import { slugify } from '@/utils/slugify';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input, Textarea } from '@/components/Input';
import { Button } from '@/components/Button';
import { Tooltip } from '@/components/Tooltip';
import { Modal } from '@/components/Modal';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@/components/EmptyState';
import formStyles from '../PublishersPage/PublishersPage.module.css';
import styles from './AuthorsPage.module.css';

export function AuthorsPage() {
  const { showToast } = useToast();
  const [authors, setAuthors] = useState<Author[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');

  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [slugTouched, setSlugTouched] = useState(false);
  const [bio, setBio] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const reload = () => {
    setLoading(true);
    listAuthors()
      .then(setAuthors)
      .finally(() => setLoading(false));
  };

  useEffect(reload, []);

  useEffect(() => {
    if (!slugTouched) setSlug(slugify(name));
  }, [name, slugTouched]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return q ? authors.filter((a) => a.name.toLowerCase().includes(q)) : authors;
  }, [authors, query]);

  const openDialog = () => {
    setName('');
    setSlug('');
    setSlugTouched(false);
    setBio('');
    setErrors({});
    setOpen(true);
  };

  const onSubmit = async () => {
    const next: Record<string, string> = {};
    if (!name.trim()) next.name = 'Name is required';
    if (!slug.trim()) next.slug = 'Slug is required';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSaving(true);
    try {
      await createAuthor({ name: name.trim(), slug: slug.trim(), bio: bio.trim() || null });
      showToast(`Author “${name.trim()}” created`);
      setOpen(false);
      reload();
    } catch {
      showToast('Failed to create author — the slug may already exist', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Authors"
        subtitle={`${authors.length} author${authors.length === 1 ? '' : 's'} in the catalogue.`}
        actions={
          <Button onClick={openDialog}>
            <Plus size={15} />
            Add author
          </Button>
        }
      />

      <div className={formStyles.searchWrap}>
        <Search size={15} className={formStyles.searchIcon} />
        <input
          className={formStyles.searchInput}
          placeholder="Filter authors…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      {loading ? (
        <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>
      ) : filtered.length === 0 ? (
        <Card>
          <EmptyState
            icon={Users}
            title={query ? 'No matches' : 'No authors yet'}
            description={query ? `Nothing matches “${query}”.` : 'Add the first author to get started.'}
            action={
              !query ? (
                <Button size="sm" onClick={openDialog}>
                  <Plus size={14} />
                  Add author
                </Button>
              ) : undefined
            }
          />
        </Card>
      ) : (
        <div className={styles.grid}>
          {filtered.map((a) => (
            <Card key={a.id} className={styles.authorCard}>
              {a.photoUrl ? (
                <img className={styles.avatarPhoto} src={a.photoUrl} alt={a.name} />
              ) : (
                <span className={styles.avatar}>
                  {a.name.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()}
                </span>
              )}
              <span className={styles.authorName}>{a.name}</span>
              {a.bio ? <span className={styles.authorBio}>{a.bio}</span> : <span className={styles.authorSlug}>/{a.slug}</span>}
            </Card>
          ))}
        </div>
      )}

      <Modal open={open} onClose={() => setOpen(false)} title="New author">
        <div className={formStyles.form}>
          <Input
            label="Name"
            required
            placeholder="e.g. Robert C. Martin"
            value={name}
            error={errors.name}
            onChange={(e) => setName(e.target.value)}
          />
          <div className={formStyles.slugRow}>
            <Input
              label="Slug"
              required
              hint="URL segment"
              placeholder="robert-c-martin"
              value={slug}
              error={errors.slug}
              onChange={(e) => {
                setSlugTouched(true);
                setSlug(e.target.value);
              }}
            />
            <Tooltip label="Regenerate from name">
              <Button
                variant="secondary"
                iconOnly
                aria-label="Regenerate slug"
                onClick={() => {
                  setSlugTouched(false);
                  setSlug(slugify(name));
                }}
              >
                <Wand2 size={15} />
              </Button>
            </Tooltip>
          </div>
          <Textarea
            label="Bio"
            hint="Optional"
            rows={3}
            placeholder="A short author bio…"
            value={bio}
            onChange={(e) => setBio(e.target.value)}
          />
          <Button onClick={onSubmit} disabled={saving} block>
            <Plus size={15} />
            {saving ? 'Creating…' : 'Create author'}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
