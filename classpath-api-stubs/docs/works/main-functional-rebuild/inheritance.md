# Inheritance Integration Evidence

[Work](work.md) | [Context](context.md) | [Plan](plan.md) | [Waves](waves.md)

This ordinary evidence record belongs to `I-inheritance-docs`. It consumes the completed `I-inheritance` code Result and records only that candidate's behavior and checks. The prepared separate docs-only commit is intended to follow the code candidate directly. Its signing command timed out after 60 seconds, so `I-inheritance-docs` is blocked: main remains unchanged and no docs commit SHA or signature verification exists. The five authorized docs remain staged. The wider Plan remains in progress. Parameters, worker-memory, FIR, actual/P4, plugin and final acceptance remain pending.

## Candidate and provenance

| Identity | Value |
| --- | --- |
| Completed inheritance code / docs parent | `d4dd6a46f9a5d9a20474bd3e2fcc8932c61eead2` |
| Code tree | `02220a80245bdd8faeca07099cb5421bd495c961` |
| Merge first parent, retained formatter commit | `991e57f0e05829d2c59f3ce93cd2710048f2a2d9` |
| Merge second parent, frozen inheritance source | `d05686ae9f3f2171a567e239bc347f10ac712f19` |
| Completed base/maintenance candidate | `d8c59ab668aa304797fc0870e5217a1edc97fa8b` |
| Exact rebuild base | `af8cf67d12dcb1c8fbca2b3d9c46a62b699a7d28` |
| Classpath version, applied once by inheritance | `0.1.12+terrarium.0.1.12` |

The full code Result is [result-1.json](C:/Users/WINDOWS/.pi/agent/sessions/--E--Projects-cloche--/2026-09-26T19-48-28-195Z_01a0df43-0ba3-7696-8006-8634ea05c989/tasks/retry-inheritance-merge-signing-artifacts/result-1.json). Its evidence directory is:

```text
C:/Users/WINDOWS/.pi/agent/sessions/--E--Projects-cloche--/2026-09-26T19-48-28-195Z_01a0df43-0ba3-7696-8006-8634ea05c989/tasks/retry-inheritance-merge-signing-artifacts
```

Artifact names below resolve within that directory. `candidate-audit.json` records merge parents, signatures, ancestry, all 40 input mappings, 29 frozen/recovery refs, source test blobs, original XML hashes and production class versions. `tested-inputs-before.json` records each input's index blob, raw SHA256 and length. `final-verification.json` records the same code SHA/tree after supplemental checks, with no input, ref or residual drift.

Before editing these docs, this docs execution independently confirmed local main/HEAD and tree equal that completed candidate; all 40 inputs still have the recorded candidate/index blobs, raw SHA256 and lengths. Index was empty, unmerged count was zero and `MERGE_HEAD` was absent. All 23 preexisting residual files matched the code Result, including the four then-untracked tuple files. The 29 frozen/recovery refs, both dirty inheritance source-test blobs and all 18 XML lengths/hashes were unchanged. Builds are not rerun by this docs outcome; the exact input mapping supports reuse of the existing evidence. The prepared/staged diff was independently confirmed to contain only the five authorized ordinary documents. After the signing timeout, the blocked Result records retained index state and rechecks the unchanged code inputs.

## Integrated behavior and retained baseline

The completed code Result establishes N-way inheritance from every original contributing input and its own classpath. Current-entry full nodes supply intersection/Code evidence; ancestor lookup uses headers. Two- and three-input permutations make consistent common-ancestor decisions. Missing parent bytes permit conservative `java/lang/Object` fallback; corrupt classes, I/O errors and cycles fail generation with resource release. Object terminates the ancestry walk.

