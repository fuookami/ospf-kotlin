package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

class CompositeCvarFunctionModelTest {
    @Test
    fun clampRegistersItsComposedRowsInLinearMechanismModel() {
        val inputVar = RealVar("composite_clamp_input")
        inputVar.range.geq(Flt64(-4.0))
        inputVar.range.leq(Flt64(5.0))
        val input = variable(inputVar)
        val functionResult = ClampFunction(
            input = input,
            lower = Flt64(-1.0),
            upper = Flt64(2.0),
            converter = IntoValue.Identity,
            name = "composite_clamp"
        )
        assertTrue(functionResult is Ok)
        val function = functionResult.value
        val metaModel = LinearMetaModel<Flt64>(
            name = "composite-clamp-model",
            converter = IntoValue.Identity
        )
        try {
            val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
            assertTrue(metaModel.minimize(zero) is Ok)
            assertTrue(metaModel.add(listOf(inputVar)) is Ok)
            assertTrue(function.registerAuxiliaryTokens(metaModel.tokens) is Ok)

            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            assertTrue(function.registerConstraints(mechanism) is Ok)
            assertEquals(10, mechanism.constraints.size)
            assertEquals(Flt64(-1.0), function.evaluate(mapOf(inputVar to Flt64(-3.0))))
            assertEquals(Flt64(0.5), function.evaluate(mapOf(inputVar to Flt64(0.5))))
            assertEquals(Flt64(2.0), function.evaluate(mapOf(inputVar to Flt64(4.0))))
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun exactAndEpigraphCvarRegisterExpectedRowsAndAgreeOnEvaluation() {
        val losses = listOf(0.0, 2.0, 10.0).map { value ->
            LinearPolynomial<Flt64>(emptyList(), Flt64(value))
        }
        val probabilities = listOf(Flt64(0.2), Flt64(0.5), Flt64(0.3))
        val exactResult = CvarFunction(
            losses = losses,
            probabilities = probabilities,
            alpha = Flt64(0.5),
            converter = IntoValue.Identity,
            name = "exact_cvar"
        )
        val epigraphResult = CvarEpigraphFunction(
            losses = losses,
            probabilities = probabilities,
            alpha = Flt64(0.5),
            converter = IntoValue.Identity,
            name = "epigraph_cvar"
        )
        assertTrue(exactResult is Ok)
        assertTrue(epigraphResult is Ok)
        val exact = exactResult.value
        val epigraph = epigraphResult.value
        val metaModel = LinearMetaModel<Flt64>(
            name = "cvar-model",
            converter = IntoValue.Identity
        )
        try {
            val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
            assertTrue(metaModel.minimize(zero) is Ok)
            assertTrue(exact.registerAuxiliaryTokens(metaModel.tokens) is Ok)
            assertTrue(epigraph.registerAuxiliaryTokens(metaModel.tokens) is Ok)

            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            assertTrue(exact.registerConstraints(mechanism) is Ok)
            assertTrue(epigraph.registerConstraints(mechanism) is Ok)

            assertTrue(mechanism.constraints.isNotEmpty())
            assertEquals(3, mechanism.constraints.count { it.name.startsWith("epigraph_cvar_excess_lb_") })
            assertEquals(6.8, exact.evaluate(emptyMap())!!.toDouble(), 1e-10)
            assertEquals(6.8, epigraph.evaluate(emptyMap())!!.toDouble(), 1e-10)
        } finally {
            metaModel.close()
        }
    }

    private fun variable(variable: RealVar): LinearPolynomial<Flt64> = LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, variable)),
        Flt64.zero
    )
}
