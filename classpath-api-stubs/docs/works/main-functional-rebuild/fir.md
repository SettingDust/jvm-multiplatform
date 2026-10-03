# FIR Integration Evidence

[Work](work.md) | [Plan](plan.md) | [Context](context.md) | [Waves](waves.md)

This ordinary evidence record is the recoverable documentation candidate for `I-fir-docs`. It consumes the completed `I-fir` code Result and records the exact signed candidate, captured FIR mapping, fresh compiler/runtime fixture check and remaining boundary. The code and documentation outcomes are separate. `W-fir` remains pending until this candidate is signed; actual/P4, plugin and final acceptance remain pending.

## Candidate and provenance

| Identity | Value |
| --- | --- |
| Recovery payload | `refs/heads/recovery/i-preserve-20261003-01/fir` at `45f010c168ef023e7983bf82800eb79475c2adf7` |
| Signed FIR code commit | `ff007244fe97cfc88223c525520df5debf3bc681` |
| Code commit tree / parent | `af5df416c9f06694aa1fd03c28e0770c9f0368e8` / `1bf3c3baa5e87db805929dba1f4d0e04cf4d8b5a` |
| Signed main merge | `3c0653b23f7e6bf190a4c18e715253e0c4b57f59` |
| Main merge tree / parents | `af5df416c9f06694aa1fd03c28e0770c9f0368e8` / `1bf3c3baa5e87db805929dba1f4d0e04cf4d8b5a` + `ff007244fe97cfc88223c525520df5debf3bc681` |
| Code signatures | `git verify-commit` exit 0 for `ff007244` and `3c0653b`; Good EDDSA from `3B992602764F685EE1DD065D4FFAF1C206BF8586` |
| Kotlin plugin version | `1.0.11+terrarium.1.0.11` |
| Old combined integration | `1bf7076716bb7cb0291259c81003dad971d16a94` is not an ancestor of the main candidate |

The code candidate is the exact signed non-fast-forward merge now at local `main`. Its code diff contains exactly 12 FIR-owned paths: `class-extensions/kotlin-plugin/gradle.properties`, eight FIR production files (`ClassExtensionFirDeclarations`, `FirClassExtensionResolver`, `GeneratedDeclarationResolver`, `LazyExtensionFinder`, `SymbolCopyProvider`, `copiers/ConstructorCopier`, `copiers/FunctionCopier` and `copiers/PropertyCopier`), `CodegenTest.kt`, and the `test-common.kt` and `test-platform.kt` fixtures. No actual compiler formatter or other downstream path entered either code commit.

The payload mapping in `completed-payload-mapping.json` correlates the 11 recovered source/test/fixture paths with their final candidate blobs; the owning properties file is the separate twelfth code-diff path with the accepted version. Ten mapped paths are equal after line-ending normalization. `LazyExtensionFinder.kt` has the required in-scope semantic adaptation: source text normalizes CRLF and CR to LF before Kotlin light-tree parsing. The candidate hash is `3450151678CD899CA01E7590BCF004F132C0E8EC0C34A9FCD0288D21D9CDE2D5`. The other payload differences are inherited EOL normalization only, with their final physical hashes recorded in `completed-hashes.json`.

The complete consumed code Result is:

```text
C:/Users/WINDOWS/.pi/agent/sessions/--E--Projects-cloche--/2026-09-26T19-48-28-195Z_01a0df43-0ba3-7696-8006-8634ea05c989/tasks/fir-integration-cache-warm-retry-artifacts/result-1.json
```

Its SHA256 is `C1BF8AF37F11F1B54428C4A07B18020D7CD35960C9D78E20DB467383E7C347E5`. The linked artifact directory is `C:/Users/WINDOWS/AppData/Local/Temp/i-fir-20261004-01/`. Evidence artifact hashes used for this reconciliation are:

| Artifact | SHA256 |
| --- | --- |
| `completed-hashes.json` | `B8C9361D3AF0FCC7561AB7C20B06C40D4197C8A8C86718C3BA66C492C40AEA5D` |
| `completed-payload-mapping.json` | `4E8349348039963D30600012F73219AAAA0DFAE28C003AFA5202BEF5FB18A0C0` |
| `completed-state.json` | `588A2E84D8847D56BD791877B008F64EA5AD58739638169DC8379A9D20772C00` |
| `completed-refs.txt` | `689929A87FBD6E240D237BE40A2AFB1A6A3C293F5A84F0197B153843636907AA` |
| `normalized-test-invocation.json` | `E38DEE43503EF74E3376F24048DB37CEB5A2AD41A9A6DF73254AB671BE9AABBE` |
| `normalized-test.log` | `069D05D6896E64BA67FDDD2B595BCA94C7E4CA77E504998DF82EEC6BD9B7D9DB` |
| `normalized-test-exit.json` | `66A3D219326B5D813024990970FB44448A7164702CDA1E0A61630BF95F837BC7` |
| `normalized-CodegenTest.xml` | `BC87D5DAFA4E5E9D4BD760683B51A6FA3414663B3C7441D349B4EE9D4FE64697` |
| `passing-production-classes.json` | `B10EE6B47ED9A302E02F456D4A9E8896CF8786E10DB76420DA6E9094CBAF7F01` |
| `warm-artifact.json` | `F85A621BFDA1906514509D660986A45889894FE8876E90EF00E235FD08BF669D` |

## Executed check consumed from the code Result

This documentation outcome ran no build, compiler or test. It consumed the fresh code check recorded by the completed Result:

```text
C:/Users/WINDOWS/.gradle/wrapper/dists/gradle-9.2.0-bin/11i5gvueggl8a5cioxuftxrik/gradle-9.2.0/bin/gradle.bat -p E:/Projects/cloche/jvm-multiplatform :kotlin-class-extensions-plugin:test --tests net.msrandom.classextensions.kotlin.plugin.CodegenTest --offline --no-daemon --no-configuration-cache --console=plain --max-workers=2 --rerun-tasks -Dorg.gradle.java.installations.paths=D:/Applications/Scoop/apps/zulu8-jdk/8.92.0.21 -Dorg.gradle.java.installations.auto-download=false --stacktrace
```

The process used `JAVA_HOME=D:/Applications/Scoop/apps/temurin21-jdk/21.0.10-7.0` and the isolated `GRADLE_USER_HOME=C:/Users/WINDOWS/AppData/Local/Temp/i-fir-20261004-01/physical-gradle-user-home`. It exited 0 with `BUILD SUCCESSFUL in 26s` and 10 actionable tasks executed. The fresh JUnit report is `normalized-CodegenTest.xml`: `tests=1`, `skipped=0`, `failures=0`, `errors=0`.

The test assertions passed the Kotlin CLI with `ExitCode.OK`; construction of the generated consumer class; `injected()` returning `42`; `hi()` returning `hi`; `consume()` returning `42`; primitive `int` field `c`; mutable property `7` changing to `9`; and removal of `a/b/c/AExtension.class`. The compiled production output contains 21 classes and every audited class has major version `52`, preserving the Java 8 target. The fixture source files and `CodegenTest.kt` are the exact tested candidate inputs recorded by the payload and hash artifacts.

The first candidate test failure showed platform extension discovery with zero generated classes. The isolated probe identified Windows CRLF input as the cause of Kotlin light-tree `ERROR_ELEMENT` output. The final code candidate includes only the owning normalization in `LazyExtensionFinder`; the fresh rerun above passed all compiler and runtime assertions.

## Artifact and input integrity

Before this docs delivery, local `main` was independently confirmed at `3c0653b23f7e6bf190a4c18e715253e0c4b57f59`, with tree `af5df416c9f06694aa1fd03c28e0770c9f0368e8`, an empty index and no `MERGE_HEAD`. The completed state and refs snapshots match the current candidate; all 45 local refs in `completed-refs.txt` resolve unchanged. The old combined integration ancestry check returned 1. The three source checkouts remain at their recorded tips: inheritance `d05686ae9f3f2171a567e239bc347f10ac712f19` with its two preserved formatter test diffs, parameters `08e4239cdc1e2f84f6f645a3a0188b27443ff4c0` clean, and worker `2d584ad5a48a8ceb9597790d0559d2856f2fdd82` clean.

The 48 physical input/residual files in `completed-hashes.json` still match their recorded SHA256 values: 27 passing inputs and 21 residual paths. Git checkout filters can make a physical text-file hash differ from its canonical tree blob; the reconciliation therefore records both physical SHA256 and candidate blob identity, rather than treating EOL conversion as code drift. The 21 class artifacts independently match their saved SHA256 values and major-version 52 audit.

The fixed IntelliJ 2.2.1 Gradle plugin artifact remains in the isolated cache with 31,264,345 bytes and SHA256 `05F4069F608A29F01134DFFB16FA972EC3411E4AB93A3F78963513C12DB1F866`, matching its URL digest. The official `.sha256` sidecar returned 404 and was not treated as evidence. A separate preexisting `C:/Users/WINDOWS/caches/modules-2` cache tree was neither deleted nor used; its preservation is disclosed as residual environment state.

## Residual state and boundary

The current main worktree retains exactly the preexisting tracked formatter change in `kmp-actual-stubs/compiler-plugin/build.gradle.kts` (physical SHA256 `AC56A695791F4489A1B82EE2061667C842E12B7340BE774B0529E046550F4334`) and the 18 untracked `.intellijPlatform/localPlatformArtifacts/*.xml` files. Their saved hashes remain unchanged. The inherited `.git/next-index-264852.lock` remains present with its recorded 27,760-byte hash; no lock or XML was staged, changed, deleted or excluded. Recovery refs and frozen source refs remain preserved.

The completed hash inventory locates all 18 XML paths and records `.git/info/exclude` SHA256 `6671FE83B7A07C8932EE89164D1F2793B2318058EB8B98DC5C06EE0A5A3B0EC1` (240 bytes) and lock SHA256 `C2F6980661CFB072A13CC9912ED76CC144A3A82B979BF461FD973A2543F9716B` (27,760 bytes). The unrelated `C:/Users/WINDOWS/caches/modules-2` tree contains 12,968 files totaling 1,974,753,309 bytes; its sorted path/length/SHA256 inventory digest at docs entry is `963ED5C68FCA07DD767B078C314005D0828B7E36C141EC7F43809E1010E60805`. No cleanup or cache mutation belongs to this delivery.

The accepted classpath version remains `0.1.12+terrarium.0.1.12`; FIR applies `1.0.11+terrarium.1.0.11`. Actual/P4 has not run and its `0.1.2+terrarium.0.1.2` version remains pending. The passing physical Gradle 9.2.0 root CodegenTest route establishes only that selected FIR check; it does not establish a full root check, actual/P4, classpath plugin or final acceptance. Earlier isolated classpath passes remain distinct. Historical signing and build failures remain historical evidence in prior records; FIR's prior resolution/settings-ABI and failed-fixture checkpoints are superseded by the isolated cache, signed code commits and fresh passing CodegenTest result above.

`I-fir` is complete. `I-fir-docs` remains pending because the required signed docs commit was blocked after a 300-second GPG attempt; `W-fir` is not released. This five-path candidate remains staged and recoverable. No actual, plugin or final task was executed by this documentation attempt.