Raw superclass/interface names and surviving generic Signatures agree. Interfaces match by full internal name; formal variables, bounds, scopes and erasures must be compatible across inputs. Unproved or malformed generic relationships cause omission of the whole affected Signature. When class Signature is omitted, member Signatures are checked for dangling class variables. Java and Kotlin consumers compiled successfully for the targeted generic and raw fallback fixtures; this is bounded fixture evidence, not a runtime guarantee for all generated jars.

Base `preserveMethodBodies=false`, full-byte input tracking and all-original `CodeMethodNode` body comparison are retained. Multi-input body preservation compares executable operands/control flow and ordered handlers, ignores debug/frame differences, and conservatively omits unsupported Code metadata. Matching and mismatching three-input bodies, constructors/initializers, abstract/native declarations and option/cache behavior have existing checks. One-input generation copies the original class bytes for either option. Uncached streams and exception-safe closure remain in the shared generator.

`scoped-diff.json` records 11 classpath paths relative to the completed base/maintenance candidate: two build/version files, five production files and four tests, with 839 insertions and 318 deletions. The earlier `991e57f` commit retained the three main formatter deltas; the two source-checkout test formatting deltas match their recovery and candidate blobs. Signing retry and supplemental checks introduced no new source/build/version change.

The committed actual compiler tree still equals the rebuild base. `MethodBodies.kt`, `CodeMethodNode.kt` and `MethodBodiesTest.kt` also equal that base, as recorded by `scoped-diff.json` and the code Result. This docs execution rechecked the actual compiler tree and MethodBodies implementation/test comparison without changing them. The unstaged actual build formatter is separate residual work; actual/P4 integration and its version are not completed here. The Code and actual Specs retain their historical baseline/status wording and have not been upgraded to implementation evidence.

## Executed tests consumed from the code Result

All rows below are previously executed code checks. This docs execution ran no Gradle task, compiler or test.

| Evidence | JUnit cases / assertions | Result |
| --- | --- | --- |
| Signing retry: `CommonSuperClassFinderTest` | 2 JUnit cases | failures/errors/skipped = 0 |
| Signing retry: `signature.SignatureIntersectionTest` | 5 JUnit cases | failures/errors/skipped = 0 |
| Signing retry: `signature.ParsingTest` | 1 JUnit case | failures/errors/skipped = 0 |
| Signing retry: `MethodBodiesTest` | 4 JUnit cases | failures/errors/skipped = 0 |
| Signing retry: `PreserveMethodBodiesTest` | 1 JUnit case | failures/errors/skipped = 0 |
| Earlier unchanged candidate: `InheritanceGenerationTest`, reused | 3 JUnit cases | failures/errors/skipped = 0 |
| Repository-external `verifySingleInputBytes` fixture | 2 complete class-byte assertions | true and false outputs both equal the input |

There are 16 JUnit cases in the evidence set: 13 freshly executed by the signing retry plus 3 reused from the earlier unchanged candidate. They are not one full-suite invocation; the two external bytes assertions are additional checks, not JUnit cases. The earlier successful five-class run had no retained case total and is not counted again. Its XML was replaced by the later selector run, so the retry reran only those five classes to restore exact reports. No full `:classpath-api-stubs:check` or worker/parameter acceptance is established.

`focused-invocation.json`, `focused-tests.log` and `focused-test-reports.json` retain the five-class command, exit 0, elapsed 47.873 seconds and all 13 cases' XML/hashes. The actual output includes:

```text
BUILD SUCCESSFUL in 47s
7 actionable tasks: 1 executed, 6 up-to-date
```

The actual launcher/arguments were:

```text
D:/Applications/Scoop/apps/gradle/9.5.1/bin/gradle.bat -p E:/Projects/cloche/jvm-multiplatform/classpath-api-stubs/verification :classpath-api-stubs:test --tests net.msrandom.stubs.CommonSuperClassFinderTest --tests net.msrandom.stubs.signature.SignatureIntersectionTest --tests net.msrandom.stubs.signature.ParsingTest --tests net.msrandom.stubs.MethodBodiesTest --tests net.msrandom.stubs.PreserveMethodBodiesTest -Dorg.gradle.java.installations.paths=D:/Applications/Scoop/apps/zulu8-jdk/current --offline --no-daemon --no-configuration-cache --console=plain --max-workers=2
```

