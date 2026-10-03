> **Historical recovery note (`I-worker-docs`):** This after-base ordinary document was restored from original-main commit `cd623a95a75f07639f35a2bbf1a36f1926605551` (`docs(stubs): record functional branch sync plan`, parent `cf98546a6e7f828b30b4b4a560c86bbf02b20578`). It records the historical old `main` to functional-branches direction and retains its original completion/readiness records and source references. It is ordinary provenance only; it does not support current `main-functional-rebuild` Worker completion, authority or any later gate. Current Worker evidence is [worker.md](../main-functional-rebuild/worker.md).
# Selective API Stub Branch Synchronization

[Work](work.md) | [Context](context.md) | Sole executable graph: [Waves](waves.md)

## Objective And Confirmation Boundary

One shared, confirmation-ready implementation plan for selectively bringing the fixed main implementation into three existing functional branches, retaining current work, and producing one validated signed local commit and SHA per branch. This Plan is derived from the [intersection/worker Spec](../../spec/api-stub-intersection-and-workers.md) and [Code Spec](../../spec/preserve-method-bodies.md), not new authority.

The Router must substantively disclose the detailed scopes and obtain explicit current-user confirmation before implementation. Readiness, the Handoff, old intent confirmation, paths, and graph validation are not detailed-plan approval. There is one matched `plan.md`/`waves.md` pair, no overview stage, and no new or selected `tasks.md`.

The selected Plan outcome requires gate `W-functional-sync` in [waves.md](waves.md): three separate completed branch Results, each including actual passing verification and its own signed local commit SHA. This planning Handler only prepares the package; every branch item starts unexecuted.

## Fixed Source And Exclusions

Source each relevant hunk from Git object `cf98546a6e7f828b30b4b4a560c86bbf02b20578` in `E:/Projects/cloche/jvm-multiplatform`. Targets and starting HEADs are recorded in [Context](context.md). No moving-main synchronization, full merge, cherry-pick of a mixed-feature commit, replacement of dirty files, fetch, push, publication, tag, rebase, reset, cleanup, or global configuration/environment changes are authorized.

Do not change Cloche, actual-stubs, Codev, root settings, unrelated functions, the Specs, or historical Work/Plan/Tasks. Do not migrate historical delivery before this new scope. File equality with main is not functional success; differences between the target branches are intentional.

## Behavior And Ownership Boundaries

| Item | Selective behavior | Minimal compatibility dependencies | Deliberate exclusions |
| --- | --- | --- | --- |
| `I-inheritance` | Preserve/complete the all-original-input ancestor/signature slice; conservative generic omission and ancestor decisions are permutation-safe and consistent with raw headers. | Existing list-based class/loader association and signature helpers; main's one-input class-byte copy with uncached connection; byte-sensitive artifact inputs; minimal TestKit launcher/classpath setup. | No parameter-consensus backport, Code toggle, worker DTO/redesign, or unrelated source replacement. |
| `I-parameters` | Compute both parameter-annotation visibilities/counts, names and flags from every original common method; retain every current defensive guard. | Keep the binary class-intersection interface; share metadata application between binary/all-original-method paths; generator original-method collection before unchanged Code restoration; one-input byte-copy and physically pinned TestKit support. | No N-way class/ancestor/signature rewrite, worker DTO/submission or parsed-node cache removal. |
| `I-worker-memory` | Finish one no-isolation action/task, sequential entries, plain parameters, bounded retention/header reads, exception-safe ownership and uncached JAR connections. | Existing worker changes; experimental-path opt-in, optional-version fallback, single-input passthrough and physically pinned TestKit; preserve Code/input tracking. | No ancestor/generic/parameter consensus backport, coroutines, per-class workers, weighted scheduler, global max-workers=1, BuildService cap, OOM stress test or unmeasured speedup promise. |

Exact commit file allowlists and item-sized steps/checks live only in [Waves](waves.md). Existing in-scope dirty changes belong in the intended branch commit; preserve them rather than committing only newly added hunks. Shared source files need selective edits, not full main-file imports. In-scope corrections needed for these stated behaviors stay within the listed files. A new functional requirement/file boundary requires reconciliation before proceeding.

Inheritance's lack of a Code toggle predates this scope. Require byte-exact one-input preservation and byte-sensitive inputs, but do not claim it implements main's optional multi-input Code feature. Parameters and worker-memory already have that feature and must retain passing Code tests.

## Dependencies And Serialization

There is no code dependency between target outcomes: each reads the same fixed main object and its own branch. The graph has one Wave, three independent items with empty item prerequisites and explicit mutual conflicts. One Handler runs at a time for its entire edit/verify/stage/sign sequence. Proposed order is inheritance, parameters, worker-memory; order alone is not a dependency or release predicate.

Exclusivity covers shared Git refs/configuration/hooks, Gradle daemons/caches, host memory/CPU, and signing-agent/pinentry interaction. Worktree indexes and local outputs are distinct. No separate audit, resource, test-only, staging-only, or commit-only delivery is created. Verification/signing belong to the branch's one observable outcome. An incomplete Result does not satisfy the Wave gate or authorize a successor by inference; the Router projects each Result independently and owns further release decisions.

## Verification Route

Keep `jvmToolchain(8)` unchanged. Run Gradle on the physical Java 21 launcher; production targets remain Java 8. Use the target's `classpath-api-stubs/verification/settings.gradle`, including only this real module build. Do not edit root settings to evade the refreshed missing `class-extensions/idea-plugin` failure.

