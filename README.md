# jvm-multiplatform

## Fork Changes
This fork adds the following committed changes relative to `upstream/main`.

- **Classpath API stubs:** generation now processes class entries in parallel, releases JAR and bytecode-cache resources, and adds a `preserveMethodBodies` task property (disabled by default) for methods with identical bytecode. Intersection also retains more signature, annotation, parameter, and legacy-frame information. ([e77650d](https://github.com/SettingDust/jvm-multiplatform/commit/e77650d), [8455991](https://github.com/SettingDust/jvm-multiplatform/commit/8455991), [4306f26](https://github.com/SettingDust/jvm-multiplatform/commit/4306f26), [085d931](https://github.com/SettingDust/jvm-multiplatform/commit/085d931), [1e7eb3c](https://github.com/SettingDust/jvm-multiplatform/commit/1e7eb3c))
- **KMP actual stubs:** when K2 compiles a common source fragment with a matching refining-fragment `actual`, the compiler plugin retains its `expect` declaration; it also suppresses false `ACTUAL_WITHOUT_EXPECT` diagnostics for actual declarations. ([b9873eb](https://github.com/SettingDust/jvm-multiplatform/commit/b9873eb))
- **Build and publishing:** the wrapper is on Gradle 9.4.1, with KSP 2.3.6 and IntelliJ Platform Gradle Plugin 2.14.0. Maven publication runs from `main` after `gradle.properties` changes or by manual dispatch, using JDK 21. ([a8bd0dc](https://github.com/SettingDust/jvm-multiplatform/commit/a8bd0dc), [92e9cc7](https://github.com/SettingDust/jvm-multiplatform/commit/92e9cc7))

### A collection of multiplatform utilities for the JVM

These utilities use static linkage for handling common code, rather than dynamic linking (JARs in the classpath) which is what's usually used in the JVM Ecosystem

## Virtual SourceSets
The base for any jvm-multiplatform utility to work, as it allows the statically linked source sets to work
- In Java, by including the common code in the same compilation
- In Kotlin by using a Kotlin Multiplatform structure while still compiling only for the JVM platform

```groovy
sourceSets {
    platform {
        // Add main as a common, while the platform source set is the one expected to be runnable
        staticLinkage.link(sourceSets.main)
    }
}
```

## Java
The jvm-multiplatform collection includes @Expect/@Actual annotations and class extensions for the Java platform, examples are as follows:

### Expect/Actual
Allow replacing expected platform stubs with actualized platform logic, similarly to kotlin's expect/actual

```java
// Common.java
package a.b.c;

import net.msrandom.multiplatform.annotations.Expect;

@Expect
public class Common {
    public final int field;

    public Common(int value);

    public int getter();
}
```

```java
// CommonActual.java
package a.b.c;

import net.msrandom.multiplatform.annotations.Actual;

@Actual
public class CommonActual {
    @Actual
    public final int field;

    @Actual
    public CommonActual(int value) {
        this.field = value;
    }

    @Actual
    public int getter() {
        return privateGet();
    }

    private int privateGet() {
        return field;
    }
}
```

Alternatively, you can do static fields/methods instead of full classes/types
```java
// Common.java
package a.b.c;

import net.msrandom.multiplatform.annotations.Expect;

public class Common {
    @Expect
    public static final float CONSTANT;

    @Expect
    public static void printConstant();
}
```

```java
// CommonActual.java
package a.b.c;

import net.msrandom.multiplatform.annotations.Actual;

public class CommonActual {
    @Actual
    public static final float CONSTANT = 3.14;

    @Actual
    public static void printConstant() {
        System.out.println(CONSTANT);
    }
}
```

Can be configured as follows:
```groovy
// Assuming the previous structure of common main + platform source set
tasks.compileJava {
    options.compilerArgs.add("-AgenerateExpectStubs")
}

dependencies {
    compileOnly("net.msrandom:java-expect-actual-annotations:1.0.0")
    annotationProcessor("net.msrandom:java-expect-actual-processor:1.0.9")
}
```

### Class Extensions
More granular than @Expect/@Actual, but allows injecting methods and fields into a base class and shadowing existing members. Useful for interface injection, along with adding platform specific code.

```java
// Common.java
package a.b.c;

public class Common {
    public final int field;

    public Common(int value) {
        this.field = value;
    }

    public void printExtensionElement() {
        System.out.println(this.injected);
    }
}
```

```java
// CommonExtension.java
package a.b.c;

import net.msrandom.classextensions.ClassExtension;
import net.msrandom.classextensions.ExtensionShadow;
import net.msrandom.classextensions.ExtensionInject;

@ClassExtension(Common.class)
public class CommonExtension implements SomeInterface {
    @ExtensionShadow
    public final int field;

    @ExtensionInject
    private String injected;

    @ExtensionInject
    public CommonExtension() {
        this(0);
    }

    @ExtensionInject
    @Override
    public void interfaceMethod() {
        System.out.println("Extension injected");
    }
}
```

The Java version of Class extensions can be applied using by adding the annotation processor
```groovy
// Assuming the previous structure of common main + platform source set
dependencies {
    platformCompileOnly("net.msrandom:class-extension-annotations:1.0.0")
    platformAnnotationProcessor("net.msrandom:java-class-extensions-processor:1.0.0")
}
```

## Kotlin
For kotlin, while you already have expect/actual, you also get class extensions, along with a stub annotation processor that will allow you to compile common sources to the JVM platform

### Class Extensions
Very similar to Java's, allows injecting properties, constructors and methods to common classes

```kotlin
// Common.kt
package a.b.c

class Common(val field: Int)
```

```kotlin
// CommonExtension.kt
package a.b.c

@ClassExtension(Common::class)
class CommonExtension : SomeInterface {
    @ExtensionShadow
    val field: Int = 0

    @ExtensionInject
    override fun interfaceMethod() {
        println("Extension injected")
    }
}
```

The Kotlin version of Class extensions can be applied using a Gradle Plugin
```groovy
plugins {
    id "net.msrandom.classextensions" version "1.0.11"
}
```

### KMP Stubs
To allow compiling common source sets to the JVM bytecode format:

```kotlin
import net.msrandom.stub.Stub

// Will generate `actual fun myFunction(): Int = TODO()`
@Stub
expect fun myFunction(): Int
```
