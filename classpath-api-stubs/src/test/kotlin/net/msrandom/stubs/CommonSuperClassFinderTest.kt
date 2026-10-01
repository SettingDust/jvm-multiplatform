package net.msrandom.stubs

import org.junit.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.io.path.outputStream
import kotlin.test.assertEquals

class CommonSuperClassFinderTest {
    @Test
    fun `common ancestor is independent of three input order`() {
        val jars = (1..3).map { fixtureJar("C", "java/lang/Object") }
        val loaders = jars.map { StubGenerator.ClasspathLoader(listOf(it.toFile()), emptyList()) }
        try {
            val nodes = listOf("A", "B", "D").map { ClassNode().also { node -> node.superName = "C" } }
            assertEquals(
                "C",
                CommonSuperClassFinder.findCommonSuper(nodes, loaders),
            )
            assertEquals(
                "C",
                CommonSuperClassFinder.findCommonSuper(nodes.reversed(), loaders.reversed()),
            )
        } finally {
            loaders.forEach { it.close() }
            jars.forEach { it.toFile().delete() }
        }
    }

    @Test
    fun `missing parent conservatively falls back to Object`() {
        val jar = fixtureJar("Unused", "java/lang/Object")
        val loader = StubGenerator.ClasspathLoader(listOf(jar.toFile()), emptyList())
        try {
            val nodes = listOf(ClassNode().also { it.superName = "missing/Parent" })
            assertEquals("java/lang/Object", CommonSuperClassFinder.findCommonSuper(nodes, listOf(loader)))
        } finally {
            loader.close()
            jar.toFile().delete()
        }
    }

    private fun fixtureJar(
        name: String,
        superName: String,
    ) = createTempDirectory().resolve("fixture.jar").also { path ->
        JarOutputStream(path.outputStream()).use { jar ->
            jar.putNextEntry(JarEntry("$name.class"))
            val writer = ClassWriter(0)
            writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, superName, null)
            writer.visitEnd()
            jar.write(writer.toByteArray())
            jar.closeEntry()
        }
    }
}
