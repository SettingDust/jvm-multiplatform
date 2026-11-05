package net.msrandom.stub.tests

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode

class Tests {
    @OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
    @Test
    fun `Plugin identity preserves fork ID`() {
        assertEquals("net.msrandom.stub.actual", net.msrandom.stub.PLUGIN_ID)
        assertEquals(net.msrandom.stub.PLUGIN_ID, net.msrandom.stub.StubCompilerPlugin().pluginId)
    }

    @Test
    fun `Test Plugin`() {
        val plugin = File(requireNotNull(System.getProperty("callable.plugin")))
        val annotations = File(requireNotNull(System.getProperty("smoke.annotations")))
        assertTrue(plugin.isFile, "Missing plugin JAR: $plugin")
        assertTrue(annotations.isFile, "Missing annotations JAR: $annotations")
        val classpath =
            listOf(requireNotNull(System.getProperty("smoke.classpath")), annotations.absolutePath)
                .joinToString(File.pathSeparator)
        val source = File("src/test/resources/test.kt").absoluteFile
        val root = File(requireNotNull(System.getProperty("callable.evidence")), "smoke").apply { mkdirs() }
        val output = File(root, "out")
        assertTrue(!output.exists() || output.deleteRecursively(), "Cannot clear smoke output: $output")
        val arguments =
            arrayOf(
                source.absolutePath,
                "-language-version",
                "2.1",
                "-api-version",
                "2.1",
                "-jvm-target",
                "1.8",
                "-no-stdlib",
                "-no-reflect",
                "-classpath",
                classpath,
                "-Xplugin=${plugin.absolutePath}",
                "-Xmulti-platform",
                "-Xfragments=main",
                "-Xfragment-sources=main:${source.absolutePath}",
                "-d",
                output.absolutePath,
            )
        val diagnostics = ByteArrayOutputStream()
        val exit = PrintStream(diagnostics).use { K2JVMCompiler().exec(it, *arguments) }
        val log = "compiler=${KotlinCompilerVersion.VERSION} jvm=${System.getProperty("java.version")}\n" +
            "compilerSource=${K2JVMCompiler::class.java.protectionDomain.codeSource.location}\n" +
            "java.home=${System.getProperty("java.home")}\njava.vendor=${System.getProperty("java.vendor")}\n" +
            "${arguments.joinToString("\n")}\nEXIT=$exit\n$diagnostics"
        File(root, "compile.log").writeText(log)
        println(log)
        assertEquals("2.4.10", KotlinCompilerVersion.VERSION, log)
        assertEquals(ExitCode.OK, exit, log)
        for (name in listOf("TestKt", "ExpectAnnotation", "ExpectClass", "ExpectItf", "ExpectObj", "ExpectEnum")) {
            assertTrue(File(output, "$name.class").isFile, "Missing generated $name.class\n$log")
        }
        // Top-level property stubs throw during TestKt initialization; inspect the function body directly.
        val facade = ClassNode().also { ClassReader(File(output, "TestKt.class").readBytes()).accept(it, 0) }
        val function = assertNotNull(facade.methods.singleOrNull {
            it.name == "expectFunction" && it.desc == "()Ljava/lang/String;"
        }, log)
        val code = function.instructions.toArray()
        assertTrue(code.any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" }, log)
        assertTrue(code.any { it is LdcInsnNode && it.cst == "Common modules are not runnable" }, log)
        assertTrue(code.any { it.opcode == Opcodes.ATHROW }, log)
        val assertions = "ASSERTIONS=ExitCode.OK; six expected classes; expectFunction bytecode throws UnsupportedOperationException: Common modules are not runnable"
        File(root, "compile.log").appendText("\n$assertions\n")
        println(assertions)
    }
}
