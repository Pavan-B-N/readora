import { useEffect, useMemo, useState } from 'react';
import { Plus, Search, Users, Wand2 } from 'lucide-react';
import { createAuthor, listAuthors } from '@/api/catalogApi';
import type { Author } from '@/types/catalog';
import { slugify } from '@/utils/slugify';
import { useToast } from '@/components/Toast';
import { Card, CardHeader } from '@/components/Card';
import { Input, Textarea } from '@/components/Input';
import { Button } from '@/components/Button';
import { Tooltip } from '@/components/Tooltip';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@/components/EmptyState';
import styles from '../PublishersPage/PublishersPage.module.css';

export function AuthorsPage() {
  const { showToast } = useToast();
  const [authors, setAuthors] = useState<Author[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');

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
      setName('');
      setSlug('');
      setSlugTouched(false);
      setBio('');
      reload();
    } catch {
      showToast('Failed to create author — the slug may already exist', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader title="Authors" subtitle={`${authors.length} author${authors.length === 1 ? '' : 's'} in the catalogue.`} />

      <div className={styles.layout}>
        <Card>
          <div className={styles.searchWrap}>
            <Search size={15} className={styles.searchIcon} />
            <input
              className={styles.searchInput}
              placeholder="Filter authors…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>

          {loading ? (
            <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>
          ) : filtered.length === 0 ? (
            <EmptyState
              icon={Users}
              title={query ? 'No matches' : 'No authors yet'}
              description={query ? `Nothing matches “${query}”.` : 'Add the first author using the form.'}
            />
          ) : (
            <ul className={styles.list}>
              {filtered.map((a) => (
                <li className={styles.item} key={a.id}>
                  <span className={styles.itemIcon}>
                    <Users size={15} />
                  </span>
                  <span className={styles.itemText}>
                    <span className={styles.itemName}>{a.name}</span>
                    {a.bio ? (
                      <span className={styles.itemBio}>{a.bio}</span>
                    ) : (
                      <span className={styles.itemSlug}>/{a.slug}</span>
                    )}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardHeader title="New author" />
          <div className={styles.form}>
            <Input
              label="Name"
              required
              placeholder="e.g. Ursula K. Le Guin"
              value={name}
              error={errors.name}
              onChange={(e) => setName(e.target.value)}
            />
            <div className={styles.slugRow}>
              <Input
                label="Slug"
                required
                hint="URL segment"
                placeholder="ursula-k-le-guin"
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
              rows={4}
              placeholder="A short biography shown on the book page."
              value={bio}
              onChange={(e) => setBio(e.target.value)}
            />
            <Button onClick={onSubmit} disabled={saving} block>
              <Plus size={15} />
              {saving ? 'Creating…' : 'Create author'}
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
