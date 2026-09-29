package net.msrandom.stub.tests

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Gradle/JUnit replay and evidence assertions for the fixed P4 product fixture. */
class P4AcceptanceTest {
    private val root = File(System.getProperty("p4.root", "build/p4-final-acceptance")).absoluteFile
    private val expectedHash = "e81a9246deb8b60ff78721e7b8041e28c531db1091c314e10af64756e9e31cee"

    @Test
    fun `A1-A15 and product evidence`() {
        assertArtifact()
        assertSuite()
        assertProductRounds()
        if (System.getProperty("p4.replay", "false").toBoolean()) replayProduct()
    }

    private fun assertArtifact() {
        val artifact = File(root, "artifact.json").readText()
        assertTrue(artifact.contains(expectedHash), "artifact hash")
        val loaded = Regex("loadedArtifact\\\"\\s*:\\s*\\\"([^\\\"]+)").find(artifact)?.groupValues?.get(1)
        assertTrue(loaded != null && sha256(File(loaded)) == expectedHash, "loaded artifact hash")
        assertTrue(File(root, "cases/provenance.txt").readText().contains("compiler=2.2.20"))
        assertTrue(File(root, "cases/provenance.txt").readText().contains("language=2.1 api=2.1"))
    }

    private fun assertSuite() {
        val xml =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(File(root, "package/build/test-results/test/TEST-net.msrandom.stub.tests.CallableMatchingTest.xml"))
                .documentElement
        assertEquals("31", xml.getAttribute("tests"))
        assertEquals("0", xml.getAttribute("failures"))
        assertEquals("0", xml.getAttribute("errors"))
        assertEquals("0", xml.getAttribute("skipped"))
        val p3 = File(root, "cases").listFiles { file -> file.isDirectory && file.name.startsWith("p3-") }.orEmpty()
        assertEquals(38, p3.size)
        File(root, "cases").walkTopDown().filter { it.name == "compile.log" }.forEach { log ->
            val text = log.readText()
            assertTrue("EXIT=OK" in text || "EXIT=COMPILATION_ERROR" in text, log.path)
            assertTrue(File(log.parentFile, "methods.txt").isFile, log.path)
            assertTrue("EXIT=INTERNAL_ERROR" !in text, log.path)
        }
    }

    private fun assertProductRounds() {
        for ((label, exit) in listOf(
            "clean" to "0",
            "change" to "0",
            "add" to "0",
            "remove" to "0",
            "orphan-native" to "1",
            "inferred-native" to "1",
            "final-restored" to "0",
        )) {
            assertEquals(exit, File(root, "$label.exit").readText().trim(), label)
        }
        assertTrue(File(root, "orphan-native-diagnostic.txt").readText().contains("has no corresponding expected declaration"))
        assertTrue(File(root, "inferred-native-diagnostic.txt").readText().contains("declarations are incompatible"))
        assertTrue(File(root, "final-restored-verified.json").isFile)
    }

    private fun replayProduct() {
        val consumer = File(root, "consumer")
        val plugin = File(root, "package/build/libs/p2-product-verification.jar")
        val repoJar =
            File(
                root,
                "repo/net/msrandom/kmp-actual-stubs-compiler-plugin/0.1.2-p2-verified/kmp-actual-stubs-compiler-plugin-0.1.2-p2-verified.jar",
            )
        repoJar.parentFile.mkdirs()
        plugin.copyTo(repoJar, overwrite = true)
        run("replay-clean", consumer, "compileParentKotlin", "compileChildKotlin", "compileDownstreamKotlin")
        run("replay-change", consumer, "compileChildKotlin", "compileDownstreamKotlin", "-PactualBody=75")
        val added = File(consumer, "src/child/main/kotlin/p1/Added.kt")
        added.writeBytes("package p1\r\nactual fun missing(): Int = 88\r\n".toByteArray())
        try {
            run("replay-add", consumer, "compileChildKotlin", "compileDownstreamKotlin", "-PactualBody=75")
        } finally {
            Files.deleteIfExists(added.toPath())
        }
        run("replay-remove", consumer, "compileChildKotlin", "compileDownstreamKotlin", "-PactualBody=75")
        run("replay-final-restored", consumer, "compileChildKotlin", "compileDownstreamKotlin", "-PactualBody=75")
    }

    private fun run(
        label: String,
        project: File,
        vararg tasks: String,
    ) {
        val wrapper = File(System.getProperty("p4.cloche.wrapper", "E:/Projects/cloche/cloche/gradlew.bat"))
        val command =
            listOf(wrapper.absolutePath, "-p", project.absolutePath) + tasks.toList() +
                listOf("-Pattempt=$label", "--console=plain", "--no-daemon")
        val result = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = result.inputStream.bufferedReader().readText()
        val exit = result.waitFor()
        File(root, "$label-kotlin.log").writeText(output)
        assertEquals(0, exit, "$label\n$output")
    }

    private fun sha256(file: File): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
}
