# Main Rebuild Context

[Work](work.md) | [Plan](plan.md) | [Waves](waves.md)

## Accepted boundary

The current Handoff resolves the prior research's open own-version suggestion: retain existing own numbers, append the frozen upstream component, and change only the three modules listed below. Reset and ordered merges are explicitly authorized. No push, tags, remote publication, publication-pipeline recreation, new method declaration type-annotation implementation, unrelated module changes, or authoritative PRD/ADR/Spec edits are included.

The publication rollback has already removed 15 new files. They are not inputs or missing authority to recreate. The root build's retained diff was formatting only and is now part of base maintenance. The classpath plugin marker regression uses a local fixture repository and is retained functionality, not the canceled publication pipeline.

## Frozen Git identities

| Role | Commit |
| --- | --- |
| Original main | `cd623a95a75f07639f35a2bbf1a36f1926605551` |
| Rebuild base | `af8cf67d12dcb1c8fbca2b3d9c46a62b699a7d28` |
| First old combined integration, excluded from new main ancestry | `1bf7076716bb7cb0291259c81003dad971d16a94` |
| Inheritance merge source | `d05686ae9f3f2171a567e239bc347f10ac712f19` |
| Parameters merge source | `08e4239cdc1e2f84f6f645a3a0188b27443ff4c0` |
| Worker-memory merge source | `2d584ad5a48a8ceb9597790d0559d2856f2fdd82` |
| Single-input byte/cache/lifecycle fix to retain by behavior | `c8cf30c73f5ef8ccf1a5088b6a215ccd3aa2bfcb` |
| Object-termination fix to retain selectively | `cf98546a6e7f828b30b4b4a560c86bbf02b20578` |
| Frozen upstream version source (no fetch) | `cc193b68ffb42d4bcdce6c8cecc0f2b9a135ccb6` |

Base tree: `2240a85596d702b0c4546a6f352016ea4033719c`. Base source trees: classpath `6e0035ba55658585fcb55d23820e08cb2b7866d3`, actual compiler `5e7661e8ba676243982806b974540182a74e6b6f`, class-extension Kotlin plugin `38a995746b8d6b2b50bd3e27d3f4e51274259244`. Base already contains actual matching and methodBodies. The inspected base/current committed actual tree is equal; do not replay original actual work as new behavior.

Repository and branch locations:

| Checkout | Branch | State observed during preparation |
| --- | --- | --- |
| `E:/Projects/cloche/jvm-multiplatform` | `main` | Original main above; index initially empty; 17 modified tracked and 32 untracked files before this tuple |
| `E:/Projects/cloche/jvm-multiplatform-inheritance` | `fix/stubs-inheritance-upstream` | Frozen inheritance tip; two formatting-only test diffs |
| `E:/Projects/cloche/jvm-multiplatform-parameters` | `fix/stubs-method-parameters` | Frozen parameters tip; prior full Result reports clean |
| `E:/Projects/cloche/jvm-multiplatform-worker-memory` | `perf/stubs-worker-memory` | Frozen worker tip; prior full Result reports clean |

Recheck every checkout before any Git write. The table above is the original preparation snapshot, not the current main state. The four tuple files stayed untracked through inheritance code delivery and, together with `inheritance.md`, entered the signed docs-only commit `7a79d220a8114c275ff05536c4148963a78f4738` directly after `d4dd6a46f9a5d9a20474bd3e2fcc8932c61eead2`. Its tree is `7eee33fd8b18e8ee4d94997357451308db26ec7c`; `git verify-commit` passed, and `W-inheritance` is released. The old inventory count is not a current gate.

## Current execution boundary and recovery mapping

`I-preserve` and `I-base` remain completed historical items. At the original graph revision, read-only Git inspection confirmed base/maintenance main `d8c59ab668aa304797fc0870e5217a1edc97fa8b` with parents `af8cf67d12dcb1c8fbca2b3d9c46a62b699a7d28` and `c15548f10b22ebb411b04a1bcf1027db578ddca6`. The latter is the build-maintenance commit on the exact base; `refs/heads/build/main-rebuild-base-20261003-01` still resolves to it. No repeated reset or preservation execution is selected.

