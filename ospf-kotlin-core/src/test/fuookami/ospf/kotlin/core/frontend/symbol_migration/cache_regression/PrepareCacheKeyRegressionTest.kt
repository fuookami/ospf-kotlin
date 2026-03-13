package fuookami.ospf.kotlin.core.frontend.symbol_migration.cache_regression

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Linear
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.core.frontend.variable.RealVar
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AutoTokenTable
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCachedWithFixedCacheKey
import fuookami.ospf.kotlin.core.frontend.expression.symbol.shouldPrepare
import fuookami.ospf.kotlin.core.frontend.expression.symbol.shouldPrepareWithFixedCacheKey

class PrepareCacheKeyRegressionTest {
    @Test
    fun fixedCacheKeyShouldIgnoreValuesCacheKey() {
        val key = LinearExpressionSymbol(
            polynomial = LinearPolynomial(constant = Flt64.one),
            name = "cache_key_symbol"
        )
        val tokenTable = AutoTokenTable(Linear, false)
        val x = RealVar("x")
        val values = mapOf<Symbol, Flt64>(x to Flt64(3.0))

        tokenTable.cache(symbol = key, solution = null, value = Flt64.one)

        assertTrue(key.shouldPrepare(values, tokenTable))
        assertFalse(key.shouldPrepareWithFixedCacheKey(values, tokenTable))
    }

    @Test
    fun fixedCacheHelperShouldSkipBlockWhenSolutionCacheExists() {
        val key = LinearExpressionSymbol(
            polynomial = LinearPolynomial(constant = Flt64.one),
            name = "cache_key_symbol_block"
        )
        val tokenTable = AutoTokenTable(Linear, false)
        val x = RealVar("x")
        val values = mapOf<Symbol, Flt64>(x to Flt64(1.0))

        tokenTable.cache(symbol = key, solution = null, value = Flt64.one)

        var fixedBlockInvoked = false
        val fixedResult = key.prepareIfNotCachedWithFixedCacheKey(values, tokenTable) {
            fixedBlockInvoked = true
            Flt64.zero
        }
        assertFalse(fixedBlockInvoked)
        assertNull(fixedResult)

        var normalBlockInvoked = false
        val normalResult = key.prepareIfNotCached(values, tokenTable) {
            normalBlockInvoked = true
            Flt64.zero
        }
        assertTrue(normalBlockInvoked)
        assertNotNull(normalResult)
    }
}
