
CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE users.addresses (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    label character varying(255) NOT NULL,
    recipient_type character varying(255) DEFAULT 'OWNER'::character varying NOT NULL,
    recipient_name character varying(255) NOT NULL,
    line1 character varying(255) NOT NULL,
    line2 character varying(255),
    city character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    postal_code character varying(255) NOT NULL,
    country_code character varying(255) NOT NULL,
    store_id uuid,
    phone character varying(255),
    is_default boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT chk_address_label CHECK (((label)::text = ANY (ARRAY[('HOME'::character varying)::text, ('WORK'::character varying)::text, ('OTHER'::character varying)::text]))),
    CONSTRAINT chk_address_recipient_type CHECK (((recipient_type)::text = ANY (ARRAY[('OWNER'::character varying)::text, ('GUEST'::character varying)::text])))
);

CREATE TABLE users.coupon_redemptions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    coupon_id uuid NOT NULL,
    user_id uuid NOT NULL,
    redeemed_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE users.coupons (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(255) NOT NULL,
    amount numeric(10,2) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    expires_at timestamp with time zone
);

CREATE TABLE users.user_profiles (
    user_id uuid NOT NULL,
    display_name character varying(255),
    avatar_url character varying(255),
    phone character varying(255),
    date_of_birth date,
    locale character varying(255),
    marketing_opt_in boolean DEFAULT false NOT NULL,
    preferred_store_id uuid,
    admin_store_id uuid,
    favorite_category_ids text
);

CREATE TABLE users.wallet_accounts (
    user_id uuid NOT NULL,
    balance numeric(10,2) DEFAULT 0 NOT NULL,
    currency character varying(3) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE users.wallet_transactions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    order_id uuid,
    amount numeric(10,2) NOT NULL,
    type character varying(255) NOT NULL,
    balance_after numeric(10,2) NOT NULL,
    idempotency_key character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_wallet_transaction_type CHECK (((type)::text = ANY (ARRAY[('SIGNUP_BONUS'::character varying)::text, ('REFERRAL_BONUS'::character varying)::text, ('REDEEMED'::character varying)::text, ('REVERSED'::character varying)::text, ('TOPUP'::character varying)::text, ('COUPON_REDEEMED'::character varying)::text])))
);

CREATE TABLE users.wishlist_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    book_id uuid NOT NULL,
    added_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY users.addresses
    ADD CONSTRAINT addresses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY users.coupon_redemptions
    ADD CONSTRAINT coupon_redemptions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY users.coupons
    ADD CONSTRAINT coupons_code_key UNIQUE (code);

ALTER TABLE ONLY users.coupons
    ADD CONSTRAINT coupons_pkey PRIMARY KEY (id);

ALTER TABLE ONLY users.wishlist_items
    ADD CONSTRAINT ukik9exvngvj5j4ro9xdpafjj8l UNIQUE (user_id, book_id);

ALTER TABLE ONLY users.coupon_redemptions
    ADD CONSTRAINT uq_coupon_redemptions_coupon_user UNIQUE (coupon_id, user_id);

ALTER TABLE ONLY users.wishlist_items
    ADD CONSTRAINT uq_wishlist_user_book UNIQUE (user_id, book_id);

ALTER TABLE ONLY users.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY users.wallet_accounts
    ADD CONSTRAINT wallet_accounts_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY users.wallet_transactions
    ADD CONSTRAINT wallet_transactions_idempotency_key_key UNIQUE (idempotency_key);

ALTER TABLE ONLY users.wallet_transactions
    ADD CONSTRAINT wallet_transactions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY users.wishlist_items
    ADD CONSTRAINT wishlist_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY users.coupon_redemptions
    ADD CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES users.coupons(id) ON DELETE CASCADE;

