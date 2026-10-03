# Actual Integration Evidence

[Work](work.md) | [Plan](plan.md) | [Context](context.md) | [Waves](waves.md)

`I-actual` is completed on signed local main merge `dc80898587c4cd9e098993901a74213c9e9acbda`. This ordinary record consumes the completed restoration, product rerun and signed-integration evidence; no build, test or network diagnostic ran during this documentation delivery. `I-actual-docs` completes and `W-actual` becomes releasable only when the containing five-path docs-only commit is signed, independently verified and reported as completed with that merge as its direct parent. The new documentation SHA belongs in its Handler Result, not in its own content. `I-plugin`, `I-plugin-docs` and `I-final` remain pending; the overall Plan is in progress.

## Candidate, recovery and retained production

| Identity | Value |
| --- | --- |
| Pre-Actual main / feature parent | `c6b830170d5a83a30c8def721c65f4707f8abc30` |
| Signed feature / retained branch | `b153670cd2e5bb6267aedd3f696aecd0820373f9` / `fix/actual-main-rebuild-20261004-01` |
| Signed main non-fast-forward merge | `dc80898587c4cd9e098993901a74213c9e9acbda` |
| Merge parents, in order | `c6b830170d5a83a30c8def721c65f4707f8abc30`, `b153670cd2e5bb6267aedd3f696aecd0820373f9` |
| Tested staged candidate, feature and merge tree | `4b01177cc33d70d636c69b96b186e0acea2d8eb8` |
| Actual recovery / its parent | `85a49556a12cc23467f0fbb3815bef1a5e7bb30f` / `cd623a95a75f07639f35a2bbf1a36f1926605551` |
| Recovery ref | `refs/heads/recovery/i-preserve-20261003-01/actual` |
| Owning version | `0.1.2+terrarium.0.1.2` |
| Retained classpath / FIR versions | `0.1.12+terrarium.0.1.12` / `1.0.11+terrarium.1.0.11` |

Actual production `kmp-actual-stubs/compiler-plugin/src/main` has no diff from rebuild base `af8cf67d12dcb1c8fbca2b3d9c46a62b699a7d28`. This delivery restores build/test/P4 capability and applies its owning version; it does not implement new Actual production behavior. Fourteen paths were transplanted from the recovery delta; the owning properties file is the fifteenth path. Old combined commit `1bf7076716bb7cb0291259c81003dad971d16a94` remains absent from rebuilt-main ancestry. Recovery ancestry was not merged into main.

The exact 15-path code diff (`675 insertions / 106 deletions`) is recorded in [owned-paths.txt](C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/git-integration-state/owned-paths.txt): module build and properties, `Tests.kt`, `P4AcceptanceTest.kt`, `P4ProvenanceRegistrar.kt`, observer service registration, two Cloche fixtures, three consumer build/settings/observer fixtures and four consumer Kotlin fixtures. All 15 are covered by the 44 tested inputs. [owned-mapping-final.json](C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/git-integration-state/owned-mapping-final.json) correlates physical SHA256, raw/normalized Git blobs, feature/merge blobs and recovery sources. [payload-mapping.json](C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/payload-mapping.json) retains the original 14-path mapping, including EOL/final-newline differences; recovery blobs and physical bytes are not universally identical.

The recovered build includes the three existing multiline `systemProperty` formatter blocks and final newline. Its raw, normalized and recovery blob is `c226d7195011142bf56209652b9d91d4d72df0eb`; physical SHA256 is `613A25ABDB984285BDCD2AF9B44B80D4D04140067B6915FD8C2451B51B4DBEC7`. The earlier formatter input SHA256 `AC56A695791F4489A1B82EE2061667C842E12B7340BE774B0529E046550F4334` identifies historical input, not a remaining unstaged diff.

The consumed integration ran each signed operation once, under a 300-second limit, with exit 0: `git commit -S -m "test(actual): retain smoke and real P4 replay"`, then `git merge --no-ff -S fix/actual-main-rebuild-20261004-01 -m "Merge Actual P4 integration"`. Its temporary linked main worktree was normally removed before returning to original main with unchanged tested physical bytes. Both code signatures independently passed again at docs entry:

```text
git verify-commit b153670cd2e5bb6267aedd3f696aecd0820373f9  # exit 0
git verify-commit dc80898587c4cd9e098993901a74213c9e9acbda  # exit 0
gpg: using EDDSA key 3B992602764F685EE1DD065D4FFAF1C206BF8586
gpg: Good signature from "settingdust (UwU) <settingdust@gmail.com>" [ultimate]
```

## Consumed Results and artifact identities

All three generation-1 Result files were read in full. Their directory is:

```text
C:/Users/WINDOWS/.pi/agent/sessions/--E--Projects-cloche--/2026-09-26T19-48-28-195Z_01a0df43-0ba3-7696-8006-8634ea05c989/tasks/
```

| Result file under that directory | SHA256 | Interpretation |
| --- | --- | --- |
| `actual-tested-candidate-signing-integration-artifacts/result-1.json` | `3B5BD43E9E8CAA366ACA09FDEF043BFEDCF076C02148F1AF94F6D9A972713390` | Completed signed feature/main integration of the unchanged passing candidate. |
| `actual-p4-verification-retry-artifacts/result-1.json` | `3A6A24CEBF45A66FCFB8A4FDA66CF0A8FE8876AA1B5EE31B9FDA2EA23FA47307` | Completed genuine product P4 rerun with fresh callable execution. |
| `actual-p4-code-integration-main-rebuild-artifacts/result-1.json` | `3946A5942F95FA900A8DCDCB80EF99A26873F0DF79049D81B1081A15936E8975` | Inner Result was historical `blocked`: restoration/smoke/callable passed; initial P4 failed dependency resolution and no code commit existed yet. Its runtime envelope says completed, which does not make that old inner Result passing. Later Results supersede its open P4/signing state. |

Evidence root: `C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/`. [Signed integration evidence](C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/git-integration-state/final-verification.json) includes command/exits/signatures, exact 15-path mapping, 44 raw/normalized tested inputs and final ref/source/residual comparison. [Product rerun summary](C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/p4-rerun-state/verification-summary.json) points to saved product commands, provenance, task inputs, bytecode and negative diagnostics. These are local evidence files, not published artifacts; their availability depends on retaining the recorded paths.

## Distinct passing executions

These are consumed historical code executions, independently parsed from saved XML and exit records during docs preparation. Each row has zero failures, errors and skips.

| Execution | Cases | XML relative to evidence root | XML SHA256 |
| --- | --- | --- | --- |
| Smoke, original successful route | 1 | `smoke-xml/TEST-net.msrandom.stub.tests.Tests.xml` | `ABA5C9E55DC3D6E0AC3AD9D46B47B42C6AFA53E6635F383D056985DBA663A94D` |
| Ordinary callable, same original route | 31 | `smoke-xml/TEST-net.msrandom.stub.tests.CallableMatchingTest.xml` | `F1561D27E26895E86B1E74A8E7C99C08CE55BBDF030A7C74AFE2A935C4585E62` |
| Later genuine product P4 rerun | 1 | `p4-rerun-state/p4-xml/TEST-net.msrandom.stub.tests.P4AcceptanceTest.xml` | `C0535C28F316F7B1498837F2F90CEE6968EA96C0E913B1834F5C7C7359A0CA8A` |
| Fresh callable prerequisite of that P4 rerun | 31 | `p4-rerun-state/callable/xml/TEST-net.msrandom.stub.tests.CallableMatchingTest.xml` | `91D3060F175594DE2ABD49ED1B0A939BCFAA749CFA9D56A26D354A9DBB3CC31C` |

The original successful route ended at `2026-10-03T16:57:01.3216821Z`, outer exit 0. Smoke asserts compiler OK, six expected classes and `expectFunction` throwing `UnsupportedOperationException("Common modules are not runnable")`. The later P4 ran from `2026-10-03T17:19:32.4398967Z` to `2026-10-03T17:24:43.5951520Z`, outer exit 0. Its callable prerequisite reran 31 cases and saved 168 compilation logs and method reports; callable success was not substituted for product P4.

Saved entry commands (not executed by this docs Handler):

```powershell
& pwsh -NoProfile -File 'C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/verify.ps1' -Route smoke
& pwsh -NoProfile -File 'C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/p4-rerun-state/verify.ps1' -Route p4
```

