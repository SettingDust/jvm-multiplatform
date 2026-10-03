> **Historical recovery note (`I-worker-docs`):** This after-base ordinary document was restored from original-main commit `cd623a95a75f07639f35a2bbf1a36f1926605551` (`docs(stubs): record functional branch sync plan`, parent `cf98546a6e7f828b30b4b4a560c86bbf02b20578`). It records the historical old `main` to functional-branches direction and retains its original completion/readiness records and source references. It is ordinary provenance only; it does not support current `main-functional-rebuild` Worker completion, authority or any later gate. Current Worker evidence is [worker.md](../main-functional-rebuild/worker.md).
# Functional Sync Context

Derived planning evidence, not product or technical authority. This package is ready for substantive Router disclosure; detailed implementation has not been approved. The current Handler only writes this new package and does not execute branch items.

## Fixed Source And Target Baselines

The source is local `main` at `cf98546a6e7f828b30b4b4a560c86bbf02b20578`, not a moving branch name or assumed remote tip. Source code was clean at refresh; before package completion main's only change was this package's new `work.md`. No fetch occurred. Future source extraction must use this Git object, not arbitrary current working files.

| Worktree | Branch | HEAD | Staged changes |
| --- | --- | --- | --- |
| `E:/Projects/cloche/jvm-multiplatform` | `main` | `cf98546a6e7f828b30b4b4a560c86bbf02b20578` | none |
| `E:/Projects/cloche/jvm-multiplatform-inheritance` | `fix/stubs-inheritance-upstream` | `cc193b68ffb42d4bcdce6c8cecc0f2b9a135ccb6` | none |
| `E:/Projects/cloche/jvm-multiplatform-parameters` | `fix/stubs-method-parameters` | `5aa0b3e1053115057f3781ab5656dccb253771fa` | none |
| `E:/Projects/cloche/jvm-multiplatform-worker-memory` | `perf/stubs-worker-memory` | `5aa0b3e1053115057f3781ab5656dccb253771fa` | none |

The following existing dirty paths are relative to each worktree; they are user work to retain, not disposable scratch files.

```text
inheritance (modified):
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/ClassNodeIntersector.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/CommonSuperClassFinder.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/StubGenerator.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/signature/SignatureIntersector.kt
inheritance (untracked):
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/CommonSuperClassFinderTest.kt
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/signature/SignatureIntersectionTest.kt
classpath-api-stubs/verification/settings.gradle
parameters (modified):
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/ClassNodeIntersector.kt
parameters (untracked):
classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/ParameterMetadataTest.kt
worker-memory (modified):
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/CommonSuperClassFinder.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/GenerateStubApi.kt
classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/StubGenerator.kt
classpath-api-stubs/verification/settings.gradle
```

Future execution refreshes HEAD, complete status, staged diff, and allowed dirty-file contents. Unexpected changes are never reset, cleaned, overwritten, or silently committed. Nonconflicting unrelated changes stay outside staging. A changed baseline that alters functional decisions requires reconciliation before edits, not a blind patch.

## Authority And Implementation Findings

- [Intersection/worker Spec](../../spec/api-stub-intersection-and-workers.md) owns all-input inheritance evidence, conservative generic metadata, descriptor-aligned parameters, worker submission, and resource ownership.
- [Code Spec](../../spec/preserve-method-bodies.md) owns default-off Code, all-original-input comparison, byte-sensitive task inputs, and one-input class-byte passthrough. Its historical implementation-pending prose is not evidence about today's main.
- Main combines all three functions. Whole-file copying the class intersector, generator, or ancestor finder would import unrelated functions. Select functional hunks and adapt calls to each target's shape.
- Inheritance already has list-based N-way intersection, main-style signature helpers, raw-header/signature checking, cycle failure, and exception-safe loader closure. It lacks the Code toggle and worker DTO route. Those are not backported. Main's one-input byte-copy path and byte-sensitive input annotation are minimal compatibility dependencies.
- Parameters has Code preservation and two-input metadata helpers. Its descriptor-length, null-entry, null-name, and annotation-layout guards are stronger than main and must survive. Add all-original-method metadata processing without converting the entire class/ancestor/signature algorithms to main's N-way implementation. Retain the binary entry point.
- Worker-memory already has `ArtifactSpec`, one `noIsolation` action, sequential entries, no full-node cache, lightweight header reads, and failure cleanup. Relative to main, it lacks the outer experimental-path opt-in, one-input byte-copy route, uncached resource connections, and optional-version fallback. Main's N-way ancestor/metadata changes are not worker dependencies.
- Existing Code tests are present on parameters/worker-memory. Inheritance has narrower ancestor/signature tests. Main tests do not prove complete Spec compliance; copying tests is not passing them.

