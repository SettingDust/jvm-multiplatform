package net.msrandom.stubs

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.test.*

class PreserveMethodBodiesTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun jar(
        file: File,
        value: Int = 1,
        debug: Int = 1,
        missing: Boolean = false,
        annotation: Boolean = false,
        unknown: Boolean = false,
    ) {
        val writer = ClassWriter(0)
        writer.visit(V1_8, ACC_PUBLIC or ACC_ABSTRACT, "Fixture", null, "java/lang/Object", null)
        for (name in listOf("m", "<init>", "<clinit>")) {
            val method =
                writer.visitMethod(
                    ACC_PUBLIC or (if (name == "<init>") 0 else ACC_STATIC),
                    name,
                    "()V",
                    null,
                    if (debug == 1) arrayOf("java/lang/Exception", "java/io/IOException") else arrayOf("java/lang/Exception"),
                )
            method.visitAnnotation("LCommon;", true).visitEnd()
            if (debug == 1) method.visitAnnotation("LOnlyFirst;", true).visitEnd()
            if (!missing) {
                method.visitCode()
                val start = Label()
                val end = Label()
                val handler = Label()
                method.visitLabel(start)
                method.visitLineNumber(debug, start)
                if (debug != 1) method.visitLabel(Label())
                method.visitIntInsn(BIPUSH, value)
                method.visitInsn(POP)
                if (name == "<init>") {
                    method.visitVarInsn(ALOAD, 0)
                    method.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                } else {
                    // Deliberately absent helper: preservation promises shape, not executability.
                    method.visitMethodInsn(INVOKESTATIC, "AbsentHelper", "call", "()V", false)
                }
                if (annotation) {
                    method
                        .visitInsnAnnotation(
                            TypeReference.newTypeReference(TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT).value,
                            null,
                            "LCode;",
                            true,
                        ).visitEnd()
                }
                method.visitLabel(end)
                method.visitInsn(RETURN)
                method.visitLabel(handler)
                if (debug == 1) {
                    method.visitFrame(F_SAME1, 0, null, 1, arrayOf("java/lang/Throwable"))
                } else {
                    method.visitFrame(F_FULL, 0, emptyArray(), 1, arrayOf("java/lang/Throwable"))
                }
                method.visitInsn(ATHROW)
                method.visitTryCatchBlock(start, end, handler, null)
                method.visitLocalVariable("local$debug", "I", null, start, end, 1)
                if (unknown) {
                    method.visitAttribute(
                        object : Attribute("UnknownCode") {
                            override fun isCodeAttribute() = true

                            override fun write(
                                classWriter: ClassWriter,
                                code: ByteArray?,
                                codeLength: Int,
                                maxStack: Int,
                                maxLocals: Int,
                            ) = ByteVector().putByte(7)
                        },
                    )
                }
                method.visitMaxs(2, 2)
            }
            method.visitEnd()
        }
        writer.visitMethod(ACC_PUBLIC or ACC_ABSTRACT, "abstractMethod", "()V", null, null).visitEnd()
        writer.visitMethod(ACC_PUBLIC or ACC_NATIVE, "nativeMethod", "()V", null, null).visitEnd()
        if (debug == 1) writer.visitMethod(ACC_PUBLIC or ACC_ABSTRACT, "onlyFirst", "()V", null, null).visitEnd()
        writer.visitEnd()
        JarOutputStream(file.outputStream()).use {
            it.putNextEntry(JarEntry("Fixture.class").apply { time = 0 })
            it.write(writer.toByteArray())
            it.closeEntry()
        }
    }

    private fun methods(
        directory: File,
        task: String,
    ) = JarFile(File(directory, "out/$task/api-stub.jar")).use { jar ->
        ClassNode().apply { ClassReader(jar.getInputStream(jar.getJarEntry("Fixture.class"))).accept(this, 0) }.methods
    }

    private fun fixture(): File {
        val directory = temporary.newFolder()
        File(directory, "settings.gradle").writeText("rootProject.name = 'fixture'\nbuildCache { local { directory = file('cache') } }\n")
        val classpath =
            System.getProperty("stubs.test.classpath").split(File.pathSeparator).joinToString(",") {
                "'" + it.replace("\\", "/").replace("'", "\\'") + "'"
            }
        File(directory, "build.gradle").writeText(
            """
            buildscript { dependencies { classpath files($classpath) } }
            import net.msrandom.stubs.GenerateStubApi
            def artifact = { name, excluded ->
                def a = objects.newInstance(GenerateStubApi.ResolvedArtifact)
                a.file.set(layout.projectDirectory.file(name + '.jar'))
                if (excluded) {
                    a.componentId.set('org.jetbrains:extra:1')
                    a.type.set(GenerateStubApi.ResolvedArtifact.Type.Module)
                }
                a
            }
            def registerStub = { name, inputs, preserve ->
                tasks.register(name, GenerateStubApi) {
                    classpaths.set(inputs.collect { [artifact(it, false)] })
                    excludes.set([])
                    if (preserve != null) preserveMethodBodies.set(preserve)
                    outputDirectory.set(layout.projectDirectory.dir('out/' + name))
                }
            }
            registerStub('defaultOff', ['a', 'b'], null)
            registerStub('pair', ['a', 'b'], true)
            registerStub('reverse', ['b', 'a'], true)
            registerStub('triple', ['a', 'b', 'c'], true)
            registerStub('tripleReverse', ['c', 'b', 'a'], true)
            registerStub('singleOn', ['a'], true)
            registerStub('singleOff', ['a'], false)
            registerStub('toggle', ['a', 'b'], providers.gradleProperty('preserve').map { it.toBoolean() }.getOrElse(false))
            tasks.register('excluded', GenerateStubApi) {
                classpaths.set([[artifact('a', false), artifact('extra', true)]])
                excludes.set([])
                outputDirectory.set(layout.projectDirectory.dir('out/excluded'))
            }
            """.trimIndent(),
        )
        return directory
    }

    private fun run(
        directory: File,
        vararg arguments: String,
    ) = GradleRunner
        .create()
        .apply { System.getenv("STUBS_TEST_GRADLE_HOME")?.let { withGradleInstallation(File(it)) } }
        .withProjectDir(directory)
        .withArguments(*arguments, "--stacktrace", "--console=plain", "--build-cache", "--max-workers=2")
        .build()

    private fun assertBodies(
        directory: File,
        task: String,
        expected: Boolean,
    ) {
        val methods = methods(directory, task)
        for (name in listOf("m", "<init>", "<clinit>")) {
            assertEquals(expected, methods.single { it.name == name }.instructions.size() > 0, "$task/$name")
        }
        for (name in listOf("abstractMethod", "nativeMethod")) assertEquals(0, methods.single { it.name == name }.instructions.size())
    }

    @Test fun artifactsAndGradleInputState() {
        val directory = fixture()
        jar(File(directory, "a.jar"))
        jar(File(directory, "b.jar"), debug = 2)
        jar(File(directory, "c.jar"), debug = 3)
        jar(File(directory, "extra.jar"))
        val tasks = arrayOf("defaultOff", "pair", "reverse", "triple", "tripleReverse", "singleOn", "singleOff", "excluded", "toggle")
        val first = run(directory, *tasks)
        assertEquals(TaskOutcome.SUCCESS, first.task(":pair")!!.outcome)
        for (task in listOf("pair", "reverse", "triple", "tripleReverse", "singleOn", "singleOff")) assertBodies(directory, task, true)
        assertBodies(directory, "defaultOff", false)
        assertBodies(directory, "toggle", false)
        val common = methods(directory, "pair").single { it.name == "m" }
        assertEquals(listOf("LCommon;"), common.visibleAnnotations.map { it.desc })
        assertEquals(listOf("java/lang/Exception"), common.exceptions)
        assertFalse(methods(directory, "pair").any { it.name == "onlyFirst" })
        assertEquals(1, common.tryCatchBlocks.size)
        assertEquals(1, common.localVariables.size)
        assertEquals(2, common.maxStack)
        assertEquals(2, common.maxLocals)
        assertTrue(common.instructions.any { it is org.objectweb.asm.tree.FrameNode })
        assertEquals("local1", common.localVariables.single().name)
        val bodyless = methods(directory, "defaultOff")
        assertEquals(
            bodyless.map { Triple(it.name, it.desc, it.access) },
            methods(directory, "pair").map { Triple(it.name, it.desc, it.access) },
        )
        val unchanged = run(directory, *tasks)
        for (task in tasks) assertEquals(TaskOutcome.UP_TO_DATE, unchanged.task(":$task")!!.outcome, task)
        val toggle = run(directory, "toggle", "-Ppreserve=true")
        assertTrue(toggle.task(":toggle")!!.outcome in listOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE))
        assertBodies(directory, "toggle", true)

        // Body-only edit, no ABI change: both passthrough modes and all-input checks must rerun.
        jar(File(directory, "a.jar"), value = 2)
        val changed = run(directory, *tasks)
        for (task in listOf(
            "pair",
            "triple",
            "singleOn",
            "singleOff",
        )) {
            assertEquals(TaskOutcome.SUCCESS, changed.task(":$task")!!.outcome, task)
        }
        for (task in listOf("pair", "reverse", "triple", "tripleReverse")) assertBodies(directory, task, false)
        for (task in listOf("singleOn", "singleOff")) {
            assertBodies(directory, task, true)
            val instruction =
                methods(directory, task)
                    .single {
                        it.name == "m"
                    }.instructions
                    .iterator()
                    .asSequence()
                    .filterIsInstance<org.objectweb.asm.tree.IntInsnNode>()
                    .first()
            assertEquals(2, instruction.operand)
        }
        File(directory, "out").deleteRecursively()
        val cached = run(directory, *tasks)
        for (task in tasks) assertEquals(TaskOutcome.FROM_CACHE, cached.task(":$task")!!.outcome, task)
        assertBodies(directory, "pair", false)
        jar(File(directory, "a.jar"))
        val restored = run(directory, "pair", "singleOn", "singleOff")
        assertEquals(TaskOutcome.FROM_CACHE, restored.task(":pair")!!.outcome)
        assertBodies(directory, "pair", true)
        jar(File(directory, "extra.jar"), value = 9)
        assertEquals(TaskOutcome.SUCCESS, run(directory, "excluded").task(":excluded")!!.outcome)
        assertContentEquals(
            File(directory, "extra.jar").readBytes(),
            File(directory, "out/excluded/org.jetbrains_extra_1/extra.jar").readBytes(),
        )
        File(directory, "out/excluded").deleteRecursively()
        assertEquals(TaskOutcome.FROM_CACHE, run(directory, "excluded").task(":excluded")!!.outcome)
        assertContentEquals(
            File(directory, "extra.jar").readBytes(),
            File(directory, "out/excluded/org.jetbrains_extra_1/extra.jar").readBytes(),
        )

        for (variation in listOf("missing", "annotation", "unknown")) {
            jar(
                File(directory, "b.jar"),
                debug = 2,
                missing = variation == "missing",
                annotation = variation == "annotation",
                unknown =
                    variation == "unknown",
            )
            run(directory, "pair", "reverse", "triple", "tripleReverse")
            for (task in listOf("pair", "reverse", "triple", "tripleReverse")) assertBodies(directory, task, false)
        }
    }
}
