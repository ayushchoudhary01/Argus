# NullPointerException When Both Agents Are DEGRADED

## Problem
If both Agent 1 (Structural) and Agent 2 (Risk) failed validation after their retry and
were marked `DEGRADED`, the pipeline crashed with an NPE deep inside
`AgentService.buildContrarianPrompt()` rather than failing cleanly.

## Root Cause
`AgentOutputValidator` correctly rejects `DEGRADED` output and the agent gets marked
accordingly, but `OrchestrationService` proceeded to call `buildAgent3Context()`
regardless of agent status. A `DEGRADED` agent output has a null `causalChain` (the
partial output returned on terminal failure doesn't populate it), so the very next step —
building Agent 3's prompt, which calls `.getCausalChain().getDriver()` on both prior
agents' outputs — threw `NullPointerException` instead of a meaningful error.

## Fix
Added an explicit guard immediately after both agent futures resolve, before any further
pipeline steps run:

```java
if (agent1Output.getStatus() == AgentStatus.DEGRADED && agent2Output.getStatus() == AgentStatus.DEGRADED) {
    log.error("Both agents DEGRADED — aborting pipeline [correlationId={}]", correlationId);
    throw new PipelineException("Both Structural and Risk agents failed validation", correlationId);
}
```

If only one agent is degraded, the pipeline still proceeds — that's intentional. Partial
insight beats silent failure, per the original design philosophy. Only the double-failure
case needs a hard stop, since there's nothing left to build a meaningful Contrarian Agent
prompt from.

## Rule
Any step that consumes the output of an upstream agent/service must check that output's
status before dereferencing fields that are only guaranteed present on success.
`DEGRADED` is a valid, expected state in this pipeline — every downstream consumer must
treat it as a real branch, not an edge case to ignore.