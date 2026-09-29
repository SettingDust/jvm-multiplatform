# API Stub Parallel Improvements: Context

## Authority and decisions

- [New owning Spec](../../spec/api-stub-intersection-and-workers.md): conservative generic/inheritance evidence, parameter layout/values, worker/resource behavior and acceptance. These are implementation choices for the authorized change, not invented individual approvals.
- [Existing Code Spec](../../spec/preserve-method-bodies.md): `preserveMethodBodies` defaults to false; single-input passthrough retains Code; enabled multi-input retains only all-input-equal Code; input changes including body-only edits must invalidate stale task output. Its historical implementation-status prose describes an earlier baseline, not this checkout's present source; do not edit that completed record.
- [Shared domain context](../../../../CONTEXT.md) defines common API intersection as a compilation view, not runtime equivalence. [Historical investigation](../../../../../upstream-differences.md) is research at fixed older commits, not live test evidence or current authority.

## Current source and worktrees

- `E:/Projects/cloche/jvm-multiplatform` main is `5aa0b3e1053115057f3781ab5656dccb253771fa`. `E:/Projects/cloche/jvm-multiplatform-inheritance` / `fix/stubs-inheritance-upstream` starts at true upstream `cc193b68ffb42d4bcdce6c8cecc0f2b9a135ccb6`. `E:/Projects/cloche/jvm-multiplatform-parameters` / `fix/stubs-method-parameters` and `E:/Projects/cloche/jvm-multiplatform-worker-memory` / `perf/stubs-worker-memory` start at local main `5aa0b3e1053115057f3781ab5656dccb253771fa`. All four were clean when inspected. Differences in ancestry matter: upstream does not have `CodeMethodNode`, `MethodBodies`, default-off option or full-byte artifact tracking.
- Current main `ClassNodeIntersector.kt` uses a raw `interfaces` set intersection but `SignatureIntersector.kt` zips superinterfaces by position, may take one side's type parameters, and assumes same-kind type-variable names correspond; `CommonSuperClassFinder.kt` returns a missing immediate parent instead of proving it. `StubGenerator.kt` reduces `(loader,node)` pairs so a third input can query ancestors through a loader that does not represent the earlier pair. `StubGenerator.kt` is also the worker's target for cache/output changes: two isolated worktrees do not remove semantic overlap.
- Main `ClassNodeIntersector.kt` does not intersect `MethodNode` parameter annotation arrays or `MethodParameters`; `StubGenerator.kt` caches full `ClassNode`s per loader, uses `FileSystems.newFileSystem` jar output, and closes classloaders only on successful completion. Main `GenerateStubApi.kt` directly invokes the generator and already has `@Input preserveMethodBodies` plus nested artifact `@InputFile` with `NAME_ONLY` path sensitivity. Existing `MethodBodiesTest.kt` and `PreserveMethodBodiesTest.kt` cover Code and Gradle input-state regression.

## Verification environment and limits

- `classpath-api-stubs/build.gradle.kts` uses ASM 9.7.1, Kotlin test, Gradle TestKit, and Java 8 production toolchain. Root wrapper is Gradle 9.2.0. The earlier [verification report](../preserve-method-bodies/verification.zh-CN.md) recorded root `settings.gradle.kts:8` `Unresolved reference 'file'` and success with isolated Gradle 9.4.1 module settings; its old feature-worktree build outputs are not a dependency. Main currently contains `classpath-api-stubs/verification/settings.gradle` (tracked); the upstream-based inheritance worktree does not. Recreate a minimal settings file in that implementation worktree if the root route fails, as the [Plan](plan.md) specifies. Do not infer current test results from the historical report.
- Actual tests, consumer compilation, task-overlap observations, handle cleanup and performance measurements have not been run for this document package. No Cloche/Codev/actual module edits, publication, commit, push, or OOM stress testing are planned.

## Open integration work

Finish all three independently scoped implementations with focused evidence, then integrate only their changes onto main in inheritance -> parameters -> worker order; reconcile `StubGenerator.kt` and preserve local Code/caching contracts. Run the final integrated checks in [Tasks](tasks.md). There is no approval request embedded in this Work.
