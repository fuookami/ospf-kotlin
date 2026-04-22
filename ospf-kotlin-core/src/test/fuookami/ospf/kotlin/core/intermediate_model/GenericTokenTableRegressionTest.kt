package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * P3-2 regression: verify generic TokenTable<V>, Cell<V>, TokenCacheContexts<V>
 * compile and function correctly when V is explicitly parameterized.
 */
class GenericTokenTableRegressionTest {

    @Test
    fun `MutableTokenTableF64 is MutableTokenTable of Flt64`() {
        val table: MutableTokenTable<Flt64> = ManualTokenTable<Flt64>(Linear, false)
        assertTrue(table is MutableTokenTableF64)
    }

    @Test
    fun `AutoTokenTableF64 is MutableTokenTable of Flt64`() {
        val table: MutableTokenTable<Flt64> = AutoTokenTable<Flt64>(Linear, false)
        assertTrue(table is MutableTokenTableF64)
    }

    @Test
    fun `ConcurrentMutableTokenTableF64 is ConcurrentMutableTokenTable of Flt64`() {
        val table: ConcurrentMutableTokenTable<Flt64> = ConcurrentManualAddTokenTable<Flt64>(Linear, false)
        assertTrue(table is ConcurrentMutableTokenTableF64)
    }

    @Test
    fun `TokenCacheContextsF64 is TokenCacheContexts of Flt64`() {
        val ctx: TokenCacheContexts<Flt64> = TokenCacheContextsF64()
        assertTrue(ctx is TokenCacheContextsF64)
    }

    @Test
    fun `LinearFlattenContextF64 is LinearFlattenContext of Flt64`() {
        val ctx: LinearFlattenContext<Flt64> = LinearFlattenContextF64()
        assertTrue(ctx is LinearFlattenContextF64)
    }

    @Test
    fun `QuadraticFlattenContextF64 is QuadraticFlattenContext of Flt64`() {
        val ctx: QuadraticFlattenContext<Flt64> = QuadraticFlattenContextF64()
        assertTrue(ctx is QuadraticFlattenContextF64)
    }

    @Test
    fun `RangeCacheContextF64 is RangeCacheContext of Flt64`() {
        val ctx: RangeCacheContext<Flt64> = RangeCacheContextF64()
        assertTrue(ctx is RangeCacheContextF64)
    }

    @Test
    fun `ValueCacheContextF64 is ValueCacheContext of Flt64`() {
        val ctx: ValueCacheContext<Flt64> = ValueCacheContextF64()
        assertTrue(ctx is ValueCacheContextF64)
    }

    @Test
    fun `LinearCellImplF64 is LinearCellImpl of Flt64`() {
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        val symbol = LinearExpressionSymbol(constant = Flt64.one, name = "cell_test_x")
        listOf(symbol).register(tokenTable)
        val token = tokenTable.find(symbol)!!
        val cell: LinearCellImpl<Flt64> = LinearCellImplF64(tokenTable, Flt64.one, token)
        assertTrue(cell is LinearCellImplF64)
        assertEquals(Flt64.one, cell.coefficientF64)
    }

    @Test
    fun `QuadraticCellImplF64 is QuadraticCellImpl of Flt64`() {
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        val symbol1 = LinearExpressionSymbol(constant = Flt64.one, name = "cell_test_x1")
        val symbol2 = LinearExpressionSymbol(constant = Flt64.one, name = "cell_test_x2")
        listOf(symbol1, symbol2).register(tokenTable)
        val token1 = tokenTable.find(symbol1)!!
        val token2 = tokenTable.find(symbol2)!!
        val cell: QuadraticCellImpl<Flt64> = QuadraticCellImplF64(tokenTable, Flt64.one, token1, token2)
        assertTrue(cell is QuadraticCellImplF64)
        assertEquals(Flt64.one, cell.coefficientF64)
    }

    @Test
    fun `TokenCacheContexts generic cache operations`() {
        val ctx = TokenCacheContexts<Flt64>()
        val key = Any()

        // Linear flatten
        val linearData = LinearFlattenData<Flt64>(emptyList(), Flt64.zero)
        ctx.linearFlatten.put(key, linearData)
        assertTrue(ctx.linearFlatten.contains(key))
        assertEquals(linearData, ctx.linearFlatten.get(key))

        // Quadratic flatten
        val quadData = QuadraticFlattenData<Flt64>(emptyList(), Flt64.zero)
        ctx.quadraticFlatten.put(key, quadData)
        assertTrue(ctx.quadraticFlatten.contains(key))
        assertEquals(quadData, ctx.quadraticFlatten.get(key))

        // Value
        ctx.value.put(key, solution = null, value = Flt64(42.0))
        assertTrue(ctx.value.cached(key, solution = null))
        assertEquals(Flt64(42.0), ctx.value.value(key, solution = null))

        // Clear
        ctx.clearAll()
        assertFalse(ctx.linearFlatten.contains(key))
        assertFalse(ctx.quadraticFlatten.contains(key))
        assertFalse(ctx.value.cached(key, solution = null))
    }

    @Test
    fun `boundSymbols returns all cached keys`() {
        val ctx = TokenCacheContexts<Flt64>()
        val key1 = Any()
        val key2 = Any()

        ctx.linearFlatten.put(key1, LinearFlattenData<Flt64>(emptyList(), Flt64.zero))
        ctx.quadraticFlatten.put(key2, QuadraticFlattenData<Flt64>(emptyList(), Flt64.zero))

        val bound = ctx.boundSymbols()
        assertTrue(bound.contains(key1))
        assertTrue(bound.contains(key2))
    }
}
