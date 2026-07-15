package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.token.*

class CacheKeyConflictTest {
    @Test
    fun symbolAndPrivateKeyShouldNotOverwriteLinearFlattenCache() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "flatten_conflict_symbol"
        )
        val privateKey = newTokenCacheKey(Linear, "__flatten_private_key__")
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        listOf(symbol).register(tokenTable)

        val symbolFlatten = symbol.flattenedMonomials
        val privateFlatten = symbol.flattenedMonomials

        tokenTable.cacheLinearFlatten(symbol, symbolFlatten)
        tokenTable.cacheLinearFlatten(privateKey, privateFlatten)

        assertTrue(tokenTable.cachedLinearFlatten(symbol))
        assertTrue(tokenTable.cachedLinearFlatten(privateKey))
        assertEquals(symbolFlatten, tokenTable.cachedLinearFlattenValue(symbol))
        assertEquals(privateFlatten, tokenTable.cachedLinearFlattenValue(privateKey))

        tokenTable.close()
    }

    @Test
    fun symbolAndPrivateKeyShouldNotOverwriteRangeCache() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "range_conflict_symbol"
        )
        val privateKey = newTokenCacheKey(Linear, "__range_private_key__")
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        listOf(symbol).register(tokenTable)

        val symbolRange = symbol.range
        val privateRange = ExpressionRange(ValueRange(Flt64.zero, Flt64.one).value!!)

        tokenTable.cacheRange(symbol, symbolRange)
        tokenTable.cacheRange(privateKey, privateRange)

        assertTrue(tokenTable.cachedRange(symbol))
        assertTrue(tokenTable.cachedRange(privateKey))
        assertEquals(symbolRange, tokenTable.cachedRangeValue(symbol))
        assertEquals(privateRange, tokenTable.cachedRangeValue(privateKey))

        tokenTable.close()
    }

    @Test
    fun symbolAndPrivateKeyShouldNotOverwriteValueCache() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "value_conflict_symbol"
        )
        val privateKey = newTokenCacheKey(Linear, "__value_private_key__")
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        val fixedValues = emptyMap<Symbol, Flt64>()

        listOf(symbol).register(tokenTable)

        tokenTable.cache(symbol, null, Flt64(1.0))
        tokenTable.cache(privateKey, fixedValues, Flt64(2.0))

        assertTrue(tokenTable.cached(symbol, null))
        assertEquals(true, tokenTable.cached(privateKey, fixedValues))
        assertEquals(Flt64(1.0), tokenTable.cachedValue(symbol, null))
        assertEquals(Flt64(2.0), tokenTable.cachedValue(privateKey, fixedValues))

        tokenTable.close()
    }

    @Test
    fun readingWithSymbolKeyShouldNotReturnPrivateKeyData() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "cross_read_symbol"
        )
        val privateKey = newTokenCacheKey(Linear, "__cross_read_private_key__")
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        val fixedValues = emptyMap<Symbol, Flt64>()

        val privateFlatten = symbol.flattenedMonomials
        val privateRange = ExpressionRange(ValueRange(Flt64.zero, Flt64.one).value!!)

        tokenTable.cacheLinearFlatten(privateKey, privateFlatten)
        tokenTable.cacheRange(privateKey, privateRange)
        tokenTable.cache(privateKey, fixedValues, Flt64(42.0))

        assertTrue(tokenTable.cachedLinearFlatten(privateKey))
        assertTrue(tokenTable.cachedRange(privateKey))
        assertEquals(true, tokenTable.cached(privateKey, fixedValues))

        assertEquals(false, tokenTable.cachedLinearFlatten(symbol))
        assertEquals(false, tokenTable.cachedRange(symbol))
        assertEquals(false, tokenTable.cached(symbol, fixedValues))

        tokenTable.close()
    }

    @Test
    fun readingWithPrivateKeyShouldNotReturnSymbolKeyData() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "reverse_cross_read_symbol"
        )
        val privateKey = newTokenCacheKey(Linear, "__reverse_cross_key__")
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        listOf(symbol).register(tokenTable)

        val anotherPrivateKey = newTokenCacheKey(Linear, "__another_private_key__")

        assertEquals(false, tokenTable.cachedLinearFlatten(anotherPrivateKey))
        assertEquals(false, tokenTable.cachedRange(anotherPrivateKey))
        assertEquals(false, tokenTable.cached(anotherPrivateKey, null))

        tokenTable.close()
    }

    @Test
    fun twoPrivateKeysShouldNotConflict() {
        val key1 = newTokenCacheKey(Linear, "__private_key_a__")
        val key2 = newTokenCacheKey(Linear, "__private_key_b__")
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        val fixedValues = emptyMap<Symbol, Flt64>()

        val range1 = ExpressionRange(ValueRange(Flt64.zero, Flt64(1.0)).value!!)
        val range2 = ExpressionRange(ValueRange(Flt64(2.0), Flt64(3.0)).value!!)

        tokenTable.cacheRange(key1, range1)
        tokenTable.cacheRange(key2, range2)
        tokenTable.cache(key1, fixedValues, Flt64(10.0))
        tokenTable.cache(key2, fixedValues, Flt64(20.0))

        assertTrue(tokenTable.cachedRange(key1))
        assertTrue(tokenTable.cachedRange(key2))
        assertEquals(range1, tokenTable.cachedRangeValue(key1))
        assertEquals(range2, tokenTable.cachedRangeValue(key2))

        assertEquals(true, tokenTable.cached(key1, fixedValues))
        assertEquals(true, tokenTable.cached(key2, fixedValues))
        assertEquals(Flt64(10.0), tokenTable.cachedValue(key1, fixedValues))
        assertEquals(Flt64(20.0), tokenTable.cachedValue(key2, fixedValues))

        tokenTable.close()
    }
}
