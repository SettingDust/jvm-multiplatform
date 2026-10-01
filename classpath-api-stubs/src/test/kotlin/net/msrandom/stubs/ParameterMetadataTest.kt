package net.msrandom.stubs

import org.objectweb.asm.Opcodes.ACC_MANDATED
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.ParameterNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class ParameterMetadataTest {
    private fun annotation(
        desc: String,
        value: Any,
    ): AnnotationNode = AnnotationNode(desc).apply { values = mutableListOf("value", value) }

    private fun nested(value: String): AnnotationNode = AnnotationNode("LNested;").apply { values = mutableListOf("value", value) }

    private fun method(
        descriptor: String = "(JDLjava/lang/String;)V",
        annotations: Array<MutableList<AnnotationNode>?>? = null,
        annotableCount: Int = -1,
        parameters: List<ParameterNode>? = null,
    ) = MethodNode(0, "m", descriptor, null, null).apply {
        visibleParameterAnnotations = annotations
        visibleAnnotableParameterCount = annotableCount
        this.parameters = parameters?.toMutableList()
    }

    private fun intersect(
        first: MethodNode,
        second: MethodNode,
    ): MethodNode {
        val firstClass =
            ClassNode().apply {
                name = "Example"
                superName = "java/lang/Object"
                methods = mutableListOf(first)
            }
        val secondClass =
            ClassNode().apply {
                name = "Example"
                superName = "java/lang/Object"
                methods = mutableListOf(second)
            }
        val firstLoader = StubGenerator.ClasspathLoader(emptyList(), emptyList())
        val secondLoader = StubGenerator.ClasspathLoader(emptyList(), emptyList())
        return try {
            ClassNodeIntersector.intersectClassNodes(firstClass, secondClass, firstLoader, secondLoader).methods.single()
        } finally {
            firstLoader.close()
            secondLoader.close()
        }
    }

    @Test
    fun annotationsUseDescriptorPositionsAndDeepDuplicateMatching() {
        val shared = annotation("LMarker;", nested("same"))
        val first =
            method(
                annotations =
                    arrayOf(
                        mutableListOf(annotation("LWrong;", "wide")),
                        null,
                        mutableListOf(shared, annotation("LMarker;", nested("different"))),
                    ),
            )
        val second =
            method(
                annotations =
                    arrayOf(
                        mutableListOf(annotation("LWrong;", "other")),
                        null,
                        mutableListOf(annotation("LMarker;", nested("conflict")), shared),
                    ),
            )

        val result = intersect(first, second)
        assertEquals(3, result.visibleParameterAnnotations!!.size)
        assertNull(result.visibleParameterAnnotations!![0])
        assertEquals(1, result.visibleParameterAnnotations!![2]!!.size)
        assertEquals("same", (result.visibleParameterAnnotations!![2]!![0].values!![1] as AnnotationNode).values!![1])
    }

    @Test
    fun shortTableIsOnlyAcceptedWithProvenSyntheticPrefix() {
        val annotation = annotation("LMarker;", "value")
        val parameters =
            listOf(
                ParameterNode("generated", ACC_SYNTHETIC or ACC_MANDATED),
                ParameterNode("value", 0),
            )
        val result =
            intersect(
                method("(Ljava/lang/Object;I)V", arrayOf(mutableListOf(annotation)), 1, parameters),
                method("(Ljava/lang/Object;I)V", arrayOf(mutableListOf(annotation("LMarker;", "value"))), 1, parameters),
            )
        assertNotNull(result.visibleParameterAnnotations)
        assertEquals(1, result.visibleParameterAnnotations!!.size)
        assertEquals(1, result.visibleAnnotableParameterCount)

        val ambiguous =
            intersect(
                method("(Ljava/lang/Object;I)V", arrayOf(mutableListOf(annotation)), 1),
                method("(Ljava/lang/Object;I)V", arrayOf(mutableListOf(annotation("LMarker;", "value"))), 1),
            )
        assertNull(ambiguous.visibleParameterAnnotations)
    }

    private fun permutations(methods: List<MethodNode>): List<List<MethodNode>> =
        if (methods.isEmpty()) listOf(emptyList()) else methods.indices.flatMap { index ->
            permutations(methods.filterIndexed { i, _ -> i != index }).map { listOf(methods[index]) + it }
        }

    @Test
    fun originalGroupsRetainMappingAfterNamesAreOmitted() {
        for (count in 2..3) for (visible in listOf(false, true)) {
            val originals = (0 until count).map { index ->
                method("(Ljava/lang/Object;JD)V", parameters = listOf(
                    ParameterNode("generated$index", ACC_SYNTHETIC), ParameterNode("wide", 0), ParameterNode("other", 0),
                )).apply {
                    val table = arrayOf<MutableList<AnnotationNode>?>(null, mutableListOf(annotation("LMarker;", nested("same"))))
                    if (visible) {
                        visibleParameterAnnotations = table
                        visibleAnnotableParameterCount = 2
                    } else {
                        invisibleParameterAnnotations = table
                        invisibleAnnotableParameterCount = 2
                    }
                }
            }
            for (order in permutations(originals)) {
                val result = method("(Ljava/lang/Object;JD)V")
                ClassNodeIntersector.applyParameterMetadata(result, order)
                assertNull(result.parameters)
                val table = if (visible) result.visibleParameterAnnotations else result.invisibleParameterAnnotations
                assertEquals(2, table!!.size)
                assertNull(table[0])
                assertEquals("LMarker;", table[1]!!.single().desc)
                assertEquals(2, if (visible) result.visibleAnnotableParameterCount else result.invisibleAnnotableParameterCount)
            }
        }
    }

    @Test
    fun malformedLayoutsAndParametersAreConservativelyOmitted() {
        val desc = "(Ljava/lang/Object;J)V"
        fun valid() = method(desc, arrayOf(mutableListOf(annotation("LMarker;", "same"))), 1,
            listOf(ParameterNode("prefix", ACC_MANDATED), ParameterNode("value", 0)))
        val mutations: List<(MethodNode) -> Unit> = listOf(
            { it.parameters = null },
            { it.parameters = mutableListOf(ParameterNode("short", ACC_SYNTHETIC)) },
            { it.parameters.add(ParameterNode("extra", 0)) },
            { @Suppress("UNCHECKED_CAST") val nodes = mutableListOf<ParameterNode?>(null, ParameterNode("value", 0)); it.parameters = nodes as MutableList<ParameterNode> },
            { it.parameters[0].access = 0 },
            { it.visibleAnnotableParameterCount = 0 },
            { it.visibleAnnotableParameterCount = 3 },
            { it.visibleParameterAnnotations = arrayOf(null, null, mutableListOf(annotation("LMarker;", "same"))) },
            { it.visibleParameterAnnotations = arrayOf(null, mutableListOf(annotation("LMarker;", "same"))) },
            { it.visibleParameterAnnotations = null },
        )
        for ((index, mutate) in mutations.withIndex()) for (count in 2..3) {
            val bad = valid().also(mutate)
            val inputs = listOf(bad) + List(count - 1) { valid() }
            for (order in permutations(inputs)) {
                val target = valid()
                ClassNodeIntersector.applyParameterMetadata(target, order)
                assertNull(target.visibleParameterAnnotations, "mutation $index")
                assertEquals(0, target.visibleAnnotableParameterCount)
            }
        }
        for (count in 2..3) for (order in permutations(listOf(valid().apply { parameters[1].name = null }) + List(count - 1) { valid() })) {
            val target = valid()
            ClassNodeIntersector.applyParameterMetadata(target, order)
            assertNull(target.parameters)
            assertNotNull(target.visibleParameterAnnotations)
        }
        assertFailsWith<IllegalArgumentException> {
            ClassNodeIntersector.applyParameterMetadata(valid(), listOf(valid(), method("(J)V")))
        }
    }

    @Test
    fun recursiveValuesAndIncompleteValuesDoNotCreateFalseConsensus() {
        val cases = listOf<Pair<Any, Any>>(
            nested("same") to nested("other"),
            arrayOf("LEnum;", "A") to arrayOf("LEnum;", "B"),
            intArrayOf(1, 2) to intArrayOf(1, 3),
            listOf(nested("same"), "tail") to listOf(nested("same"), "other"),
            1 to 1L,
        )
        for ((a, b) in cases) for (count in 2..3) {
            fun annotated(value: Any) = method("(I)V", arrayOf(mutableListOf(annotation("LMarker;", value))), 1)
            for (order in permutations(List(count - 1) { annotated(a) } + annotated(b))) {
                val target = annotated(a)
                ClassNodeIntersector.applyParameterMetadata(target, order)
                assertNull(target.visibleParameterAnnotations)
            }
        }
        val incomplete = annotation("LMarker;", "same").apply { values = mutableListOf("missing") }
        val a = method("(I)V", arrayOf(mutableListOf(incomplete)), 1)
        assertNull(intersect(a, a).visibleParameterAnnotations)
    }

    @Test
    fun namesAndFlagsRequireAllInputConsensus() {
        val first = listOf(ParameterNode("value", 0), ParameterNode("wide", ACC_SYNTHETIC))
        val second = listOf(ParameterNode("value", 0), ParameterNode("wide", ACC_SYNTHETIC))
        assertNotNull(intersect(method("(IJ)V", parameters = first), method("(IJ)V", parameters = second)).parameters)

        val conflicting = listOf(ParameterNode("other", 0), ParameterNode("wide", ACC_SYNTHETIC))
        assertNull(intersect(method("(IJ)V", parameters = first), method("(IJ)V", parameters = conflicting)).parameters)
    }
}
