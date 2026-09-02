import { useEffect, useState } from 'react';
import { BadgeCheck, Star, Trash2 } from 'lucide-react';
import { deleteReview, getBookReviews } from '@/api/catalogApi';
import type { Review } from '@/types/catalog';
import { useToast } from '@readora/shared-ui';
import { Card, CardHeader } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import styles from './BookFormPage.module.css';

export function ReviewsSection({ bookId }: { bookId: string }) {
  const { showToast } = useToast();
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const reload = () => {
    setLoading(true);
    getBookReviews(bookId)
      .then((page) => setReviews(page.items))
      .finally(() => setLoading(false));
  };

  useEffect(reload, [bookId]);

  const onDelete = async (reviewId: string) => {
    setDeletingId(reviewId);
    try {
      await deleteReview(reviewId);
      showToast('Review deleted');
      reload();
    } catch {
      showToast('Failed to delete review', 'error');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <Card>
      <CardHeader title="Reviews" subtitle="Moderation — remove any review that violates content policy." />

      {loading ? (
        <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>
      ) : reviews.length === 0 ? (
        <p style={{ color: 'var(--color-text-muted)' }}>No reviews yet.</p>
      ) : (
        <div className={styles.reviewList}>
          {reviews.map((review) => (
            <div className={styles.reviewRow} key={review.id}>
              <div className={styles.reviewMeta}>
                <span className={styles.reviewStars}>
                  {[1, 2, 3, 4, 5].map((n) => (
                    <Star key={n} size={13} fill={n <= review.rating ? 'currentColor' : 'none'} />
                  ))}
                </span>
                <span className={styles.reviewAuthor}>{review.authorDisplayName}</span>
                {review.verifiedPurchase && (
                  <span className={styles.reviewVerified}>
                    <BadgeCheck size={12} />
                    Verified
                  </span>
                )}
                <span className={styles.reviewDate}>{new Date(review.createdAt).toLocaleDateString()}</span>
              </div>
              {review.comment && <p className={styles.reviewComment}>{review.comment}</p>}
              <Button variant="ghost" size="sm" onClick={() => onDelete(review.id)} disabled={deletingId === review.id}>
                <Trash2 size={13} />
                {deletingId === review.id ? 'Deleting…' : 'Delete'}
              </Button>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}