In PowerShell the script quotes each argument, including `-Dorg.gradle.java.installations.paths=...`; use the retained `verify-focused.ps1` for the exact invocation. It selects process-local `JAVA_HOME=D:/Applications/Scoop/apps/temurin21-jdk/current` and `GRADLE_USER_HOME=C:/Users/WINDOWS/.gradle`, and removes the process's `STUBS_TEST_GRADLE_HOME` for this focused route.

`prior-test-evidence.json` retains the earlier successful five-class output (`BUILD SUCCESSFUL in 3m 20s`, exit 0), followed by the successful `InheritanceGenerationTest` selector (`BUILD SUCCESSFUL in 1m 2s`, exit 0). The latter uses the same launcher/common flags plus process-local `STUBS_TEST_GRADLE_HOME=D:/Applications/Scoop/apps/gradle/9.5.1` and `STUBS_TEST_JAVA_HOME=D:/Applications/Scoop/apps/temurin21-jdk/current`. Its original three-case XML SHA256 is `251BC343E2E03378671C58C5D092F12FB0BC231CEBD9CB204D1EB0D319A2C0C1`. That saved XML includes all testcase names and four consumer compiler invocations, each exiting 0: Java generic/raw with `--release 8`, and Kotlin generic/raw with `-jvm-target 1.8`. The mapping confirms the last source edit preceded the successful checks, the signed merge tree equals the tested staged tree, and no tested inputs changed afterward.

`single-input-invocation.json`, `single-input-bytes.log` and `single-input-fixture/build.gradle` retain the external supplement. It uses the same physical launcher and common flags with `-p <evidence-directory>/single-input-fixture verifySingleInputBytes`, reading candidate plugin-under-test metadata and compiled classes/ASM dependencies. It exits 0 in 6.884 seconds without editing repository code/build files. Actual output:

```text
PASS singleOn preserveMethodBodies=true length=148 SHA256=fc1e454cf7171cb9cc17fede12a49df7c00b1cf7e165ca712ab06c90cf9b9b0d
PASS singleOff preserveMethodBodies=false length=148 SHA256=fc1e454cf7171cb9cc17fede12a49df7c00b1cf7e165ca712ab06c90cf9b9b0d
BUILD SUCCESSFUL in 6s
```

Coverage described by the code Result includes all-input ancestry, missing/corrupt/cyclic/I/O cases and handle release, raw/Signature consistency, formal scopes/bounds/erasure, recursive bounds, dangling variables, real Java/Kotlin consumers, and preserved Code/cache behavior including body-only and excluded-copy changes, UP-TO-DATE, FROM_CACHE and option switching. The extra fixture closes the exact bytes assertion gap for both one-input option values; it does not represent a new code implementation.

## Toolchains, version and historical limitations

The passing route is the isolated `classpath-api-stubs/verification/settings.gradle` module using physical Gradle 9.5.1 and Eclipse Adoptium Java 21.0.10. Java 8 discovery uses Azul `1.8.0_482`; production remains `jvmToolchain(8)`, with recorded class major versions `[52]`. The targeted consumer Kotlin CLI/stdlib comes from Gradle 9.5.1's Kotlin 2.3.20 distribution. Runtime Java 21, production Java 8 and this consumer compiler are separate facts. They do not prove wrapper Gradle 9.2.0/Kotlin 2.2.20, actual compiler acceptance or product P4's required Gradle 9.4.0 route.

The version source is candidate `classpath-api-stubs/gradle.properties`, whose only version edit is `version=0.1.12+terrarium.0.1.12`; `scoped-diff.json` retains the original `0.1.12` comparison. No other module properties or fixture dependency versions changed in inheritance or these docs. FIR `1.0.11+terrarium.1.0.11` and actual `0.1.2+terrarium.0.1.2` remain owning-item targets, not already applied feature results.

