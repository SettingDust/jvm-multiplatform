package net.msrandom.stubs

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import org.objectweb.asm.*
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.test.*

class InheritanceGenerationTest {
    private val root = Files.createTempDirectory("inheritance-generation-").toFile()
    private val project by lazy { ProjectBuilder.builder().withProjectDir(root).build() }

    private fun bytes(
        bound: String = "java/lang/Object",
        variable: String = "T",
        body: Int = 1,
        parent: String = "java/lang/Object",
        interfaces: Array<String> = emptyArray(),
        name: String = "Fixture",
    ): ByteArray {
        val writer = ClassWriter(0)
        writer.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT,
            name,
            "<$variable:L$bound;>L$parent;" + interfaces.joinToString("") { "L$it;" },
            parent,
            interfaces,
        )
        writer.visitField(Opcodes.ACC_PUBLIC, "value", "Ljava/lang/Object;", "T$variable;", null).visitEnd()
        writer.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT, "value", "()Ljava/lang/Object;", "()T$variable;", null).visitEnd()
        writer.visitField(Opcodes.ACC_PUBLIC, "array", "[Ljava/lang/Object;", "[T$variable;", null).visitEnd()
        writer.visitField(Opcodes.ACC_PUBLIC, "badErasure", "Ljava/lang/Integer;", "Ljava/lang/String;", null).visitEnd()
        writer.visitField(Opcodes.ACC_PUBLIC, "dangling", "Ljava/util/List;", "Ljava/util/List<TAbsent;>;", null).visitEnd()
        writer
            .visitMethod(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT,
                "echo",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                "<U:Ljava/lang/Object;>(TU;)TU;",
                null,
            ).visitEnd()
        writer
            .visitMethod(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT,
                "badErasure",
                "()Ljava/lang/Integer;",
                "()Ljava/lang/String;",
                null,
            ).visitEnd()
        writer.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "body", "()I", null, null).apply {
            visitCode()
            visitIntInsn(Opcodes.BIPUSH, body)
            visitInsn(Opcodes.IRETURN)
            visitMaxs(1, 0)
            visitEnd()
        }
        writer.visitEnd()
        return writer.toByteArray()
    }

    private fun jar(
        name: String,
        entries: Map<String, ByteArray>,
    ): File =
        File(root, name).also { file ->
            JarOutputStream(file.outputStream()).use { output ->
                entries.forEach { (name, bytes) ->
                    output.putNextEntry(JarEntry("$name.class"))
                    output.write(bytes)
                    output.closeEntry()
                }
            }
        }

    private fun generate(
        inputs: List<File>,
        name: String,
    ): File {
        val artifacts =
            inputs.map { input ->
                listOf(project.objects.newInstance(GenerateStubApi.ResolvedArtifact::class.java).apply { file.set(input) })
            }
        return File(root, name).also { StubGenerator.generateStub(artifacts, emptyList(), it.toPath()) }
    }

    private fun read(file: File): ClassNode =
        JarFile(file).use { jar ->
            ClassNode().also { ClassReader(jar.getInputStream(jar.getJarEntry("Fixture.class"))).accept(it, 0) }
        }

    private fun <T> permutations(items: List<T>): List<List<T>> =
        if (items.isEmpty()) {
            listOf(emptyList())
        } else {
            items.flatMap { first ->
                permutations(items - first).map { listOf(first) + it }
            }
        }

    private fun checkSignatures(node: ClassNode) {
        assertNull(node.fields.first { it.name == "badErasure" }.signature)
        assertNull(node.fields.first { it.name == "dangling" }.signature)
        assertNull(node.methods.first { it.name == "badErasure" }.signature)
        assertEquals("<U:Ljava/lang/Object;>(TU;)TU;", node.methods.first { it.name == "echo" }.signature)
        node.signature?.let { value ->
            val writer = SignatureWriter()
            SignatureReader(value).accept(writer)
            assertEquals(value, writer.toString())
            assertEquals(
                value,
                net.msrandom.stubs.signature.SignatureIntersector
                    .validClass(value, node.superName, node.interfaces),
            )
        }
        node.fields.forEach { field ->
            field.signature?.let { value ->
                val writer = SignatureWriter()
                SignatureReader(value).acceptType(writer)
                assertEquals(value, writer.toString())
                assertEquals(
                    value,
                    net.msrandom.stubs.signature.SignatureIntersector
                        .validMember(value, field.desc, node.signature, false),
                )
            }
        }
        node.methods.forEach { method ->
            method.signature?.let { value ->
                val writer = SignatureWriter()
                SignatureReader(value).accept(writer)
                assertEquals(value, writer.toString())
                assertEquals(
                    value,
                    net.msrandom.stubs.signature.SignatureIntersector
                        .validMember(value, method.desc, node.signature, true),
                )
            }
        }
    }

    @Test fun `generator permutations headers signatures and actual consumers`() {
        val interfaces = arrayOf("java/io/Serializable", "java/lang/Cloneable")
        val inputs =
            (0..2).map { index ->
                jar(
                    "generic$index.jar",
                    mapOf("Fixture" to bytes(interfaces = if (index == 1) interfaces.reversedArray() else interfaces)),
                )
            }
        var generic: File? = null
        for (size in 2..3) {
            for ((index, order) in permutations(inputs.take(size)).withIndex()) {
                val output = generate(order, "generic-$size-$index.jar")
                val node = read(output)
                assertEquals(interfaces.toSet(), node.interfaces.toSet())
                assertEquals("java/lang/Object", node.superName)
                assertNotNull(node.signature)
                assertEquals("TT;", node.fields.first { it.name == "value" }.signature)
                assertEquals("()TT;", node.methods.first { it.name == "value" }.signature)
                assertTrue(node.methods.all { it.instructions.size() == 0 })
                checkSignatures(node)
                generic = output
            }
        }
        val renamed = jar("renamed.jar", mapOf("Fixture" to bytes(variable = "U", interfaces = interfaces)))
        for (order in permutations(listOf(inputs[0], renamed))) {
            val node = read(generate(order, "renamed-out.jar"))
            assertNotNull(node.signature)
            assertNull(node.fields.first { it.name == "value" }.signature)
            assertNull(node.methods.first { it.name == "value" }.signature)
            checkSignatures(node)
        }
        val conflicting = jar("conflicting.jar", mapOf("Fixture" to bytes(bound = "java/lang/Number", interfaces = interfaces)))
        var raw: File? = null
        for (size in 2..3) {
            for (order in permutations(listOf(inputs[0], conflicting, inputs[2]).take(size))) {
                val output = generate(order, "raw-$size.jar")
                val node = read(output)
                assertNull(node.signature)
                assertNull(node.fields.first { it.name == "value" }.signature)
                assertNull(node.methods.first { it.name == "value" }.signature)
                checkSignatures(node)
                raw = output
            }
        }
        val unrelated = jar("unrelated.jar", mapOf("Fixture" to bytes(interfaces = arrayOf("other/Serializable"))))
        for (order in permutations(listOf(inputs[0], unrelated))) {
            val node = read(generate(order, "unrelated-out.jar"))
            assertTrue(node.interfaces.isEmpty())
            assertNull(node.signature)
            checkSignatures(node)
        }
        consumers(generic!!, true)
        consumers(raw!!, false)
    }

    private fun consumers(
        output: File,
        generic: Boolean,
    ) {
        val suffix = if (generic) "Generic" else "Raw"
        val java = File(root, "Java$suffix.java")
        java.writeText(
            if (generic) {
                "class Java$suffix { String use(Fixture<String> f) { return f.value(); } }"
            } else {
                "class Java$suffix { Object use(Fixture f) { return f.value(); } }"
            },
        )
        val kotlin = File(root, "Kotlin$suffix.kt")
        kotlin.writeText(
            if (generic) {
                "class Kotlin$suffix { fun use(f: Fixture<String>): String = f.value() }"
            } else {
                "class Kotlin$suffix { fun use(f: Fixture): Any? = f.value() }"
            },
        )
        val javaHome = System.getenv("STUBS_TEST_JAVA_HOME") ?: System.getProperty("java.home")
        run(File(javaHome, "bin/javac.exe").path, "--release", "8", "-classpath", output.path, "-d", root.path, java.path)
        val distribution = File(requireNotNull(System.getenv("STUBS_TEST_GRADLE_HOME")))
        val stdlib = File(distribution, "lib").listFiles()!!.single { it.name.startsWith("kotlin-stdlib-") && it.extension == "jar" }
        run(
            File(javaHome, "bin/java.exe").path,
            "-cp",
            File(distribution, "lib/*").path,
            "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-no-stdlib",
            "-no-reflect",
            "-jvm-target",
            "1.8",
            "-classpath",
            output.path + File.pathSeparator + stdlib.path,
            "-d",
            File(root, "kotlin$suffix").path,
            kotlin.path,
        )
    }

    private fun run(vararg command: String) {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        println(command.joinToString(" ") + "\n" + output + "\nexit=" + exit)
        assertEquals(0, exit, output)
    }

    @Test fun `generator failures release jars and recover`() {
        val good = jar("good.jar", mapOf("Fixture" to bytes()))
        val missing = jar("missing.jar", mapOf("Fixture" to bytes(parent = "Absent")))
        assertEquals("java/lang/Object", read(generate(listOf(good, missing), "missing-out.jar")).superName)
        val failures =
            listOf(
                jar("cycle.jar", mapOf("Fixture" to bytes(parent = "Loop"), "Loop" to bytes(parent = "Loop", name = "Loop"))),
                jar("corrupt.jar", mapOf("Fixture" to bytes(parent = "Broken"), "Broken" to byteArrayOf(1, 2, 3))),
            )
        for (input in failures) {
            for (order in permutations(listOf(input, good))) {
                assertFails { generate(order, "failure.jar") }
            }
            val moved = File(root, input.name + ".moved")
            Files.move(input.toPath(), moved.toPath())
            Files.move(moved.toPath(), input.toPath())
            // Windows exclusive access fails if a stream still owns the input.
            java.nio.channels.FileChannel
                .open(input.toPath(), java.nio.file.StandardOpenOption.WRITE)
                .use { it.truncate(0) }
            jar(input.name, mapOf("Fixture" to bytes()))
            checkSignatures(read(generate(listOf(input, good), "recovered.jar")))
        }
        assertFails { generate(listOf(File(root, "nonexistent.jar"), good), "read-failure.jar") }
        val directory =
            File(root, "output-directory").apply {
                mkdir()
                File(this, "occupied").writeText("x")
            }
        assertFails { generate(listOf(good, good), directory.name) }
        val moved = File(root, "good-moved.jar")
        Files.move(good.toPath(), moved.toPath())
        Files.move(moved.toPath(), good.toPath())
        checkSignatures(read(generate(listOf(good, good), "after-io.jar")))
    }

    @Test fun `single input class bytes and real task body change`() {
        val input = jar("input.jar", mapOf("Fixture" to bytes()))
        val output = generate(listOf(input), "single.jar")
        JarFile(output).use { jar -> assertContentEquals(bytes(), jar.getInputStream(jar.getJarEntry("Fixture.class")).readBytes()) }
        File(root, "settings.gradle").writeText("rootProject.name = 'inheritance-task'")
        val classpath =
            System.getProperty("stubs.test.classpath").split(File.pathSeparator).joinToString(",") {
                "'" + it.replace("\\", "/").replace("'", "\\'") + "'"
            }
        File(root, "build.gradle").writeText(
            """
            buildscript { dependencies { classpath files($classpath) } }
            import net.msrandom.stubs.GenerateStubApi
            def artifact = objects.newInstance(GenerateStubApi.ResolvedArtifact)
            artifact.file.set(layout.projectDirectory.file('input.jar'))
            tasks.register('stub', GenerateStubApi) {
                classpaths.set([[artifact]])
                excludes.set([])
                outputDirectory.set(layout.projectDirectory.dir('out'))
            }
            """.trimIndent(),
        )

        fun execute(): org.gradle.testkit.runner.BuildResult {
            val runner =
                GradleRunner
                    .create()
                    .withProjectDir(root)
                    .withArguments("stub", "--stacktrace", "--no-configuration-cache", "--max-workers=2", "--build-cache")
            System.getenv("STUBS_TEST_GRADLE_HOME")?.let { runner.withGradleInstallation(File(it)) }
            System.getenv("STUBS_TEST_KIT_HOME")?.let { runner.withTestKitDir(File(it)) }
            return runner.build().also { println(it.output) }
        }
        assertEquals(TaskOutcome.SUCCESS, execute().task(":stub")!!.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, execute().task(":stub")!!.outcome)
        jar("input.jar", mapOf("Fixture" to bytes(body = 2)))
        assertEquals(TaskOutcome.SUCCESS, execute().task(":stub")!!.outcome)
        JarFile(File(root, "out/api-stub.jar")).use { jar ->
            assertContentEquals(bytes(body = 2), jar.getInputStream(jar.getJarEntry("Fixture.class")).readBytes())
        }
    }
}
