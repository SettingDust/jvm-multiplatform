# Worker Integration Evidence

[Work](work.md) | [Context](context.md) | [Plan](plan.md) | [Waves](waves.md)

This ordinary evidence record belongs to `I-worker-docs`. It consumes the completed `I-worker` code Result and records the worker candidate's integrated behavior, resource checks, bounded observations and historical-document reconciliation. The separate signed docs-only commit created by this delivery records this file and the matched Work documents; `I-worker-docs` is completed and `W-worker` is released. FIR, actual/P4, plugin and final acceptance remain pending.

## Candidate and provenance

| Identity | Value |
| --- | --- |
| Worker source tip | `2d584ad5a48a8ceb9597790d0559d2856f2fdd82` |
| Worker integration commit | `0638eaeed173e08a131ba35b8c27d9eba364b6f4` |
| Integration commit tree | `eb5b1ece93d1fb3cd3396a80bc16f37c7828442a` |
| Integration parents | `25b34165c5d93b571f0df730b9af400dd11891fb` + `2d584ad5a48a8ceb9597790d0559d2856f2fdd82` |
| Main worker merge / current code candidate | `20885fe6a10eb496005384460dabd5648121a25d` |
| Main candidate tree | `eb5b1ece93d1fb3cd3396a80bc16f37c7828442a` |
| Main merge parents | `25b34165c5d93b571f0df730b9af400dd11891fb` + `0638eaeed173e08a131ba35b8c27d9eba364b6f4` |
| Code signatures | `git verify-commit` exit 0 for both `0638eae` and `20885fe`; Good EDDSA signatures from `3B992602764F685EE1DD065D4FFAF1C206BF8586` |
| Classpath version | `0.1.12+terrarium.0.1.12` |

The complete Worker evidence is retained in `C:/Users/WINDOWS/AppData/Local/Temp/i-worker-20261004-01`. `code-identity.txt`, `main-identity.txt`, `merge-order.txt`, `tested-inputs.json`, `focused-xml/`, `check-xml/`, `semantic-comparison.txt`, `final-audit.json`, the invocation records and signature logs are the consumed artifacts. This docs delivery did not rerun a build or test.

The code commit changed exactly these five classpath paths:

```text
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/CommonSuperClassFinder.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/GenerateStubApi.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/StubGenerator.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/InheritanceGenerationTest.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/WorkerExecutionTest.kt
```

The code Result retained one `noIsolation` action per task, plain DTO parameters, sequential entries, header-only ancestor reads, no full-node cache, uncached resource connections, exception-safe stream/loader/output cleanup and optional-version fallback. Earlier inheritance and parameter behavior remained in the integrated candidate; no old pairwise ancestor/intersector/signature files were imported from the Worker source.

## Executed checks consumed from the code Result

All rows below are historical code checks. This documentation outcome ran no Gradle task, compiler or test.

| Evidence | Cases | Result |
| --- | ---: | --- |
| Focused isolated module test route | 26 JUnit cases across `WorkerExecutionTest` 1, `CommonSuperClassFinderTest` 2, `InheritanceGenerationTest` 3, `signature.SignatureIntersectionTest` 5, `signature.ParsingTest` 1, `ParameterMetadataTest` 8, `ParameterGenerationTest` 1, `MethodBodiesTest` 4 and `PreserveMethodBodiesTest` 1 | exit 0; failures/errors/skipped all 0 |
| Full isolated `:classpath-api-stubs:check` | 26 JUnit cases in the same saved reports | exit 0; failures/errors/skipped all 0 |
| Root/wrapper route | same focused selector attempted through wrapper 9.2.0 | exit 1 while resolving the offline embedded Kotlin plugin; no root pass claimed |

The focused route used `D:/Applications/Scoop/apps/gradle/9.5.1/bin/gradle.bat` with `-p E:/Projects/cloche/jvm-multiplatform/classpath-api-stubs/verification`, Java 21 runtime, `D:/Applications/Scoop/apps/zulu8-jdk/current` as the Java 8 toolchain path, `--offline --no-daemon --no-configuration-cache --console=plain --max-workers=2`, and production class major version 52. The Gradle invocation records report 65.434 seconds for the focused invocation and 58.535 seconds for the full check. These are historical execution records, not docs-time work.

`WorkerExecutionTest` used two real tasks over four shared jars with 256 classes and 16 methods per class, one warm-up, one measured run and a worker limit of 2. The recorded task intervals overlapped for 227 ms. The test passed semantic jar and manifest checks, class and generic/parameter metadata checks, Code behavior, byte-exact single-input output for both options, classpath precedence, excluded-copy bytes, optional versions, cache/body invalidation, genuine failure cleanup and release of resources followed by a successful rerun. The saved before/current fixture semantic hashes were both `33A167C18712A47AEF0762BE08E7382867E8F19591A8DEFC93F88F7E83DDF242`; both comparison invocations exited 0.

The focused Worker observation recorded `elapsedMs=362`, four 100 ms samples and `sampledPeakWorkingSetBytes=502337536`. The full-check observation recorded `elapsedMs=380`, three samples and `sampledPeakWorkingSetBytes=504176640`. Sampling excluded the launcher and test JVM and measured process working set, not JVM heap or OOM safety. Predecessor `25b34165` compiled successfully and its four semantic comparisons matched the Worker candidate; a comparable performance or memory baseline was unavailable, so these are bounded final-candidate observations only. They establish no before/after performance or memory improvement and do not support a speed or memory claim.

The final audit recorded 27 tested inputs with zero content drift, 21 residual hashes with zero residual drift, 43 original/ref identities with zero non-main ref drift and zero source-worktree drift. Two tested source files have raw CRLF normalization after checkout/merge: `CommonSuperClassFinder.kt` and `StubGenerator.kt`. Their canonical Git blobs and semantic comparison are unchanged; the result cannot claim that every raw working-file hash remained identical.

## Residual state and historical documents

The accepted classpath version remains unchanged. The actual compiler formatter at `kmp-actual-stubs/compiler-plugin/build.gradle.kts`, 18 untracked `.intellijPlatform/localPlatformArtifacts/*.xml` files, `.git/info/exclude` and `.git/next-index-264852.lock` were preserved and left untouched. The XML files remain byte-identical to their saved hashes; no local exclusion was added. No code, build, version, XML, lock, Spec, PRD, ADR or Tasks-history edit belongs to this docs delivery.

The four ordinary documents under [`api-stub-functional-branch-sync`](../api-stub-functional-branch-sync/) were restored from after-base original-main commit `cd623a95a75f07639f35a2bbf1a36f1926605551`, `docs(stubs): record functional branch sync plan`, whose parent is `cf98546a6e7f828b30b4b4a560c86bbf02b20578`. Each retains its original old `main` to functional-branches direction, completion/readiness records and source references, with a recovery note identifying the documents as historical. They are ordinary provenance records only: they do not support current Worker completion, current rebuild authority or any later gate. The old `api-stub-parallel-improvements` and `preserve-method-bodies` Tasks/history remain unchanged.

`I-worker` and `I-worker-docs` are completed and `W-worker` is released. The next explicit dependency is `I-fir`; FIR, actual/P4, plugin, their matching docs and `I-final` remain pending. No FIR or later task ran as part of this delivery.
