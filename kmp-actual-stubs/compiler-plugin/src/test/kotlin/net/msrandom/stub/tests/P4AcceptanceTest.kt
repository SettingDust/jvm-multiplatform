package net.msrandom.stub.tests

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.*

/** Fresh callable and real Cloche acceptance; no prior build output is an input. */
class P4AcceptanceTest {
    private val root = File(requireNotNull(System.getProperty("p4.root")))
    private val plugin = File(requireNotNull(System.getProperty("callable.plugin")))
    private val clocheSource = File(requireNotNull(System.getProperty("p4.clocheSource")))
    private val consumer = File(root, "consumer")
    private val hash = sha256(plugin)

    @Test
    fun `A1-A15 and product evidence`() {
        assertSuite()
        assertTrue(!root.exists() || root.deleteRecursively(), "Cannot clear P4 output: $root")
        root.mkdirs()
        val cloche = File(root, "cloche")
        assertTrue(File(clocheSource, "src/main").isDirectory, "Current Cloche source required: $clocheSource")
        for (path in listOf("src/main", "models/src/main")) {
            File(clocheSource, path).copyRecursively(File(cloche, path), overwrite = true)
        }
        for (path in listOf("gradle.properties", "models/build.gradle.kts")) {
            File(cloche, path).apply {
                parentFile.mkdirs()
                writeBytes(File(clocheSource, path).readBytes())
            }
        }
        File(cloche, "models/build.gradle.kts").appendText(
            "\njava { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }\n" +
                "kotlin.compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)\n",
        )
        for (path in listOf(
            "cloche/build.gradle.kts",
            "cloche/settings.gradle.kts",
            "consumer/build.gradle",
            "consumer/settings.gradle",
            "consumer/observe.gradle",
            "consumer/src/parent/main/kotlin/p1/Parent.kt",
            "consumer/src/parent/main/kotlin/p1/Excluded.kt",
            "consumer/src/child/main/kotlin/p1/Child.kt",
            "consumer/src/downstream/main/kotlin/p1/Downstream.kt",
        )) {
            File(root, path).apply {
                parentFile.mkdirs()
                writeBytes(requireNotNull(this@P4AcceptanceTest.javaClass.getResourceAsStream("/p4/$path")) { path }.use { it.readBytes() })
            }
        }
        File(cloche, "build.gradle.kts").appendText(
            """

            tasks.register("recordP4Classpath") {
                dependsOn(tasks.jar, configurations.runtimeClasspath)
                doLast {
                    file("p4-classpath.txt").writeText(
                        (listOf(tasks.jar.get().archiveFile.get().asFile) + configurations.runtimeClasspath.get().files)
                            .joinToString(System.lineSeparator()) { it.absolutePath }
                    )
                }
            }
            """.trimIndent(),
        )
        File(root, "source-sha256.txt").writeText(
            cloche.walkTopDown().filter { it.isFile }.joinToString("\n") {
                "${sha256(it)} ${it.relativeTo(cloche).invariantSeparatorsPath}"
            },
        )
        run("build-cloche", cloche, 0, "recordP4Classpath")
        val cp = File(cloche, "p4-classpath.txt")
        val vss = File(requireNotNull(System.getProperty("p4.vss")))
        cp.writeText(
            cp
                .readLines()
                .filterNot { File(it).name.startsWith("jvm-virtual-source-sets-") }
                .plus(vss.absolutePath)
                .joinToString("\n"),
        )
        val product = run("product-provenance", consumer, 0, "recordProduct")
        assertTrue("P4_GRADLE=9.4.0" in product, product)
        assertTrue("kotlin-compiler-embeddable:2.2.20" in product, product)

        round("clean", 73, false, parent = true)
        round("change", 75, false)
        val added = File(consumer, "src/child/main/kotlin/p1/Added.kt")
        added.writeBytes("package p1\r\nactual fun missing(): Int = 88\r\n".toByteArray())
        try {
            round("add", 75, true)
        } finally {
            assertTrue(added.delete())
        }
        round("remove", 75, false)
        val orphan = File(consumer, "src/child/main/kotlin/p1/Orphan.kt")
        orphan.writeText("package p1\nactual fun orphan(): Int = 99\n")
        try {
            val output = run("orphan-native", consumer, 1, "compileChildKotlin", "-PactualBody=75")
            assertLoaded(output)
            assertTrue("Orphan.kt" in output && "has no corresponding expected declaration" in output, output)
        } finally {
            assertTrue(orphan.delete())
        }
        val downstream = File(consumer, "src/downstream/main/kotlin/p1/Downstream.kt")
        val restored = downstream.readBytes()
        downstream.writeText("package p1\nactual fun leafValue() = \"wrong\"\n")
        try {
            val output = run("inferred-native", consumer, 1, "compileDownstreamKotlin", "-PactualBody=75")
            assertLoaded(output)
            assertTrue("Downstream.kt" in output && "declarations are incompatible" in output, output)
        } finally {
            downstream.writeBytes(restored)
        }
        round("final-restored", 75, false)
        assertEquals(hash, sha256(plugin), "Gradle plugin jar changed during acceptance")
        File(root, "artifact.txt").writeText("plugin=${plugin.canonicalPath}\nsha256=$hash\nverifiedUtc=${Instant.now()}\n")
    }

