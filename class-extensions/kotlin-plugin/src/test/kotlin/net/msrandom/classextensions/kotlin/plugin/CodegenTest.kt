package net.msrandom.classextensions.kotlin.plugin

import net.msrandom.classextensions.ClassExtension
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import kotlin.system.exitProcess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val REGISTRAR_SERVICE = "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar"

private fun sha256(file: File): String =
    MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

private fun compilerIdentity() {
    assertEquals("2.4.10", KotlinCompilerVersion.VERSION)
    assertEquals(21, System.getProperty("java.specification.version").toInt())
    println("compiler=${KotlinCompilerVersion.VERSION} source=${K2JVMCompiler::class.java.protectionDomain.codeSource.location}")
    println(
        "java.home=${System.getProperty(
            "java.home",
        )} vendor=${System.getProperty("java.vendor")} version=${System.getProperty("java.version")}",
    )
}

class CodegenTest {
    private val common = File("src/test/resources/test-common.kt").absolutePath
    private val platform = File("src/test/resources/test-platform.kt").absolutePath
    private val extraSources = listOf("test-imported.kt", "test-decoys.kt").map { File("src/test/resources/$it").absolutePath }
    private val plugin =
        File(
            ClassExtensionsPlugin::class.java.protectionDomain.codeSource.location
                .toURI(),
        )
    private val resources =
        File(
            requireNotNull(javaClass.classLoader.getResource(REGISTRAR_SERVICE)).toURI(),
        ).parentFile.parentFile.parentFile

    private fun arguments(
        output: File,
        pluginPath: String,
    ): Array<String> {
        val classpath =
            listOf(ClassExtension::class.java, Unit::class.java).joinToString(File.pathSeparator) {
                File(
                    it.protectionDomain.codeSource.location
                        .toURI(),
                ).path
            }
        return arrayOf(
            common,
            platform,
            *extraSources.toTypedArray(),
            "-language-version",
            "2.4",
            "-no-stdlib",
            "-no-reflect",
            "-classpath",
            classpath,
            "-d",
            output.path,
            "-Xplugin=$pluginPath",
            "-Xmulti-platform",
            "-Xfragments=main,platform",
            "-Xfragment-refines=platform:main",
            "-Xfragment-sources=main:$common",
            "-Xfragment-sources=platform:$platform",
            *extraSources.map { "-Xfragment-sources=platform:$it" }.toTypedArray(),
        )
    }

    @Test
    fun `CLI loads plugin and executes generated members`() {
        val output = Files.createTempDirectory("class-extension-consumer").toFile()
        try {
            compilerIdentity()
            val args = arguments(output, "${plugin.path},${resources.path}")
            println("directory CLI args=${args.toList()}")
            val messages = ByteArrayOutputStream()
            val exit = K2JVMCompiler().exec(PrintStream(messages), *args)
            assertEquals(ExitCode.OK, exit, messages.toString())
            assertOutput(output)
        } finally {
            output.deleteRecursively()
        }
    }

    @Test
    fun `CLI loads the KSP packaged JAR without plugin directories`() {
        val jar = File(requireNotNull(System.getProperty("extensions.plugin.jar"))).canonicalFile
        val generatedService = File("build/generated/ksp/main/resources/$REGISTRAR_SERVICE")
        val serviceText = generatedService.readText().trim()
        assertEquals(ClassExtensionsPlugin::class.java.name, serviceText)
        JarFile(jar).use { archive ->
            assertEquals(
                serviceText,
                archive
                    .getInputStream(requireNotNull(archive.getJarEntry(REGISTRAR_SERVICE)))
                    .bufferedReader()
                    .readText()
                    .trim(),
            )
        }
        val hash = sha256(jar)
        val compilerClasspath =
            requireNotNull(System.getProperty("extensions.compiler.classpath"))
                .split(File.pathSeparator)
                .map { File(it).canonicalFile }
                .filter { it != plugin.canonicalFile && it != resources.canonicalFile && it != jar }
        assertTrue(compilerClasspath.none { File(it, "net/msrandom/classextensions/kotlin/plugin/ClassExtensionsPlugin.class").exists() })
        val evidence = File("build/extensions-acceptance").absoluteFile.apply { mkdirs() }
        val output = Files.createTempDirectory("class-extension-packaged").toFile()
        try {
            val args = arguments(output, jar.path)
            val command =
                listOf(
                    File(System.getProperty("java.home"), "bin/java").path,
                    "-Xlog:class+load=info:file=class-load.log",
                    "-cp",
                    compilerClasspath.joinToString(File.pathSeparator),
                    PackagedCompiler::class.java.name,
                    jar.path,
                    hash,
                    *args,
                )
            println("packaged command=$command")
            val log = File(evidence, "compiler.log")
            val process =
                ProcessBuilder(command)
                    .directory(evidence)
                    .redirectErrorStream(true)
                    .redirectOutput(log)
                    .start()
            assertTrue(process.waitFor(120, TimeUnit.SECONDS), "Packaged compiler timed out; see $log")
            val messages = log.readText()
            println(messages)
            assertEquals(0, process.exitValue(), messages)
            val loaded = File(evidence, "class-load.log").readLines()
            for (name in listOf(
                ClassExtensionsPlugin::class.java.name,
                ClassExtensionFirDeclarations::class.java.name,
                ExcludeClassExtensionsIrGenerationExtension::class.java.name,
            )) {
                val line = loaded.single { it.contains(" $name source: ") }
                assertEquals(jar, File(java.net.URI(line.substringAfter("source: ").trim())).canonicalFile, line)
                println("actual compiler load: $line")
            }
            assertEquals(hash, sha256(jar), "Plugin changed during compilation")
            assertOutput(output)
            println("KSP service=$serviceText jar=$jar sha256=$hash")
        } finally {
            output.deleteRecursively()
        }
    }

