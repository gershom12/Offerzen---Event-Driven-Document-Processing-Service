# README.md

# Event Driven Document Processing Service

## Overview

This service processes document update events from Kafka and maintains the latest state of each document in PostgreSQL.

The solution is designed around the following principles:

* Event-driven architecture
* Idempotent event processing
* Ordered document updates
* Fault tolerance through retries and DLQ
* Optimistic locking for concurrency control
* Cache-assisted reads
* Structured logging and observability
* Clean architecture and separation of concerns

---

# Features

* Consume document events from Kafka
* Create and update document state
* Enforce event ordering using sequence numbers
* Prevent duplicate event processing
* Persist document state in PostgreSQL
* Expose REST API for document retrieval
* Retry transient failures
* Route permanently failing messages to DLQ
* Correlation ID based logging
* In-memory caching for hot documents

---

# Architecture

```text
                    +----------------+
                    |   Kafka Topic  |
                    | document-events|
                    +--------+-------+
                             |
                             v
                 +-----------+------------+
                 | DocumentEventConsumer  |
                 +-----------+------------+
                             |
                             v
                 +-----------+------------+
                 | DocumentEventProcessor |
                 +-----------+------------+
                             |
          +------------------+------------------+
          |                                     |
          v                                     v

+---------------------+             +--------------------+
| Sequence Validation |             | Idempotency Check  |
+---------------------+             +--------------------+
          |                                     |
          +------------------+------------------+
                             |
                             v

                 +-----------+------------+
                 | DocumentStateService   |
                 +-----------+------------+
                             |
                +------------+-----------+
                |                        |
                v                        v

         +------------+         +----------------+
         |   Cache    |         | PostgreSQL DB  |
         +------------+         +----------------+

```

---

# Project Structure

```text
src/main/java/com/document

api
└── controller

application
├── processor
└── service

domain
├── exception
├── model
└── policy

infrastructure
├── cache
├── kafka
├── observability
└── persistence

config
```

---

# Technology Stack

| Component  | Technology        |
| ---------- | ----------------- |
| Language   | Java 21           |
| Framework  | Spring Boot       |
| Database   | PostgreSQL        |
| Messaging  | Apache Kafka      |
| ORM        | Spring Data JPA   |
| Cache      | ConcurrentHashMap |
| Logging    | SLF4J             |
| Build Tool | Maven             |

---

# Database Schema

## documents

| Column        | Description             |
| ------------- | ----------------------- |
| document_id   | Primary key             |
| content       | Latest document state   |
| last_sequence | Last processed sequence |
| created_at    | Creation timestamp      |
| updated_at    | Last update timestamp   |
| version       | Optimistic lock version |

---

## processed_events

| Column       | Description          |
| ------------ | -------------------- |
| event_id     | Primary key          |
| document_id  | Associated document  |
| processed_at | Processing timestamp |

---

# Event Model

Example Event

```json
{
  "eventId": "evt-1001",
  "documentId": "doc-1",
  "sequence": 1,
  "timestamp": "2026-01-01T10:00:00Z",
  "type": "UPDATE",
  "payload": {
    "title": "Legal Contract"
  }
}
```

---

# Event Processing Flow

1. Kafka receives event
2. Consumer deserializes payload
3. Processor validates idempotency
4. Processor validates sequence ordering
5. Document state loaded
6. Update applied
7. State persisted
8. Event marked as processed
9. Success logged

---

# Idempotency

Kafka guarantees at-least-once delivery.

The same event may be delivered multiple times.

To prevent duplicate updates:

```text
processed_events
```

stores every successfully processed event.

If the event already exists:

```text
Processing is skipped
```

---

# Ordering Guarantee

Every document contains:

```text
lastSequence
```

Rule:

```text
incomingSequence == lastSequence + 1
```

Example:

Current:

```text
5
```

Valid:

```text
6
```

Invalid:

```text
8
```

This guarantees ordered updates.

---

# Caching Strategy

Read Flow

```text
Cache
 ↓
Database
 ↓
Cache Refresh
```

Benefits:

* Reduced database load
* Faster reads
* Lower latency

---

# Concurrency Control

The service uses optimistic locking.

```java
@Version
private Long version;
```

Benefits:

* Prevents lost updates
* Detects concurrent modifications
* Supports horizontal scaling

---

# Kafka Topics

## Main Topic

```text
document-events
```

## Dead Letter Queue

```text
document-events-dlq
```

---

# Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/docdb
    username: postgres
    password: postgres

  jpa:
    hibernate:
      ddl-auto: update

  kafka:
    bootstrap-servers: localhost:9092

    consumer:
      group-id: doc-service
      auto-offset-reset: earliest

logging:
  pattern:
    console: "%d [%X{correlationId}] %-5level %logger - %msg%n"
```

---

# Running Locally

## Start PostgreSQL

```sql
CREATE DATABASE docdb;
```

---

## Start Kafka

```bash
docker-compose up -d
```

---

## Build

```bash
mvn clean install
```

---

## Run

```bash
mvn spring-boot:run
```

---

# API

## Get Document

```http
GET /documents/{id}
```

Example:

```http
GET /documents/doc-1
```

Response

```json
{
  "documentId": "doc-1",
  "content": "{}",
  "lastSequence": 5
}
```

---

# Logging

Example:

```text
2026-01-01 10:00:00
[9f7f9b88]
INFO
DocumentEventProcessor
PROCESS_SUCCESS eventId=evt-1001
```

---