package net.msrandom.stubs

import net.msrandom.stubs.StubGenerator.ClasspathLoader
import org.objectweb.asm.tree.ClassNode

internal object CommonSuperClassFinder {
    internal fun findCommonSuper(
        nodeA: ClassNode,
        nodeB: ClassNode,
        classpathA: ClasspathLoader,
        classpathB: ClasspathLoader,
    ): String? {
        var superNameA = nodeA.superName
        var superNameB = nodeB.superName

        val visitedSuperNamesA = mutableListOf<String?>(superNameA)
        val visitedSuperNamesB = hashSetOf<String?>(superNameB)

        // A chain is exhausted when its superName is null (reached java/lang/Object)
        // or when the classpath entry for the current superName cannot be resolved.
        var chainAExhausted = superNameA == null
        var chainBExhausted = superNameB == null

        while (true) {
            for (name in visitedSuperNamesA) {
                if (name in visitedSuperNamesB) {
                    return name
                }
            }

            if (chainAExhausted && chainBExhausted) {
                // Both chains fully explored without finding a common named ancestor.
                // Return java/lang/Object as the universal safe fallback so the generated
                // stub never references a superclass that only exists in one version's classpath.
                return "java/lang/Object"
            }

            if (!chainAExhausted) {
                // Use lightweight ClassReader to resolve only the super-class name,
                // avoiding full ClassNode allocation for ancestor traversal.
                val resolvedSuperA = classpathA.superClassOf("$superNameA.class")

                if (resolvedSuperA == null) {
                    // Could not resolve the class OR reached java/lang/Object (superName is null).
                    // Both cases mean this chain can no longer advance.
                    chainAExhausted = true
                } else {
                    superNameA = resolvedSuperA

                    if (superNameA in visitedSuperNamesB) {
                        return superNameA
                    }

                    visitedSuperNamesA.add(superNameA)
                }
            }

            if (!chainBExhausted) {
                val resolvedSuperB = classpathB.superClassOf("$superNameB.class")

                if (resolvedSuperB == null) {
                    chainBExhausted = true
                } else {
                    superNameB = resolvedSuperB

                    if (superNameB in visitedSuperNamesA) {
                        return superNameB
                    }

                    visitedSuperNamesB.add(superNameB)
                }
            }
        }
    }
}