    private fun assertSuite() {
        val suiteRoot = File(requireNotNull(System.getProperty("p4.callableRoot")))
        val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(suiteRoot, "xml/TEST-net.msrandom.stub.tests.CallableMatchingTest.xml")).documentElement
        assertEquals("31", xml.getAttribute("tests"))
        for (attribute in listOf("failures", "errors", "skipped")) assertEquals("0", xml.getAttribute(attribute), attribute)
        val cases = File(suiteRoot, "cases")
        assertEquals(38, cases.listFiles { f -> f.isDirectory && f.name.startsWith("p3-") }.orEmpty().size)
        val logs = cases.walkTopDown().filter { it.name == "compile.log" }.toList()
        assertEquals(168, logs.size)
        for (log in logs) {
            val text = log.readText()
            assertTrue("EXIT=OK" in text || "EXIT=COMPILATION_ERROR" in text, log.path)
            assertFalse("EXIT=INTERNAL_ERROR" in text, log.path)
            assertTrue(File(log.parentFile, "methods.txt").isFile, log.path)
        }
        val provenance = File(cases, "provenance.txt").readText()
        assertTrue("compiler=2.2.20" in provenance && "language=2.1 api=2.1" in provenance, provenance)
        assertTrue("sha256=$hash" in provenance, provenance)
    }

    private fun round(
        label: String,
        body: Int,
        added: Boolean,
        parent: Boolean = false,
    ) {
        val tasks =
            (if (parent) listOf("compileParentKotlin") else emptyList()) +
                listOf("compileChildKotlin", "compileDownstreamKotlin", "-PactualBody=$body")
        val output = run(label, consumer, 0, *tasks.toTypedArray())
        assertLoaded(output)
        for (target in (if (parent) listOf("parent") else emptyList()) + listOf("child", "downstream")) {
            val classesDir = File(consumer, "build/classes/kotlin/$target")
            val snapshot = File(root, "$label-classes/$target")
            assertTrue(classesDir.isDirectory, classesDir.path)
            classesDir.copyRecursively(snapshot, overwrite = true)
            val classes =
                snapshot
                    .walkTopDown()
                    .filter { it.extension == "class" }
                    .map { file ->
                        ClassNode().also { ClassReader(file.readBytes()).accept(it, 0) }
                    }.toList()

            fun method(name: String): Pair<String, MethodNode> =
                classes
                    .flatMap { node ->
                        node.methods.filter { it.name == name && it.desc == "()I" }.map { node.name to it }
                    }.also { assertEquals(1, it.size, "$label/$target $name()I") }
                    .single()

            fun declaration(
                name: String,
                owner: String,
                value: Int?,
            ) {
                val (actualOwner, method) = method(name)
                assertEquals("p1/$owner", actualOwner, "$label/$target/$name")
                val code = method.instructions.toArray()
                if (value == null) {
                    assertTrue(code.any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" }, name)
                    assertTrue(code.any { it is LdcInsnNode && it.cst == "Common modules are not runnable" }, name)
                    assertTrue(code.any { it.opcode == Opcodes.ATHROW }, name)
                } else {
                    assertTrue(code.any { it is IntInsnNode && it.operand == value }, "$name = $value")
                    assertFalse(code.any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" }, name)
                }
            }
            declaration("value", if (target == "parent") "ParentKt" else "GeneratedKt", if (target == "parent") null else body)
            declaration("leafValue", if (target == "downstream") "DownstreamKt" else "ParentKt", if (target == "downstream") 74 else null)
            declaration("missing", if (added) "AddedKt" else "ParentKt", if (added) 88 else null)
            if (target != "parent") declaration("ordinaryChild", "ChildKt", 19)
            for ((caller, callee) in listOf("callValue" to "value", "callLeaf" to "leafValue", "callMissing" to "missing")) {
                val owner = method(callee).first
                assertTrue(
                    method(caller).second.instructions.toArray().any {
                        it is MethodInsnNode && it.owner == owner && it.name == callee && it.desc == "()I"
                    },
                    "$label/$target $caller -> $owner.$callee()I",
                )
            }
            File(root, "$label-$target-methods.txt").writeText(
                classes
                    .flatMap { node ->
                        node.methods.map { "${node.name}.${it.name}${it.desc}" }
                    }.joinToString("\n"),
            )
            if (label == "final-restored" && target == "child" && ":compileChildKotlin UP-TO-DATE" in output) {
                assertTaskInputs("inferred-native", target)
            } else {
                assertTaskInputs(label, target)
            }

        }
        File(root, "$label-verified.txt").writeText("body=$body added=$added sha256=$hash\n")
    }

    private fun assertTaskInputs(label: String, target: String) {
        val inputs = File(root, "$label-compile${target.replaceFirstChar { it.uppercase() }}Kotlin-inputs.txt")
        assertTrue(inputs.isFile, "Missing fresh task inputs: $inputs")
        val text = inputs.readText()
        assertTrue("incremental=true" in text && "strategy=DAEMON" in text, text)
        assertTrue("language=2.1" in text && "api=2.1" in text && "jvmTarget=17" in text, text)
        assertFalse("Excluded.kt" in text.lineSequence().single { it.startsWith("sources=") }, text)
        if (target != "parent") assertTrue("Generated.kt" in text && "Parent.kt" in text && "Child.kt" in text, text)
        assertTrue("plugin.sha256=$hash" in text, text)
    }

    private fun assertLoaded(output: String) {
        val records = output.lineSequence().filter { "P4_PROVENANCE|" in it }.toList()
        assertTrue(records.isNotEmpty(), "No compiler-side provenance\n$output")
        for (record in records) {
            assertTrue("compiler=2.2.20|language=2.1|api=2.1|" in record, record)
            assertTrue("|sha256=$hash|" in record, record)
            val loaded = File(record.substringAfter("|plugin=").substringBefore("|sha256="))
            assertEquals(hash, sha256(loaded), record)
            assertTrue("kotlin-compiler-embeddable-2.2.20.jar" in record, record)
        }
    }

    private fun run(
        label: String,
        project: File,
        expectedExit: Int,
        vararg tasks: String,
    ): String {
        val executable =
            System.getProperty("p4.gradleCommand")
                ?: File(clocheSource, if (File.separatorChar == '\\') "gradlew.bat" else "gradlew").absolutePath
        val command =
            listOf(executable, "-p", project.absolutePath) + tasks +
                listOf(
                    "-Pattempt=$label",
                    "--console=plain",
                    "--no-daemon",
                    "--no-configuration-cache",
                    "--no-configure-on-demand",
                    "-Dorg.gradle.internal.http.connectionTimeout=15000",
                    "-Dorg.gradle.internal.http.socketTimeout=15000",
                ) +
                (
                    System.getProperty("p4.javaPaths")?.let {
                        listOf("-Porg.gradle.java.installations.paths=$it", "-Porg.gradle.java.installations.auto-download=false")
                    }
                        ?: emptyList()
                ) +
                if (project == consumer) {
                    listOf(
                        "-PclocheClasspath=${File(root, "cloche/p4-classpath.txt")}",
                        "-PstubJar=${plugin.absolutePath}",
                        "-PobserverJar=${System.getProperty("p4.observer")}",
                        "-PannotationsJar=${System.getProperty("smoke.annotations")}",
                        "-PclassAnnotationsJar=${System.getProperty("p4.classAnnotations")}",
                    )
                } else {
                    emptyList()
                }
        File(root, "$label.command.txt").writeText(command.joinToString("\n") + "\nstartedUtc=${Instant.now()}\n")
        val log = File(root, "$label.log")
        val process =
            ProcessBuilder(command)
                .directory(project)
                .redirectErrorStream(true)
                .redirectOutput(log)
                .apply {
                    environment()["JAVA_HOME"] = requireNotNull(System.getProperty("p4.javaHome"))
                }.start()
        val exit = process.waitFor()
        File(root, "$label.exit").writeText("$exit\n")
        File(root, "$label.command.txt").appendText("finishedUtc=${Instant.now()}\nexit=$exit\n")
        val output = log.readText()
        assertEquals(expectedExit, exit, "$label\n$output")
        assertFalse("Internal compiler error" in output || "EXIT=INTERNAL_ERROR" in output, output)
        return output
    }

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") {
            "%02x".format(it)
        }
}
