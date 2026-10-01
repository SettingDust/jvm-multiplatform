package net.msrandom.stubs

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.test.*

class WorkerExecutionTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun jar(
        file: File,
        value: Int = 1,
        corrupt: Boolean = false,
    ) {
        JarOutputStream(file.outputStream()).use { jar ->
            repeat(256) { index ->
                val writer = ClassWriter(0)
                writer.visit(V1_8, ACC_PUBLIC, "fixture/C$index", null, "java/lang/Object", null)
                writer.visitAnnotation("LMarker;", true).visitEnd()
                writer.visitField(ACC_PUBLIC, "field", "I", null, null).visitEnd()
                repeat(16) { method ->
                    writer.visitMethod(ACC_PUBLIC or ACC_STATIC, "m$method", "()I", null, null).apply {
                        visitAnnotation("LMethodMarker;", true).visitEnd()
                        visitCode()
                        visitIntInsn(BIPUSH, value)
                        visitInsn(IRETURN)
                        visitMaxs(1, 0)
                        visitEnd()
                    }
                }
                writer.visitEnd()
                jar.putNextEntry(JarEntry("fixture/C$index.class"))
                jar.write(if (corrupt && index == 0) byteArrayOf(1, 2, 3) else writer.toByteArray())
                jar.closeEntry()
            }
        }
    }

    private fun fixture(): File {
        val directory = temporary.newFolder()
        File(
            directory,
            "settings.gradle",
        ).writeText("rootProject.name='worker-fixture'\nbuildCache { local { directory=file('cache') } }\n")
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
                    // Optional version deliberately absent for the two-input excluded selection.
                }
                a
            }
            def registerStub = { name, groups, preserve ->
                tasks.register(name, GenerateStubApi) {
                    classpaths.set(groups.collect { names -> names.collect { artifact(it, false) } })
                    excludes.set([])
                    if (preserve != null) preserveMethodBodies.set(preserve)
                    outputDirectory.set(layout.projectDirectory.dir('out/' + name))
                }
            }
            registerStub('left', [['a'], ['b'], ['c'], ['d']], true)
            registerStub('right', [['a'], ['b'], ['c'], ['d']], true)
            registerStub('singleOn', [['a']], true)
            registerStub('singleOff', [['a']], false)
            registerStub('defaultOff', [['a'], ['b']], null)
            registerStub('precedence', [['a','b']], true)
            registerStub('readFailure', [['io'], ['a']], true)
            registerStub('corrupt', [['bad'], ['a']], true)
            registerStub('outputFailure', [['a'], ['b']], true)
            tasks.named('outputFailure') {
                if (providers.gradleProperty('badOutput').isPresent()) apiFileName.set('missing/api.jar')
            }
            tasks.register('excluded', GenerateStubApi) {
                classpaths.set([[artifact('a',false),artifact('b',true)], [artifact('c',false),artifact('d',true)]])
                excludes.set([])
                outputDirectory.set(layout.projectDirectory.dir('out/excluded'))
            }
            """.trimIndent(),
        )
        // Operation events surround the actual WorkAction, not the task submission.
        File(directory, "operations.gradle").writeText(
            """
            import org.gradle.internal.operations.*
            import org.gradle.workers.internal.ExecuteWorkItemBuildOperationType
            import java.lang.management.ManagementFactory
            def events = new File(gradle.startParameter.currentDir, 'operations.tsv')
            events.text = ''
            new File(gradle.startParameter.currentDir, 'daemon.pid').text = ManagementFactory.runtimeMXBean.name.split('@')[0]
            def listener = new BuildOperationListener() {
                void started(BuildOperationDescriptor d, OperationStartEvent e) {}
                void progress(OperationIdentifier id, OperationProgressEvent e) {}
                void finished(BuildOperationDescriptor d, OperationFinishEvent e) {
                    if (d.details instanceof ExecuteWorkItemBuildOperationType.Details &&
                        d.details.className.contains('GenerateStubWorkAction')) {
                        synchronized(events) { events.append(d.id.toString() + '\t' + e.startTime + '\t' + e.endTime + '\t' + d.displayName + '\n') }
                    }
                }
            }
            def manager = gradle.services.get(BuildOperationListenerManager)
            manager.addListener(listener)
            gradle.buildFinished { manager.removeListener(listener) }
            """.trimIndent(),
        )
        listOf("a", "b", "c", "d", "bad").forEach { jar(File(directory, "$it.jar")) }
        return directory
    }

    private fun runner(
        directory: File,
        vararg arguments: String,
    ) = GradleRunner
        .create()
        .withProjectDir(directory)
        .apply {
            System.getenv("STUBS_TEST_GRADLE_HOME")?.let { withGradleInstallation(File(it)) }
            System.getenv("STUBS_TEST_KIT_HOME")?.let { withTestKitDir(File(it)) }
        }.withArguments(
            *arguments,
            "--offline",
            "--stacktrace",
            "--console=plain",
            "--build-cache",
            "--max-workers=2",
            "--init-script",
            "operations.gradle",
        )

    private fun verify(
        directory: File,
        task: String,
        bodies: Boolean,
    ): List<String> =
        JarFile(File(directory, "out/$task/api-stub.jar")).use { jar ->
            assertNotNull(jar.manifest)
            val entries =
                jar
                    .entries()
                    .asSequence()
                    .filter { !it.isDirectory }
                    .map { it.name }
                    .toList()
            assertEquals(257, entries.size)
            assertEquals(entries.size, entries.toSet().size)
            (0 until 256).map { index ->
                val node = ClassNode()
                jar.getInputStream(jar.getJarEntry("fixture/C$index.class")).use { ClassReader(it).accept(node, 0) }
                assertEquals("fixture/C$index", node.name)
                assertEquals("java/lang/Object", node.superName)
                assertEquals(listOf("LMarker;"), node.visibleAnnotations.map { it.desc })
                assertEquals(listOf("field"), node.fields.map { it.name })
                assertEquals(16, node.methods.size)
                for (method in node.methods) {
                    assertEquals("()I", method.desc)
                    assertEquals(listOf("LMethodMarker;"), method.visibleAnnotations.map { it.desc })
                    assertEquals(bodies, method.instructions.size() > 0)
                    if (bodies) assertEquals(1, (method.instructions.first as org.objectweb.asm.tree.IntInsnNode).operand)
                }
                node.name + node.access + node.superName + node.interfaces + node.signature +
                    node.fields.map { it.name + it.desc + it.access } +
                    node.methods.map { it.name + it.desc + it.access + it.instructions.toArray().map { insn -> insn.opcode } }
            }
        }

    private fun release(directory: File) {
        directory.walkTopDown().filter { it.isFile && it.extension == "jar" }.toList().forEach { file ->
            JarFile(file).use { assertTrue(it.size() > 0) }
            val renamed = File(file.parentFile, file.name + ".renamed")
            assertTrue(file.renameTo(renamed), "release $file")
            assertTrue(renamed.renameTo(file), "restore $file")
        }
    }

    @Test fun realWorkersResourcesAndObservation() {
        val directory = fixture()
        val warm = runner(directory, "left", "right", "--rerun-tasks").build()
        assertEquals(TaskOutcome.SUCCESS, warm.task(":left")!!.outcome)
        val pid = File(directory, "daemon.pid").readText().trim()
        val sampler = File(directory, "sample.ps1")
        sampler.writeText(
            """
            param([int]${'$'}ObservedPid, [string]${'$'}Root)
            'utc,workingSetBytes' | Set-Content "${'$'}Root/memory.csv"
            ${'$'}end = [DateTime]::UtcNow.AddSeconds(60)
            while (!(Test-Path "${'$'}Root/done") -and [DateTime]::UtcNow -lt ${'$'}end) {
                ${'$'}observed = Get-Process -Id ${'$'}ObservedPid -ErrorAction Stop
                ([DateTime]::UtcNow.ToString('o') + ',' + ${'$'}observed.WorkingSet64) | Add-Content "${'$'}Root/memory.csv"
                Set-Content "${'$'}Root/ready" 'ready'
                Start-Sleep -Milliseconds 100
            }
            """.trimIndent(),
        )
        val process =
            ProcessBuilder("pwsh", "-NoProfile", "-File", sampler.absolutePath, pid, directory.absolutePath)
                .redirectErrorStream(true)
                .redirectOutput(File(directory, "sampler.log"))
                .start()
        val readyDeadline = System.nanoTime() + 10_000_000_000L
        while (!File(directory, "ready").exists() && System.nanoTime() < readyDeadline && process.isAlive) Thread.sleep(10)
        assertTrue(File(directory, "ready").exists(), "sampler must start before measurement")
        val start = System.nanoTime()
        var elapsed = 0L
        try {
            val measured = runner(directory, "left", "right", "--rerun-tasks").build()
            for (task in listOf("left", "right")) assertEquals(TaskOutcome.SUCCESS, measured.task(":$task")!!.outcome)
        } finally {
            elapsed = (System.nanoTime() - start) / 1_000_000
            File(directory, "done").writeText("done")
            assertEquals(0, process.waitFor(), File(directory, "sampler.log").readText())
        }
        assertEquals(pid, File(directory, "daemon.pid").readText().trim(), "measured daemon must match sampled process")
        val samples = File(directory, "memory.csv").readLines().drop(1).map { it.substringAfter(',').toLong() }
        assertTrue(samples.isNotEmpty())
        println(
            "OBSERVATION workload=2 tasks, shared 4 jars x 256 classes x 16 methods; workers=2; warmups=1; measured=1; elapsedMs=$elapsed; GradleDaemonPid=$pid; sampleCount=${samples.size}; sampledPeakWorkingSetBytes=${samples.maxOrNull()}; interval=100ms; excludes launcher/test JVM; not heap/OOM evidence; baseline compilation unavailable",
        )
        val intervals = File(directory, "operations.tsv").readLines().map { it.split('\t') }
        println("WORK_ACTION_INTERVALS " + intervals.joinToString(";"))
        assertEquals(2, intervals.size, "one action per task, not per class")
        assertTrue(
            maxOf(intervals[0][1].toLong(), intervals[1][1].toLong()) < minOf(intervals[0][2].toLong(), intervals[1][2].toLong()),
            "actual actions must overlap",
        )
        assertEquals(verify(directory, "left", true), verify(directory, "right", true))
        release(directory)
        val unchanged = runner(directory, "left", "right").build()
        listOf("left", "right").forEach { assertEquals(TaskOutcome.UP_TO_DATE, unchanged.task(":$it")!!.outcome) }
        File(directory, "out").deleteRecursively()
        val cached = runner(directory, "left", "right").build()
        listOf("left", "right").forEach { assertEquals(TaskOutcome.FROM_CACHE, cached.task(":$it")!!.outcome) }
        jar(File(directory, "b.jar"), value = 9)
        val changed = runner(directory, "left", "right").build()
        listOf("left", "right").forEach {
            assertEquals(TaskOutcome.SUCCESS, changed.task(":$it")!!.outcome)
            verify(directory, it, false)
        }
        runner(directory, "singleOn", "singleOff", "defaultOff", "precedence", "excluded").build()
        verify(directory, "defaultOff", false)
        for (task in listOf("singleOn", "singleOff", "precedence")) {
            verify(directory, task, true)
            JarFile(File(directory, "a.jar")).use { input ->
                JarFile(File(directory, "out/$task/api-stub.jar")).use { output ->
                    repeat(256) { index ->
                        val name = "fixture/C$index.class"
                        assertContentEquals(
                            input.getInputStream(input.getJarEntry(name)).use {
                                it.readBytes()
                            },
                            output.getInputStream(output.getJarEntry(name)).use { it.readBytes() },
                        )
                    }
                }
            }
        }
        val copied = File(directory, "out/excluded/org.jetbrains_extra_1/b.jar")
        assertContentEquals(File(directory, "b.jar").readBytes(), copied.readBytes())
        jar(File(directory, "b.jar"), value = 7)
        assertEquals(TaskOutcome.SUCCESS, runner(directory, "excluded").build().task(":excluded")!!.outcome)
        assertContentEquals(File(directory, "b.jar").readBytes(), copied.readBytes())
        jar(File(directory, "bad.jar"), corrupt = true)
        assertEquals(TaskOutcome.FAILED, runner(directory, "corrupt").buildAndFail().task(":corrupt")!!.outcome)
        assertFalse(File(directory, "out/corrupt").exists())
        release(directory)
        jar(File(directory, "bad.jar"))
        assertEquals(TaskOutcome.SUCCESS, runner(directory, "corrupt", "--rerun-tasks").build().task(":corrupt")!!.outcome)
        assertEquals(
            TaskOutcome.FAILED,
            runner(directory, "outputFailure", "-PbadOutput=true").buildAndFail().task(":outputFailure")!!.outcome,
        )
        assertFalse(File(directory, "out/outputFailure").exists())
        release(directory)
        assertEquals(TaskOutcome.SUCCESS, runner(directory, "outputFailure", "--rerun-tasks").build().task(":outputFailure")!!.outcome)
        // Corrupt a DEFLATE block while keeping the ZIP directory valid: actual stream I/O error.
        val ioJar = File(directory, "io.jar")
        jar(ioJar)
        java.io.RandomAccessFile(ioJar, "rw").use { file ->
            file.seek(26)
            val nameLength = file.readUnsignedByte() or (file.readUnsignedByte() shl 8)
            val extraLength = file.readUnsignedByte() or (file.readUnsignedByte() shl 8)
            file.seek(30L + nameLength + extraLength)
            file.write(255) // BTYPE=3 is invalid, so InflaterInputStream must throw.
        }
        StubGenerator.ClasspathLoader(listOf(ioJar), emptyList()).use { loader ->
            assertNull(loader.entry("Missing.class"))
            assertFailsWith<java.io.IOException> { loader.entry("fixture/C0.class") }
            assertFailsWith<java.io.IOException> { loader.header("fixture/C0.class") }
        }
        val ioFailure = runner(directory, "readFailure").buildAndFail()
        assertEquals(TaskOutcome.FAILED, ioFailure.task(":readFailure")!!.outcome)
        assertTrue(ioFailure.output.contains("invalid block type"), ioFailure.output)
        assertFalse(File(directory, "out/readFailure").exists())
        release(directory)
        jar(ioJar)
        assertEquals(TaskOutcome.SUCCESS, runner(directory, "readFailure", "--rerun-tasks").build().task(":readFailure")!!.outcome)
        release(directory)
        println(
            "VERIFIED semantic outputs, manifest/entries, byte-exact single/precedence, excluded copies/optional versions, cache/body invalidation, failure cleanup/release/rerun; Code matrix covered additionally by PreserveMethodBodiesTest",
        )
    }
}
