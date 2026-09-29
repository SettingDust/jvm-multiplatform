# API Stub Parallel Improvements: Implementation Plan

## Authority, objective, scope

This is one implementation scope derived from [the new Spec](../../spec/api-stub-intersection-and-workers.md) and [the existing Code Spec](../../spec/preserve-method-bodies.md); [Work Context](context.md) records inspected source. Produce conservative inheritance/generic and method-parameter metadata, and task-level parallel worker execution with bounded retention and reliable resource release, then integrate the three results without changing existing Code behavior. The user authorized proceeding with this document package and implementation direction; this Plan records recommended choices and does not claim three separate decisions or a separate receipt for each rule. Do not implement as part of writing this Plan.

Scope is `classpath-api-stubs` production code and focused tests only, plus a per-worktree minimal verification settings file if needed. No change to Cloche, Codev, actual-stubs, release, historical completed records, or main's existing Code Spec. No new coroutine dependency, weighted scheduler, OOM pressure test, or unmeasured performance claim.

## Parallel execution and ownership

The three feature tasks can start concurrently because they use separate worktrees, have independent outcome checks and do not require each other's implementation to begin. This is write isolation, **not** semantic independence:

| Feature / base | Owned change | Overlap and guard |
| --- | --- | --- |
| Inheritance / upstream `cc193b68`, `E:/Projects/cloche/jvm-multiplatform-inheritance` | `CommonSuperClassFinder.kt`, `signature/SignatureIntersector.kt`, raw/header consistency in `ClassNodeIntersector.kt`; if necessary `StubGenerator.kt` reduction for three-input provenance; tests | Upstream lacks current Code/input-tracking implementation. Change only inheritance logic, carry provenance across all original inputs, and present `StubGenerator.kt` diff as an explicit integration touchpoint. Do not transplant upstream generator or task wholesale. |
| Parameters / local main `5aa0b3e`, `E:/Projects/cloche/jvm-multiplatform-parameters` | `ClassNodeIntersector.kt` parameter metadata and focused tests | Shares this file with inheritance. Keep edits to method metadata so integration can reconcile both policies; no signature/inheritance or Code comparator changes. |
| Worker / local main `5aa0b3e`, `E:/Projects/cloche/jvm-multiplatform-worker-memory` | `GenerateStubApi.kt` task submission; `StubGenerator.kt` cache, jar output and resource lifetime; focused tests/measurement | Shares generator with inheritance. Keep class reduction/inheritance decisions out of worker edits; retain complete original nodes long enough for all-input Code comparison but not across unrelated classes. No one-worker global cap. |

Feature task steps and handoff boundaries are in [Tasks](tasks.md). Integration follows **inheritance -> parameters -> worker** on main after all three feature outcomes have evidence. The upstream-based inheritance branch has distinct ancestry; integrate only the scoped feature diff/commits against main after review, never merge the upstream baseline as though it contained local Code. Reconcile the final `ClassNodeIntersector.kt` and `StubGenerator.kt` against local `CodeMethodNode`/`MethodBodies` and existing task input normalization. If a signature/header or Code conflict cannot satisfy both Specs, stop integration and report the conflict rather than replacing one contract with the other's baseline. Separate worktrees do not justify claiming zero merge conflicts.

## Verification strategy

1. Inheritance task: ASM fixture jars with 2/3 inputs and permutations, disordered interfaces, common/differing generic arguments and type variables, superclass chains, absent ancestors, corrupt classes, read errors and cycles. Assert raw header/Signature matching, erasure and scope of every retained type-variable reference. Compile Java and Kotlin consumers against representative generated stubs, particularly when a class Signature must be dropped and method/field signatures reference its formals. A failed consumer compilation is a real gate, not resolved by emitting a one-sided Signature.
2. Parameters task: method descriptor position and local slot cases (instance/static, long/double), synthetic/mandated prefixes, short annotation arrays and annotable counts, visible/invisible annotations, full nested values, name/flag consensus and LVT disagreement. ASM-read generated jar for two/three inputs and permutations; ambiguous mappings omit rather than attach to the wrong slot.
3. Worker task: two independent `GenerateStubApi` tasks with `--max-workers=2` and observable overlap of their `noIsolation` actions; one action per task, per-task sequential class processing. Compare semantic API/manifest/excluded-copy/Code outputs with the pre-worker behavior on fixed fixtures. Inject input and output failures and check handles close and reruns work. On a bounded representative workload record input size/class count, Gradle/JDK, worker limit, elapsed time and peak memory before/after; do not infer a throughput or memory win without measurements. A BuildService limit is not required without evidence.
4. Integration gate: after the ordered application of three feature diffs, rerun all affected package tests, Java/Kotlin consumers, worker overlap and error/resource tests and existing Code + task-cache fixture. Confirm default-off multi-input bodyless behavior, single-input passthrough with either option, two/three-input Code retention/omission and body-only/excluded-copy Gradle invalidation. Review final diff for only authorized module code/test/verification changes, both Specs satisfied, and no commit/push. Report baseline build limitations separately from change failures.

### Reproducible module build route

First try the checkout's own `:classpath-api-stubs:test` task. The prior root-settings failure is recorded in [Context](context.md) but must not be assumed to recur or to count as a passing test. If it does recur, in **that implementation worktree** use an independent installed Gradle 9.4.1, a JDK suitable for running Gradle (the previous route used JDK 21), and a configured Java 8 toolchain. Create `classpath-api-stubs/verification/settings.gradle` in that worktree if absent with exactly this minimal content (the current main tree already tracks it):

```groovy
rootProject.name = 'classpath-api-stubs-verification'
include(':classpath-api-stubs')
project(':classpath-api-stubs').projectDir = file('..')
```

From the worktree root, run `gradle -p classpath-api-stubs/verification :classpath-api-stubs:test --console=plain` using the installed Gradle 9.4.1 executable and an environment-specific Java 8 toolchain location, for example `-Porg.gradle.java.installations.paths=<JDK8_PATH>`. Do not depend on the deleted preservation worktree, its Gradle distribution path, its `build/` outputs, or a successful root settings evaluation. This minimal settings route uses the module's own `build.gradle.kts` and tests; existing plugin implementation-class warnings must be distinguished from test failures. If a checkout has different Gradle/toolchain constraints, record the exact successful or failed invocation rather than claiming the historical run applies.

## Risks and completion

The primary cross-branch conflict is `StubGenerator.kt`: inheritance needs full multi-input ancestry provenance, worker reduces long-lived full-node caching, and local Code needs original per-class methods. Retain full nodes only for the currently processed entry while keeping cheap parent headers available, and verify the combined flow on three inputs. A second conflict is `ClassNodeIntersector.kt`: parameter changes must not overwrite header/signature changes. Annotation layout ambiguity and incomplete generic evidence require conservative omission, not false precision. Worker concurrency can expose resource peaks; start with Gradle's configured worker quota and revisit a BuildService only if measured need appears. No migration or rollback mechanism beyond restoring prior task configuration is needed. Completion is passing feature evidence plus integrated checks, not merely three independent diffs or a successful build on one branch.
