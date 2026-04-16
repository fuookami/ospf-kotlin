package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.intermediate_model.*
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import kotlin.test.Test
import kotlin.test.assertEquals

class CacheRebindTest {
    @Test
    fun removeShouldClearCachesAndAllowRebind() {
        val symbol = LinearExpressionSymbol(
            polynomial = LinearPolynomial(constant = Flt64.one),
            name = "remove_rebind_symbol"
        )
        val tokenTable = AutoTokenTable(Linear, false)

        listOf(symbol).register(tokenTable)
        tokenTable.cache(symbol, null, Flt64.one)

        assertEquals(tokenTable, boundTokenTableContext(symbol))
        assertEquals(true, tokenTable.cachedLinearFlatten(symbol))
        assertEquals(true, tokenTable.cachedRange(symbol))
        assertEquals(true, tokenTable.cached(symbol, null))

        tokenTable.remove(symbol)

        assertEquals(null, boundTokenTableContext(symbol))
        assertEquals(false, tokenTable.cachedLinearFlatten(symbol))
        assertEquals(false, tokenTable.cachedRange(symbol))
        assertEquals(false, tokenTable.cached(symbol, null))

        listOf(symbol).register(tokenTable)

        assertEquals(tokenTable, boundTokenTableContext(symbol))
        assertEquals(true, tokenTable.cachedLinearFlatten(symbol))
        assertEquals(true, tokenTable.cachedRange(symbol))
        assertEquals(true, tokenTable.cached(symbol, null))

        tokenTable.close()
    }

    @Test
    fun rebindToNewTokenTableShouldInvalidateOldTableCaches() {
        val symbol = LinearExpressionSymbol(
            polynomial = LinearPolynomial(constant = Flt64.one),
            name = "dual_table_rebind_symbol"
        )
        val oldTokenTable = AutoTokenTable(Linear, false)
        val newTokenTable = AutoTokenTable(Linear, false)

        listOf(symbol).register(oldTokenTable)
        oldTokenTable.cache(symbol, null, Flt64.one)

        assertEquals(oldTokenTable, boundTokenTableContext(symbol))
        assertEquals(true, oldTokenTable.cachedLinearFlatten(symbol))
        assertEquals(true, oldTokenTable.cachedRange(symbol))
        assertEquals(true, oldTokenTable.cached(symbol, null))

        listOf(symbol).register(newTokenTable)

        assertEquals(newTokenTable, boundTokenTableContext(symbol))
        assertEquals(true, newTokenTable.cachedLinearFlatten(symbol))
        assertEquals(true, newTokenTable.cachedRange(symbol))
        assertEquals(true, newTokenTable.cached(symbol, null))

        assertEquals(false, oldTokenTable.cachedLinearFlatten(symbol))
        assertEquals(false, oldTokenTable.cachedRange(symbol))
        assertEquals(false, oldTokenTable.cached(symbol, null))

        oldTokenTable.close()
        newTokenTable.close()
    }
}
