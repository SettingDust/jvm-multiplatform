package net.msrandom.stubs

import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InheritanceGenerationTest {
    @Test
    fun `matching generic headers survive and conflicting headers become raw`() {
        val loaders =
            listOf(
                StubGenerator.ClasspathLoader(emptyList(), emptyList()),
                StubGenerator.ClasspathLoader(emptyList(), emptyList()),
            )
        try {
            val first = node("<T:Ljava/lang/Object;>Ljava/lang/Object;", "TU;")
            val second = node("<U:Ljava/lang/Object;>Ljava/lang/Object;", "TV;")

            val matching =
                ClassNodeIntersector.intersectClassNodes(
                    listOf(first, second),
                    loaders,
                )
            assertEquals("<T:Ljava/lang/Object;>Ljava/lang/Object;", matching.signature)

            second.signature = "<U:Ljava/lang/Number;>Ljava/lang/Object;"
            val conflicting =
                ClassNodeIntersector.intersectClassNodes(
                    listOf(first, second),
                    loaders,
                )
            assertNull(conflicting.signature)

            val bytes = ClassWriter(0).also(conflicting::accept).toByteArray()
            val read = ClassNode().also { ClassReader(bytes).accept(it, 0) }
            assertNull(read.signature)
        } finally {
            loaders.forEach(StubGenerator.ClasspathLoader::close)
        }
    }

    private fun node(
        signature: String,
        methodSignature: String,
    ) = ClassNode().apply {
        version = Opcodes.V1_8
        access = Opcodes.ACC_PUBLIC
        name = "Fixture"
        superName = "java/lang/Object"
        this.signature = signature
        methods.add(
            org.objectweb.asm.tree.MethodNode(
                Opcodes.ACC_PUBLIC,
                "value",
                "()Ljava/lang/Object;",
                methodSignature,
                null,
            ),
        )
    }
}
