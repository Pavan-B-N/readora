-- Dummy data for every model across auth / users / catalog schemas. Deterministic UUIDs via
-- \set, so re-running this script is safe (every INSERT uses ON CONFLICT DO NOTHING).
--
-- Not seeded, on purpose: commerce/payments (orders, payments). Those carry real workflow state
-- (stock reservation, outbox events, Kafka choreography) that's better exercised by actually
-- using the checkout flow once the backend + frontend are running, than faked here.
--
-- All seeded users share the password: Password123!
-- (bcrypt hash below verified against Spring Security's own BCryptPasswordEncoder, not assumed)

-- ============================================================================
-- auth schema
-- ============================================================================

\set role_customer '10000000-0000-0000-0000-000000000001'
\set role_admin    '10000000-0000-0000-0000-000000000002'

INSERT INTO auth.roles (id, code, description) VALUES
  (:'role_customer', 'CUSTOMER', 'Default role for registered customers'),
  (:'role_admin',    'ADMIN',    'Administrative role with elevated privileges')
ON CONFLICT DO NOTHING;

\set user_admin '20000000-0000-0000-0000-000000000001'
\set user_alice '20000000-0000-0000-0000-000000000002'
\set user_bob   '20000000-0000-0000-0000-000000000003'
\set user_carol '20000000-0000-0000-0000-000000000004'
\set demo_password_hash '$2y$10$3tEIXdfY.cmxmMtuI4RIbOIY/sO19PceBf9xhsuyIPQ7l0ELl6q0i'

INSERT INTO auth.users (id, email, password_hash, status, email_verified, failed_login_attempts, created_at, updated_at) VALUES
  (:'user_admin', 'admin@readora.dev', :'demo_password_hash', 'ACTIVE', true, 0, now(), now()),
  (:'user_alice', 'alice@readora.dev', :'demo_password_hash', 'ACTIVE', true, 0, now(), now()),
  (:'user_bob',   'bob@readora.dev',   :'demo_password_hash', 'ACTIVE', true, 0, now(), now()),
  (:'user_carol', 'carol@readora.dev', :'demo_password_hash', 'ACTIVE', true, 0, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id) VALUES
  (:'user_admin', :'role_admin'),
  (:'user_alice', :'role_customer'),
  (:'user_bob',   :'role_customer'),
  (:'user_carol', :'role_customer')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- users schema
-- ============================================================================

INSERT INTO users.user_profiles (user_id, display_name, phone, locale, marketing_opt_in) VALUES
  (:'user_admin', 'Readora Admin',  '+91-9000000001', 'en-IN', false),
  (:'user_alice', 'Alice Nguyen',   '+91-9000000002', 'en-IN', true),
  (:'user_bob',   'Bob Martinez',   '+91-9000000003', 'en-IN', false),
  (:'user_carol', 'Carol Singh',    '+91-9000000004', 'en-IN', true)
ON CONFLICT DO NOTHING;

INSERT INTO users.wallet_accounts (user_id, balance, currency, updated_at) VALUES
  (:'user_admin', 0.00,   'INR', now()),
  (:'user_alice', 100.00, 'INR', now()),
  (:'user_bob',   250.00, 'INR', now()),
  (:'user_carol', 0.00,   'INR', now())
ON CONFLICT DO NOTHING;

INSERT INTO users.addresses (id, user_id, label, recipient_name, line1, line2, city, state, postal_code, country_code, phone, is_default) VALUES
  ('30000000-0000-0000-0000-000000000001', :'user_alice', 'HOME', 'Alice Nguyen', '221B Baker Street', NULL, 'Bengaluru', 'Karnataka', '560001', 'IN', '+91-9000000002', true),
  ('30000000-0000-0000-0000-000000000002', :'user_bob',   'HOME', 'Bob Martinez', '742 Evergreen Terrace', NULL, 'Mumbai', 'Maharashtra', '400001', 'IN', '+91-9000000003', true),
  ('30000000-0000-0000-0000-000000000003', :'user_bob',   'WORK', 'Bob Martinez', '1 Infinite Loop', 'Tower B, 4th Floor', 'Mumbai', 'Maharashtra', '400051', 'IN', '+91-9000000003', false),
  ('30000000-0000-0000-0000-000000000004', :'user_carol', 'HOME', 'Carol Singh',  '4 Privet Drive', NULL, 'Delhi', 'Delhi', '110001', 'IN', '+91-9000000004', true)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- catalog schema
-- ============================================================================

\set cat_fiction    '40000000-0000-0000-0000-000000000001'
\set cat_scifi      '40000000-0000-0000-0000-000000000002'
\set cat_mystery    '40000000-0000-0000-0000-000000000003'
\set cat_nonfiction '40000000-0000-0000-0000-000000000004'
\set cat_technology '40000000-0000-0000-0000-000000000005'
\set cat_business   '40000000-0000-0000-0000-000000000006'
\set cat_science    '40000000-0000-0000-0000-000000000007'

INSERT INTO catalog.categories (id, name, slug, parent_id, display_order) VALUES
  (:'cat_fiction',    'Fiction',                     'fiction',     NULL,             1),
  (:'cat_scifi',      'Science Fiction & Fantasy',   'sci-fi-fantasy', :'cat_fiction', 1),
  (:'cat_mystery',    'Mystery & Thriller',          'mystery-thriller', :'cat_fiction', 2),
  (:'cat_nonfiction', 'Non-Fiction',                 'non-fiction', NULL,             2),
  (:'cat_technology', 'Technology',                  'technology',  :'cat_nonfiction', 1),
  (:'cat_business',   'Business',                    'business',    :'cat_nonfiction', 2),
  (:'cat_science',    'Science',                     'science',     :'cat_nonfiction', 3)
ON CONFLICT DO NOTHING;

\set pub_penguin '50000000-0000-0000-0000-000000000001'
\set pub_tor     '50000000-0000-0000-0000-000000000002'
\set pub_oreilly '50000000-0000-0000-0000-000000000003'
\set pub_manning '50000000-0000-0000-0000-000000000004'
\set pub_harper  '50000000-0000-0000-0000-000000000005'

INSERT INTO catalog.publishers (id, name, slug) VALUES
  (:'pub_penguin', 'Penguin Classics',        'penguin-classics'),
  (:'pub_tor',     'Tor Books',               'tor-books'),
  (:'pub_oreilly', 'O''Reilly Media',         'oreilly-media'),
  (:'pub_manning', 'Manning Publications',    'manning-publications'),
  (:'pub_harper',  'HarperCollins',           'harpercollins')
ON CONFLICT DO NOTHING;

\set auth_orwell   '60000000-0000-0000-0000-000000000001'
\set auth_asimov   '60000000-0000-0000-0000-000000000002'
\set auth_herbert  '60000000-0000-0000-0000-000000000003'
\set auth_christie '60000000-0000-0000-0000-000000000004'
\set auth_weir     '60000000-0000-0000-0000-000000000005'
\set auth_martin   '60000000-0000-0000-0000-000000000006'
\set auth_fowler   '60000000-0000-0000-0000-000000000007'
\set auth_harari   '60000000-0000-0000-0000-000000000008'
\set auth_isaacson '60000000-0000-0000-0000-000000000009'
\set auth_tolkien  '60000000-0000-0000-0000-000000000010'

INSERT INTO catalog.authors (id, name, slug, bio) VALUES
  (:'auth_orwell',   'George Orwell',        'george-orwell',        'English novelist and essayist, best known for dystopian fiction.'),
  (:'auth_asimov',   'Isaac Asimov',         'isaac-asimov',         'American writer and biochemist, prolific author of science fiction.'),
  (:'auth_herbert',  'Frank Herbert',        'frank-herbert',        'American science fiction author, creator of the Dune universe.'),
  (:'auth_christie', 'Agatha Christie',      'agatha-christie',      'English writer known for detective novels.'),
  (:'auth_weir',     'Andy Weir',            'andy-weir',            'American novelist known for hard science fiction.'),
  (:'auth_martin',   'Robert C. Martin',     'robert-c-martin',      'Software engineer and author of books on software craftsmanship.'),
  (:'auth_fowler',   'Martin Fowler',        'martin-fowler',        'Author and speaker on software design and architecture.'),
  (:'auth_harari',   'Yuval Noah Harari',    'yuval-noah-harari',    'Israeli historian and author of popular science books.'),
  (:'auth_isaacson', 'Walter Isaacson',      'walter-isaacson',      'American writer known for biographies of innovators.'),
  (:'auth_tolkien',  'J.R.R. Tolkien',       'jrr-tolkien',          'English writer and philologist, author of Middle-earth novels.')
ON CONFLICT DO NOTHING;

\set book_1984         '70000000-0000-0000-0000-000000000001'
\set book_animalfarm    '70000000-0000-0000-0000-000000000002'
\set book_foundation    '70000000-0000-0000-0000-000000000003'
\set book_irobot        '70000000-0000-0000-0000-000000000004'
\set book_dune          '70000000-0000-0000-0000-000000000005'
\set book_orientexpress '70000000-0000-0000-0000-000000000006'
\set book_hobbit        '70000000-0000-0000-0000-000000000007'
\set book_fellowship    '70000000-0000-0000-0000-000000000008'
\set book_martian       '70000000-0000-0000-0000-000000000009'
\set book_cleancode     '70000000-0000-0000-0000-000000000010'
\set book_cleanarch     '70000000-0000-0000-0000-000000000011'
\set book_refactoring   '70000000-0000-0000-0000-000000000012'
\set book_poeaa         '70000000-0000-0000-0000-000000000013'
\set book_sapiens       '70000000-0000-0000-0000-000000000014'
\set book_homodeus      '70000000-0000-0000-0000-000000000015'
\set book_stevejobs     '70000000-0000-0000-0000-000000000016'

INSERT INTO catalog.books (
  id, isbn13, title, subtitle, description, table_of_contents, category_id, publisher_id,
  language, format, page_count, published_on, list_price, currency, cover_image_url, is_active,
  created_at, updated_at
) VALUES
  (:'book_1984', '9780451524935', '1984', NULL,
   'A dystopian social science fiction novel following Winston Smith''s resistance against a totalitarian surveillance state.',
   NULL, :'cat_scifi', :'pub_penguin', 'en', 'PAPERBACK', 328, '1949-06-08', 399.00, 'INR', NULL, true, now(), now()),

  (:'book_animalfarm', '9780451526342', 'Animal Farm', NULL,
   'An allegorical novella reflecting events leading up to the Russian Revolution, told through farm animals.',
   NULL, :'cat_fiction', :'pub_penguin', 'en', 'PAPERBACK', 112, '1945-08-17', 299.00, 'INR', NULL, true, now(), now()),

  (:'book_foundation', '9780553293357', 'Foundation', NULL,
   'The first novel in the Foundation series, chronicling the fall and rebirth of a galactic empire.',
   NULL, :'cat_scifi', :'pub_tor', 'en', 'PAPERBACK', 255, '1951-05-01', 499.00, 'INR', NULL, true, now(), now()),

  (:'book_irobot', '9780553294385', 'I, Robot', NULL,
   'A collection of nine science fiction short stories exploring the Three Laws of Robotics.',
   NULL, :'cat_scifi', :'pub_tor', 'en', 'PAPERBACK', 253, '1950-12-02', 449.00, 'INR', NULL, true, now(), now()),

  (:'book_dune', '9780441172719', 'Dune', NULL,
   'A science fiction epic set on the desert planet Arrakis, following Paul Atreides.',
   NULL, :'cat_scifi', :'pub_tor', 'en', 'HARDCOVER', 688, '1965-08-01', 799.00, 'INR', NULL, true, now(), now()),

  (:'book_orientexpress', '9780062073501', 'Murder on the Orient Express', NULL,
   'Detective Hercule Poirot investigates a murder aboard the famous Orient Express.',
   NULL, :'cat_mystery', :'pub_harper', 'en', 'PAPERBACK', 256, '1934-01-01', 349.00, 'INR', NULL, true, now(), now()),

  (:'book_hobbit', '9780547928227', 'The Hobbit', NULL,
   'Bilbo Baggins is swept into an epic quest to reclaim the Lonely Mountain from the dragon Smaug.',
   NULL, :'cat_scifi', :'pub_harper', 'en', 'HARDCOVER', 310, '1937-09-21', 599.00, 'INR', NULL, true, now(), now()),

  (:'book_fellowship', '9780547928210', 'The Fellowship of the Ring', 'The Lord of the Rings, Part One',
   'The first volume of The Lord of the Rings, following Frodo Baggins and the Fellowship.',
   NULL, :'cat_scifi', :'pub_harper', 'en', 'HARDCOVER', 423, '1954-07-29', 649.00, 'INR', NULL, true, now(), now()),

  (:'book_martian', '9780553418026', 'The Martian', NULL,
   'An astronaut stranded on Mars must rely on his ingenuity to survive.',
   NULL, :'cat_scifi', :'pub_penguin', 'en', 'EBOOK', 369, '2011-09-27', 349.00, 'INR', NULL, true, now(), now()),

  (:'book_cleancode', '9780132350884', 'Clean Code', 'A Handbook of Agile Software Craftsmanship',
   'Best practices for writing readable, maintainable software.',
   '{"Introduction": ["Bad Code", "Total Cost of Owning a Mess"], "Meaningful Names": ["Naming Conventions"], "Functions": ["Small Functions", "One Level of Abstraction"], "Error Handling": ["Exceptions vs Return Codes"]}',
   :'cat_technology', :'pub_oreilly', 'en', 'PAPERBACK', 464, '2008-08-01', 899.00, 'INR', NULL, true, now(), now()),

  (:'book_cleanarch', '9780134494166', 'Clean Architecture', 'A Craftsman''s Guide to Software Structure and Design',
   'Principles for designing software systems that are independent of frameworks and delivery mechanisms.',
   '{"Introduction": ["What is Design and Architecture"], "Building Blocks": ["Programming Paradigms"], "Design Principles": ["SRP", "OCP", "LSP", "ISP", "DIP"], "Component Principles": ["Cohesion", "Coupling"]}',
   :'cat_technology', :'pub_oreilly', 'en', 'PAPERBACK', 432, '2017-09-20', 949.00, 'INR', NULL, true, now(), now()),

  (:'book_refactoring', '9780134757599', 'Refactoring', 'Improving the Design of Existing Code',
   'A catalog of refactoring techniques for improving code structure without changing behavior.',
   '{"Introduction": ["Why Refactor"], "Bad Smells in Code": ["Duplicated Code", "Long Method"], "Catalog": ["Extract Function", "Inline Function", "Rename Variable"]}',
   :'cat_technology', :'pub_oreilly', 'en', 'HARDCOVER', 448, '2018-11-30', 1099.00, 'INR', NULL, true, now(), now()),

  (:'book_poeaa', '9780321127426', 'Patterns of Enterprise Application Architecture', NULL,
   'A catalog of architectural patterns for enterprise application development.',
   NULL, :'cat_technology', :'pub_manning', 'en', 'HARDCOVER', 560, '2002-11-15', 1299.00, 'INR', NULL, true, now(), now()),

  (:'book_sapiens', '9780062316097', 'Sapiens', 'A Brief History of Humankind',
   'A sweeping narrative of humankind''s creation and evolution.',
   NULL, :'cat_science', :'pub_harper', 'en', 'PAPERBACK', 443, '2011-01-01', 599.00, 'INR', NULL, true, now(), now()),

  (:'book_homodeus', '9780062464316', 'Homo Deus', 'A Brief History of Tomorrow',
   'An exploration of humanity''s future, from biotechnology to artificial intelligence.',
   NULL, :'cat_science', :'pub_harper', 'en', 'PAPERBACK', 448, '2015-01-01', 649.00, 'INR', NULL, true, now(), now()),

  (:'book_stevejobs', '9781451648539', 'Steve Jobs', NULL,
   'The exclusive biography of Apple co-founder Steve Jobs, based on over forty interviews.',
   NULL, :'cat_business', :'pub_penguin', 'en', 'HARDCOVER', 656, '2011-10-24', 899.00, 'INR', NULL, true, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO catalog.book_authors (book_id, author_id) VALUES
  (:'book_1984', :'auth_orwell'),
  (:'book_animalfarm', :'auth_orwell'),
  (:'book_foundation', :'auth_asimov'),
  (:'book_irobot', :'auth_asimov'),
  (:'book_dune', :'auth_herbert'),
  (:'book_orientexpress', :'auth_christie'),
  (:'book_hobbit', :'auth_tolkien'),
  (:'book_fellowship', :'auth_tolkien'),
  (:'book_martian', :'auth_weir'),
  (:'book_cleancode', :'auth_martin'),
  (:'book_cleanarch', :'auth_martin'),
  (:'book_refactoring', :'auth_fowler'),
  (:'book_poeaa', :'auth_fowler'),
  (:'book_sapiens', :'auth_harari'),
  (:'book_homodeus', :'auth_harari'),
  (:'book_stevejobs', :'auth_isaacson')
ON CONFLICT DO NOTHING;

INSERT INTO catalog.inventory (book_id, qty_on_hand, qty_reserved, reorder_threshold, updated_at) VALUES
  (:'book_1984', 40, 0, 5, now()),
  (:'book_animalfarm', 35, 0, 5, now()),
  (:'book_foundation', 20, 0, 5, now()),
  (:'book_irobot', 18, 0, 5, now()),
  (:'book_dune', 25, 0, 5, now()),
  (:'book_orientexpress', 30, 0, 5, now()),
  (:'book_hobbit', 22, 0, 5, now()),
  (:'book_fellowship', 15, 0, 5, now()),
  (:'book_martian', 0, 0, 5, now()),
  (:'book_cleancode', 50, 0, 10, now()),
  (:'book_cleanarch', 45, 0, 10, now()),
  (:'book_refactoring', 12, 0, 5, now()),
  (:'book_poeaa', 8, 0, 5, now()),
  (:'book_sapiens', 33, 0, 5, now()),
  (:'book_homodeus', 27, 0, 5, now()),
  (:'book_stevejobs', 19, 0, 5, now())
ON CONFLICT DO NOTHING;

-- Virtual editions for a subset of books, matching each book's own currency/price scale.
INSERT INTO catalog.virtual_editions (book_id, file_url, file_format, file_size_bytes, price, currency, is_active) VALUES
  (:'book_1984', 's3://readora-virtual-editions/9780451524935.epub', 'EPUB', 512000, 249.00, 'INR', true),
  (:'book_foundation', 's3://readora-virtual-editions/9780553293357.epub', 'EPUB', 480000, 299.00, 'INR', true),
  (:'book_dune', 's3://readora-virtual-editions/9780441172719.epub', 'EPUB', 890000, 449.00, 'INR', true),
  (:'book_martian', 's3://readora-virtual-editions/9780553418026.epub', 'EPUB', 610000, 249.00, 'INR', true),
  (:'book_cleancode', 's3://readora-virtual-editions/9780132350884.pdf', 'PDF', 3200000, 699.00, 'INR', true),
  (:'book_cleanarch', 's3://readora-virtual-editions/9780134494166.pdf', 'PDF', 2900000, 749.00, 'INR', true),
  (:'book_refactoring', 's3://readora-virtual-editions/9780134757599.pdf', 'PDF', 3400000, 849.00, 'INR', true)
ON CONFLICT DO NOTHING;
