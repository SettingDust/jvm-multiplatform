# Preserve Method Bodies: Tasks

Derived from the single [Implementation Plan](plan.md) and [confirmed Spec](../../spec/preserve-method-bodies.md). T1-T4 were completed in the separate implementation worktree following user authorization. No separate Plan confirmation is claimed. The Spec and this Work/Plan/Tasks set remain on main; [verification evidence](verification.zh-CN.md) records the isolated build path and limitations.

## T1: Task option and input tracking (complete)

- **Observed:** Default-off `@Input` option and file-byte tracking implemented. TestKit checks option toggling, single-input body-only changes for both settings, excluded-copy byte changes, and up-to-date/cache behavior; see the [report](verification.zh-CN.md).

- **Objective:** Make the new default-off boolean task option and all output-affecting artifact bytes participate in Gradle input state.
- **Authority/context:** Spec, [Work Context](context.md), [`GenerateStubApi.kt`](../../../src/main/kotlin/net/msrandom/stubs/GenerateStubApi.kt), and [`StubGenerator.kt`](../../../src/main/kotlin/net/msrandom/stubs/StubGenerator.kt).
- **Touches/constraints:** Add `preserveMethodBodies` as a task `@Input` with `false` convention; propagate its value to the generator interface without altering one-input passthrough or off-mode behavior. Replace `@CompileClasspath` normalization on nested artifact files with input tracking that sees body-only changes to included jars and changes to excluded artifacts copied into output, including when the option is off. Keep existing output and artifact-selection behavior.
- **Evidence:** Focused task fixture demonstrates the switch participates in the input state; a body-only change in an included input of a one-input task with either switch value and changed bytes in an excluded copied artifact trigger reevaluation or correctly keyed cache retrieval, not stale `UP-TO-DATE`/cache output. An unchanged run can remain up-to-date. Full multiple-input cache/output transition is checked in T4.
- **Depends on:** Dedicated implementation worktree setup (complete).

## T2: Conservative structural Code comparison (complete)

- **Observed:** Four `MethodBodiesTest` tests passed, covering symmetric structural comparison, Code presence, operands/constants/control flow/handlers, ignored debug/frame differences, and conservative uncomparable cases; see the [report](verification.zh-CN.md).

- **Objective:** Decide, symmetrically, whether two original methods have comparable equal Code under the Spec.
- **Authority/context:** Spec, [Plan](plan.md); ASM tree 9.7.1 from [`build.gradle.kts`](../../../build.gradle.kts).
- **Touches/constraints:** Add only the comparison logic needed in `classpath-api-stubs`; compare complete ordered executable instructions and typed operands, constants by value/kind (including nested `ConstantDynamic`), `invokedynamic` bootstrap data, branch/switch positions with end sentinel, and ordered exception handlers with end-exclusive boundaries and catch types. Ignore debug lines, local-variable debug tables, redundant labels, and frame representation. Compare Code type annotations with target positions/values or treat Code as uncomparable; unsupported Code metadata, instruction kinds, or constant kinds cannot establish equality. Distinguish Code absent from Code containing no executable instructions; do not mistake abstract/native for candidates. Do not compare member declarations here.
- **Evidence:** ASM-generated method fixtures assert symmetric equality for debug/frame/label-only differences and mismatch for changed opcode/operand, branch/switch target, bootstrap argument or nested constant, handler range/type/order, and annotated position/value difference (or conservative omission). Include unknown Code metadata and Code presence cases. No JVM execution or compilation check.
- **Depends on:** Dedicated implementation worktree setup (complete); parallel with T1 when edits do not conflict.

## T3: Select and emit common bodies (complete)

- **Observed:** ASM-read generated jars in `PreserveMethodBodiesTest` check two/three inputs, reversed order, default-off and single-input cases, eligible and ineligible Code, special methods, and declaration intersection; see the [report](verification.zh-CN.md).

- **Objective:** Preserve only all-input-equal Code on already-common methods while retaining the existing declaration intersection.
- **Authority/context:** Spec, [Plan](plan.md), [`StubGenerator.kt`](../../../src/main/kotlin/net/msrandom/stubs/StubGenerator.kt), [`ClassNodeIntersector.kt`](../../../src/main/kotlin/net/msrandom/stubs/ClassNodeIntersector.kt), T1, T2.
- **Touches/constraints:** Keep existing method selection and class/member/access/signature intersection. For enabled multi-input generation, compare the original methods of every contributing input by JVM name/descriptor; never use a bodyless intermediate intersection as a comparison source. On match, transfer one original, coherent complete Code body into the intersected declaration without replacing its method-level annotations/exceptions or mixing source labels/handlers/frames/maxima. On mismatch, missing Code, or uncertainty, leave the common declaration bodyless, without synthetic Code or difference-only failure. One input passthrough, disabled multi-input behavior, constructors, initializers, abstract/native and input-order independence follow the Spec.
- **Evidence:** ASM-read jar fixture confirms eligible two- and three-input bodies present, reversed inputs make the same retain/omit decision, disabled multiple inputs omit Code, single input preserves Code with either setting, and unequal/missing/uncomparable Code leaves the declaration but no Code. Assert surviving members and declaration metadata are unchanged versus the existing intersection; include `<init>`, `<clinit>`, abstract/native. Do not claim equal references imply equal helper implementations.
- **Depends on:** T1 and T2. Serialize shared edits if necessary.

## T4: End-to-end task and cache gate (complete)

- **Observed:** One TestKit test ran 11 real Gradle builds, checking body-only changes, selected output transitions, `UP_TO_DATE`, `SUCCESS`, `FROM_CACHE`, single-input and excluded copies. JUnit XML reports six tests total, zero failures/errors/skips; source diff is scoped to `classpath-api-stubs`. Verification used isolated Gradle 9.4.1 settings because the repository wrapper/root settings failed. See the [report](verification.zh-CN.md); no root-build success or runtime validity is claimed.

- **Objective:** Establish reproducible evidence that all Spec acceptance cases hold in real generated artifacts and Gradle task input state.
- **Authority/context:** Spec acceptance section, [Plan](plan.md), T1-T3, existing `kotlin("test")` and `gradleTestKit()` dependencies in [`build.gradle.kts`](../../../build.gradle.kts).
- **Touches/constraints:** Complete focused ASM jar assertions and a real Gradle task fixture (registering the task directly if existing plugin wiring prevents plugin application). Change only the method body of an included jar, without ABI change, between runs; verify task reevaluation plus the expected retain/omit output transition with up-to-date checks and build cache in scope. Repeat one-input body-only change with option enabled/disabled, toggle the option, and check changed excluded-copy bytes. Confirm equal-body positive and difference/absence/uncomparable negative cases, debug/frame-insensitive cases, and preserved declaration intersection. Treat stub compilation, JVM load/run, frame verification, and injection as non-gates.
- **Evidence:** Run the smallest affected package tests (for example `./gradlew :classpath-api-stubs:test` in the implementation worktree) and report test outcomes plus any baseline/toolchain limitations. Inspect final diff for `classpath-api-stubs`-only code/test changes; report remaining conservative omissions rather than claiming unsupported Code can be preserved. No release, Cloche activation, commit, or push is implied.
- **Depends on:** T3. Completion closes the Plan only when the checks pass against the confirmed Spec.
