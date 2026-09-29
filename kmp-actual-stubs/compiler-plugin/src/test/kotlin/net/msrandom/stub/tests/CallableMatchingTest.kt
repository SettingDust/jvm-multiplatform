package net.msrandom.stub.tests

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarFile
import kotlin.test.*

/** Packaged-plugin callable regressions; deliberately excludes the historical probe controls. */
class CallableMatchingTest {
    private val plugin = File(requireNotNull(System.getProperty("callable.plugin")))
    private val root = File(System.getProperty("callable.evidence", "build/callable-evidence"))
    private val stdlib =
        File(
            Unit::class.java.protectionDomain.codeSource.location
                .toURI(),
        )

    private data class Result(
        val exit: ExitCode,
        val diagnostics: String,
        val classes: Map<String, ClassNode>,
    ) {
        fun method(
            name: String,
            descriptor: String,
        ): Pair<String, MethodNode> {
            val methods =
                classes.flatMap { (owner, node) ->
                    node.methods.filter { it.name == name && it.desc == descriptor }.map {
                        owner to
                            it
                    }
                }
            assertEquals(1, methods.size, "$name$descriptor owners=${methods.map { it.first }}")
            return methods.single()
        }

        fun real(
            name: String,
            descriptor: String,
            value: Int = 73,
        ) {
            val code = method(name, descriptor).second.instructions.toArray()
            assertTrue(code.any { it is IntInsnNode && it.operand == value }, "$name must retain $value")
            assertFalse(code.any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" })
        }

        fun stub(
            name: String,
            descriptor: String,
        ) {
            val code = method(name, descriptor).second.instructions.toArray()
            assertTrue(code.any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" }, name)
            assertTrue(code.any { it is LdcInsnNode && it.cst == "Common modules are not runnable" }, name)
            assertTrue(code.any { it.opcode == Opcodes.ATHROW }, name)
        }

        fun call(
            caller: String,
            target: String,
            descriptor: String,
        ) {
            val owner = method(target, descriptor).first
            val code =
                classes.values
                    .flatMap { it.methods }
                    .single { it.name == caller }
                    .instructions
                    .toArray()
            assertTrue(
                code.any {
                    it is MethodInsnNode && it.owner == owner && it.name == target && it.desc == descriptor
                },
                "$caller -> $owner.$target$descriptor",
            )
        }
    }

    private fun compile(
        name: String,
        common: String,
        actual: String,
        enabled: Boolean = true,
        reverse: Boolean = false,
        childFiles: Map<String, String> = emptyMap(),
        intermediateFiles: Map<String, String> = emptyMap(),
        excludedFiles: Map<String, String> = emptyMap(),
        siblingFiles: Map<String, String> = emptyMap(),
        lineSeparator: String? = null,
    ): Result {
        assertEquals("2.2.20", KotlinCompilerVersion.VERSION)
        val dir = File(root, name)
        dir.deleteRecursively()
        dir.mkdirs()
        val c =
            File(dir, "odd/common/Same.kt").apply {
                parentFile.mkdirs()
                writeText("@file:kotlin.jvm.JvmName(\"CommonDeclarations\")\n$common")
            }
        val a =
            File(dir, "elsewhere/child/Same.kt").apply {
                parentFile.mkdirs()
                writeText("@file:kotlin.jvm.JvmName(\"ActualDeclarations\")\n$actual")
            }
        val annotation =
            File(dir, "Stub.kt").apply {
                writeText(
                    "package net.msrandom.stub\n@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CLASS) annotation class Stub",
                )
            }
        val extra =
            childFiles.map { (path, text) ->
                File(dir, "extra/$path").apply {
                    parentFile.mkdirs()
                    writeText(text)
                }
            }
        val middle =
            intermediateFiles.map { (path, text) ->
                File(dir, "intermediate/$path").apply { parentFile.mkdirs(); writeText(text) }
            }
        excludedFiles.forEach { (path, text) ->
            File(dir, "excluded/$path").apply { parentFile.mkdirs(); writeText(text) }
        }
        val sibling =
            siblingFiles.map { (path, text) ->
                File(dir, "sibling/$path").apply {
                    parentFile.mkdirs()
                    writeText(text)
                }
            }
        val leaf = if (sibling.isEmpty()) emptyList() else listOf(File(dir, "Leaf.kt").apply { writeText("package leaf") })
        val files = (listOf(c, annotation, a) + extra + middle + sibling + leaf).let { if (reverse) it.reversed() else it }
        if (lineSeparator != null) files.forEach { it.writeText(it.readText().replace("\r\n", "\n").replace("\n", lineSeparator)) }
        val output = File(dir, "out")
        val argv =
            listOf(
                "-language-version",
                "2.1",
                "-api-version",
                "2.1",
                "-jvm-target",
                "1.8",
                "-no-stdlib",
                "-no-reflect",
                "-classpath",
                stdlib.absolutePath,
                "-Xmulti-platform",
                "-Xrender-internal-diagnostic-names",
                "-Xfragments=" + (listOf("common") + (if (middle.isEmpty()) emptyList() else listOf("middle")) + "child" +
                    (if (sibling.isEmpty()) emptyList() else listOf("sibling", "leaf"))).joinToString(","),
                "-Xfragment-sources=" +
                    (
                        listOf("common:${c.absolutePath}", "common:${annotation.absolutePath}", "child:${a.absolutePath}") +
                            middle.map { "middle:${it.absolutePath}" } +
                            extra.map { "child:${it.absolutePath}" } +
                            sibling.map { "sibling:${it.absolutePath}" } +
                            leaf.map { "leaf:${it.absolutePath}" }
                    ).joinToString(","),
                "-Xfragment-refines=" + (listOf(if (middle.isEmpty()) "child:common" else "middle:common,child:middle") +
                    (if (sibling.isEmpty()) emptyList() else listOf("sibling:common", "leaf:child", "leaf:sibling"))).joinToString(","),
                "-d",
                output.absolutePath,
            ) +
                (if (enabled) listOf("-Xplugin=${plugin.absolutePath}") else emptyList()) + files.map { it.absolutePath }
        val messages = StringBuilder()
        var errors = false
        val collector =
            object : MessageCollector {
                override fun clear() {
                    errors = false
                }

                override fun hasErrors() = errors

                override fun report(
                    severity: CompilerMessageSeverity,
                    message: String,
                    location: CompilerMessageSourceLocation?,
                ) {
                    if (severity.isError) errors = true
                    messages.appendLine("$severity ${location?.path}:${location?.line}:${location?.column} $message")
                }
            }
        val compiler = K2JVMCompiler()
        val exit =
            compiler.exec(
                collector,
                Services.EMPTY,
                compiler.createArguments().also { compiler.parseArguments(argv.toTypedArray(), it) },
            )
        File(dir, "compile.log").writeText("${argv.joinToString("\n")}\nEXIT=$exit\n$messages")
        println("$name EXIT=$exit\n$messages")
        assertNotEquals(ExitCode.INTERNAL_ERROR, exit, messages.toString())
        val classes =
            if (exit == ExitCode.OK) {
                output
                    .walkTopDown()
                    .filter { it.extension == "class" }
                    .map { f ->
                        ClassNode().also { ClassReader(f.readBytes()).accept(it, 0) }.let { it.name to it }
                    }.toMap()
            } else {
                emptyMap()
            }
        File(dir, "methods.txt").writeText(
            classes
                .flatMap { (owner, node) ->
                    node.methods.map { "$owner.${it.name}${it.desc}" }
                }.joinToString("\n"),
        )
        return Result(exit, messages.toString(), classes)
    }

    private fun positive(
        name: String,
        common: String,
        actual: String,
        reverse: Boolean = false,
    ): Result = compile(name, common, actual, reverse = reverse).also { assertEquals(ExitCode.OK, it.exit, it.diagnostics) }

    @Test fun crlfCallableInput() {
        val common = "package sample\nexpect fun pair(): Int\nfun callPair() = pair()\n"
        val actual = "package sample\nactual fun pair(): Int = 73\n"
        for ((name, enabled, separator) in listOf(
            Triple("crlf-native", false, "\r\n"),
            Triple("lf-plugin", true, "\n"),
            Triple("crlf-plugin", true, "\r\n"),
        )) {
            val result = compile(name, common, actual, enabled = enabled, lineSeparator = separator)
            assertEquals(ExitCode.OK, result.exit, result.diagnostics)
            result.real("pair", "()I")
            result.call("callPair", "pair", "()I")
        }
        val mixed =
            compile(
                "crlf-mixed-plugin",
                common + "expect fun missing(): Int\nfun callMissing() = missing()\n",
                actual,
                lineSeparator = "\r\n",
            )
        assertEquals(ExitCode.OK, mixed.exit, mixed.diagnostics)
        mixed.real("pair", "()I")
        mixed.stub("missing", "()I")
        mixed.call("callMissing", "missing", "()I")

        val malformed =
            compile(
                "crlf-malformed-plugin",
                common,
                "package sample\nactual fun pair( = 73\n",
                lineSeparator = "\r\n",
            )
        assertEquals(ExitCode.COMPILATION_ERROR, malformed.exit, malformed.diagnostics)
        assertTrue(malformed.diagnostics.contains("STUB_INPUT_UNAVAILABLE: source parse error"), malformed.diagnostics)
    }

    @Test fun packaging() {
        JarFile(plugin).use { jar ->
            for ((service, registrar) in listOf(
                "CompilerPluginRegistrar" to "StubCompilerPlugin",
                "ComponentRegistrar" to "StubInputRegistrar",
            )) {
                val entry = assertNotNull(jar.getJarEntry("META-INF/services/org.jetbrains.kotlin.compiler.plugin.$service"))
                assertEquals(
                    "net.msrandom.stub.$registrar",
                    jar
                        .getInputStream(entry)
                        .bufferedReader()
                        .readText()
                        .trim(),
                )
            }
        }
        root.mkdirs()

        fun hash(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        File(
            root,
            "provenance.txt",
        ).writeText(
            "plugin=$plugin\nsha256=${hash(
                plugin,
            )}\ncompiler=${KotlinCompilerVersion.VERSION}\ncompilerSource=${K2JVMCompiler::class.java.protectionDomain.codeSource.location}\njvm=${System.getProperty(
                "java.version",
            )}\nlanguage=2.1 api=2.1\n",
        )
    }

    @Test fun mixedAndEmptySourceBucket() {
        for (reverse in listOf(false, true)) {
            val r =
                positive(
                    "mixed-$reverse",
                    """
                    package sample
                    class Token
                    expect fun `real value`(): Int
                    expect fun absent(token: Token): String
                    expect fun unit(): Unit
                    fun callReal() = `real value`()
                    fun callMissing(t: Token) = absent(t)
                    // actual fun absent(token: Token): String = "fake"
                    val text = "actual fun unit() {}"
                    fun ordinary() = 91
                    @net.msrandom.stub.Stub fun explicit() = 23
                    """.trimIndent(),
                    "package sample\nactual fun `real value`(): Int = 73",
                    reverse,
                )
            r.real("real value", "()I")
            r.stub("absent", "(Lsample/Token;)Ljava/lang/String;")
            r.stub("unit", "()V")
            r.real("ordinary", "()I", 91)
            r.stub("explicit", "()I")
            r.call("callReal", "real value", "()I")
            r.call("callMissing", "absent", "(Lsample/Token;)Ljava/lang/String;")
        }
    }

    @Test fun markerOwnership() {
        val r =
            positive(
                "marker-ownership",
                """
                package sample
                fun ordinaryMarker() = 91
                val ordinaryProperty: Int get() = 92
                @net.msrandom.stub.Stub fun explicitMarker() = 23
                @net.msrandom.stub.Stub val explicitProperty: Int get() = 24
                expect fun generatedMarker(): Int
                expect val generatedProperty: Int
                expect fun retainedMarker(): Int
                expect val retainedProperty: Int
                """.trimIndent(),
                "package sample\nactual fun retainedMarker(): Int = 73\nactual val retainedProperty: Int get() = 74",
            )
        r.real("ordinaryMarker", "()I", 91)
        r.real("getOrdinaryProperty", "()I", 92)
        r.real("retainedMarker", "()I")
        r.real("getRetainedProperty", "()I", 74)
        for (name in listOf("explicitMarker", "getExplicitProperty", "generatedMarker", "getGeneratedProperty")) r.stub(name, "()I")

        fun hasStub(name: String): Boolean {
            val method = r.method(name, "()I").second
            return (method.visibleAnnotations.orEmpty() + method.invisibleAnnotations.orEmpty()).any {
                it.desc == "Lnet/msrandom/stub/Stub;"
            }
        }
        assertTrue(hasStub("explicitMarker"))
        assertFalse(hasStub("generatedMarker"))
        assertFalse(hasStub("retainedMarker"))
    }

    @Test fun markerDiagnosticIsolation() {
        for ((name, declaration, category) in listOf(
            Triple("function", "fun invalid(): Int", "NON_MEMBER_FUNCTION_NO_BODY"),
            Triple("property", "val invalid: Int", "MUST_BE_INITIALIZED"),
            Triple("extension", "val String.invalid: Int", "EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT"),
        )) {
            for (explicit in listOf(false, true)) {
                val invalid = (if (explicit) "@net.msrandom.stub.Stub " else "") + declaration
                for (enabled in listOf(false, true)) {
                    val r =
                        compile(
                            "marker-invalid-$name-$explicit-$enabled",
                            "package sample\nexpect fun retained(): Int\n$invalid" + if (enabled) "\nexpect fun missing(): Int" else "",
                            "package sample\nactual fun retained(): Int = 73",
                            enabled = enabled,
                        )
                    assertEquals(ExitCode.COMPILATION_ERROR, r.exit, r.diagnostics)
                    assertTrue(r.diagnostics.contains("[$category]"), r.diagnostics)
                }
            }
        }
    }

    @Test fun markerClassAndEnumRegression() {
        val r =
            positive(
                "marker-class-enum",
                """
                package sample
                expect class MissingClass
                expect enum class MissingEnum { ONE, TWO }
                @net.msrandom.stub.Stub class ExplicitClass { fun member() = 23 }
                class OrdinaryClass { fun ordinaryMember() = 91 }
                """.trimIndent(),
                "package sample",
            )
        r.stub("member", "()I")
        r.real("ordinaryMember", "()I", 91)
        for (owner in listOf("sample/MissingClass", "sample/MissingEnum", "sample/ExplicitClass")) {
            val node = assertNotNull(r.classes[owner])
            val constructors = node.methods.filter { it.name == "<init>" }
            assertTrue(constructors.isNotEmpty(), owner)
            for (constructor in constructors) {
                val code = constructor.instructions.toArray()
                assertTrue(code.any { it is LdcInsnNode && it.cst == "Common modules are not runnable" }, owner)
                assertTrue(code.any { it.opcode == Opcodes.ATHROW }, owner)
            }
            val annotations = node.visibleAnnotations.orEmpty() + node.invisibleAnnotations.orEmpty()
            assertEquals(owner == "sample/ExplicitClass", annotations.any { it.desc == "Lnet/msrandom/stub/Stub;" }, owner)
        }
        assertTrue(
            r.classes
                .getValue("sample/MissingEnum")
                .fields
                .map { it.name }
                .containsAll(listOf("ONE", "TWO")),
        )
    }

    @Test fun overloads() {
        for ((name, definitions, left, right) in listOf(
            listOf("library", "", "Int", "String"),
            listOf("source", "class Token\nclass Other", "Token", "Other"),
            listOf("alias", "class Token\ntypealias Alias = Token", "Alias", "String"),
        )) {
            for (receiver in listOf(false, true)) {
                for (reverse in listOf(false, true)) {
                    fun declaration(type: String) = if (receiver) "fun $type.choose(): Int" else "fun choose(value: $type): Int"

                    fun call(type: String) = if (receiver) "value.choose()" else "choose(value)"
                    val common = "package sample\n$definitions\nexpect ${declaration(
                        left,
                    )}\nexpect ${declaration(
                        right,
                    )}\nfun callLeft(value: $left) = ${call(left)}\nfun callRight(value: $right) = ${call(right)}"
                    val r =
                        positive("overload-$name-$receiver-$reverse", common, "package sample\nactual ${declaration(left)} = 73", reverse)
                    val first = if (left == "Int") "I" else "Lsample/Token;"
                    val second = if (right == "Other") "Lsample/Other;" else "Ljava/lang/String;"
                    r.real("choose", "($first)I")
                    r.stub("choose", "($second)I")
                    r.call("callLeft", "choose", "($first)I")
                    r.call("callRight", "choose", "($second)I")
                }
            }
        }
    }

    @Test fun properties() {
        val r =
            positive(
                "properties",
                """
                package sample
                expect val real: Int
                expect val missingVal: Int
                expect var missingVar: String
                expect var String.extension: Int
                fun useReal() = real
                fun useVal() = missingVal
                fun useVar(): String { missingVar = "x"; return missingVar }
                fun useExtension(): Int { "x".extension = 1; return "x".extension }
                """.trimIndent(),
                "package sample\nactual val real: Int get() = 73",
            )
        r.real("getReal", "()I")
        r.stub("getMissingVal", "()I")
        r.stub("getMissingVar", "()Ljava/lang/String;")
        r.stub("setMissingVar", "(Ljava/lang/String;)V")
        r.stub("getExtension", "(Ljava/lang/String;)I")
        r.stub("setExtension", "(Ljava/lang/String;I)V")
        r.call("useReal", "getReal", "()I")
    }

    @Test fun nativeErrors() {
        val cases =
            listOf(
                Triple("parameter", "expect fun pair(value: Int): Int", "actual fun pair(value: String): Int = 73"),
                Triple("receiver", "expect fun Int.pair(): Int", "actual fun String.pair(): Int = 73"),
                Triple("generic", "expect fun <T> pair(value: T): Int", "actual fun pair(value: Int): Int = 73"),
                Triple("return", "expect fun pair(): Int", "actual fun pair(): String = \"wrong\""),
                Triple("inferred", "expect fun pair(): Int", "actual fun pair() = \"wrong\""),
                Triple("propertyType", "expect val pair: Int", "actual val pair: String = \"wrong\""),
                Triple("mutability", "expect var pair: Int", "actual val pair: Int = 73"),
                Triple("orphan", "expect fun pair(): Int", "actual fun pair(): Int = 73\nactual fun orphan(): Int = 1"),
                Triple("orphanProperty", "expect fun pair(): Int", "actual fun pair(): Int = 73\nactual val orphan: Int = 1"),
                Triple("sourceError", "expect fun pair(): Int\nval bad: Int = \"wrong\"", "actual fun pair(): Int = 73"),
            )
        for ((name, common, actual) in cases) {
            val control = compile("negative-$name-native", "package sample\n$common", "package sample\n$actual", enabled = false)
            val changed = compile("negative-$name-plugin", "package sample\n$common", "package sample\n$actual")
            assertEquals(ExitCode.COMPILATION_ERROR, control.exit)
            assertEquals(ExitCode.COMPILATION_ERROR, changed.exit)
            val category =
                when (name) {
                    "return", "inferred", "propertyType" -> "EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE"
                    "sourceError" -> "INITIALIZER_TYPE_MISMATCH"
                    "mutability" -> "EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_KIND"
                    else -> "ACTUAL_WITHOUT_EXPECT"
                }
            assertTrue(control.diagnostics.contains(category), control.diagnostics)
            assertTrue(changed.diagnostics.contains(category), changed.diagnostics)
        }
    }

    @Test fun genericsAndImports() {
        val r =
            positive(
                "generics",
                """
                package sample
                class Token
                expect fun <T : CharSequence> choose(value: T): Int
                expect fun <T : Number> choose(value: T): Int
                fun callLeft(value: String) = choose(value)
                fun callRight(value: Int) = choose(value)
                """.trimIndent(),
                "package sample\nactual fun <T : CharSequence> choose(value: T): Int = 73",
            )
        r.real("choose", "(Ljava/lang/CharSequence;)I")
        r.stub("choose", "(Ljava/lang/Number;)I")
        val imports =
            positive(
                "imports",
                "package sample\nclass Token\nexpect fun choose(value: Token): Int\nexpect fun choose(value: String): Int",
                "package sample\nimport sample.Token as Imported\nactual fun choose(value: Imported): Int = 73",
            )
        imports.real("choose", "(Lsample/Token;)I")
        imports.stub("choose", "(Ljava/lang/String;)I")
    }

    @Test fun futureAliasFiles() {
        for ((name, imports, type, definition) in listOf(
            listOf("same-package", "", "Alias", "package sample\ntypealias Alias = Token"),
            listOf("explicit", "import aliases.Alias", "Alias", "package aliases\nimport sample.Token\ntypealias Alias = Token"),
            listOf(
                "renamed",
                "import aliases.Alias as Renamed",
                "Renamed",
                "package aliases\nimport sample.Token\ntypealias Alias = Token",
            ),
            listOf("qualified", "", "aliases.Alias", "package aliases\nimport sample.Token\ntypealias Alias = Token"),
            listOf(
                "generic",
                "import aliases.Alias as Renamed",
                "Renamed<Token>",
                "package aliases\nimport types.Sequence as Expanded\ntypealias Alias<T> = Expanded<T>",
            ),
        )) {
            val left = if (name == "generic") "List<Token>" else "Token"
            val descriptor = if (name == "generic") "Ljava/util/List;" else "Lsample/Token;"
            val common = "package sample\nclass Token\nexpect fun choose(value: $left): Int\nexpect fun choose(value: String): Int"
            val actual = "package sample\n$imports\nactual fun choose(value: $type): Int = 73"
            val extra =
                buildMap {
                    put("unrelated/Same.kt", definition)
                    if (name == "generic") put("nested/Same.kt", "package types\ntypealias Sequence<T> = List<T>")
                    if (name == "explicit") put("shadow/Same.kt", "package sample\ntypealias Alias = String")
                }
            for (reverse in listOf(false, true)) {
                val native =
                    compile(
                        "alias-files-$name-native-$reverse",
                        common,
                        "$actual\nactual fun choose(value: String): Int = 74",
                        enabled = false,
                        reverse = reverse,
                        childFiles = extra,
                    )
                assertEquals(ExitCode.OK, native.exit, native.diagnostics)
                native.real("choose", "($descriptor)I")
                native.real("choose", "(Ljava/lang/String;)I", 74)
                val result = compile("alias-files-$name-plugin-$reverse", common, actual, reverse = reverse, childFiles = extra)
                assertEquals(ExitCode.OK, result.exit, result.diagnostics)
                result.real("choose", "($descriptor)I")
                result.stub("choose", "(Ljava/lang/String;)I")
            }
        }
    }

    @Test fun futureAliasVisibilityErrors() {
        val common = "package sample\nclass Token\nexpect fun choose(value: Token): Int\nexpect fun choose(value: String): Int"
        for ((name, imports, type, definition, category) in listOf(
            listOf("no-import", "", "Alias", "package aliases\ntypealias Alias = sample.Token", "UNRESOLVED_REFERENCE"),
            listOf(
                "renamed-original",
                "import aliases.Alias as Renamed",
                "Alias",
                "package aliases\ntypealias Alias = sample.Token",
                "UNRESOLVED_REFERENCE",
            ),
            listOf("private", "", "Alias", "package sample\nprivate typealias Alias = Token", "INVISIBLE_REFERENCE"),
            listOf("sibling", "import aliases.Alias", "Alias", "package aliases\ntypealias Alias = sample.Token", "UNRESOLVED_REFERENCE"),
        )) {
            val extra = mapOf("unrelated/Same.kt" to definition)
            for (enabled in listOf(false, true)) {
                val result =
                    compile(
                        "alias-visibility-$name-$enabled",
                        common,
                        "package sample\n$imports\nactual fun choose(value: $type): Int = 73\nactual fun choose(value: String): Int = 74",
                        enabled = enabled,
                        childFiles = if (name == "sibling") emptyMap() else extra,
                        siblingFiles = if (name == "sibling") extra else emptyMap(),
                    )
                assertEquals(ExitCode.COMPILATION_ERROR, result.exit, result.diagnostics)
                assertTrue(result.diagnostics.contains("[$category]"), result.diagnostics)
            }
        }
    }

    @Test fun sameNameOtherOwner() {
        val common =
            """
            package sample
            class Token
            class Other { fun choose(value: Token) = 91 }
            expect fun choose(value: Token): Int
            expect fun choose(value: String): Int
            fun callReal(value: Token) = choose(value)
            fun callMissing(value: String) = choose(value)
            """.trimIndent()
        for (reverse in listOf(false, true)) {
            val actual = "package sample\nactual fun choose(value: Token): Int = 73"
            val native =
                compile(
                    "owner-native-$reverse",
                    common,
                    "$actual\nactual fun choose(value: String): Int = 74",
                    enabled = false,
                    reverse = reverse,
                    childFiles = mapOf("other/Same.kt" to "package other\nfun choose(value: sample.Token) = 92"),
                )
            assertEquals(ExitCode.OK, native.exit, native.diagnostics)
            val r =
                compile(
                    "owner-plugin-$reverse",
                    common,
                    actual,
                    reverse = reverse,
                    childFiles = mapOf("other/Same.kt" to "package other\nfun choose(value: sample.Token) = 92"),
                )
            assertEquals(ExitCode.OK, r.exit, r.diagnostics)
            val topLevel = r.copy(classes = r.classes.filterKeys { it != "sample/Other" && it != "other/SameKt" })
            topLevel.real("choose", "(Lsample/Token;)I")
            topLevel.stub("choose", "(Ljava/lang/String;)I")
            topLevel.call("callReal", "choose", "(Lsample/Token;)I")
            topLevel.call("callMissing", "choose", "(Ljava/lang/String;)I")
            assertTrue(
                r.classes
                    .getValue("sample/Other")
                    .methods
                    .any { it.name == "choose" && it.desc == "(Lsample/Token;)I" },
            )
            assertTrue(
                r.classes
                    .getValue("other/SameKt")
                    .methods
                    .any { it.name == "choose" && it.desc == "(Lsample/Token;)I" },
            )
        }
        val orphan =
            compile("owner-orphan-native", common, "package other\nactual fun choose(value: sample.Token): Int = 73", enabled = false)
        val rejected = compile("owner-orphan-plugin", common, "package other\nactual fun choose(value: sample.Token): Int = 73")
        for (r in listOf(orphan, rejected)) {
            assertEquals(ExitCode.COMPILATION_ERROR, r.exit, r.diagnostics)
            assertTrue(r.diagnostics.contains("[ACTUAL_WITHOUT_EXPECT]"), r.diagnostics)
        }
    }

    @Test fun sourceBoundsAndSubstitution() {
        val common =
            """
            package sample
            open class Root
            class Token : Root()
            class Other : Root()
            expect fun <T : Token> choose(value: T): Int
            expect fun <T : Other> choose(value: T): Int
            fun callReal(value: Token) = choose(value)
            fun callMissing(value: Other) = choose(value)
            """.trimIndent()
        val actual = "package sample\nactual fun <T : Token> choose(value: T): Int = 73"
        for (reverse in listOf(false, true)) {
            val native =
                compile(
                    "bounds-native-$reverse",
                    common,
                    "$actual\nactual fun <T : Other> choose(value: T): Int = 74",
                    enabled = false,
                    reverse = reverse,
                )
            assertEquals(ExitCode.OK, native.exit, native.diagnostics)
            val r = compile("bounds-plugin-$reverse", common, actual, reverse = reverse)
            assertEquals(ExitCode.OK, r.exit, r.diagnostics)
            r.real("choose", "(Lsample/Token;)I")
            r.stub("choose", "(Lsample/Other;)I")
            r.call("callReal", "choose", "(Lsample/Token;)I")
            r.call("callMissing", "choose", "(Lsample/Other;)I")
        }
        val aliasCommon =
            """
            package sample
            open class Root
            expect fun <T : Root> pick(value: List<T>): Int
            expect fun <T : Root> pick(value: Set<T>): Int
            fun callList(value: List<Root>) = pick(value)
            fun callSet(value: Set<Root>) = pick(value)
            """.trimIndent()
        val aliasActual = "package sample\ntypealias Box<T> = List<T>\nactual fun <T : Root> pick(value: Box<T>): Int = 73"
        val aliasNative =
            compile(
                "bounds-alias-native",
                aliasCommon,
                "$aliasActual\nactual fun <T : Root> pick(value: Set<T>): Int = 74",
                enabled = false,
            )
        assertEquals(ExitCode.OK, aliasNative.exit, aliasNative.diagnostics)
        val aliasPlugin = positive("bounds-alias-plugin", aliasCommon, aliasActual)
        aliasPlugin.real("pick", "(Ljava/util/List;)I")
        aliasPlugin.stub("pick", "(Ljava/util/Set;)I")
        aliasPlugin.call("callList", "pick", "(Ljava/util/List;)I")
        aliasPlugin.call("callSet", "pick", "(Ljava/util/Set;)I")
        val invalid = "package sample\nactual fun <T : Root> choose(value: T): Int = 73"
        for (enabled in listOf(false, true)) {
            val r = compile("bounds-invalid-$enabled", common, invalid, enabled = enabled)
            assertEquals(ExitCode.COMPILATION_ERROR, r.exit, r.diagnostics)
            assertTrue(r.diagnostics.contains("[ACTUAL_WITHOUT_EXPECT]"), r.diagnostics)
        }
    }

    @Test fun propertyAccessorVariants() {
        val common =
            """
            package sample
            expect var retained: Int
            expect val String.retainedExtension: Int
            expect val String.missingExtension: Int
            expect var String.missingMutable: Int
            fun readRetained() = retained
            fun writeRetained() { retained = 1 }
            fun readExtension() = "x".retainedExtension
            """.trimIndent()
        val missingCalls =
            """
            package sample
            fun readMissing() = "x".missingExtension
            fun useMutable(): Int { "x".missingMutable = 1; return "x".missingMutable }
            """.trimIndent()
        val actual =
            """
            package sample
            actual var retained: Int = 73
            actual val String.retainedExtension: Int get() = 74
            """.trimIndent()
        for (reverse in listOf(false, true)) {
            val native =
                compile(
                    "property-variants-native-$reverse",
                    common,
                    "$actual\nactual val String.missingExtension: Int get() = 75\nactual var String.missingMutable: Int get() = 76; set(value) {}",
                    enabled = false,
                    reverse = reverse,
                    childFiles = mapOf("calls/Calls.kt" to missingCalls),
                )
            assertEquals(ExitCode.OK, native.exit, native.diagnostics)
            val r =
                compile(
                    "property-variants-plugin-$reverse",
                    common,
                    actual,
                    reverse = reverse,
                    childFiles = mapOf("calls/Calls.kt" to missingCalls),
                )
            assertEquals(ExitCode.OK, r.exit, r.diagnostics)
            r.method("getRetained", "()I")
            r.method("setRetained", "(I)V")
            r.real("getRetainedExtension", "(Ljava/lang/String;)I", 74)
            r.stub("getMissingExtension", "(Ljava/lang/String;)I")
            r.stub("getMissingMutable", "(Ljava/lang/String;)I")
            r.stub("setMissingMutable", "(Ljava/lang/String;I)V")
            r.call("readRetained", "getRetained", "()I")
            r.call("writeRetained", "setRetained", "(I)V")
            r.call("readExtension", "getRetainedExtension", "(Ljava/lang/String;)I")
            r.call("readMissing", "getMissingExtension", "(Ljava/lang/String;)I")
            r.call("useMutable", "getMissingMutable", "(Ljava/lang/String;)I")
            r.call("useMutable", "setMissingMutable", "(Ljava/lang/String;I)V")
        }
    }

    @Test fun partialExtensionPropertyOverloads() {
        val common =
            """
            package sample
            expect val String.choice: Int
            expect val Int.choice: Int
            fun callReal() = "x".choice
            fun callMissing() = 1.choice
            """.trimIndent()
        val actual = "package sample\nactual val String.choice: Int get() = 73"
        for (reverse in listOf(false, true)) {
            val native =
                compile(
                    "property-overload-native-$reverse",
                    common,
                    "$actual\nactual val Int.choice: Int get() = 74",
                    enabled = false,
                    reverse = reverse,
                )
            assertEquals(ExitCode.OK, native.exit, native.diagnostics)
            val r = compile("property-overload-plugin-$reverse", common, actual, reverse = reverse)
            assertEquals(ExitCode.OK, r.exit, r.diagnostics)
            r.real("getChoice", "(Ljava/lang/String;)I")
            r.stub("getChoice", "(I)I")
            r.call("callReal", "getChoice", "(Ljava/lang/String;)I")
        }
        val inferred = "package sample\nactual val String.choice get() = 73"
        val inferredNative =
            compile(
                "property-inferred-native",
                common,
                "$inferred\nactual val Int.choice: Int get() = 74",
                enabled = false,
            )
        assertEquals(ExitCode.OK, inferredNative.exit, inferredNative.diagnostics)
        val inferredPlugin = positive("property-inferred-plugin", common, inferred)
        inferredPlugin.real("getChoice", "(Ljava/lang/String;)I")
        inferredPlugin.stub("getChoice", "(I)I")
    }

    @Test fun multilineHeaderAndUnknown() {
        val common =
            """
            package sample
            class Token
            class Other
            expect fun <T : Token>
                `multi line`(
                    value: T
                ): Int
            expect fun <T : Other>
                `multi line`(
                    value: T
                ): Int
            fun readReal(t: Token) = `multi line`(t)
            fun readMissing(t: Other) = `multi line`(t)
            // actual fun `multi line`(value: Other) = 99
            val unrelated = "actual fun multi line"
            """.trimIndent()
        val actual =
            """
            package sample
            actual fun <T : Token>
                `multi line`(
                    value: T
                ): Int = 73
            """.trimIndent()
        for (reverse in listOf(false, true)) {
            val native =
                compile(
                    "multiline-native-$reverse",
                    common,
                    "$actual\nactual fun <T : Other> `multi line`(value: T): Int = 74",
                    enabled = false,
                    reverse = reverse,
                )
            assertEquals(ExitCode.OK, native.exit, native.diagnostics)
            val r = compile("multiline-plugin-$reverse", common, actual, reverse = reverse)
            assertEquals(ExitCode.OK, r.exit, r.diagnostics)
            r.real("multi line", "(Lsample/Token;)I")
            r.stub("multi line", "(Lsample/Other;)I")
            r.call("readReal", "multi line", "(Lsample/Token;)I")
            r.call("readMissing", "multi line", "(Lsample/Other;)I")
        }
    }

    @Test fun futureNestedSourceClassifier() {
        val common =
            """
            package sample
            class Outer { class Token }
            expect fun choose(value: Outer.Token): Int
            expect fun choose(value: String): Int
            fun callReal(value: Outer.Token) = choose(value)
            fun callMissing(value: String) = choose(value)
            """.trimIndent()
        val actual = "package sample\nactual fun choose(value: Outer.Token): Int = 73"
        for (reverse in listOf(false, true)) {
            val native =
                compile(
                    "nested-native-$reverse",
                    common,
                    "$actual\nactual fun choose(value: String): Int = 74",
                    enabled = false,
                    reverse = reverse,
                )
            assertEquals(ExitCode.OK, native.exit, native.diagnostics)
            val r = compile("nested-plugin-$reverse", common, actual, reverse = reverse)
            assertEquals(ExitCode.OK, r.exit, r.diagnostics)
            r.real("choose", "(Lsample/Outer\u0024Token;)I")
            r.stub("choose", "(Ljava/lang/String;)I")
            r.call("callReal", "choose", "(Lsample/Outer\u0024Token;)I")
            r.call("callMissing", "choose", "(Ljava/lang/String;)I")
        }
    }

    @Test fun forwardSourceSupertypeBound() {
        val common =
            """
            package sample
            open class Parent
            class Derived : Parent()
            class Other : Parent()
            expect fun <T : Derived> choose(value: T): Int
            expect fun <T : Other> choose(value: T): Int
            fun callReal(value: Derived) = choose(value)
            fun callMissing(value: Other) = choose(value)
            """.trimIndent()
        val actual = "package sample\nactual fun <T : Derived> choose(value: T): Int = 73"
        for (reverse in listOf(false, true)) {
            val native =
                compile(
                    "forward-supertype-native-$reverse",
                    common,
                    "$actual\nactual fun <T : Other> choose(value: T): Int = 74",
                    enabled = false,
                    reverse = reverse,
                )
            assertEquals(ExitCode.OK, native.exit, native.diagnostics)
            val r = compile("forward-supertype-plugin-$reverse", common, actual, reverse = reverse)
            assertEquals(ExitCode.OK, r.exit, r.diagnostics)
            r.real("choose", "(Lsample/Derived;)I")
            r.stub("choose", "(Lsample/Other;)I")
            r.call("callReal", "choose", "(Lsample/Derived;)I")
            r.call("callMissing", "choose", "(Lsample/Other;)I")
        }
    }

    @Test fun futureFunctionTypeAlias() {
        val common =
            """
            package sample
            expect fun choose(value: (Int) -> Int): Int
            expect fun choose(value: String): Int
            fun callReal(value: (Int) -> Int) = choose(value)
            fun callMissing(value: String) = choose(value)
            """.trimIndent()
        val actual = "package sample\nactual fun choose(value: Handler): Int = 73"
        val extra = mapOf("aliases/Alias.kt" to "package sample\ntypealias Handler = (Int) -> Int")
        for (reverse in listOf(false, true)) {
            val native =
                compile(
                    "function-alias-native-$reverse",
                    common,
                    "$actual\nactual fun choose(value: String): Int = 74",
                    enabled = false,
                    reverse = reverse,
                    childFiles = extra,
                )
            assertEquals(ExitCode.OK, native.exit, native.diagnostics)
            val r = compile("function-alias-plugin-$reverse", common, actual, reverse = reverse, childFiles = extra)
            assertEquals(ExitCode.OK, r.exit, r.diagnostics)
            r.real("choose", "(Lkotlin/jvm/functions/Function1;)I")
            r.stub("choose", "(Ljava/lang/String;)I")
            r.call("callReal", "choose", "(Lkotlin/jvm/functions/Function1;)I")
            r.call("callMissing", "choose", "(Ljava/lang/String;)I")
        }
    }

    @Test fun recursiveAliasRemainsAnError() {
        val common = "package sample\nexpect fun choose(value: String): Int\nexpect fun choose(value: Int): Int"
        val actual = "package sample\ntypealias Loop = Loop\nactual fun choose(value: Loop): Int = 73\nactual fun choose(value: String): Int = 74"
        for (enabled in listOf(false, true)) {
            val r = compile("recursive-alias-$enabled", common, actual, enabled = enabled)
            assertEquals(ExitCode.COMPILATION_ERROR, r.exit, r.diagnostics)
            assertTrue(r.diagnostics.contains("[RECURSIVE_TYPEALIAS_EXPANSION]"), r.diagnostics)
        }
    }

    @Test fun futureAliasOverload() {
        val common = "package sample\nclass Token\nexpect fun choose(value: Token): Int\nexpect fun choose(value: String): Int"
        val actual = "package sample\ntypealias Alias = Token\nactual fun choose(value: Alias): Int = 73"
        for (reverse in listOf(false, true)) {
            val native =
                compile(
                    "future-alias-native-$reverse",
                    common,
                    "$actual\nactual fun choose(value: String): Int = 74",
                    enabled = false,
                    reverse = reverse,
                )
            assertEquals(ExitCode.OK, native.exit, native.diagnostics)
            native.real("choose", "(Lsample/Token;)I")
            native.real("choose", "(Ljava/lang/String;)I", 74)
            val r = positive("future-alias-plugin-$reverse", common, actual, reverse)
            r.real("choose", "(Lsample/Token;)I")
            r.stub("choose", "(Ljava/lang/String;)I")
        }
    }

    @Test fun declarationClassesAndMembers() {
        val missing = compile(
            "p3-missing-class",
            """
            package sample
            expect class Implicit { fun ping(): Int; val property: Int }
            expect class Absent(value: Int) {
                val item: Int
                var mutable: Int
                fun compute(): Int
                class Inner() { fun nested(): Int }
            }
            expect interface MissingInterface { fun run(): Int; val value: Int }
            expect object MissingObject { fun run(): Int }
            expect annotation class MissingAnnotation(val number: Int)
            fun use(a: Absent): Int = a.compute() + a.item + a.mutable
            """.trimIndent(),
            "package sample",
        )
        assertEquals(ExitCode.OK, missing.exit, missing.diagnostics)
        val implicit = missing.classes.getValue("sample/Implicit")
        for ((name, desc) in listOf("<init>" to "()V", "ping" to "()I", "getProperty" to "()I")) {
            assertTrue(implicit.methods.single { it.name == name && it.desc == desc }.instructions.toArray().any {
                it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException"
            }, "Implicit.$name")
        }
        val missingClass = missing.classes.getValue("sample/Absent")
        for ((name, desc) in listOf("<init>" to "(I)V", "compute" to "()I", "getItem" to "()I", "getMutable" to "()I", "setMutable" to "(I)V")) {
            val code = missingClass.methods.single { it.name == name && it.desc == desc }.instructions.toArray()
            assertTrue(code.any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" }, name)
        }
        val nested = missing.classes.getValue("sample/Absent\u0024Inner")
        for ((name, desc) in listOf("<init>" to "()V", "nested" to "()I")) {
            assertTrue(nested.methods.single { it.name == name && it.desc == desc }.instructions.toArray().any {
                it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException"
            }, name)
        }
        val missingInterface = missing.classes.getValue("sample/MissingInterface")
        assertTrue(missingInterface.access and Opcodes.ACC_INTERFACE != 0)
        for (name in listOf("run", "getValue")) assertTrue(missingInterface.methods.single { it.name == name }.access and Opcodes.ACC_ABSTRACT != 0)
        val missingObject = missing.classes.getValue("sample/MissingObject")
        assertTrue(missingObject.fields.any { it.name == "INSTANCE" && it.desc == "Lsample/MissingObject;" })
        for (name in listOf("<init>", "run")) assertTrue(missingObject.methods.single { it.name == name }.instructions.toArray().any {
            it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException"
        })
        val annotation = missing.classes.getValue("sample/MissingAnnotation")
        assertTrue(annotation.access and Opcodes.ACC_ANNOTATION != 0)
        assertTrue(annotation.methods.any { it.name == "number" && it.desc == "()I" && it.access and Opcodes.ACC_ABSTRACT != 0 })
        for (owner in listOf("sample/Implicit", "sample/Absent", "sample/Absent\u0024Inner", "sample/MissingInterface", "sample/MissingObject", "sample/MissingAnnotation")) {
            assertNotNull(missing.classes[owner])
        }


        val common = """
            package sample
            expect class Matched(value: Int) { fun real(): Int; val item: Int; var mutable: Int; class Inner() { fun nested(): Int } }
            expect interface RealInterface { fun real(): Int }
            expect object RealObject { fun real(): Int }
            expect annotation class RealAnnotation(val number: Int)
            expect fun missing(): Int
            fun use(m: Matched) = m.real() + Matched.Inner().nested()
        """.trimIndent()
        val actual = """
            package sample
            actual class Matched actual constructor(value: Int) {
                actual fun real() = 73
                actual val item: Int get() = 74
                actual var mutable: Int = 75
                actual class Inner actual constructor() { actual fun nested() = 76 }
            }
            actual interface RealInterface { actual fun real(): Int }
            actual object RealObject { actual fun real() = 77 }
            actual annotation class RealAnnotation(actual val number: Int)
        """.trimIndent()
        val matched = compile("p3-matched-class", common, actual)
        assertEquals(ExitCode.OK, matched.exit, matched.diagnostics)
        for ((name, value) in listOf("real" to 73, "getItem" to 74, "nested" to 76)) {
            val owner = if (name == "nested") "sample/Matched\u0024Inner" else "sample/Matched"
            val instructions = matched.classes.getValue(owner).methods.single { it.name == name }.instructions.toArray()
            assertTrue(instructions.any { it is IntInsnNode && it.operand == value }, "$owner.$name")
            assertFalse(instructions.any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" })
        }
        val matchedClass = matched.classes.getValue("sample/Matched")
        assertEquals(1, matchedClass.methods.count { it.name == "<init>" && it.desc == "(I)V" })
        assertFalse(matchedClass.methods.single { it.name == "<init>" }.instructions.toArray().any {
            it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException"
        })
        for (name in listOf("getMutable", "setMutable")) assertFalse(matchedClass.methods.single { it.name == name }.instructions.toArray().any {
            it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException"
        })
        assertTrue(matched.classes.getValue("sample/RealInterface").methods.single { it.name == "real" }.access and Opcodes.ACC_ABSTRACT != 0)
        assertTrue(matched.classes.getValue("sample/RealObject").methods.single { it.name == "real" }.instructions.toArray().any {
            it is IntInsnNode && it.operand == 77
        })
        assertTrue(matched.classes.getValue("sample/RealAnnotation").methods.any {
            it.name == "number" && it.desc == "()I" && it.access and Opcodes.ACC_ABSTRACT != 0
        })
        matched.stub("missing", "()I")
        val call = matched.classes.values.flatMap { it.methods }.single { it.name == "use" }.instructions.toArray()
        assertTrue(call.any { it is MethodInsnNode && it.owner == "sample/Matched" && it.name == "real" && it.desc == "()I" })
    }

    @Test fun enumOwnership() {
        val common = """
            package sample
            expect enum class State { ON, OFF; fun score(): Int }
            fun stateName() = State.ON.name
            expect fun absent(): Int
        """.trimIndent()
        val missing = positive("p3-enum-missing", common, "package sample")
        assertNotNull(missing.classes["sample/State"])
        for (entry in listOf("ON", "OFF")) assertTrue(missing.classes.getValue("sample/State").fields.any { it.name == entry })
        missing.stub("score", "()I")
        val generatedConstructor = missing.classes.getValue("sample/State").methods.single { it.name == "<init>" }
        assertTrue(generatedConstructor.instructions.toArray().any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" })
        missing.stub("absent", "()I")
        val actual = "package sample\nactual enum class State { ON, OFF; actual fun score(): Int = 73 }"
        val matched = positive("p3-enum-matched", common, actual)
        matched.real("score", "()I")
        assertEquals(1, matched.classes.getValue("sample/State").methods.count { it.name == "<init>" }, "retained enum constructor")
        assertFalse(matched.classes.getValue("sample/State").methods.single { it.name == "<init>" }.instructions.toArray().any {
            it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException"
        })
        assertTrue(matched.classes.getValue("sample/State").fields.any { it.name == "ON" })
        for ((name, source) in listOf(
            "entry" to "actual enum class State { ON; actual fun score(): Int = 73 }",
            "member" to "actual enum class State { ON, OFF; actual fun score(): String = \"bad\" }",
        )) {
            val native = compile("p3-enum-$name-native", common, "package sample\n$source", enabled = false)
            val pluginResult = compile("p3-enum-$name-plugin", common, "package sample\n$source")
            assertEquals(ExitCode.COMPILATION_ERROR, native.exit, native.diagnostics)
            assertEquals(ExitCode.COMPILATION_ERROR, pluginResult.exit, pluginResult.diagnostics)
            assertTrue(pluginResult.diagnostics.contains("[EXPECT_ACTUAL") || pluginResult.diagnostics.contains("[NO_ACTUAL"), pluginResult.diagnostics)
        }
    }

    @Test fun typealiasOwnership() {
        val common = """
            package sample
            expect class Alias(value: Int) { fun value(): Int }
            expect fun absent(): Int
            fun useAlias(a: Alias) = a.value()
        """.trimIndent()
        val actual = "package sample\nclass Target(val value: Int) { fun value() = 73 }\nactual typealias Alias = Target"
        val matched = positive("p3-alias-matched", common, actual)
        assertNull(matched.classes["sample/Alias"])
        assertNotNull(matched.classes["sample/Target"])
        val target = matched.classes.getValue("sample/Target").methods.single { it.name == "value" && it.desc == "()I" }
        assertTrue(target.instructions.toArray().any { it is IntInsnNode && it.operand == 73 })
        val call = matched.classes.values.flatMap { it.methods }.single { it.name == "useAlias" }.instructions.toArray()
        assertTrue(call.any { it is MethodInsnNode && it.owner == "sample/Target" && it.name == "value" })
        matched.stub("absent", "()I")
        for ((name, source, diagnostic) in listOf(
            Triple("member", "class Target(val value: Int)\nactual typealias Alias = Target", "[NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS]"),
            Triple("target", "class Target(val value: String) { fun value() = 73 }\nactual typealias Alias = Target", "[NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS]"),
        )) {
            val native = compile("p3-alias-$name-native", common, "package sample\n$source", enabled = false)
            val pluginResult = compile("p3-alias-$name-plugin", common, "package sample\n$source")
            assertEquals(ExitCode.COMPILATION_ERROR, native.exit, native.diagnostics)
            assertEquals(ExitCode.COMPILATION_ERROR, pluginResult.exit, pluginResult.diagnostics)
            assertTrue(pluginResult.diagnostics.contains(diagnostic), pluginResult.diagnostics)
        }
        val orphan = compile("p3-alias-orphan", "package sample\nexpect fun pair(): Int", "package sample\nactual fun pair() = 73\nclass Target\nactual typealias Orphan = Target")
        assertEquals(ExitCode.COMPILATION_ERROR, orphan.exit, orphan.diagnostics)
        assertTrue(orphan.diagnostics.contains("[ACTUAL_WITHOUT_EXPECT]"), orphan.diagnostics)
    }

    @Test fun fragmentGraphAndExcludedSources() {
        val common = """
            package sample
            expect fun middle(): Int
            expect fun bottom(): Int
            expect fun absent(): Int
            expect class Holder { fun value(): Int }
            fun read() = middle() + bottom() + absent()
        """.trimIndent()
        val middle = mapOf("strange/Same.kt" to "package sample\nactual fun middle(): Int = 73\nactual class Holder { actual fun value() = 73 }")
        val leaf = "package sample\nactual fun bottom(): Int = 74"
        for (reverse in listOf(false, true)) {
            val r = compile("p3-chain-$reverse", common, leaf, reverse = reverse, intermediateFiles = middle)
            assertEquals(ExitCode.OK, r.exit, r.diagnostics)
            r.real("middle", "()I")
            r.real("bottom", "()I", 74)
            r.stub("absent", "()I")
            r.real("value", "()I")
            for (name in listOf("middle", "bottom", "absent")) r.call("read", name, "()I")
        }
        val excluded = compile(
            "p3-excluded",
            "package sample\nexpect fun absent(): Int\nexpect class Holder { fun value(): Int }",
            "package sample",
            excludedFiles = mapOf("strange/Same.kt" to "package sample\nactual fun absent() = 73\nactual class Holder { actual fun value() = 74 }"),
        )
        assertEquals(ExitCode.OK, excluded.exit, excluded.diagnostics)
        excluded.stub("absent", "()I")
        excluded.stub("value", "()I")
        assertTrue(excluded.classes.getValue("sample/Holder").methods.any { it.name == "value" })
    }

    @Test fun siblingOrphanAndDuplicateActual() {
        val siblingCommon = "package sample\nexpect fun pair(): Int\nexpect class Holder { fun value(): Int }"
        val siblingActual = "package sample\nactual fun pair() = 73\nactual class Holder { actual fun value() = 74 }"
        val sibling = mapOf("strange/Same.kt" to "package sample\nactual fun orphan() = 73\nactual class Stray")
        for (enabled in listOf(false, true)) {
            val r = compile("p3-sibling-$enabled", siblingCommon, siblingActual, enabled = enabled, siblingFiles = sibling)
            assertEquals(ExitCode.COMPILATION_ERROR, r.exit, r.diagnostics)
            assertTrue(r.diagnostics.contains("[ACTUAL_WITHOUT_EXPECT]"), r.diagnostics)
        }
        for (enabled in listOf(false, true)) {
            val r = compile("p3-duplicate-$enabled", siblingCommon,
                "$siblingActual\nactual fun pair(): Int = 75\nactual class Holder", enabled = enabled)
            assertEquals(ExitCode.COMPILATION_ERROR, r.exit, r.diagnostics)
            assertTrue(r.diagnostics.contains("[CONFLICTING_OVERLOADS]") || r.diagnostics.contains("[REDECLARATION]"), r.diagnostics)
        }
    }

    @Test fun classSyntaxAndOrder() {
        val common = """
            package sample
            // actual class Absent { actual fun value() = 74 }
            val irrelevant = "actual class Absent"
            expect class `Kept`
            constructor() {
                fun value(): Int
            }
            expect class `Absent` { fun value(): Int }
        """.trimIndent()
        val actual = """
            package sample
            actual class `Kept`
            actual constructor() {
                actual fun value() = 73
            }
        """.trimIndent()
        for (reverse in listOf(false, true)) {
            val r = compile("p3-class-syntax-$reverse", common, actual, reverse = reverse)
            assertEquals(ExitCode.OK, r.exit, r.diagnostics)
            val retained = r.classes.getValue("sample/Kept").methods.single { it.name == "value" && it.desc == "()I" }.instructions.toArray()
            assertTrue(retained.any { it is IntInsnNode && it.operand == 73 })
            assertFalse(retained.any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" })
            assertTrue(r.classes.getValue("sample/Absent").methods.single { it.name == "value" }.instructions.toArray().any {
                it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException"
            })
            assertEquals(1, r.classes.getValue("sample/Kept").methods.count { it.name == "<init>" })
        }
    }

    @Test fun ambiguousDiamondRemainsNative() {
        val common = "package sample\nexpect fun pair(): Int\nexpect class Holder { fun value(): Int }"
        val middle = mapOf("strange/Same.kt" to "package sample\nactual fun pair() = 73\nactual class Holder { actual fun value() = 73 }")
        val sibling = mapOf("strange/Same.kt" to "package sample\nactual fun pair() = 74\nactual class Holder { actual fun value() = 74 }")
        for (enabled in listOf(false, true)) {
            val r = compile("p3-diamond-$enabled", common, "package sample", enabled = enabled,
                intermediateFiles = middle, siblingFiles = sibling)
            assertEquals(ExitCode.COMPILATION_ERROR, r.exit, r.diagnostics)
            assertTrue(r.diagnostics.contains("[AMBIGUOUS_EXPECTS]") || r.diagnostics.contains("[AMBIGUOUS_ACTUALS]"), r.diagnostics)
            val classes = compile("p3-diamond-class-$enabled", "package sample\nexpect class Holder { fun value(): Int }",
                "package sample", enabled = enabled,
                intermediateFiles = mapOf("strange/Same.kt" to "package sample\nactual class Holder { actual fun value() = 73 }"),
                siblingFiles = mapOf("strange/Same.kt" to "package sample\nactual class Holder { actual fun value() = 74 }"))
            assertEquals(ExitCode.COMPILATION_ERROR, classes.exit, classes.diagnostics)
            assertTrue(classes.diagnostics.contains("[AMBIGUOUS_ACTUALS]") ||
                classes.diagnostics.contains("[DUPLICATE_CLASS_NAMES]"), classes.diagnostics)
        }
    }

    @Test fun orphanClassAndClassMarkerIsolation() {
        val common = "package sample\nexpect class Matched { fun value(): Int }\nexpect fun absent(): Int\nfun ordinary() = 91"
        val actual = "package sample\nactual class Matched { actual fun value() = 73 }\nactual class Orphan"
        for (enabled in listOf(false, true)) {
            val r = compile("p3-class-orphan-$enabled", common, actual, enabled = enabled)
            assertEquals(ExitCode.COMPILATION_ERROR, r.exit, r.diagnostics)
            assertTrue(r.diagnostics.contains("[ACTUAL_WITHOUT_EXPECT]"), r.diagnostics)
        }
        val good = positive("p3-class-ordinary", common,
            "package sample\nactual class Matched { actual fun value() = 73 }\n@net.msrandom.stub.Stub class Explicit { fun value() = 23 }")
        good.real("ordinary", "()I", 91)
        val real = good.classes.getValue("sample/Matched").methods.single { it.name == "value" }
        assertFalse(real.instructions.toArray().any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" })
        val explicit = good.classes.getValue("sample/Explicit").methods.single { it.name == "value" }
        assertTrue(explicit.instructions.toArray().any { it is TypeInsnNode && it.desc == "java/lang/UnsupportedOperationException" })
        good.stub("absent", "()I")
    }

    @Test fun classMemberErrorsStayNative() {
        val common = "package sample\nexpect class Matched(value: Int) { fun required(): Int; val item: Int; class Inner { fun nested(): Int } }"
        for ((name, body) in listOf(
            "missing" to "actual class Matched actual constructor(value: Int)",
            "incompatible" to "actual class Matched actual constructor(value: Int) { actual fun required(): String = \"wrong\"; actual val item: Int = 1; actual class Inner { actual fun nested() = 73 } }",
            "nested" to "actual class Matched actual constructor(value: Int) { actual fun required() = 73; actual val item: Int = 1 }",
            "constructor" to "actual class Matched { actual fun required() = 73; actual val item: Int = 1; actual class Inner { actual fun nested() = 73 } }",
        )) {
            val native = compile("p3-member-$name-native", common, "package sample\n$body", enabled = false)
            val pluginResult = compile("p3-member-$name-plugin", common, "package sample\n$body")
            assertEquals(ExitCode.COMPILATION_ERROR, native.exit, native.diagnostics)
            assertEquals(ExitCode.COMPILATION_ERROR, pluginResult.exit, pluginResult.diagnostics)
            assertTrue(pluginResult.diagnostics.contains("[NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS]") ||
                pluginResult.diagnostics.contains("[EXPECT_ACTUAL_INCOMPATIBLE") ||
                pluginResult.diagnostics.contains("[ACTUAL_MISSING]"), pluginResult.diagnostics)
            assertFalse(pluginResult.diagnostics.contains("STUB_INPUT_UNAVAILABLE"), pluginResult.diagnostics)
        }
    }
}
