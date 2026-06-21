# Virtual Thread Executor Never Closed

## Problem
`OrchestrationService` created a new virtual thread executor on every pipeline run to
fan out Agent 1 and Agent 2, but never closed it. Each request leaked an `ExecutorService`
instance.

## Root Cause
```java
Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
```

`Executors.newVirtualThreadPerTaskExecutor()` returns an `ExecutorService`, which
implements `AutoCloseable`. Assigning it to a plain `Executor` reference and never calling
`.close()` or `.shutdown()` means the underlying resources are never released. Under
sustained load this would accumulate unclosed executors over time.

## Fix
Wrapped the executor in a `try-with-resources` block scoped to exactly where it's needed:

```java
try (var virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    CompletableFuture<AgentOutput> agent1Future = CompletableFuture.supplyAsync(
            () -> agentService.runStructuralAgent(marketContext, request),
            virtualThreadExecutor);
    CompletableFuture<AgentOutput> agent2Future = CompletableFuture.supplyAsync(
            () -> agentService.runRiskAgent(marketContext, request),
            virtualThreadExecutor);

    agent1Output = agent1Future.get();
    agent2Output = agent2Future.get();
}
```

`try-with-resources` calls `.close()` automatically once both futures resolve or an
exception propagates out of the block.

## Detection
Caught via external code review, not via observed failure. No crash or visible symptom
at current request volume — this is a slow resource leak that would only manifest under
sustained production load.

## Rule
Any `ExecutorService` created locally within a method (not injected as a managed Spring
bean) must be scoped with `try-with-resources` unless there's a specific reason to keep
it alive beyond that method call.