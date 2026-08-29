CREATE TABLE users.browsing_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    book_id uuid NOT NULL,
    viewed_at timestamp with time zone NOT NULL,
    CONSTRAINT uq_browsing_history_user_book UNIQUE (user_id, book_id)
);

CREATE INDEX idx_browsing_history_user_viewed_at ON users.browsing_history (user_id, viewed_at DESC);