    private fun assertOutput(output: File) {
        fun bytecode(name: String): ClassNode =
            ClassNode().also {
                ClassReader(File(output, name.replace('.', '/') + ".class").readBytes()).accept(it, 0)
            }

        fun method(
            node: ClassNode,
            name: String,
            descriptor: String,
            modifiers: Int,
        ) {
            val member = node.methods.single { it.name == name && it.desc == descriptor }
            val mask =
                Opcodes.ACC_PUBLIC or Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or
                    Opcodes.ACC_ABSTRACT or
                    Opcodes.ACC_NATIVE
            assertEquals(modifiers, member.access and mask, "${node.name}.$name$descriptor modifiers")
            println("member=${node.name}.$name$descriptor access=${member.access}")
        }
        val targetNode = bytecode("a.b.c.A")
        assertEquals(listOf("a/b/c/I"), targetNode.interfaces, "Existing interface must not be duplicated")
        method(targetNode, "injected", "()I", Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL)
        method(targetNode, "hi", "()Ljava/lang/String;", Opcodes.ACC_PUBLIC)
        method(targetNode, "getMutable", "()I", Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL)
        method(targetNode, "setMutable", "(I)V", Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL)
        val c = targetNode.fields.single { it.name == "c" }
        assertEquals("I", c.desc)
        assertEquals(Opcodes.ACC_PRIVATE, c.access)
        val mutable = targetNode.fields.single { it.name == "mutable" }
        assertEquals("I", mutable.desc)
        assertEquals(Opcodes.ACC_PRIVATE, mutable.access)
        val packageTargetNode = bytecode("a.b.c.PackageTarget")
        method(packageTargetNode, "packageResolved", "()I", Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL)
        val imported = bytecode("a.b.c.ImportedTarget")
        assertEquals(listOf("a/b/c/Added"), imported.interfaces, "Extension must add the new interface exactly once")
        method(imported, "added", "()I", Opcodes.ACC_PUBLIC)
        method(imported, "getReadOnly", "()I", Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL)
        method(imported, "getDelegated", "()I", Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL)
        method(imported, "setDelegated", "(I)V", Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL)
        method(imported, "echo", "(Ljava/lang/String;)Ljava/lang/String;", Opcodes.ACC_PUBLIC)
        method(imported, "protectedValue", "()I", Opcodes.ACC_PROTECTED or Opcodes.ACC_FINAL)
        method(imported, "withTarget", "(Ljava/lang/String;)Ljava/lang/String;", Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL)
        assertFalse(imported.methods.any { it.name in setOf("setReadOnly", "documentaryOnly", "unmarked", "getUnmarkedProperty") })
        assertFalse(imported.fields.any { it.name == "unmarkedProperty" })
        val readOnlyField = imported.fields.single { it.name == "readOnly" }
        assertEquals("I", readOnlyField.desc)
        assertEquals(Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL, readOnlyField.access)
        val delegatedField = imported.fields.single { it.name == "delegated" }
        assertEquals("I", delegatedField.desc)
        assertEquals(Opcodes.ACC_PRIVATE, delegatedField.access)
        assertEquals(1, imported.fields.count { it.name == "seed" }, "Shadow must use the target field")
        assertEquals(1, imported.fields.count { it.name == "state" }, "Shadow must use the target field")
        for (node in listOf(targetNode, imported, packageTargetNode)) {
            val annotations =
                node.methods.flatMap { it.visibleAnnotations.orEmpty() + it.invisibleAnnotations.orEmpty() } +
                    node.fields.flatMap { it.visibleAnnotations.orEmpty() + it.invisibleAnnotations.orEmpty() }
            assertFalse(
                annotations.any {
                    it.desc == "Lnet/msrandom/classextensions/ExtensionInject;"
                },
                "Selection marker must be removed from ${node.name}",
            )
        }
        for (name in listOf("A", "ImportedTarget", "PackageTarget")) {
            val decoy = bytecode("a.b.decoys.$name")
            assertEquals(listOf("<init>"), decoy.methods.map { it.name }, "Nonmatching owner must not receive members")
            assertTrue(decoy.fields.isEmpty())
            assertTrue(decoy.interfaces.isEmpty())
        }
        URLClassLoader(arrayOf(output.toURI().toURL()), javaClass.classLoader).use { loader ->
            val target = loader.loadClass("a.b.c.A")
            val instance = target.getConstructor().newInstance()
            assertEquals(42, target.getMethod("injected").invoke(instance))
            assertEquals("hi", loader.loadClass("a.b.c.I").getMethod("hi").invoke(instance))
            assertEquals(42, loader.loadClass("a.b.c.Test_commonKt").getMethod("consume").invoke(null))
            assertEquals(Int::class.javaPrimitiveType, target.getDeclaredField("c").type)
            assertEquals(7, target.getMethod("getMutable").invoke(instance))
            target.getMethod("setMutable", Int::class.javaPrimitiveType).invoke(instance, 9)
            assertEquals(9, target.getMethod("getMutable").invoke(instance))
            val importedTarget = loader.loadClass("a.b.c.ImportedTarget")
            val importedInstance = importedTarget.getConstructor().newInstance()
            assertEquals(10, loader.loadClass("a.b.c.Added").getMethod("added").invoke(importedInstance))
            assertEquals(10, loader.loadClass("a.b.c.Test_commonKt").getMethod("consumeAdded").invoke(null))
            assertEquals(13, importedTarget.getMethod("getReadOnly").invoke(importedInstance))
            assertEquals(3, importedTarget.getMethod("getDelegated").invoke(importedInstance))
            importedTarget.getMethod("setDelegated", Int::class.javaPrimitiveType).invoke(importedInstance, 9)
            assertEquals(19, importedTarget.getMethod("getState").invoke(importedInstance))
            assertEquals(19, importedTarget.getMethod("getDelegated").invoke(importedInstance))
            assertEquals(29, importedTarget.getMethod("getReadOnly").invoke(importedInstance))
            assertEquals("echo10", importedTarget.getMethod("echo", String::class.java).invoke(importedInstance, "echo"))
            assertEquals("receiver10", importedTarget.getMethod("withTarget", String::class.java).invoke(importedInstance, "receiver"))
            assertEquals(10, importedTarget.getDeclaredMethod("protectedValue").apply { isAccessible = true }.invoke(importedInstance))
            val packageTarget = loader.loadClass("a.b.c.PackageTarget")
            assertEquals(23, packageTarget.getMethod("packageResolved").invoke(packageTarget.getConstructor().newInstance()))
        }
        for (carrier in listOf("a/b/c/AExtension", "a/b/c/PackageExtension", "a/b/extensions/ImportedExtension")) {
            assertFalse(File(output, "$carrier.class").exists(), "Extension carrier must be removed by IR plugin")
        }
        println(
            "assertions passed: original 42/hi/common42/Int/mutable7->9/carrier; package/import/nonmatching; exact descriptors/modifiers; val/custom var/extension receiver/shadow; existing/new interfaces; selection markers/unmarked/documentary/carriers",
        )
    }
}

object PackagedCompiler {
    @JvmStatic
    fun main(args: Array<String>) {
        compilerIdentity()
        val jar = File(args[0])
        assertEquals(args[1], sha256(jar))
        println("packaged plugin=$jar sha256=${sha256(jar)} CLI args=${args.drop(2)}")
        exitProcess(K2JVMCompiler().exec(System.out, *args.drop(2).toTypedArray()).code)
    }
}
