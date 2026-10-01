package net.msrandom.stubs.signature

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignatureIntersectionTest {
    @Test
    fun `scope bounds erasure and malformed signatures are conservative`() {
        val owner = "<T:Ljava/lang/Number;>Ljava/lang/Object;"
        assertEquals(owner, SignatureIntersector.validClass(owner, "java/lang/Object", emptyList()))
        assertEquals("TT;", SignatureIntersector.validMember("TT;", "Ljava/lang/Number;", owner, false))
        assertNull(SignatureIntersector.validMember("TT;", "Ljava/lang/Object;", owner, false))
        assertNull(SignatureIntersector.validMember("()TT;", "()Ljava/lang/Object;", owner, true))
        assertNull(SignatureIntersector.validMember("TT;", "Ljava/lang/Object;", null, false))
        assertNull(SignatureIntersector.validMember("()TT;", "()Ljava/lang/Object;", null, true))
        assertNull(SignatureIntersector.validMember("Ljava/util/List<TU;>;", "Ljava/util/List;", owner, false))
        assertNull(SignatureIntersector.validMember("TU;", "()Ljava/lang/Object;", owner, true))
        assertNull(SignatureIntersector.validMember("TV;", "()Ljava/lang/Object;", owner, true))
        assertNull(SignatureIntersector.validMember("Ljava/lang/String;", "Ljava/lang/Integer;", owner, false))
        val local = "<T:Ljava/lang/Object;>(TT;)TT;"
        assertEquals(local, SignatureIntersector.validMember(local, "(Ljava/lang/Object;)Ljava/lang/Object;", owner, true))
        assertNull(SignatureIntersector.validMember("<U:TV;>()TU;", "()Ljava/lang/Object;", owner, true))
        for (invalid in listOf("<T:TU;>Ljava/lang/Object;", "<T:TT;>Ljava/lang/Object;", "<T:TU;U:TT;>Ljava/lang/Object;", "Ljava/lang/Object;Lbad/Interface<TU;>;")) {
            assertNull(SignatureIntersector.validClass(invalid, "java/lang/Object", emptyList()), invalid)
        }
        val recursive = "<T::Ljava/lang/Comparable<TT;>;>Ljava/lang/Object;"
        assertEquals(recursive, SignatureIntersector.validClass(recursive, "java/lang/Object", emptyList()))
        assertEquals("TT;", SignatureIntersector.validMember("TT;", "Ljava/lang/Comparable;", recursive, false))
        assertNull(SignatureIntersector.validClass(owner, "java/lang/Number", emptyList()))
        assertNull(SignatureIntersector.validClass(owner, "java/lang/Object", listOf("java/io/Serializable")))
        val array = "[TT;"
        assertEquals(array, SignatureIntersector.validMember(array, "[Ljava/lang/Number;", owner, false))
    }

    @Test
    fun `method local names follow declaration position and bounds`() {
        val expected = "<U:Ljava/lang/Object;>(TU;)TU;"
        assertEquals(expected, SignatureIntersector.intersectMethodSignatures(listOf(expected, "<V:Ljava/lang/Object;>(TV;)TV;")))
        assertEquals(expected, SignatureIntersector.validMember(expected, "(Ljava/lang/Object;)Ljava/lang/Object;", null, true))
        assertNull(SignatureIntersector.intersectFieldSignatures(listOf("Ljava/util/List<Ljava/lang/String;>;", "Ljava/util/List<Ljava/lang/Integer;>;")))
    }

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
