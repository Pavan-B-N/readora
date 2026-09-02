import { useEffect, useState } from 'react';
import { BadgeCheck, MessageSquare } from 'lucide-react';
import { deleteOwnReview, getReviews, upsertReview } from '@/api/catalogApi';
import type { Review } from '@/types/catalog';
import { useAppSelector } from '@/redux/hooks';
import { useToast } from '@readora/shared-ui';
import { Button } from '@readora/shared-ui';
import { StarRating } from '@/components/StarRating';
import { ROUTES } from '@/constants/routes';
import { useNavigate } from 'react-router-dom';
import styles from './ReviewsSection.module.css';

export function ReviewsSection({ bookId }: { bookId: string }) {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const userId = useAppSelector((state) => state.auth.userId);

  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [draftRating, setDraftRating] = useState(0);
  const [draftComment, setDraftComment] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const reload = () => {
    setLoading(true);
    getReviews(bookId)
      .then((page) => setReviews(page.items ?? []))
      .finally(() => setLoading(false));
  };

  useEffect(reload, [bookId]);

  const myReview = reviews.find((r) => r.userId === userId);

  useEffect(() => {
    if (myReview) {
      setDraftRating(myReview.rating);
      setDraftComment(myReview.comment ?? '');
    }
  }, [myReview]);

  const onSubmit = async () => {
    if (!accessToken) {
      navigate(ROUTES.login, { state: { from: { pathname: ROUTES.bookDetail(bookId) } } });
      return;
    }
    if (draftRating === 0) {
      showToast('Pick a star rating first', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await upsertReview(bookId, draftRating, draftComment.trim() || null);
      showToast(myReview ? 'Review updated' : 'Review posted');
      reload();
    } catch {
      showToast('Could not save your review', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const onDelete = async () => {
    setSubmitting(true);
    try {
      await deleteOwnReview(bookId);
      setDraftRating(0);
      setDraftComment('');
      showToast('Review removed');
      reload();
    } catch {
      showToast('Could not remove your review', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.section}>
      <h2 className={styles.sectionTitle}>Reviews</h2>

      <div className={styles.form}>
        <span className={styles.formLabel}>{myReview ? 'Your review' : 'Write a review'}</span>
        <StarRating value={draftRating} size={20} onChange={setDraftRating} />
        <textarea
          className={styles.textarea}
          placeholder="What did you think of this book? (optional)"
          value={draftComment}
          onChange={(e) => setDraftComment(e.target.value)}
          rows={3}
        />
        <div className={styles.formActions}>
          <Button size="sm" onClick={onSubmit} disabled={submitting}>
            {submitting ? 'Saving…' : myReview ? 'Update review' : 'Post review'}
          </Button>
          {myReview && (
            <Button size="sm" variant="ghost" onClick={onDelete} disabled={submitting}>
              Delete
            </Button>
          )}
        </div>
      </div>

      {!loading && reviews.length === 0 ? (
        <p className={styles.empty}>
          <MessageSquare size={14} />
          No reviews yet — be the first.
        </p>
      ) : (
        <div className={styles.list}>
          {reviews.map((review) => (
            <div className={styles.review} key={review.id}>
              <div className={styles.reviewHeader}>
                <StarRating value={review.rating} size={13} />
                <span className={styles.reviewAuthor}>{review.authorDisplayName}</span>
                {review.verifiedPurchase && (
                  <span className={styles.verified}>
                    <BadgeCheck size={12} />
                    Verified purchase
                  </span>
                )}
                <span className={styles.reviewDate}>{new Date(review.createdAt).toLocaleDateString()}</span>
              </div>
              {review.comment && <p className={styles.reviewComment}>{review.comment}</p>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
