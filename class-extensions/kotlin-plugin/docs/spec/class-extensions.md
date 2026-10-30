# Class Extensions Kotlin Compiler Plugin Spec

## Status and Scope

This Spec defines the behavior to preserve when migrating `class-extensions/kotlin-plugin` to Kotlin compiler API **2.4.10**. Kotlin 2.4.10 is the single target baseline; this is not a compatibility promise for older Kotlin compiler versions. The migration retains the selected Gradle 9.8.2 and KSP 2.3.12 versions. It does not authorize product or architecture changes.

The owning system is the Kotlin compiler plugin in this module. Its contract is limited to discovering class-extension source declarations, exposing injected functions and properties and extension supertypes through FIR, and removing extension carrier classes during IR generation. It does not define the Java annotation processor or Gradle plugin APIs.

This Spec separates the current implementation/test observations below from the normative migration requirements. Historical rebuild records are evidence about an earlier Kotlin/compiler baseline, not evidence that Kotlin 2.4.10 has been migrated or verified.

## Normative Behavior

1. A source class annotated with `@ClassExtension(Target::class)` associates that extension with the resolved target class. Package and import context must continue to resolve the class-literal target. Only class owners matching that target receive the extension behavior.
2. Only extension functions and properties marked `@ExtensionInject` are generated as target members. Their callable signatures, source modifiers and visibility, and runtime behavior must remain equivalent to the declarations in the extension class. In particular, generated property accessors must preserve `val`/`var` behavior, and references to target members must resolve against the target receiver.
3. Declared extension supertypes must be added to the target's supertypes without adding a duplicate of an already-resolved supertype. Injected overrides must remain callable through the target type.
4. `@ExtensionInject` is a compile-time selection marker, not an annotation on the resulting copied member. The `@ClassExtension` carrier declaration must not be emitted as a class in compiled output. `@ExtensionShadow` is documentary and does not itself generate a member.
5. Migration to Kotlin 2.4.10 may adapt FIR function/visitor, builder/copy, and symbol APIs, but must not change the behaviors above, caller-visible members, or annotation treatment. No older compiler baseline is required by this Spec.

## Evidence and Limits

The current implementation discovers extension classes among the relevant source files for the compiler module, resolves their targets in the extension file's package/import scope, and builds generated FIR declarations for injected functions and properties. It separately contributes declared extension supertypes and removes classes annotated with `@ClassExtension` from IR output. See [source discovery](../../src/main/kotlin/net/msrandom/classextensions/kotlin/plugin/LazyExtensionFinder.kt), [FIR declarations](../../src/main/kotlin/net/msrandom/classextensions/kotlin/plugin/ClassExtensionFirDeclarations.kt), [FIR supertypes](../../src/main/kotlin/net/msrandom/classextensions/kotlin/plugin/ClassExtensionFirSupertypes.kt), and [IR carrier removal](../../src/main/kotlin/net/msrandom/classextensions/kotlin/plugin/ExcludeClassExtensionsIrGenerationExtension.kt). `@ExtensionShadow`'s source annotation describes documentation-only behavior.

The existing [CodegenTest](../../src/test/kotlin/net/msrandom/classextensions/kotlin/plugin/CodegenTest.kt) compiles a common/platform fixture through the real Kotlin CLI with the plugin loaded, then loads and invokes compiled output. Its assertions cover an injected function returning `42`, an injected interface implementation returning `"hi"`, a common-source consumer returning `42`, an `Int` field, mutable getter/setter behavior (`7` then `9`), and absence of `AExtension.class`. The [fixtures](../../src/test/resources/test-platform.kt) also show `@ExtensionShadow` resolving to the target's existing `a` property.

The historical [FIR integration record](../../../../classpath-api-stubs/docs/works/main-functional-rebuild/fir.md) records a passing CodegenTest on an earlier Kotlin 2.2-era baseline, including the same runtime and carrier-removal assertions. It is historical evidence only. The current test still requests language version `2.2`; neither it nor the historical record demonstrates compatibility with Kotlin compiler API 2.4.10.

Constructors and nested classifiers appear in extension-source parsing structures, but the current FIR declaration path does not populate generated constructor or nested-classifier declarations. They are outside this migration contract; do not infer support from parser fields. Exact diagnostics for malformed annotations are also outside the preservation claims of the existing runtime test.

## Migration Acceptance

The migration is acceptable only when all of the following hold on the Kotlin compiler 2.4.10 baseline:

- The real CLI plugin-loading test compiles the fixtures successfully and executes every existing runtime/reflection assertion listed above.
- The test runs against the Kotlin compiler 2.4.10 artifacts and uses the corresponding supported language-version mode (the CLI language-version value is not a compiler patch version).
- The generated function/property signatures, visibility/modifiers, target-member resolution, injected-interface behavior, and compile-time marker handling remain unchanged. Any behavior not directly asserted by the current test must be checked against the declarations and compiler output before acceptance; do not treat a passing exit code alone as proof.
- The extension carrier class remains absent from output, and the compiler's successful result is not achieved by bypassing plugin loading or the consumer invocation.

These are acceptance conditions, not a claim that migration or verification has already occurred. The prior CodegenTest pass is not reusable as Kotlin 2.4.10 acceptance evidence.

## Author Self-Check

- Confirm changes stay within `class-extensions/kotlin-plugin` and its migration's compiler-API call sites.
- Compare generated member descriptors and visibility/modifiers, accessor behavior, target-member references, injected supertypes, annotation handling, and carrier output against this Spec.
- Run the focused CodegenTest using Kotlin 2.4.10; inspect actual generated/runtime results, not just task success.
- Keep Gradle 9.8.2 and KSP 2.3.12 unchanged. Do not add old-Kotlin compatibility, new features, or acceptance claims from historical runs.
- Revisit this Spec only if implementation evidence exposes a genuine behavior choice not resolved here; do not silently expand its scope.
