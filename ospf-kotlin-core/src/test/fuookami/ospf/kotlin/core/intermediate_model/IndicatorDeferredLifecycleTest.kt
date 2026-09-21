package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.RealVar

class IndicatorDeferredLifecycleTest {
    @Test
    fun fallbackRetainsBothBranchesAndGap() {
        verifyBranchesAndGap(null)
    }

    @Test
    fun configuredGapIsSharedByEvaluationAndFallback() {
        verifyBranchesAndGap(Flt64(1e-4))
    }

    private fun verifyBranchesAndGap(tolerance: Flt64?) {
        val input = RealVar("indicator_input")
        val function = BinaryzationFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            converter = IntoValue.Identity,
            bigM = Flt64(10.0),
            name = "indicator",
            tolerance = tolerance
        )
        val structure = function.deferredStructure()
        assertEquals(tolerance ?: Flt64(NONZERO_TOLERANCE), structure.tolerance)
        val rows = requireOk(CoreDeferredFunctionFallbackMaterializer.materialize(structure)).constraints
        assertEquals(2, rows.size)
        assertEquals(requireOk(structure.generateConstraints()), rows)
        val epsilon = structure.tolerance.toDouble()
        for (value in listOf(-10.0, -1.0, 0.0, epsilon / 2, epsilon, 1.0, 10.0)) {
            val expected = when {
                value <= 0.0 -> Flt64.zero
                value >= epsilon -> Flt64.one
                else -> null
            }
            assertEquals(expected, function.evaluate(mapOf(input to Flt64(value))))
            for (flag in listOf(Flt64.zero, Flt64.one)) {
                val values = mapOf<Symbol, Flt64>(input to Flt64(value), structure.resultVariable to flag)
                val feasible = rows.all { row ->
                    val left = row.lhs.evaluateWith(values)!!.toDouble()
                    val right = row.rhs.constant.toDouble()
                    if (row.comparison == Comparison.LE) left <= right + 1e-12 else left >= right - 1e-12
                }
                assertEquals(expected == flag, feasible, "input=$value flag=$flag")
            }
        }
        assertTrue(CoreDeferredFunctionFallbackMaterializer.materialize(structure.copy(bigM = Flt64.nan)) is Failed)
        for (invalid in listOf(Flt64.zero, Flt64(-1.0), Flt64.nan, Flt64(Double.POSITIVE_INFINITY))) {
            assertTrue(CoreDeferredFunctionFallbackMaterializer.materialize(structure.copy(tolerance = invalid)) is Failed)
        }
    }

    @Test
    fun deferredPoliciesMaterializeOnceAndPreserveUsage() = runBlocking {
        for (policy in FunctionExpansionPolicy.entries) {
            val input = RealVar("indicator_input")
            input.range.geq(Flt64(-2.0))
            input.range.leq(Flt64(2.0))
            val function = BinaryzationFunction(
                polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
                converter = IntoValue.Identity,
                name = "indicator"
            )
            val meta = LinearMetaModel<Flt64>(
                name = "indicator_lifecycle",
                configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = policy),
                converter = IntoValue.Identity
            )
            try {
                assertTrue(meta.add(input) is Ok)
                assertTrue(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
                assertTrue(meta.minimize(function.resultPolynomial) is Ok)
                val mechanism = requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false))
                mechanism.use {
                    assertEquals(if (policy == FunctionExpansionPolicy.EAGER) 2 else 0, mechanism.constraints.size)
                    repeat(2) {
                        val triad = requireOk(LinearTriadModel.invokeResult(
                            model = mechanism,
                            dumpConstraintsToBounds = false,
                            concurrent = false
                        ))
                        triad.use {
                            assertEquals(2, triad.constraints.size)
                            assertEquals(2, triad.variables.size)
                            val snapshot = assertIs<IndicatorStructure<*>>(triad.deferredFunctionStructures.single())
                            assertTrue(snapshot.usage.inObjective)
                            assertFalse(snapshot.usage.inConstraint)
                            assertEquals(2, triad.deferredFunctionConstraintRegions.single().constraintCount)
                        }
                    }
                }
            } finally {
                meta.close()
            }
        }
    }
    private fun <T> requireOk(result: Ret<T>): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> fail(result.error.message)
            is Fatal -> fail(result.errors.joinToString { it.message })
        }
    }
}
