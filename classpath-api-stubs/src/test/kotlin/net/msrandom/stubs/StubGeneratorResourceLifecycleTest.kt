package net.msrandom.stubs

import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StubGeneratorResourceLifecycleTest {

    private fun createJarWithClass(className: String, bytes: ByteArray): File {
        val jar = Files.createTempFile("stub-resource-test-", ".jar").toFile()

        ZipOutputStream(jar.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("$className.class"))
            zip.write(bytes)
            zip.closeEntry()
        }

        return jar
    }

    @Test
    fun `classpath loader class entry scan releases jars after close`() {
        val className = "example/Test"
        val jar = createJarWithClass(className, byteArrayOf(0x00, 0x01, 0x02, 0x03))

        val loader = StubGenerator.ClasspathLoader(listOf(jar), emptyList())
        val entries = loader.classEntries().toList()
        loader.close()

        assertEquals(listOf("$className.class"), entries)
        assertTrue(jar.delete(), "Classpath jar should be deletable after loader close")
    }
}
