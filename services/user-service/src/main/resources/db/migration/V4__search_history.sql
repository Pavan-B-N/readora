CREATE TABLE users.search_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    query character varying(255) NOT NULL,
    searched_at timestamp with time zone NOT NULL,
    CONSTRAINT uq_search_history_user_query UNIQUE (user_id, query)
);

CREATE INDEX idx_search_history_user_searched_at ON users.search_history (user_id, searched_at DESC);
