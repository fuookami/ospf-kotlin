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

class GurobiDeferredEqualityIT {
    @Test
    fun bothPolaritiesRetainBandGapAndPublicSide() = runBlocking {
        for (sign in listOf(Comparison.EQ, Comparison.NE)) {
            for (value in listOf(-2.0, -0.5, -0.25, -0.125, -0.0625, 0.0, 0.0625, 0.125, 0.25, 0.5, 2.0)) {
                for (flag in listOf(0.0, 1.0)) {
                    for (side in listOf(0.0, 1.0)) {
                        for (mode in listOf("eager", "native", "failed", "smallM")) {
                            verify(sign, value, flag, side, 0.125, mode)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun zeroWidthBandAndSdkBoundsFailurePreserveFallback() = runBlocking {
        for (sign in listOf(Comparison.EQ, Comparison.NE)) {
            for (value in listOf(-0.5, 0.0, 0.5)) {
                for (flag in listOf(0.0, 1.0)) {
                    for (side in listOf(0.0, 1.0)) {
                        for (mode in listOf("eager", "native", "sdk")) {
                            verify(sign, value, flag, side, 0.0, mode)
                        }
                    }
                }
            }
        }
    }

    private suspend fun verify(
        sign: Comparison,
        value: Double,
        flag: Double,
        side: Double,
        band: Double,
        mode: String
    ) {
        val input = RealVar("equality_input")
        input.range.geq(Flt64(-1.0))
        input.range.leq(Flt64(3.0))
        val bigM = if (mode == "smallM") 0.75 else 2.0
        val boundary = 0.5
        val function = InequalityFunction(
            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64(3.0)),
            rhs = Flt64(4.0),
            sign = sign,
            converter = IntoValue.Identity,
            bigM = Flt64(bigM),
            tolerance = Flt64(band),
            strictBoundary = Flt64(boundary),
            name = "equality"
        )
        val flagVariable = function.helperVariables[0]
        val sideVariable = function.helperVariables[1]
        fun polynomial(variable: AbstractVariableItem<*, *>) =
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero)
        val policy = if (mode == "eager") FunctionExpansionPolicy.EAGER else FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
        val meta = LinearMetaModel<Flt64>(
            name = "equality_native",
            configuration = MetaModelConfiguration(concurrent = false, dumpBlocking = true, functionExpansionPolicy = policy),
            converter = IntoValue.Identity
        )
        try {
            requireOk(meta.add(input))
            requireOk(meta.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)))
            for ((variable, fixed) in listOf(input to value + 1.0, flagVariable to flag, sideVariable to side)) {
                requireOk(meta.addConstraint(relation = LinearInequality(
                    lhs = polynomial(variable),
                    rhs = LinearPolynomial(emptyList(), Flt64(fixed)),
                    comparison = Comparison.EQ,
                    name = "fix_${variable.name}"
                ), name = "fix_${variable.name}"))
            }
            requireOk(if (side == 0.0) meta.minimize(function.result) else meta.maximize(polynomial(sideVariable)))
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
                if (mode == "failed" || mode == "sdk") {
                    solver.nativeIndicatorWriter = { model, variables, structures ->
                        writes++
                        if (mode == "sdk") {
                            variables.getValue(input.key).set(GRB.DoubleAttr.UB, 4.0)
                            val result = addGurobiNativeIndicator(model, variables, structures)
                            assertTrue(result is Failed)
                            result
                        } else {
                            requireOk(addGurobiNativeIndicator(model, variables, structures))
                            Failed(ErrorCode.OREngineModelingException, "injected failure after native write")
                        }
                    }
                }
                val outside = if (sign == Comparison.NE) flag else 1.0 - flag
                val bandM = bigM + band
                val outM = bigM + boundary
                val feasible = kotlin.math.abs(value) <= band + bandM * outside &&
                    value - outM * outside - outM * side >= boundary - 2.0 * outM &&
                    value + outM * outside - outM * side <= -boundary + outM
                val report = requireOk(solver.solve(mechanism, IntoValue.Identity))
                val context = "$sign value=$value flag=$flag side=$side band=$band mode=$mode"
                if (!feasible) assertEquals(ProblemStatus.Infeasible, report.problemStatus, context)
                else {
                    val tokens = mechanism.tokens.tokensInSolver
                    assertEquals(flag, report.values[tokens.indexOfFirst { it.key == flagVariable.key }].toDouble(), 1e-8, context)
                    assertEquals(side, report.values[tokens.indexOfFirst { it.key == sideVariable.key }].toDouble(), 1e-8, context)
                    if (mode == "native") {
                        val residual = report.diagnostics.constraintEvaluations.single { it.constraintId.toString().contains("zero-band:") }
                        assertTrue(residual.satisfied, context)
                        assertEquals(0.0, residual.violation.toDouble(), 1e-8, context)
                        assertTrue(report.fingerprints.model!!.schemaVersion.endsWith("indicator-6"))
                    }
                }
                assertEquals(listOf(if (mode == "native") Triple(3, 3, 4) else Triple(3, 7, 0)), shapes, context)
                assertEquals(if (mode == "failed" || mode == "sdk") 1 else 0, writes, context)
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