The completed `I-inheritance` Result now establishes code candidate `d4dd6a46f9a5d9a20474bd3e2fcc8932c61eead2`, tree `02220a80245bdd8faeca07099cb5421bd495c961`, parents `991e57f0e05829d2c59f3ce93cd2710048f2a2d9` and frozen inheritance tip `d05686ae9f3f2171a567e239bc347f10ac712f19`. `991e57f` retained the three owned main formatter deltas on `d8c59ab`. Before writing this tuple update, `I-inheritance-docs` independently checked main/HEAD, all 40 tested input blobs/raw SHA256/lengths, empty index, absent MERGE_HEAD, all 29 frozen/recovery refs, the 23 initial residual hashes and 18 original XML lengths/hashes; there was no drift. The five authorized ordinary-documentation paths were committed directly after `d4dd6a46` as signed docs-only commit `7a79d220a8114c275ff05536c4148963a78f4738`, tree `7eee33fd8b18e8ee4d94997357451308db26ec7c`; `git verify-commit` passed. `I-inheritance-docs` is completed and `W-inheritance` is released. [Inheritance evidence](inheritance.md) locates the full consumed code Result and all artifacts.

The subsequent authorized docs signing retry is retained as historical evidence in [Inheritance evidence](inheritance.md). It began with the exact retained index tree `5a12cc99fbb63a6bb8a4e29315b6da427173dce4`, and its 300-second attempt did not establish a commit at that checkpoint. That intermediate state was superseded by the successful signed docs-only commit `7a79d220a8114c275ff05536c4148963a78f4738`; no code/build/version inputs changed, and `W-inheritance` is now released. No second retry, signature amendment, build or downstream execution occurred.

The following existing recovery payload refs were resolved locally. Each payload commit's parent is original main `cd623a95a75f07639f35a2bbf1a36f1926605551`; transplant only its owned delta, never its old-main ancestry. Their prior signing/retrieval evidence belongs to the completed preservation Result and was not re-executed by this documentation revision.

| Recovery payload ref | Frozen commit | Consumer |
| --- | --- | --- |
| `refs/heads/recovery/i-preserve-20261003-01/inheritance` | `b4144d9e314cf277c9003d520663c98f5de88f6f` | `I-inheritance`: only the two captured formatting test paths |
| `refs/heads/recovery/i-preserve-20261003-01/fir` | `45f010c168ef023e7983bf82800eb79475c2adf7` | `I-fir` |
| `refs/heads/recovery/i-preserve-20261003-01/actual` | `85a49556a12cc23467f0fbb3815bef1a5e7bb30f` | `I-actual` |
| `refs/heads/recovery/i-preserve-20261003-01/plugin` | `1c917c0a16aacac07758de35416605c86814f439` | `I-plugin` |
| `refs/heads/recovery/i-preserve-20261003-01/maintenance` | `85bb84af3eab67293d38a035e8241ad001635a27` | Already consumed by `I-base`; retain recovery |
| `refs/heads/recovery/i-preserve-20261003-01/documentation` | `489de7331a62de12cbcf14063883e48a380aa7f7` | Docs items: recovery/history evidence only |

Pinned refs `refs/recovery/i-preserve-20261003-01/base` and `/upstream` resolve to the frozen base/upstream SHAs above. The original inventory remains pinned under `/original-01` through `/original-17`; relevant resolved pins are `/original-07` = original main, `/original-05` = inheritance, `/original-06` = parameters, and `/original-08` = worker. Keep all original mappings from the completed preservation Result; these role checks do not replace its complete inventory.

`inheritance-main-functional-rebuild` generation 1 is terminal `blocked`: admission rejected its combined feature merge/test and matching docs-commit outcomes. It made no edits, tests or commits. Retain the six original code IDs and add independent docs IDs in Waves; this revision does not convert that blocked Result into passing code evidence.

The original main formatter inventory had four diffs (47 additions/18 deletions), read as line wrapping/indentation, blank lines, trailing commas and final newline only. `ClassNodeIntersector.kt`, `GenerateStubApi.kt`, and `PreserveMethodBodiesTest.kt` were owned by `I-inheritance` and have now been retained in signed `991e57f` and the integrated candidate. Its two captured source-checkout test deltas also match their recovery/candidate raw blobs. Only `kmp-actual-stubs/compiler-plugin/build.gradle.kts`, owned by `I-actual`, remains unstaged (23 additions/4 deletions, raw SHA256 `AC56A695791F4489A1B82EE2061667C842E12B7340BE774B0529E046550F4334`). It is preserved through this docs delivery. The five ordinary-documentation paths were committed in signed `7a79d220a8114c275ff05536c4148963a78f4738`; the 18 generated `.intellijPlatform` XML files remain untracked and byte-identical, with no new local exclusion. The inherited `.git/next-index-264852.lock` (27760 bytes) is retained. Exact residual paths/hashes remain in the code artifacts and the docs Result.

