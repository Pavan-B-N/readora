import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  BookOpen,
  Check,
  ChevronRight,
  Download,
  Minus,
  Plus,
  Share2,
  ShoppingCart,
  Truck,
  User,
  Zap,
} from 'lucide-react';
import { getBookDetail, getLibrary, getRelatedBooks } from '@/api/catalogApi';
import { recordBookView } from '@/api/userApi';
import type { BookDetail, RelatedBook } from '@/types/catalog';
import type { DeliveryType } from '@/types/cart';
import { useAppDispatch, useAppSelector } from '@/redux/hooks';
import { addToCart, fetchCart, updateCartItemQty } from '@/redux/slices/cartSlice';
import { useToast } from '@/components/Toast';
import { Button } from '@/components/Button';
import { Badge } from '@/components/Badge';
import { Modal } from '@/components/Modal';
import { Spinner } from '@/components/Spinner';
import { StarRating } from '@/components/StarRating';
import { ROUTES } from '@/constants/routes';
import { ReviewsSection } from './ReviewsSection';
import styles from './BookDetailPage.module.css';

const MAX_PER_TITLE = 10;

export function BookDetailPage() {
  const { bookId } = useParams<{ bookId: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { showToast } = useToast();
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const cartItems = useAppSelector((state) => state.cart.items);
  const { selectedId: storeId, resolved: storeResolved } = useAppSelector((state) => state.store);

  const [book, setBook] = useState<BookDetail | null>(null);
  const [related, setRelated] = useState<RelatedBook[]>([]);
  const [busy, setBusy] = useState(false);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [ownedBookIds, setOwnedBookIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (!bookId || !storeResolved) return;
    getBookDetail(bookId, storeId ?? undefined).then(setBook);
    getRelatedBooks(bookId).then(setRelated);
  }, [bookId, storeId, storeResolved]);

  useEffect(() => {
    if (accessToken) dispatch(fetchCart());
  }, [accessToken, dispatch]);

  // Fire-and-forget — a failed history write shouldn't ever surface to the reader.
  useEffect(() => {
    if (accessToken && bookId) recordBookView(bookId).catch(() => {});
  }, [accessToken, bookId]);

  // Whether the signed-in caller already owns this book's virtual edition — if so, there's
  // nothing left to sell them here; the purchase UI gives way to a "Read now" prompt instead.
  useEffect(() => {
    if (!accessToken) {
      setOwnedBookIds(new Set());
      return;
    }
    getLibrary().then((books) => setOwnedBookIds(new Set(books.map((b) => b.id))));
  }, [accessToken]);

  // Keeps the browser tab and any share sheet that reads the live DOM (rather than a crawler
  // hitting the server-rendered OG tags in server.mjs) in sync with the book being viewed.
  useEffect(() => {
    if (!book) return;
    const previousTitle = document.title;
    document.title = `${book.title} — Readora`;

    const descriptionTag = document.querySelector('meta[name="description"]');
    const previousDescription = descriptionTag?.getAttribute('content') ?? null;
    if (descriptionTag) {
      descriptionTag.setAttribute(
        'content',
        book.description ?? `${book.title} by ${book.authors.map((a) => a.name).join(', ')} — available now on Readora.`,
      );
    }

    return () => {
      document.title = previousTitle;
      if (descriptionTag && previousDescription !== null) {
        descriptionTag.setAttribute('content', previousDescription);
      }
    };
  }, [book]);

  if (!book) return <Spinner />;

  const onCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      showToast('Link copied to clipboard');
    } catch {
      showToast('Could not copy link', 'error');
    }
  };

  const inStock = book.availability.status === 'IN_STOCK';
  const notAvailableAtStore = book.availability.status === 'NOT_AVAILABLE_AT_STORE';
  const noPhysicalEdition = book.availability.status === 'NO_PHYSICAL_EDITION';
  const ownsVirtual = ownedBookIds.has(book.id);
  // Once owned, the virtual edition is no longer something to buy — every purchase-flow check
  // below (the picker modal, the add-to-cart click) should behave exactly as if it didn't exist.
  const hasVirtual = book.virtualEdition !== null && !ownsVirtual;

  const physicalLine = cartItems.find((i) => i.bookId === book.id && i.deliveryType === 'PHYSICAL');
  const virtualLine = cartItems.find((i) => i.bookId === book.id && i.deliveryType === 'VIRTUAL');
  const physicalStockCap = Math.min(MAX_PER_TITLE, book.availability.quantityAvailable || 0);

  const addOne = async (deliveryType: DeliveryType) => {
    if (!accessToken) {
      navigate(ROUTES.login, { state: { from: { pathname: ROUTES.bookDetail(book.id) } } });
      return;
    }
    setBusy(true);
    try {
      await dispatch(
        addToCart({ bookId: book.id, qty: 1, deliveryType, storeId: storeId ?? undefined }),
      ).unwrap();
      showToast(`Added ${deliveryType === 'VIRTUAL' ? 'the virtual edition' : 'a physical copy'} to cart`);
      setPickerOpen(false);
    } catch {
      showToast('Could not add to cart', 'error');
    } finally {
      setBusy(false);
    }
  };

  const onAddToCartClick = () => {
    // Only actually a "choice" when both editions are real options here — a virtual-only book
    // (or one whose physical copy is out of stock / not stocked at this store) has exactly one
    // buyable edition, so there's nothing to pick between and the modal should never appear.
    if (hasVirtual && inStock) {
      setPickerOpen(true);
    } else if (hasVirtual) {
      addOne('VIRTUAL');
    } else {
      addOne('PHYSICAL');
    }
  };

  const changeQty = async (deliveryType: DeliveryType, nextQty: number) => {
    setBusy(true);
    try {
      await dispatch(updateCartItemQty({ bookId: book.id, deliveryType, qty: nextQty })).unwrap();
      showToast(nextQty === 0 ? 'Removed from cart' : 'Cart updated');
    } catch {
      showToast('Could not update the cart', 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <nav className={styles.breadcrumb}>
        <Link to={ROUTES.home}>Home</Link>
        {book.category && (
          <>
            <ChevronRight size={12} />
            <span>{book.category.name}</span>
          </>
        )}
        <ChevronRight size={12} />
        <span>{book.title}</span>
      </nav>

      <div className={styles.layout}>
        <div className={styles.coverColumn}>
          <div className={styles.cover}>
            {book.images[0] ? (
              <img src={book.images[0]} alt={book.title} />
            ) : (
              <span className={styles.coverFallback}>
                <BookOpen size={28} />
                <span className={styles.fallbackTitle}>{book.title}</span>
              </span>
            )}
          </div>
        </div>

        <div>
          <div className={styles.titleRow}>
            <h1 className={styles.title}>{book.title}</h1>
            <button
              type="button"
              className={styles.shareButton}
              onClick={onCopyLink}
              aria-label="Copy link to this book"
              title="Copy link"
            >
              <Share2 size={15} />
            </button>
          </div>
          {book.subtitle && <p className={styles.subtitle}>{book.subtitle}</p>}
          <p className={styles.authors}>
            by <span className={styles.authorName}>{book.authors.map((a) => a.name).join(', ')}</span>
          </p>

          {book.reviewCount > 0 && (
            <div className={styles.ratingSummary}>
              <StarRating value={book.averageRating ?? 0} size={15} />
              <span className={styles.ratingValue}>{book.averageRating?.toFixed(1)}</span>
              <span className={styles.ratingCount}>
                ({book.reviewCount} review{book.reviewCount === 1 ? '' : 's'})
              </span>
            </div>
          )}

          <div className={styles.metaRow}>
            {book.pageCount && <Badge>{book.pageCount} pages</Badge>}
            {book.language && <Badge>{book.language.toUpperCase()}</Badge>}
            {book.publisher && <Badge>{book.publisher.name}</Badge>}
            {hasVirtual && (
              <Badge variant="info">
                <Zap size={11} />
                Virtual edition available
              </Badge>
            )}
          </div>

          <div className={styles.purchaseBox}>
            {noPhysicalEdition && ownsVirtual ? (
              <>
                <div className={[styles.availability, styles.virtualOnly].join(' ')}>
                  <Check size={14} />
                  You own the virtual edition
                </div>
                <Button onClick={() => navigate(ROUTES.read(book.id))}>
                  <BookOpen size={15} />
                  Read now
                </Button>
              </>
            ) : (
              <>
            <div className={styles.priceRow}>
              <span className={styles.price}>₹{book.listPrice}</span>
              <span className={styles.currency}>{book.currency}</span>
            </div>

            {!noPhysicalEdition && (
              <div className={[styles.availability, inStock ? styles.inStock : styles.outOfStock].join(' ')}>
                {inStock ? (
                  <>
                    <Check size={14} />
                    In stock — {book.availability.quantityAvailable} available
                  </>
                ) : notAvailableAtStore ? (
                  `Not available for delivery from your store${book.store ? ` — only stocked at ${book.store.name}` : ''}`
                ) : (
                  'Currently out of stock'
                )}
              </div>
            )}
            {noPhysicalEdition && hasVirtual && (
              <div className={[styles.availability, styles.virtualOnly].join(' ')}>
                <Zap size={14} />
                Available as a virtual edition only
              </div>
            )}

            <div className={styles.purchaseRow}>
              {/* A virtual edition is a single digital copy — once it's in the cart there's
                  nothing left to add, so the button would just look unresponsive on a second
                  click. Only ever hidden for a virtual-only book; a mixed book still needs it
                  for adding physical copies even after the virtual one's in the cart. */}
              {!(noPhysicalEdition && virtualLine) && (
                <Button
                  onClick={onAddToCartClick}
                  disabled={((!inStock || notAvailableAtStore) && !hasVirtual) || busy}
                >
                  <ShoppingCart size={15} />
                  {busy ? 'Adding…' : 'Add to cart'}
                </Button>
              )}
              {(physicalLine || virtualLine) && (
                <Button variant="secondary" onClick={() => navigate(ROUTES.cart)}>
                  Go to cart
                </Button>
              )}
            </div>

            {physicalLine && (
              <div className={styles.inCartRow}>
                <div className={styles.qtyStepper}>
                  <button
                    type="button"
                    className={styles.qtyButton}
                    onClick={() => changeQty('PHYSICAL', physicalLine.qty - 1)}
                    disabled={busy}
                    aria-label={physicalLine.qty === 1 ? 'Remove physical copy' : 'Decrease physical quantity'}
                  >
                    <Minus size={13} />
                  </button>
                  <span className={styles.qtyValue}>{physicalLine.qty}</span>
                  <button
                    type="button"
                    className={styles.qtyButton}
                    onClick={() => changeQty('PHYSICAL', physicalLine.qty + 1)}
                    disabled={busy || physicalLine.qty >= physicalStockCap}
                    aria-label="Increase physical quantity"
                  >
                    <Plus size={13} />
                  </button>
                </div>
              </div>
            )}

            {virtualLine && (
              <div className={styles.inCartRow}>
                <button
                  type="button"
                  className={styles.qtyButton}
                  onClick={() => changeQty('VIRTUAL', 0)}
                  disabled={busy}
                  aria-label="Remove virtual edition"
                  title="A virtual edition is a single digital copy"
                >
                  <Minus size={13} />
                  In cart
                </button>
              </div>
            )}

            {inStock && <p className={styles.limitNote}>Up to {MAX_PER_TITLE} copies per title.</p>}
              </>
            )}
          </div>

          {book.description && (
            <div className={styles.section}>
              <h2 className={styles.sectionTitle}>About this book</h2>
              <p className={styles.description}>{book.description}</p>
            </div>
          )}

          {book.topics.length > 0 && (
            <div className={styles.section}>
              <h2 className={styles.sectionTitle}>Topics</h2>
              <div className={styles.topics}>
                {book.topics.map((topic) => (
                  <span className={styles.topicTag} key={topic}>
                    {topic}
                  </span>
                ))}
              </div>
            </div>
          )}

          {book.authors.some((a) => a.bio) && (
            <div className={styles.section}>
              <h2 className={styles.sectionTitle}>About the author{book.authors.length > 1 ? 's' : ''}</h2>
              <div className={styles.authorBios}>
                {book.authors
                  .filter((a) => a.bio)
                  .map((author) => (
                    <div className={styles.authorBio} key={author.id}>
                      <div className={styles.authorBioHeader}>
                        {author.photoUrl ? (
                          <img className={styles.authorAvatar} src={author.photoUrl} alt={author.name} />
                        ) : (
                          <span className={styles.authorAvatarFallback}>
                            <User size={18} />
                          </span>
                        )}
                        <span className={styles.authorBioName}>{author.name}</span>
                      </div>
                      <p className={styles.authorBioText}>{author.bio}</p>
                    </div>
                  ))}
              </div>
            </div>
          )}

          <div className={styles.specs}>
            <div className={styles.spec}>
              <span className={styles.specLabel}>ISBN-13</span>
              <span className={styles.specValue}>{book.isbn13}</span>
            </div>
            {book.publishedOn && (
              <div className={styles.spec}>
                <span className={styles.specLabel}>Published</span>
                <span className={styles.specValue}>{new Date(book.publishedOn).toLocaleDateString()}</span>
              </div>
            )}
            <div className={styles.spec}>
              <span className={styles.specLabel}>Delivery</span>
              <span className={styles.specValue}>~30 min from your store</span>
            </div>
          </div>

          <ReviewsSection bookId={book.id} />

          {related.length > 0 && (
            <div className={styles.section}>
              <h2 className={styles.sectionTitle}>Related reads</h2>
              <div className={styles.relatedGrid}>
                {related.map((r) => (
                  <Link key={r.id} to={ROUTES.bookDetail(r.id)} className={styles.relatedCard}>
                    <div className={styles.relatedCover}>
                      {r.coverImageUrl ? <img src={r.coverImageUrl} alt="" /> : <BookOpen size={18} />}
                    </div>
                    <span className={styles.relatedTitle}>{r.title}</span>
                    <span className={styles.relatedPrice}>₹{r.listPrice}</span>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      <Modal open={pickerOpen} onClose={() => setPickerOpen(false)} title="Choose an edition" width={480}>
        <div className={styles.editionChoices}>
          {inStock && (
            <button type="button" className={styles.editionCard} onClick={() => addOne('PHYSICAL')} disabled={busy}>
              <span className={styles.editionIcon}>
                <Truck size={18} />
              </span>
              <span className={styles.editionText}>
                <span className={styles.editionName}>Physical copy</span>
                <span className={styles.editionHint}>Delivered in ~30 min from your store</span>
              </span>
              <span className={styles.editionPrice}>
                ₹{book.listPrice} <span>{book.currency}</span>
              </span>
            </button>
          )}
          {book.virtualEdition && (
            <button type="button" className={styles.editionCard} onClick={() => addOne('VIRTUAL')} disabled={busy}>
              <span className={styles.editionIcon}>
                <Download size={18} />
              </span>
              <span className={styles.editionText}>
                <span className={styles.editionName}>Virtual edition</span>
                <span className={styles.editionHint}>Available instantly, read in-app</span>
              </span>
              <span className={styles.editionPrice}>
                ₹{book.virtualEdition.price} <span>{book.virtualEdition.currency}</span>
              </span>
            </button>
          )}
        </div>
      </Modal>
    </div>
  );
}
