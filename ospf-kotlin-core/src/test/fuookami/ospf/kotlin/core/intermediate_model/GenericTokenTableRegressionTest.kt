package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.intermediate.LinearCellImpl
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentAutoTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentManualAddTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenContext
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.token.MutableTokenTable
import fuookami.ospf.kotlin.core.token.QuadraticFlattenContext
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.token.RangeCacheContext
import fuookami.ospf.kotlin.core.token.TokenCacheContexts
import fuookami.ospf.kotlin.core.token.ValueCacheContext
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * P3-2 regression: verify generic TokenTable, Cell, TokenCacheContexts
 * compile and function correctly when V is explicitly parameterized.
 */
class GenericTokenTableRegressionTest {

    @Test
    fun mutableTokenTableFlt64IsMutableTokenTableOfFlt64() {
        val table: MutableTokenTable<Flt64> = ManualTokenTable<Flt64>(Linear, false)
        assertTrue(table is ManualTokenTable<*>)
    }

    @Test
    fun autoTokenTableFlt64IsMutableTokenTableOfFlt64() {
        val table: MutableTokenTable<Flt64> = AutoTokenTable<Flt64>(Linear, false)
        assertTrue(table is AutoTokenTable<*>)
    }

    @Test
    fun concurrentMutableTokenTableFlt64IsConcurrentMutableTokenTableOfFlt64() {
        val table: ConcurrentMutableTokenTable<Flt64> = ConcurrentManualAddTokenTable<Flt64>(Linear, false)
        assertTrue(table is ConcurrentManualAddTokenTable<*>)
    }

    @Test
    fun tokenCacheContextsFlt64IsTokenCacheContextsOfFlt64() {
        val ctx: Any = TokenCacheContexts<Flt64>()
        assertTrue(ctx is TokenCacheContexts<*>)
    }

    @Test
    fun linearFlattenContextFlt64IsLinearFlattenContextOfFlt64() {
        val ctx: Any = LinearFlattenContext<Flt64>()
        assertTrue(ctx is LinearFlattenContext<*>)
    }

    @Test
    fun quadraticFlattenContextFlt64IsQuadraticFlattenContextOfFlt64() {
        val ctx: Any = QuadraticFlattenContext<Flt64>()
        assertTrue(ctx is QuadraticFlattenContext<*>)
    }

    @Test
    fun rangeCacheContextFlt64IsRangeCacheContextOfFlt64() {
        val ctx: Any = RangeCacheContext<Flt64>()
        assertTrue(ctx is RangeCacheContext<*>)
    }

    @Test
    fun valueCacheContextFlt64IsValueCacheContextOfFlt64() {
        val ctx: Any = ValueCacheContext<Flt64>()
        assertTrue(ctx is ValueCacheContext<*>)
    }

    @Test
    fun linearCellImplFlt64IsLinearCellImplOfFlt64() {
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        val x = RealVar("cell_test_x")
        tokenTable.add(x)
        val token = tokenTable.find(x)!!
        val cell: LinearCellImpl<Flt64> = LinearCellImpl<Flt64>(tokenTable, Flt64.one, token, IntoValue.Identity)
        assertEquals(Flt64.one, cell.coefficient)
    }

    @Test
    fun quadraticCellImplFlt64IsQuadraticCellImplOfFlt64() {
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        val x1 = RealVar("cell_test_x1")
        val x2 = RealVar("cell_test_x2")
        tokenTable.add(x1)
        tokenTable.add(x2)
        val token1 = tokenTable.find(x1)!!
        val token2 = tokenTable.find(x2)!!
        val cell: QuadraticCellImpl<Flt64> = QuadraticCellImpl<Flt64>(tokenTable, Flt64.one, token1, token2, IntoValue.Identity)
        assertEquals(Flt64.one, cell.coefficient)
    }

    @Test
    fun tokenCacheContextsGenericCacheOperations() {
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
    fun boundSymbolsReturnsAllCachedKeys() {
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
    fun autoTokenTableCopyPreservesTokensAndIndices() {
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
    fun manualTokenTableCopyPreservesTokensAndIndices() {
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
    fun concurrentAutoTokenTableCopyPreservesTokensAndIndices() {
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
    fun concurrentManualAddTokenTableCopyPreservesTokensAndIndices() {
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
    fun copyOfEmptyAutoTokenTableYieldsEmptyTable() {
        val table = AutoTokenTable<Flt64>(Linear, false)
        val copied = table.copy()
        assertEquals(0, copied.tokens.size, "empty table copy should have 0 tokens")
    }

    @Test
    fun copyOfEmptyManualTokenTableYieldsEmptyTable() {
        val table = ManualTokenTable<Flt64>(Linear, false)
        val copied = table.copy()
        assertEquals(0, copied.tokens.size, "empty table copy should have 0 tokens")
    }
}
