package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.RealVar

class Gurobi11IndicatorPlannerTest {
    @Test
    fun selectsNativeRelationButRejectsMaterializedFallback() = runBlocking {
        val input = RealVar("planner_input")
        input.range.geq(Flt64(-1.0))
        input.range.leq(Flt64.one)
        val function = BinaryzationFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            converter = IntoValue.Identity,
            name = "planner_indicator"
        )
        val meta = LinearMetaModel<Flt64>(
            name = "indicator_planner",
            configuration = MetaModelConfiguration(
                concurrent = false,
                dumpBlocking = true,
                functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
            ),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
            requireOk(meta.maximize(function.resultPolynomial))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                requireOk(LinearTriadModel.invokeResult(
                    model = mechanism,
                    nativeFunctionKeys = setOf(function.resultVar.key),
                    concurrent = false
                )).use { native ->
                    assertTrue(planGurobiFunctionLowering(native).single().decision.useNative)
                    val unavailable = FunctionSolverCapabilities("gurobi11")
                    assertFalse(planGurobiFunctionLowering(native, unavailable).single().decision.useNative)
                }
                requireOk(LinearTriadModel.invokeResult(model = mechanism, concurrent = false)).use { fallback ->
                    assertFalse(planGurobiFunctionLowering(fallback).single().decision.useNative)
                }
            }
        } finally {
            meta.close()
        }
    }

    private fun <T> requireOk(result: Ret<T>): T = when (result) {
        is Ok -> result.value
        is Failed -> fail(result.error.message)
        is Fatal -> fail(result.errors.joinToString { it.message })
    }
}
