package net.msrandom.stubs

import net.msrandom.stubs.StubGenerator.ClasspathLoader
import net.msrandom.stubs.signature.SignatureIntersector
import net.msrandom.stubs.signature.parseClassSignature
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.MethodNode

object ClassNodeIntersector {
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
        if (signature == null) return null
        val parsed = runCatching { parseClassSignature(signature) }.getOrNull() ?: return null
        if (parsed.superClass.base.name != superName) return null
        if (!parsed.superInterfaces.all { it.base.name in interfaces }) return null
        return signature
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
                    SignatureIntersector.intersectFieldSignatures(present.map { it.signature }),
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
                    SignatureIntersector.intersectMethodSignatures(present.map { it.signature }),
                    present
                        .drop(1)
                        .fold(present.first().exceptions.toList()) { common, other ->
                            common.filter { it in other.exceptions }
                        }.toTypedArray(),
                ).also {
                    it.visibleAnnotations = intersectAnnotations(present.map { current -> current.visibleAnnotations })
                    it.invisibleAnnotations = intersectAnnotations(present.map { current -> current.invisibleAnnotations })
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
