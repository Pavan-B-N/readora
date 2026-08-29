
CREATE SCHEMA IF NOT EXISTS commerce;

CREATE TABLE commerce.order_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    book_id uuid NOT NULL,
    title_snapshot character varying(255) NOT NULL,
    isbn_snapshot character varying(13),
    unit_price_snapshot numeric(10,2) NOT NULL,
    qty integer NOT NULL,
    line_total numeric(10,2) NOT NULL,
    delivery_type character varying(255) DEFAULT 'PHYSICAL'::character varying NOT NULL,
    CONSTRAINT chk_order_items_delivery_type CHECK (((delivery_type)::text = ANY (ARRAY[('PHYSICAL'::character varying)::text, ('VIRTUAL'::character varying)::text])))
);

CREATE TABLE commerce.order_shipping_addresses (
    order_id uuid NOT NULL,
    recipient_name character varying(255) NOT NULL,
    line1 character varying(255) NOT NULL,
    line2 character varying(255),
    city character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    postal_code character varying(255) NOT NULL,
    country_code character varying(2) NOT NULL,
    phone character varying(255)
);

CREATE TABLE commerce.order_status_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    from_status character varying(255),
    to_status character varying(255) NOT NULL,
    reason character varying(255),
    changed_by character varying(255),
    changed_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_order_history_from_status CHECK (((from_status)::text = ANY ((ARRAY['PENDING_PAYMENT'::character varying, 'PAID'::character varying, 'CONFIRMED'::character varying, 'ASSIGNED'::character varying, 'SHIPPED'::character varying, 'DELIVERED'::character varying, 'PAYMENT_FAILED'::character varying, 'CANCELLED'::character varying, 'RETURN_REQUESTED'::character varying, 'RETURN_REJECTED'::character varying, 'RETURN_APPROVED'::character varying, 'RETURN_ASSIGNED'::character varying, 'RETURN_EN_ROUTE'::character varying, 'RETURN_COLLECTED'::character varying, 'REFUND_INITIATED'::character varying, 'RETURNED'::character varying])::text[]))),
    CONSTRAINT chk_order_history_to_status CHECK (((to_status)::text = ANY ((ARRAY['PENDING_PAYMENT'::character varying, 'PAID'::character varying, 'CONFIRMED'::character varying, 'ASSIGNED'::character varying, 'SHIPPED'::character varying, 'DELIVERED'::character varying, 'PAYMENT_FAILED'::character varying, 'CANCELLED'::character varying, 'RETURN_REQUESTED'::character varying, 'RETURN_REJECTED'::character varying, 'RETURN_APPROVED'::character varying, 'RETURN_ASSIGNED'::character varying, 'RETURN_EN_ROUTE'::character varying, 'RETURN_COLLECTED'::character varying, 'REFUND_INITIATED'::character varying, 'RETURNED'::character varying])::text[])))
);

CREATE TABLE commerce.orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_number character varying(255) NOT NULL,
    user_id uuid NOT NULL,
    status character varying(255) NOT NULL,
    currency character varying(3) NOT NULL,
    subtotal numeric(10,2) NOT NULL,
    shipping_fee numeric(10,2) NOT NULL,
    packaging_fee numeric(10,2) DEFAULT 0 NOT NULL,
    tax_amount numeric(10,2) NOT NULL,
    grand_total numeric(10,2) NOT NULL,
    wallet_amount_used numeric(10,2) DEFAULT 0 NOT NULL,
    payment_method character varying(255) DEFAULT 'WALLET'::character varying NOT NULL,
    placed_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    cancel_reason character varying(255),
    idempotency_key character varying(255) NOT NULL,
    delivery_type character varying(255) NOT NULL,
    store_id uuid,
    delivery_agent_id uuid,
    delivery_agent_name character varying(255),
    delivered_at timestamp with time zone,
    admin_reviewed_at timestamp with time zone,
    admin_reviewed_by_user_id uuid,
    admin_note text,
    return_agent_id uuid,
    return_agent_name character varying(255),
    CONSTRAINT chk_order_delivery_type CHECK (((delivery_type)::text = ANY (ARRAY[('PHYSICAL'::character varying)::text, ('VIRTUAL'::character varying)::text]))),
    CONSTRAINT chk_order_status CHECK (((status)::text = ANY ((ARRAY['PENDING_PAYMENT'::character varying, 'PAID'::character varying, 'CONFIRMED'::character varying, 'ASSIGNED'::character varying, 'SHIPPED'::character varying, 'DELIVERED'::character varying, 'PAYMENT_FAILED'::character varying, 'CANCELLED'::character varying, 'RETURN_REQUESTED'::character varying, 'RETURN_REJECTED'::character varying, 'RETURN_APPROVED'::character varying, 'RETURN_ASSIGNED'::character varying, 'RETURN_EN_ROUTE'::character varying, 'RETURN_COLLECTED'::character varying, 'REFUND_INITIATED'::character varying, 'RETURNED'::character varying])::text[])))
);

CREATE TABLE commerce.outbox_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    aggregate_type character varying(255) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type character varying(255) NOT NULL,
    payload text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    published_at timestamp with time zone
);

CREATE TABLE commerce.return_messages (
    id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    sender_role character varying(255) NOT NULL,
    sender_user_id uuid NOT NULL,
    order_id uuid NOT NULL,
    CONSTRAINT return_messages_sender_role_check CHECK (((sender_role)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'ADMIN'::character varying])::text[])))
);

ALTER TABLE ONLY commerce.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY commerce.order_shipping_addresses
    ADD CONSTRAINT order_shipping_addresses_pkey PRIMARY KEY (order_id);

ALTER TABLE ONLY commerce.order_status_history
    ADD CONSTRAINT order_status_history_pkey PRIMARY KEY (id);

ALTER TABLE ONLY commerce.orders
    ADD CONSTRAINT orders_idempotency_key_key UNIQUE (idempotency_key);

ALTER TABLE ONLY commerce.orders
    ADD CONSTRAINT orders_order_number_key UNIQUE (order_number);

ALTER TABLE ONLY commerce.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY commerce.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY commerce.return_messages
    ADD CONSTRAINT return_messages_pkey PRIMARY KEY (id);

CREATE INDEX idx_order_items_order_id ON commerce.order_items USING btree (order_id);

CREATE INDEX idx_order_status_history_order_id ON commerce.order_status_history USING btree (order_id);

CREATE INDEX idx_orders_user_id ON commerce.orders USING btree (user_id);

ALTER TABLE ONLY commerce.return_messages
    ADD CONSTRAINT fk8qc54p0ib7hkk4ahgvcygnjx FOREIGN KEY (order_id) REFERENCES commerce.orders(id);

ALTER TABLE ONLY commerce.order_items
    ADD CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES commerce.orders(id) ON DELETE CASCADE;

ALTER TABLE ONLY commerce.order_shipping_addresses
    ADD CONSTRAINT fk_order_shipping_address_order FOREIGN KEY (order_id) REFERENCES commerce.orders(id) ON DELETE CASCADE;

ALTER TABLE ONLY commerce.order_status_history
    ADD CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES commerce.orders(id) ON DELETE CASCADE;