## Refreshed Build And Signing Evidence

Physical Gradle: `D:/Applications/Scoop/apps/gradle/9.5.1`. `D:/Applications/Scoop/apps/gradle/current` is a Junction to it. Physical launcher JDK: `D:/Applications/Scoop/apps/temurin21-jdk/21.0.10-7.0`. Physical Java 8 toolchain: `D:/Applications/Scoop/apps/zulu8-jdk/8.92.0.21`. The repository wrapper selects Gradle 9.2.0, so the physical 9.5.1 route is an explicit local verification choice, not wrapper-version verification.

Actually executed probes used process-local `JAVA_HOME`, physical `gradle.bat`, `--offline --no-daemon --no-configuration-cache --console=plain`, and separate temporary project-cache directories:

1. Main root `help` failed: `Configuring project ':class-extensions-idea' without an existing directory is not allowed.` Its configured `E:\Projects\cloche\jvm-multiplatform\class-extensions\idea-plugin` directory is absent. This is refreshed evidence, not an assumed historical failure.
2. Main isolated `-p E:/Projects/cloche/jvm-multiplatform/classpath-api-stubs/verification help` succeeded in 6 seconds with the explicit Java 8 installation path. Configuration isolation is viable. This proves neither compilation/toolchain resolution nor any target tests/cache behavior.

No target test suite, consumer compilation, worker overlap measurement, memory benchmark, or signing attempt was executed by this planning Handler. `help` probes can produce ignored build reports; they did not edit tracked source/settings, indexes, or configuration.

Read-only configuration shows `user.name=SettingDust`, `user.email=settingdust@gmail.com`, `user.signingkey=4FFAF1C206BF8586`, and `core.autocrlf=true`. `commit.gpgsign`, `gpg.program`, and `gpg.format` are unset; this is not permission for unsigned delivery. Main HEAD has a good GPG signature (`%G?=G`) from that key. GPG resolves through `D:/Applications/Scoop/shims/gpg.exe`; a local secret key for `4FFAF1C206BF8586` is listed. These facts do not prove future signing/pinentry success.

Worktrees have separate indexes/HEAD files and shared Git common metadata/configuration. Git mutations, Gradle/TestKit workloads, host build resources, and GPG/pinentry interaction must not overlap across Handlers. `--max-workers=2` inside worker verification tests task overlap, not concurrent branch execution.

## Historical Sources And Open Conditions

Complete historical audit Result: `C:/Users/WINDOWS/.pi/agent/sessions/--E--Projects-cloche--/2026-09-26T19-48-28-195Z_01a0df43-0ba3-7696-8006-8634ea05c989/tasks/report-jvm-worktree-diffs-artifacts/result-1.json`.

Complete previous blocked planning Result: `C:/Users/WINDOWS/.pi/agent/sessions/--E--Projects-cloche--/2026-09-26T19-48-28-195Z_01a0df43-0ba3-7696-8006-8634ea05c989/tasks/plan-functional-branch-sync-artifacts/result-1.json`.

Both were read as history, not current Git/test evidence. The former Tasks-selector blockage is avoided by this new Work/Plan/Waves tuple, not historical migration. The old `api-stub-parallel-improvements` Work/Context/Plan/Tasks and both Specs remain unchanged. Open execution conditions are explicit detailed-scope confirmation, fresh target reconciliation, real passing verification, and usable signing. Evidence outside the stated slice requires disclosure, not automatic scope expansion.