The rerun script is a byte-identical copy of the original script, using its existing script-root behavior to separate output. Exact expanded argv, process-local environment and timestamps are in [smoke-invocation.json](C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/smoke-invocation.json) and [p4-invocation.json](C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/p4-rerun-state/p4-invocation.json); corresponding `*-exit.json` files record exit 0. Smoke explicitly selects the annotations jar before actual test. P4 also explicitly selects VSS/class annotations jars and uses `-Pp4Replay=true --tests net.msrandom.stub.tests.P4AcceptanceTest` with declared callable/observer dependencies. Outer commands use offline/no-daemon/no-configuration-cache/plain output, worker limit 2 and rerun-tasks; product fixture dependency resolution follows the original online test route.

## A1-A15 compiler evidence

The mapping consumes the restoration Result's coverage groups and the later callable XML plus [saved cases](C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/p4-rerun-state/callable/cases). Each case saves `compile.log`, `methods.txt` and invocation/artifact evidence. Native controls preserve compiler diagnostics; expected compilation errors are passing negative assertions, not successful output artifacts. The owning [Actual Spec](../../../../kmp-actual-stubs/compiler-plugin/docs/spec/preserve-actual-matching.md) remains read-only; its old pending status is not current execution evidence.

| Obligation | Observed coverage / representative saved case group |
| --- | --- |
| A1 | Compatible actual body, intended descriptor and caller owner: `mixed-*`, `overload-source-*`. |
| A2 | Missing function/property/accessor shapes and throwing stubs: `properties`, `property-variants-*`. |
| A3 | Real actual plus independent missing stub in one invocation: `mixed-*`. |
| A4 | Orphan function/property/class/typealias diagnostics: `negative-orphan-*`, `negative-orphanProperty-*`, `p3-class-orphan-*`, `p3-alias-orphan`. |
| A5 | Parameter/receiver/generic/return/mutability incompatibility with native controls, including inferred return: `negative-parameter-*`, `negative-receiver-*`, `negative-generic-*`, `negative-return-*`, `negative-mutability-*`, `negative-inferred-*`. |
| A6 | Same-name and receiver overloads, one matched and one missing, owner distinction: `overload-source-*`, `property-overload-*`, `owner-*`. |
| A7 | Direct/transitive refinement with actuals at different levels and missing stubs: `p3-chain-*`. |
| A8 | Excluded invocation input and sibling orphan: `p3-excluded`, `p3-sibling-*`. |
| A9 | Duplicate/diamond compiler errors: `p3-duplicate-*`, `p3-diamond-*`, `p3-diamond-class-*`. |
| A10 | Missing class/member/nested/interface/object/annotation shape and stubs: `p3-missing-class`. |
| A11 | Matched class/member bodies and missing/incompatible member failures with native controls: `p3-matched-class`, `p3-member-*`. |
| A12 | Missing/matched enum and invalid entries/members: `p3-enum-*`. |
| A13 | Matched actual typealias, invalid target/member and orphan: `p3-alias-*`. |
| A14 | Reordered inputs, directory/basename independence, multiline/backtick/CRLF and irrelevant text: `multiline-*`, `crlf-*`, alias file variants. |
| A15 | Ordinary declarations, explicit Stub ownership and unrelated source errors: `p3-class-ordinary`, `marker-ownership`, `negative-sourceError-*`. |

## Product P4, provenance and runtime distinctions

[Product evidence](C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/p4-rerun-state/product) saves each exact `*.command.txt`, `*.exit`, log, task-input snapshot, method report and bytecode snapshot. `build-cloche` and `product-provenance` exit 0. The five compile rounds also exit 0 and assert real actual bodies, missing throwing stubs and added/removed actual behavior:

| Round | Verified body / added actual | Saved assertion |
| --- | --- | --- |
| clean | `73 / false` | `clean-verified.txt` |
| change | `75 / false` | `change-verified.txt` |
| add | `75 / true` | `add-verified.txt` |
| remove | `75 / false` | `remove-verified.txt` |
| final-restored | `75 / false` | `final-restored-verified.txt` |

`orphan-native` and `inferred-native` exit 1 as expected negative controls. Their logs preserve `has no corresponding expected declaration` and `declarations are incompatible`, respectively. The P4 test asserts these failures; they are not a failed P4 run.

