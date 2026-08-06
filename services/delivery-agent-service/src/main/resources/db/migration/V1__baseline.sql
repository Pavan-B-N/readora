
CREATE SCHEMA IF NOT EXISTS delivery;

CREATE TABLE delivery.delivery_agents (
    user_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    phone character varying(255),
    store_id uuid NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    on_duty boolean DEFAULT false NOT NULL
);

CREATE TABLE delivery.delivery_assignments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    order_number character varying(255) NOT NULL,
    store_id uuid NOT NULL,
    agent_id uuid,
    status character varying(255) DEFAULT 'UNASSIGNED'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    assigned_at timestamp with time zone,
    out_for_delivery_at timestamp with time zone,
    delivered_at timestamp with time zone,
    destination_city character varying(255),
    CONSTRAINT chk_delivery_assignment_status CHECK (((status)::text = ANY (ARRAY[('UNASSIGNED'::character varying)::text, ('ASSIGNED'::character varying)::text, ('OUT_FOR_DELIVERY'::character varying)::text, ('DELIVERED'::character varying)::text])))
);

CREATE TABLE delivery.return_pickup_assignments (
    id uuid NOT NULL,
    agent_id uuid,
    agent_name character varying(255),
    assigned_at timestamp(6) with time zone,
    collected_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    destination_city character varying(255),
    en_route_at timestamp(6) with time zone,
    order_id uuid NOT NULL,
    order_number character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    store_id uuid NOT NULL,
    CONSTRAINT return_pickup_assignments_status_check CHECK (((status)::text = ANY ((ARRAY['UNASSIGNED'::character varying, 'ASSIGNED'::character varying, 'EN_ROUTE'::character varying, 'COLLECTED'::character varying])::text[])))
);

ALTER TABLE ONLY delivery.delivery_agents
    ADD CONSTRAINT delivery_agents_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY delivery.delivery_assignments
    ADD CONSTRAINT delivery_assignments_order_id_key UNIQUE (order_id);

ALTER TABLE ONLY delivery.delivery_assignments
    ADD CONSTRAINT delivery_assignments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY delivery.return_pickup_assignments
    ADD CONSTRAINT return_pickup_assignments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY delivery.return_pickup_assignments
    ADD CONSTRAINT uk93w3vmi87o2qqiqru4ioy6sek UNIQUE (order_id);

