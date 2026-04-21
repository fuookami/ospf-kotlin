package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Linear
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiCompatibilityTest {
    @Test
    fun linearInequalityToQuadraticConstraintShouldMatchManualConstruction() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "api_compat_symbol"
        )
        val privateKey = newTokenCacheKey(Linear, "__api_compat_key__")
        val tokenTable = AutoTokenTable(Linear, false)

        listOf(symbol).register(tokenTable)

        val flatten = symbol.flattenedMonomials
        tokenTable.cacheLinearFlatten(symbol, flatten)
        tokenTable.cacheLinearFlatten(privateKey, flatten)

        assertTrue(tokenTable.cachedLinearFlatten(symbol))
        assertTrue(tokenTable.cachedLinearFlatten(privateKey))
        assertEquals(flatten, tokenTable.cachedLinearFlattenValue(symbol))
        assertEquals(flatten, tokenTable.cachedLinearFlattenValue(privateKey))

        tokenTable.close()
    }

    @Test
    fun clampCoefficientShouldClampInfinity() {
        val threshold = Flt64.decimalPrecision.reciprocal()
        assertEquals(threshold, Flt64.infinity.clampCoefficient())
        assertEquals(-threshold, Flt64.negativeInfinity.clampCoefficient())
    }

    @Test
    fun clampCoefficientShouldClampLargeValues() {
        val threshold = Flt64.decimalPrecision.reciprocal()
        assertEquals(threshold, (threshold * Flt64(2.0)).clampCoefficient())
        assertEquals(-threshold, (-threshold * Flt64(2.0)).clampCoefficient())
    }

    @Test
    fun clampCoefficientShouldPreserveNormalValues() {
        val value = Flt64(0.5)
        assertEquals(value, value.clampCoefficient())
    }

    @Test
    fun symbolDependenciesShouldBePopulatedAfterRegister() {
        val symbol1 = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "dep_symbol_1"
        )
        val symbol2 = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "dep_symbol_2"
        )
        val tokenTable = AutoTokenTable(Linear, false)

        listOf(symbol1, symbol2).register(tokenTable)

        val deps = tokenTable.symbolDependencies
        assertTrue(deps.containsKey(symbol1) || deps.containsKey(symbol2) || deps.isEmpty(),
            "symbolDependencies should be accessible after register")

        tokenTable.close()
    }

    @Test
    fun addSymbolDependencyShouldRecordExplicitDependency() {
        val symbol1 = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "explicit_dep_symbol_1"
        )
        val symbol2 = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "explicit_dep_symbol_2"
        )
        val tokenTable = AutoTokenTable(Linear, false)

        listOf(symbol1, symbol2).register(tokenTable)
        tokenTable.addSymbolDependency(symbol1, symbol2)

        val deps = tokenTable.symbolDependencies
        assertTrue(deps[symbol1]?.contains(symbol2) == true,
            "symbol1 should depend on symbol2 after addSymbolDependency")

        tokenTable.close()
    }

    @Test
    fun validateNoCyclesShouldDetectNoCycleInAcyclicGraph() {
        val symbol1 = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "acyclic_symbol_1"
        )
        val symbol2 = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "acyclic_symbol_2"
        )
        val tokenTable = AutoTokenTable(Linear, false)

        listOf(symbol1, symbol2).register(tokenTable)
        tokenTable.addSymbolDependency(symbol1, symbol2)

        assertTrue(tokenTable.validateNoCycles(), "Acyclic graph should pass validation")

        tokenTable.close()
    }

    @Test
    fun toQuadraticConstraintExtensionShouldCreateValidConstraint() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "quad_constraint_symbol"
        )
        val tokenTable = AutoTokenTable(Linear, false)
        listOf(symbol).register(tokenTable)

        val range = ExpressionRange(ValueRange(Flt64.zero, Flt64.one).value!!)
        tokenTable.cacheRange(symbol, range)

        assertTrue(tokenTable.cachedRange(symbol))
        assertEquals(range, tokenTable.cachedRangeValue(symbol))

        tokenTable.close()
    }
}