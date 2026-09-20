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
import fuookami.ospf.kotlin.core.variable.IntVar

class GurobiDeferredIfIT {
    @Test
    fun normalizedRelationsPreserveBoundsAndFallback() = runBlocking {
        verifyModes(listOf("eager", "native", "failed", "narrow"))
    }

    @Test
    fun publicEquivalentColumnCanBeUsedInObjectiveAndConstraint() = runBlocking {
        verifyModes(listOf("alias"))
    }

    @Test
    fun constantBranchesKeepBothPublicResults() = runBlocking {
        verifyModes(listOf("folded"))
    }

    private suspend fun verifyModes(modes: List<String>) {
        for (sign in listOf(Comparison.LT, Comparison.GT, Comparison.LE, Comparison.GE)) {
            for (value in listOf(-1.0, 0.0, 1.0, 2.0, 3.0)) {
                for (mode in modes) {
                    val input = IntVar("relation_input")
                    input.range.geq(Int64(-2))
                    input.range.leq(Int64(3))
                    if (mode == "folded") {
                        input.range.geq(Int64(value.toLong()))
                        input.range.leq(Int64(value.toLong()))
                    }
                    val function = IfFunction(
                        condition = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64(-1.0)),
                        relation = sign,
                        converter = IntoValue.Identity,
                        conditionBounds = if (mode == "narrow") ConditionBounds(Flt64(-1.0), Flt64.one) else null,
                        strictBoundary = Flt64.one,
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
                            rhs = LinearPolynomial(emptyList(), Flt64(value)),
                            comparison = Comparison.EQ,
                            name = "fixed_input"
                        ), name = "fixed_input"))
                        val aliasPolynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.indicatorVar)), Flt64.zero)
                        if (mode == "alias") {
                            requireOk(meta.addConstraint(relation = LinearInequality(
                                lhs = function.resultPolynomial,
                                rhs = LinearPolynomial(emptyList(), function.evaluate(mapOf(input to Flt64(value)))!!),
                                comparison = Comparison.EQ,
                                name = "public_result"
                            ), name = "public_result"))
                        }
                        requireOk(meta.maximize(if (mode == "alias") aliasPolynomial else function.resultPolynomial))
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
                            val expected = function.evaluate(mapOf(input to Flt64(value)))
                            val context = "$sign input=$value mode=$mode"
                            if (expected == null || mode == "narrow" && (value < 0.0 || value > 2.0)) {
                                assertEquals(ProblemStatus.Infeasible, requireOk(outcome).problemStatus, context)
                            } else {
                                val report = requireOk(outcome)
                                val resultIndex = mechanism.tokens.tokensInSolver.indexOfFirst { it.key == function.resultVar.key }
                                assertEquals(expected.toDouble(), report.values[resultIndex].toDouble(), 1e-8, context)
                                val aliasIndex = mechanism.tokens.tokensInSolver.indexOfFirst { it.key == function.indicatorVar.key }
                                assertEquals(expected.toDouble(), report.values[aliasIndex].toDouble(), 1e-8, context)
                                if (mode == "native" || mode == "alias") {
                                    val residual = report.diagnostics.constraintEvaluations.single { it.constraintId.toString().contains("indicator:") }
                                    val equivalent = report.diagnostics.constraintEvaluations.single { it.constraintId.toString().contains("indicator-equivalence:") }
                                    assertTrue(equivalent.satisfied, context)
                                    assertEquals(0.0, equivalent.violation.toDouble(), 1e-8, context)
                                    assertTrue(residual.satisfied, context)
                                    assertEquals(0.0, residual.violation.toDouble(), 1e-8, context)
                                    assertTrue(report.fingerprints.model!!.schemaVersion.endsWith("indicator-3"))
                                }
                            }
                            val expectedShape = when (mode) {
                                "native" -> 2 to 2
                                "alias" -> 3 to 2
                                "folded" -> 3 to 0
                                else -> 4 to 0
                            }
                            assertEquals(listOf(expectedShape), shapes, context)
                            if (mode == "folded") assertNull(function.deferredStructure())
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
