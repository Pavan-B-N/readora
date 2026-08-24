# Readora — Engineering Notes & Interview Prep

> Everything you need to explain this project from memory: what was built, why each technology
> was chosen, how the pieces fit, and the questions an interviewer will ask about each.
>
> Scope: backend services, PostgreSQL, and infrastructure. Frontends are mentioned only where
> they touch the backend contract.

---

## Table of Contents

1. [The 60-Second Pitch](#1-the-60-second-pitch)
2. [Architecture at a Glance](#2-architecture-at-a-glance)
3. [Tech Stack and Why](#3-tech-stack-and-why)
4. [Core Concepts Explained](#4-core-concepts-explained)
5. [The Database](#5-the-database)
6. [End-to-End Request Flows](#6-end-to-end-request-flows)
7. [Security Model](#7-security-model)
8. [Infrastructure & Local Dev](#8-infrastructure--local-dev)
9. [Design Decisions and Trade-offs](#9-design-decisions-and-trade-offs)
10. [Known Gaps](#10-known-gaps-be-honest-about-these)
11. [Interview Questions](#11-interview-questions)
12. [Quick Reference Cheat Sheet](#12-quick-reference-cheat-sheet)

---

## 1. The 60-Second Pitch

**Readora is a books-only e-commerce platform built as 9 Spring Boot microservices**, backed by
PostgreSQL, Redis, and Kafka, with an AI layer for semantic search and conversational assistance.

What makes it more than a CRUD app:

- **Two fulfilment models** — physical books (shipped, stock-reserved) and virtual editions
  (instant digital delivery, no stock). An order is entirely one or the other.
- **Event-driven order processing** — checkout returns immediately; payment settles
  asynchronously over Kafka using the **transactional outbox pattern**.
- **Semantic search via RAG** — book text is embedded into pgvector, so "dystopian totalitarian
  society" finds *1984* without those words appearing in the title.
- **Deny-by-default security at every layer** — gateway JWT validation, a shared-secret check so
  services reject direct traffic, and per-service role enforcement.

**Elevator version:** "A microservices bookstore where the interesting parts are the async
order/payment choreography over Kafka with an outbox pattern, and a RAG pipeline that keeps
book embeddings fresh through domain events."

---

## 2. Architecture at a Glance

### Service Map

| Service | Port | Owns | Responsibility |
|---|---|---|---|
| **api-gateway** | 8080 | — | Single entry point. Routing, JWT validation, rate limiting, CORS |
| **auth-service** | 8081 | `auth` schema | Registration, login, JWT issuing, refresh-token rotation |
| **user-service** | 8082 | `users` schema | Profiles, addresses, wallet + ledger |
| **catalog-service** | 8083 | `catalog` schema | Books, authors, categories, publishers, inventory, virtual editions |
| **commerce-service** | 8084 | `commerce` schema | Cart (Redis), orders, checkout, cancellation |
| **payment-service** | 8085 | `payments` schema | Payments, refunds (dummy provider) |
| **notification-service** | 8086 | — (stateless) | WebSocket/STOMP push notifications |
| **mcp-server** | 8087 | — (stateless) | Read-only MCP tools for the AI assistant |
| **ai-service** | 8088 | `ai` schema | RAG search, chat, embedding pipeline |

### The Shape of It

```
                    Browser (frontend-user :5173 / frontend-admin :5174)
                                       │
                                       ▼
                        ┌──────────────────────────────┐
                        │      api-gateway :8080       │
                        │  JWT · rate limit · routing  │
                        └──────────────────────────────┘
                                       │  (adds X-User-Id, X-User-Roles,
                                       │   X-Gateway-Secret, X-Correlation-Id)
        ┌──────────┬──────────┬────────┼────────┬──────────┬──────────┐
        ▼          ▼          ▼        ▼        ▼          ▼          ▼
      auth       user      catalog  commerce  payment   ai-service   (notification
      :8081      :8082      :8083    :8084     :8085      :8088       :8086 — direct
        │          │          │        │         │          │          WebSocket)
        └──────────┴──────────┴────────┴─────────┴──────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              ▼                        ▼                        ▼
        PostgreSQL                  Kafka                     Redis
    (6 schemas, 1 DB)        (event choreography)     (cart + rate-limit buckets)
```

### Key architectural rules

1. **Each service owns its schema.** No service reads another's tables — ever. Cross-service data
   comes over HTTP (synchronous) or Kafka (asynchronous).
2. **Cross-service references are plain UUID columns, never JPA relationships.** `payments.payments.user_id`
   is a `UUID` column, not a `@ManyToOne User` — because that entity lives in another service's schema.
3. **The gateway is the only public door.** Every service rejects requests that don't carry the
   shared gateway secret.

---

## 3. Tech Stack and Why

| Technology | Version | Why this, not something else |
|---|---|---|
| **Java** | 21 | LTS. Records, pattern matching, sealed types |
| **Spring Boot** | 3.3.13 | Industry standard; the whole ecosystem (Data, Security, Kafka) lines up |
| **Spring Cloud Gateway** | 2023.0.6 | Reactive (WebFlux) gateway — non-blocking, right for a proxy that's mostly I/O wait |
| **PostgreSQL** | 18 | ACID, mature, and `pgvector` means no separate vector DB |
| **Redis** | 7 | Sub-millisecond reads for cart; distributed counters for rate limiting |
| **Apache Kafka** | 3.8 (KRaft) | Durable, replayable event log — see [why Kafka](#42-kafka-and-event-driven-architecture) |
| **Spring AI** | 1.0.0 | Provider-agnostic `ChatClient` / `VectorStore` abstractions |
| **Azure OpenAI** | — | `gpt-4o` (chat) + `text-embedding-ada-002` (embeddings) |
| **Bucket4j** | — | Token-bucket rate limiting with distributed Redis state |
| **JJWT** | 0.12.6 | JWT signing/validation |
| **OpenTelemetry** | 2.31.1 agent | Vendor-neutral tracing; zero code changes |
| **Maven** | multi-module | One reactor build, shared parent POM |

**Note on the reactive/blocking split:** api-gateway uses **WebFlux** (reactive, non-blocking) —
correct for a proxy handling many concurrent connections that mostly wait on I/O. Every other
service uses **Spring MVC** (servlet, blocking) — simpler to reason about, and they do real
CPU/DB work where reactive adds complexity without much payoff. Being able to explain *why the
split exists* is a strong interview signal.

---

## 4. Core Concepts Explained

### 4.1 Microservices

**What:** The application is split into independently deployable services, each owning one
business capability and its own database schema.

**Why here:**
- **Independent scaling** — catalog browsing gets far more traffic than checkout.
- **Fault isolation** — if ai-service dies, people can still buy books.
- **Clear ownership** — payment logic lives in exactly one place.

**The cost (be ready to name it):** distributed transactions are hard, debugging spans multiple
logs, network calls fail in ways in-process calls don't, and eventual consistency becomes
something users can observe.

**How boundaries were drawn:** by *business capability*, not by technical layer. There's no
"database service" or "controller service" — each service is a vertical slice owning its data,
logic, and API.

---

### 4.2 Kafka and Event-Driven Architecture

#### What is Kafka?

Kafka is a **distributed, append-only, durable log**. Producers append messages to **topics**;
consumers read at their own pace, tracking their position with an **offset**.

Unlike a traditional queue (RabbitMQ, SQS) where a message is consumed and gone, Kafka
**retains** messages. Multiple independent consumer groups can read the same topic, and a
consumer can rewind and replay history.

Key terms:
- **Topic** — a named stream of events (e.g. `order.created`)
- **Partition** — topics split for parallelism; ordering is guaranteed *within* a partition
- **Consumer group** — consumers sharing a `groupId` split partitions between them; each group
  gets its own copy of the stream
- **Offset** — a consumer's bookmark
- **KRaft** — Kafka's built-in consensus, replacing the old ZooKeeper dependency (this project uses KRaft)

#### Why Kafka here, specifically?

Consider checkout without it — commerce-service would synchronously call payment-service, wait,
then call user-service to adjust the wallet, then notification-service. Problems:

1. **Slow** — the user waits for the entire chain.
2. **Fragile** — if notification-service is down, does checkout fail? It shouldn't.
3. **Tightly coupled** — adding "email a receipt" means editing commerce-service.

With Kafka, commerce-service publishes `order.created` and returns immediately. payment-service,
user-service, and notification-service each react on their own schedule. **Adding a new consumer
requires zero changes to the producer** — that's the real payoff.

#### The topics in this system

| Topic | Produced by | Consumed by | Meaning |
|---|---|---|---|
| `order.created` | commerce | payment | An order was placed, needs payment |
| `order.cancelled` | commerce | payment | Order cancelled, issue a refund |
| `payment.captured` | payment | commerce, user, notification | Money taken successfully |
| `payment.failed` | payment | commerce, notification | Payment failed |
| `refund.completed` | payment | user, notification | Refund processed |
| `book.upserted` | catalog | ai | Book created/updated — re-embed it |
| `embedding.backfill.requested` | ai | ai | Run a full catalogue re-embed asynchronously |

Note `payment.captured` has **three** independent consumers, each doing something different with
the same event. That's the pattern working as intended.

#### Choreography vs Orchestration

This system uses **choreography** — each service reacts to events and publishes its own; there's
no central coordinator. The alternative, **orchestration** (a saga coordinator telling each
service what to do), is easier to trace and reason about but reintroduces a central point of
coupling. For a system this size, choreography is the lighter choice.

---

### 4.3 The Transactional Outbox Pattern

**This is the single most interview-worthy piece of the project.** Understand it cold.

#### The problem it solves

In `checkout()`, two things must happen: save the order to Postgres, and publish `order.created`
to Kafka. These are **two different systems** — you cannot wrap them in one transaction.

Naive approach A — save, then publish:
```java
orderRepository.save(order);        // committed
kafkaTemplate.send("order.created", event);   // ← network fails here
```
The order exists but no one is ever told. Payment never happens. **Silent data loss.**

Naive approach B — publish, then save:
```java
kafkaTemplate.send("order.created", event);   // succeeds
orderRepository.save(order);        // ← transaction rolls back
```
Payment service charges a customer for an order that doesn't exist. **Worse.**

This is the **dual-write problem**.

#### The solution

Write the event **to the same database, in the same transaction** as the business data:

```java
@Transactional
public CheckoutResponse checkout(...) {
    orderRepository.save(order);                    // business row
    outboxEventRepository.save(new OutboxEvent(     // event row — SAME transaction
        "Order", order.getId(), "order.created", json));
}   // both commit together, or neither does — atomic
```

Then a separate scheduled job drains the outbox to Kafka:

```java
@Scheduled(fixedDelay = 2000)
public void relay() {
    for (OutboxEvent event : outboxEventRepository.findAllByPublishedAtIsNull()) {
        kafkaTemplate.send(event.getEventType(), event.getId().toString(), event.getPayload());
        event.markPublished();
        outboxEventRepository.save(event);
    }
}
```

**Guarantee:** if the order committed, the event row committed too, and the relay will
eventually deliver it. If the relay crashes mid-run, unmarked rows are simply retried next tick.

**This is at-least-once delivery**, not exactly-once — a crash after `send()` but before
`markPublished()` re-sends the event. Which is why…

#### Consumers must be idempotent

Every consumer is written to tolerate duplicate delivery:

- **payment-service** uses `idempotencyKey = "order:" + orderId` with a unique constraint —
  a duplicate `order.created` finds the existing payment and returns early.
- **commerce-service** guards state transitions: `if (order.getStatus() != PENDING_PAYMENT) return;`
- **user-service** uses `idempotencyKey = "payment:" + paymentId` on the wallet ledger.

**Interview soundbite:** *"At-least-once delivery plus idempotent consumers gives you
effectively-once processing, without needing exactly-once semantics."*

Outbox tables live in **catalog**, **commerce**, and **payments** schemas — every service that
publishes events.

---

### 4.4 Redis — Two Distinct Uses

Redis is an in-memory key-value store. It's used for two unrelated things here; don't conflate them.

#### Use 1: The shopping cart (commerce-service)

The cart is stored as **one JSON-encoded list per user** under key `cart:{userId}`, with a
**30-day TTL**.

**Why Redis and not Postgres?**
- Carts are **high-write, low-value** — abandoned constantly, updated on every click.
- They're **naturally ephemeral** — the TTL expires abandoned carts automatically, no cleanup job.
- Sub-millisecond reads on a page users hit constantly.
- A cart isn't a business record. An *order* is — and orders go to Postgres.

**Trade-off:** Redis persistence is weaker than Postgres. Losing a cart is annoying but not a
business-critical data loss, so that's an acceptable trade.

**Design detail:** each cart item stores a `unitPriceSnapshot` — the price when the item was
added. This prevents a price change mid-session from silently altering what a user sees.

#### Use 2: Rate-limit buckets (api-gateway)

Bucket4j stores token-bucket state in Redis so that **rate limits are shared across gateway
instances**. Run three gateways behind a load balancer and a user's limit is still 20/min total,
not 60. In-memory counters would break the moment you scale horizontally.

---

### 4.5 Rate Limiting — Token Bucket

**The algorithm:** each key (route + user/IP) gets a bucket with a capacity. Every request
consumes one token. Tokens refill continuously over time. Empty bucket → `429 Too Many Requests`.

**Why token bucket over a fixed window?** A fixed-window counter ("20 requests per minute,
reset on the minute") has a **boundary-burst flaw**: 20 requests at 11:59:59 and 20 more at
12:00:00 means 40 requests in one second, technically within limits. Token buckets refill
*gradually*, so that burst isn't possible.

**Implementation** — `RateLimitingGlobalFilter` (order `0`, runs after JWT so `X-User-Id` exists):
- Key: `ratelimit:{routeId}:{userId or IP}` — authenticated users get per-user limits, anonymous
  callers fall back to IP.
- Per-route rules from config: `ai-service` gets 5/min (LLM calls are expensive), `auth-service`
  20/min, everything else the default 20/min.
- Rejection returns the same JSON error envelope as everything else, plus a `Retry-After` header.

---

### 4.6 PostgreSQL — Schema per Service

**The setup:** one physical database (`readora`), six schemas — `auth`, `users`, `catalog`,
`commerce`, `payments`, `ai`. Each service is configured with `default_schema` pointing at its own.

**Why not one database per service (the textbook answer)?** Separate databases give the
strongest isolation, but for a single-machine project that means six Postgres instances to run,
back up, and connect to. **Schema-per-service preserves the important property — no service
touches another's tables — at a fraction of the operational cost.** The migration path to
separate databases later is straightforward because no query ever crosses a schema boundary.

**Interview framing:** *"It's the logical separation of database-per-service with the
operational simplicity of one instance. The discipline that matters — no cross-schema queries —
is enforced in code."*

---

### 4.7 JPA / Hibernate Patterns

Conventions used consistently across every entity:

**ID-based `equals`/`hashCode`, null-safe:**
```java
@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Book book)) return false;
    return id != null && Objects.equals(id, book.id);
}
```
The `id != null` guard matters: two unsaved (transient) entities must never be "equal", or they'd
collapse into one entry in a `HashSet`.

**`protected` no-arg constructors** — JPA requires one for reflection; `protected` keeps
application code from using it accidentally.

**Lifecycle callbacks** for timestamps:
```java
@PrePersist  protected void onCreate() { createdAt = updatedAt = Instant.now(); }
@PreUpdate   protected void onUpdate() { updatedAt = Instant.now(); }
```

**`@MapsId` for shared-primary-key 1:1** — `Inventory` and `VirtualEdition` both use `book_id`
as *both* primary key and foreign key:
```java
@Id @Column(name = "book_id") private UUID bookId;
@OneToOne(fetch = LAZY) @MapsId @JoinColumn(name = "book_id") private Book book;
```
This enforces "at most one inventory row per book" at the schema level rather than in code.

**`@Enumerated(EnumType.STRING)`, never `ORDINAL`** — ordinal stores `0`, `1`, `2`. Insert a new
enum constant in the middle and every existing row silently changes meaning. Strings are
self-describing and reorder-safe.

**`LAZY` fetching by default** on `@ManyToOne`/`@OneToOne` to avoid loading object graphs you
don't need (the N+1 problem).

**Specifications for dynamic queries** — `BookSpecifications.withFilters(...)` composes optional
filters (query, category, publisher, format, price range) into one Criteria query, instead of a
combinatorial explosion of hand-written JPQL methods.

**`BigDecimal` for money, never `double`** — floating point can't represent `0.1` exactly.
Columns are `precision = 10, scale = 2`.

**`Instant` for timestamps, not `LocalDateTime`** — `Instant` is an unambiguous point on the
global timeline (UTC); `LocalDateTime` has no timezone and is ambiguous across regions.

---

### 4.8 RAG, Embeddings, and pgvector

#### What is an embedding?

A model converts text into a **vector** — a list of numbers (1536 of them for
`text-embedding-ada-002`) positioning that text in high-dimensional "meaning space". Texts with
similar meaning land near each other, **even with no words in common**.

#### What is RAG?

**Retrieval-Augmented Generation**: instead of relying on what an LLM memorised during training,
you *retrieve* relevant documents from your own data and feed them to the model as context.

Benefits: the model can answer about *your* catalogue, answers are grounded in real data
(reducing hallucination), and updating knowledge means updating a database row rather than
retraining.

#### What is pgvector?

A PostgreSQL extension adding a `vector` column type and similarity operators. **Why it matters:**
no separate vector database (Pinecone, Weaviate) to run — vectors sit in the same Postgres
already being used.

#### How it works here

1. Book text is assembled: `title + authors + description + tableOfContents`
2. Azure OpenAI converts that to a 1536-dimension vector
3. The vector is stored in `ai.vector_store` with `bookId`/`title` metadata
4. A search query is embedded the same way, and pgvector finds the nearest vectors by cosine similarity

**Real result from this system:** querying *"dystopian totalitarian society"* returns **1984**
at the top (score 0.85) — the phrase appears nowhere in its title. That's semantic search working.

#### Keeping embeddings fresh — two paths

This is a genuinely good design story:

- **Incremental (the default):** admin saves a book → catalog-service writes a `book.upserted`
  outbox row → relay publishes to Kafka → ai-service's `BookEventsListener` re-embeds *that one book*.
- **Full backfill (admin-triggered, also async over Kafka):** `POST /api/v1/admin/embeddings/backfill`
  does **not** do the work inline. It creates an `ai.embedding_jobs` row, publishes
  `embedding.backfill.requested`, and returns **202 Accepted** with the job id. A separate consumer
  (`EmbeddingJobListener`, its own `ai-service-backfill` group) runs it, writing progress back to
  the job row after each page so the admin UI can poll a live percentage.

**Why both?** Events only cover changes *from now on*. They do nothing for books that already
existed, or if the vector store is wiped. The original design re-embedded everything on every
service startup — wasteful and slow. Event-driven + manual backfill is strictly better.

**Three details worth raising in an interview:**

1. **Why is the backfill async rather than a synchronous endpoint?** A full re-embed calls an
   external embedding API once per page and can run for minutes — far too long to hold an HTTP
   connection open. The request enqueues a job and returns immediately; the consumer does the work.
2. **Concurrency guard.** `requestBackfill` rejects with **409** if a job is already `QUEUED` or
   `RUNNING`. Concurrent backfills would duplicate expensive embedding-API calls for zero benefit.
3. **Progress commits in its own transaction** (`Propagation.REQUIRES_NEW`). Without that, progress
   updates would be invisible until the entire long-running job committed — defeating the point of
   polling for them.

**Job lifecycle:** `QUEUED → RUNNING → COMPLETED | FAILED`, with `totalBooks`, `processedBooks`,
`currentBookTitle`, timings, and any error message recorded for the run-history view.

**Design detail worth mentioning:** the document ID is the book's own UUID, so re-embedding
**overwrites** rather than duplicating.

---

### 4.9 MCP (Model Context Protocol) and Tools

**What:** MCP is a protocol for exposing tools to an LLM in a standard way. `mcp-server` publishes
read-only tools (`searchBooks`, `getBookDetails`, `checkInventory`, `getOrderHistory`, `getCart`,
`getUserProfile`, `getWalletBalance`) that the chat model can invoke.

#### RAG vs Tools — know the difference

This distinction gets asked, and conflating them is a red flag:

| | RAG (vector search) | Tools (MCP / REST) |
|---|---|---|
| **Good for** | Fuzzy, semantic, "find me something like…" | Exact, live, structured lookups |
| **Example** | "a bleak book about surveillance" | "what's the stock level of book X?" |
| **Data freshness** | As fresh as the last embedding | Always live |
| **Backed by** | pgvector similarity | A real service call |

You wouldn't use RAG to check inventory (needs live exactness). You wouldn't use a SQL `LIKE`
query for "bleak book about surveillance" (needs semantic understanding). **They're
complementary, not alternatives.**

**Security note:** all MCP tools are **read-only by design**. There is no `addToCart` or
`placeOrder` tool anywhere. Mutations only happen through the authenticated REST API, driven by
a real user click — never by a model's decision.

---

### 4.10 WebSockets and STOMP (notification-service)

**Why WebSockets?** HTTP is request/response — the server can't initiate. For "your payment was
captured" to appear without the user refreshing, you need a persistent bidirectional connection.

**STOMP** is a simple text protocol layered over WebSocket, giving subscribe/publish semantics
and message routing rather than raw frames.

**How it works here:** notification-service consumes `payment.captured`, `payment.failed`,
`order.cancelled`, and `refund.completed` from Kafka, then pushes to that specific user via
`convertAndSendToUser(userId, "/queue/notifications", payload)`.

**Note:** this service is **not** routed through api-gateway — proxying WebSocket/STOMP through
Spring Cloud Gateway is real added complexity that was deferred. It therefore validates JWTs
itself in `StompAuthChannelInterceptor` on the STOMP `CONNECT` frame.

---

### 4.11 OpenTelemetry and Observability

**The problem:** in a monolith a stack trace tells the whole story. In microservices one user
action touches five services — you need to correlate across them.

**Two mechanisms here:**

1. **Correlation IDs (hand-rolled)** — the gateway generates an `X-Correlation-Id` per request
   and forwards it; every service puts it into **SLF4J MDC** so it appears in every log line, and
   the `GlobalExceptionHandler` returns it as `traceId` in error responses. A user reports an
   error, quotes the trace ID, and you can grep every service's logs for that one request.

2. **OpenTelemetry Java agent** — attached via `-javaagent`, it auto-instruments Spring MVC, JDBC,
   Kafka clients, and HTTP clients through **bytecode manipulation at class load time**. No
   dependency in `pom.xml`, no code changes. Traces/metrics/logs go via OTLP to an
   **OpenTelemetry Collector**, which forwards to New Relic.

**The toggle:** `OTEL_SDK_DISABLED=true` (the default) turns all three signals off while leaving
the agent attached. Flip to `false` for one run to capture telemetry. Note this must be a **real
environment variable**, not a Spring `.env` value — the agent's `premain` runs *before* Spring boots.

---

## 5. The Database

One database, `readora`, six schemas. `pgvector` extension enabled.

### `auth` — credentials only

| Table | Notable columns |
|---|---|
| `users` | `email` (unique), `password_hash` (BCrypt), `status`, `failed_login_attempts`, `email_verified` |
| `roles` | `code` (CUSTOMER / ADMIN), `description` |
| `user_roles` | join table — many-to-many |
| `refresh_tokens` | `token_hash` (SHA-256, unique), `expires_at`, `revoked_at`, `user_agent`, `ip_address` |

**Deliberate:** this schema holds **credentials only** — no display name, no avatar. Profile data
lives in `users`. Separating identity from profile keeps the security-critical surface small.

### `users` — profile, addresses, money

| Table | Notable columns |
|---|---|
| `user_profiles` | PK is `user_id` (cross-service ref to `auth.users`), `display_name`, `locale` |
| `addresses` | `label` (HOME/WORK/OTHER), `is_default`, `deleted_at` (soft delete) |
| `wallet_accounts` | `balance`, `currency` — a **cached** balance |
| `wallet_transactions` | `amount`, `type`, `balance_after`, `idempotency_key` (unique) — **append-only ledger** |

**Two design points worth explaining:**
- **Append-only ledger.** A reversal is a *new row*, never an edit. `wallet_accounts.balance` is
  a derived cache of the ledger — the ledger is the source of truth and gives a full audit trail.
- **Soft delete on addresses** (`deleted_at`) — an order that already shipped keeps its own
  immutable address snapshot in `commerce.order_shipping_addresses`, so deleting an address
  never corrupts order history.

### `catalog` — the product domain

| Table | Notes |
|---|---|
| `books` | `isbn13` unique, `description` + `table_of_contents` (feed the RAG embedding), `is_active` |
| `authors`, `publishers`, `categories` | `categories` is **self-referencing** (`parent_id`) for a tree |
| `book_authors` | many-to-many join |
| `inventory` | `@MapsId` on `book_id`; `qty_on_hand`, `qty_reserved`; **available = on_hand − reserved** |
| `virtual_editions` | `@MapsId` on `book_id`; `file_url`, `file_format`, own `price` |
| `book_images`, `related_books` | gallery and cross-sell |
| `outbox_events` | publishes `book.upserted` |

**Note on `table_of_contents`:** stored as a **JSON string in a TEXT column**, not a relational
structure. It's never queried structurally — only flattened into text for embedding — so
normalising it would add complexity for no benefit.

**Note on reserved stock:** `qty_reserved` is why `available` is computed rather than stored. A
book with 40 on hand and 2 reserved shows 38 available. Reservation happens *at checkout*, before
payment completes, so two users can't buy the last copy.

### `commerce` — cart and orders

| Table | Notes |
|---|---|
| `orders` | `order_number` (`RDA-{year}-{6 digits}`), `status`, `delivery_type`, `idempotency_key` (unique), money columns |
| `order_items` | **snapshot columns**: `title_snapshot`, `isbn_snapshot`, `unit_price_snapshot` |
| `order_shipping_addresses` | `@MapsId` 1:1 — an immutable address snapshot |
| `order_status_history` | every transition: `from_status`, `to_status`, `changed_at`, `changed_by` |
| `outbox_events` | publishes `order.created`, `order.cancelled` |

**Snapshots are the key insight here.** `order_items` stores the title and price *as they were at
purchase time*. If a book is renamed or repriced tomorrow, the order still shows what the customer
actually bought. **Orders are historical records, not live views.**

The cart is **not** in Postgres — it's Redis (§4.4).

### `payments`

| Table | Notes |
|---|---|
| `payments` | `order_id` (unique — one payment per order), `status`, `idempotency_key` (unique) |
| `payment_attempts` | audit trail of each attempt |
| `refunds` | linked to a payment |
| `outbox_events` | publishes `payment.captured`, `payment.failed`, `refund.completed` |

**Payment status flow:** `INITIATED → AUTHORIZED → CAPTURED`, or `FAILED`, or `REFUNDED`.

**Be upfront in interviews:** the payment provider is a **dummy that auto-approves everything**.
This is a deliberate scope decision, not unfinished work — swapping in Stripe would touch only
`PaymentService`, because the event contracts around it are already correct.

### `ai`

| Table | Notes |
|---|---|
| `conversations` | chat sessions, scoped by `user_id` |
| `messages` | `role` (USER/ASSISTANT), `content` |
| `vector_store` | pgvector — 1536-dim embeddings + metadata (managed by Spring AI) |
| `embedding_jobs` | backfill run history: status, progress counters, timings, error message |

---

## 6. End-to-End Request Flows

### 6.1 Login

```
POST /api/v1/auth/login  →  gateway (public route, no JWT needed)
                         →  auth-service
                            ├─ find user by email
                            ├─ if LOCKED → 423
                            ├─ BCrypt.matches(password, hash)
                            │   └─ fail → increment counter; ≥5 → lock account → 401
                            ├─ reset counter, stamp last_login_at
                            ├─ sign JWT (sub=userId, email, roles[], 15min TTL)
                            └─ issue refresh token (raw returned, SHA-256 hash stored, 30d)
```

**Deliberate:** unknown email and wrong password both return the *same* `InvalidCredentialsException`.
Different errors would let an attacker enumerate which emails are registered.

### 6.2 Physical Checkout — the full choreography

```
POST /api/v1/orders/checkout  {deliveryType: PHYSICAL, items, address, Idempotency-Key}
   │
   ├─ gateway: validate JWT → inject X-User-Id, X-User-Roles, X-Gateway-Secret
   │
   ├─ commerce-service:
   │    ├─ empty cart? → 409
   │    ├─ PHYSICAL but no address? → 400
   │    ├─ replay of this Idempotency-Key? → return the ORIGINAL order (no duplicate)
   │    ├─ HTTP → catalog-service /internal/inventory/reserve   ← SYNCHRONOUS
   │    │     └─ atomically: available >= qty? → qty_reserved += qty, else 409
   │    ├─ price it: subtotal + 9% tax
   │    ├─ save order (PENDING_PAYMENT) + items + address snapshot   ─┐
   │    ├─ save outbox row: order.created                             ├─ ONE TRANSACTION
   │    └─ clear the Redis cart                                      ─┘
   │
   └─ 201 Created — user sees "order placed" immediately
                     ↓
   OutboxRelay (every 2s) → publishes order.created to Kafka
                     ↓
   payment-service consumes order.created
     ├─ idempotency check: already have payment for this order? → return
     ├─ authorize + capture (dummy provider — always succeeds)
     └─ outbox row: payment.captured → Kafka
                     ↓
        ┌────────────┼────────────────────┐
        ▼            ▼                    ▼
   commerce      user-service      notification-service
   PENDING_PAYMENT   debit wallet      WebSocket push
   → PAID            (if used)         "order confirmed"
   → CONFIRMED       write ledger row
```

**Why `Idempotency-Key`?** If the user double-clicks, or the network retries a request whose
response was lost, the second call must not create a second order. The key is stored with a
unique constraint; a replay returns the original order.

### 6.3 Virtual Checkout — where it differs

```
├─ NO stock reservation (a digital copy doesn't deplete)
├─ Instead: lookup virtual editions — every book must have an ACTIVE one, else 409
├─ Priced at the VIRTUAL edition's price (can differ from the physical list price)
├─ NO shipping address required
└─ On payment.captured: PENDING_PAYMENT → PAID → CONFIRMED → DELIVERED
                                                              ↑
                              physical orders stop at CONFIRMED (shipping not built);
                              virtual has nothing to ship, so it completes immediately
```

**Design decision:** an order is **entirely physical or entirely virtual** — no mixing. The
alternative (per-item delivery type) means one order with two fulfilment paths, partial shipping,
split refunds. Per-order was chosen deliberately for simplicity.

### 6.4 Order Cancellation

```
POST /api/v1/orders/{id}/cancel
   ├─ already CANCELLED? → 409
   ├─ SHIPPED or DELIVERED? → 409
   ├─ placed more than 48h ago? → 409
   ├─ status → CANCELLED, write history row
   └─ outbox: order.cancelled → payment-service refunds → refund.completed
                                          → user-service credits wallet back
```

---

## 7. Security Model

**Five layers, each doing one job.** Being able to name all five is a strong answer.

### Layer 1 — Gateway JWT validation (deny by default)

`JwtAuthenticationGlobalFilter` (order `-1`): if the path isn't in `app.security.public-routes`,
a valid `Bearer` token is required or the request gets `401` — it never reaches a service.

On success the gateway **injects trusted headers**: `X-User-Id`, `X-User-Email`, `X-User-Roles`.

### Layer 2 — Gateway secret (services reject direct traffic)

Every service runs `GatewaySecretFilter` (order `-30`, first) checking `X-Gateway-Secret` against
a shared value, using **`MessageDigest.isEqual`** for **constant-time comparison** — a normal
`String.equals` short-circuits on the first differing byte, leaking information through timing.

**Why this matters:** without it, anyone who can reach `localhost:8083` could forge an
`X-User-Id` header and impersonate any user. This filter is what makes header-trust safe.

### Layer 3 — Per-service deny-by-default

`UserContextFilter` (order `-10`): every route not in that service's own `public-routes` list
requires `X-User-Id`, else `401`. Each service maintains its own allowlist rather than trusting
the gateway to have gotten it right.

### Layer 4 — Role-based authorization

Any path under `/api/v1/admin/**` additionally requires `ADMIN` in `X-User-Roles`, else `403`.
Enforced in catalog-service and ai-service.

The role chain end to end: role in DB → `roles` claim in the JWT → `X-User-Roles` header from the
gateway → per-service check.

### Layer 5 — Credential handling

- **Passwords:** BCrypt (adaptive, salted, deliberately slow — resists brute force)
- **Refresh tokens:** SHA-256 hashed before storage; the raw value exists only on the client.
  (Fast hash is correct here — a UUID already has 128 bits of entropy, unlike a human password.)
- **Account lockout:** 5 consecutive failures → `LOCKED`
- **Access tokens:** short-lived (15 min), so a leaked token has a small blast radius

### Refresh token rotation with reuse detection

This is a strong detail to bring up unprompted:

```
POST /auth/refresh with token T
   ├─ T revoked already? → SOMEONE STOLE IT
   │     └─ revoke EVERY active token for that user → kill the whole session
   ├─ T expired? → 401
   └─ valid: revoke T, issue T' + new access token
```

**The reasoning:** each refresh token is single-use. If a revoked one is presented again, either
an attacker is using a stolen token or the legitimate user is replaying — both mean the session is
compromised, so *all* tokens are nuked. This turns token theft into a detectable event.

---

## 8. Infrastructure & Local Dev

### Docker Compose (`infra/docker-compose.yml`)

| Container | Purpose |
|---|---|
| `redis:7-alpine` | Cart storage + rate-limit buckets |
| `apache/kafka:3.8.0` | Event bus, **KRaft mode** (no ZooKeeper) |
| `otel-collector` | Receives OTLP, forwards to New Relic |

**PostgreSQL is not containerized** — it runs locally (Homebrew). Setup is captured in
`infra/db/setup.sql`: create the 6 schemas + `pgvector` extension. Tables themselves are created
by Hibernate's `ddl-auto: update` on service startup.

### Running the backend

```bash
cd infra && docker compose up -d     # redis, kafka, otel-collector
./run-all.sh                          # all 9 services in parallel
./stop-all.sh                         # stop cleanly
```

`run-all.sh` backgrounds each `mvn spring-boot:run`, logging to `logs/<service>.log` and tracking
PIDs. `stop-all.sh` kills each **process group** — killing just the `mvn` wrapper would orphan the
actual JVM holding the port.

### Seed data

`seedData/run-seed.sh` loads 4 users (1 admin, 3 customers — password `Password123!`), 16 books,
10 authors, 7 categories, 5 publishers, inventory, and 7 virtual editions. Deterministic UUIDs +
`ON CONFLICT DO NOTHING` make it safe to re-run.

### Testing

`e2e/` — Playwright API tests (`npm run test:smoke`, 14 tests). Not browser automation; it uses
Playwright's `request` fixture. Notably, it verifies *wiring*, not just liveness: a real
register→login→token flow, then that token used against each backend service through the gateway,
plus 403 checks on admin routes and rejection of unauthenticated/direct access.

---

## 9. Design Decisions and Trade-offs

Have a crisp answer for each of these — they're the "why did you do it that way" questions.

| Decision | Why | What was given up |
|---|---|---|
| Schema-per-service, not DB-per-service | One instance to run locally; logical isolation preserved | Weaker physical isolation; a shared DB is a shared failure domain |
| Choreography, not orchestration | No central coupling; new consumers need no producer changes | Harder to trace the overall flow; no single place showing the whole saga |
| Outbox pattern | Atomic business-write + event-publish | ~2s publish latency; extra table and a polling job |
| Cart in Redis | High-write, ephemeral, naturally TTL'd | Weaker durability than Postgres (acceptable — it's not a business record) |
| Per-order delivery type | One fulfilment path per order; far simpler | Users can't buy physical + virtual in one transaction |
| Snapshot columns on order items | Orders stay historically accurate | Data duplication; renames don't propagate (correct here) |
| Header-based identity downstream | Services don't each re-validate JWTs | Depends entirely on the gateway-secret layer holding |
| Token bucket over fixed window | No boundary-burst exploit | Slightly more complex; needs distributed state |
| Dummy payment provider | Project scope is the architecture, not a PCI integration | Not production-usable — isolated to one class though |
| OTel Java agent over SDK | Zero code changes, zero dependencies | Less fine-grained control than manual instrumentation |

---

## 10. Known Gaps (Be Honest About These)

Volunteering these makes you look **more** competent, not less. Interviewers probe for
self-awareness, and every one of these has a reasoned explanation.

1. **`ChatService` userId injection** — MCP user-scoped tools take `userId` as a *tool parameter*,
   meaning the model supplies it. It should come from the authenticated request at the transport
   layer. **This is a real security gap, documented in the code.** The fix needs either a
   per-request MCP client carrying userId as a header, or Spring AI's `ToolContext` if it extends
   to remote MCP tools.

2. **Spring AI MCP client transport** — `application.yml` uses
   `spring.ai.mcp.client.streamable-http.url`, which doesn't exist in Spring AI 1.0.0 (only `sse`
   and `stdio` are auto-configured). ai-service likely isn't reaching mcp-server at all.

3. **Wallet-funded checkout not implemented** — `walletAmountUsed` is always zero. The
   *consumer side* is fully wired and idempotent; only the checkout side is missing.

4. **Physical orders stop at CONFIRMED** — `SHIPPED`/`DELIVERED` need a shipping integration
   that doesn't exist yet.

5. **notification-service bypasses the gateway** — WebSocket proxying through Spring Cloud
   Gateway was deferred; it validates JWTs itself instead.

6. **No automated unit/integration tests** — only the Playwright smoke suite. Real gap.

7. **`ddl-auto: update` instead of Flyway/Liquibase** — fine for development, **not acceptable
   for production**: no version history, no rollback, and it never drops or alters columns safely.

### Bugs found and fixed during end-to-end testing (good stories to tell)

These demonstrate debugging ability — worth having ready:

- **Cart never cleared after checkout.** `CartRepository.clear()` existed but nothing called it.
  Purchased items lingered, risking accidental re-purchase. Fixed by wiring it into
  `OrderService.checkout()`.
- **Virtual checkout silently broken.** The frontend's address schema required all fields
  unconditionally, but those fields aren't rendered for virtual delivery — so validation failed
  invisibly and the button did nothing. Fixed by making the schema conditional.
- **Azure embedding 400s.** `text-embedding-ada-002` rejects the `dimensions` parameter outright,
  even set to its own native 1536. Only `text-embedding-3-*` models support it.
- **CORS gap.** The gateway allowed only `:5173`; the admin frontend on `:5174` was blocked.
- **`-javaagent` path with a space.** The repo path contains a space, and Spring Boot's
  `jvmArguments` re-splits on whitespace — every JVM crashed with a truncated agent path. Fixed
  with a symlink to a space-free path.

---

## 11. Interview Questions

### Architecture & Microservices

**Q: Walk me through your architecture.**
Nine Spring Boot services behind a Spring Cloud Gateway. Each owns a PostgreSQL schema and never
reads another's tables. Synchronous cross-service calls go over HTTP for things needed immediately
(stock reservation at checkout); everything else is asynchronous over Kafka. Redis handles cart
storage and rate limiting. There's an AI layer doing RAG over pgvector.

**Q: How did you decide service boundaries?**
By business capability, not technical layer. Each service is a vertical slice owning its data,
logic, and API. The test: could this be owned by a separate team and deployed independently?

**Q: What are the downsides of microservices here?**
Distributed transactions become choreography with eventual consistency. Debugging spans multiple
logs — which is exactly why correlation IDs exist. Network calls fail in ways in-process calls
don't. For a project this size, a modular monolith would honestly have been simpler; microservices
were chosen to exercise distributed patterns.

**Q: How do services communicate?**
Two ways, deliberately chosen per case. **Synchronous HTTP** when the caller needs the answer to
proceed — checkout must know whether stock reservation succeeded. **Asynchronous Kafka** when it
doesn't — the user shouldn't wait for payment, wallet updates, and notifications.

**Q: How do you handle a service being down?**
Kafka-based flows are naturally resilient — events queue up and process when the consumer returns.
Synchronous calls are the fragile ones: if catalog-service is down, checkout fails. Production
would need circuit breakers (Resilience4j) and retries with backoff. **That's a real gap here.**

---

### Kafka & Events

**Q: What is Kafka and why did you use it?**
A distributed append-only log. Producers append to topics; consumers read at their own pace via
offsets. Unlike a queue, messages are retained, so multiple independent consumer groups can read
the same stream and replay history. Used here to decouple checkout from payment, wallet, and
notifications — checkout returns immediately, and adding a new consumer needs no producer change.

**Q: Kafka vs RabbitMQ?**
RabbitMQ is a message broker — routes a message to a consumer, then it's gone. Kafka is a durable
log — messages persist, multiple groups read independently, and you can replay. Kafka fits when
several services care about the same event (`payment.captured` has three consumers) and when
replay matters. RabbitMQ fits complex routing and per-message acknowledgement.

**Q: What's a consumer group?**
Consumers sharing a `groupId` split a topic's partitions between them — that's how you scale
consumption horizontally. Different groups each get their **own** copy of the stream. Here
commerce, user, and notification each use their own `groupId`, so all three receive every
`payment.captured`.

**Q: How do you guarantee ordering?**
Kafka guarantees ordering **within a partition**, not across a topic. To order events for one
entity, key by that entity's ID so they land in the same partition.

**Q: What happens if a consumer fails mid-processing?**
The offset isn't committed, so the message is redelivered. That's why consumers are idempotent.
Spring Kafka retries with backoff and eventually gives up — production would route those to a
**dead-letter topic**, which this project doesn't have yet.

---

### The Outbox Pattern *(expect deep probing here)*

**Q: Why not just publish to Kafka after saving?**
The dual-write problem. Two systems, no shared transaction. Save-then-publish loses events when
the publish fails. Publish-then-save charges customers for orders that don't exist. Neither is
acceptable.

**Q: Explain your outbox implementation.**
The event is written as a row to the same database, in the same transaction as the business data —
so they commit or roll back atomically. A `@Scheduled` relay polls every 2 seconds for unpublished
rows, sends them to Kafka, and marks them published. If it crashes mid-run, unmarked rows are
retried on the next tick.

**Q: Doesn't that give you duplicates?**
Yes — it's at-least-once. A crash after `send()` but before `markPublished()` re-sends. That's
handled by making every consumer idempotent: payment-service uses a unique idempotency key,
commerce-service guards on current status, user-service keys the wallet ledger. **At-least-once
delivery plus idempotent consumers gives effectively-once processing.**

**Q: What's the latency cost?**
Up to the poll interval — 2 seconds here. Acceptable because the user already got their `201`;
payment settling a moment later is invisible. Lower latency would mean a shorter interval or
switching to CDC (Debezium reading the WAL).

**Q: How would you scale the relay with multiple instances?**
Currently every instance would poll the same rows. Fix with `SELECT … FOR UPDATE SKIP LOCKED`,
or leader election, or CDC instead of polling.

---

### Database

**Q: Why schema-per-service and not database-per-service?**
Database-per-service is the textbook answer and gives stronger isolation, but means six Postgres
instances locally. Schema-per-service preserves the property that actually matters — no service
touches another's tables — at a fraction of the operational cost, and migrating later is
straightforward because no query crosses a schema.

**Q: How do you handle relationships across services?**
Plain UUID columns, never JPA relationships. `payments.payments.user_id` is a `UUID`, not a
`@ManyToOne User`. There's no referential integrity across schemas — that's the accepted cost of
service autonomy, and it's why events matter for keeping things consistent.

**Q: Why snapshot title and price on order items?**
Orders are historical records. If a book is repriced or renamed, the order must still show what
the customer actually bought. Joining live to `books` would silently rewrite history.

**Q: Why is the wallet an append-only ledger?**
Auditability. Every balance change is a row with `balance_after`, so you can reconstruct the
balance at any point in time. A reversal is a new row, never an edit. `wallet_accounts.balance` is
a derived cache; the ledger is the source of truth.

**Q: How do you prevent overselling?**
`inventory` has `qty_on_hand` and `qty_reserved`; available is the difference. Reservation happens
**at checkout**, inside a transaction, *before* payment completes. **Weakness to acknowledge:**
it relies on transaction isolation rather than explicit pessimistic locking. Under real concurrency
I'd add `SELECT … FOR UPDATE` or an optimistic-locking `@Version` column.

**Q: `ddl-auto: update` in production?**
No — that's a development shortcut. Production needs Flyway or Liquibase: versioned, reviewable,
rollback-capable migrations. `ddl-auto` has no history and can't safely alter or drop columns.

---

### Security

**Q: Walk me through authentication.**
Login verifies a BCrypt hash and returns a short-lived (15 min) HS256 JWT plus a 30-day refresh
token. The gateway validates the JWT on every request and injects `X-User-Id`/`X-User-Roles`.
Services trust those headers **only because** the gateway-secret filter proves the request came
through the gateway.

**Q: Isn't trusting headers dangerous?**
It would be, without the gateway secret. Every service checks `X-Gateway-Secret` with a
constant-time comparison before anything else runs (filter order `-30`). Without it, anyone
reaching a service port directly could forge `X-User-Id`. That filter is what makes header-trust safe.

**Q: Why constant-time comparison?**
`String.equals` short-circuits at the first differing byte, so response timing leaks how many
leading bytes were correct — enough to recover a secret byte by byte. `MessageDigest.isEqual`
always compares the full length.

**Q: Explain refresh token rotation and reuse detection.**
Each refresh token is single-use: presenting it revokes it and issues a new pair. If an
*already-revoked* token is presented, that means either an attacker has a stolen token or the
user is replaying one — either way the session is compromised, so every active token for that
user is revoked. It turns token theft into a detectable event.

**Q: Why hash refresh tokens with SHA-256 but passwords with BCrypt?**
Different threat models. Passwords are low-entropy and human-chosen, so they need a slow, salted,
adaptive hash to resist brute force. A refresh token is already a 128-bit random UUID — brute
force is infeasible regardless — so a fast deterministic hash is both sufficient and *necessary*
for indexed exact-match lookup.

**Q: Why the same error for unknown email and wrong password?**
To prevent account enumeration. Different responses would let an attacker discover which emails
are registered.

**Q: Where's the JWT stored client-side?**
Access token in memory only (Redux state, never persisted), refresh token in localStorage.
**Acknowledged trade-off:** localStorage is readable by XSS. The stronger design is an httpOnly,
Secure, SameSite cookie for the refresh token.

---

### Redis, Caching & Rate Limiting

**Q: Why is the cart in Redis and not Postgres?**
Carts are high-write, low-value, and naturally ephemeral. Redis gives sub-millisecond reads and a
TTL that expires abandoned carts with no cleanup job. A cart isn't a business record — an order
is, and orders go to Postgres. If Redis loses a cart, that's annoying, not a data-integrity failure.

**Q: Explain your rate limiting.**
Token bucket via Bucket4j, with state in Redis so limits hold across gateway instances. Keyed by
route + user ID (falling back to IP when anonymous), with per-route rules — ai-service gets 5/min
because LLM calls are expensive; the default is 20/min.

**Q: Why token bucket instead of a fixed window?**
Fixed windows have a boundary-burst flaw — 20 requests at 11:59:59 plus 20 at 12:00:00 is 40 in
one second, technically legal. Token buckets refill gradually, so that burst can't happen.

**Q: Why does bucket state need to be in Redis?**
So the limit is global. With in-memory counters, three gateway instances would each allow 20/min —
60 total. Redis makes the state shared.

---

### AI / RAG

**Q: What is RAG and why use it?**
Retrieval-Augmented Generation: retrieve relevant documents from your own data and feed them to
the LLM as context, rather than relying on training data. It lets the model answer about *your*
catalogue, grounds answers in real data (reducing hallucination), and updating knowledge is a
database write rather than retraining.

**Q: How do embeddings work?**
A model maps text to a vector — 1536 numbers for ada-002 — positioning it in "meaning space".
Similar meanings land near each other, so cosine similarity finds semantically related text even
with zero shared words. Searching "dystopian totalitarian society" returns *1984* first.

**Q: Why pgvector instead of a dedicated vector DB?**
No extra infrastructure. Vectors live in the Postgres already being run, so it's one thing to
back up and operate. A dedicated store like Pinecone would matter at much larger scale or with
demanding filtering needs.

**Q: How do you keep embeddings fresh?**
Event-driven. Saving a book writes a `book.upserted` outbox row; ai-service consumes it and
re-embeds that one book. There's also an admin-triggered full backfill for bootstrapping,
recovering from missed events, or switching embedding models.

**Q: Why both incremental and batch?**
Events only cover changes going forward — useless for pre-existing books or a wiped vector store.
The original design re-embedded everything on every startup, which was wasteful. Events plus a
manual backfill covers both cases properly.

**Q: RAG vs tools — when do you use which?**
RAG for fuzzy semantic queries ("a bleak book about surveillance"). Tools for exact, live,
structured lookups ("current stock of book X"). You wouldn't check inventory with RAG — it needs
live exactness — and you wouldn't answer a vibes-based query with SQL `LIKE`. Complementary, not
competing.

**Q: What are your MCP tools and why read-only?**
Search, book details, inventory, order history, cart, profile, wallet — all reads. There's
deliberately no `placeOrder` or `addToCart` tool. Mutations happen only through the authenticated
REST API driven by a real user click, never by a model's decision.

---

### Spring & Java

**Q: Why records for DTOs but classes for entities?**
Records are immutable with a final all-args constructor — perfect for DTOs. JPA needs a no-arg
constructor and mutable fields for proxying and dirty checking, so entities must be classes.

**Q: Why `EnumType.STRING` over `ORDINAL`?**
Ordinal stores the position. Insert a new constant in the middle and every existing row silently
changes meaning. Strings are self-describing and safe to reorder.

**Q: What's `@Transactional` actually doing?**
Spring wraps the bean in a proxy that opens a transaction before the method and commits or rolls
back after. **Gotcha:** it only applies to calls that go *through* the proxy — a `this.method()`
self-invocation bypasses it entirely. It also only rolls back on unchecked exceptions by default.

**Q: How does the OTel Java agent work without code changes?**
It attaches via `-javaagent` and runs `premain` *before* the main class loads, then rewrites
bytecode at class-load time to inject instrumentation into known libraries (Spring MVC, JDBC,
Kafka). That's why its config must come from real environment variables — Spring's `.env` is
loaded far too late.

**Q: Why is the gateway reactive but everything else is not?**
A gateway is almost pure I/O wait — WebFlux handles many concurrent connections on few threads.
The other services do real DB work where blocking MVC is simpler to write and debug, and reactive
would add complexity for little gain.

---

### System Design / Scaling

**Q: How would you scale this?**
Stateless services scale horizontally behind a load balancer — that's why rate-limit state is in
Redis rather than memory. Kafka scales by adding partitions and consumer instances. Postgres
scales with read replicas for catalog browsing (the heaviest read path). Caching hot book detail
pages in Redis would be the highest-value next step.

**Q: What's your biggest bottleneck?**
Postgres, since all six schemas share one instance — a shared failure domain and a shared
connection pool ceiling. First fix: split the highest-traffic schema (catalog) onto its own
instance. The code needs no changes because nothing queries across schemas.

**Q: How do you debug a failing request across services?**
Correlation IDs. The gateway generates `X-Correlation-Id`, every service puts it in SLF4J MDC so
it tags every log line, and error responses return it as `traceId`. A user reports an error with
that ID and you can grep all nine services for exactly that request. OpenTelemetry gives the same
picture as a visual distributed trace.

**Q: What would you do differently?**
Three things. **Flyway from day one** — `ddl-auto: update` was a shortcut with no migration
history. **Circuit breakers on synchronous calls** — right now catalog-service being down fails
checkout with no graceful degradation. **Automated tests alongside the code**, not a smoke suite
bolted on at the end.

---

## 12. Quick Reference Cheat Sheet

**Ports:** gateway 8080 · auth 8081 · user 8082 · catalog 8083 · commerce 8084 · payment 8085 ·
notification 8086 · mcp 8087 · ai 8088 · Postgres 5432 · Redis 6379 · Kafka 9092 · OTLP 4317

**Schemas:** `auth` · `users` · `catalog` · `commerce` · `payments` · `ai`

**Kafka topics:** `order.created` · `order.cancelled` · `payment.captured` · `payment.failed` ·
`refund.completed` · `book.upserted` · `embedding.backfill.requested`

**Order status:** `PENDING_PAYMENT → PAID → CONFIRMED → SHIPPED → DELIVERED`, plus
`PAYMENT_FAILED` and `CANCELLED` (only within 48h and pre-shipping)

**Payment status:** `INITIATED → AUTHORIZED → CAPTURED` | `FAILED` | `REFUNDED`

**Filter order (downstream services):** GatewaySecret `-30` → CorrelationId `-20` → UserContext `-10`

**Filter order (gateway):** GatewaySecret `-3` → CorrelationId `-2` → JWT `-1` → RateLimit `0`

**Headers the gateway injects:** `X-User-Id` · `X-User-Email` · `X-User-Roles` ·
`X-Gateway-Secret` · `X-Correlation-Id`

**Business rules:** 9% tax · max 10 per title in cart · 48h cancellation window · 5 failed logins
locks an account · 15-min access token · 30-day refresh token · 30-day cart TTL · ₹100 signup
bonus · max 20 addresses per user

**Seed credentials:** `admin@readora.dev` (ADMIN), `alice@` / `bob@` / `carol@readora.dev`
(CUSTOMER) — all `Password123!`

---

## The Three Things to Lead With

If you only get to make three points about this project, make these:

1. **The transactional outbox pattern** — because it shows you understand that distributed systems
   can't rely on distributed transactions, and that at-least-once delivery plus idempotent
   consumers is the practical answer.

2. **The layered security model** — because "the gateway validates JWTs and injects headers, and
   the gateway-secret filter is what makes trusting those headers safe" demonstrates thinking in
   terms of threat models rather than checklists.

3. **The event-driven RAG pipeline** — because replacing "re-embed everything on startup" with
   "publish a domain event, embed one book" shows you can spot a design smell and fix it with a
   pattern already in the system.
