# AGENTS.md

## Project Overview

`jvm-multiplatform` is a collection of multiplatform utilities for the JVM ecosystem developed under `net.msrandom`. Instead of the usual JVM dynamic linking (separate JARs on the classpath), it enables **static linkage** — including common source sets in the same compilation unit.

The repository is a Gradle Kotlin DSL monorepo containing several cooperating subprojects:

| Subproject | Module name | Purpose |
|---|---|---|
| `virtual-source-sets/gradle-plugin` | `jvm-virtual-source-sets` | Core Gradle plugin enabling static source set linkage |
| `virtual-source-sets/idea-plugin` | `jvm-virtual-source-sets-idea` | IntelliJ IDEA plugin for IDE support |
| `java-expect-actual/annotations` | `java-expect-actual-annotations` | `@Expect`/`@Actual` annotations for Java |
| `java-expect-actual/processor` | `java-expect-actual-processor` | Java annotation processor for expect/actual |
| `java-expect-actual/idea-plugin` | `java-expect-actual-idea` | IntelliJ IDEA plugin for expect/actual |
| `kmp-actual-stubs/annotations` | `kmp-actual-stub-annotations` | `@Stub` annotation for KMP actual stubs |
| `kmp-actual-stubs/compiler-plugin` | `kmp-actual-stubs-compiler-plugin` | Kotlin compiler plugin generating actual stubs |
| `class-extensions/annotations` | `class-extension-annotations` | Annotations for class extensions |
| `class-extensions/java-processor` | `java-class-extensions-processor` | Java annotation processor for class extensions |
| `class-extensions/kotlin-plugin` | `kotlin-class-extensions-plugin` | Kotlin compiler plugin for class extensions |
| `class-extensions/gradle-plugin` | `class-extensions-gradle-plugin` | Gradle plugin wiring the Kotlin class extension plugin |
| `class-extensions/idea-plugin` | `class-extensions-idea` | IntelliJ IDEA plugin for class extensions |
| `java-processor-util` | `java-processor-util` | Shared utilities for Java annotation processors |
| `classpath-api-stubs` | `classpath-api-stubs` | Gradle plugin generating stub JARs from classpath intersections |

### Key Technologies

