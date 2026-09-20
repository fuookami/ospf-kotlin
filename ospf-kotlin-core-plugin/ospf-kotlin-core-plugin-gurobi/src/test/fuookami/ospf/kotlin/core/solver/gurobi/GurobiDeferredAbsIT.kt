package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import gurobi.GRB
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.plus
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.AbsFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar

/** Gurobi ABS 原生单解集成测试。 / Gurobi native ABS single-solve integration tests. */
class GurobiDeferredAbsIT {
    @Test
    fun nativeAbsPreservesPositiveZeroAndNegativeValues() = runBlocking {
        for (policy in listOf(
            FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            FunctionExpansionPolicy.AUTO
        )) {
            for ((point, expected) in listOf(
                Flt64(1.5) to Flt64(1.5),
                Flt64.zero to Flt64.zero,
                Flt64(-1.5) to Flt64(1.5)
            )) {
                val scenario = buildAbsScenario(
                    policy = policy,
                    point = point,
                    objective = Objective.Minimum
                )
                try {
                    val shapes = ArrayList<NativeModelShape>()
                    val report = requireAvailable(
                        GurobiLinearSolver(
                            config = solverConfig(policy),
                            callBack = afterModelingCallback(shapes)
                        ).solve(scenario.mechanism, IntoValue.Identity),
                        "Gurobi native ABS at $point with $policy"
                    )

                    assertEquals(1, shapes.size)
                    assertEquals(NativeModelShape(numVars = 2, numConstrs = 1, numGenConstrs = 1), shapes.single())
                    assertEquals(point, valueAt(report, scenario.mechanism, scenario.input))
                    assertEquals(expected, valueAt(report, scenario.mechanism, scenario.function.resultVar))
                } finally {
                    scenario.close()
                }
            }
        }
    }

    @Test
    fun maximizingResultWithFixedZeroStillUsesExactAbsGraph() = runBlocking {
        val scenario = buildAbsScenario(
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = Flt64.zero,
            objective = Objective.Maximum
        )
        try {
            val shapes = ArrayList<NativeModelShape>()
            val report = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(shapes)
                ).solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi native ABS maximum objective"
            )

