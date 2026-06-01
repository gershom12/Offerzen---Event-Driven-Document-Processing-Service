# SOLUTION.md

# Document Processing Service – Solution Design

## 1. Introduction

This document explains the architectural decisions made for the Document Processing Service, the trade-offs considered, failure handling strategies, scalability approach, and how the service would be deployed and operated in a production environment.

The primary goal of the solution is to process document update events reliably while guaranteeing:

* Idempotent processing
* Ordered updates per document
* Durable persistence
* Fault tolerance
* Horizontal scalability

The implementation intentionally prioritizes correctness and maintainability over premature optimization.

---

# 2. Problem Understanding

The system receives document events from Kafka.

Example:

```json
{
  "eventId": "evt-1001",
  "documentId": "doc-1",
  "sequence": 15,
  "type": "UPDATE",
  "payload": {}
}
```

Each event represents a state change to a document.

Key requirements:

1. Process events exactly once from a business perspective.
2. Preserve ordering of updates for each document.
3. Maintain current document state.
4. Recover from failures.
5. Scale horizontally.

---

# 3. Architectural Approach

The solution follows an event-driven architecture.

```text
Kafka
  |
  v
Consumer
  |
  v
Processor
  |
  +----> Idempotency Validation
  |
  +----> Sequence Validation
  |
  +----> State Management
  |
  v
PostgreSQL
```

This approach provides loose coupling between producers and consumers while allowing the system to scale independently.

---

# 4. Correctness Guarantees

Correctness was the primary design concern.

The implementation addresses three major risks:

1. Duplicate events
2. Out-of-order events
3. Concurrent updates

---

# 5. Idempotency Strategy

## Problem

Kafka provides at-least-once delivery.

The same message may be delivered multiple times.

Example:

```text
Process Event
      |
DB Commit
      |
Consumer Crashes
      |
Kafka Redelivers
```

Without protection:

```text
Document updated twice
```

---

## Solution

Every successfully processed event is stored in:

```text
processed_events
```

Before processing:

```text
Check eventId
```

If found:

```text
Skip processing
```

Benefits:

* Safe retries
* Duplicate protection
* Business-level exactly-once behavior

---

# 6. Ordering Strategy

## Problem

Document updates must be applied in sequence.

Example:

```text
Sequence 1
Sequence 3
Sequence 2
```

Applying sequence 3 before sequence 2 may corrupt document state.

---

## Solution

Each document stores:

```text
lastSequence
```

Validation rule:

```text
incomingSequence == lastSequence + 1
```

Benefits:

* Deterministic state transitions
* Simple implementation
* Strong ordering guarantees

---

## Limitation

Out-of-order events are currently rejected.

Production systems would typically:

* Buffer events
* Store pending events
* Retry later

This was intentionally omitted to keep the exercise focused.

---

# 7. Concurrency Control

## Problem

Multiple consumers may attempt to update the same document.

Potential result:

```text
Lost Updates
```

---

## Solution

Optimistic locking:

```java
@Version
private Long version;
```

Benefits:

* No distributed lock manager
* Minimal performance overhead
* Automatic conflict detection

---

# 8. Failure Handling

Failures are classified into two categories.

---

## Retryable Failures

Examples:

* Database timeout
* Network interruption
* Kafka broker connectivity issues

Handling:

```text
Retry 3 times
Backoff 1 second
```

---

## Non-Retryable Failures

Examples:

* Invalid payload
* Invalid sequence
* Corrupt event

Handling:

```text
Dead Letter Queue
```

---

# 9. Dead Letter Queue Design

Messages that cannot be processed after retries are routed to:

```text
document-events-dlq
```

Benefits:

* Prevents poison messages blocking consumers
* Enables operational investigation
* Preserves failed events

---

# 10. Scalability Considerations

The service is designed for horizontal scaling.

Kafka consumer groups distribute partitions across service instances.

Example:

```text
Instance A -> Partition 1
Instance B -> Partition 2
Instance C -> Partition 3
```

Benefits:

* Parallel processing
* Increased throughput
* Fault tolerance

---

# 11. Persistence Design

