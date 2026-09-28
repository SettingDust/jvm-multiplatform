# Preserve Method Bodies: Work Context

## Authority and settled constraints

- The sole owning technical authority for this feature is [the confirmed Spec](../../spec/preserve-method-bodies.md). It applies only to `classpath-api-stubs`; it does not authorize Cloche activation or changes to other packages.
- The user authorized implementation in a separate branch and worktree. The confirmed Spec and Work/Plan/Tasks remain in the main working tree. No additional, separate Plan confirmation is claimed.
- Both worktrees have HEAD `9bd0e543ab6d2de4698846cda47b21356ae04acb`. The Spec and Work documents are uncommitted on main and were not copied into the implementation worktree. Implementation is on `feat/preserve-method-bodies` at `E:/Projects/cloche/jvm-multiplatform-preserve-method-bodies`.

## Implementation and evidence

- In the implementation worktree, `GenerateStubApi.kt` exposes a default-off `@Input preserveMethodBodies` option and tracks nested artifact file bytes with `@InputFile` and `@PathSensitive(NAME_ONLY)`, including single-input passthrough and excluded copies.
- `MethodBodies.kt` compares original methods' Code structurally and conservatively rejects unsupported metadata. `StubGenerator.kt` preserves whole Code from one original source only for all-input-equal Code on already-intersected methods; the declaration intersection remains owned by `ClassNodeIntersector.kt`.
- The [verification report](verification.zh-CN.md) records T1-T4 checks: six JUnit tests passed (zero failures/errors/skips), including one TestKit test running 11 Gradle builds. The tests ran through isolated `verification/settings.gradle` with Gradle 9.4.1, not the failing repository wrapper/root settings. Test results and logs remain generated outputs in the implementation worktree.
- The implementation diff is limited to `classpath-api-stubs`; no commit, push, or merge has been made. The original plugin implementation-class warning persists. JVM load/execution, verifier, stub compilation, and injection were not tested as completion gates; identical bytecode references do not establish helper consistency.

## Status

T1-T4 completed against the confirmed Spec's scoped acceptance checks, with the wrapper/root-build limitation documented. The Spec remains the owning behavior authority; this Work records implementation evidence without changing its historical status text or claiming broader integration.
