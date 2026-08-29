
CREATE EXTENSION IF NOT EXISTS vector;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE SCHEMA IF NOT EXISTS ai;

CREATE TABLE ai.book_content_vector_store (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    content text,
    metadata json,
    embedding public.vector(1536)
);

CREATE TABLE ai.book_reader_index (
    book_id uuid NOT NULL,
    chunk_count integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    error_message character varying(255),
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT book_reader_index_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'READY'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE ai.conversations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    title character varying(255),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    book_id uuid
);

CREATE TABLE ai.embedding_job_book_logs (
    id uuid NOT NULL,
    book_id uuid NOT NULL,
    processed_at timestamp(6) with time zone NOT NULL,
    title character varying(255) NOT NULL,
    job_id uuid NOT NULL
);

CREATE TABLE ai.embedding_jobs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    status character varying(255) NOT NULL,
    triggered_by uuid,
    total_books integer DEFAULT 0 NOT NULL,
    processed_books integer DEFAULT 0 NOT NULL,
    current_book_title character varying(255),
    error_message text,
    queued_at timestamp with time zone DEFAULT now() NOT NULL,
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    CONSTRAINT chk_embedding_job_status CHECK (((status)::text = ANY (ARRAY[('QUEUED'::character varying)::text, ('RUNNING'::character varying)::text, ('COMPLETED'::character varying)::text, ('FAILED'::character varying)::text])))
);

CREATE TABLE ai.messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_id uuid NOT NULL,
    role character varying(255) NOT NULL,
    content text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    book_ids text[],
    CONSTRAINT chk_message_role CHECK (((role)::text = ANY (ARRAY[('USER'::character varying)::text, ('ASSISTANT'::character varying)::text])))
);

CREATE TABLE ai.vector_store (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    content text,
    metadata json,
    embedding public.vector(1536)
);

ALTER TABLE ONLY ai.book_content_vector_store
    ADD CONSTRAINT book_content_vector_store_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ai.book_reader_index
    ADD CONSTRAINT book_reader_index_pkey PRIMARY KEY (book_id);

ALTER TABLE ONLY ai.conversations
    ADD CONSTRAINT conversations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ai.embedding_job_book_logs
    ADD CONSTRAINT embedding_job_book_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ai.embedding_jobs
    ADD CONSTRAINT embedding_jobs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ai.messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ai.vector_store
    ADD CONSTRAINT vector_store_pkey PRIMARY KEY (id);

CREATE INDEX book_content_vector_store_index ON ai.book_content_vector_store USING hnsw (embedding public.vector_cosine_ops);

CREATE INDEX idx_conversations_user_id ON ai.conversations USING btree (user_id);

CREATE INDEX idx_messages_conversation_id ON ai.messages USING btree (conversation_id);

CREATE INDEX spring_ai_vector_index ON ai.vector_store USING hnsw (embedding public.vector_cosine_ops);

ALTER TABLE ONLY ai.messages
    ADD CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES ai.conversations(id) ON DELETE CASCADE;

ALTER TABLE ONLY ai.embedding_job_book_logs
    ADD CONSTRAINT fkqs7q0lphwwyojo1dev3shf704 FOREIGN KEY (job_id) REFERENCES ai.embedding_jobs(id);

