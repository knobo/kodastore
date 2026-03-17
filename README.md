# KodaStore

**An open-source Event Sourcing database built on PostgreSQL.**

KodaStore gives you the power of a dedicated event store with the operational simplicity of Postgres. Store events, rebuild state, and stream changes — using the database you already know and trust.

```
POST /api/streams/Order-123/events
{
  "expectedVersion": -1,
  "events": [
    { "eventType": "OrderCreated", "payload": { "customerId": "c-42", "amount": 149.99 } }
  ]
}
→ 201 Created
```

## Why KodaStore?

Event Sourcing is a powerful pattern, but the tooling has historically been either proprietary, complex to operate, or requires infrastructure most teams don't want to maintain. KodaStore takes a different approach:

- **PostgreSQL is the engine.** No custom storage layer. Your events live in Postgres, with all the backup, replication, and tooling you already have.
- **Optimistic concurrency built in.** Hash-partitioned tables with unique constraints give you safe concurrent writes without application-level locking.
- **Modern Postgres features.** Designed for Postgres 17/18 — hash partitioning, JSONB, `JSON_TABLE` for projections, failover slots for reliable subscriptions.
- **Simple to run.** One `docker compose up` and you're writing events.

## Quick Start

### 1. Start the database

```bash
docker compose up -d
```

### 2. Start KodaStore

```bash
./gradlew :infrastructure:bootRun
```

KodaStore is now running at `http://localhost:8080` with a built-in UI for stream inspection.

### 3. Write your first events

```bash
curl -X POST http://localhost:8080/api/streams/Order-001/events \
  -H "Content-Type: application/json" \
  -d '{
    "expectedVersion": -1,
    "events": [
      {"eventType": "OrderCreated", "payload": {"customerId": "c-1", "amount": 99.99}},
      {"eventType": "ItemAdded", "payload": {"itemId": "sku-42", "quantity": 2}}
    ]
  }'
```

### 4. Read them back

```bash
# Read a single stream
curl http://localhost:8080/api/streams/Order-001

# Read all events globally
curl http://localhost:8080/api/streams?fromOffset=0&limit=100

# Read by category
curl http://localhost:8080/api/categories/Order
```

## REST API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/streams/{id}/events` | Append events (201 / 409 on conflict) |
| `GET` | `/api/streams/{id}` | Read a stream |
| `GET` | `/api/streams?fromOffset=&limit=` | Read global event feed |
| `GET` | `/api/categories/{name}` | Read events by category |
| `GET` | `/actuator/health` | Health check |

### Optimistic Concurrency Control

When appending events, use `expectedVersion` to prevent conflicts:

| Value | Meaning |
|-------|---------|
| `-1` | Stream must not exist (creating new) |
| `N` | Stream must be at exactly version N |
| `null` | No check — always append |

A version mismatch returns `409 Conflict`.

## Architecture

KodaStore follows hexagonal architecture with three Gradle modules:

```
domain/           Pure Kotlin — event model, stream abstractions, port interfaces
application/      Use cases — append, read stream, read all, read category
infrastructure/   Spring Boot, PostgreSQL adapter, REST API, Flyway migrations
```

The domain module has **zero framework dependencies**. All infrastructure concerns (database, HTTP, serialization) are isolated in the outer layer.

### Database Design

Events are stored in a hash-partitioned table for write scalability:

```sql
CREATE TABLE events (
    global_offset   BIGSERIAL,
    stream_id       VARCHAR(255),
    stream_version  INT,
    event_type      VARCHAR(100),
    payload         JSONB,
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE (stream_id, stream_version)
) PARTITION BY HASH (stream_id);
```

8 partitions distribute writes across the table, reducing lock contention. The `UNIQUE` constraint on `(stream_id, stream_version)` is the backbone of optimistic concurrency — the database itself guarantees consistency.

### High Throughput Configuration

- **Virtual Threads** (JVM 21) — thousands of concurrent connections without thread pool exhaustion
- **HikariCP** connection pooling tuned for virtual threads
- **PostgreSQL group commit** (`commit_delay`) batches WAL flushes under load
- **INSERT ... RETURNING** eliminates extra round-trips

## Example: Order Service

See [`examples/order-service/`](examples/order-service/) for a complete Spring Boot application that uses KodaStore as its event store. It demonstrates:

- Defining an aggregate with typed events and immutable state
- Rebuilding state from the event stream (`evolve` / `rebuild` pattern)
- Optimistic concurrency in practice
- A clean REST API backed entirely by events

```bash
# Start KodaStore first, then:
./gradlew :examples:order-service:bootRun

# Create an order
curl -X POST http://localhost:9090/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "customer-1"}'

# Add items, confirm, inspect — all backed by events in KodaStore
```

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Kotlin 2.1 |
| Runtime | JVM 21+ with Virtual Threads |
| Framework | Spring Boot 3.4 |
| Database | PostgreSQL 17+ |
| Migrations | Flyway |
| Build | Gradle (Kotlin DSL) |

## The AI-Assisted Open Source Experiment

This project was bootstrapped in a single session using [Claude Code](https://claude.ai/claude-code). From an empty directory with nothing but a spec document, an AI agent generated the entire codebase — architecture, database schema, REST API, integration tests, a web UI, and a working example application.

This is both exciting and concerning.

**The opportunity:** AI-assisted development makes it possible for small teams (or even individuals) to build things that previously required months of work. A working event store with partitioned tables, optimistic concurrency, a timeline UI, and a full example app — created in one conversation.

**The risk:** When it's this easy to create something from scratch, why would anyone contribute to someone else's project? The gravitational pull of "I'll just build my own" has never been stronger. Every developer with an AI assistant can spin up a custom solution that fits their exact needs in an afternoon.

**This is the trap we want to avoid.**

The history of open source shows that the best tools aren't the ones that fit one person's needs perfectly — they're the ones that are flexible enough to fit *most* people's needs *well enough*, with the rough edges smoothed by hundreds of contributors over time. PostgreSQL itself is the ultimate example: nobody's custom database will ever match what decades of community effort have produced.

**Our bet is on flexibility:**

- KodaStore is designed to be extended, not forked. The hexagonal architecture means you can swap out the REST API for gRPC, replace the storage adapter, or add your own projection engine — without touching the core.
- We'd rather have 10 well-designed extension points than a monolithic "perfect" solution.
- Every feature decision will ask: "Does this make KodaStore useful to more people, or just one person?"

If you're thinking about building your own event store — consider contributing to this one instead. Your use case probably isn't as unique as it feels, and together we can build something none of us could alone.

## Contributing

We welcome contributions of all kinds. Some areas where help is especially valuable:

- **gRPC API** — high-performance streaming interface alongside REST
- **Subscription engine** — real-time event streaming using PostgreSQL logical replication
- **Snapshot support** — automatic snapshotting for long-lived streams
- **Client libraries** — Kotlin, Java, TypeScript clients
- **Projections** — built-in read model support with transactional guarantees
- **Performance testing** — benchmarks, load tests, optimization
- **Documentation** — guides, tutorials, architectural decision records

Open an issue to discuss your idea before starting large changes.

## License

[Apache License 2.0](LICENSE)

---

*Built with curiosity, PostgreSQL, and a conversation with an AI.*
