package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.token.*

class TokenCacheContextsTest {
    @Test
    fun valueCacheContextShouldSeparateSolutionAndFixedCacheKey() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "value_context_symbol"
        )
        val context = ValueCacheContext<Flt64>()
        val fixedValues = mapOf<Symbol, Flt64>(symbol to Flt64(3.0))

        assertFalse(context.cached(symbol, null))
        assertFalse(context.cached(symbol, fixedValues))

        context.put(symbol, null, Flt64.one)
        context.put(symbol, fixedValues, Flt64(2.0))

        assertTrue(context.cached(symbol, null))
        assertTrue(context.cached(symbol, fixedValues))
        assertEquals(Flt64.one, context.value(symbol, null))
        assertEquals(Flt64(2.0), context.value(symbol, fixedValues))
    }

    @Test
    fun tokenCacheContextsShouldFlushIndependently() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "context_flush_symbol"
        )
        val contexts = TokenCacheContexts<Flt64>()
        val range = ExpressionRange(ValueRange(Flt64.zero, Flt64.one).value!!)

        contexts.linearFlatten.put(symbol, symbol.flattenedMonomials)
        contexts.value.put(symbol, null, Flt64.one)
        contexts.range.put(symbol, range)

        contexts.clearFlatten()
        assertFalse(contexts.linearFlatten.contains(symbol))
        assertTrue(contexts.value.cached(symbol, null))
        assertTrue(contexts.range.contains(symbol))

        contexts.clearValue()
        assertFalse(contexts.value.cached(symbol, null))
        assertTrue(contexts.range.contains(symbol))

        contexts.clearRange()
        assertFalse(contexts.range.contains(symbol))
    }

    @Test
    fun tokenTableShouldExposeFlattenAndRangeContext() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "table_context_symbol"
        )
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        tokenTable.cacheLinearFlatten(symbol, symbol.flattenedMonomials)
        tokenTable.cacheRange(symbol, symbol.range)

        assertEquals(true, tokenTable.cachedLinearFlatten(symbol))
        assertEquals(true, tokenTable.cachedRange(symbol))

        tokenTable.flush()

        assertEquals(false, tokenTable.cachedLinearFlatten(symbol))
        assertEquals(false, tokenTable.cachedRange(symbol))
    }

    @Test
    fun registerShouldPopulateFlattenAndRangeContext() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "register_context_symbol"
        )
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        listOf(symbol).register(tokenTable)

        assertEquals(true, tokenTable.cachedLinearFlatten(symbol))
        assertEquals(true, tokenTable.cachedRange(symbol))
    }

    @Test
    fun closeShouldUnbindTokenTableContext() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "close_context_symbol"
        )
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        listOf(symbol).register(tokenTable)
        assertEquals(tokenTable, boundTokenTableContext(symbol))

        tokenTable.close()
        assertEquals(null, boundTokenTableContext(symbol))
    }

    @Test
    fun contextsShouldSupportNonSymbolCacheKey() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "nonsymbol_cache_key"
        )
        val monomialKey = newTokenCacheKey(Linear, "__monomial_cache_key__")
        val polynomialKey = newTokenCacheKey(Linear, "__polynomial_cache_key__")
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        val fixedValues = emptyMap<Symbol, Flt64>()
        val range = ExpressionRange(ValueRange(Flt64.zero, Flt64.one).value!!)
        val flattened = symbol.flattenedMonomials

        tokenTable.cacheLinearFlatten(monomialKey, flattened)
        tokenTable.cacheRange(polynomialKey, range)
        tokenTable.cache(monomialKey, null, Flt64.one)
        tokenTable.cache(polynomialKey, fixedValues, Flt64(2.0))

        assertEquals(true, tokenTable.cachedLinearFlatten(monomialKey))
        assertEquals(flattened, tokenTable.cachedLinearFlattenValue(monomialKey))
        assertEquals(range, tokenTable.cachedRangeValue(polynomialKey))
        assertEquals(Flt64.one, tokenTable.cachedValue(monomialKey, null))
        assertEquals(Flt64(2.0), tokenTable.cachedValue(polynomialKey, fixedValues))
    }

    @Test
    fun concurrentRegisterShouldSucceed() = runBlocking {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "concurrent_register_context_symbol"
        )
        val tokenTable = ConcurrentAutoTokenTable<Flt64>(Linear, false)

        val result = listOf(symbol).register(tokenTable)

        assertTrue(result is Ok<*, *, *>)

        tokenTable.close()
    }
}
