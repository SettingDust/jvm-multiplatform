package net.msrandom.stubs

import net.msrandom.stubs.ClassNodeIntersector.intersectClassNodes
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

object StubGenerator {
    internal data class ArtifactSpec(
        val file: File,
        val componentId: String?,
        val type: GenerateStubApi.ResolvedArtifact.Type?,
        val moduleId: String?,
        val moduleVersion: String?,
    )

    private fun createFileIntersection(
        streams: List<Pair<ClasspathLoader, ClassNode>>,
        output: Path,
        preserveMethodBodies: Boolean,
    ) {
        output.parent?.createDirectories()

        val node = intersectClassNodes(streams.map { it.second }, streams.map { it.first })

        if (preserveMethodBodies && streams.size > 1) {
            for (method in node.methods) {
                val originals =
                    streams.map { (_, original) ->
                        original.methods.first { it.name == method.name && it.desc == method.desc } as CodeMethodNode
                    }
                MethodBodies.preserve(method, originals)
            }
        }

        val writer = ClassWriter(0)
        node.accept(writer)
        output.writeBytes(writer.toByteArray())
    }

    private fun getClassEntries(stream: ZipInputStream) =
        sequence {
            while (true) {
                val entry = stream.nextEntry ?: break
                if (entry.name.endsWith(".class")) yield(entry.name)
            }
        }

    internal fun generateStub(
        classpaths: Iterable<List<ArtifactSpec>>,
        extraExcludes: List<String>,
        output: Path,
        preserveMethodBodies: Boolean = false,
    ): List<ArtifactSpec> {
        val exclude =
            listOf(
                "org.jetbrains",
                "org.apache",
                "org.codehaus",
                "org.ow2",
                "org.lwjgl",
                "com.google",
                "net.java",
                "ca.weblite",
                "com.ibm",
                "org.scala-lang",
                "org.clojure",
                "io.netty",
                "org.slf4j",
                "org.lz4",
                "org.joml",
                "net.sf",
                "it.unimi",
                "commons-",
                "com.github",
                "org.antlr",
                "org.openjdk",
                "net.minecrell",
                "org.jline",
                "net.jodah",
                "org.checkerframework",
                "org.spongepowered",
                "net.fabricmc:sponge",
            ) + extraExcludes

        val loaders = mutableListOf<ClasspathLoader>()
        try {
            for (artifacts in classpaths) {
                val (excluded, included) =
                    artifacts.partition {
                        val id = it.componentId ?: return@partition false
                        it.type == GenerateStubApi.ResolvedArtifact.Type.Module && exclude.any(id::startsWith)
                    }
                loaders += ClasspathLoader(included.map(ArtifactSpec::file), excluded)
            }

            val first = loaders.first()
            val rest = loaders.subList(1, loaders.size)
            val entries =
                first.intersectionIncluded.asSequence().flatMap { file ->
                    ZipInputStream(file.inputStream()).use { getClassEntries(it).toList() }.asSequence()
                }
            val filteredEntries = entries.filter { entry -> rest.all { it.loader.getResource(entry) != null } }

            output.deleteIfExists()
            FileSystems.newFileSystem(URI.create("jar:${output.toUri()}"), mapOf("create" to true.toString())).use { fileSystem ->
                val manifestPath = fileSystem.getPath(JarFile.MANIFEST_NAME)
                manifestPath.parent.createDirectory()
                manifestPath.outputStream().use(Manifest()::write)
                for (entry in filteredEntries) {
                    val target = fileSystem.getPath(entry)
                    if (loaders.size == 1) {
                        target.parent?.createDirectories()
                        first.resourceStream(entry)!!.use { stream ->
                            target.outputStream().use { stream.copyTo(it) }
                        }
                    } else {
                        val streams = loaders.map { it to it.entry(entry)!! }
                        createFileIntersection(streams, target, preserveMethodBodies)
                    }
                }
            }

            return loaders.map { it.intersectionExcluded }.reduce { a, b ->
                val artifactsA = a.groupBy { it.moduleId }
                val artifactsB = b.groupBy { it.moduleId }
                artifactsA.keys.intersect(artifactsB.keys).flatMap { id ->
                    val relevantA = artifactsA[id] ?: return@flatMap emptyList()
                    val relevantB = artifactsB[id] ?: return@flatMap emptyList()
                    listOf(relevantA, relevantB).minBy { it[0].moduleVersion ?: "" }
                }
            }
        } finally {
            loaders.asReversed().forEach { it.close() }
        }
    }

    internal class ClasspathLoader(
        val intersectionIncluded: List<File>,
        val intersectionExcluded: List<ArtifactSpec>,
    ) : AutoCloseable {
        val loader =
            URLClassLoader(
                (intersectionIncluded + intersectionExcluded.map(ArtifactSpec::file))
                    .map { it.toURI().toURL() }
                    .toTypedArray(),
            )

        // Each worker owns its stream; closing another loader must not close a shared cached JAR.
        fun resourceStream(name: String) =
            loader
                .getResource(name)
                ?.openConnection()
                ?.also { it.useCaches = false }
                ?.getInputStream()

        fun entry(name: String): ClassNode? =
            resourceStream(name)?.use { stream ->
                object : ClassNode(Opcodes.ASM9) {
                    override fun visitMethod(
                        access: Int,
                        name: String,
                        descriptor: String,
                        signature: String?,
                        exceptions: Array<out String>?,
                    ): CodeMethodNode = CodeMethodNode(access, name, descriptor, signature, exceptions).also(methods::add)
                }.also { node -> ClassReader(stream).accept(node, 0) }
            }

        data class Header(
            val superName: String?,
        )

        fun header(name: String): Header? =
            resourceStream(name)?.use { stream ->
                var superName: String? = null
                ClassReader(stream).accept(
                    object : ClassVisitor(Opcodes.ASM9) {
                        override fun visit(
                            version: Int,
                            access: Int,
                            name: String,
                            signature: String?,
                            superNameValue: String?,
                            interfaces: Array<out String>?,
                        ) {
                            superName = superNameValue
                        }
                    },
                    ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES,
                )
                Header(superName)
            }

        override fun close() = loader.close()
    }
}
