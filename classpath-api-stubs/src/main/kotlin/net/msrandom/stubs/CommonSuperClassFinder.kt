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
                val superA = classpathA.entry("$superNameA.class")

                if (superA != null) {
                    superNameA = superA.superName

                    if (superNameA in visitedSuperNamesB) {
                        return superNameA
                    }

                    visitedSuperNamesA.add(superNameA)

                    if (superNameA == null) {
                        chainAExhausted = true
                    }
                } else {
                    // Parent class not resolvable in this version's classpath.
                    // Do NOT return superNameA here — it may not exist in the other version.
                    // Mark the chain as exhausted and let the other side continue.
                    chainAExhausted = true
                }
            }

            if (!chainBExhausted) {
                val superB = classpathB.entry("$superNameB.class")

                if (superB != null) {
                    superNameB = superB.superName

                    if (superNameB in visitedSuperNamesA) {
                        return superNameB
                    }

                    visitedSuperNamesB.add(superNameB)

                    if (superNameB == null) {
                        chainBExhausted = true
                    }
                } else {
                    chainBExhausted = true
                }
            }
        }
    }
}
