# Preserve Actual Matching in JVM Stub Compilation

Status: **Accepted behavior; implementation and verification pending.** This Spec records the accepted direction for `kmp-actual-stubs/compiler-plugin`. It does not approve an implementation Plan, prescribe an unproven FIR algorithm, or claim that acceptance checks have passed.

## Ownership and boundary

The plugin compiles shared Kotlin sources into JVM compilation stubs. In a single K2 JVM invocation, some expected declarations may already have actual implementations in participating refining fragments, while other expected declarations still need stubs. The plugin must preserve the former relationships and generate placeholders for the latter without accepting invalid actual declarations.

This contract owns only the stub compiler plugin and its necessary compilation tests. It does not change source-set orchestration, Cloche, Codev, Java expect/actual, or `classpath-api-stubs`. Existing method-body preservation work in `classpath-api-stubs` must remain untouched. A common stub artifact is not a runnable platform implementation, even when some declarations come from real actuals. See the repository [domain context](../../../../CONTEXT.md) and the independent [method-body Spec](../../../../classpath-api-stubs/docs/spec/preserve-method-bodies.md).

Documentation is maintained in the current `main` workspace. Subsequent code implementation belongs in a separate worktree and branch; this document does not create either. K1, IDE/Analysis API integration, arbitrary Kotlin versions, repository-wide upgrades, metadata-only compilation, and new public configuration options are outside this contract.

## Baseline and compiler version

The inspected main baseline is `dbf9ca3e581a5754201971cdd7775868a0761c06`. Its [registrar](../../src/main/kotlin/net/msrandom/stub/StubCompilerPlugin.kt) installs a status transformer, an enum constructor generator, and an IR body generator and declares K2 support. Its [status transformer](../../src/main/kotlin/net/msrandom/stub/fir/FirExpectTransformer.kt) selects declarations with an explicit source `expect` modifier and removes `isExpect` for functions, properties, and regular classes without testing for actuals. Its [IR generator](../../src/main/kotlin/net/msrandom/stub/ir/IrStubBodyGenerator.kt) writes throwing bodies for marked declarations and members of marked classes. These are source observations, not a reproduction of mixed-fragment failure.

The baseline version chain is:

| Setting | Evidence and interpretation |
| --- | --- |
| Gradle wrapper | [Wrapper properties](../../../../gradle/wrapper/gradle-wrapper.properties) select Gradle **9.2.0**. |
| Embedded Kotlin | The [Gradle 9.2.0 compatibility matrix](https://docs.gradle.org/9.2.0/userguide/compatibility.html#kotlin) identifies **2.2.20** as its embedded Kotlin version. |
| Kotlin plugin and compiler dependency | The [root build](../../../../build.gradle.kts) requests `embedded-kotlin` and `kotlin-dsl` with `apply false`; the [module build](../../build.gradle.kts) applies `kotlin("jvm")` without a local version override and depends on `kotlin("compiler-embeddable")`. The intended baseline is therefore **compiler-embeddable 2.2.20**, subject to recording actual dependency resolution during implementation verification. This is not a claim that Gradle dependencies were resolved in this documentation task. |
| Language version | The [existing test](../../src/test/kotlin/net/msrandom/stub/tests/Tests.kt) explicitly selects **2.1**. This is a language mode, not a compiler binary version. |
| JVM | The module selects `jvmToolchain(8)`; this does not make Java 8 a supported runtime for Gradle 9.2.0. Verification must record the Gradle runtime and test/compiler JVM separately. |

Acceptance targets K2 JVM compiler **2.2.20**, with language version **2.1** as the existing fixture baseline. Fixture flags must enable multiplatform compilation and describe actual fragment membership and refinement. A different resolved compiler is a baseline discrepancy to explain before claiming acceptance. Changing Gradle can change embedded Kotlin and hence compiler/plugin dependencies in this build; a newer local Gradle or historical fork wrapper is not evidence that this baseline works. No wrapper or compiler upgrade is authorized here. KSP's root version expression (`$embeddedKotlinVersion-+`) is also not a compiler-version pin; verification must record its resolved version when the normal plugin packaging uses it.

## Behavioral contract

### Inputs and correspondence

The relevant inputs are the source files actually submitted to one compiler invocation, their assigned fragments, the directed refinement relationships, compiler options, and the configured dependency classpath. Files merely present elsewhere in a source directory or repository are not participating declarations.

A corresponding actual is determined under the selected compiler's expect/actual rules and the invocation's fragment graph. Package/name equality, source text, file name suffixes, matching `ClassId` alone across arbitrary modules, or a different symbol object do not establish complete compatibility. Callable overloads, receivers, type parameters, owning classes, and actual typealias expansion must receive compiler-equivalent treatment. Moving files without changing their content or fragment assignment, or reordering source arguments, must not change the match/stub decision.

**Matching and compatibility checking are distinct.** An actual can bind to an expect and still fail compatibility checks, for example on its return type. Such a pair is not equivalent to an absent actual. The plugin must preserve the compiler's ability to reject it. These rules define observable outcomes, not a requirement to ask for fully checked matching data during status transformation.

### Outcomes

1. **Compatible actual present:** The expect must remain available for normal compiler matching, checking, and actualization. The plugin must not convert it into a competing ordinary declaration, add plugin-generated stub behavior to that pair, or replace the actual's body. The final JVM artifact must contain the actualized declaration with its real implementation and no competing generated stub. Retaining the expect is a requirement during compilation, not a requirement to emit an additional expect declaration into JVM output.
2. **No actual present:** An otherwise valid expected declaration within the existing transformation surface must still compile to a stub. It must not fail solely because its actual is absent. Its public shape must remain usable by code compiled in that invocation: functions, properties, class members, and supported class forms must retain their declared callable/type shape. Generated concrete executable bodies must retain the existing `UnsupportedOperationException("Common modules are not runnable")` behavior. Abstract/interface/annotation declarations need not gain artificial executable bodies where the JVM representation does not require them.
3. **Orphan actual:** An actual with no corresponding expect in its legal refinement ancestry must remain a compilation error. The plugin must not remove `actual`, invent an expect, or suppress `ACTUAL_WITHOUT_EXPECT` to make it pass.
4. **Incompatible or ambiguous actual:** An incompatible signature, incompatible class/member/typealias target, or compiler-rejected ambiguity must remain an error. Stub fallback must not turn the invocation into success. For a pair that binds but fails checking, the relevant compatibility error must remain observable; replacing it with a successful stub or erasing the pairing obligation is forbidden. A parameter/receiver mismatch that prevents binding may produce `ACTUAL_WITHOUT_EXPECT`; an identical diagnostic transcript across all cases is not required.
5. **Mixed input:** The decisions are per declaration and compiler relationship, not per file or package. A valid actual for one overload does not satisfy another overload, and a valid pair does not remove the requirement to stub unrelated missing declarations. If any genuine error remains, the overall compilation must fail even if other declarations could be stubbed successfully.

Ordinary declarations and user annotations must retain their existing meaning. No plugin-generated broad suppression of `ACTUAL_WITHOUT_EXPECT`, expect/actual incompatibility, ambiguity, or unrelated source errors is permitted. Existing narrowly scoped missing-body/initializer allowances may be used only for declarations actually converted into stubs. Verification of diagnostic preservation must use fixtures without user-written diagnostic suppressions; the plugin is not required to override intentional user suppression semantics.

An unavailable symbol, failed lookup, or incomplete compiler phase must not silently count as proof that a legal actual is absent. The implementation must either meet the specified compile outcome or expose its limitation/failure; an internal compiler error is never an acceptable substitute for an expected source diagnostic.

### Classes, members, enums, and typealiases

The current transformation surface includes regular classes, interfaces, objects, annotation classes, enum classes, top-level functions and properties, extension properties, and the constructors/functions/properties reached through converted class declarations. The existing test resource includes class members without explicit `expect` modifiers; these members are part of the acceptance boundary rather than excluded by the current selector's implementation detail.

For a preserved class pair, its expected constructors and members must remain available for class-scope checking. A missing or incompatible member in its actual class must not be filled with a stub to conceal that error. Members of a class with no actual must still receive the required stub treatment, including members without explicit `expect` keywords. A preserved expect enum must not receive plugin-generated constructors that interfere with normal actualization; a converted enum must retain its entries and the constructor support needed for successful stub compilation. The same separation applies to nested declarations reached by the existing class traversal.

A compiler-valid `actual typealias` may satisfy an expected class-like declaration. Its target must be checked under the compiler's rules, including required members; the plugin must not emit a competing class stub or rewrite the alias target into one. This does not introduce an `expect typealias` feature or promise a new transformation for arbitrary typealiases.

## Acceptance evidence

Acceptance requires actual compiler exit results, captured diagnostics, and inspection of emitted JVM artifacts. A name-index unit test, the absence of exceptions in a test method, or a successful Gradle task without asserted compiler results is insufficient. The current single-fragment test calls the CLI entry point but does not provide the required matrix or explicit diagnostic/artifact assertions.

Each fixture must use an isolated output location and record compiler/plugin versions, language/API settings, fragment/source/refinement arguments, classpath, exit code, and diagnostics. Positive cases must assert successful compilation and inspect relevant emitted declarations/descriptors and bodies. Negative cases must assert compilation failure and a relevant source diagnostic, excluding setup failures and compiler crashes; output left after failure must not be treated as an accepted artifact. Where diagnostic identity depends on binding versus checking, establish the expected category using the same compiler and equivalent valid fragment setup without this plugin. Such controls are required for incompatible/ambiguous cases, not as a demand that an unimplemented expect compile without the plugin.

The following is a required matrix. Rows are coverage obligations, not test framework or file-layout instructions. All checks are **pending**.

| ID | Fixture and variation | Required observable result |
| --- | --- | --- |
| A1 | Common expect function; compatible actual in a refining fragment; call from common code | Success; one actual implementation with the intended descriptor; its identifiable real body is not replaced by the stub throw. |
| A2 | No actual: function returning a value, Unit function, `val`, `var`, and extension property | Success; declarations/accessors retain their shape; generated executable stub bodies throw the established exception. |
| A3 | Same invocation and common file contain one valid pair and one missing declaration | Success; actual body retained and missing declaration stubbed independently. |
| A4 | Orphan actual function, property, class, and typealias in otherwise valid input | Failure with `ACTUAL_WITHOUT_EXPECT` or the compiler's relevant orphan diagnostic; no plugin suppression. Include an unrelated valid pair in one variant. |
| A5 | Pair candidates with changed parameter type, extension receiver, generic signature, return type, or `val`/`var` compatibility | Failure on each compiler-invalid variant. Include an inferred actual return type to cover checking after matching. A binding-compatible return mismatch must retain its incompatibility diagnostic. |
| A6 | Two same-name expect overloads, only one actual; repeat with receiver-based overloads | Success; the corresponding overload keeps its actual and the other gets a stub. Both can be referenced. Same-name declarations in another package or class must not affect either decision. |
| A7 | `base <- intermediate <- leaf` refinement, with an actual in intermediate and a different actual in leaf | Success for legal distinct pairs through direct/transitive refinement, together with a missing declaration stub. No assumption that all actuals occur only in the final fragment. |
| A8 | Sibling fragments both refined by one leaf, but an actual in one sibling has an expect only in the other; also an actual file excluded from the invocation | Sibling orphan remains an error. An excluded file cannot prevent stub generation for an otherwise missing expect. Physical proximity does not imply refinement. |
| A9 | Compiler-invalid ambiguous expect ancestry or duplicate actual declarations | Failure with the corresponding ambiguity/conflict diagnostic; no arbitrary selection or stub fallback that yields success. |
| A10 | No-actual class with constructor, property, mutable property, function, and nested class; interface, object, and annotation class variants | Success with required JVM shapes and stub bodies where concrete bodies are generated. Implicit expect members without explicit modifiers must be covered. Preserve the existing annotation parameter shape. |
| A11 | Compatible actual class with constructors/members; separate missing-member and incompatible-member variants | Valid pair succeeds with actual member bodies retained. Invalid variants fail with class-scope/member diagnostics; no member repair by stubbing. Include compatible interface, object, and annotation-class pairs. |
| A12 | Expect enum with entries and member(s), first with no actual, then with a compatible actual enum; then a compiler-invalid entry/member mismatch | Missing case compiles with enum entries and necessary stub constructor/body behavior; matched case uses the actual with no extra constructor conflict; mismatch fails. |
| A13 | Expected class satisfied by compatible actual typealias; incompatible alias target/member shape and orphan alias variants | Valid alias actualization succeeds without a competing generated class. Invalid targets and orphan alias fail with relevant diagnostics. No requirement for a separate JVM class file named after an erased alias. |
| A14 | Repeat representative mixed/overload/fragment cases with reordered file arguments, files outside package-shaped directories, identical basenames, multiline declarations and backtick identifiers; add irrelevant comment/string text containing `actual` | Outcomes unchanged for semantically equivalent inputs; no text-index or filename-based false matches. |
| A15 | Ordinary implemented declarations, existing explicit `@Stub` usage, and an unrelated source type error beside a valid pair | Ordinary bodies remain intact, established explicit stub behavior remains, and the unrelated error still fails compilation. Assertions about retained actual bodies use actuals without intentional user `@Stub` annotations. |

Artifact inspection may use bytecode or compiler output tooling; it must distinguish a retained actual body from the generated throwing body and detect duplicate declarations. Stub non-runnability is checked by inspecting generated executable bodies; running a whole common module or promising runtime dependency closure is not required. Successful compilation is required for this plugin's positive fixtures and does not change the separate bytecode-intersection feature's non-runtime contract.

## Compiler evidence and implementation limits

The Kotlin **v2.2.20** primary sources establish constraints rather than a ready implementation:

- [K2 KMP design](https://github.com/JetBrains/kotlin/blob/v2.2.20/docs/fir/k2_kmp.md): fragments and refinement form the compilation model; frontend modules are analyzed in dependency order; matching differs from compatibility checking, and inferred return types are not available for early binding.
- [FIR resolve phases](https://github.com/JetBrains/kotlin/blob/v2.2.20/compiler/fir/tree/src/org/jetbrains/kotlin/fir/declarations/FirResolvePhase.kt): `STATUS` precedes `EXPECT_ACTUAL_MATCHING`, and earlier phases must not rely on later-phase information.
- [Matcher transformer](https://github.com/JetBrains/kotlin/blob/v2.2.20/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/mpp/FirExpectActualMatcherTransformer.kt) and [resolver](https://github.com/JetBrains/kotlin/blob/v2.2.20/compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/mpp/FirExpectActualResolver.kt): actual-to-expect mappings include members/typealiases and dependency ancestry; they are not a package/name scan.
- [Declaration checker](https://github.com/JetBrains/kotlin/blob/v2.2.20/compiler/fir/checkers/src/org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirExpectActualDeclarationChecker.kt): checks orphan actuals, ambiguities, callable compatibility, class scopes, and typealias-backed class compatibility. Erasing an expect can affect more than one diagnostic path.

Consequently, this Spec does not require reading a completed actual mapping in `FirStatusTransformerExtension`, looking forward into later sessions, forcing later-phase resolution, or moving everything to IR. Feasibility of a compiler-phase-safe mechanism satisfying the matrix remains unproven. A mechanism that cannot preserve diagnostics or handle mixed missing/actualized declarations does not meet the contract; changing the accepted behavior or target compiler would require an explicit decision.

The historical fork commit `b9873eb` (also present in snapshot `cc705a4`) is reference material only. Its source-root regex/name index, filename-tail fallback, and generated `ACTUAL_WITHOUT_EXPECT` suppression are not accepted solutions. The [historical comparison](../../../../../upstream-differences.md) did not reproduce the mixed-actual issue. The separate [upstream review](../../../../../upstream-code-review.md) records an older isolated class-member missing-body failure; it is not evidence that this matrix was run against the current main or that the present mixed-actual hypothesis was reproduced.

## Unresolved items and readiness

No additional product choice is needed to state the accepted behavior above, and no conflicting owning Spec or PRD/ADR was found for this plugin in the inspected workspace. The method-body Spec belongs to a different module and does not authorize Kotlin actual-body rewriting.

Before implementation can be accepted, evidence is still needed for actual dependency resolution and packaging at the stated baseline, a phase-safe matching/stubbing mechanism, class/member/enum/typealias behavior under that mechanism, and the full diagnostic/artifact matrix. In particular, existing class-member transformation issues must not be silently excluded from A10 merely because they predate this fix. These are implementation and verification gaps, not approved Plan steps. If they require an unsupported compiler upgrade, broader scope, relaxed error preservation, or reduced declaration coverage, that is a substantive decision to surface explicitly rather than encode as a fallback.

This document was checked against local source, domain documentation, historical Git objects, and the linked versioned compiler sources. No compiler reproduction, Gradle build, or implementation test was executed for this Spec.