Historical boundaries are retained:

- The original compound inheritance Handoff was rejected as `blocked` before edits/tests/commits. The graph then separated code and documentation Results.
- A later code attempt produced the tested candidate but was blocked by GPG/pinentry signing timeouts. The original transcript records merge signing waits of 60/90/45 seconds and a 30-second batch probe without process exit codes. The successful retry preserved that index and executed `git commit -S --no-edit` once: exit 0, 0.427 seconds, no timeout, global configuration change, unsigned fallback or hooks bypass.
- `git verify-commit` for both `d4dd6a46` and `991e57f0` returned 0. This docs execution independently reran both signature checks. The actual verification output includes `using EDDSA key 3B992602764F685EE1DD065D4FFAF1C206BF8586` and `Good signature from "settingdust (UwU) <settingdust@gmail.com>" [ultimate]`.
- Six earlier Gradle attempts failed with exit 1: root offline KSP `2.3.20-+` resolution; two Scoop Gradle cache-lock hashing attempts; Java 8 discovery; a misquoted PowerShell `-D` parameter; and wrapper 9.2 isolated Kotlin DSL compilation with unresolved `plugins`, `kotlin-dsl`, `java-gradle-plugin`, `maven-publish`. Commands/output remain in `prior-failure-evidence.json`. Those failures are not root/wrapper passes and were not rerun by the docs execution.
- The failed `MERGE_HEAD` query after the successful code commit returned 1 because the merge had finished; the commit command itself returned 0.

## Original docs signing attempt

After explicit staging, the prepared docs tree was `ea5b78cd99e9ba30844823910944081ef81cf8aa`; cached diff check passed and all staged doc blobs matched the reviewed files. No hooks configuration or active hook script was found. This execution attempted `git -C E:/Projects/cloche/jvm-multiplatform commit -S -m 'docs(stubs): record inheritance verification'` under the existing configuration. The tool reported `Command timed out after 60 seconds`, without a commit process exit code or successful signature output. Immediate read-only inspection confirmed HEAD/tree still equal the code candidate, all five docs staged, no unmerged entries or MERGE_HEAD, and the same prepared index tree. Only the original lock remained. The observed GPG agent and residual pinentry process were retained; no unsigned fallback, configuration change, hooks bypass, signing retry or process cleanup followed.

The completion statements were corrected to `blocked` in the retained documents; their final staged tree and exact residual/process state are supplied by this Result. The code Result remains completed, while the docs predicate and containing inheritance gate remain unfulfilled.

## Authorized docs signing retry

This retry first consumed both full previous Results and independently checked the exact retained five-document staged tree `5a12cc99fbb63a6bb8a4e29315b6da427173dce4`. Every staged document blob equalled its raw file and the original blocked Result. Main/HEAD was still the completed code candidate, index contained only those five additions, cached diff check returned 0 and `MERGE_HEAD` was absent. All 40 tested inputs matched their recorded HEAD/index blobs, raw SHA256 and lengths; all 29 frozen/recovery refs, both inheritance source-test blobs, actual formatter and 18 original XML lengths/hashes were unchanged. Both code signatures independently returned 0. The five current focused XML hashes matched the saved reports (13 passing JUnit cases); the saved unchanged-tree inheritance XML had three passing cases, and the recorded two-option bytes invocation/log still showed exit 0 and both exact-byte assertions. No test or build was rerun.

