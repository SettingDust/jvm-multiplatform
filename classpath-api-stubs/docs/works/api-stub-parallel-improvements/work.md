# API Stub Parallel Improvements: Work

## Goal and authority

Deliver the bounded intersection, parameter-metadata, and worker/resource behavior in [the new Spec](../../spec/api-stub-intersection-and-workers.md) while preserving [the existing Code contract](../../spec/preserve-method-bodies.md). This entry coordinates delivery; it does not replace either Spec. [Processed Context](context.md), [Plan](plan.md), and [Tasks](tasks.md) are the execution package.

## Stage and location

Planning ready; no implementation, integration, tests, commit, or push performed by this Work. These rules record the recommended implementation choices for this authorized work, not separate item-by-item user approvals. This Work and both Specs live only in the `E:/Projects/cloche/jvm-multiplatform` main worktree; workers reference these absolute paths instead of copying documents to feature worktrees. The three implementation tasks are independently startable in their named worktrees. Integration is separately gated by all three outcomes, with order inheritance, parameters, worker.

## Completion signal

The three feature tasks and final integration gate in [Tasks](tasks.md) pass their targeted checks, with actual build commands and limitations reported; Code behavior and cache-input regressions pass on the integrated tree. No claim of performance improvement without representative measured evidence. No historical completion record changes or production changes outside `classpath-api-stubs`.
