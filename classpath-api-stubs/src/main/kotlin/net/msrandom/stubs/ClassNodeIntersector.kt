package net.msrandom.stubs

import net.msrandom.stubs.StubGenerator.ClasspathLoader
import net.msrandom.stubs.signature.SignatureIntersector
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.ParameterNode
import kotlin.math.min
import java.lang.reflect.Array as ReflectArray

object ClassNodeIntersector {
    private data class ParameterAnnotationLayout(
        val annotations: Array<MutableList<AnnotationNode>?>,
        val encodedCount: Int,
        val omittedPrefix: Int,
    )

    private fun annotationValueEquals(first: Any?, second: Any?): Boolean {
        if (first == null || second == null) return false
        if (first is AnnotationNode && second is AnnotationNode) {
            return first.desc == second.desc && annotationValuesEqual(first.values, second.values)
        }
        if (first.javaClass.isArray && second.javaClass.isArray) {
            if (first.javaClass != second.javaClass) return false
            val firstLength = ReflectArray.getLength(first)

            if (firstLength != ReflectArray.getLength(second)) return false

            return (0 until firstLength).all {
                annotationValueEquals(ReflectArray.get(first, it), ReflectArray.get(second, it))

            }
        }
        if (first is List<*> && second is List<*>) {
            return first.size == second.size && first.indices.all {
                annotationValueEquals(first[it], second[it])
            }
        }
        return when (first) {
            is Byte, is Boolean, is Char, is Short, is Int, is Long, is Float, is Double, is String, is Type -> first == second
            else -> false
        }
    }

    private fun annotationValuesEqual(first: List<Any?>?, second: List<Any?>?): Boolean {
        if (first == null || second == null) return first == second
        if (first.size % 2 != 0 || second.size % 2 != 0) return false
        if (first.indices.step(2).any { first[it] !is String || second.getOrNull(it) !is String }) return false
        return first.size == second.size && first.indices.all {
            annotationValueEquals(first[it], second[it])
        }
    }

    private fun annotationEquals(first: AnnotationNode, second: AnnotationNode): Boolean =
        first.desc == second.desc && annotationValuesEqual(first.values, second.values)

    private fun intersectAnnotationLists(
        first: List<AnnotationNode>,
        second: List<AnnotationNode>,
    ): List<AnnotationNode> {
        val remaining = second.toMutableList()
        return first.mapNotNull { annotation ->
            val index = remaining.indexOfFirst { annotationEquals(annotation, it) }
            if (index < 0) null else {
                remaining.removeAt(index)
                annotation
            }
        }
    }

    private fun parameterAnnotationLayout(
        method: MethodNode,
        annotations: Array<MutableList<AnnotationNode>?>?,
        annotableCount: Int,
    ): ParameterAnnotationLayout? {
        annotations ?: return null
        val parameterCount = Type.getArgumentTypes(method.desc).size
        if (annotations.size > parameterCount) return null
        if (annotations.size == parameterCount && (annotableCount < 0 || annotableCount == parameterCount)) {
            return ParameterAnnotationLayout(annotations, parameterCount, 0)
        }

        // ASM pads a short encoded table to descriptor size on read; trailing slots are not encoded.
        if (annotableCount !in 1 until parameterCount) return null
        if (annotations.size != annotableCount && annotations.size != parameterCount) return null
        if (annotations.drop(annotableCount).any { !it.isNullOrEmpty() }) return null
        val omittedPrefix = parameterCount - annotableCount
        val parameters = method.parameters ?: return null
        if (parameters.size != parameterCount) return null
        if ((0 until omittedPrefix).any { parameters[it] == null || parameters[it]!!.access and (Opcodes.ACC_SYNTHETIC or Opcodes.ACC_MANDATED) == 0 }) {
            return null
        }
        return ParameterAnnotationLayout(annotations.copyOf(annotableCount), annotableCount, omittedPrefix)
    }

    private fun intersectParameterAnnotations(
        methods: List<MethodNode>,
        visible: Boolean,
    ): Array<MutableList<AnnotationNode>?>? {
        val layouts = methods.map { method ->
            parameterAnnotationLayout(
                method,
                if (visible) method.visibleParameterAnnotations else method.invisibleParameterAnnotations,
                if (visible) method.visibleAnnotableParameterCount else method.invisibleAnnotableParameterCount,
            ) ?: return null
        }
        val first = layouts.first()
        if (layouts.any { it.omittedPrefix != first.omittedPrefix || it.encodedCount != first.encodedCount }) return null
        val result = Array(first.encodedCount) { index ->
            layouts.drop(1).fold(first.annotations[index]?.toList() ?: emptyList()) { common, layout ->
                intersectAnnotationLists(common, layout.annotations[index] ?: emptyList())
            }.toMutableList().takeIf { it.isNotEmpty() }
        }
        return result.takeIf { it.any { annotations -> !annotations.isNullOrEmpty() } }
    }

