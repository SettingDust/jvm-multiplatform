package net.msrandom.stubs

import net.msrandom.stubs.StubGenerator.ClasspathLoader
import net.msrandom.stubs.signature.SignatureIntersector
import net.msrandom.stubs.signature.parseClassSignature
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.ParameterNode
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

    private fun intersectAnnotations(annotations: List<List<AnnotationNode>?>): List<AnnotationNode>? {
        if (annotations.any { it == null }) return null
        return annotations.first()!!.filter { annotation ->
            annotations.drop(1).all { other -> other!!.any { it.desc == annotation.desc } }
        }
    }

    private fun validClassSignature(
        signature: String?,
        superName: String,
        interfaces: List<String>,
    ): String? {
        return SignatureIntersector.validClass(signature, superName, interfaces)
    }

    internal fun intersectClassNodes(
        nodes: List<ClassNode>,
        classpaths: List<ClasspathLoader>,
    ): ClassNode {
        require(nodes.size == classpaths.size && nodes.isNotEmpty())
        val first = nodes.first()
        val node = ClassNode()
        node.version = nodes.minOf { it.version }
        node.access = first.access
        node.name = first.name
        node.superName = CommonSuperClassFinder.findCommonSuper(nodes, classpaths)
        node.interfaces =
            nodes
                .drop(1)
                .fold(first.interfaces.toSet()) { common, current ->
                    common intersect current.interfaces.toSet()
                }.toList()
        node.signature =
            validClassSignature(
                SignatureIntersector.intersectClassSignatures(nodes.map { it.signature }),
                node.superName!!,
                node.interfaces,
            )

        val memberClassSignature = node.signature.takeIf {
            nodes.map { runCatching { parseClassSignature(it.signature).typeParameters }.getOrNull() }.distinct().size == 1
        }

        node.innerClasses =
            first.innerClasses.mapNotNull { innerClass ->
                val matching = nodes.drop(1).mapNotNull { other -> other.innerClasses.firstOrNull { it.name == innerClass.name } }
                if (matching.size != nodes.size - 1) {
                    null
                } else {
                    InnerClassNode(
                        innerClass.name,
                        innerClass.outerName,
                        innerClass.innerName,
                        matching.fold(innerClass.access) { access, other -> AccessIntersector.intersect(access, other.access) },
                    )
                }
            }
        node.outerClass = first.outerClass
        node.outerMethod = first.outerMethod
        node.outerMethodDesc = first.outerMethodDesc
        node.visibleAnnotations = intersectAnnotations(nodes.map { it.visibleAnnotations })
        node.invisibleAnnotations = intersectAnnotations(nodes.map { it.invisibleAnnotations })

        node.fields =
            first.fields.mapNotNull { field ->
                val fields = nodes.map { current -> current.fields.firstOrNull { it.name == field.name && it.desc == field.desc } }
                if (fields.any { it == null }) return@mapNotNull null
                val present = fields.map { it!! }
                FieldNode(
                    present.fold(field.access) { access, other -> AccessIntersector.intersect(access, other.access) },
                    field.name,
                    field.desc,
                    SignatureIntersector.validMember(
                        SignatureIntersector.intersectFieldSignatures(present.mapIndexed { index, member ->
                            SignatureIntersector.validMember(member.signature, member.desc, nodes[index].signature, false)
                        }),
                        field.desc,
                        memberClassSignature,
                        false,
                    ),
                    null,
                ).also {
                    it.visibleAnnotations = intersectAnnotations(present.map { field -> field.visibleAnnotations })
                    it.invisibleAnnotations = intersectAnnotations(present.map { field -> field.invisibleAnnotations })
                }
            }

        node.methods =
            first.methods.mapNotNull { method ->
                val methods = nodes.map { current -> current.methods.firstOrNull { it.name == method.name && it.desc == method.desc } }
                if (methods.any { it == null }) return@mapNotNull null
                val present = methods.map { it!! }
                MethodNode(
                    present.fold(method.access) { access, other -> AccessIntersector.intersect(access, other.access) },
                    method.name,
                    method.desc,
                    SignatureIntersector.validMember(
                        SignatureIntersector.intersectMethodSignatures(present.mapIndexed { index, member ->
                            SignatureIntersector.validMember(member.signature, member.desc, nodes[index].signature, true)
                        }),
                        method.desc,
                        memberClassSignature,
                        true,
                    ),
                    present
                        .drop(1)
                        .fold(present.first().exceptions.toList()) { common, other ->
                            common.filter { it in other.exceptions }
                        }.toTypedArray(),
                ).also {
                    it.visibleAnnotations = intersectAnnotations(present.map { current -> current.visibleAnnotations })
                    it.invisibleAnnotations = intersectAnnotations(present.map { current -> current.invisibleAnnotations })
                    applyParameterMetadata(it, present)
                    it.annotationDefault = method.annotationDefault
                }
            }
        return node
    }

    internal fun intersectClassNodes(
        nodeA: ClassNode,
        nodeB: ClassNode,
        classpathA: ClasspathLoader,
        classpathB: ClasspathLoader,
    ): ClassNode = intersectClassNodes(listOf(nodeA, nodeB), listOf(classpathA, classpathB))
}
