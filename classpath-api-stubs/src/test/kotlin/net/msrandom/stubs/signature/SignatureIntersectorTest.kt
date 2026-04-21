package net.msrandom.stubs.signature

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for [SignatureIntersector.intersectClassSignatures].
 *
 * Key regression: when two class signatures declare the same interfaces but in different
 * ORDER (common across MC versions when one version adds a new interface at an arbitrary
 * position), the previous positional-zip implementation paired unrelated interfaces and
 * returned null for every pair — resulting in an empty superInterfaces list and a Signature
 * attribute that hid all implemented interfaces from the compiler.
 *
 * Example (Registry regression):
 *   MC 1.20.1 / 1.21.1 : interface Registry<T> extends Keyable, IdMap<T>
 *   MC 26.1.2           : interface Registry<T> extends IdMap<T>, Keyable, HolderLookup$RegistryLookup<T>
 *   Expected stub       : interface Registry<T> extends Keyable, IdMap<T>   (common subset, any order)
 *   Old (buggy) stub    : interface Registry<T>                              (no interfaces in Signature)
 */
class SignatureIntersectorTest {

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun classSignature(
        typeParam: String,
        vararg interfaces: String,
    ): String {
        // Produces e.g. "<T:Ljava/lang/Object;>Ljava/lang/Object;Lcom/example/Foo;Lcom/example/Bar;"
        val tp = "<$typeParam:Ljava/lang/Object;>"
        val ifaces = interfaces.joinToString("") { "L$it;" }
        return "${tp}Ljava/lang/Object;$ifaces"
    }

    private fun classSignatureNoTypeParam(vararg interfaces: String): String {
        val ifaces = interfaces.joinToString("") { "L$it;" }
        return "Ljava/lang/Object;$ifaces"
    }

    /**
     * Assert that the result signature contains all [expectedInterfaces] as superinterface
     * entries and does NOT contain [unexpectedInterfaces].
     */
    private fun assertInterfaceSignatures(
        result: String,
        expectedInterfaces: List<String>,
        unexpectedInterfaces: List<String> = emptyList(),
    ) {
        val parsed = parseClassSignature(result)
        val resultNames = parsed.superInterfaces.map { it.base.name }

        for (expected in expectedInterfaces) {
            assert(expected in resultNames) {
                "Expected interface '$expected' in result signature interfaces $resultNames"
            }
        }
        for (unexpected in unexpectedInterfaces) {
            assert(unexpected !in resultNames) {
                "Unexpected interface '$unexpected' should NOT be in result signature interfaces $resultNames"
            }
        }
    }

    // ── tests ──────────────────────────────────────────────────────────────────

    /**
     * Regression: Registry in MC 1.20.1 declares interfaces as [Keyable, IdMap]
     * while 26.1.2 declares them as [IdMap, Keyable, HolderLookup$RegistryLookup].
     * The intersection should preserve Keyable and IdMap regardless of order.
     */
    @Test
    fun `interface order difference does not produce empty intersection - Registry scenario`() {
        val sig1201 = classSignature(
            "T",
            "com/mojang/serialization/Keyable",
            "net/minecraft/core/IdMap",
        )
        val sig261 = classSignature(
            "T",
            "net/minecraft/core/IdMap",
            "com/mojang/serialization/Keyable",
            "net/minecraft/core/HolderLookup\$RegistryLookup",
        )

        val result = SignatureIntersector.intersectClassSignatures(sig1201, sig261)

        assertNotNull(result, "Intersection of compatible signatures must not be null")
        assertInterfaceSignatures(
            result,
            expectedInterfaces = listOf(
                "com/mojang/serialization/Keyable",
                "net/minecraft/core/IdMap",
            ),
            unexpectedInterfaces = listOf(
                "net/minecraft/core/HolderLookup\$RegistryLookup",
            ),
        )
    }

    @Test
    fun `interface only in one version is excluded from intersection`() {
        val sigA = classSignatureNoTypeParam(
            "com/example/Common",
            "com/example/OnlyInA",
        )
        val sigB = classSignatureNoTypeParam(
            "com/example/Common",
        )

        val result = SignatureIntersector.intersectClassSignatures(sigA, sigB)

        assertNotNull(result)
        assertInterfaceSignatures(
            result,
            expectedInterfaces = listOf("com/example/Common"),
            unexpectedInterfaces = listOf("com/example/OnlyInA"),
        )
    }

    @Test
    fun `both versions have no interfaces produces empty superInterfaces`() {
        val sigA = classSignatureNoTypeParam()
        val sigB = classSignatureNoTypeParam()

        val result = SignatureIntersector.intersectClassSignatures(sigA, sigB)

        assertNotNull(result)
        val parsed = parseClassSignature(result)
        assert(parsed.superInterfaces.isEmpty()) {
            "Expected empty superInterfaces, got ${parsed.superInterfaces}"
        }
    }

    @Test
    fun `null signature on either side returns null`() {
        val sig = classSignatureNoTypeParam("com/example/Foo")

        assertNull(SignatureIntersector.intersectClassSignatures(null, sig))
        assertNull(SignatureIntersector.intersectClassSignatures(sig, null))
        assertNull(SignatureIntersector.intersectClassSignatures(null, null))
    }

    @Test
    fun `identical signatures are preserved`() {
        val sig = classSignature(
            "T",
            "com/mojang/serialization/Keyable",
            "net/minecraft/core/IdMap",
        )

        val result = SignatureIntersector.intersectClassSignatures(sig, sig)

        assertNotNull(result)
        assertInterfaceSignatures(
            result,
            expectedInterfaces = listOf(
                "com/mojang/serialization/Keyable",
                "net/minecraft/core/IdMap",
            ),
        )
    }
}
