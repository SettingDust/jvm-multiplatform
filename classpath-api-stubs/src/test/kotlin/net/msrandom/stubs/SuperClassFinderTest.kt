package net.msrandom.stubs

import org.junit.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals

/**
 * Unit tests for [CommonSuperClassFinder.findCommonSuper].
 *
 * Each test builds small in-memory JARs via ASM, creates [StubGenerator.ClasspathLoader]
 * instances pointing at those JARs, and asserts the resolved common superclass.
 *
 * Scenarios covered:
 *  1. Both versions share the same direct parent — returned immediately.
 *  2. Chains converge at an intermediate ancestor.
 *  3. A parent class is missing from one classpath (the Registry regression case):
 *     the algorithm must NOT return the version-local parent; fall back to java/lang/Object.
 *  4. Parent classes missing from both classpaths — fall back to java/lang/Object.
 *  5. Both chains eventually reach Object (null superName) — return java/lang/Object.
 */
class SuperClassFinderTest {

    // ── helpers ────────────────────────────────────────────────────────────────

    /** Produces raw ASM bytecode for a class with the given name and superName. */
    private fun classBytes(name: String, superName: String? = "java/lang/Object"): ByteArray {
        val cw = ClassWriter(0)
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT, name, null, superName, null)
        cw.visitEnd()
        return cw.toByteArray()
    }

    /**
     * Writes [classes] into a temporary ZIP/JAR file and returns a
     * [StubGenerator.ClasspathLoader] backed by that file.
     * The returned loader **must be closed** by the caller.
     */
    private fun loaderOf(vararg classes: Pair<String, ByteArray>): StubGenerator.ClasspathLoader {
        val jar = File.createTempFile("stub-test-cp-", ".jar").also { it.deleteOnExit() }

        ZipOutputStream(jar.outputStream()).use { zip ->
            for ((name, bytes) in classes) {
                zip.putNextEntry(ZipEntry("$name.class"))
                zip.write(bytes)
                zip.closeEntry()
            }
        }

        return StubGenerator.ClasspathLoader(listOf(jar), emptyList())
    }

    /** Returns a [ClassNode] for [name] with the given [superName]. */
    private fun node(name: String, superName: String? = "java/lang/Object"): ClassNode =
        ClassNode().also { cn ->
            ClassNode().apply {
                cn.name = name
                cn.superName = superName
            }
            cn.name = name
            cn.superName = superName
        }

    // ── tests ──────────────────────────────────────────────────────────────────

    @Test
    fun `identical direct parents are returned immediately`() {
        val parentBytes = classBytes("example/Parent")

        val cpA = loaderOf("example/Parent" to parentBytes)
        val cpB = loaderOf("example/Parent" to parentBytes)

        cpA.use { a ->
            cpB.use { b ->
                val result = CommonSuperClassFinder.findCommonSuper(
                    node("example/ClassA", "example/Parent"),
                    node("example/ClassB", "example/Parent"),
                    a,
                    b,
                )
                assertEquals("example/Parent", result)
            }
        }
    }

    @Test
    fun `chains converge at intermediate common ancestor`() {
        // A-side chain: ClassA → IntermA → CommonBase → Object
        // B-side chain: ClassB → IntermB → CommonBase → Object
        val commonBaseBytes = classBytes("example/CommonBase")
        val intermABytes    = classBytes("example/IntermA",   "example/CommonBase")
        val intermBBytes    = classBytes("example/IntermB",   "example/CommonBase")

        val cpA = loaderOf(
            "example/IntermA"   to intermABytes,
            "example/CommonBase" to commonBaseBytes,
        )
        val cpB = loaderOf(
            "example/IntermB"   to intermBBytes,
            "example/CommonBase" to commonBaseBytes,
        )

        cpA.use { a ->
            cpB.use { b ->
                val result = CommonSuperClassFinder.findCommonSuper(
                    node("example/ClassA", "example/IntermA"),
                    node("example/ClassB", "example/IntermB"),
                    a,
                    b,
                )
                assertEquals("example/CommonBase", result)
            }
        }
    }

    /**
     * Regression: mirrors the `net.minecraft.core.Registry` scenario.
     *
     * In version A, Registry extends VersionABase which is NOT present in the
     * classpath (e.g., excluded library class or remapping artefact). In version B,
     * Registry extends java/lang/Object directly.
     *
     * Before the fix the algorithm returned "example/VersionABase" — a class that
     * does not exist in version B's classpath. After the fix it must return
     * "java/lang/Object" as the universal safe ancestor.
     */
    @Test
    fun `missing parent in one classpath falls back to java-lang-Object`() {
        // cpA contains VersionABase; cpB does NOT
        val versionABaseBytes = classBytes("example/VersionABase")

        val cpA = loaderOf("example/VersionABase" to versionABaseBytes)
        // cpB intentionally empty — Registry in version B extends Object directly
        val cpB = loaderOf()

        cpA.use { a ->
            cpB.use { b ->
                val result = CommonSuperClassFinder.findCommonSuper(
                    node("net/minecraft/core/Registry", "example/VersionABase"),
                    node("net/minecraft/core/Registry", "java/lang/Object"),
                    a,
                    b,
                )
                assertEquals("java/lang/Object", result,
                    "Should fall back to java/lang/Object when a parent class is " +
                    "only resolvable in one version's classpath")
            }
        }
    }

    @Test
    fun `missing parents in both classpaths falls back to java-lang-Object`() {
        // Neither classpath contains the declared parent classes
        val cpA = loaderOf()  // no entries
        val cpB = loaderOf()

        cpA.use { a ->
            cpB.use { b ->
                val result = CommonSuperClassFinder.findCommonSuper(
                    node("example/ClassA", "example/MissingA"),
                    node("example/ClassB", "example/MissingB"),
                    a,
                    b,
                )
                assertEquals("java/lang/Object", result)
            }
        }
    }

    @Test
    fun `chains that both reach Object return java-lang-Object`() {
        // A-side: ClassA → IntermA → Object
        // B-side: ClassB → IntermB → Object
        // Neither intermediate class is in the other's chain.
        val intermABytes = classBytes("example/IntermA", "java/lang/Object")
        val intermBBytes = classBytes("example/IntermB", "java/lang/Object")
        val objectBytes  = classBytes("java/lang/Object", null)

        val cpA = loaderOf(
            "example/IntermA" to intermABytes,
            "java/lang/Object" to objectBytes,
        )
        val cpB = loaderOf(
            "example/IntermB" to intermBBytes,
            "java/lang/Object" to objectBytes,
        )

        cpA.use { a ->
            cpB.use { b ->
                val result = CommonSuperClassFinder.findCommonSuper(
                    node("example/ClassA", "example/IntermA"),
                    node("example/ClassB", "example/IntermB"),
                    a,
                    b,
                )
                assertEquals("java/lang/Object", result)
            }
        }
    }
}
