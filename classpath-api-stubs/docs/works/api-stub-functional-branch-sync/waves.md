> **Historical recovery note (`I-worker-docs`):** This after-base ordinary document was restored from original-main commit `cd623a95a75f07639f35a2bbf1a36f1926605551` (`docs(stubs): record functional branch sync plan`, parent `cf98546a6e7f828b30b4b4a560c86bbf02b20578`). It records the historical old `main` to functional-branches direction and retains its original completion/readiness records and source references. It is ordinary provenance only; it does not support current `main-functional-rebuild` Worker completion, authority or any later gate. Current Worker evidence is [worker.md](../main-functional-rebuild/worker.md).
# Functional Branch Sync Waves

[Work](work.md) | Selected [Plan](plan.md) | Processed [Context](context.md)

Sole executable graph for the selected Plan's three independently verified signed local branch commits, not implementation approval. Each item requires Router-obtained current-user confirmation, fresh state reconciliation, the fixed source SHA, and exclusive resources. Every item is unexecuted. Each has exactly one Handler and one Result; edit/test/stage/commit steps are internal, not separate deliveries.

## Wave W-functional-sync

- ID: `W-functional-sync`
- Items: [`I-inheritance`, `I-parameters`, `I-worker-memory`]
- Predecessor gates: []
- Incremental outcome: three functional branches each contain their own narrowly synchronized, validated signed local commit, with existing changes preserved.
- Release predicate: all three items have full `completed` Results proving their own predicates; Router checks fixed source, passing checks, allowed paths, actual full SHA/signature and residual state. Missing, `partial`, `blocked`, or `failed` Results do not satisfy this gate. The selected Plan requires this gate and no other gate.

Items have no code prerequisites, but mutually conflict. Only one Handler may hold Git mutation/build/signing ownership at a time for its whole item. Preferred serial order is inheritance, parameters, worker-memory. Shared read-only main/Specs do not conflict. Display order does not create dependencies. Incomplete Results remain independent; they neither complete this Wave nor silently authorize further work.

## Shared Item Rules

Read [intersection/worker Spec](../../spec/api-stub-intersection-and-workers.md), [Code Spec](../../spec/preserve-method-bodies.md), selected Plan/Work/Context and source object `cf98546a6e7f828b30b4b4a560c86bbf02b20578`. Refresh target branch/HEAD/status and preserve existing changes/guards. Select functional hunks; no full merge, reset, cleanup, global changes, authority writes or historic Tasks execution/migration.

Each allowlist below is exact and worktree-relative. It bounds both edits and staging, including fixtures/minimal verification support. No edits outside it. Include existing in-scope dirty work in the one verified commit without silently weakening it. Unchanged listed files need not appear in the commit. Ordinary ignored build outputs may be generated but never staged; neither temporary evidence nor this shared package belongs in a branch commit.

Follow Plan's physical Gradle/JDK route, Java 8 production toolchain, no-clean checks, explicit-path staging and signed-commit failure rules. Every Result reports source SHA, target/branch, pre/post HEAD, changed/committed paths, exact executed commands/outcomes, signature evidence and residual index/worktree state. Test reports/measurement evidence do not become separate outcomes.

## Item I-inheritance

- ID: `I-inheritance`
- Wave: `W-functional-sync`
- Prerequisite items: []
- Conflicts: [`I-parameters`, `I-worker-memory`]
- Owner: `E:/Projects/cloche/jvm-multiplatform-inheritance`, branch `fix/stubs-inheritance-upstream`.
- Starting HEAD: `cc193b68ffb42d4bcdce6c8cecc0f2b9a135ccb6`, subject to non-destructive refresh.
- Objective: one validated signed local commit completing only inheritance/generics and necessary passthrough/input/test compatibility.
- Completion predicate: intended branch has one new signed commit parented by refreshed pre-commit HEAD; changed paths stay within this allowlist; its unchanged candidate passes every check below; its full SHA and successful signature verification appear in this Result. Existing in-scope work is retained and unrelated dirty work not committed.

### Exact Write And Commit Allowlist

```text
classpath-api-stubs/build.gradle.kts
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/ClassNodeIntersector.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/CommonSuperClassFinder.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/GenerateStubApi.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/StubGenerator.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/signature/SignatureIntersector.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/CommonSuperClassFinderTest.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/signature/SignatureIntersectionTest.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/InheritanceGenerationTest.kt
classpath-api-stubs/verification/settings.gradle
```

### In-Item Steps And Verification