- **Language**: Kotlin (JVM), Groovy (Gradle tooling extension)
- **Build tool**: Gradle with Kotlin DSL (`build.gradle.kts`)
- **JVM target**: Java 8 for most modules; Java 11 for `java-processor-util`; Java 11+ for IDEA plugins
- **Kotlin compiler plugins**: Written against `kotlin-compiler-embeddable`
- **IDEA plugins**: Built with [IntelliJ Platform Gradle Plugin v2](https://github.com/JetBrains/intellij-platform-gradle-plugin) targeting IntelliJ IDEA Community 2024.3.1
- **Service registration**: `auto-service-ksp` (KSP-based)
- **Publishing**: Maven (`maven-publish` plugin), published to `https://maven.msrandom.net/`
- **CI**: GitHub Actions — publishes on push to `main` when any `gradle.properties` file changes

---

## Setup Commands

```bash
# Clone the repository
git clone https://github.com/terrarium-earth/jvm-multiplatform.git
cd jvm-multiplatform

# Build all subprojects
./gradlew build

# Build a specific subproject
./gradlew :jvm-virtual-source-sets:build
```

> On Windows use `gradlew.bat` instead of `./gradlew`.

---

## Development Workflow

### Project accessors

The build uses `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`, so cross-project dependencies are referenced as `projects.someSubprojectName` (camelCase of the module name).

### Gradle feature flags (gradle.properties)

```properties
org.gradle.configuration-cache=true
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true
```

Configuration cache is **enabled**. Avoid using APIs that are incompatible with the configuration cache (e.g., `project` at execution time, `buildSrc` task references across projects).

### JVM toolchains

All modules target **Java 8** via `kotlin { jvmToolchain(8) }` / `java.toolchain`. `java-processor-util` additionally compiles a `java11` source set at Java 11. IDEA plugins target Java 11. Make sure the appropriate JDK toolchains are available locally (Gradle auto-provisions via toolchain support).

### Dependency locking

Buildscript (classpath) configurations use `resolutionStrategy.activateDependencyLocking()`. Run the following to update locks:

```bash
./gradlew dependencies --write-locks
```

---

## Testing Instructions

Tests use JUnit Platform (JUnit 5). Each subproject with tests configures:

```kotlin
tasks.test {
    useJUnitPlatform()
}
```

```bash
# Run all tests across the entire repo
./gradlew test

# Run tests for a single subproject
./gradlew :classpath-api-stubs:test
./gradlew :kmp-actual-stubs-compiler-plugin:test
```

Gradle TestKit is used in `classpath-api-stubs` tests — test classes are in `src/test/kotlin/`.

---

## Code Style

- **Kotlin code style**: `kotlin.code.style=official` (set in root `gradle.properties`)
- Follow idiomatic Kotlin conventions (expression bodies, extension functions, etc.)
- Gradle build scripts use Kotlin DSL (`.gradle.kts`); prefer `kotlin-dsl` plugin for Gradle plugins
- IDEA plugin code targets Java 11 source/target compatibility
- Compiler plugins must be written against `kotlin-compiler-embeddable`; avoid internal APIs where possible
- Java annotation processors implement the standard `javax.annotation.processing.Processor` interface and are registered via AutoService

---

## Build and Publishing

### Build all artifacts

```bash
./gradlew build
```

### Publish to local Maven repository (for local testing)

```bash
./gradlew publishToMavenLocal
# Artifacts land in ~/.m2/repository/net/msrandom/
```

### Publish to local file-system repository (inside the repo)

```bash
./gradlew publishAllPublicationsToProjectRepository
# Artifacts land in <repo-root>/publish/
```

### Publishing targets

| Artifact type | Repository URL |
|---|---|
| Gradle plugins / IDEA plugins | `https://maven.msrandom.net/repository/cloche/` |
| Annotation processors & annotations | `https://maven.msrandom.net/repository/root/` |

Credentials are supplied via `mavenUsername` / `mavenPassword` Gradle properties (e.g., `~/.gradle/gradle.properties`).

### CI/CD

Publishing is automated via `.github/workflows/maven-publish.yml` — triggered on push to `main` when a `gradle.properties` file is modified. Requires a `MAVEN_REPO_TOKEN` Actions secret.

---

## Subproject-Specific Notes

### `virtual-source-sets/idea-plugin`

- Has an additional `gradleToolingExtension` source set compiled with Groovy
- Targets IntelliJ IDEA Community 2024.3.1 with bundled plugins `com.intellij.java` and `com.intellij.gradle`

### `java-processor-util`

- Exports two source sets: `main` (Java 8) and `java11` (Java 11+)
- Uses `java-library` plugin; consumers must declare the correct artifact variant

### `kmp-actual-stubs/compiler-plugin` and `class-extensions/kotlin-plugin`

- Depend directly on `kotlin-compiler-embeddable`
- Service registration via KSP AutoService (`dev.zacsweers.autoservice:auto-service-ksp`)

---

## Common Gotchas

- **Configuration cache**: Do not call `project.*` APIs inside task actions. Use `Provider`-based APIs or capture values at configuration time.
- **Module naming**: The Gradle project name (used in `include()`/`includeSubproject()`) does **not** always match the directory name — check `settings.gradle.kts` for the canonical mapping.
- **Kotlin compiler plugins**: When modifying compiler extensions, rebuilding may require a clean (`./gradlew clean`) due to stale compiler plugin registrations in the Gradle daemon.
- **IDEA plugin**: Run `./gradlew :jvm-virtual-source-sets-idea:runIde` (or the equivalent for other IDEA plugins) to launch a sandbox IDE instance for manual testing.