The earlier offline root `help` and later root test attempt failed KSP resolution. The passing inheritance route was the isolated module on physical Gradle 9.5.1 / Java 21.0.10, preserving Azul Java 8 `1.8.0_482` production targets and recorded class major version 52. Its evidence is 13 freshly rerun focused JUnit cases plus 3 reused `InheritanceGenerationTest` cases on unchanged inputs, with failures/errors/skipped all zero, and 2 separate true/false exact single-input bytes assertions. No full suite, root/wrapper 9.2 pass, worker acceptance or actual/P4 acceptance is claimed. This docs execution consumes those reports without running a build.

## Parameter integration and documentation boundary

The completed `I-parameters` code Result is signed merge commit `a2609ff0d901f077da7042fc1ac70625f4613e5d` with tree `07af1d2009dca9c44f1b2791dadb01783f709a6d`. Its parents are the integrated pre-parameter candidate `fd5be9ef6135ec3bef1f472b942957bd46f581b2` and frozen parameter source `08e4239cdc1e2f84f6f645a3a0188b27443ff4c0`. `git verify-commit` returned exit 0 with a Good EDDSA signature from key `3B992602764F685EE1DD065D4FFAF1C206BF8586`. The code commit changed exactly these four paths:

- `classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/ClassNodeIntersector.kt`
- `classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/ParameterGenerationTest.kt`
- `classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/ParameterMetadataTest.kt`
- `classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/PreserveMethodBodiesTest.kt`

Its historical verification used the isolated `classpath-api-stubs/verification/settings.gradle` route, physical Gradle 9.5.1 and Java 21 runtime, with the Azul Java 8 toolchain and Java 8 production target. The eight saved XML reports contain 25 tests with zero failures, errors or skipped cases: CommonSuperClassFinder 2, InheritanceGeneration 3, MethodBodies 4, ParameterGeneration 1, ParameterMetadata 8, PreserveMethodBodies 1, signature Parsing 1 and SignatureIntersection 5. The evidence also records 30 tested inputs, 19 residual hashes, 29 refs and 3 source checkouts. These are associated historical executions consumed by this docs outcome; no build or test was rerun while writing these documents.

The parameter behavior covers all-original N-way metadata consensus, descriptor-position mapping, wide parameters, synthetic/mandated short tables, both annotation visibilities, nested/enum/array values, names and flags, null and malformed inputs, pair/triple permutations, and retained Code/cache checks. The classpath version remains `0.1.12+terrarium.0.1.12`, applied once by the earlier inheritance integration. The root/wrapper Gradle 9.2.0 offline route remains limited by embedded Kotlin plugin resolution (`org.gradle.kotlin.embedded-kotlin:6.4.1`); it is not a passing root build. A full `:classpath-api-stubs:check` was not run for this docs outcome.

`I-parameters-docs` records this code Result in `parameters.md` and updates the four matched Work documents in its separate signed docs-only commit. Both parameter Results are completed, so `W-parameters` is released. The Worker code Result is now completed at signed main merge `20885fe6a10eb496005384460dabd5648121a25d`, and its separate `I-worker-docs` delivery records the exact candidate, 26-case focused/check evidence, resource and semantic checks, bounded observations and historical branch-sync restoration. `W-worker` is released; FIR, actual/P4, plugin and final audit remain pending.

## Worker integration and documentation boundary

The completed Worker code Result consumed the frozen source tip `2d584ad5a48a8ceb9597790d0559d2856f2fdd82` in signed integration `0638eaeed173e08a131ba35b8c27d9eba364b6f4`, then signed main merge `20885fe6a10eb496005384460dabd5648121a25d` followed the parameter docs candidate. Both code commits have tree `eb5b1ece93d1fb3cd3396a80bc16f37c7828442a`; the integration parents are `25b34165c5d93b571f0df730b9af400dd11891fb` and `2d584ad5a48a8ceb9597790d0559d2856f2fdd82`, and the main merge parents are `25b34165c5d93b571f0df730b9af400dd11891fb` and `0638eaeed173e08a131ba35b8c27d9eba364b6f4`. Both signatures verify Good EDDSA. The code diff contains exactly five classpath paths and retains the accepted classpath version `0.1.12+terrarium.0.1.12`.

