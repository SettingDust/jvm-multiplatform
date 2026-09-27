# Shared JVM Sources and Compilation Views Context

This bounded context describes how shared JVM sources enter platform compilation and how common referenceable structures and platform implementations are formed. It does not own Minecraft versions, loader targets, game artifact naming, or Mod packaging.

## Language

**shared source**: Source intended to participate in more than one platform compilation through the linkage or implementation mechanisms defined here. It is not a compiled common JAR by definition. [Tool scope](README.md#L1)

**SourceSet static linkage**: A platform SourceSet compiles sources from its linked dependency SourceSets together with its own sources. It is not adding a compiled common JAR to a runtime classpath. [Linkage relationship](virtual-source-sets/gradle-plugin/src/main/kotlin/net/msrandom/virtualsourcesets/SourceSetStaticLinkageInfo.kt#L9)

**weak tree link**: A Kotlin K2 source-dependency-tree supplement. It does not replace static linkage and does not define dynamic or runtime linkage. [Linkage explanation](README.md#L5)

**common API intersection**: A compilation view formed from classes, members, and signatures shared across multiple classpaths, with separate handling for excluded modules. It is not a Gradle `api` dependency or Minecraft's game common view, and it does not guarantee runtime semantic compatibility. [Intersection inputs](classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/GenerateStubApi.kt#L27) [Member selection](classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/ClassNodeIntersector.kt#L28)

**Java expect/actual**: Compile-time matching between shared Java declarations and platform implementations. Missing or multiple implementations can be rejected; an allowed expect stub is not a runnable platform implementation. [Matching](java-expect-actual/processor/src/main/kotlin/net/msrandom/multiplatform/ExpectActualProcessor.kt#L43)

**Kotlin expect/actual**: Kotlin's platform declaration and implementation mechanism, distinct from Java annotation processing. [Kotlin usage](README.md#L20)

**stub**: A generated placeholder used to compile shared sources separately. Kotlin `@Stub` bodies throw rather than provide business behavior, and an expect stub is not a platform implementation. [Stub bodies](kmp-actual-stubs/compiler-plugin/src/main/kotlin/net/msrandom/stub/ir/IrStubBodyGenerator.kt#L27) [Expect stubs](java-expect-actual/processor/src/main/kotlin/net/msrandom/multiplatform/ExpectActualProcessor.kt#L43)

**class extension**: A mechanism for adding platform-specific members or behavior to a shared class. It is distinct from replacing an expect declaration. [Extension examples](README.md#L102)

**platform implementation**: A platform-specific declaration or behavior supplied for shared code. Its existence does not prove that the common API intersection has identical runtime semantics across platforms. [Common structures](classpath-api-stubs/src/main/kotlin/net/msrandom/stubs/StubGenerator.kt#L55)

## Unresolved

- The inspected source does not establish a unified user contract for zero classpath inputs or SourceSet dependency cycles.
- The intersection algorithm is not a complete binary or semantic compatibility guarantee.
- Cloche translates its target relationships into this context's linkage and intersection inputs, but cross-repository ownership and version coordination are not established.
