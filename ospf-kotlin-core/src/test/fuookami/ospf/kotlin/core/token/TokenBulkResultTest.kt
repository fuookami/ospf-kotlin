package fuookami.ospf.kotlin.core.token

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.variable.*

class TokenBulkResultTest {
    @Test
    fun allResultPathsRefreshSharedImmutableAndMutableOwners() {
        for (auto in listOf(false, true)) {
            val list: MutableTokenList<Flt64> = if (auto) AutoTokenList(false) else ManualTokenList(false)
            val variables = List(4) { RealVar("shared_$it") }
            assertTrue(list.add(variables) is Ok)
            val copy = list.copy()
            val view = TokenList(list)
            val subset = TokenList(mapOf(variables[0].key to assertNotNull(list.find(variables[0]))))
            val owners = listOf(list, copy, view, subset)
            try {
                assertTrue(owners.all { !it.cachedSolution })
                list.setSolverSolution(List(4) { Flt64.one })
                assertTrue(owners.all { it.cachedSolution })
                copy.clearSolution()
                assertTrue(owners.all { !it.cachedSolution })
                view.setSolverSolution(mapOf(variables[1] to Flt64.one))
                assertTrue(list.cachedSolution)
                assertTrue(copy.cachedSolution)
                assertTrue(view.cachedSolution)
                assertFalse(subset.cachedSolution)
                assertNotNull(list.find(variables[1]))._result = null
                assertTrue(owners.all { !it.cachedSolution })
                copy.setSolution(List(4) { Flt64.zero })
                assertTrue(owners.all { it.cachedSolution })
                view.clearSolution()
                subset.setSolution(mapOf(variables[0] to Flt64.one))
                assertTrue(owners.all { it.cachedSolution })
                list.remove(variables[0])
                assertFalse(list.cachedSolution)
                assertTrue(copy.cachedSolution)
                subset.clearSolution()
                assertTrue(owners.all { !it.cachedSolution })
            } finally {
                owners.reversed().forEach { it.close() }
            }
        }
    }

    @Test
    fun sharedOwnerCallbacksDoNotRescanTheTableForEveryAssignedToken() {
        val list = ManualTokenList<Flt64>(false)
        val size = 256
        assertTrue(list.add(List(size) { RealVar("counted_$it") }) is Ok)
        val base = list.tokensInSolver.reversed().associateBy { it.key }
        var reads = 0
        val counted = object : Map<VariableItemKey, Token<Flt64>> by base {
            override val values: Collection<Token<Flt64>> = object : AbstractCollection<Token<Flt64>>() {
                override val size = base.size
                override fun iterator(): Iterator<Token<Flt64>> {
                    val iterator = base.values.iterator()
                    return object : Iterator<Token<Flt64>> {
                        override fun hasNext() = iterator.hasNext()
                        override fun next(): Token<Flt64> {
                            ++reads
                            return iterator.next()
                        }
                    }
                }
            }
        }
        val view = TokenList(counted)
        try {
            assertFalse(view.cachedSolution)
            reads = 0
            list.setSolution(List(size) { Flt64.one })
            assertTrue(view.cachedSolution)
            assertTrue(reads <= size, "Shared owner scanned $reads tokens for $size assignments")
            list.clearSolution()
            assertFalse(view.cachedSolution)
        } finally {
            view.close()
            list.close()
        }
    }
}