1. Preserve the four existing source edits and two untracked tests; compare functional hunks to pinned main. Keep all-original-class/own-classpath association and list/binary adapters. Do not import parameter helpers or worker DTOs.
2. Ensure complete-name interface matching, conservative Signature omission, raw-header consistency, valid erasures and no dangling type variables in affected declarations. Missing parent bytes may fall back to Object; cycles and corrupt/I/O failures fail generation with owned resources closed. Any correction stays in listed inheritance/signature files.
3. Import main's single-input class-byte copy using an uncached connection in `StubGenerator.kt`; retain exception-safe loader closure. In `GenerateStubApi.kt`, import only byte-sensitive `@InputFile`/`@PathSensitive(NAME_ONLY)` artifact tracking needed for passthrough/copies. No worker or optional Code feature. Multi-input generation remains bodyless.
4. Keep `jvmToolchain(8)`; add only existing target-family current-JDK test launcher and `stubs.test.classpath` setup to `build.gradle.kts`. Isolated settings include the real module. New fixtures use physical Gradle when Plan's environment variable is supplied.
5. Focused selectors: `net.msrandom.stubs.CommonSuperClassFinderTest`, `net.msrandom.stubs.signature.SignatureIntersectionTest`, and new `net.msrandom.stubs.InheritanceGenerationTest`. Cover two/three-input permutations, disordered/unrelated interface names, missing parents, cycle/corrupt-parent failure, renamed/conflicting variables/bounds, erasure mismatch and formerly dangling variables. ASM-read every surviving affected Signature/raw header. Compile representative Java/Kotlin consumers against supported generated generic/raw-fallback fixtures using existing Gradle/Kotlin infrastructure, without new framework/dependency or runtime Code promise. Verify single-input class bytes equal input bytes and body-only changes invalidate real task inputs.
6. Run isolated `:classpath-api-stubs:check --rerun-tasks`; record toolchain/compile target, tests and consumer commands/results. Failure forbids staging/commit. Only after all checks pass, stage explicit allowed paths, create one `git commit -S`, verify signature/paths and report full SHA.

### Required Evidence And Risks

Retained-dirty-change comparison, pinned-source rationale, ASM/consumer/passthrough results, focused/full-module results, candidate identity and commit parent/paths/signature/SHA/residual status. Narrow existing tests alone do not prove generic correctness. This pre-Code branch intentionally remains without multi-input preservation; do not claim the whole Code feature or all main functions were synchronized.

## Item I-parameters

- ID: `I-parameters`
- Wave: `W-functional-sync`
- Prerequisite items: []
- Conflicts: [`I-inheritance`, `I-worker-memory`]
- Owner: `E:/Projects/cloche/jvm-multiplatform-parameters`, branch `fix/stubs-method-parameters`.
- Starting HEAD: `5aa0b3e1053115057f3781ab5656dccb253771fa`, subject to non-destructive refresh.
- Objective: one validated signed local commit synchronizing all-original-input parameter metadata without losing guards or existing Code behavior.
- Completion predicate: intended branch has one new signed allowed-file commit parented by refreshed pre-commit HEAD; all required checks pass for its unchanged candidate; full SHA and successful signature verification appear in this Result; guards/existing work survive and unrelated dirty work is not committed.

### Exact Write And Commit Allowlist

```text
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/ClassNodeIntersector.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/StubGenerator.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/ParameterMetadataTest.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/ParameterGenerationTest.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/PreserveMethodBodiesTest.kt
```

### In-Item Steps And Verification

1. Preserve existing metadata edits/test. Port main's recursive annotation-value equality, per-visibility layout consensus and all-original-method selection while retaining descriptor-size, null ParameterNode/null-name, flag and short-table checks. No replacement with weaker validation or unrelated class/signature/ancestor code.
2. Keep binary intersector callers compatible. Use one metadata-application path for binary and complete original groups. In `StubGenerator.kt`, collect every original method matching an emitted JVM name/descriptor; apply metadata after declaration intersection, before unchanged `MethodBodies.preserve`. An intermediate result is not all-input consensus evidence. No whole N-way class rewrite or generic framework.
3. Preserve single-input bytes with main's direct byte-copy/uncached connection, without importing DTO/cache-removal changes. Existing byte-sensitive inputs, Code options and remaining loader behavior stay unchanged.
4. New `ParameterGenerationTest.kt` covers generator/task flow, not just binary helpers. Change `PreserveMethodBodiesTest.kt` only for TestKit physical-distribution selection; retain all assertions. This target already has appropriate test launcher/classpath and isolated settings, so no build/settings edits.
5. Focused selectors: `net.msrandom.stubs.ParameterMetadataTest`, new `net.msrandom.stubs.ParameterGenerationTest`, `net.msrandom.stubs.MethodBodiesTest`, `net.msrandom.stubs.PreserveMethodBodiesTest`. ASM-read both annotation arrays/counts, names/flags for static/instance descriptors, receiver/wide slots, synthetic/mandated prefixes, absent/short/overlong/ambiguous tables, absent/wrong-length/null-entry/null-name MethodParameters, nested/enum/array value consensus/conflict, LVT-only names, and every two/three-input permutation. Include an intermediate-omission case that needs original mapping evidence. Verify byte-exact one-input output, no NPE/metadata shift on malformed inputs, and existing default-off/all-input body/cache invalidation assertions.
6. Run isolated `:classpath-api-stubs:check --rerun-tasks` including existing signature parsing tests. Verify unchanged Java 8 target. Only after all checks pass, stage explicit allowed paths, create one signed local commit, verify signature/paths and report full SHA.

### Required Evidence And Risks