Before signing, the only relevant process was the existing gpg-agent PID 72296; no active Git/GPG signing/pinentry/Java/Gradle process or configured hook was found. Exactly one authorized retry invoked `git -C E:/Projects/cloche/jvm-multiplatform commit -S -m 'docs(stubs): record inheritance verification'`, using the existing configuration and a 300-second execution limit. The runner returned background terminal `bt-8c1014a94aaba6a2-1` (launch PID 71740) after one second with empty stdout/stderr. No terminal completion report was delivered during this execution, so a retry exit code, elapsed completion time and tool-reported timeout classification are unavailable. After allowing the full execution budget, independent inspection found no Git/GPG signing process, no new docs commit and no HEAD/tree/index movement. The observed gpg-agent PID 72296 and pinentry PID 69708 were retained; available ancestry did not conclusively establish pinentry ownership, so no process was terminated.

The signed docs predicate remains unmet. The expected direct parent would still be `d4dd6a46f9a5d9a20474bd3e2fcc8932c61eead2`, but no actual docs parent, SHA or signature can be claimed. These five ordinary docs were updated only to record the original 60-second timeout and this unsuccessful, incompletely reported retry accurately. At the pre-staging check, a newly observed signing chain comprised Git PIDs 38820/73928/67332 and GPG PID 48012, created at 15:01:25/26 local time under PowerShell PID 58584. Its observed ancestry did not establish ownership by this attempt; it was not terminated or waited for. Further Git mutations stopped. The original five docs remain staged at tree `5a12cc99fbb63a6bb8a4e29315b6da427173dce4`, with these retry updates left unstaged. The final index/worktree blobs and exact residual state belong to this retry's Result. No second signing retry, unsigned fallback, global configuration change, hook bypass, signature amendment, code-history edit or downstream execution occurred.

## Preservation and remaining scope

`candidate-audit.json` and `refs-before.txt` retain all 29 related refs: 19 original/base/upstream recovery pins, six recovery payload branches, three frozen feature branches and the base-maintenance branch. This docs execution resolved them unchanged before writing. Their prior preservation signatures are inherited evidence; preservation/reset are not repeated. Old combined integration `1bf7076`, original main `cd623a9` and the inheritance recovery commit `b4144d9` were absent from the code candidate's ancestry, while the exact base and frozen inheritance tip were ancestors.

Before this docs commit, main had 23 residual paths: the actual build formatter, four untracked tuple files and 18 untracked generated XML files. Only the four tuple files plus this new `inheritance.md` are staged for the docs commit, which was not created. Besides those five staged additions, the blocked Result must verify the same 19 unrelated residual paths: unstaged `kmp-actual-stubs/compiler-plugin/build.gradle.kts` (23 additions/4 deletions, SHA256 `AC56A695791F4489A1B82EE2061667C842E12B7340BE774B0529E046550F4334`) and the 18 XML files inventoried in `candidate-audit.json` / `residual-before.json`, each still matching its preservation length/SHA256. No local XML exclusion is added. Existing `info/exclude` SHA256 is `6671FE83B7A07C8932EE89164D1F2793B2318058EB8B98DC5C06EE0A5A3B0EC1`. The preexisting `.git/next-index-264852.lock` (27760 bytes) is retained.

The inheritance source checkout still has exactly its two captured dirty tests (`CommonSuperClassFinderTest.kt` and `signature/SignatureIntersectionTest.kt`), whose raw blobs match both recovery and main candidate. Parameters and worker checkouts remain clean at their frozen tips. Recovery/source branches are retained. No reset, clean, archive, Python, remote operation or publication is part of this docs outcome.

`I-inheritance` is completed, but the signed/verified `I-inheritance-docs` Result required by `W-inheritance` is still missing because signing is blocked. The gate remains unreleased; the Router owns dependency release. No later item is executed here. The remaining graph retains parameter code/docs, worker code/docs and historical branch-sync reconciliation, FIR code/docs, actual/P4 code/docs, plugin code/docs and independent final read-only acceptance with the full clean/version/preservation predicate. Recovery documentation `489de7331a62de12cbcf14063883e48a380aa7f7` and existing base docs remain intact; old branch-sync main-into-branches history is not promoted to current rebuild completion.