The saved isolated Gradle 9.5.1 / Java 21 route with the Azul Java 8 toolchain recorded 26 focused tests and 26 full-check tests, all with failures/errors/skipped zero. Worker execution proved 227 ms overlap for two tasks over four 256-class/16-method jars at worker limit 2, semantic before/current equality, manifest/classes/generics/parameters/Code behavior, byte-exact single-input output, excluded-copy and cache/body invalidation, failure cleanup/release/rerun and resource closure. Focused/check observations were 362/380 ms elapsed with sampled peak working sets about 502/504 MB; baseline compilation was unavailable, so no performance or memory improvement is claimed. The root/wrapper Gradle 9.2.0 offline route failed embedded Kotlin resolution and is not a passing root check. The full evidence and raw-hash/EOL limitation are recorded in [Worker evidence](worker.md).

`I-worker-docs` consumed that unchanged code candidate without rerunning tests. It restored only the four after-base ordinary documents under `api-stub-functional-branch-sync` from original-main commit `cd623a95a75f07639f35a2bbf1a36f1926605551`, retaining their historical old `main` to branches direction, completion/readiness records and source references. Recovery notes mark them as provenance only; they do not support current rebuild authority or Worker completion. Old Tasks/history remains unchanged. `W-worker` is released, and `I-fir` is the next pending item.

## Preservation ownership

| Recovery owner | Valuable dirty content | Eventual integration owner |
| --- | --- | --- |
| FIR feature | Eight production files under `class-extensions/kotlin-plugin/src/main/.../plugin`: `ClassExtensionFirDeclarations`, `FirClassExtensionResolver`, `GeneratedDeclarationResolver`, `LazyExtensionFinder`, `SymbolCopyProvider`, `copiers/ConstructorCopier`, `copiers/FunctionCopier`, `copiers/PropertyCopier`; `CodegenTest.kt`, `test-common.kt`, `test-platform.kt` | `I-fir` |
| Actual feature | `kmp-actual-stubs/compiler-plugin/build.gradle.kts`, `Tests.kt`, `P4AcceptanceTest.kt`, new `P4ProvenanceRegistrar.kt`, service registration under `p4-observer`, and all new `src/test/resources/p4` Cloche/consumer fixtures | `I-actual` |
| Classpath plugin feature | `classpath-api-stubs/build.gradle.kts`, new `ClasspathApiStubsPlugin.kt` and `ClasspathApiStubsPluginTest.kt` | `I-plugin` |
| Build maintenance | Root settings physical path resolution, `class-extensions/idea-plugin/.gitkeep`, root build formatting diff | `I-base` |
| Documentation recovery | Original untracked tuple; original-main after-base docs including branch-sync tuple | Preserved before reset; `I-inheritance-docs` delivered the five paths in signed `7a79d220a8114c275ff05536c4148963a78f4738`, and `W-inheritance` is released; `I-worker-docs` restored the four historical branch-sync documents from `cd623a95a75f07639f35a2bbf1a36f1926605551`, preserving their old `main` to branches direction and provenance; `W-worker` is released |
| Inheritance formatting | `CommonSuperClassFinderTest.kt`, `signature/SignatureIntersectionTest.kt` in inheritance checkout, 21 additions/3 deletions; former main `ClassNodeIntersector.kt`, `GenerateStubApi.kt`, `PreserveMethodBodiesTest.kt` formatter diffs | Incorporated by completed `I-inheritance`; retain captured ref/source tips and dirty source bytes unchanged |
| Actual formatting | Current main's `kmp-actual-stubs/compiler-plugin/build.gradle.kts` formatter diff | Preserve through earlier items; incorporate explicitly with `I-actual` |

The 18 `.intellijPlatform/localPlatformArtifacts/*.xml` files are unrelated generated files: retain on disk byte-for-byte, never stage or delete. If literal clean status is required, narrowly exclude the inventoried exact paths in repository-local `info/exclude` and disclose them; do not change global ignores or hide other content.

