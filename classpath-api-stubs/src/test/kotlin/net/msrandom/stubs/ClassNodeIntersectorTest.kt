package net.msrandom.stubs

import org.junit.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.TypeReference
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeAnnotationNode
import java.io.File
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClassNodeIntersectorTest {

    private fun createEmptyJar(): File {
        val jar = Files.createTempFile("stub-intersector-test-", ".jar").toFile()
        ZipOutputStream(jar.outputStream()).use { }
        return jar
    }

    private fun createJar(vararg entries: Pair<String, ByteArray>): File {
        val jar = Files.createTempFile("stub-intersector-test-", ".jar").toFile()

        ZipOutputStream(jar.outputStream()).use { output ->
            for ((name, bytes) in entries) {
                output.putNextEntry(JarEntry(name))
                output.write(bytes)
                output.closeEntry()
            }
        }

        return jar
    }

    private fun classBytes(version: Int, compressedFrame: Boolean): ByteArray {
        val writer = ClassWriter(0)

        writer.visit(version, Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER, "example/Test", null, "java/lang/Object", null)

        writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).apply {
            visitCode()
            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            visitInsn(Opcodes.RETURN)
            visitMaxs(1, 1)
            visitEnd()
        }

        writer.visitMethod(Opcodes.ACC_STATIC, "shared", "(Z)I", null, null).apply {
            visitCode()

            val elseLabel = org.objectweb.asm.Label()
            val endLabel = org.objectweb.asm.Label()

            visitVarInsn(Opcodes.ILOAD, 0)
            visitJumpInsn(Opcodes.IFEQ, elseLabel)
            visitInsn(Opcodes.ICONST_1)
            visitJumpInsn(Opcodes.GOTO, endLabel)
            visitLabel(elseLabel)

            if (compressedFrame) {
                visitFrame(Opcodes.F_SAME, 0, null, 0, null)
            } else {
                visitFrame(Opcodes.F_NEW, 1, arrayOf(Opcodes.INTEGER), 0, null)
            }

            visitInsn(Opcodes.ICONST_0)
            visitLabel(endLabel)

            if (compressedFrame) {
                visitFrame(Opcodes.F_SAME1, 0, null, 1, arrayOf(Opcodes.INTEGER))
            } else {
                visitFrame(Opcodes.F_NEW, 1, arrayOf(Opcodes.INTEGER), 1, arrayOf(Opcodes.INTEGER))
            }

            visitInsn(Opcodes.IRETURN)
            visitMaxs(1, 1)
            visitEnd()
        }

        writer.visitEnd()

        return writer.toByteArray()
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

    @Test
    fun `preserved method bodies from old class versions currently fail ASM emission`() {
        val methodA = MethodNode(Opcodes.ACC_PUBLIC, "foo", "()V", null, null).apply {
            val start = LabelNode()
            instructions.add(start)
            instructions.add(FrameNode(Opcodes.F_SAME, 0, null, 0, null))
            instructions.add(InsnNode(Opcodes.RETURN))
            maxStack = 0
            maxLocals = 1
        }
        val methodB = MethodNode(Opcodes.ACC_PUBLIC, "foo", "()V", null, null).apply {
            val start = LabelNode()
            instructions.add(start)
            instructions.add(FrameNode(Opcodes.F_SAME, 0, null, 0, null))
            instructions.add(InsnNode(Opcodes.RETURN))
            maxStack = 0
            maxLocals = 1
        }

        val classA = createClassNode(methodA).apply {
            version = Opcodes.V1_5
        }
        val classB = createClassNode(methodB).apply {
            version = Opcodes.V1_8
        }

        val jarA = createEmptyJar()
        val jarB = createEmptyJar()

        StubGenerator.ClasspathLoader(listOf(jarA), emptyList()).use { cpA ->
            StubGenerator.ClasspathLoader(listOf(jarB), emptyList()).use { cpB ->
                val result = ClassNodeIntersector.intersectClassNodes(classA, classB, cpA, cpB, true)

                val error = assertFailsWith<IllegalArgumentException> {
                    result.accept(ClassWriter(0))
                }

                assertEquals("Class versions V1_5 or less must use F_NEW frames.", error.message)
            }
        }
    }

    @Test
    fun `classpath loader expands frames so preserved method bodies can be emitted for legacy merged version`() {
        val jarA = createJar("example/Test.class" to classBytes(Opcodes.V1_5, compressedFrame = false))
        val jarB = createJar("example/Test.class" to classBytes(Opcodes.V1_6, compressedFrame = true))

        StubGenerator.ClasspathLoader(listOf(jarA), emptyList()).use { cpA ->
            StubGenerator.ClasspathLoader(listOf(jarB), emptyList()).use { cpB ->
                val classA = cpA.entry("example/Test.class")!!
                val classB = cpB.entry("example/Test.class")!!

                val result = ClassNodeIntersector.intersectClassNodes(classA, classB, cpA, cpB, true)
                val resultMethod = result.methods.single { it.name == "shared" }

                assertEquals(Opcodes.V1_5, result.version)
                assertTrue(resultMethod.instructions.iterator().asSequence().any { it is FrameNode })

                result.accept(ClassWriter(0))
            }
        }
    }
}
