BEGIN;

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
    ),
    (
        gen_random_uuid(),
        'DELIVERY_AGENT',
        'Delivers physical orders assigned to them'
    )
ON CONFLICT (code) DO NOTHING;


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


-- Stores span 21 Indian cities so store-scoped browsing and delivery actually vary by store.

\set stores_json `cat "data/stores.json"`

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
SELECT
    (s->>'id')::uuid,
    s->>'name',
    s->>'city',
    s->>'line1',
    s->>'line2',
    s->>'state',
    s->>'postalCode',
    s->>'countryCode'
FROM jsonb_array_elements(
    :'stores_json'::jsonb
) AS s
ON CONFLICT (id) DO NOTHING;


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

-- Load authors JSON.
\set authors_json `cat "data/authors.json"`

-- Insert authors using slug as the unique key.
INSERT INTO catalog.authors (
    id,
    name,
    slug,
    bio,
    photo_url
)
SELECT
    gen_random_uuid(),
    a.name,
    a.slug,
    a.bio,
    a."photoUrl"
FROM jsonb_to_recordset(
    :'authors_json'::jsonb
) AS a(
    name text,
    slug text,
    bio text,
    "photoUrl" text
)
ON CONFLICT (slug) DO NOTHING;

-- Load books JSON.
\set books_json `cat "data/books.json"`

-- Insert books and resolve category/publisher IDs by name.
INSERT INTO catalog.books (
    id,
    isbn13,
    title,

    description,
    table_of_contents,
    category_id,
    publisher_id,
    store_id,
    language,
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

    b->>'description',
    b->>'tableOfContents',
    c.id,
    p.id,
    CASE
        WHEN (b->>'virtualOnly')::boolean IS TRUE THEN NULL::uuid
        WHEN b->>'preferredStoreCity' IS NOT NULL THEN (
            SELECT s.id
            FROM catalog.stores AS s
            WHERE s.city = b->>'preferredStoreCity'
            LIMIT 1
        )
        ELSE (
            SELECT s.id
            FROM catalog.stores AS s
            ORDER BY s.name
            OFFSET (ord - 1) % (SELECT count(*) FROM catalog.stores)
            LIMIT 1
        )
    END,
    b->>'language',
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
) WITH ORDINALITY AS elems(b, ord)
LEFT JOIN catalog.categories AS c
    ON c.name = b->>'category'
JOIN catalog.publishers AS p
    ON p.name = b->>'publisher'
ON CONFLICT (isbn13) DO NOTHING;


UPDATE catalog.books
SET store_id = target_store.id
FROM jsonb_array_elements(:'books_json'::jsonb) AS wanted(spec),
     catalog.stores AS target_store
WHERE catalog.books.isbn13 = wanted.spec->>'isbn13'
  AND wanted.spec->>'preferredStoreCity' IS NOT NULL
  AND target_store.city = wanted.spec->>'preferredStoreCity';

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


-- Create user profiles — admin_store_id is only ever set here, never by an application endpoint.
INSERT INTO users.user_profiles (
    user_id,
    display_name,
    phone,
    marketing_opt_in,
    preferred_store_id,
    admin_store_id
)
SELECT
    au.id,
    u->>'displayName',
    u->>'phone',
    (u->>'marketingOptIn')::boolean,
    '00000000-0000-0000-0000-0000000000b1',
    NULLIF(u->>'assignedStoreId', '')::uuid
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

-- Load delivery agents JSON.
\set delivery_agents_json `cat "data/delivery_agents.json"`

-- Creates each agent's profile, resolving their user id by email (already inserted above).
INSERT INTO delivery.delivery_agents (
    user_id,
    name,
    phone,
    store_id
)
SELECT
    au.id,
    a->>'name',
    a->>'phone',
    (a->>'storeId')::uuid
FROM jsonb_array_elements(
    :'delivery_agents_json'::jsonb
) AS a
JOIN auth.users AS au
    ON au.email = a->>'email'
ON CONFLICT (user_id) DO NOTHING;

-- Load reviews JSON.
\set reviews_json `cat "data/reviews.json"`

-- Attach each review to its book (by isbn13) and reviewing customer (by email).
INSERT INTO catalog.reviews (
    id,
    book_id,
    user_id,
    author_display_name,
    rating,
    comment,
    verified_purchase
)
SELECT
    gen_random_uuid(),
    b.id,
    au.id,
    r->>'authorDisplayName',
    (r->>'rating')::int,
    r->>'comment',
    (r->>'verifiedPurchase')::boolean
FROM jsonb_array_elements(
    :'reviews_json'::jsonb
) AS r
JOIN catalog.books AS b
    ON b.isbn13 = r->>'isbn13'
JOIN auth.users AS au
    ON au.email = r->>'email'
ON CONFLICT (book_id, user_id) DO NOTHING;

COMMIT;


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

UNION ALL

SELECT 'delivery.delivery_agents', COUNT(*)
FROM delivery.delivery_agents

UNION ALL

SELECT 'catalog.reviews', COUNT(*)
FROM catalog.reviews

ORDER BY table_name;


-- Print completion message.
\echo ' Readora database setup and seed completed successfully.'
