
CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE payments.outbox_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    aggregate_type character varying(255) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type character varying(255) NOT NULL,
    payload text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    published_at timestamp with time zone
);

CREATE TABLE payments.payment_attempts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    payment_id uuid NOT NULL,
    attempt_no integer NOT NULL,
    status character varying(255) NOT NULL,
    provider_response text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE payments.payments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    user_id uuid NOT NULL,
    method character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    amount numeric(10,2) NOT NULL,
    wallet_amount_used numeric(10,2) DEFAULT 0 NOT NULL,
    idempotency_key character varying(255) NOT NULL,
    authorized_at timestamp with time zone,
    captured_at timestamp with time zone,
    failure_code character varying(255),
    failure_reason character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_payment_method CHECK (((method)::text = ANY (ARRAY[('CARD'::character varying)::text, ('UPI'::character varying)::text, ('NETBANKING'::character varying)::text, ('WALLET'::character varying)::text]))),
    CONSTRAINT chk_payment_status CHECK (((status)::text = ANY (ARRAY[('INITIATED'::character varying)::text, ('AUTHORIZED'::character varying)::text, ('CAPTURED'::character varying)::text, ('FAILED'::character varying)::text, ('REFUNDED'::character varying)::text])))
);

CREATE TABLE payments.refunds (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    payment_id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    status character varying(255) NOT NULL,
    reason character varying(255),
    completed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_refund_status CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('COMPLETED'::character varying)::text, ('FAILED'::character varying)::text])))
);

ALTER TABLE ONLY payments.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY payments.payment_attempts
    ADD CONSTRAINT payment_attempts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY payments.payments
    ADD CONSTRAINT payments_idempotency_key_key UNIQUE (idempotency_key);

ALTER TABLE ONLY payments.payments
    ADD CONSTRAINT payments_order_id_key UNIQUE (order_id);

ALTER TABLE ONLY payments.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY payments.refunds
    ADD CONSTRAINT refunds_pkey PRIMARY KEY (id);

CREATE INDEX idx_payment_attempts_payment_id ON payments.payment_attempts USING btree (payment_id);

CREATE INDEX idx_payments_user_id ON payments.payments USING btree (user_id);

CREATE INDEX idx_refunds_payment_id ON payments.refunds USING btree (payment_id);

ALTER TABLE ONLY payments.payment_attempts
    ADD CONSTRAINT fk_payment_attempts_payment FOREIGN KEY (payment_id) REFERENCES payments.payments(id) ON DELETE CASCADE;

ALTER TABLE ONLY payments.refunds
    ADD CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments.payments(id) ON DELETE CASCADE;

