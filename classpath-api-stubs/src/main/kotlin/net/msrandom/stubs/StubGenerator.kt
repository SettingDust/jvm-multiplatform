package net.msrandom.stubs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.msrandom.stubs.ClassNodeIntersector.intersectClassNodes
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import java.util.jar.Manifest
import kotlin.io.path.*

object StubGenerator {

    private fun createFileIntersection(
        streams: List<Pair<ClasspathLoader, ClassNode>>,
        output: Path,
        preserveMethodBodies: Boolean,
    ) {
        output.parent?.createDirectories()

        val (_, node) = streams.reduce { (classpathA, nodeA), (classpathB, nodeB) ->
            classpathB to intersectClassNodes(nodeA, nodeB, classpathA, classpathB, preserveMethodBodies)
        }

        val writer = ClassWriter(0)

        try {
            node.accept(writer)
        } catch (exception: IllegalArgumentException) {
            logIntersectionFailure(streams, node, output, preserveMethodBodies, exception)
            throw exception
        }

        output.writeBytes(writer.toByteArray())
    }

    private fun logIntersectionFailure(
        streams: List<Pair<ClasspathLoader, ClassNode>>,
        intersection: ClassNode,
        output: Path,
        preserveMethodBodies: Boolean,
        exception: IllegalArgumentException,
    ) {
        System.err.println("[classpath-api-stubs] Failed to emit intersection for ${intersection.name} -> $output")
        System.err.println("[classpath-api-stubs] preserveMethodBodies=$preserveMethodBodies, mergedVersion=${describeVersion(intersection.version)}, message=${exception.message}")

        intersection.methods
            .filter { it.hasFrames() }
            .forEach { method ->
                System.err.println("[classpath-api-stubs] mergedMethod ${method.debugSummary()}")
            }

        streams.forEachIndexed { index, (classpath, node) ->
            val source = classpath.entrySource("${node.name}.class")
            System.err.println(
                "[classpath-api-stubs] source[$index] class=${node.name}, version=${describeVersion(node.version)}, from=${source?.absolutePath ?: "<missing>"}"
            )

            node.methods
                .filter { it.hasFrames() }
                .forEach { method ->
                    System.err.println("[classpath-api-stubs] source[$index] method ${method.debugSummary()}")
                }
        }
    }

    private fun describeVersion(version: Int): String {
        val javaVersion = version - 44

        return "$version(Java $javaVersion)"
    }

    private fun MethodNode.hasFrames(): Boolean =
        instructions.iterator().asSequence().any { it is FrameNode }

    private fun MethodNode.debugSummary(): String {
        val instructionCount = instructions.iterator().asSequence().count()
        val frameCount = instructions.iterator().asSequence().count { it is FrameNode }

        return "$name$desc frames=$frameCount instructions=$instructionCount maxStack=$maxStack maxLocals=$maxLocals"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun generateStub(
        classpaths: Iterable<List<GenerateStubApi.ResolvedArtifact>>,
        extraExcludes: List<String>,
        output: Path,
        preserveMethodBodies: Boolean,
    ): List<GenerateStubApi.ResolvedArtifact> {
        val exclude = listOf(
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

        val classpaths = classpaths.map { artifacts ->
            val (excluded, included) = artifacts.partition {
                val type = it.type.orNull
                val id = it.componentId.orNull ?: return@partition false

                type === GenerateStubApi.ResolvedArtifact.Type.Module && exclude.any(id::startsWith)
            }

            ClasspathLoader(
                included.map { it.file.asFile.get() },
                excluded,
            )
        }

        try {
            val first = classpaths.first()
            val rest = classpaths.subList(1, classpaths.size)

            val entries = first.classEntries()

            val filteredEntries = entries.filter { entry ->
                rest.all {
                    it.hasEntry(entry)
                }
            }

            output.deleteIfExists()
            FileSystems.newFileSystem(URI.create("jar:${output.toUri()}"), mapOf("create" to true.toString()))
                .use { fileSystem ->
                    val manifestPath = fileSystem.getPath(JarFile.MANIFEST_NAME)

                    manifestPath.parent.createDirectory()

                    manifestPath.outputStream().use(Manifest()::write)

                    runBlocking(Dispatchers.IO.limitedParallelism(16)) {
                        filteredEntries.map { entry ->
                            launch {
                                val streams = classpaths.map {
                                    it to it.entry(entry)!!
                                }

                                val path = fileSystem.getPath(entry)

                                synchronized(fileSystem) {
                                    path.parent?.createDirectories()
                                }

                                createFileIntersection(streams, path, preserveMethodBodies)

                                // Release bytecodeCache entry for this class (super-class entries remain cached)
                                for (classpath in classpaths) {
                                    classpath.evictEntry(entry)
                                }
                            }
                        }.forEach { it.join() }
                    }
                }

            return classpaths.map { it.intersectionExcluded }.reduce { a, b ->
                val artifactsA = a.groupBy { it.moduleId.get() }

                val artifactsB = b.groupBy { it.moduleId.get() }

                val intersections = artifactsA.keys.intersect(artifactsB.keys)

                intersections.flatMap { id ->
                    val relevantArtifactsA = artifactsA[id] ?: return@flatMap emptyList()
                    val relevantArtifactsB = artifactsB[id] ?: return@flatMap emptyList()

                    listOf(relevantArtifactsA, relevantArtifactsB).minBy {
                        it[0].moduleVersion.get()
                    }
                }
            }
        } finally {
            for (classpath in classpaths) {
                classpath.close()
            }
        }
    }

    internal class ClasspathLoader(
        val intersectionIncluded: List<File>,
        val intersectionExcluded: List<GenerateStubApi.ResolvedArtifact>,
        // Cache bytecode to avoid repeated JAR reads
        private val bytecodeCache: ConcurrentHashMap<String, ByteArray?> = ConcurrentHashMap(),
    ) : AutoCloseable {
        private val allFiles = intersectionIncluded + intersectionExcluded.map { it.file.asFile.get() }

        // Use JarFile for thread-safe access
        private val jarFiles = allFiles.map { JarFile(it) }

        fun classEntries(): Sequence<String> = sequence {
            for (jar in jarFiles) {
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.name.endsWith(".class")) {
                        yield(entry.name)
                    }
                }
            }
        }

        fun hasEntry(name: String): Boolean {
            return jarFiles.any { it.getEntry(name) != null }
        }

        fun evictEntry(name: String) {
            bytecodeCache.remove(name)
        }

        fun superClassOf(name: String): String? {
            val bytecode = bytecodeCache.computeIfAbsent(name) {
                for (jar in jarFiles) {
                    val entry = jar.getEntry(name) ?: continue
                    return@computeIfAbsent jar.getInputStream(entry).use { it.readBytes() }
                }
                null
            } ?: return null

            return ClassReader(bytecode).superName
        }

        fun entry(name: String): ClassNode? {
            val bytecode = bytecodeCache.computeIfAbsent(name) {
                for (jar in jarFiles) {
                    val entry = jar.getEntry(name) ?: continue
                    return@computeIfAbsent jar.getInputStream(entry).use { it.readBytes() }
                }
                null
            } ?: return null

            // Parse fresh ClassNode from bytecode to avoid shared mutable state
            return ClassNode().apply {
                ClassReader(bytecode).accept(this, ClassReader.EXPAND_FRAMES)
            }
        }

        fun entrySource(name: String): File? {
            return jarFiles.indices.firstNotNullOfOrNull { index ->
                allFiles[index].takeIf { jarFiles[index].getEntry(name) != null }
            }
        }

        override fun close() {
            jarFiles.forEach { it.close() }
        }
    }
}