    private fun intersectMethodParameters(methods: List<MethodNode>): MutableList<ParameterNode>? {
        val parameterCount = Type.getArgumentTypes(methods.first().desc).size
        val parameters = methods.map { method ->
            if (Type.getArgumentTypes(method.desc).size != parameterCount) return null
            val parameters = method.parameters ?: return null
            if (parameters.size != parameterCount || parameters.any { it == null || it.name == null }) return null
            parameters
        }
        val first = parameters.first()
        if (parameters.drop(1).any { other -> first.zip(other).any { (a, b) -> a.name != b.name || a.access != b.access } }) return null
        return first.map { ParameterNode(it.name, it.access) }.toMutableList()
    }

    internal fun applyParameterMetadata(target: MethodNode, originals: List<MethodNode>) {
        require(originals.isNotEmpty() && originals.all { it.name == target.name && it.desc == target.desc })
        target.visibleParameterAnnotations = intersectParameterAnnotations(originals, true)
        target.invisibleParameterAnnotations = intersectParameterAnnotations(originals, false)
        target.visibleAnnotableParameterCount = target.visibleParameterAnnotations?.size ?: 0
        target.invisibleAnnotableParameterCount = target.invisibleParameterAnnotations?.size ?: 0
        target.parameters = intersectMethodParameters(originals)
    }

    private fun intersectAnnotations(
        annotationsA: List<AnnotationNode>?,
        annotationsB: List<AnnotationNode>?,
    ): List<AnnotationNode>? {
        if (annotationsA == null || annotationsB == null) {
            return null
        }

        val matchingAnnotations = annotationsA.filter { annotation ->
            annotationsB.any { it.desc == annotation.desc }
        }

        return matchingAnnotations
    }

    internal fun intersectClassNodes(
        nodeA: ClassNode,
        nodeB: ClassNode,
        classpathA: ClasspathLoader,
        classpathB: ClasspathLoader,
    ): ClassNode {
        val node = ClassNode()

        node.version = min(nodeA.version, nodeB.version)
        node.access = nodeA.access
        node.name = nodeA.name

        node.signature = SignatureIntersector.intersectClassSignatures(nodeA.signature, nodeB.signature)

        node.superName = CommonSuperClassFinder.findCommonSuper(nodeA, nodeB, classpathA, classpathB)
        node.interfaces = nodeA.interfaces.intersect(nodeB.interfaces.toSet()).toList()

        node.innerClasses =
            nodeA.innerClasses.mapNotNull { innerClass ->
                val inner = nodeB.innerClasses.firstOrNull {
                    it.name == innerClass.name
                }

                if (inner == null) {
                    null
                } else {
                    InnerClassNode(
                        innerClass.name,
                        innerClass.outerName,
                        innerClass.innerName,
                        AccessIntersector.intersect(inner.access, innerClass.access),
                    )
                }
            }

        node.outerClass = nodeA.outerClass
        node.outerMethod = nodeA.outerMethod
        node.outerMethodDesc = nodeA.outerMethodDesc

        node.visibleAnnotations = intersectAnnotations(nodeA.visibleAnnotations, nodeB.visibleAnnotations)
        node.invisibleAnnotations = intersectAnnotations(nodeA.invisibleAnnotations, nodeB.invisibleAnnotations)

        node.fields =
            nodeA.fields.mapNotNull { fieldA ->
                nodeB.fields.firstOrNull { it.name == fieldA.name && it.desc == fieldA.desc }?.let { fieldB ->
                    val access = AccessIntersector.intersect(fieldB.access, fieldA.access)

                    val signature = SignatureIntersector.intersectFieldSignature(fieldA.signature, fieldB.signature)

                    FieldNode(access, fieldB.name, fieldB.desc, signature, null).also {
                        it.visibleAnnotations =
                            intersectAnnotations(fieldA.visibleAnnotations, fieldB.visibleAnnotations)
                        it.invisibleAnnotations =
                            intersectAnnotations(fieldA.invisibleAnnotations, fieldB.invisibleAnnotations)
                    }
                }
            }

        node.methods =
            nodeA.methods.mapNotNull { methodA ->
                nodeB.methods.firstOrNull { it.name == methodA.name && it.desc == methodA.desc }?.let { methodB ->
                    val access = AccessIntersector.intersect(methodB.access, methodA.access)

                    val signature = SignatureIntersector.intersectMethodSignature(methodA.signature, methodB.signature)

                    val visibleAnnotations =
                        intersectAnnotations(methodA.visibleAnnotations, methodB.visibleAnnotations)
                    val invisibleAnnotations =
                        intersectAnnotations(methodA.invisibleAnnotations, methodB.invisibleAnnotations)

                    val method = MethodNode(
                        access,
                        methodB.name,
                        methodB.desc,
                        signature,
                        methodB.exceptions.toTypedArray().intersect(methodA.exceptions.toSet()).toTypedArray(),
                    )

                    method.visibleAnnotations = visibleAnnotations
                    method.invisibleAnnotations = invisibleAnnotations
                    applyParameterMetadata(method, listOf(methodA, methodB))

                    // TODO This is not fully correct
                    method.annotationDefault = methodA.annotationDefault

                    method
                }
            }

        return node
    }
}
