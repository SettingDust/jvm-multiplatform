package net.msrandom.stubs

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.test.*

class ParameterGenerationTest {
    @get:Rule val temporary = TemporaryFolder()

    private val cases =
        listOf("instance", "static", "short", "mandated", "ambiguous", "absent", "wrongLength", "nullName", "flags", "conflict", "lvt")
    private val descriptor = "(Ljava/lang/Object;JDLjava/lang/String;)V"

    private fun fixture(
        file: File,
        variant: Int,
    ) {
        val writer = ClassWriter(0)
        writer.visit(V1_8, ACC_PUBLIC, "Parameters", null, "java/lang/Object", null)
        for (name in cases) {
            val static = name == "static"
            val method = writer.visitMethod(ACC_PUBLIC or (if (static) ACC_STATIC else 0), name, descriptor, null, null)
            val short = name in listOf("short", "mandated", "ambiguous")
            if (name !in listOf("ambiguous", "lvt")) {
                method.visitParameter(
                    if (short) "prefix$variant" else "prefix",
                    if (short) {
                        (
                            if (name ==
                                "mandated"
                            ) {
                                ACC_MANDATED
                            } else {
                                ACC_SYNTHETIC
                            }
                        )
                    } else {
                        0
                    },
                )
                method.visitParameter("wide", if (name == "flags" && variant == 2) ACC_FINAL else 0)
                method.visitParameter("double", 0)
                if (name != "wrongLength") method.visitParameter(if (name == "nullName") null else "text", 0)
            }
            for (visible in listOf(false, true)) {
                if (name == "absent" && variant == 2) continue
                val count = if (short) 3 else 4
                method.visitAnnotableParameterCount(count, visible)
                // Encoded zero slot remains empty; the last annotation stays on descriptor parameter 3.
                val annotation = method.visitParameterAnnotation(count - 1, if (visible) "LVisible;" else "LInvisible;", visible)
                val nested = annotation.visitAnnotation("nested", "LNested;")
                nested.visit("value", if (name == "conflict" && variant == 2) "different" else "same")
                nested.visitEnd()
                annotation.visitEnum("enum", "LEnum;", "VALUE")
                annotation.visit("ints", intArrayOf(1, 2))
                val array = annotation.visitArray("array")
                array.visit(null, "one")
                array.visit(null, "two")
                array.visitEnd()
                annotation.visitEnd()
            }
            method.visitCode()
            val start = Label()
            val end = Label()
            method.visitLabel(start)
            method.visitInsn(RETURN)
            method.visitLabel(end)
            var slot = if (static) 0 else 1
            for ((index, type) in Type.getArgumentTypes(descriptor).withIndex()) {
                method.visitLocalVariable("debug${variant}_$index", type.descriptor, null, start, end, slot)
                slot += type.size
            }
            method.visitMaxs(0, slot)
            method.visitEnd()
        }
        writer.visitEnd()
        JarOutputStream(file.outputStream()).use {
            it.putNextEntry(JarEntry("Parameters.class"))
            it.write(writer.toByteArray())
            it.closeEntry()
        }
    }

    private fun bytes(file: File): ByteArray =
        JarFile(file).use { jar ->
            jar.getInputStream(jar.getJarEntry("Parameters.class")).use { it.readBytes() }
        }

    private fun methods(file: File): List<MethodNode> = ClassNode().apply { ClassReader(bytes(file)).accept(this, 0) }.methods

    private fun permutations(inputs: List<String>): List<List<String>> =
        if (inputs.isEmpty()) listOf(emptyList()) else inputs.flatMap { first -> permutations(inputs - first).map { listOf(first) + it } }

