# SOLUTION.md

# Solution Design and Architectural Decisions

## Problem Statement

Build a document processing platform capable of:

* Consuming document events
* Maintaining document state
* Handling duplicate deliveries
* Preserving update order
* Scaling horizontally
* Recovering from failures

---

# Design Goals

The primary goals were:

1. Reliability
2. Consistency
3. Scalability
4. Fault Tolerance
5. Maintainability
6. Observability

---

# High-Level Design

```text
Kafka
 |
 v
Consumer
 |
 v
Processor
 |
 +----> Idempotency
 |
 +----> Ordering
 |
 +----> State Management
 |
 v
Database

Failures
 |
 v
Retry
 |
 v
DLQ
```

---

# Why Event Driven Architecture

A synchronous request-response design tightly couples systems.

Problems:

* Higher latency
* Lower scalability
* Failure propagation

Kafka provides:

* Decoupling
* Durability
* Replayability
* High throughput

---

# Why Kafka

Kafka guarantees ordering within a partition.

Events are partitioned using:

```text
documentId
```

This ensures all updates for the same document are processed sequentially.

Benefits:

* Ordered processing
* Horizontal scalability
* Fault tolerance

---

# Idempotency Design

## Problem

Kafka uses at-least-once delivery.

An event can be processed more than once.

Example:

```text
Process Event
Commit Fails
Consumer Restarts
Kafka Redelivers
```

Without protection:

```text
Document updated twice
```

---

## Solution

Persist every processed event.

Table:

```text
processed_events
```

Key:

```text
eventId
```

Flow:

```text
Event Received
       |
Check processed_events
       |
Already Exists?
       |
     Yes
       |
     Skip
```

Benefits:

* Safe retries
* Duplicate prevention
* Exactly-once business outcome

---

# Ordering Strategy

## Problem

Events may arrive out of order.

Example:

```text
Seq 1
Seq 3
Seq 2
```

Applying sequence 3 before sequence 2 corrupts state.

---

## Solution

Store:

```text
lastSequence
```

Rule:

```text
incoming == lastSequence + 1
```

Only sequential updates are accepted.

Benefits:

* Deterministic state
* Data consistency
* Simpler processing model

---

# State Management

The DocumentStateService centralizes state operations.

Responsibilities:

* Load state
* Create state
* Update state
* Synchronize cache
* Persist state

Benefits:

* Single responsibility
* Easier testing
* Cleaner processor

---

# Concurrency Strategy

## Problem

Multiple instances may process updates concurrently.

Potential issue:

```text
Lost Updates
```

---

## Solution

Optimistic Locking

```java
@Version
private Long version;
```

Workflow:

```text
Read Version 5
Update
Commit

Another Update
Version Mismatch
Exception
Rollback
```

Benefits:

* No distributed locks
* Better throughput
* Conflict detection

---

# Retry Strategy

Retryable failures:

* Database timeout
* Temporary network failure
* Kafka connectivity issue

Strategy:

```text
3 retries
1 second backoff
```

Benefits:

* Self-healing system
* Reduced operational intervention

---

# Dead Letter Queue

Permanent failures should not block processing.

Flow:

```text
Process
 ↓
Retry
 ↓
Retry
 ↓
Retry
 ↓
DLQ
```

DLQ Topic:

```text
document-events-dlq
```

Benefits:

* Prevents poison message loops
* Preserves failed events
* Enables investigation

---

# Caching Design

Current implementation:

```text
ConcurrentHashMap
```

Purpose:

* Faster reads
* Reduced DB calls

Future enhancement:

```text
Redis Cluster
```

Benefits:

* Distributed cache
* Cross-instance sharing
* Higher scalability

---

# Database Design

Why PostgreSQL?

Benefits:

* ACID transactions
* Strong consistency
* Mature ecosystem
* Excellent Spring support

---

# Observability

Current:

* Structured logging
* Correlation IDs

Future:

* Prometheus
* Grafana
* OpenTelemetry
* Distributed tracing

---


# Tradeoffs

## Why Not Distributed Locks

Pros:

* Strong synchronization

Cons:

* Complexity
* Performance overhead
* Additional infrastructure

Decision:

Use optimistic locking.

---

## Why Not Event Sourcing

Pros:

* Complete audit trail
* Replay capability

Cons:

* Higher complexity
* Increased storage
* More operational overhead

Decision:

Persist current state only.

---

# Scalability Analysis

Horizontal scaling achieved through:

```text
Kafka Consumer Groups
```

Example:

```text
Instance A -> Partition 1
Instance B -> Partition 2
Instance C -> Partition 3
```

Benefits:

* Parallel processing
* High throughput
* Fault tolerance

---

# Reliability Guarantees

The system provides:

✓ At-Least-Once Consumption

✓ Idempotent Processing

✓ Ordered Updates Per Document

✓ Optimistic Concurrency Control

✓ Retry Handling

✓ Dead Letter Queue Recovery

✓ Durable Persistence

---

# Conclusion

This solution implements a reliable and scalable event-driven document processing platform using Spring Boot, Kafka, and PostgreSQL.

The design prioritizes:

* Correctness
* Reliability
* Scalability
* Maintainability
* Fault Tolerance

while remaining simple enough to evolve into a large-scale enterprise event-processing architecture.
