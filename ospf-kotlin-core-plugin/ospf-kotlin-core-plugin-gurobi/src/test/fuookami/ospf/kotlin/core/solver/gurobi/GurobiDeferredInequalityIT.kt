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
import fuookami.ospf.kotlin.core.variable.RealVar

class GurobiDeferredInequalityIT {
    @Test
    fun orderedComparisonsPreserveGapAndFallback() = runBlocking {
        for (sign in listOf(Comparison.LT, Comparison.GT, Comparison.LE, Comparison.GE)) {
            for (value in listOf(-0.5, -0.05, 0.0, 0.05, 0.5)) {
                for (mode in listOf("eager", "native", "failed", "smallM")) {
                    val input = RealVar("relation_input")
                    input.range.geq(Flt64(-1.0))
                    input.range.leq(Flt64(3.0))
                    val function = InequalityFunction(
                        lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64(3.0)),
                        rhs = Flt64(4.0),
                        sign = sign,
                        converter = IntoValue.Identity,
                        bigM = Flt64(if (mode == "smallM") 1.0 else 2.0),
                        strictBoundary = Flt64(0.1),
                        tolerance = Flt64(0.1),
                        name = "relation"
                    )
                    val policy = if (mode == "eager") FunctionExpansionPolicy.EAGER else FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
                    val meta = LinearMetaModel<Flt64>(
                        name = "strict_relation",
                        configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = policy),
                        converter = IntoValue.Identity
                    )
                    try {
                        requireOk(meta.add(input))
                        requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
                        requireOk(meta.addConstraint(relation = LinearInequality(
                            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
                            rhs = LinearPolynomial(emptyList(), Flt64(value + 1.0)),
                            comparison = Comparison.EQ,
                            name = "fixed_input"
                        ), name = "fixed_input"))
                        requireOk(meta.maximize(function.result))
                        requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false)).use { mechanism ->
                            val shapes = mutableListOf<Pair<Int, Int>>()
                            val solver = GurobiLinearSolver(
                                config = SolverConfig(threadNum = UInt64.one, dumpIntermediateModelBounds = false, functionExpansionPolicy = policy),
                                callBack = GurobiLinearSolverCallBack().afterModeling { _, model, _, _ ->
                                    model.update()
                                    shapes += model.get(GRB.IntAttr.NumConstrs) to model.get(GRB.IntAttr.NumGenConstrs)
                                    ok
                                }
                            )
                            var writes = 0
                            if (mode == "failed") {
                                solver.nativeIndicatorWriter = { model, variables, structures ->
                                    writes++
                                    requireOk(addGurobiNativeIndicator(model, variables, structures))
                                    Failed(ErrorCode.OREngineModelingException, "injected failure after native write")
                                }
                            }
                            val outcome = solver.solve(mechanism, IntoValue.Identity)
                            val expected = function.evaluate(mapOf(input to Flt64(value + 1.0)))
                            val context = "$sign input=$value mode=$mode"
                            if (expected == null) {
                                assertEquals(ProblemStatus.Infeasible, requireOk(outcome).problemStatus, context)
                            } else {
                                val report = requireOk(outcome)
                                val resultIndex = mechanism.tokens.tokensInSolver.indexOfFirst { it.key == function.helperVariables.single().key }
                                assertEquals(expected.toDouble(), report.values[resultIndex].toDouble(), 1e-8, context)
                                if (mode == "native") {
                                    val residual = report.diagnostics.constraintEvaluations.single { it.constraintId.toString().contains("indicator:") }
                                    assertTrue(residual.satisfied, context)
                                    assertEquals(0.0, residual.violation.toDouble(), 1e-8, context)
                                    assertTrue(report.fingerprints.model!!.schemaVersion.endsWith("indicator-2"))
                                }
                            }
                            assertEquals(listOf(if (mode == "native") 1 to 2 else 3 to 0), shapes, context)
                            assertEquals(if (mode == "failed") 1 else 0, writes, context)
                        }
                    } finally {
                        meta.close()
                    }
                }
            }
        }
    }

    private fun <T> requireOk(result: Ret<T>): T = when (result) {
        is Ok -> result.value
        is Failed -> fail(result.error.message)
        is Fatal -> fail(result.errors.joinToString { it.message })
    }
}
