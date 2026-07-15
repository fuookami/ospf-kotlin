package fuookami.ospf.kotlin.math.symbol

import kotlin.test.*
import org.junit.jupiter.api.Test

class SymbolIdentityTest {
    private data class TestSymbol(
        override val name: String,
        override val displayName: String? = null
    ) : Symbol

    private data class TestIdentifiedSymbol(
        override val name: String,
        override val symbolId: String,
        override val displayName: String? = null
    ) : Symbol, IdentifiedSymbol

    @Test
    fun stableIdShouldPreferIdentifiedAndOwned() {
        val identified = TestIdentifiedSymbol(name = "x", symbolId = "s-x")
        val owned = identified.owned()

        assertEquals(SymbolId("s-x"), identified.stableId())
        assertEquals(SymbolId("s-x"), owned.stableId())
        assertEquals("s-x", owned.identity())
    }

    @Test
    fun stableIdShouldFallbackToObjectIdentity() {
        val s1 = TestSymbol("x")
        val s2 = TestSymbol("x")

        assertNull(s1.stableIdOrNull())
        assertNotEquals(s1.identity(), s2.identity())
    }

    @Test
    fun defaultComparatorShouldUseNameThenStableId() {
        val a = TestIdentifiedSymbol(name = "a", symbolId = "2")
        val b = TestIdentifiedSymbol(name = "b", symbolId = "1")
        val c = TestIdentifiedSymbol(name = "a", symbolId = "1")

        val sorted = listOf(a, b, c).sortedWith(defaultSymbolComparator)
        assertEquals(listOf(c, a, b), sorted)
    }

    @Test
    fun stableIdentityHelpersShouldExposeExplicitIdentityOnly() {
        val identified = TestIdentifiedSymbol(name = "x", symbolId = "stable-x")
        val plain = TestSymbol("x")

        assertTrue(identified.hasStableId())
        assertEquals(SymbolId("stable-x"), identified.requireStableId().value!!)
        assertTrue(plain.requireStableId().failed)
    }

    @Test
    fun stableComparatorShouldRequireExplicitStableIds() {
        val a = TestIdentifiedSymbol(name = "a", symbolId = "2")
        val b = TestIdentifiedSymbol(name = "a", symbolId = "1")

        val sorted = listOf(a, b).sortedWith(defaultStableSymbolComparator)
        assertEquals(listOf(b, a), sorted)
    }
}