            assertEquals(1, shapes.single().numGenConstrs)
            assertEquals(Flt64.zero, valueAt(report, scenario.mechanism, scenario.function.resultVar))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun constraintOnlyAndSimultaneousReferencesKeepNativeRelation() = runBlocking {
        val constraintOnly = buildAbsScenario(
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = Flt64(-1.5),
            objective = Objective.Constant,
            resultConstraint = true
        )
        val simultaneous = buildAbsScenario(
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = Flt64(-1.5),
            objective = Objective.Minimum,
            resultConstraint = true
        )
        try {
            val constraintOnlyShapes = ArrayList<NativeModelShape>()
            val simultaneousShapes = ArrayList<NativeModelShape>()
            val constraintOnlyReport = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(constraintOnlyShapes)
                )
                    .solve(constraintOnly.mechanism, IntoValue.Identity),
                "Gurobi native ABS constraint-only"
            )
            val simultaneousReport = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(simultaneousShapes)
                )
                    .solve(simultaneous.mechanism, IntoValue.Identity),
                "Gurobi native ABS simultaneous reference"
            )

            assertEquals(1, constraintOnlyShapes.single().numGenConstrs)
            assertEquals(1, simultaneousShapes.single().numGenConstrs)
            assertEquals(Flt64(1.5), valueAt(constraintOnlyReport, constraintOnly.mechanism, constraintOnly.function.resultVar))
            assertEquals(Flt64(1.5), valueAt(simultaneousReport, simultaneous.mechanism, simultaneous.function.resultVar))
        } finally {
            constraintOnly.close()
            simultaneous.close()
        }
    }

    @Test
    fun smallMAndExternalHelperUseLinearFallback() = runBlocking {
        val smallM = buildAbsScenario(
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = Flt64.zero,
            objective = Objective.Minimum,
            bigM = Flt64.one
        )
        val externalHelper = buildAbsScenario(
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = Flt64(-1.0),
            objective = Objective.Minimum,
            externalHelper = true
        )
        val smallMInfeasible = buildAbsScenario(
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = Flt64(1.5),
            objective = Objective.Minimum,
            bigM = Flt64.one
        )
        try {
            val smallMShape = ArrayList<NativeModelShape>()
            val externalHelperShape = ArrayList<NativeModelShape>()
            val smallMReport = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(smallMShape)
                ).solve(smallM.mechanism, IntoValue.Identity),
                "Gurobi ABS small-M fallback"
            )
            val externalHelperReport = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(externalHelperShape)
                ).solve(externalHelper.mechanism, IntoValue.Identity),
                "Gurobi ABS external-helper fallback"
            )
            val smallMInfeasibleReport = requireAvailable(
                GurobiLinearSolver(config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST))
                    .solve(smallMInfeasible.mechanism, IntoValue.Identity),
                "Gurobi ABS small-M infeasible fallback"
            )

            assertEquals(0, smallMShape.single().numGenConstrs)
            assertEquals(0, externalHelperShape.single().numGenConstrs)
            assertEquals(Flt64.zero, valueAt(smallMReport, smallM.mechanism, smallM.function.resultVar))
            assertEquals(Flt64.one, valueAt(externalHelperReport, externalHelper.mechanism, externalHelper.function.resultVar))
            assertEquals(ProblemStatus.Infeasible, smallMInfeasibleReport.problemStatus)
        } finally {
            smallM.close()
            externalHelper.close()
            smallMInfeasible.close()
        }
    }

    @Test
    fun mixedPiecewiseAndAbsUseBothNativeRelations() = runBlocking {
        val scenario = buildMixedScenario()
        try {
            val shapes = ArrayList<NativeModelShape>()
            val report = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(shapes)
                ).solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi mixed native PWL and ABS"
            )

            assertEquals(NativeModelShape(numVars = 4, numConstrs = 2, numGenConstrs = 2), shapes.single())
            assertEquals(Flt64(-1.5), valueAt(report, scenario.mechanism, scenario.absInput))
            assertEquals(Flt64(1.5), valueAt(report, scenario.mechanism, scenario.absFunction.resultVar))
            assertEquals(Flt64.two, valueAt(report, scenario.mechanism, scenario.pwlFunction.resultVar))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun absWriterFailureDisposesNativeAttemptAndRebuildsFallback() = runBlocking {
        val scenario = buildAbsScenario(
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = Flt64(-1.5),
            objective = Objective.Minimum
        )
        try {
            val shapes = ArrayList<NativeModelShape>()
            var writerCalls = 0
            val solver = GurobiLinearSolver(
                config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                callBack = afterModelingCallback(shapes)
            )
            solver.nativeAbsWriter = { model, variables, structures ->
                writerCalls++
                val nativeWrite = addGurobiNativeAbs(model, variables, structures)
                assertTrue(nativeWrite is Ok)
                Failed(ErrorCode.Other, "intentional native ABS failure / intentional native ABS failure")
            }

            val report = requireAvailable(
                solver.solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi fallback after native ABS writer failure"
            )

            assertEquals(1, writerCalls)
            assertEquals(1, shapes.size)
            assertEquals(0, shapes.single().numGenConstrs)
            assertEquals(Flt64(1.5), valueAt(report, scenario.mechanism, scenario.function.resultVar))
        } finally {
            scenario.close()
        }
    }

    private suspend fun buildAbsScenario(
        policy: FunctionExpansionPolicy,
        point: Flt64,
        objective: Objective,
        resultConstraint: Boolean = false,
        bigM: Flt64 = Flt64(2.0),
        externalHelper: Boolean = false
    ): AbsScenario {
        val input = RealVar("gurobi_abs_x_${point.toDouble()}_${policy.name}_${objective.name}")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(2.0))
        val function = AbsFunction(
            polynomial = LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, input)),
                Flt64.zero
            ),
            converter = IntoValue.Identity,
            bigM = bigM,
            name = "gurobi_abs_${point.toDouble()}_${objective.name}"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "gurobi-abs-it-${point.toDouble()}-${objective.name}",
            configuration = MetaModelConfiguration(functionExpansionPolicy = policy),
            converter = IntoValue.Identity
        )
        return try {
            assertTrue(metaModel.add(input) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
            assertTrue(metaModel.addConstraint(
                relation = LinearInequality(
                    lhs = function.polynomial,
                    rhs = LinearPolynomial(emptyList(), point),
                    comparison = Comparison.EQ,
                    name = "gurobi_abs_point"
                ),
                name = "gurobi_abs_point"
            ) is Ok)
            if (resultConstraint) {
                assertTrue(metaModel.addConstraint(
                    relation = LinearInequality(
                        lhs = function.resultPolynomial,
                        rhs = LinearPolynomial(emptyList(), Flt64(2.0)),
                        comparison = Comparison.LE,
                        name = "gurobi_abs_result_reference"
                    ),
                    name = "gurobi_abs_result_reference"
                ) is Ok)
            }
            if (externalHelper) {
                assertTrue(metaModel.addConstraint(
                    relation = LinearInequality(
                        lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.posVar)), Flt64.zero),
                        rhs = LinearPolynomial(emptyList(), Flt64.one),
                        comparison = Comparison.LE,
                        name = "gurobi_abs_external_helper"
                    ),
                    name = "gurobi_abs_external_helper"
                ) is Ok)
            }
            when (objective) {
                Objective.Minimum -> assertTrue(metaModel.minimize(function.resultPolynomial) is Ok)
                Objective.Maximum -> assertTrue(metaModel.maximize(function.resultPolynomial) is Ok)
                Objective.Constant -> assertTrue(metaModel.minimize(
                    LinearPolynomial(emptyList(), Flt64.zero)
                ) is Ok)
            }
            val mechanism = when (val result = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )) {
                is Ok -> result.value
                is Failed -> fail(result.error.message ?: "failed to build ABS mechanism model")
                is Fatal -> fail(result.errors.joinToString { it.message ?: "" })
            }
            AbsScenario(metaModel, mechanism, input, function)
        } catch (error: Throwable) {
            metaModel.close()
            throw error
        }
    }

    private suspend fun buildMixedScenario(): MixedScenario {
        val absInput = RealVar("gurobi_mixed_abs_input")
        val pwlInput = RealVar("gurobi_mixed_pwl_input")
        absInput.range.geq(Flt64(-2.0))
        absInput.range.leq(Flt64(2.0))
        pwlInput.range.geq(Flt64.zero)
        pwlInput.range.leq(Flt64.two)
        val absFunction = AbsFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, absInput)), Flt64.zero),
            converter = IntoValue.Identity,
            bigM = Flt64(2.0),
            name = "gurobi_mixed_abs"
        )
        val pwlFunction = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, pwlInput)), Flt64.zero),
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64.two),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, Flt64(-1.0)),
            converter = IntoValue.Identity,
            name = "gurobi_mixed_pwl"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "gurobi-mixed-pwl-abs-it",
            configuration = MetaModelConfiguration(functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        return try {
            assertTrue(metaModel.add(listOf(absInput, pwlInput)) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(absFunction, IntoValue.Identity)) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(pwlFunction, IntoValue.Identity)) is Ok)
            assertTrue(metaModel.addConstraint(
                relation = LinearInequality(
                    lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, absInput)), Flt64.zero),
                    rhs = LinearPolynomial(emptyList(), Flt64(-1.5)),
                    comparison = Comparison.EQ,
                    name = "gurobi_mixed_abs_point"
                ),
                name = "gurobi_mixed_abs_point"
            ) is Ok)
            assertTrue(metaModel.addConstraint(
                relation = LinearInequality(
                    lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, pwlInput)), Flt64.zero),
                    rhs = LinearPolynomial(emptyList(), Flt64(1.5)),
                    comparison = Comparison.EQ,
                    name = "gurobi_mixed_pwl_point"
                ),
                name = "gurobi_mixed_pwl_point"
            ) is Ok)
            assertTrue(metaModel.minimize(
                absFunction.resultPolynomial + pwlFunction.resultPolynomial
            ) is Ok)
            val mechanism = when (val result = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )) {
                is Ok -> result.value
                is Failed -> fail(result.error.message ?: "failed to build mixed mechanism model")
                is Fatal -> fail(result.errors.joinToString { it.message ?: "" })
            }
            MixedScenario(metaModel, mechanism, absInput, absFunction, pwlFunction)
        } catch (error: Throwable) {
            metaModel.close()
            throw error
        }
    }

    private fun solverConfig(policy: FunctionExpansionPolicy): SolverConfig {
        return SolverConfig(
            threadNum = UInt64.one,
            dumpIntermediateModelBounds = false,
            functionExpansionPolicy = policy
        )
    }

    private fun afterModelingCallback(shapes: MutableList<NativeModelShape>): GurobiLinearSolverCallBack {
        return GurobiLinearSolverCallBack().afterModeling { _, model, _, _ ->
            model.update()
            shapes += NativeModelShape(
                numVars = model.get(GRB.IntAttr.NumVars),
                numConstrs = model.get(GRB.IntAttr.NumConstrs),
                numGenConstrs = model.get(GRB.IntAttr.NumGenConstrs)
            )
            ok
        }
    }

    private fun valueAt(
        report: SolveReport<Flt64>,
        model: LinearMechanismModel<Flt64>,
        variable: AbstractVariableItem<*, *>
    ): Flt64 {
        val index = model.tokens.tokensInSolver.indexOfFirst { it.key == variable.key }
        assertTrue(index >= 0, "Missing ${variable.name} in solver token order")
        return report.values[index]
    }

    private fun <T> requireAvailable(result: Ret<T>, label: String): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> {
                if (result.error.code in unavailableCodes) {
                    assumeTrue(false, "$label skipped: ${result.error.message}")
                }
                error("$label failed: ${result.error.code}: ${result.error.message}")
            }
            is Fatal -> {
                if (result.errors.all { it.code in unavailableCodes }) {
                    assumeTrue(false, "$label skipped: ${result.errors.joinToString { it.message ?: "" }}")
                }
                error("$label failed fatally: ${result.errors}")
            }
        }
    }

    private enum class Objective {
        Minimum,
        Maximum,
        Constant
    }

    private companion object {
        val unavailableCodes = setOf(
            ErrorCode.SolverNotFound,
            ErrorCode.OREngineEnvironmentLost,
            ErrorCode.OREngineConnectionOvertime
        )
    }

    private data class AbsScenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>,
        val input: RealVar,
        val function: AbsFunction<Flt64>
    ) : AutoCloseable {
        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private data class MixedScenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>,
        val absInput: RealVar,
        val absFunction: AbsFunction<Flt64>,
        val pwlFunction: UnivariateLinearPiecewiseFunction<Flt64>
    ) : AutoCloseable {
        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private data class NativeModelShape(
        val numVars: Int,
        val numConstrs: Int,
        val numGenConstrs: Int
    )
}
