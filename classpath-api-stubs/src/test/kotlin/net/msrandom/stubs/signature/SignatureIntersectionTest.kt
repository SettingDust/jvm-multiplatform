package net.msrandom.stubs.signature

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignatureIntersectionTest {
    @Test
    fun `class type variables are matched by declaration position`() {
        assertEquals(
            "<T:Ljava/lang/Object;>Ljava/util/List<TT;>;",
            SignatureIntersector.intersectClassSignatures(
                listOf(
                    "<T:Ljava/lang/Object;>Ljava/util/List<TT;>;",
                    "<U:Ljava/lang/Object;>Ljava/util/List<TU;>;",
                    "<V:Ljava/lang/Object;>Ljava/util/List<TV;>;",
                ),
            ),
        )
    }

    @Test
    fun `conflicting generic arguments are omitted`() {
        assertNull(
            SignatureIntersector.intersectClassSignatures(
                listOf(
                    "Ljava/util/List<Ljava/lang/String;>;",
                    "Ljava/util/List<Ljava/lang/Integer;>;",
                ),
            ),
        )
    }

    @Test
    fun `method type variable conflicts are omitted`() {
        assertNull(
            SignatureIntersector.intersectMethodSignatures(
                listOf(
                    "<T:Ljava/lang/Object;>(TT;)TT;",
                    "<T:Ljava/lang/Number;>(TT;)TT;",
                ),
            ),
        )
    }
}
