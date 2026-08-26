-- ============================================================================
-- READORA DATABASE SETUP + SEED
-- PostgreSQL 18
--
-- Initial connection:
--   postgres, as a superuser (needed only for step 1's CREATE DATABASE — the
--   readora role itself doesn't have CREATEDB)
--
-- This script:
--   1. Creates readora if it does not exist (as the superuser).
--   2. Reconnects to readora, still as the superuser — required for step 3's
--      CREATE EXTENSION vector.
--   3. Creates required extensions.
--   4. Reconnects again, this time AS THE readora ROLE (see the comment right
--      before that reconnect for why this matters) — every schema, table,
--      and row from here on must be owned by readora, not the superuser.
--   5. Creates the six service schemas.
--   6. Creates all application tables.
--   7. Creates useful indexes.
--   8. Loads development data from data/*.json.
--
-- Run from:
--   dataset/
--
-- Command:
--   PGPASSWORD=readora psql -h localhost -p 5432 -U "$(whoami)" -d postgres \
--        -v ON_ERROR_STOP=1 -f seed.sql
--
-- PGPASSWORD is required here even though the initial connection (as your OS
-- superuser) likely doesn't need one — it's what lets the \connect in step 2
-- authenticate as readora without an interactive password prompt.
--
-- ============================================================================


-- ============================================================================
-- 1. DATABASE
-- ============================================================================

-- Create readora only when it does not already exist.
SELECT 'CREATE DATABASE readora OWNER readora'
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_database
    WHERE datname = 'readora'
)
\gexec


-- ============================================================================
-- 2. CONNECT TO READORA
-- ============================================================================

-- Switch from the postgres database to readora — still as the superuser for
-- now, because creating the vector extension below requires it (pgcrypto
-- happens not to, but keep both here together rather than splitting them).
-- The switch to the readora role happens after, in section 4.
\connect readora


-- ============================================================================
-- 3. EXTENSIONS
-- ============================================================================

-- Provides gen_random_uuid().
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Provides the vector type used by Spring AI.
CREATE EXTENSION IF NOT EXISTS vector;


-- ============================================================================
-- 4. SCHEMAS
-- ============================================================================

-- NOW switch to the readora role — everything from here on (schemas, tables,
-- indexes, seed data) must be owned by readora, since that's who every
-- service's own datasource connects as. Reconnecting with plain `\connect
-- readora` would silently keep the superuser role instead, which is exactly
-- what caused every table to end up owned by the OS superuser rather than
-- readora before this was fixed — every service's own JDBC connection then
-- gets "permission denied for schema ..." trying to touch its own tables.
\connect readora readora

-- Create the six service-owned schemas.
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS commerce;
CREATE SCHEMA IF NOT EXISTS payments;
CREATE SCHEMA IF NOT EXISTS notifications;
CREATE SCHEMA IF NOT EXISTS ai;


-- ============================================================================
-- 5. TABLES
-- ============================================================================

BEGIN;


-- ============================================================================
-- AUTH SERVICE
-- ============================================================================


-- Stores authentication credentials and account status.
CREATE TABLE IF NOT EXISTS auth.users (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 text NOT NULL UNIQUE,
    password_hash         text NOT NULL,
    status                varchar NOT NULL DEFAULT 'ACTIVE',
    email_verified        boolean NOT NULL DEFAULT false,
    failed_login_attempts int NOT NULL DEFAULT 0,
    last_login_at         timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_auth_users_status
        CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
);


-- Stores the fixed application roles.
CREATE TABLE IF NOT EXISTS auth.roles (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code        varchar NOT NULL UNIQUE,
    description text,

    CONSTRAINT chk_auth_roles_code
        CHECK (code IN ('CUSTOMER', 'ADMIN'))
);


-- Maps users to their roles.
CREATE TABLE IF NOT EXISTS auth.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES auth.users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES auth.roles(id)
        ON DELETE CASCADE
);


-- Stores hashed refresh tokens and their revocation state.
CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL,
    token_hash text NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    user_agent text,
    ip_address text,

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES auth.users(id)
        ON DELETE CASCADE
);


-- ============================================================================
-- USER SERVICE
-- ============================================================================


-- Stores profile information owned by user-service.
CREATE TABLE IF NOT EXISTS users.user_profiles (
    user_id               uuid PRIMARY KEY,
    display_name          text,
    avatar_url            text,
    phone                 text,
    date_of_birth         date,
    locale                text,
    marketing_opt_in      boolean NOT NULL DEFAULT false,
    preferred_store_id    uuid,
    favorite_category_ids text
);


-- Stores the user's current wallet balance.
CREATE TABLE IF NOT EXISTS users.wallet_accounts (
    user_id    uuid PRIMARY KEY,
    balance    numeric(10, 2) NOT NULL DEFAULT 0,
    currency   varchar(3) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);


-- Stores the immutable wallet transaction ledger.
CREATE TABLE IF NOT EXISTS users.wallet_transactions (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL,
    order_id        uuid,
    amount          numeric(10, 2) NOT NULL,
    type            varchar NOT NULL,
    balance_after   numeric(10, 2) NOT NULL,
    idempotency_key text NOT NULL UNIQUE,
    created_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_wallet_transaction_type
        CHECK (
            type IN (
                'SIGNUP_BONUS',
                'REFERRAL_BONUS',
                'REDEEMED',
                'REVERSED',
                'TOPUP',
                'COUPON_REDEEMED'
            )
        )
);


-- Stores wallet-credit coupon codes, Amazon-Pay-style.
CREATE TABLE IF NOT EXISTS users.coupons (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code       text NOT NULL UNIQUE,
    amount     numeric(10, 2) NOT NULL,
    is_active  boolean NOT NULL DEFAULT true,
    expires_at timestamptz
);


-- Tracks that a user already redeemed a given coupon — one redemption per user per coupon.
CREATE TABLE IF NOT EXISTS users.coupon_redemptions (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id    uuid NOT NULL,
    user_id      uuid NOT NULL,
    redeemed_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_coupon_redemptions_coupon
        FOREIGN KEY (coupon_id)
        REFERENCES users.coupons(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_coupon_redemptions_coupon_user
        UNIQUE (coupon_id, user_id)
);


-- Demo coupon codes.
INSERT INTO users.coupons (code, amount) VALUES
    ('WELCOME50', 50.00),
    ('READORA100', 100.00),
    ('QUICKCOMM200', 200.00)
ON CONFLICT (code) DO NOTHING;


-- Stores the user's address book.
CREATE TABLE IF NOT EXISTS users.addresses (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid NOT NULL,
    label          varchar NOT NULL,
    recipient_type varchar NOT NULL DEFAULT 'OWNER',
    recipient_name text NOT NULL,
    line1          text NOT NULL,
    line2          text,
    city           text NOT NULL,
    state          text NOT NULL,
    postal_code    text NOT NULL,
    country_code   text NOT NULL,
    store_id       uuid,
    phone          text,
    is_default     boolean NOT NULL DEFAULT false,
    deleted_at     timestamptz,

    CONSTRAINT chk_address_label
        CHECK (label IN ('HOME', 'WORK', 'OTHER')),

    CONSTRAINT chk_address_recipient_type
        CHECK (recipient_type IN ('OWNER', 'GUEST'))
);


-- ============================================================================
-- CATALOG SERVICE
-- ============================================================================


-- Stores categories. Deliberately flat (1D) — no nesting.
CREATE TABLE IF NOT EXISTS catalog.categories (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name          text NOT NULL,
    slug          text NOT NULL UNIQUE,
    display_order int NOT NULL DEFAULT 0
);


-- A fulfilment location. Every book belongs to exactly one store.
CREATE TABLE IF NOT EXISTS catalog.stores (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name         text NOT NULL,
    city         text NOT NULL,
    line1        text NOT NULL,
    line2        text,
    state        text NOT NULL,
    postal_code  text NOT NULL,
    country_code varchar(2) NOT NULL,
    is_active    boolean NOT NULL DEFAULT true
);


-- Stores book publishers.
CREATE TABLE IF NOT EXISTS catalog.publishers (
    id   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    slug text NOT NULL UNIQUE
);


-- Stores book authors.
CREATE TABLE IF NOT EXISTS catalog.authors (
    id   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    slug text NOT NULL UNIQUE,
    bio  text
);


-- Stores the main book catalog.
CREATE TABLE IF NOT EXISTS catalog.books (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    isbn13            varchar(13) NOT NULL UNIQUE,
    title             text NOT NULL,
    subtitle          text,
    description       text,
    table_of_contents text,
    category_id       uuid,
    publisher_id      uuid NOT NULL,
    store_id          uuid,
    language          text NOT NULL,
    format            varchar NOT NULL,
    page_count        int,
    published_on      date,
    list_price        numeric(10, 2) NOT NULL,
    currency          varchar(3) NOT NULL,
    cover_image_url   text,
    is_active         boolean NOT NULL DEFAULT true,
    created_by_user_id uuid,
    embedded_at       timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_books_category
        FOREIGN KEY (category_id)
        REFERENCES catalog.categories(id),

    CONSTRAINT fk_books_publisher
        FOREIGN KEY (publisher_id)
        REFERENCES catalog.publishers(id),

    CONSTRAINT fk_books_store
        FOREIGN KEY (store_id)
        REFERENCES catalog.stores(id),

    CONSTRAINT chk_books_format
        CHECK (format IN ('HARDCOVER', 'PAPERBACK', 'EBOOK'))
);


-- Maps books to authors.
CREATE TABLE IF NOT EXISTS catalog.book_authors (
    book_id   uuid NOT NULL,
    author_id uuid NOT NULL,

    PRIMARY KEY (book_id, author_id),

    CONSTRAINT fk_book_authors_book
        FOREIGN KEY (book_id)
        REFERENCES catalog.books(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_book_authors_author
        FOREIGN KEY (author_id)
        REFERENCES catalog.authors(id)
        ON DELETE CASCADE
);


-- Stores inventory using book_id as a shared primary key.
CREATE TABLE IF NOT EXISTS catalog.inventory (
    book_id           uuid PRIMARY KEY,
    qty_on_hand       int NOT NULL DEFAULT 0,
    qty_reserved      int NOT NULL DEFAULT 0,
    reorder_threshold int NOT NULL DEFAULT 0,
    updated_at        timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_inventory_book
        FOREIGN KEY (book_id)
        REFERENCES catalog.books(id)
        ON DELETE CASCADE
);


-- Stores an optional digital edition for a book.
CREATE TABLE IF NOT EXISTS catalog.virtual_editions (
    book_id         uuid PRIMARY KEY,
    file_url        text NOT NULL,
    file_format     varchar NOT NULL,
    file_size_bytes bigint,
    price           numeric(10, 2) NOT NULL,
    currency        varchar(3) NOT NULL,
    is_active       boolean NOT NULL DEFAULT true,
    created_by_user_id uuid,

    CONSTRAINT fk_virtual_editions_book
        FOREIGN KEY (book_id)
        REFERENCES catalog.books(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_virtual_edition_format
        CHECK (file_format IN ('PDF', 'EPUB'))
);


-- Stores additional ordered gallery images.
CREATE TABLE IF NOT EXISTS catalog.book_images (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id    uuid NOT NULL,
    url        text NOT NULL,
    sort_order int NOT NULL DEFAULT 0,

    CONSTRAINT fk_book_images_book
        FOREIGN KEY (book_id)
        REFERENCES catalog.books(id)
        ON DELETE CASCADE
);


-- Stores curated relationships between books.
CREATE TABLE IF NOT EXISTS catalog.related_books (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id         uuid NOT NULL,
    related_book_id uuid NOT NULL,
    relation_type   text NOT NULL,

    CONSTRAINT fk_related_books_book
        FOREIGN KEY (book_id)
        REFERENCES catalog.books(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_related_books_related
        FOREIGN KEY (related_book_id)
        REFERENCES catalog.books(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_related_books_not_self
        CHECK (book_id <> related_book_id),

    CONSTRAINT uq_related_books_pair
        UNIQUE (book_id, related_book_id)
);


-- Stores catalog transactional outbox events.
CREATE TABLE IF NOT EXISTS catalog.outbox_events (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type text NOT NULL,
    aggregate_id   uuid NOT NULL,
    event_type     text NOT NULL,
    payload        text NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    published_at   timestamptz
);


-- ============================================================================
-- COMMERCE SERVICE
-- ============================================================================


-- Stores durable placed orders.
CREATE TABLE IF NOT EXISTS commerce.orders (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    text NOT NULL UNIQUE,
    user_id         uuid NOT NULL,
    status          varchar NOT NULL,
    currency        varchar(3) NOT NULL,
    subtotal        numeric(10, 2) NOT NULL,
    shipping_fee    numeric(10, 2) NOT NULL,
    packaging_fee   numeric(10, 2) NOT NULL DEFAULT 0,
    tax_amount      numeric(10, 2) NOT NULL,
    grand_total     numeric(10, 2) NOT NULL,
    wallet_amount_used numeric(10, 2) NOT NULL DEFAULT 0,
    payment_method  varchar NOT NULL DEFAULT 'WALLET',
    placed_at       timestamptz,
    cancelled_at    timestamptz,
    cancel_reason   text,
    idempotency_key text NOT NULL UNIQUE,
    delivery_type   varchar NOT NULL,

    CONSTRAINT chk_order_status
        CHECK (
            status IN (
                'PENDING_PAYMENT',
                'PAID',
                'CONFIRMED',
                'SHIPPED',
                'DELIVERED',
                'PAYMENT_FAILED',
                'CANCELLED',
                'RETURNED'
            )
        ),

    CONSTRAINT chk_order_delivery_type
        CHECK (delivery_type IN ('PHYSICAL', 'VIRTUAL'))
);


-- Stores immutable purchase-time book snapshots.
CREATE TABLE IF NOT EXISTS commerce.order_items (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            uuid NOT NULL,
    book_id             uuid NOT NULL,
    title_snapshot      text NOT NULL,
    isbn_snapshot       varchar(13),
    unit_price_snapshot numeric(10, 2) NOT NULL,
    qty                 int NOT NULL,
    line_total          numeric(10, 2) NOT NULL,
    delivery_type       varchar NOT NULL DEFAULT 'PHYSICAL',

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES commerce.orders(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_order_items_delivery_type
        CHECK (delivery_type IN ('PHYSICAL', 'VIRTUAL'))
);


-- Stores the immutable shipping address copied at checkout.
CREATE TABLE IF NOT EXISTS commerce.order_shipping_addresses (
    order_id       uuid PRIMARY KEY,
    recipient_name text NOT NULL,
    line1          text NOT NULL,
    line2          text,
    city            text NOT NULL,
    state           text NOT NULL,
    postal_code    text NOT NULL,
    country_code   varchar(2) NOT NULL,
    phone          text,

    CONSTRAINT fk_order_shipping_address_order
        FOREIGN KEY (order_id)
        REFERENCES commerce.orders(id)
        ON DELETE CASCADE
);


-- Stores the order status transition audit trail.
CREATE TABLE IF NOT EXISTS commerce.order_status_history (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    uuid NOT NULL,
    from_status varchar NOT NULL,
    to_status   varchar NOT NULL,
    reason      text,
    changed_by  text,
    changed_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id)
        REFERENCES commerce.orders(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_order_history_from_status
        CHECK (
            from_status IN (
                'PENDING_PAYMENT',
                'PAID',
                'CONFIRMED',
                'SHIPPED',
                'DELIVERED',
                'PAYMENT_FAILED',
                'CANCELLED',
                'RETURNED'
            )
        ),

    CONSTRAINT chk_order_history_to_status
        CHECK (
            to_status IN (
                'PENDING_PAYMENT',
                'PAID',
                'CONFIRMED',
                'SHIPPED',
                'DELIVERED',
                'PAYMENT_FAILED',
                'CANCELLED',
                'RETURNED'
            )
        )
);


-- Stores commerce transactional outbox events.
CREATE TABLE IF NOT EXISTS commerce.outbox_events (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type text NOT NULL,
    aggregate_id   uuid NOT NULL,
    event_type     text NOT NULL,
    payload        text NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    published_at   timestamptz
);


-- ============================================================================
-- PAYMENT SERVICE
-- ============================================================================


-- Stores the payment associated with an order.
CREATE TABLE IF NOT EXISTS payments.payments (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id           uuid NOT NULL UNIQUE,
    user_id            uuid NOT NULL,
    method             varchar NOT NULL,
    status             varchar NOT NULL,
    amount             numeric(10, 2) NOT NULL,
    wallet_amount_used numeric(10, 2) NOT NULL DEFAULT 0,
    idempotency_key    text NOT NULL UNIQUE,
    authorized_at      timestamptz,
    captured_at        timestamptz,
    failure_code       text,
    failure_reason     text,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_payment_method
        CHECK (
            method IN (
                'CARD',
                'UPI',
                'NETBANKING',
                'WALLET'
            )
        ),

    CONSTRAINT chk_payment_status
        CHECK (
            status IN (
                'INITIATED',
                'AUTHORIZED',
                'CAPTURED',
                'FAILED',
                'REFUNDED'
            )
        )
);


-- Stores every payment gateway attempt.
CREATE TABLE IF NOT EXISTS payments.payment_attempts (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id        uuid NOT NULL,
    attempt_no        int NOT NULL,
    status            varchar NOT NULL,
    provider_response text,
    created_at        timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_attempts_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments.payments(id)
        ON DELETE CASCADE
);


-- Stores refunds against payments.
CREATE TABLE IF NOT EXISTS payments.refunds (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id   uuid NOT NULL,
    amount       numeric(10, 2) NOT NULL,
    status       varchar NOT NULL,
    reason       text,
    completed_at timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_refunds_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments.payments(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_refund_status
        CHECK (
            status IN (
                'PENDING',
                'COMPLETED',
                'FAILED'
            )
        )
);


-- Stores payment transactional outbox events.
CREATE TABLE IF NOT EXISTS payments.outbox_events (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type text NOT NULL,
    aggregate_id   uuid NOT NULL,
    event_type     text NOT NULL,
    payload        text NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    published_at   timestamptz
);


-- ============================================================================
-- NOTIFICATION SERVICE
-- ============================================================================

-- Stores persisted notifications — pushed live over WebSocket when created, listable/markable-read after.
CREATE TABLE IF NOT EXISTS notifications.notifications (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL,
    type       text NOT NULL,
    title      text NOT NULL,
    message    text NOT NULL,
    order_id   uuid,
    read       boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id
    ON notifications.notifications(user_id, created_at DESC);


-- ============================================================================
-- AI SERVICE
-- ============================================================================


-- Stores user chat conversations.
CREATE TABLE IF NOT EXISTS ai.conversations (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL,
    title      text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);


-- Stores individual conversation messages.
CREATE TABLE IF NOT EXISTS ai.messages (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id uuid NOT NULL,
    role            varchar NOT NULL,
    content         text NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES ai.conversations(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_message_role
        CHECK (role IN ('USER', 'ASSISTANT'))
);


-- Tracks catalog embedding jobs.
CREATE TABLE IF NOT EXISTS ai.embedding_jobs (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    status             varchar NOT NULL,
    triggered_by       uuid,
    total_books        int NOT NULL DEFAULT 0,
    processed_books    int NOT NULL DEFAULT 0,
    current_book_title text,
    error_message      text,
    queued_at          timestamptz NOT NULL DEFAULT now(),
    started_at         timestamptz,
    finished_at        timestamptz,

    CONSTRAINT chk_embedding_job_status
        CHECK (
            status IN (
                'QUEUED',
                'RUNNING',
                'COMPLETED',
                'FAILED'
            )
        )
);


-- ============================================================================
-- VECTOR STORE
-- ============================================================================
--
-- ai.vector_store is intentionally NOT created here.
--
-- Spring AI owns this table and creates it when:
--
--   vectorstore.pgvector.initialize-schema=true
--
-- The pgvector extension itself is installed above.
-- ============================================================================


-- ============================================================================
-- 6. INDEXES
-- ============================================================================

-- Speeds up category-based book searches.
CREATE INDEX IF NOT EXISTS idx_books_category_id
    ON catalog.books(category_id);

-- Speeds up publisher-based book searches.
CREATE INDEX IF NOT EXISTS idx_books_publisher_id
    ON catalog.books(publisher_id);

-- Speeds up store-based book browsing (quick-commerce: browse one store at a time).
CREATE INDEX IF NOT EXISTS idx_books_store_id
    ON catalog.books(store_id);

-- Speeds up author-based book searches.
CREATE INDEX IF NOT EXISTS idx_book_authors_author_id
    ON catalog.book_authors(author_id);

-- Speeds up book gallery loading.
CREATE INDEX IF NOT EXISTS idx_book_images_book_id
    ON catalog.book_images(book_id);

-- Speeds up related-book loading.
CREATE INDEX IF NOT EXISTS idx_related_books_book_id
    ON catalog.related_books(book_id);

-- Speeds up user order history.
CREATE INDEX IF NOT EXISTS idx_orders_user_id
    ON commerce.orders(user_id);

-- Speeds up order item loading.
CREATE INDEX IF NOT EXISTS idx_order_items_order_id
    ON commerce.order_items(order_id);

-- Speeds up order status history.
CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id
    ON commerce.order_status_history(order_id);

-- Speeds up user payment lookup.
CREATE INDEX IF NOT EXISTS idx_payments_user_id
    ON payments.payments(user_id);

-- Speeds up payment attempt lookup.
CREATE INDEX IF NOT EXISTS idx_payment_attempts_payment_id
    ON payments.payment_attempts(payment_id);

-- Speeds up refund lookup.
CREATE INDEX IF NOT EXISTS idx_refunds_payment_id
    ON payments.refunds(payment_id);

-- Speeds up conversation lookup by user.
CREATE INDEX IF NOT EXISTS idx_conversations_user_id
    ON ai.conversations(user_id);

-- Speeds up message lookup by conversation.
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id
    ON ai.messages(conversation_id);


-- ============================================================================
-- 7. FIXED AUTH ROLES
-- ============================================================================

-- Insert the two predefined application roles.
INSERT INTO auth.roles (
    id,
    code,
    description
)
VALUES
    (
        gen_random_uuid(),
        'CUSTOMER',
        'Default role for registered customers'
    ),
    (
        gen_random_uuid(),
        'ADMIN',
        'Administrative role with elevated privileges'
    )
ON CONFLICT (code) DO NOTHING;


-- ============================================================================
-- 8. CATEGORIES
-- ============================================================================

-- Load categories JSON into a psql variable.
\set categories_json `cat "data/categories.json"`


-- Insert categories using slug as the natural unique key.
INSERT INTO catalog.categories (
    id,
    name,
    slug,
    display_order
)
SELECT
    gen_random_uuid(),
    c.name,
    c.slug,
    c."displayOrder"
FROM jsonb_to_recordset(
    :'categories_json'::jsonb
) AS c(
    name text,
    slug text,
    "displayOrder" int
)
ON CONFLICT (slug) DO NOTHING;


-- ============================================================================
-- 8b. STORES
-- ============================================================================
--
-- Quick-commerce model: every book belongs to exactly one store, and a customer shops one
-- store at a time. Only Bangalore is seeded today; more stores can be added later.

INSERT INTO catalog.stores (
    id,
    name,
    city,
    line1,
    line2,
    state,
    postal_code,
    country_code
)
VALUES (
    '00000000-0000-0000-0000-0000000000b1',
    'Readora Bangalore',
    'Bangalore',
    '100 Indiranagar 100ft Road',
    'Near CMH Road',
    'Karnataka',
    '560038',
    'IN'
)
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- 9. PUBLISHERS
-- ============================================================================

-- Load publishers JSON.
\set publishers_json `cat "data/publishers.json"`


-- Insert publishers using slug as the unique key.
INSERT INTO catalog.publishers (
    id,
    name,
    slug
)
SELECT
    gen_random_uuid(),
    p.name,
    p.slug
FROM jsonb_to_recordset(
    :'publishers_json'::jsonb
) AS p(
    name text,
    slug text
)
ON CONFLICT (slug) DO NOTHING;


-- ============================================================================
-- 10. AUTHORS
-- ============================================================================

-- Load authors JSON.
\set authors_json `cat "data/authors.json"`


-- Insert authors using slug as the unique key.
INSERT INTO catalog.authors (
    id,
    name,
    slug,
    bio
)
SELECT
    gen_random_uuid(),
    a.name,
    a.slug,
    a.bio
FROM jsonb_to_recordset(
    :'authors_json'::jsonb
) AS a(
    name text,
    slug text,
    bio text
)
ON CONFLICT (slug) DO NOTHING;


-- ============================================================================
-- 11. BOOKS
-- ============================================================================

-- Load books JSON.
\set books_json `cat "data/books.json"`


-- Insert books and resolve category/publisher IDs by name.
INSERT INTO catalog.books (
    id,
    isbn13,
    title,
    subtitle,
    description,
    table_of_contents,
    category_id,
    publisher_id,
    store_id,
    language,
    format,
    page_count,
    published_on,
    list_price,
    currency,
    cover_image_url,
    is_active,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b->>'isbn13',
    b->>'title',
    b->>'subtitle',
    b->>'description',
    b->>'tableOfContents',
    c.id,
    p.id,
    -- Books are physically stocked at Bangalore by default; a book explicitly marked
    -- "virtualOnly" in the seed JSON gets no store — it's a universal virtual-only title.
    CASE WHEN (b->>'virtualOnly')::boolean IS TRUE THEN NULL::uuid ELSE '00000000-0000-0000-0000-0000000000b1'::uuid END,
    b->>'language',
    b->>'format',
    (b->>'pageCount')::int,
    (b->>'publishedOn')::date,
    (b->>'listPrice')::numeric,
    b->>'currency',
    b->>'coverImageUrl',
    true,
    now(),
    now()
FROM jsonb_array_elements(
    :'books_json'::jsonb
) AS b
-- LEFT JOIN: a book with no clean-fitting category (e.g. general literary classics that don't
-- belong under any of the flat, specific topics) ships with category_id NULL rather than being
-- forced into an inaccurate bucket, or silently dropped from the seed like an INNER JOIN would.
LEFT JOIN catalog.categories AS c
    ON c.name = b->>'category'
JOIN catalog.publishers AS p
    ON p.name = b->>'publisher'
ON CONFLICT (isbn13) DO NOTHING;


-- ============================================================================
-- 12. BOOK AUTHORS
-- ============================================================================

-- Create book-author relationships from the JSON authors array.
INSERT INTO catalog.book_authors (
    book_id,
    author_id
)
SELECT
    book.id,
    author.id
FROM jsonb_array_elements(
    :'books_json'::jsonb
) AS b
JOIN catalog.books AS book
    ON book.isbn13 = b->>'isbn13'
CROSS JOIN LATERAL jsonb_array_elements_text(
    b->'authors'
) AS author_name
JOIN catalog.authors AS author
    ON author.name = author_name
ON CONFLICT DO NOTHING;


-- ============================================================================
-- 13. INVENTORY
-- ============================================================================

-- Load inventory JSON.
\set inventory_json `cat "data/inventory.json"`


-- Insert inventory using ISBN-13 to resolve book IDs.
INSERT INTO catalog.inventory (
    book_id,
    qty_on_hand,
    qty_reserved,
    reorder_threshold,
    updated_at
)
SELECT
    book.id,
    (i->>'qtyOnHand')::int,
    (i->>'qtyReserved')::int,
    (i->>'reorderThreshold')::int,
    now()
FROM jsonb_array_elements(
    :'inventory_json'::jsonb
) AS i
JOIN catalog.books AS book
    ON book.isbn13 = i->>'isbn13'
ON CONFLICT (book_id) DO NOTHING;


-- ============================================================================
-- 14. VIRTUAL EDITIONS
-- ============================================================================

-- Load virtual editions JSON.
\set virtual_editions_json `cat "data/virtual_editions.json"`


-- Insert digital editions using ISBN-13 to resolve book IDs.
INSERT INTO catalog.virtual_editions (
    book_id,
    file_url,
    file_format,
    file_size_bytes,
    price,
    currency,
    is_active
)
SELECT
    book.id,
    v->>'fileUrl',
    v->>'fileFormat',
    (v->>'fileSizeBytes')::bigint,
    (v->>'price')::numeric,
    v->>'currency',
    (v->>'isActive')::boolean
FROM jsonb_array_elements(
    :'virtual_editions_json'::jsonb
) AS v
JOIN catalog.books AS book
    ON book.isbn13 = v->>'isbn13'
ON CONFLICT (book_id) DO NOTHING;


-- ============================================================================
-- 15. USERS
-- ============================================================================

-- Load users JSON.
\set users_json `cat "data/users.json"`


-- Insert authentication accounts using email as the natural key.
INSERT INTO auth.users (
    id,
    email,
    password_hash,
    status,
    email_verified,
    failed_login_attempts,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    u->>'email',
    :'users_json'::jsonb->>'passwordHash',
    'ACTIVE',
    true,
    0,
    now(),
    now()
FROM jsonb_array_elements(
    (:'users_json'::jsonb)->'users'
) AS u
ON CONFLICT (email) DO NOTHING;


-- Assign each user their configured role.
INSERT INTO auth.user_roles (
    user_id,
    role_id
)
SELECT
    au.id,
    r.id
FROM jsonb_array_elements(
    (:'users_json'::jsonb)->'users'
) AS u
JOIN auth.users AS au
    ON au.email = u->>'email'
JOIN auth.roles AS r
    ON r.code = u->>'role'
ON CONFLICT DO NOTHING;


-- Create user profiles.
INSERT INTO users.user_profiles (
    user_id,
    display_name,
    phone,
    marketing_opt_in,
    preferred_store_id
)
SELECT
    au.id,
    u->>'displayName',
    u->>'phone',
    (u->>'marketingOptIn')::boolean,
    '00000000-0000-0000-0000-0000000000b1'
FROM jsonb_array_elements(
    (:'users_json'::jsonb)->'users'
) AS u
JOIN auth.users AS au
    ON au.email = u->>'email'
ON CONFLICT (user_id) DO NOTHING;


-- Create wallet accounts.
INSERT INTO users.wallet_accounts (
    user_id,
    balance,
    currency,
    updated_at
)
SELECT
    au.id,
    (u->>'walletBalance')::numeric,
    'INR',
    now()
FROM jsonb_array_elements(
    (:'users_json'::jsonb)->'users'
) AS u
JOIN auth.users AS au
    ON au.email = u->>'email'
ON CONFLICT (user_id) DO NOTHING;


-- Create addresses when the user has no existing addresses.
INSERT INTO users.addresses (
    id,
    user_id,
    label,
    recipient_type,
    recipient_name,
    line1,
    line2,
    city,
    state,
    postal_code,
    country_code,
    store_id,
    phone,
    is_default
)
SELECT
    gen_random_uuid(),
    au.id,
    address->>'label',
    'OWNER',
    address->>'recipientName',
    address->>'line1',
    address->>'line2',
    address->>'city',
    address->>'state',
    address->>'postalCode',
    address->>'countryCode',
    '00000000-0000-0000-0000-0000000000b1',
    address->>'phone',
    (address->>'isDefault')::boolean
FROM jsonb_array_elements(
    (:'users_json'::jsonb)->'users'
) AS u
JOIN auth.users AS au
    ON au.email = u->>'email'
CROSS JOIN LATERAL jsonb_array_elements(
    u->'addresses'
) AS address
WHERE NOT EXISTS (
    SELECT 1
    FROM users.addresses AS existing
    WHERE existing.user_id = au.id
);


-- ============================================================================
-- 16. COMMIT
-- ============================================================================

-- Commit the complete schema and seed transaction.
COMMIT;


-- ============================================================================
-- 17. VERIFICATION
-- ============================================================================

-- Display row counts for the main seeded tables.
SELECT 'auth.users' AS table_name, COUNT(*) AS row_count
FROM auth.users

UNION ALL

SELECT 'auth.roles', COUNT(*)
FROM auth.roles

UNION ALL

SELECT 'auth.user_roles', COUNT(*)
FROM auth.user_roles

UNION ALL

SELECT 'users.user_profiles', COUNT(*)
FROM users.user_profiles

UNION ALL

SELECT 'users.wallet_accounts', COUNT(*)
FROM users.wallet_accounts

UNION ALL

SELECT 'users.addresses', COUNT(*)
FROM users.addresses

UNION ALL

SELECT 'catalog.categories', COUNT(*)
FROM catalog.categories

UNION ALL

SELECT 'catalog.stores', COUNT(*)
FROM catalog.stores

UNION ALL

SELECT 'catalog.publishers', COUNT(*)
FROM catalog.publishers

UNION ALL

SELECT 'catalog.authors', COUNT(*)
FROM catalog.authors

UNION ALL

SELECT 'catalog.books', COUNT(*)
FROM catalog.books

UNION ALL

SELECT 'catalog.book_authors', COUNT(*)
FROM catalog.book_authors

UNION ALL

SELECT 'catalog.inventory', COUNT(*)
FROM catalog.inventory

UNION ALL

SELECT 'catalog.virtual_editions', COUNT(*)
FROM catalog.virtual_editions

ORDER BY table_name;


-- Print completion message.
\echo ''
\echo '============================================================'
\echo ' Readora database setup and seed completed successfully.'
\echo '============================================================'
\echo ''