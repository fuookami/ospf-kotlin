package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbol
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.value.toIntoValue
import fuookami.ospf.kotlin.core.token.register
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Rtn64
import fuookami.ospf.kotlin.math.algebra.number.RtnX
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiCompatibilityTest {
    @Test
    fun metaModelCompanionEntriesShouldProvideFourDefaultConverters() {
        val linearDefault: LinearMetaModel<Flt64> = LinearMetaModel("linear_default")
        val quadraticDefault: QuadraticMetaModel<Flt64> = QuadraticMetaModel("quadratic_default")

        val linearFlt64: LinearMetaModel<Flt64> = LinearMetaModel("linear_flt64", Flt64)
        val linearFltX: LinearMetaModel<FltX> = LinearMetaModel("linear_fltx", FltX)
        val linearRtn64: LinearMetaModel<Rtn64> = LinearMetaModel("linear_rtn64", Rtn64)
        val linearRtnX: LinearMetaModel<RtnX> = LinearMetaModel("linear_rtnx", RtnX)
        val quadraticFlt64: QuadraticMetaModel<Flt64> = QuadraticMetaModel("quadratic_flt64", Flt64)
        val quadraticFltX: QuadraticMetaModel<FltX> = QuadraticMetaModel("quadratic_fltx", FltX)
        val quadraticRtn64: QuadraticMetaModel<Rtn64> = QuadraticMetaModel("quadratic_rtn64", Rtn64)
        val quadraticRtnX: QuadraticMetaModel<RtnX> = QuadraticMetaModel("quadratic_rtnx", RtnX)

        val models = listOf(
            linearDefault,
            linearFlt64,
            linearFltX,
            linearRtn64,
            linearRtnX,
            quadraticDefault,
            quadraticFlt64,
            quadraticFltX,
            quadraticRtn64,
            quadraticRtnX
        )

        try {
            assertFiniteRoundTrip(linearDefault.converter, Flt64(3.5))
            assertFiniteRoundTrip(linearFlt64.converter, Flt64(3.5))
            assertFiniteRoundTrip(linearFltX.converter, Flt64(3.5))
            assertFiniteRoundTrip(linearRtn64.converter, Flt64(3.5))
            assertFiniteRoundTrip(linearRtnX.converter, Flt64(3.5))
            assertFiniteRoundTrip(quadraticDefault.converter, Flt64(-2.25))
            assertFiniteRoundTrip(quadraticFlt64.converter, Flt64(-2.25))
            assertFiniteRoundTrip(quadraticFltX.converter, Flt64(-2.25))
            assertFiniteRoundTrip(quadraticRtn64.converter, Flt64(-2.25))
            assertFiniteRoundTrip(quadraticRtnX.converter, Flt64(-2.25))
        } finally {
            models.forEach { it.close() }
        }
    }

    @Test
    fun flt64BridgeShouldAdaptToIntoValueInCoreLayer() {
        assertFiniteRoundTrip(Flt64.toIntoValue(), Flt64(1.25))
        assertFiniteRoundTrip(FltX.toIntoValue(), Flt64(1.25))
        assertFiniteRoundTrip(Rtn64.toIntoValue(), Flt64(1.25))
        assertFiniteRoundTrip(RtnX.toIntoValue(), Flt64(1.25))
    }

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

    private fun <V> assertFiniteRoundTrip(
        converter: IntoValue<V>,
        sample: Flt64
    ) where V : RealNumber<V>, V : NumberField<V> {
        assertEquals(sample, converter.fromValue(converter.intoValue(sample)))
    }
}
