package net.msrandom.stubs

import org.junit.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.TypeReference
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeAnnotationNode
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ClassNodeIntersectorTest {

    private fun createEmptyJar(): File {
        val jar = Files.createTempFile("stub-intersector-test-", ".jar").toFile()
        ZipOutputStream(jar.outputStream()).use { }
        return jar
    }

    private fun createClassNode(method: MethodNode): ClassNode {
        return ClassNode().apply {
            version = Opcodes.V1_8
            access = Opcodes.ACC_PUBLIC
            name = "example/Test"
            superName = "java/lang/Object"
            methods = mutableListOf(method)
        }
    }

    @Test
    fun `intersection keeps shared method declaration type and parameter annotations`() {
        val methodA = MethodNode(Opcodes.ACC_PUBLIC, "foo", "(Ljava/lang/String;)Ljava/lang/String;", null, null)
        val methodB = MethodNode(Opcodes.ACC_PUBLIC, "foo", "(Ljava/lang/String;)Ljava/lang/String;", null, null)

        methodA.visibleAnnotations = mutableListOf(
            AnnotationNode("Lann/Visible;"),
            AnnotationNode("Lann/OnlyInA;"),
        )
        methodB.visibleAnnotations = mutableListOf(
            AnnotationNode("Lann/Visible;"),
            AnnotationNode("Lann/OnlyInB;"),
        )

        methodA.invisibleAnnotations = mutableListOf(AnnotationNode("Lorg/jetbrains/annotations/Nullable;"))
        methodB.invisibleAnnotations = mutableListOf(AnnotationNode("Lorg/jetbrains/annotations/Nullable;"))

        val returnRef = TypeReference.newTypeReference(TypeReference.METHOD_RETURN).value
        val paramTypeRef = TypeReference.newFormalParameterReference(0).value

        methodA.visibleTypeAnnotations = mutableListOf(
            TypeAnnotationNode(returnRef, null, "Lann/ReturnType;"),
            TypeAnnotationNode(paramTypeRef, null, "Lann/ParamType;"),
            TypeAnnotationNode(returnRef, null, "Lann/OnlyTypeInA;"),
        )
        methodB.visibleTypeAnnotations = mutableListOf(
            TypeAnnotationNode(returnRef, null, "Lann/ReturnType;"),
            TypeAnnotationNode(paramTypeRef, null, "Lann/ParamType;"),
        )

        methodA.visibleParameterAnnotations = arrayOf(
            mutableListOf(
                AnnotationNode("Lann/ParamVisible;"),
                AnnotationNode("Lann/ParamOnlyInA;"),
            )
        )
        methodB.visibleParameterAnnotations = arrayOf(
            mutableListOf(AnnotationNode("Lann/ParamVisible;"))
        )

        methodA.invisibleParameterAnnotations = arrayOf(
            mutableListOf(AnnotationNode("Lorg/jetbrains/annotations/Nullable;"))
        )
        methodB.invisibleParameterAnnotations = arrayOf(
            mutableListOf(AnnotationNode("Lorg/jetbrains/annotations/Nullable;"))
        )

        val classA = createClassNode(methodA)
        val classB = createClassNode(methodB)

        val jarA = createEmptyJar()
        val jarB = createEmptyJar()

        StubGenerator.ClasspathLoader(listOf(jarA), emptyList()).use { cpA ->
            StubGenerator.ClasspathLoader(listOf(jarB), emptyList()).use { cpB ->
                val result = ClassNodeIntersector.intersectClassNodes(classA, classB, cpA, cpB, false)
                val resultMethod = result.methods.single()

                assertEquals(listOf("Lann/Visible;"), resultMethod.visibleAnnotations?.map { it.desc })
                assertEquals(
                    listOf("Lorg/jetbrains/annotations/Nullable;"),
                    resultMethod.invisibleAnnotations?.map { it.desc },
                )

                assertEquals(
                    setOf("Lann/ReturnType;", "Lann/ParamType;"),
                    resultMethod.visibleTypeAnnotations?.map { it.desc }?.toSet(),
                )

                val visibleParameterAnnotations = assertNotNull(resultMethod.visibleParameterAnnotations)
                assertEquals(listOf("Lann/ParamVisible;"), visibleParameterAnnotations[0]?.map { it.desc })

                val invisibleParameterAnnotations = assertNotNull(resultMethod.invisibleParameterAnnotations)
                assertEquals(
                    listOf("Lorg/jetbrains/annotations/Nullable;"),
                    invisibleParameterAnnotations[0]?.map { it.desc },
                )
            }
        }
    }
}
