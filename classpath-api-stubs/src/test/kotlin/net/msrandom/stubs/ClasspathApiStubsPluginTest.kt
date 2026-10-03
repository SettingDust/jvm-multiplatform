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
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.test.*

class ClasspathApiStubsPluginTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun fixtureJar(
        file: File,
        side: String,
    ) {
        JarOutputStream(file.outputStream()).use { jar ->
            for (name in listOf("fixture/Common", "fixture/Only$side")) {
                val writer = ClassWriter(0)
                writer.visit(V1_8, ACC_PUBLIC or ACC_SUPER, name, null, "java/lang/Object", null)
                val methods = if (name == "fixture/Common") listOf("common", "only$side") else listOf("common")
                for (methodName in methods) {
                    writer.visitMethod(ACC_PUBLIC or ACC_STATIC, methodName, "()I", null, null).apply {
                        visitCode()
                        visitIntInsn(BIPUSH, 7)
                        visitInsn(IRETURN)
                        visitMaxs(1, 0)
                        visitEnd()
                    }
                }
                writer.visitEnd()
                jar.putNextEntry(JarEntry("$name.class").apply { time = 0 })
                jar.write(writer.toByteArray())
                jar.closeEntry()
            }
        }
    }

    private fun commonClass(file: File) =
        JarFile(file).use { jar ->
            jar.getInputStream(jar.getJarEntry("fixture/Common.class")).use { input ->
                ClassNode().apply { ClassReader(input).accept(this, 0) }
            }
        }

    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    @Test
    fun `published marker applies plugin and generates the common API`() {
        val project = temporary.newFolder()
        val repository = File(System.getProperty("stubs.test.marker.repository"))
        val version = System.getProperty("stubs.test.marker.version")
        val implementationGroup = System.getProperty("stubs.test.implementation.group")
        val pluginId = "net.msrandom.classpath-api-stubs"
        val marker =
            repository.resolve(
                "${pluginId.replace('.', '/')}/$pluginId.gradle.plugin/$version/$pluginId.gradle.plugin-$version.pom",
            )
        val implementation =
            repository.resolve(
                "${implementationGroup.replace('.', '/')}/classpath-api-stubs/$version/classpath-api-stubs-$version.jar",
            )
        assertTrue(marker.isFile, "Missing Gradle-generated marker: $marker")
        assertTrue(implementation.isFile, "Missing Gradle-generated implementation: $implementation")
        JarFile(implementation).use { jar ->
            assertNotNull(jar.getJarEntry("net/msrandom/stubs/ClasspathApiStubsPlugin.class"))
        }
        project.resolve("settings.gradle").writeText(
            """
            pluginManagement {
                repositories {
                    exclusiveContent {
                        forRepository { maven { url = uri('${repository.toURI()}') } }
                        filter {
                            includeGroup('$pluginId')
                            includeModule('$implementationGroup', 'classpath-api-stubs')
                        }
                    }
                    mavenCentral()
                }
            }
            rootProject.name = 'published-marker-fixture'
            """.trimIndent(),
        )
        project.resolve("build.gradle").writeText(
            """
            plugins { id '$pluginId' version '$version' }

            assert plugins.getPlugin('$pluginId') instanceof net.msrandom.stubs.ClasspathApiStubsPlugin
            def generator = tasks.named('generateStubApi', net.msrandom.stubs.GenerateStubApi)
            generator.configure {
                assert apiFileName.get() == 'api-stub.jar'
                assert preserveMethodBodies.get() == false
                classpaths.set(['left.jar', 'right.jar'].collect { name ->
                    def artifact = objects.newInstance(net.msrandom.stubs.GenerateStubApi.ResolvedArtifact)
                    artifact.file.set(layout.projectDirectory.file(name))
                    [artifact]
                })
                excludes.set([])
                outputDirectory.set(layout.projectDirectory.dir('generated'))
                doFirst {
                    println "MARKER_GENERATOR plugin=${'$'}{plugins.getPlugin('$pluginId').class.name} task=${'$'}{generator.get().class.name} gradle=${'$'}{gradle.gradleVersion} java=${'$'}{System.getProperty('java.runtime.version')}"
                }
            }
            """.trimIndent(),
        )
        val inputs = listOf(project.resolve("left.jar"), project.resolve("right.jar"))
        inputs.zip(listOf("Left", "Right")).forEach { (file, side) -> fixtureJar(file, side) }
        val inputHashes = inputs.map(::sha256)
        for (input in inputs) {
            val node = commonClass(input)
            assertEquals(V1_8, node.version)
            assertEquals(2, node.methods.size)
            assertTrue(node.methods.all { it.instructions.size() > 0 })
        }
        val arguments =
            listOf(
                "generateStubApi",
                "--offline",
                "--no-build-cache",
                "--no-configuration-cache",
                "--max-workers=2",
                "--stacktrace",
                "--console=plain",
            )
        val result =
            GradleRunner
                .create()
                .withProjectDir(project)
                .withTestKitDir(File(System.getProperty("stubs.test.gradle.user.home")))
                .withArguments(arguments)
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateStubApi")?.outcome, result.output)
        val output = project.resolve("generated/api-stub.jar")
        assertTrue(output.isFile, "Generator must produce its default JAR")
        JarFile(output).use { jar ->
            assertEquals(
                setOf("fixture/Common.class"),
                jar
                    .entries()
                    .asSequence()
                    .filter { it.name.endsWith(".class") }
                    .map { it.name }
                    .toSet(),
            )
        }
        val common = commonClass(output)
        assertEquals(V1_8, common.version)
        assertEquals(listOf("common" to "()I"), common.methods.map { it.name to it.desc })
        assertEquals(ACC_PUBLIC or ACC_STATIC, common.methods.single().access)
        assertEquals(
            0,
            common.methods
                .single()
                .instructions
                .size(),
            "Default false must omit even identical method bodies",
        )
        assertEquals(inputHashes, inputs.map(::sha256), "Generation must preserve both input JARs")
        println(
            "PUBLISHED_MARKER repository=$repository marker=$pluginId:$pluginId.gradle.plugin:$version implementation=$implementationGroup:classpath-api-stubs:$version",
        )
        println("PRODUCTION_SHA256 marker=${sha256(marker)} implementation=${sha256(implementation)}")
        println("GENERATION_SHA256 left=${inputHashes[0]} right=${inputHashes[1]} output=${sha256(output)}")
        println("TESTKIT_ARGUMENTS ${arguments.joinToString(" ")}")
        println(result.output)
    }
}
