# Preserve Method Bodies Work

## Goal and authority

Deliver the `classpath-api-stubs` method-body preservation behavior specified in [the confirmed Spec](../../spec/preserve-method-bodies.md), without changing existing class/member selection. This Work is a coordination entry, not a new behavior contract.

- Baseline: `main` at `9bd0e543ab6d2de4698846cda47b21356ae04acb`. The confirmed Spec and these Work documents remain uncommitted in the main working tree; the implementation worktree was created from that HEAD.
- [Processed context](context.md) records the implementation and verification boundaries.
- [Implementation Plan](plan.md), [Tasks](tasks.md), and [verification report](verification.zh-CN.md) record the completed work and its evidence.

## Stage and boundary

Implementation and verification of T1-T4 are complete in the separate `feat/preserve-method-bodies` worktree at `E:/Projects/cloche/jvm-multiplatform-preserve-method-bodies`. The user authorized implementation; this record does not claim a separate, later confirmation of the Plan. The main working tree holds the Spec and all Work documents, including the migrated verification report. No commit, push, or merge is part of this recorded result.

## Completion signal

T1-T4 checks passed: six JUnit tests, zero failures/errors/skips, including a TestKit fixture with 11 Gradle builds. The [report](verification.zh-CN.md) links the implementation-worktree test evidence and records the isolated Gradle 9.4.1 verification route. The repository's Gradle 9.2.0 wrapper/root settings did not pass; this is not a claim of a successful root build. Stub compilation, JVM execution, verifier success, and injection success were not gates. Implementation changes remain uncommitted on the feature worktree.