    @Test
    fun generatedMetadataUsesOriginalsForEveryPairAndTripleOrder() {
        val directory = temporary.newFolder()
        File(directory, "settings.gradle").writeText("rootProject.name = 'parameter-fixture'\n")
        for ((index, name) in listOf("a", "b", "c").withIndex()) fixture(File(directory, "$name.jar"), index)
        val orders = listOf(listOf("a", "b"), listOf("a", "c"), listOf("b", "c"), listOf("a", "b", "c")).flatMap(::permutations)
        val classpath =
            System.getProperty("stubs.test.classpath").split(File.pathSeparator).joinToString(",") {
                "'" + it.replace("\\", "/").replace("'", "\\'") + "'"
            }
        val registrations =
            orders.mapIndexed { index, inputs ->
                "stub('order$index', [${inputs.joinToString(",") { "'$it'" }}], true)"
            } + listOf("stub('singleOn', ['a'], true)", "stub('singleOff', ['a'], false)", "stub('defaultOff', ['a','b','c'], false)")
        File(directory, "build.gradle").writeText(
            """
            buildscript { dependencies { classpath files($classpath) } }
            import net.msrandom.stubs.GenerateStubApi
            def stub = { name, inputs, preserve ->
                tasks.register(name, GenerateStubApi) {
                    classpaths.set(inputs.collect { input ->
                        def artifact = objects.newInstance(GenerateStubApi.ResolvedArtifact)
                        artifact.file.set(layout.projectDirectory.file(input + '.jar'))
                        [artifact]
                    })
                    excludes.set([])
                    preserveMethodBodies.set(preserve)
                    outputDirectory.set(layout.projectDirectory.dir('out/' + name))
                }
            }
            ${registrations.joinToString("\n")}
            """.trimIndent(),
        )
        val tasks = orders.indices.map { "order$it" } + listOf("singleOn", "singleOff", "defaultOff")
        val result =
            GradleRunner
                .create()
                .apply { System.getenv("STUBS_TEST_GRADLE_HOME")?.let { withGradleInstallation(File(it)) } }
                .withProjectDir(directory)
                .withArguments(tasks + listOf("--stacktrace", "--console=plain", "--max-workers=2"))
                .build()
        println(result.output)
        for (task in tasks) assertEquals(TaskOutcome.SUCCESS, result.task(":$task")!!.outcome, task)
        for (task in listOf("singleOn", "singleOff")) {
            assertContentEquals(bytes(File(directory, "a.jar")), bytes(File(directory, "out/$task/api-stub.jar")), task)
        }
        for ((index, inputs) in orders.withIndex()) {
            val output = methods(File(directory, "out/order$index/api-stub.jar"))
            assertEquals(cases.toSet(), output.map { it.name }.toSet())
            for (method in output) {
                val name = method.name
                val short = name in listOf("short", "mandated")
                val omitted = name == "ambiguous" || (name in listOf("absent", "conflict") && "c" in inputs)
                assertEquals(descriptor, method.desc)
                assertTrue(method.instructions.size() > 0, "$index/$name Code")
                assertEquals(if (name == "static") listOf(0, 1, 3, 5) else listOf(1, 2, 4, 6), method.localVariables.map { it.index })
                for (visible in listOf(false, true)) {
                    val table = if (visible) method.visibleParameterAnnotations else method.invisibleParameterAnnotations
                    val count = if (visible) method.visibleAnnotableParameterCount else method.invisibleAnnotableParameterCount
                    if (omitted) {
                        assertNull(table, "$index/$name/$visible")
                        assertEquals(0, count)
                    } else {
                        assertNotNull(table, "$index/$name/$visible")
                        val encoded = if (short) 3 else 4
                        assertEquals(encoded, count)
                        assertEquals(4, table.size) // ClassReader pads to the descriptor count.
                        assertTrue(table.indices.filter { it != encoded - 1 }.all { table[it].isNullOrEmpty() })
                        val annotation = table[encoded - 1]!!.single()
                        assertEquals(if (visible) "LVisible;" else "LInvisible;", annotation.desc)
                        val values = annotation.values.chunked(2).associate { it[0] as String to it[1] }
                        assertEquals("same", (values["nested"] as org.objectweb.asm.tree.AnnotationNode).values[1])
                        assertEquals(listOf("LEnum;", "VALUE"), (values["enum"] as Array<*>).toList())
                        assertEquals(listOf(1, 2), values["ints"])
                        assertEquals(listOf("one", "two"), values["array"])
                    }
                }
                val namesOmitted =
                    name in listOf("short", "mandated", "ambiguous", "wrongLength", "nullName", "lvt") || (name == "flags" && "c" in inputs)
                if (namesOmitted) {
                    assertNull(method.parameters, "$index/$name names")
                } else {
                    assertEquals(listOf("prefix", "wide", "double", "text"), method.parameters.map { it.name })
                    assertEquals(listOf(0, 0, 0, 0), method.parameters.map { it.access })
                }
            }
        }
        assertTrue(methods(File(directory, "out/defaultOff/api-stub.jar")).all { it.instructions.size() == 0 })
        println(
            "Verified 6 pair orders, 6 triple orders, 11 method cases, both visibilities, Code, descriptor/LVT slots, and byte-exact singleOn/singleOff.",
        )
    }
}
