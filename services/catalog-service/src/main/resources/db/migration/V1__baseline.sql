
CREATE SCHEMA IF NOT EXISTS catalog;

CREATE TABLE catalog.authors (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    slug character varying(255) NOT NULL,
    bio text,
    photo_url text
);

CREATE TABLE catalog.book_authors (
    book_id uuid NOT NULL,
    author_id uuid NOT NULL
);

CREATE TABLE catalog.book_images (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    book_id uuid NOT NULL,
    url character varying(255) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);

CREATE TABLE catalog.books (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    isbn13 character varying(13) NOT NULL,
    title character varying(255) NOT NULL,
    subtitle character varying(255),
    description text,
    table_of_contents text,
    category_id uuid,
    publisher_id uuid NOT NULL,
    store_id uuid,
    language character varying(255) NOT NULL,
    page_count integer,
    published_on date,
    list_price numeric(10,2) NOT NULL,
    currency character varying(3) NOT NULL,
    cover_image_url character varying(255),
    is_active boolean DEFAULT true NOT NULL,
    created_by_user_id uuid,
    embedded_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE catalog.categories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    slug character varying(255) NOT NULL,
    display_order integer DEFAULT 0 NOT NULL
);

CREATE TABLE catalog.inventory (
    book_id uuid NOT NULL,
    qty_on_hand integer DEFAULT 0 NOT NULL,
    qty_reserved integer DEFAULT 0 NOT NULL,
    reorder_threshold integer DEFAULT 0 NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE catalog.outbox_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    aggregate_type character varying(255) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type character varying(255) NOT NULL,
    payload text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    published_at timestamp with time zone
);

CREATE TABLE catalog.publishers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    slug character varying(255) NOT NULL
);

CREATE TABLE catalog.related_books (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    book_id uuid NOT NULL,
    related_book_id uuid NOT NULL,
    relation_type character varying(255) NOT NULL,
    CONSTRAINT chk_related_books_not_self CHECK ((book_id <> related_book_id))
);

CREATE TABLE catalog.reviews (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    book_id uuid NOT NULL,
    user_id uuid NOT NULL,
    author_display_name character varying(255),
    rating integer NOT NULL,
    comment text,
    verified_purchase boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_reviews_rating CHECK (((rating >= 1) AND (rating <= 5)))
);

CREATE TABLE catalog.stores (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    city character varying(255) NOT NULL,
    line1 character varying(255) NOT NULL,
    line2 character varying(255),
    state character varying(255) NOT NULL,
    postal_code character varying(255) NOT NULL,
    country_code character varying(2) NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);

CREATE TABLE catalog.virtual_editions (
    book_id uuid NOT NULL,
    file_url character varying(255) NOT NULL,
    file_format character varying(255) NOT NULL,
    file_size_bytes bigint,
    price numeric(10,2) NOT NULL,
    currency character varying(3) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_by_user_id uuid,
    CONSTRAINT chk_virtual_edition_format CHECK (((file_format)::text = ANY (ARRAY[('PDF'::character varying)::text, ('EPUB'::character varying)::text])))
);

ALTER TABLE ONLY catalog.authors
    ADD CONSTRAINT authors_pkey PRIMARY KEY (id);

ALTER TABLE ONLY catalog.authors
    ADD CONSTRAINT authors_slug_key UNIQUE (slug);

ALTER TABLE ONLY catalog.book_authors
    ADD CONSTRAINT book_authors_pkey PRIMARY KEY (book_id, author_id);

ALTER TABLE ONLY catalog.book_images
    ADD CONSTRAINT book_images_pkey PRIMARY KEY (id);

ALTER TABLE ONLY catalog.books
    ADD CONSTRAINT books_isbn13_key UNIQUE (isbn13);

ALTER TABLE ONLY catalog.books
    ADD CONSTRAINT books_pkey PRIMARY KEY (id);

ALTER TABLE ONLY catalog.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY catalog.categories
    ADD CONSTRAINT categories_slug_key UNIQUE (slug);

