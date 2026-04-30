package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.RealVar
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
    fun `MutableTokenTableFlt64 is MutableTokenTable of Flt64`() {
        val table: MutableTokenTable<Flt64> = ManualTokenTable<Flt64>(Linear, false)
        assertTrue(table is MutableTokenTableFlt64)
    }

    @Test
    fun `AutoTokenTableFlt64 is MutableTokenTable of Flt64`() {
        val table: MutableTokenTable<Flt64> = AutoTokenTable<Flt64>(Linear, false)
        assertTrue(table is MutableTokenTableFlt64)
    }

    @Test
    fun `ConcurrentMutableTokenTableFlt64 is ConcurrentMutableTokenTable of Flt64`() {
        val table: ConcurrentMutableTokenTable<Flt64> = ConcurrentManualAddTokenTable<Flt64>(Linear, false)
        assertTrue(table is ConcurrentMutableTokenTableFlt64)
    }

    @Test
    fun `TokenCacheContextsFlt64 is TokenCacheContexts of Flt64`() {
        val ctx: TokenCacheContexts<Flt64> = TokenCacheContextsFlt64()
        assertTrue(ctx is TokenCacheContextsFlt64)
    }

    @Test
    fun `LinearFlattenContextFlt64 is LinearFlattenContext of Flt64`() {
        val ctx: LinearFlattenContext<Flt64> = LinearFlattenContextFlt64()
        assertTrue(ctx is LinearFlattenContextFlt64)
    }

    @Test
    fun `QuadraticFlattenContextFlt64 is QuadraticFlattenContext of Flt64`() {
        val ctx: QuadraticFlattenContext<Flt64> = QuadraticFlattenContextFlt64()
        assertTrue(ctx is QuadraticFlattenContextFlt64)
    }

    @Test
    fun `RangeCacheContextFlt64 is RangeCacheContext of Flt64`() {
        val ctx: RangeCacheContext<Flt64> = RangeCacheContextFlt64()
        assertTrue(ctx is RangeCacheContextFlt64)
    }

    @Test
    fun `ValueCacheContextFlt64 is ValueCacheContext of Flt64`() {
        val ctx: ValueCacheContext<Flt64> = ValueCacheContextFlt64()
        assertTrue(ctx is ValueCacheContextFlt64)
    }

    @Test
    fun `LinearCellImplFlt64 is LinearCellImpl of Flt64`() {
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        val x = RealVar("cell_test_x")
        tokenTable.add(x)
        val token = tokenTable.find(x)!!
        val cell: LinearCellImpl<Flt64> = LinearCellImplFlt64(tokenTable, Flt64.one, token)
        assertTrue(cell is LinearCellImplFlt64)
        assertEquals(Flt64.one, cell.coefficientFlt64)
    }

    @Test
    fun `QuadraticCellImplFlt64 is QuadraticCellImpl of Flt64`() {
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        val x1 = RealVar("cell_test_x1")
        val x2 = RealVar("cell_test_x2")
        tokenTable.add(x1)
        tokenTable.add(x2)
        val token1 = tokenTable.find(x1)!!
        val token2 = tokenTable.find(x2)!!
        val cell: QuadraticCellImpl<Flt64> = QuadraticCellImplFlt64(tokenTable, Flt64.one, token1, token2)
        assertTrue(cell is QuadraticCellImplFlt64)
        assertEquals(Flt64.one, cell.coefficientFlt64)
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

    // ========== copy() regression: token contents and indices must survive copy ==========

    @Test
    fun `AutoTokenTable copy preserves tokens and indices`() {
        val table = AutoTokenTable<Flt64>(Linear, false)
        val x = RealVar("copy_auto_x")
        val y = RealVar("copy_auto_y")
        table.add(x)
        table.add(y)

        val copied = table.copy() as AutoTokenTable<Flt64>
        val tokenX = copied.find(x)
        val tokenY = copied.find(y)
        assertNotNull(tokenX, "copied table should contain x")
        assertNotNull(tokenY, "copied table should contain y")
        assertEquals(2, copied.tokens.size, "copied table should have 2 tokens")
        assertEquals(0, tokenX!!.solverIndex, "x should keep solverIndex 0")
        assertEquals(1, tokenY!!.solverIndex, "y should keep solverIndex 1")
    }

    @Test
    fun `ManualTokenTable copy preserves tokens and indices`() {
        val table = ManualTokenTable<Flt64>(Linear, false)
        val x = RealVar("copy_manual_x")
        val y = RealVar("copy_manual_y")
        table.add(x)
        table.add(y)

        val copied = table.copy() as ManualTokenTable<Flt64>
        val tokenX = copied.find(x)
        val tokenY = copied.find(y)
        assertNotNull(tokenX, "copied table should contain x")
        assertNotNull(tokenY, "copied table should contain y")
        assertEquals(2, copied.tokens.size, "copied table should have 2 tokens")
        assertEquals(0, tokenX!!.solverIndex, "x should keep solverIndex 0")
        assertEquals(1, tokenY!!.solverIndex, "y should keep solverIndex 1")
    }

    @Test
    fun `ConcurrentAutoTokenTable copy preserves tokens and indices`() {
        val table = ConcurrentAutoTokenTable<Flt64>(Linear, false)
        val x = RealVar("copy_conc_auto_x")
        val y = RealVar("copy_conc_auto_y")
        table.add(x)
        table.add(y)

        val copied = table.copy() as ConcurrentAutoTokenTable<Flt64>
        val tokenX = copied.find(x)
        val tokenY = copied.find(y)
        assertNotNull(tokenX, "copied table should contain x")
        assertNotNull(tokenY, "copied table should contain y")
        assertEquals(2, copied.tokens.size, "copied table should have 2 tokens")
        assertEquals(0, tokenX!!.solverIndex, "x should keep solverIndex 0")
        assertEquals(1, tokenY!!.solverIndex, "y should keep solverIndex 1")
    }

    @Test
    fun `ConcurrentManualAddTokenTable copy preserves tokens and indices`() {
        val table = ConcurrentManualAddTokenTable<Flt64>(Linear, false)
        val x = RealVar("copy_conc_manual_x")
        val y = RealVar("copy_conc_manual_y")
        table.add(x)
        table.add(y)

        val copied = table.copy() as ConcurrentManualAddTokenTable<Flt64>
        val tokenX = copied.find(x)
        val tokenY = copied.find(y)
        assertNotNull(tokenX, "copied table should contain x")
        assertNotNull(tokenY, "copied table should contain y")
        assertEquals(2, copied.tokens.size, "copied table should have 2 tokens")
        assertEquals(0, tokenX!!.solverIndex, "x should keep solverIndex 0")
        assertEquals(1, tokenY!!.solverIndex, "y should keep solverIndex 1")
    }

    @Test
    fun `copy of empty AutoTokenTable yields empty table`() {
        val table = AutoTokenTable<Flt64>(Linear, false)
        val copied = table.copy()
        assertEquals(0, copied.tokens.size, "empty table copy should have 0 tokens")
    }

    @Test
    fun `copy of empty ManualTokenTable yields empty table`() {
        val table = ManualTokenTable<Flt64>(Linear, false)
        val copied = table.copy()
        assertEquals(0, copied.tokens.size, "empty table copy should have 0 tokens")
    }
}
