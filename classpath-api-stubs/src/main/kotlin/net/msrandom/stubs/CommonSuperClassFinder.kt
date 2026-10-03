package net.msrandom.stubs

import net.msrandom.stubs.StubGenerator.ClasspathLoader
import org.objectweb.asm.tree.ClassNode

internal object CommonSuperClassFinder {
    internal fun findCommonSuper(
        nodes: List<ClassNode>,
        classpaths: List<ClasspathLoader>,
    ): String {
        require(nodes.size == classpaths.size && nodes.isNotEmpty())

        val chains = nodes.indices.map { index ->
            val chain = linkedSetOf<String>()
            var name = nodes[index].superName
            while (name != null) {
                if (name == "java/lang/Object") {
                    chain.add(name)
                    break
                }
                val parent = classpaths[index].header("$name.class")
                if (parent == null) {
                    chain.clear()
                    break
                }
                if (!chain.add(name)) {
                    error("Inheritance cycle while resolving $name")
                }
                name = parent.superName
            }
            chain.add("java/lang/Object")
            chain
        }

        return chains.first().first { candidate -> chains.all { candidate in it } }
    }

    internal fun findCommonSuper(
        nodeA: ClassNode,
        nodeB: ClassNode,
        classpathA: ClasspathLoader,
        classpathB: ClasspathLoader,
    ): String = findCommonSuper(listOf(nodeA, nodeB), listOf(classpathA, classpathB))
}
