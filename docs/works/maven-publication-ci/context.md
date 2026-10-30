# Maven publication CI Context

[Work](work.md) | [Plan](plan.md) | [Waves](waves.md)

## Authority assessment

| Family | Disposition and owner | Evidence and reason |
| --- | --- | --- |
| PRD | Skip; repository publishing infrastructure | The [ADR context](../../adr/git-backed-maven-publishing.md#context) and [Spec](../../spec/maven-publication-ci.md) explicitly require no product-requirements change. |
| ADR | Reuse; repository publishing architecture | [Accepted Git-backed Maven publishing](../../adr/git-backed-maven-publishing.md) owns destination, triggers, credentials, complete publications, and retained HTTP support. |
| Spec | Reuse; repository publishing behavior and acceptance | [Maven publication CI](../../spec/maven-publication-ci.md) closes release/snapshot and main-freshness policy, including seeded metadata, complete path/byte comparison, and failure behavior. No policy decision remains open. |

The supplied previous preparation result is adopted as processed history: the two policy gaps were closed, ADR/Spec alignment and 19 relative links were checked, and no Gradle/CI/publication was run. This is not a rerun or an available original report. The current author reread both authority files and inspected the sources below; no contradiction requiring authority changes was found. This package does not edit authority.

## Current repository observations

Inspection baseline: local branch `main`, HEAD `0eee443e2b376fe9730de022dc4fdf31acfe7242`. Initial `git status --short` reported untracked `.intellijPlatform/` and `docs/`; preserve both, including the supplied authority documents. No remote synchronization was performed. The target Work directory did not exist before preparation. The inspected `.github` directory contains issue templates and no workflow. Existing module Work records remain outside this scope.

[Settings](../../../settings.gradle.kts) maps 14 projects, with no additional included root publication build. [Root build](../../../build.gradle.kts) applies `maven-publish` to subprojects and configures HTTP repositories. [Root properties](../../../gradle.properties) enable configuration-on-demand, configuration cache, parallelism and caching; the group is `net.msrandom`, while versions are per-module. At the initial inspection the wrapper was 9.2.0 and KSP was dynamic; the current user-selected wrapper/KSP baseline is 9.8.2/2.3.12, checked against the later version and configuration evidence below. CI must deliberately configure every included project rather than inherit configuration-on-demand's partial view.

The following publication names are source expectations, not an executed inventory. Runtime `MavenPublication` containers, final coordinates, artifacts and destination-specific tasks are the acceptance source; there is no fixed publication-count assertion.

| Project | Expected publications and source |
| --- | --- |
| `:jvm-virtual-source-sets` | `pluginMaven`, `javaVirtualSourceSetsPluginMarkerMaven`; [build](../../../virtual-source-sets/gradle-plugin/build.gradle.kts), plugin ID `net.msrandom.virtual-source-sets`. |
| `:class-extensions-gradle-plugin` | `pluginMaven`, `jvmClassExtensionsPluginMarkerMaven`; [build](../../../class-extensions/gradle-plugin/build.gradle.kts), ID `net.msrandom.classextensions`. |
| `:classpath-api-stubs` | `pluginMaven`, `classpathApiStubsPluginMarkerMaven`; [build](../../../classpath-api-stubs/build.gradle.kts), ID `net.msrandom.classpath-api-stubs`. Its randomized `markerTest` repository and test-task publication dependencies are separate from CI. |
| `:class-extension-annotations` | `maven`; [build](../../../class-extensions/annotations/build.gradle.kts). |
| `:java-class-extensions-processor` | `maven`; [build](../../../class-extensions/java-processor/build.gradle.kts). |
| `:kotlin-class-extensions-plugin` | `maven`; [build](../../../class-extensions/kotlin-plugin/build.gradle.kts). |
| `:java-expect-actual-annotations` | `maven`; [build](../../../java-expect-actual/annotations/build.gradle.kts) also sets version `1.0.0` directly. |
| `:java-expect-actual-processor` | `maven`; [build](../../../java-expect-actual/processor/build.gradle.kts). |
| `:kmp-actual-stub-annotations` | `maven`; [build](../../../kmp-actual-stubs/annotations/build.gradle.kts). |
| `:kmp-actual-stubs-compiler-plugin` | `maven`; [build](../../../kmp-actual-stubs/compiler-plugin/build.gradle.kts). Test observer JARs are not publication declarations. |
| `:java-processor-util` | `maven`; [build](../../../java-processor-util/build.gradle.kts) publishes the Java component with sources and a Java 11 feature. |
| `:jvm-virtual-source-sets-idea`, `:java-expect-actual-idea`, `:class-extensions-idea` | No explicit Maven publication in the [first build](../../../virtual-source-sets/idea-plugin/build.gradle.kts) or [second build](../../../java-expect-actual/idea-plugin/build.gradle.kts); the [third directory](../../../class-extensions/idea-plugin/) contains `.gitkeep`. Include plugin-added publications if runtime configuration produces any; invent none. |

Most production compilation uses JDK 8. `java-processor-util` needs an actual JDK 8 `tools.jar` and JDK 11 compilers/launchers for its feature. IntelliJ builds configure Java 11 targets and Marketplace tasks; full-project configuration may resolve IntelliJ tooling even when it publishes no Maven artifacts. The selected KSP version is fixed at 2.3.12. These remain risks to verify at the producer/task and publication boundary, not permission to change dependency policy.

## Later execution evidence and recovery boundary

The implementation Handler `implement-jvm-maven-local` g1 returned `failed` in the Settings ABI configuration phase. Per the Router-provided processed history, it changed no source, produced no publisher, and supplied no publication or consumer evidence; `I-local` and `G-local` therefore remain unsatisfied. This failure fact is retained, not replaced by the later configuration check. Its original Result locator is intentionally not persisted here.

The user later authorized Gradle 9.8.2 and manually fixed KSP at 2.3.12. Existing-repository evidence is [configuration validation](../../../build/tmp/verify-user-jvm-config-fix-20261030-064235/config-validation.log) and [version report](../../../build/tmp/verify-user-jvm-config-fix-20261030-064235/version.log): Gradle 9.8.2, Kotlin 2.4.10, JetBrains JBR 21.0.11; `help` and `javaToolchains` both exited 0, and both tasks were executed. Gradle discovered JDK 8, 11, 16, 17, 21 and 25. The check reused the user's existing Gradle home and proxy. Configuration files were unchanged across this validation. This verifies the corrected configuration only: no compilation, publication, consumer check, producer predicate or gate was completed.

At that refinement, `I-local-recovery`/`W-local-recovery`/`G-local-recovery` were the new S1 recovery boundaries, separate from failed `I-local`/unreleased `G-local`. Their original producer/consumer contract remains in Waves as history. The later mixed attempt below does not satisfy them; the current replacement is `I-local-acceptance-recovery`, not a resume of any terminal Result.

## Settled constraints and remaining proof

The ADR/Spec own the complete confirmed policy, including branch-root output, full publication set with POM-only markers, native metadata, immutable complete releases, mutable snapshots retaining history, and the current remote `main` checkpoint. Supplied history says the three old `maven-repo` branches were deleted and the central repository retained; neither statement was remotely rechecked here, and neither is an implementation dependency.

Concrete implementation choices in the Plan concern local paths, task wiring, tool provisioning and comparison. Generation 2 establishes one actual inventory and complete empty-baseline publication, not full S1 consumption/toolchains, S2 repeatability/history or S3 safe Git updates. Hosted execution and real token permissions remain unverified after local acceptance. This documentation maintenance does not run Gradle or graph items, commit, or publish.

## Mixed generation 2 partial and retained effects

The supplied adopted Result was fully read and its JSON envelope decoded: task `implement-jvm-maven-local-tree`, generation **2**, transport `completed`, semantic **partial**. The attempted scope mixed original S1 `I-local-recovery` with independently gated S2 `I-candidate`. Preserve its partial status and all effective work, without assigning S2 a completed Result, rewriting the original S1 contract, or resuming the terminal Handler. This maintenance did not execute either outcome.

Source HEAD reported there, and confirmed by this maintenance's read-only `git rev-parse HEAD`, is `bc755a701dd0400477ef6a001145c2f30a1a93f4`, following migration commits `70e1b3d`/`bc755a7`. HEAD alone does not identify the uncommitted publisher/fixtures: the Result records those retained working-tree effects. The current read-only status still shows modified root build and untracked publishing support/other user material; nothing is removed or staged.

Existing readable repository evidence:

| Evidence | Actual method/result and limit |
| --- | --- |
| [Inventory](../../../build/tmp/i-local-recovery-current/inspect.log) | `inspectCiMaven`, exit 0: dynamic final project/publication/GAV/artifact/destination-task correspondence. Root and three IDEA projects evaluated with no publications; none excluded. 14 is observed, never a fixed count or allowlist. |
| [Publication](../../../build/tmp/i-local-recovery-current/publish.log) | `publishCiMaven` with independent baseline/candidate properties, `--no-configure-on-demand --no-configuration-cache --no-parallel --no-build-cache --rerun-tasks --console=plain`, exit 0, `BUILD SUCCESSFUL in 43s`, 91 tasks executed. Fourteen `CI_VERIFIED` lines and `CI_MAVEN_COMPLETE publications=14`; only named `CiMaven` publication tasks, no HTTP/mavenLocal/markerTest/Marketplace or root test/check publication path. The existing validator checked artifacts/classifiers, POM identities, applicable module metadata, four checksum types and marker dependencies against an empty baseline. This is not independent proof of all candidate policies. |
| [Consumer attempt](../../../build/tmp/i-local-recovery-current/plugin-consumer.log) | Independent-Home `verifyPlugin` attempt stopped during wrapper download around 60%; no task completion or exit code. The Result reports no Java 11 consumer log or consumer exit record, consistent with the current evidence directory. Neither consumer passed; this is not a consumer assertion failure. |
| [Publisher](../../../gradle/maven-publication-ci.gradle.kts) and [consumer support](../../../.github/scripts/maven-publication-consumers/) | Root apply, dynamic inventory/task wiring, baseline/candidate path checks, release rebuilding/comparison, checksum/POM checks and preliminary snapshot-history code are retained. Existing plugin/java11/producer fixtures are retained; source inspection establishes their existence, not successful execution. Candidate-policy effects remain unverified S2 work. |

The Result reports checked-in Gradle **9.8.2**, Microsoft OpenJDK **17.0.19+10-LTS**, and use of the existing user Gradle home for production publication. It reports that the candidate still had **295 files**, an empty independent baseline, both formerly missing compiler-plugin JARs and the Java 11 classifier. This author reread the repository logs, not the temporary trees or a new publication; temporary output availability/current applicability must be checked at execution. The older 14/12 failure remains historical failure as reported by the adopted generation-2 Result, not current output truth or a newly recovered original. Generation 2's 14/14 success supersedes that output shortage only; it completes no item/gate and proves neither P4 nor the full project tests.

The earlier JBR 21 `help`/`javaToolchains` check is configuration-only; it cannot turn this Microsoft 17 publication into JDK 21 producer acceptance. Actual JDK 8 compiler/`tools.jar` and JDK 11 toolchain use/installations were not fully recorded for generation 2. Preserve runtime **JDK 21** plus JDK **8/11** acceptance from Plan/Waves; the mistaken JDK 17 Handoff did not change that requirement.

Missing S1 proof is plugin-ID resolution from the candidate, Java 11 metadata variant selection and usable artifacts, current output applicability and necessary runtime/toolchain correlation. Missing S2 proof is actual independent forced-regeneration path/byte repeatability, one-module bump, complete release conflict/rejection cases, seeded metadata/version-list/older-numbered latest/release and twice-published native snapshot/current-snapshot consumption with retained history. No workflow/Git helper, remote publication, commit or gate release resulted from this attempt.

## Current bounded recovery and review boundary

New `I-local-acceptance-recovery`/`W-local-acceptance-recovery`/`G-local-acceptance-recovery` carry only original **S1** in the sole Waves graph. They require a new Handler and independent Result, not successful completion of historical partials. Inspect latest source/inventory/output/evidence first and retain applicable effects. Rebuild only for a concrete source/output/environment/provenance mismatch; known JDK 17 versus required JDK 21 is not waived or hidden behind historical configuration. Do not rerun all publication or S2 policy checks merely because consumer download was interrupted. S2 remains `I-candidate`, waiting for the new complete S1 Result and gate; retain its premature code without accepting its policy claims.

Reuse the user's Gradle Home, installed distribution/toolchains and proxy. Do not create another entire Home and redownload the wrapper. Consumer acceptance still requires genuinely fresh tested-coordinate resolution with candidate-only marker/implementation and Java 11 metadata/artifact provenance, no same-GAV cache or external fallback. Isolate only necessary consumer resolution state, and report the exact mechanism/evidence; source settings, byte equality or refresh flags alone do not prove freshness. If these constraints cannot be jointly met, report the specific incompatibility rather than weakening consumption or altering user/global caches/settings.

The user defers Cloche production adaptation and P4 local Codev wiring: [migration closeout](../kotlin-2410-plugin-migration/context.md#accepted-closeout-and-deferred-actual-boundary). Both migrations were subsequently committed per supplied authorization; this does not complete P4, this Work or any Maven gate. No new P4 work/dependency, test-count target, test deletion, general framework or separate audit/diagnostic item is introduced. No own implementation/build is active per supplied coordination context; no process-control action was taken here.

The author self-check covers original S1/S2/S3/gate coverage, single-result recovery, history versus current membership, combined item/member-gate/predecessor waits, conflicts and readable relative links. Independent read-only review remains required for this local change: full-predicate S1 replacement and retained partial/14-publication effects; JDK 17/21 and actual toolchain gap; reuse-versus-regeneration and fresh-consumer/Home compatibility; candidate's unchanged S2 boundary and new prerequisite; downstream waits/history/portable references. Unchanged policy is not reopened. This record is not that independent review, dispatch, implementation acceptance or gate release.
