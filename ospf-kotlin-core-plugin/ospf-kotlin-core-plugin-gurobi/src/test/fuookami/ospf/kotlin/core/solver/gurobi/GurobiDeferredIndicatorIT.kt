package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import gurobi.GRB
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.RealVar

class GurobiDeferredIndicatorIT {
    @Test
    fun nativeMatchesEagerAndFailedWriterRebuildsFallback() = runBlocking {
        for (inputValue in listOf(-1.0, 0.0, 1.0)) {
            for (mode in listOf("eager", "native", "failed", "smallM")) {
                val input = RealVar("indicator_input")
                input.range.geq(Flt64(inputValue))
                input.range.leq(Flt64(inputValue))
                val function = BinaryzationFunction(
                    polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
                    converter = IntoValue.Identity,
                    bigM = Flt64(if (mode == "smallM") 0.5 else 2.0),
                    name = "indicator"
                )
                if (mode == "smallM" && inputValue != 0.0) continue
                val policy = if (mode == "eager") FunctionExpansionPolicy.EAGER else FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
                val meta = LinearMetaModel<Flt64>(
                    name = "indicator_it",
                    configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = policy),
                    converter = IntoValue.Identity
                )
                try {
                    requireOk(meta.add(input))
                    requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
                    requireOk(meta.minimize(function.resultPolynomial))
                    val mechanism = requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false))
                    mechanism.use {
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
                        val report = requireOk(solver.solve(mechanism, IntoValue.Identity))
                        val resultIndex = mechanism.tokens.tokensInSolver.indexOfFirst { it.key == function.resultVar.key }
                        assertEquals(if (inputValue > 0) 1.0 else 0.0, report.values[resultIndex].toDouble(), 1e-8)
                        assertEquals(listOf(if (mode == "eager" || mode == "failed") 2 to 0 else 0 to 2), shapes, mode)
                        assertEquals(if (mode == "failed") 1 else 0, writes)
                        if (mode == "native") {
                            assertTrue(report.fingerprints.model!!.schemaVersion.contains("indicator"))
                            assertTrue(report.diagnostics.constraintEvaluations.any { it.constraintId.toString().contains("indicator") && it.satisfied })
                        }
                    }
                } finally {
                    meta.close()
                }
            }
        }
    }

    @Test
    fun nativePreservesMaximumConstraintMixedAndNestedUses() = runBlocking {
        for (usage in listOf("maximum", "constraint", "mixed", "nested", "smallM")) {
            for (policy in listOf(FunctionExpansionPolicy.EAGER, FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST)) {
                val input = RealVar("usage_input")
                input.range.geq(Flt64(-2.0))
                input.range.leq(Flt64(2.0))
                val inputPolynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero)
                val function = BinaryzationFunction(
                    polynomial = inputPolynomial,
                    converter = IntoValue.Identity,
                    bigM = Flt64(if (usage == "smallM") 1.0 else 2.0),
                    name = "usage_indicator",
                    tolerance = Flt64(1e-4)
                )
                val nested = if (usage == "nested") BinaryzationFunction(
                    polynomial = function.resultPolynomial,
                    converter = IntoValue.Identity,
                    bigM = Flt64.one,
                    name = "nested_indicator",
                    tolerance = Flt64(1e-4)
                ) else null
                val meta = LinearMetaModel<Flt64>(
                    name = "indicator_usage",
                    configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = policy),
                    converter = IntoValue.Identity
                )
                try {
                    requireOk(meta.add(input))
                    requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
                    if (nested != null) requireOk(meta.add(LinearFunctionSymbolAdapter(nested, IntoValue.Identity)))
                    if (usage in listOf("constraint", "mixed", "smallM")) {
                        val relation = fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality(
                            lhs = if (usage == "smallM") inputPolynomial else function.resultPolynomial,
                            rhs = LinearPolynomial(emptyList(), Flt64(if (usage == "smallM") 0.5 else 1.0)),
                            comparison = fuookami.ospf.kotlin.math.symbol.inequality.Comparison.EQ,
                            name = "external_reference"
                        )
                        requireOk(meta.addConstraint(relation = relation, name = relation.name))
                    }
                    val objective = if (usage == "constraint") LinearPolynomial(emptyList(), Flt64.zero)
                        else nested?.resultPolynomial ?: function.resultPolynomial
                    requireOk(meta.maximize(objective))
                    val mechanism = requireOk(LinearMechanismModel.invoke(metaModel = meta, concurrent = false))
                    mechanism.use {
                        val shapes = mutableListOf<Pair<Int, Int>>()
                        val solver = GurobiLinearSolver(
                            config = SolverConfig(threadNum = UInt64.one, dumpIntermediateModelBounds = false, functionExpansionPolicy = policy),
                            callBack = GurobiLinearSolverCallBack().afterModeling { _, model, _, _ ->
                                model.update()
                                shapes += model.get(GRB.IntAttr.NumConstrs) to model.get(GRB.IntAttr.NumGenConstrs)
                                ok
                            }
                        )
                        val report = requireOk(solver.solve(mechanism, IntoValue.Identity))
                        val index = mechanism.tokens.tokensInSolver.indexOfFirst { it.key == function.resultVar.key }
                        assertEquals(1.0, report.values[index].toDouble(), 1e-8, "$usage $policy ${report.values}")
                        if (nested != null) {
                            val nestedIndex = mechanism.tokens.tokensInSolver.indexOfFirst { it.key == nested.resultVar.key }
                            assertEquals(1.0, report.values[nestedIndex].toDouble(), 1e-8)
                        }
                        val count = if (nested == null) 2 else 4
                        val external = if (usage in listOf("constraint", "mixed", "smallM")) 1 else 0
                        val native = policy != FunctionExpansionPolicy.EAGER && usage != "smallM"
                        assertEquals(listOf(if (native) external to count else external + count to 0), shapes, "$usage $policy")
                    }
                } finally {
                    meta.close()
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