ALTER TABLE ONLY catalog.inventory
    ADD CONSTRAINT inventory_pkey PRIMARY KEY (book_id);

ALTER TABLE ONLY catalog.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY catalog.publishers
    ADD CONSTRAINT publishers_pkey PRIMARY KEY (id);

ALTER TABLE ONLY catalog.publishers
    ADD CONSTRAINT publishers_slug_key UNIQUE (slug);

ALTER TABLE ONLY catalog.related_books
    ADD CONSTRAINT related_books_pkey PRIMARY KEY (id);

ALTER TABLE ONLY catalog.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);

ALTER TABLE ONLY catalog.stores
    ADD CONSTRAINT stores_pkey PRIMARY KEY (id);

ALTER TABLE ONLY catalog.reviews
    ADD CONSTRAINT uk8dwwvmbh89prx5sbwddb860tc UNIQUE (book_id, user_id);

ALTER TABLE ONLY catalog.related_books
    ADD CONSTRAINT uq_related_books_pair UNIQUE (book_id, related_book_id);

ALTER TABLE ONLY catalog.reviews
    ADD CONSTRAINT uq_reviews_book_user UNIQUE (book_id, user_id);

ALTER TABLE ONLY catalog.virtual_editions
    ADD CONSTRAINT virtual_editions_pkey PRIMARY KEY (book_id);

CREATE INDEX idx_book_authors_author_id ON catalog.book_authors USING btree (author_id);

CREATE INDEX idx_book_images_book_id ON catalog.book_images USING btree (book_id);

CREATE INDEX idx_books_category_id ON catalog.books USING btree (category_id);

CREATE INDEX idx_books_publisher_id ON catalog.books USING btree (publisher_id);

CREATE INDEX idx_books_store_id ON catalog.books USING btree (store_id);

CREATE INDEX idx_related_books_book_id ON catalog.related_books USING btree (book_id);

ALTER TABLE ONLY catalog.book_authors
    ADD CONSTRAINT fk_book_authors_author FOREIGN KEY (author_id) REFERENCES catalog.authors(id) ON DELETE CASCADE;

ALTER TABLE ONLY catalog.book_authors
    ADD CONSTRAINT fk_book_authors_book FOREIGN KEY (book_id) REFERENCES catalog.books(id) ON DELETE CASCADE;

ALTER TABLE ONLY catalog.book_images
    ADD CONSTRAINT fk_book_images_book FOREIGN KEY (book_id) REFERENCES catalog.books(id) ON DELETE CASCADE;

ALTER TABLE ONLY catalog.books
    ADD CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES catalog.categories(id);

ALTER TABLE ONLY catalog.books
    ADD CONSTRAINT fk_books_publisher FOREIGN KEY (publisher_id) REFERENCES catalog.publishers(id);

ALTER TABLE ONLY catalog.books
    ADD CONSTRAINT fk_books_store FOREIGN KEY (store_id) REFERENCES catalog.stores(id);

ALTER TABLE ONLY catalog.inventory
    ADD CONSTRAINT fk_inventory_book FOREIGN KEY (book_id) REFERENCES catalog.books(id) ON DELETE CASCADE;

ALTER TABLE ONLY catalog.related_books
    ADD CONSTRAINT fk_related_books_book FOREIGN KEY (book_id) REFERENCES catalog.books(id) ON DELETE CASCADE;

ALTER TABLE ONLY catalog.related_books
    ADD CONSTRAINT fk_related_books_related FOREIGN KEY (related_book_id) REFERENCES catalog.books(id) ON DELETE CASCADE;

ALTER TABLE ONLY catalog.reviews
    ADD CONSTRAINT fk_reviews_book FOREIGN KEY (book_id) REFERENCES catalog.books(id) ON DELETE CASCADE;

ALTER TABLE ONLY catalog.virtual_editions
    ADD CONSTRAINT fk_virtual_editions_book FOREIGN KEY (book_id) REFERENCES catalog.books(id) ON DELETE CASCADE;