Use this process-local PowerShell route, substituting the target and a distinct project-cache directory. Outer Gradle and TestKit fixtures use the physical distribution. The minimal allowed runner adaptation reads `STUBS_TEST_GRADLE_HOME` when supplied and otherwise preserves ordinary TestKit behavior.

```powershell
$env:JAVA_HOME = 'D:/Applications/Scoop/apps/temurin21-jdk/21.0.10-7.0'
$env:STUBS_TEST_GRADLE_HOME = 'D:/Applications/Scoop/apps/gradle/9.5.1'
$target = 'E:/Projects/cloche/jvm-multiplatform-inheritance'
$gradle = 'D:/Applications/Scoop/apps/gradle/9.5.1/bin/gradle.bat'
$common = @(
  '-p', "$target/classpath-api-stubs/verification",
  '--no-daemon', '--no-configuration-cache', '--console=plain', '--max-workers=2',
  '--project-cache-dir', "$env:TEMP/cloche-functional-sync-inheritance",
  '-Porg.gradle.java.installations.paths=D:/Applications/Scoop/apps/zulu8-jdk/8.92.0.21',
  '-Porg.gradle.java.installations.auto-download=false'
)
& $gradle @common ':classpath-api-stubs:test' '--tests' 'net.msrandom.stubs.CommonSuperClassFinderTest'
& $gradle @common ':classpath-api-stubs:check' '--rerun-tasks'
```

The first command illustrates one focused selector; Waves supplies every required selector. Check each exit code before proceeding. Use array elements so PowerShell does not parse dotted `-P` names. No `clean` or persistent environment changes. Gradle 9.5.1 `help` success only proves configuration, not tests or wrapper 9.2.0 equivalence. Record actual Gradle/JDK/toolchain paths and executed tasks/test counts. Dependency-cache/network or Java 8 discovery failure is a blocker, not a pass; no download/installation occurred for this Plan.

Each item runs focused tests, then isolated `:classpath-api-stubs:check --rerun-tasks` as its minimum full-module gate. Inheritance additionally compiles bounded representative Java/Kotlin consumers against its generic/raw-fallback fixtures, not arbitrary body-bearing stubs. Parameters/worker rerun existing Code/cache and signature parser tests. Worker overlap is verified inside one invocation with `--max-workers=2`, never concurrent branch builds.

## Signed Local Commit Strategy

In the same Handler, after every required check passes, inspect the complete diff and retained dirty changes. Ensure only its allowlist is staged and no pre-staged unrelated changes are consumed. Stage explicit paths, never `git add .` or `git add -A`. Unchanged allowed files need not be committed; changed paths outside the allowlist invalidate the gate until reconciled. No main/shared-package files belong in target commits.

Create exactly one ordinary local commit with explicit `git commit -S`, using the configured identity. No `--no-gpg-sign`, configuration changes, disabled hooks, pinentry changes or unsigned fallback. Record `git rev-parse HEAD`, `git show --format=fuller --stat HEAD`, `git diff-tree --no-commit-id --name-only -r HEAD`, `git verify-commit HEAD`, and residual status. Check intended branch, parent equals refreshed pre-commit HEAD, allowed paths and verified candidate bytes.

Verification failure means no staging/commit. Signing/pinentry blockage after validation leaves verified edits and any prepared in-scope index untouched; return `blocked` with successful checks, candidate identity and status, without a claimed delivery SHA. Do not erase validated work or call unsigned/unverified delivery complete. If hooks change bytes or signature verification fails after commit creation, report actual incomplete state; no reset/amend/extra commit is authorized. Revalidate any changed candidate.

## Risks And Completion

- Main's combined code/tests are not proof of full Spec compliance. Cover authoritative affected behavior with branch fixtures. Repair only inside the confirmed slice or report the gap; do not import another feature.
- Parameter decisions must use originals, not an intermediate result. Main's weaker guards cannot replace current defensive validation.
- Cached-JAR connections can couple task classloaders. Verify shared-input overlap, uncached reads and handle release/rerun after failure; actual parse/I/O failures cannot silently become missing evidence.
- Root configuration, unavailable caches/toolchain, physical-versus-wrapper Gradle versions and signing interaction may block execution. No environment-wide repair is authorized.
- Existing signatures/secret keys do not prove a future unlocked signing agent. Serial pinentry and honest incomplete status are mandatory.

Package readiness requires linked same-directory documents, current source/state evidence, closed selectors, and unique resolved acyclic graph IDs with explicit conflicts. Implementation completion is separate: `W-functional-sync` requires three actual completed Results with passing checks, allowed-file signed commits, full SHAs and truthful residual state. It is not currently met.

## Exact Selector

```yaml
admission:
  path: planned
  authority:
    work: E:/Projects/cloche/jvm-multiplatform/classpath-api-stubs/docs/works/api-stub-functional-branch-sync/work.md
    plan: E:/Projects/cloche/jvm-multiplatform/classpath-api-stubs/docs/works/api-stub-functional-branch-sync/plan.md
    waves: E:/Projects/cloche/jvm-multiplatform/classpath-api-stubs/docs/works/api-stub-functional-branch-sync/waves.md
```

Each future single-item Handoff references these three canonical paths exactly once, with processed Context and relevant authority/source/target references added separately. There is no `tasks` selector. Local structural validation is neither successful runtime admission nor user confirmation; both remain Router-owned.