## Accepted versions

| Owning properties | Final value | Applied by |
| --- | --- | --- |
| `classpath-api-stubs/gradle.properties` | `0.1.12+terrarium.0.1.12` | Applied by completed `I-inheritance`, once for all classpath work |
| `class-extensions/kotlin-plugin/gradle.properties` | `1.0.11+terrarium.1.0.11` | `I-fir` |
| `kmp-actual-stubs/compiler-plugin/gradle.properties` | `0.1.2+terrarium.0.1.2` | `I-actual` |

Preserve class-extensions annotations `1.0.0`, gradle-plugin `1.0.12`, java-processor `1.0.1`; java-expect-actual annotations/idea-plugin `1.0.0`, processor `1.0.9`; java-processor-util `1.0.0`; kmp actual annotations `1.0.0`; virtual-source-sets gradle-plugin `1.3.6`, idea-plugin `1.0.0`. Root has no version; class-extensions IDEA has no separate version property. Keep java-expect-actual annotations' build-script `1.0.0` override and unrelated fixture dependency versions unchanged.

## Evidence and limitations

Primary planning input: `C:/Users/WINDOWS/.pi/agent/sessions/--E--Projects-cloche--/2026-09-26T19-48-28-195Z_01a0df43-0ba3-7696-8006-8634ea05c989/tasks/clarify-main-rebuild-version-scope-artifacts/result-1.json` (complete read-only Result). This tuple also uses the current Handoff's settled scope, which supersedes that Result's suggestion to ask about own values.

Preparation independently read current status/worktrees, main/base/upstream/fix identities, root build/settings diffs, owning versions, module builds, test inventory, the three linked Specs, and historical branch-sync and parallel-improvement Plans. The old branch-sync Plan describes the opposite direction (main into branches), and is not selected authority. No implementation tests, merge simulation, signing or environment installation occurred during that planning step.

The original compound inheritance Result remains blocked without execution. The subsequent tested-code attempt was separately blocked by GPG/pinentry timeouts; the successful retry used the unchanged staged tree, signed `git commit -S --no-edit` once (exit 0, 0.427 seconds) and verified both merge/formatter signatures with Good EDDSA output. The retry reran five focused classes to recover exact report counts and added an external two-option bytes fixture, without source/build/version edits. These are supplemental evidence for existing code, not new feature code. Six prior failed Gradle attempts and the historical GPG waits remain documented in [Inheritance evidence](inheritance.md), with their original commands/logs retained outside the repository. The inheritance docs execution independently verified the two inheritance code signatures and candidate mapping, then recorded that result in five ordinary docs. The resulting docs-only commit is signed `7a79d220a8114c275ff05536c4148963a78f4738`, has tree `7eee33fd8b18e8ee4d94997357451308db26ec7c`, and releases `W-inheritance`. The parameter docs execution consumed the signed `a2609ff0d901f077da7042fc1ac70625f4613e5d` code Result, independently verified its local candidate identity, signature and four-path diff, and did not rerun tests. The Worker docs execution consumes signed code candidate `20885fe6a10eb496005384460dabd5648121a25d`, records its passing historical 26-case focused/check evidence and bounded observations, restores the four historical branch-sync documents, and does not rerun tests. FIR, actual/P4, plugin and final audit remain pending.

Cross-branch conflict surfaces include `ClassNodeIntersector.kt`, `StubGenerator.kt`, `CommonSuperClassFinder.kt`, `GenerateStubApi.kt`, signature helpers, build setup and tests. Full-file selection from any one branch loses behavior. Preserve c8cf30c's byte equality/cache assertions and cf98546's Object termination, but do not replay cf98546's obsolete launcher/classpath deletion.

Gradle runtimes must be recorded separately from Java 8 production targets. Historical classpath verification used physical Gradle 9.5.1; the wrapper and actual Spec baseline use Gradle 9.2.0/Kotlin 2.2.20, and P4 explicitly asserts product Gradle 9.4.0. These are distinct verification routes, not interchangeable passing evidence. P4 uses a copied Cloche fixture; no sibling Cloche source edits are authorized. The Handoff records the prior offline root KSP limitation; remaining feature-runtime/toolchain availability and verification still require candidate evidence. A prior signing success does not prove a future commit can be signed.
