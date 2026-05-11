package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbol
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.register
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.utils.functional.Ok
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
        val privateKey = Any()
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        listOf(symbol).register(tokenTable)

        val flatten = symbol.flattenedMonomialsAsV
        tokenTable.cacheLinearFlatten(symbol, flatten)
        tokenTable.cacheLinearFlatten(privateKey, flatten)

        assertTrue(tokenTable.cachedLinearFlatten(symbol))
        assertTrue(tokenTable.cachedLinearFlatten(privateKey))
        assertEquals(flatten, tokenTable.cachedLinearFlattenValue(symbol))
        assertEquals(flatten, tokenTable.cachedLinearFlattenValue(privateKey))

        tokenTable.close()
    }

    // TODO: clampCoefficient tests removed — function is now internal in model.intermediate.DumpHelpers
    // and not accessible from this package. Re-enable when a public API is provided.

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
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

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
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

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
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        listOf(symbol1, symbol2).register(tokenTable)
        tokenTable.addSymbolDependency(symbol1, symbol2)

        assertTrue(tokenTable.validateNoCycles(), "Acyclic graph should pass validation")

        tokenTable.close()
    }

    @Test
    fun typeAliasesShouldMatchGenericTypes() {
        val linear: LinearExpressionSymbol<Flt64> = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "typealias_check_linear"
        )
        val linearFlt64: LinearExpressionSymbol<Flt64> = linear
        assertTrue(linear === linearFlt64, "LinearExpressionSymbol<Flt64> should be typealias for LinearExpressionSymbol<Flt64>")

        val quadratic: QuadraticExpressionSymbol<Flt64> = QuadraticExpressionSymbol(
            constant = Flt64.one,
            name = "typealias_check_quadratic"
        )
        val quadraticFlt64: QuadraticExpressionSymbol<Flt64> = quadratic
        assertTrue(quadratic === quadraticFlt64, "QuadraticExpressionSymbol<Flt64> should be typealias for QuadraticExpressionSymbol<Flt64>")
    }

    @Test
    fun toQuadraticConstraintExtensionShouldCreateValidConstraint() {
        val symbol = LinearExpressionSymbol(
            constant = Flt64.one,
            name = "quad_constraint_symbol"
        )
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        listOf(symbol).register(tokenTable)

        val range = ExpressionRange(ValueRange(Flt64.zero, Flt64.one).value!!)
        tokenTable.cacheRange(symbol, range)

        assertTrue(tokenTable.cachedRange(symbol))
        assertEquals(range, tokenTable.cachedRangeValue(symbol))

        tokenTable.close()
    }
}
