package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import gurobi.GRB
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.*

class GurobiDeferredIfInIT {
    @Test
    fun intervalEndpointsGapsAndPublicColumnsPreserveFallback() = runBlocking {
        for (value in listOf(-1.0, -0.25, -0.125, 0.0, 0.5, 1.0, 1.125, 1.25, 2.0)) {
            for (mode in listOf("eager", "native", "failed", "narrow", "reference")) verify(value, mode)
        }
    }

    @Test
    fun foldedIntervalsRetainOriginalRows() = runBlocking {
        for (value in listOf(-1.0, 0.0, 0.5, 1.0, 2.0)) verify(value, "folded")
    }

    private suspend fun verify(value: Double, mode: String) {
        val input = RealVar("interval_input")
        input.range.geq(Flt64(if (mode == "folded") value else -1.0))
        input.range.leq(Flt64(if (mode == "folded") value else 2.0))
        val function = IfInFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            lower = Flt64.zero,
            upper = Flt64.one,
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.25),
            conditionBounds = if (mode == "narrow") ConditionBounds(Flt64(-0.5), Flt64(1.5)) else null,
            name = "interval"
        )
        val policy = if (mode == "eager") FunctionExpansionPolicy.EAGER else FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
        val meta = LinearMetaModel<Flt64>(
            name = "ifin_native",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = policy),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
            val flags = listOf(
                function.geVar to if (value >= 0.0) 1.0 else 0.0,
                function.leVar to if (value <= 1.0) 1.0 else 0.0,
                function.resultVar to if (value in 0.0..1.0) 1.0 else 0.0
            )
            for ((variable, fixed) in listOf(input to value) + if (mode == "reference") flags else emptyList()) {
                requireOk(meta.addConstraint(relation = LinearInequality(
                    lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero),
                    rhs = LinearPolynomial(emptyList(), Flt64(fixed)),
                    comparison = Comparison.EQ,
                    name = "fix_${variable.name}"
                ), name = "fix_${variable.name}"))
            }
            if (mode == "reference") requireOk(meta.minimize(LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.leVar)), Flt64.zero)))
            else requireOk(meta.maximize(function.resultPolynomial))
            requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                val shapes = mutableListOf<Triple<Int, Int, Int>>()
                val solver = GurobiLinearSolver(
                    config = SolverConfig(threadNum = UInt64.one, dumpIntermediateModelBounds = false, functionExpansionPolicy = policy),
                    callBack = GurobiLinearSolverCallBack().afterModeling { _, model, _, _ ->
                        model.update()
                        shapes += Triple(model.get(GRB.IntAttr.NumVars), model.get(GRB.IntAttr.NumConstrs), model.get(GRB.IntAttr.NumGenConstrs))
                        ok
                    }
                )
                var writes = 0
                if (mode == "failed") solver.nativeIndicatorWriter = { model, variables, structures ->
                    writes++
                    requireOk(addGurobiNativeIndicator(model, variables, structures))
                    Failed(ErrorCode.OREngineModelingException, "injected failure after native write")
                }
                val report = requireOk(solver.solve(mechanism, IntoValue.Identity))
                val expected = function.evaluate(mapOf(input to Flt64(value)))
                val context = "input=$value mode=$mode"
                if (expected == null || mode == "narrow" && (value < -0.5 || value > 1.5)) {
                    assertEquals(ProblemStatus.Infeasible, report.problemStatus, context)
                } else {
                    val tokens = mechanism.tokens.tokensInSolver
                    for ((variable, flag) in flags) assertEquals(flag, report.values[tokens.indexOfFirst { it.key == variable.key }].toDouble(), 1e-8, context)
                    if (mode == "native" || mode == "reference") {
                        assertTrue(report.fingerprints.model!!.schemaVersion.endsWith("indicator-7"))
                        assertEquals(2, report.diagnostics.constraintEvaluations.count { it.constraintId.toString().contains("indicator:") })
                        assertTrue(report.diagnostics.constraintEvaluations.single { it.constraintId.toString().contains("conjunction:") }.satisfied, context)
                    }
                }
                val shape = when (mode) {
                    "native" -> Triple(4, 4, 4)
                    "reference" -> Triple(4, 7, 4)
                    "folded" -> Triple(4, 4, 0)
                    else -> Triple(4, 8, 0)
                }
                assertEquals(listOf(shape), shapes, context)
                assertEquals(if (mode == "failed") 1 else 0, writes, context)
                if (mode == "folded") assertNull(function.deferredStructure())
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
