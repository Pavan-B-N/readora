
CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE notifications.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    message character varying(255) NOT NULL,
    order_id uuid,
    read boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY notifications.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

CREATE INDEX idx_notifications_user_id ON notifications.notifications USING btree (user_id, created_at DESC);

