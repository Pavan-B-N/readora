import { useEffect, useMemo, useState } from 'react';
import { Pencil, Plus, Search, Trash2, Users, Wand2 } from 'lucide-react';
import { createAuthor, deleteAuthor, listAuthors, updateAuthor } from '@/api/catalogApi';
import { extractErrorMessage } from '@/api/client';
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
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [slugTouched, setSlugTouched] = useState(false);
  const [bio, setBio] = useState('');
  const [photoUrl, setPhotoUrl] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

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

  const openCreateDialog = () => {
    setEditingId(null);
    setName('');
    setSlug('');
    setSlugTouched(false);
    setBio('');
    setPhotoUrl('');
    setErrors({});
    setOpen(true);
  };

  const openEditDialog = (author: Author) => {
    setEditingId(author.id);
    setName(author.name);
    setSlug(author.slug);
    setSlugTouched(true);
    setBio(author.bio ?? '');
    setPhotoUrl(author.photoUrl ?? '');
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
      if (editingId) {
        await updateAuthor(editingId, {
          name: name.trim(),
          slug: slug.trim(),
          bio: bio.trim() || null,
          photoUrl: photoUrl.trim() || null,
        });
        showToast(`Author “${name.trim()}” updated`);
      } else {
        await createAuthor({
          name: name.trim(),
          slug: slug.trim(),
          bio: bio.trim() || null,
          photoUrl: photoUrl.trim() || null,
        });
        showToast(`Author “${name.trim()}” created`);
      }
      setOpen(false);
      reload();
    } catch {
      showToast(
        editingId ? 'Failed to update author — the slug may already exist' : 'Failed to create author — the slug may already exist',
        'error',
      );
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async (author: Author) => {
    if (!window.confirm(`Delete "${author.name}"? This can't be undone.`)) return;
    setDeletingId(author.id);
    try {
      await deleteAuthor(author.id);
      showToast(`Author “${author.name}” deleted`);
      reload();
    } catch (error) {
      showToast(extractErrorMessage(error, 'Could not delete this author'), 'error');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div>
      <PageHeader
        title="Authors"
        subtitle={`${authors.length} author${authors.length === 1 ? '' : 's'} in the catalogue.`}
        actions={
          <Button onClick={openCreateDialog}>
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
                <Button size="sm" onClick={openCreateDialog}>
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
              <span className={styles.cardActions}>
                <Tooltip label="Edit">
                  <Button variant="ghost" size="sm" iconOnly aria-label="Edit author" onClick={() => openEditDialog(a)}>
                    <Pencil size={13} />
                  </Button>
                </Tooltip>
                <Tooltip label="Delete">
                  <Button
                    variant="ghost"
                    size="sm"
                    iconOnly
                    aria-label="Delete author"
                    onClick={() => onDelete(a)}
                    disabled={deletingId === a.id}
                  >
                    <Trash2 size={13} />
                  </Button>
                </Tooltip>
              </span>

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

      <Modal open={open} onClose={() => setOpen(false)} title={editingId ? 'Edit author' : 'New author'}>
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
          <Input
            label="Photo URL"
            hint="Optional"
            placeholder="https://…"
            value={photoUrl}
            onChange={(e) => setPhotoUrl(e.target.value)}
          />
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
            {saving ? (editingId ? 'Saving…' : 'Creating…') : editingId ? 'Save changes' : 'Create author'}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
