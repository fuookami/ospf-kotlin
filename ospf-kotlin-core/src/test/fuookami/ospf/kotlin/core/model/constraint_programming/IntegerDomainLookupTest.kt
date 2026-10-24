package fuookami.ospf.kotlin.core.model.constraint_programming

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Int64

class IntegerDomainLookupTest {
    @Test
    fun factoryMembershipMatchesSparseSetAndOwnsItsSortedCopy() {
        val input = (20_000 downTo -20_000 step 2).toMutableList()
        val domain = assertNotNull(IntegerDomain.values(input + input).value)
        input.clear()
        assertEquals(20_001UL, domain.cardinality)
        assertEquals(Int64(-20_000L), domain.lowerBound)
        assertEquals(Int64(20_000L), domain.upperBound)
        assertFalse(domain.values is MutableList<*>)
        for (value in -20_001L..20_001L) {
            assertEquals(value in -20_000L..20_000L && value % 2L == 0L, Int64(value) in domain)
        }
        assertFalse(Int64(Long.MIN_VALUE) in domain)
        assertFalse(Int64(Long.MAX_VALUE) in domain)
        assertEquals(domain, domain.copy(values = domain.values.toList()))
    }

    @Test
    fun directConstructionAndCopyRetainUnsortedMutableListSemantics() {
        val values = mutableListOf(Int64(9L), Int64(-7L), Int64(3L))
        val direct = IntegerDomain.Values(values)
        val copied = assertNotNull(IntegerDomain.values(listOf(1, 2)).value).copy(values = values)
        for (domain in listOf(direct, copied)) {
            assertTrue(Int64(-7L) in domain)
            assertEquals(Int64(9L), domain.lowerBound)
            assertEquals(Int64(3L), domain.upperBound)
        }
        values[1] = Int64(11L)
        for (domain in listOf(direct, copied)) {
            assertFalse(Int64(-7L) in domain)
            assertTrue(Int64(11L) in domain)
        }
        assertFalse(Int64.zero in IntegerDomain.Values(emptyList()))
    }
}
