# 001 — Why No Kafka

## Status
Accepted

## Context
AMRS processes market events through a multi-agent reasoning pipeline. The question arose whether an event streaming platform like Apache Kafka should be introduced as the backbone for inter-service communication.

## Decision
Kafka is not used in this system.

## Reasoning
Kafka solves one problem well: high-throughput, distributed event streaming across many producers and consumers. AMRS does not have that problem.

This system processes one event at a time, deeply. The pipeline is deliberately sequential in places (Agent 3 must wait for Agents 1 and 2) and the value is in reasoning depth, not throughput. Introducing Kafka would add broker management, topic configuration, consumer group complexity, and serialization overhead with zero engineering justification.

Adding infrastructure to appear sophisticated is an anti-pattern. Every architectural component must earn its place by solving a real problem.

## Consequences
- Pipeline latency is synchronous and predictable
- Operational complexity stays low
- If throughput requirements change in the future (e.g. processing thousands of events per second), Kafka can be introduced at that point with clear justification