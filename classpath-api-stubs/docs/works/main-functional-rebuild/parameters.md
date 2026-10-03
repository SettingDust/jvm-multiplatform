# Parameter Integration Evidence

This ordinary evidence record belongs to `I-parameters-docs`. It consumes the completed parameter code Result and records the exact candidate, historical verification and remaining boundary. It is derived coordination material, not product or technical authority.

## Candidate and provenance

| Identity | Value |
| --- | --- |
| Code candidate | `a2609ff0d901f077da7042fc1ac70625f4613e5d` |
| Code tree | `07af1d2009dca9c44f1b2791dadb01783f709a6d` |
| First parent | `fd5be9ef6135ec3bef1f472b942957bd46f581b2` |
| Second parent, frozen parameter source | `08e4239cdc1e2f84f6f645a3a0188b27443ff4c0` |
| Code signature | `git verify-commit` exit 0; Good EDDSA signature from `3B992602764F685EE1DD065D4FFAF1C206BF8586` |
| Classpath version | `0.1.12+terrarium.0.1.12` |

The code candidate is already signed and merged into local `main`. Its first-parent diff contains exactly these four paths:

- `classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/ClassNodeIntersector.kt`
- `classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/ParameterGenerationTest.kt`
- `classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/ParameterMetadataTest.kt`
- `classpath-api-stubs/src/test/kotlin/net/msrandom/stubs/PreserveMethodBodiesTest.kt`

The implementation applies metadata only when every original common method supports the same defensible layout. It uses descriptor positions for parameter annotations, accepts shortened ASM tables only with a proven synthetic or mandated prefix, preserves both annotation visibilities, recursively validates annotation values, and conservatively omits malformed or conflicting metadata. Names and access flags require all-input agreement. The existing inheritance, Signature, method-body, byte and cache behavior remains in the candidate.

## Historical verification consumed

The code Result's saved isolated invocation exited 0 using physical Gradle 9.5.1 and a Java 21 runtime, with the Azul Java 8 toolchain selected and Java 8 production targets retained. It used the existing `classpath-api-stubs/verification/settings.gradle` route with offline, no-daemon, no-configuration-cache and `--max-workers=2` settings. This documentation outcome did not rerun Gradle, a compiler or tests.

| Saved report | Tests | Result |
| --- | ---: | --- |
| `net.msrandom.stubs.CommonSuperClassFinderTest` | 2 | 0 failures, 0 errors, 0 skipped |
| `net.msrandom.stubs.InheritanceGenerationTest` | 3 | 0 failures, 0 errors, 0 skipped |
| `net.msrandom.stubs.MethodBodiesTest` | 4 | 0 failures, 0 errors, 0 skipped |
| `net.msrandom.stubs.ParameterGenerationTest` | 1 | 0 failures, 0 errors, 0 skipped |
| `net.msrandom.stubs.ParameterMetadataTest` | 8 | 0 failures, 0 errors, 0 skipped |
| `net.msrandom.stubs.PreserveMethodBodiesTest` | 1 | 0 failures, 0 errors, 0 skipped |
| `net.msrandom.stubs.signature.ParsingTest` | 1 | 0 failures, 0 errors, 0 skipped |
| `net.msrandom.stubs.signature.SignatureIntersectionTest` | 5 | 0 failures, 0 errors, 0 skipped |
| **Total across 8 XML reports** | **25** | **0 failures, 0 errors, 0 skipped** |

The parameter-specific reports cover static and instance methods, wide descriptors, synthetic and mandated prefixes, ambiguous short layouts, both annotation visibilities, nested/enum/array values, names and flags, null and malformed inputs, pair/triple permutations, and Code/cache preservation. The associated audit recorded 30 tested inputs, 19 residual hashes and lengths, 29 refs and 3 source checkouts. Those counts are historical evidence associated with the signed code candidate.

The root wrapper route remains a limitation. The historical offline Gradle 9.2.0 attempt reached the root build but failed while resolving `org.gradle.kotlin.embedded-kotlin:6.4.1`; this is not a passing root build. No full `:classpath-api-stubs:check` was run for this docs-only outcome. The Java 8 target is recorded separately from the Java 21 test runtime.

## Documentation result and boundary

`I-parameters-docs` updates exactly these five ordinary-documentation paths:

- `classpath-api-stubs/docs/works/main-functional-rebuild/context.md`
- `classpath-api-stubs/docs/works/main-functional-rebuild/parameters.md`
- `classpath-api-stubs/docs/works/main-functional-rebuild/plan.md`
- `classpath-api-stubs/docs/works/main-functional-rebuild/waves.md`
- `classpath-api-stubs/docs/works/main-functional-rebuild/work.md`

The documentation commit is separate from `a2609ff`, signed with the existing GPG configuration, and follows the unchanged code candidate. It records `I-parameters` and `I-parameters-docs` as completed and releases `W-parameters`. The Plan itself remains in progress.

The worktree residuals remain outside this documentation scope: the unstaged `kmp-actual-stubs/compiler-plugin/build.gradle.kts` formatter change, 18 untracked `.intellijPlatform/localPlatformArtifacts/*.xml` files, and the inherited `.git/next-index-264852.lock` (27760 bytes). The actual formatter's preserved SHA256 is `AC56A695791F4489A1B82EE2061667C842E12B7340BE774B0529E046550F4334`; the XML files remain byte-identical and unstaged. No residual source, build file or XML was staged, modified or deleted.

Worker-memory and its documentation, FIR, actual/P4, plugin, their documentation and the final audit remain pending. No Worker or other later functionality was executed by this documentation outcome.
