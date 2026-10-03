package net.msrandom.classextensions.kotlin.plugin

import net.msrandom.classextensions.ClassExtension
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CodegenTest {
    @Test
    fun `CLI loads plugin and executes generated members`() {
        val output = Files.createTempDirectory("class-extension-consumer").toFile()
        try {
            val common = File("src/test/resources/test-common.kt").absolutePath
            val platform = File("src/test/resources/test-platform.kt").absolutePath
            val classpath =
                listOf(ClassExtension::class.java, Unit::class.java).joinToString(File.pathSeparator) {
                    File(
                        it.protectionDomain.codeSource.location
                            .toURI(),
                    ).path
                }
            val plugin =
                File(
                    ClassExtensionsPlugin::class.java.protectionDomain.codeSource.location
                        .toURI(),
                )
            val service =
                requireNotNull(
                    javaClass.classLoader.getResource(
                        "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar",
                    ),
                )
            val resources = File(service.toURI()).parentFile.parentFile.parentFile
            val messages = ByteArrayOutputStream()
            val exit =
                K2JVMCompiler().exec(
                    PrintStream(messages),
                    common,
                    platform,
                    "-language-version",
                    "2.2",
                    "-no-stdlib",
                    "-no-reflect",
                    "-classpath",
                    classpath,
                    "-d",
                    output.path,
                    "-Xplugin=${plugin.path},${resources.path}",
                    "-Xmulti-platform",
                    "-Xfragments=main,platform",
                    "-Xfragment-refines=platform:main",
                    "-Xfragment-sources=main:$common",
                    "-Xfragment-sources=platform:$platform",
                )
            assertEquals(ExitCode.OK, exit, messages.toString())
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
            }
            assertFalse(File(output, "a/b/c/AExtension.class").exists(), "Extension carrier must be removed by IR plugin")
        } finally {
            output.deleteRecursively()
        }
    }
}