| Setting | Actual evidence |
| --- | --- |
| Outer physical Gradle | 9.2.0, recorded in outer invocation launcher; Java 21.0.10 runtime. |
| Nested product Gradle / JVM | `P4_GRADLE=9.4.0 JVM=21.0.10` in `product-provenance.log`. The launcher's ancestor directory named `9.5.1` is not the product runtime. |
| Explicit compiler / language / API | `2.2.20 / 2.1 / 2.1`, consistent in callable, compiler-side `P4_PROVENANCE` and product task inputs. Product Gradle's embedded Kotlin is a separate setting. |
| Callable/compiler JVM | Azul Java `1.8.0_482`. |
| Production target | Java 8; audited production classes have major 52. |
| Product fixture target | `jvmTarget=17`, with its separate Java 17 toolchain; task inputs record `incremental=true`, `strategy=DAEMON`, source lists, fragment/refinement arguments and plugin hashes. |

Actual plugin version `0.1.2+terrarium.0.1.2` has SHA256:

```text
1a8eb23062165878fe1726759d6962fcaa8db4d51f7a3550bb9984cc0b94c939
```

The saved/current jar, callable provenance, compiler-side observer, product task inputs and [artifact.txt](C:/Users/WINDOWS/AppData/Local/Temp/i-actual-20261004-01/p4-rerun-state/product/artifact.txt) identify this same plugin. The consumer receives the local `stubJar` file; unrelated fixture dependency coordinates remain unchanged. This is local copied-product acceptance, not Maven published-consumer acceptance. The original sibling Cloche checkout at `84e31bd939685c296afea85c46aae2c7ae18a394` is a read-only source; product writes are confined to the copied fixture under the actual module's build directory.

## Docs candidate check, preserved state and limitations

At docs entry, main/HEAD equals the exact signed merge, its tree/parents match integration evidence, and index/tracked diff are empty. All 44 tested inputs match physical SHA256/length/raw blob/normalized blob and committed candidate blobs; original smoke/P4 and later rerun manifests each match all 44. All 15 code paths map to those inputs. All 291 non-index entries in the rerun's 292-path snapshot match before documentation edits. The index legitimately changed through code commits, so historical physical index SHA256 equality is not claimed. These docs change four snapshot entries; later comparison excludes only those owned documents and the index, leaving 287 unchanged snapshot entries.

All 515 source-snapshot files, four source checkout HEAD/status records, 46 refs and 14 version-input records match at docs entry. The inherited two inheritance formatting diffs remain byte-identical; parameters, worker and Cloche remain clean. Only local main may move for the documentation commit. The Actual feature ref stays at `b153670`; every other recovery/frozen/remote ref stays unchanged. No conflicting Git/build/signing process or active operation was observed; only the idle existing GPG agent was present. No hook manager configuration or active hook file was found.

The only porcelain residuals are the original 18 generated `.intellijPlatform/localPlatformArtifacts/*.xml` files, still physically present, untracked and byte-identical. Their exact paths/lengths/hashes are recorded in the documentation Result's `xml-before.json` and final comparison. No exclusion was added. `.git/info/exclude` is the original comments-only 240-byte file, SHA256 `6671FE83B7A07C8932EE89164D1F2793B2318058EB8B98DC5C06EE0A5A3B0EC1`; `.git/next-index-264852.lock` is the retained 27,760-byte inherited file, SHA256 `C2F6980661CFB072A13CC9912ED76CC144A3A82B979BF461FD973A2543F9716B`. Neither is a new active operation. Evidence and candidate comparisons for this delivery are under `C:/Users/WINDOWS/AppData/Local/Temp/i-actual-docs-20261004-01/`.

The initial P4 NeoForged/srgutils connection refusal remains historical: product `recordP4Classpath` failed resolving `net.minecraftforge:srgutils:latest.release`, and the saved old P4 XML had one failure. The later unchanged original-command rerun passed; this does not diagnose the earlier network failure. Standard root actual-test selection previously failed configure-on-demand annotations-jar configuration, and disabling configure-on-demand reached unavailable offline IDEA metadata. The explicitly selected jar route passed, establishing this bounded Actual result without establishing a complete root check. Earlier classpath/FIR route limitations retain their own historical scope.

This documentation delivery writes only `actual.md`, `work.md`, `context.md`, `plan.md` and `waves.md`. Its completion requires a docs-only staged diff, one signed commit with direct parent `dc808985`, signature/path verification, clean tracked/index state and preserved source/ref/XML/exclude/lock evidence. It performs no source/build/version/authority edit, test/build rerun, dependency/cache/global configuration change, plugin/final execution, publication, tag or push. Overall Plan completion remains unestablished.