PostgreSQL was selected because:

* Strong ACID guarantees
* Mature ecosystem
* Excellent Spring Boot integration
* Reliable transaction support

The database acts as the source of truth.

---

# 12. Caching Strategy

An in-memory cache is used to reduce database reads.

Current implementation:

```text
ConcurrentHashMap
```

Benefits:

* Faster document retrieval
* Reduced database load

---

## Production Upgrade

Replace with:

```text
Redis
```

Benefits:

* Shared cache across instances
* Distributed architecture support
* Better memory management

---

# 13. Observability

Current implementation includes:

* Structured logging
* Correlation IDs

Example:

```text
PROCESS_SUCCESS eventId=123
```

---

## Production Observability

I would additionally introduce:

### Metrics

Micrometer

Metrics:

* events_processed_total
* events_failed_total
* duplicate_events_total
* dlq_events_total

### Monitoring

Prometheus

### Dashboards

Grafana

### Distributed Tracing

OpenTelemetry

---

# 14. Security Considerations

Not implemented for this exercise.

Production implementation would include:

* TLS
* OAuth2
* JWT Authentication
* RBAC
* Secrets Manager
* Database encryption

These were intentionally omitted because they do not affect core event-processing behavior.

---

# 15. Deliberately Omitted Features

The goal was to deliver a focused, understandable solution rather than a fully featured enterprise platform.

The following were intentionally excluded:

### Event Sourcing

Reason:

Adds complexity beyond current requirements.

---

### Distributed Cache

Reason:

Single-node cache sufficient for prototype.

---

### Schema Registry

Reason:

JSON payloads simplify demonstration.

Production would use:

```text
Avro + Schema Registry
```

---

### Event Replay Tooling

Reason:

Not required for initial implementation.

---

### Out-of-Order Event Parking

Reason:

Would significantly increase implementation complexity.

---

### Multi-Region Deployment

Reason:

Outside scope of exercise.

---

# 16. Production Gaps

To move this service into production I would add:

1. Flyway migrations
2. Redis caching
3. OpenTelemetry
4. Prometheus
5. Grafana
6. Avro serialization
7. Schema Registry
8. Kubernetes deployment
9. Autoscaling
10. Disaster recovery procedures

---

# 17. Deployment Plan (AWS)

The chosen cloud platform is AWS.

---

## Infrastructure

```text
Internet
    |
ALB
    |
EKS Cluster
    |
Document Service Pods
    |
MSK (Kafka)
    |
RDS PostgreSQL
    |
ElastiCache Redis
```

---

## Components

### Amazon EKS

Hosts Spring Boot application containers.

Benefits:

* Managed Kubernetes
* Autoscaling
* Rolling deployments

---

### Amazon MSK

Managed Kafka service.

Benefits:

* High availability
* Multi-AZ deployment
* Automatic broker management

---

### Amazon RDS PostgreSQL

Stores:

* Documents
* Processed events

Benefits:

* Automated backups
* Multi-AZ failover
* Point-in-time recovery

---

### Amazon ElastiCache Redis

Distributed caching layer.

Benefits:

* Reduced database load
* Faster reads

---

### Amazon CloudWatch

Centralized logging and monitoring.

Collects:

* Application logs
* Metrics
* Alarms

---

# 18. CI/CD Strategy

GitHub Actions Pipeline

```text
Build
 |
Test
 |
Security Scan
 |
Docker Build
 |
Push ECR
 |
Deploy EKS
```

Deployment strategy:

```text
Rolling Update
```

Future enhancement:

```text
Blue-Green Deployment
```

---

# 19. Disaster Recovery

Production environment would include:

* Multi-AZ RDS
* Kafka replication factor 3
* Daily backups
* Point-in-time recovery
* Cross-region backup replication

Target Recovery Objectives:

```text
RPO: < 15 minutes
RTO: < 30 minutes
```

---

# 20. Conclusion

The design prioritizes correctness first and scalability second.

The combination of:

* Kafka
* Idempotency controls
* Sequence validation
* Optimistic locking
* Retry handling
* DLQ processing

provides a reliable foundation for document state management.