Guard-preservation diff and malformed-layout regressions, original-group/permutation outcomes, Code/cache regression and focused/full-module outputs, candidate/commit parent/paths/signature/SHA/residual state. Main's weaker guards are a known regression risk, not an accepted shortcut. Required ancestor/signature changes must not be smuggled into the shared file.

## Item I-worker-memory

- ID: `I-worker-memory`
- Wave: `W-functional-sync`
- Prerequisite items: []
- Conflicts: [`I-inheritance`, `I-parameters`]
- Owner: `E:/Projects/cloche/jvm-multiplatform-worker-memory`, branch `perf/stubs-worker-memory`.
- Starting HEAD: `5aa0b3e1053115057f3781ab5656dccb253771fa`, subject to non-destructive refresh.
- Objective: one validated signed local commit finishing worker/resources with actual task-overlap and bounded retention evidence, preserving current API/Code behavior.
- Completion predicate: intended branch has one new signed allowed-file commit parented by refreshed pre-commit HEAD; its unchanged candidate passes focused/full-module/worker-resource checks and has recorded bounded measurements; full SHA and successful signature verification appear in this Result; existing work is preserved.

### Exact Write And Commit Allowlist

```text
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/CommonSuperClassFinder.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/GenerateStubApi.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/StubGenerator.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/WorkerExecutionTest.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/PreserveMethodBodiesTest.kt
classpath-api-stubs/verification/settings.gradle
```

### In-Item Steps And Verification

1. Retain existing ArtifactSpec/plain parameters/group reconstruction, one `noIsolation` action/task, sequential writes, no full-ClassNode cache, header-only ancestor reads and exception-safe cleanup/closure. Keep current pairwise inheritance/member algorithms; no main N-way ancestor/parameter/signature imports.
2. Port main's outer experimental-path opt-in, optional-version `?: ""` fallback, direct one-input byte-copy and shared uncached resource stream (`URLConnection.useCaches=false`) for full-class/header reads. Keep classpath precedence, manifest, excluded selection/copy bytes and all-original Code comparison.
3. Keep isolated settings local to the real module. New `WorkerExecutionTest.kt` uses existing ASM/TestKit/JUnit; `PreserveMethodBodiesTest.kt` changes only to honor physical distribution selection. Production Java 8 and existing current-JDK test launcher remain unchanged.
4. Focused selectors: new `net.msrandom.stubs.WorkerExecutionTest`, `net.msrandom.stubs.MethodBodiesTest`, `net.msrandom.stubs.PreserveMethodBodiesTest`. Run at least two independent real GenerateStubApi tasks on shared JARs in one Gradle invocation with `--max-workers=2`. Record actual WorkAction start/end intervals through fixture-local Gradle operation evidence, not task registration or the flag alone. No production measurement instrumentation. Assert overlap and semantic output equality: ASM-read headers/members/metadata/Code, manifest/entries, excluded-copy bytes, classpath precedence, default-off, single-input both options, and two/three-input matching/mismatching Code. ZIP timestamps are not an equality gate.
5. Exercise corrupt class, genuine read/output failures and cleanup; close handles and reopen/rename fixture JARs on Windows after success/failure, then rerun successfully. Verify unchanged-input up-to-date/cache reuse, included-body-only/excluded-copy changes invalidate stale outputs, and each output has a single writer. Keep all existing Code assertions.
6. Attempt bounded before/after elapsed-time and sampled peak-memory observations on pre-edit dirty and final candidates using identical workload/runtime/worker limit. A temporary unstaged baseline copy may be used; never reset live work. If the baseline cannot configure/compile/run (for example, the missing experimental-path opt-in), record the exact failure, collect final-candidate observations, and explicitly state that no before/after comparison is established; do not repair another functional slice to manufacture a baseline. Representative workload: two tasks, same four generated JARs with 256 classes each and 16 methods/class, `--max-workers=2`, one warm-up plus one measured run per viable candidate. Identify monitored processes and distinguish process working-set peak from JVM heap/OOM safety. If this fixture is too short to prove overlap, report the limitation; do not claim overlap or speedup from noisy timing. No performance-improvement threshold is invented, but reproducible final observations and no unintended global serial cap are required.
7. Run isolated `:classpath-api-stubs:check --rerun-tasks`, including signature parser. Only after required correctness/resource/overlap checks pass, stage explicit allowed paths, create one signed local commit, verify signature/paths and report full SHA.

### Required Evidence And Risks

Plain-parameter/one-action/retention review, real worker intervals, workload and baseline/final observations, semantic-output/failure-release/cache regressions, focused/full-module outputs, candidate/commit parent/paths/signature/SHA/residual state. Measurements characterize this fixture, not production OOM safety. Cached-JAR coupling and experimental opt-in are actual source differences. Main's generic/parameter improvements remain excluded.

## Draft Graph Validation

One Wave, three items. IDs are unique; all memberships/conflicts resolve locally. Prerequisites/predecessor gates are explicitly empty, hence acyclic with no self-gate dependency. All three conflict pairs are reciprocal and enforce one active Handler. Plan/Work/Waves links are reciprocal direct same-directory members; exact selector is in [Plan](plan.md). Local document validation is package evidence only, not runtime Resolver success or implementation authorization.
