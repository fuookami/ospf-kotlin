package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import com.gurobi.gurobi.GRB
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.plus
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.symbol.function.AbsFunction
import fuookami.ospf.kotlin.core.symbol.function.MaxFunction
import fuookami.ospf.kotlin.core.symbol.function.SlackRangeFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/** Gurobi 11 原生 MAX 端到端集成测试。 / Gurobi 11 native MAX end-to-end integration tests. */
class Gurobi11DeferredMaxIT {
    @Test
    fun nativeAffineMaxSupportsSlackRangeWithoutExportingSdkHelpers() = runBlocking {
        val scenario = buildSlackRangeScenario()
        try {
            val shapes = ArrayList<NativeModelShape>()
            val report = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(),
                    callBack = afterModelingCallback(shapes)
                ).solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi 11 native affine MAX through SlackRange"
            )

            assertEquals(1, shapes.size)
            assertEquals(
                NativeModelShape(
                    numVars = 4,
                    callbackVariables = 2,
                    numConstrs = 3,
                    numGenConstrs = 1
                ),
                shapes.single()
            )
            assertEquals(scenario.mechanism.tokens.tokensInSolver.size, report.values.size)
            assertEquals(
                expected = Flt64.one,
                actual = valueAt(
                    report = report,
                    model = scenario.mechanism,
                    variable = scenario.function.resultVar
                )
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun nativeMaxRestoresMixedAbsAndPiecewiseResults() = runBlocking {
        val scenario = buildMixedScenario()
        try {
            val shapes = ArrayList<NativeModelShape>()
            val report = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(),
                    callBack = afterModelingCallback(shapes)
                ).solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi 11 mixed native ABS PWL MAX"
            )

            assertEquals(1, shapes.size)
            assertEquals(
                NativeModelShape(
                    numVars = 7,
                    callbackVariables = 7,
                    numConstrs = 4,
                    numGenConstrs = 3
                ),
                shapes.single()
            )
            assertEquals(
                expected = Flt64(1.5),
                actual = valueAt(
                    report = report,
                    model = scenario.mechanism,
                    variable = scenario.absFunction.resultVar
                )
            )
            assertEquals(
                expected = Flt64.two,
                actual = valueAt(
                    report = report,
                    model = scenario.mechanism,
                    variable = scenario.pwlFunction.resultVar
                )
            )
            assertEquals(
                expected = Flt64(-0.5),
                actual = valueAt(
                    report = report,
                    model = scenario.mechanism,
                    variable = scenario.maxFunction.resultVar
                )
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun restrictedMaxHelperUsesLinearFallback() = runBlocking {
        val scenario = buildHelperFallbackScenario()
        try {
            val shapes = ArrayList<NativeModelShape>()
            val report = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(),
                    callBack = afterModelingCallback(shapes)
                ).solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi 11 MAX helper fallback"
            )

            assertEquals(1, shapes.size)
            assertEquals(0, shapes.single().numGenConstrs)
            assertEquals(
                expected = Flt64(3.0),
                actual = valueAt(
                    report = report,
                    model = scenario.mechanism,
                    variable = scenario.function.resultVar
                )
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun partialMaxWriterFailureRebuildsTheWholeModel() = runBlocking {
        val scenario = buildTwoMaxScenario()
        try {
            val shapes = ArrayList<NativeModelShape>()
            var writerCalls = 0
            val solver = GurobiLinearSolver(
                config = solverConfig(),
                callBack = afterModelingCallback(shapes)
            )
            solver.nativeMaxWriter = { model, variables, structures ->
                writerCalls++
                val partial = addGurobiNativeMax(model, variables, structures.take(1))
                when (partial) {
                    is Ok -> Failed(
                        ErrorCode.Other,
                        "intentional partial MAX writer failure / intentional partial MAX writer failure"
                    )
                    is Failed -> Failed(partial.error)
                    is Fatal -> Fatal(partial.errors)
                }
            }

            val report = requireAvailable(
                solver.solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi 11 fallback after partial MAX writer failure"
            )

            assertEquals(1, writerCalls)
            assertEquals(1, shapes.size)
            assertEquals(0, shapes.single().numGenConstrs)
            assertEquals(
                expected = Flt64(2.0),
                actual = valueAt(
                    report = report,
                    model = scenario.mechanism,
                    variable = scenario.first.resultVar
                )
            )
            assertEquals(
                expected = Flt64(3.0),
                actual = valueAt(
                    report = report,
                    model = scenario.mechanism,
                    variable = scenario.second.resultVar
                )
            )
        } finally {
            scenario.close()
        }
    }

    private suspend fun buildSlackRangeScenario(): SlackRangeScenario {
        val input = RealVar("gurobi11_max_slack_input")
        input.range.geq(Flt64.zero)
        input.range.leq(Flt64(4.0))
        val function = SlackRangeFunction(
            input = LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, input)),
                Flt64.zero
            ),
            lower = Flt64.one,
            upper = Flt64.two,
            bigM = Flt64(10.0),
            converter = IntoValue.Identity,
            name = "gurobi11_max_slack"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "gurobi11-max-slack-it",
            configuration = MetaModelConfiguration(functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        return try {
            assertTrue(metaModel.add(input) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = input,
                        value = Flt64(3.0),
                        name = "gurobi11_max_slack_point"
                    )
                ) is Ok
            )
            assertTrue(metaModel.maximize(function.resultPolynomial) is Ok)
            SlackRangeScenario(
                metaModel = metaModel,
                mechanism = requireMechanism(metaModel),
                input = input,
                function = function
            )
        } catch (error: Throwable) {
            metaModel.close()
            throw error
        }
    }

    private suspend fun buildMixedScenario(): MixedScenario {
        val absInput = RealVar("gurobi11_max_mixed_abs_input")
        val pwlInput = RealVar("gurobi11_max_mixed_pwl_input")
        val maxFirst = RealVar("gurobi11_max_mixed_first")
        val maxSecond = RealVar("gurobi11_max_mixed_second")
        absInput.range.geq(Flt64(-2.0))
        absInput.range.leq(Flt64(2.0))
        pwlInput.range.geq(Flt64.zero)
        pwlInput.range.leq(Flt64.two)
        maxFirst.range.geq(Flt64(-2.0))
        maxFirst.range.leq(Flt64(2.0))
        maxSecond.range.geq(Flt64(-2.0))
        maxSecond.range.leq(Flt64(2.0))
        val absFunction = AbsFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, absInput)), Flt64.zero),
            converter = IntoValue.Identity,
            bigM = Flt64(2.0),
            name = "gurobi11_max_mixed_abs"
        )
        val pwlFunction = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, pwlInput)), Flt64.zero),
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64.two),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, Flt64(-1.0)),
            converter = IntoValue.Identity,
            name = "gurobi11_max_mixed_pwl"
        )
        val maxFunction = MaxFunction(
            polynomials = listOf(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, maxFirst)), Flt64.zero),
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, maxSecond)), Flt64.zero)
            ),
            bigM = Flt64(4.0),
            converter = IntoValue.Identity,
            name = "gurobi11_max_mixed_max"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "gurobi11-max-mixed-it",
            configuration = MetaModelConfiguration(functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        return try {
            assertTrue(metaModel.add(listOf(absInput, pwlInput, maxFirst, maxSecond)) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(absFunction, IntoValue.Identity)) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(pwlFunction, IntoValue.Identity)) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(maxFunction, IntoValue.Identity)) is Ok)
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = absInput,
                        value = Flt64(-1.5),
                        name = "gurobi11_max_mixed_abs_point"
                    )
                ) is Ok
            )
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = pwlInput,
                        value = Flt64(1.5),
                        name = "gurobi11_max_mixed_pwl_point"
                    )
                ) is Ok
            )
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = maxFirst,
                        value = Flt64(-1.0),
                        name = "gurobi11_max_mixed_first_point"
                    )
                ) is Ok
            )
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = maxSecond,
                        value = Flt64(-0.5),
                        name = "gurobi11_max_mixed_second_point"
                    )
                ) is Ok
            )
            assertTrue(metaModel.minimize(
                absFunction.resultPolynomial + pwlFunction.resultPolynomial + maxFunction.resultPolynomial
            ) is Ok)
            MixedScenario(
                metaModel = metaModel,
                mechanism = requireMechanism(metaModel),
                absFunction = absFunction,
                pwlFunction = pwlFunction,
                maxFunction = maxFunction
            )
        } catch (error: Throwable) {
            metaModel.close()
            throw error
        }
    }

    private suspend fun buildHelperFallbackScenario(): HelperFallbackScenario {
        val first = RealVar("gurobi11_max_helper_first")
        val second = RealVar("gurobi11_max_helper_second")
        first.range.geq(Flt64.zero)
        first.range.leq(Flt64(4.0))
        second.range.geq(Flt64.zero)
        second.range.leq(Flt64(4.0))
        val function = maxFunction(
            first = first,
            second = second,
            name = "gurobi11_max_helper"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "gurobi11-max-helper-fallback-it",
            configuration = MetaModelConfiguration(functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        return try {
            assertTrue(metaModel.add(listOf(first, second)) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = first,
                        value = Flt64(2.0),
                        name = "gurobi11_max_helper_first_point"
                    )
                ) is Ok
            )
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = second,
                        value = Flt64(3.0),
                        name = "gurobi11_max_helper_second_point"
                    )
                ) is Ok
            )
            assertTrue(metaModel.addConstraint(
                LinearInequality(
                    lhs = LinearPolynomial(
                        listOf(LinearMonomial(Flt64.one, function.selectorVars.first())),
                        Flt64.zero
                    ),
                    rhs = LinearPolynomial(emptyList(), Flt64.zero),
                    comparison = Comparison.LE,
                    name = "gurobi11_max_helper_restriction"
                ),
                name = "gurobi11_max_helper_restriction"
            ) is Ok)
            assertTrue(metaModel.minimize(function.resultPolynomial) is Ok)
            HelperFallbackScenario(
                metaModel = metaModel,
                mechanism = requireMechanism(metaModel),
                function = function
            )
        } catch (error: Throwable) {
            metaModel.close()
            throw error
        }
    }

    private suspend fun buildTwoMaxScenario(): TwoMaxScenario {
        val firstInput = RealVar("gurobi11_max_failure_first_input")
        val secondInput = RealVar("gurobi11_max_failure_second_input")
        val thirdInput = RealVar("gurobi11_max_failure_third_input")
        val fourthInput = RealVar("gurobi11_max_failure_fourth_input")
        listOf(firstInput, secondInput, thirdInput, fourthInput).forEach {
            it.range.geq(Flt64.zero)
            it.range.leq(Flt64(4.0))
        }
        val first = maxFunction(
            first = firstInput,
            second = secondInput,
            name = "gurobi11_max_failure_first"
        )
        val second = maxFunction(
            first = thirdInput,
            second = fourthInput,
            name = "gurobi11_max_failure_second"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "gurobi11-max-writer-failure-it",
            configuration = MetaModelConfiguration(functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        return try {
            assertTrue(metaModel.add(listOf(firstInput, secondInput, thirdInput, fourthInput)) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(first, IntoValue.Identity)) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(second, IntoValue.Identity)) is Ok)
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = firstInput,
                        value = Flt64.one,
                        name = "gurobi11_max_failure_first_point"
                    )
                ) is Ok
            )
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = secondInput,
                        value = Flt64(2.0),
                        name = "gurobi11_max_failure_second_point"
                    )
                ) is Ok
            )
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = thirdInput,
                        value = Flt64(3.0),
                        name = "gurobi11_max_failure_third_point"
                    )
                ) is Ok
            )
            assertTrue(
                metaModel.addConstraint(
                    pointConstraint(
                        variable = fourthInput,
                        value = Flt64.one,
                        name = "gurobi11_max_failure_fourth_point"
                    )
                ) is Ok
            )
            assertTrue(metaModel.minimize(first.resultPolynomial + second.resultPolynomial) is Ok)
            TwoMaxScenario(
                metaModel = metaModel,
                mechanism = requireMechanism(metaModel),
                first = first,
                second = second
            )
        } catch (error: Throwable) {
            metaModel.close()
            throw error
        }
    }

    private fun maxFunction(
        first: RealVar,
        second: RealVar,
        name: String
    ): MaxFunction<Flt64> {
        return MaxFunction(
            polynomials = listOf(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, first)), Flt64.zero),
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, second)), Flt64.zero)
            ),
            bigM = Flt64(4.0),
            converter = IntoValue.Identity,
            name = name
        )
    }

    private fun pointConstraint(
        variable: AbstractVariableItem<*, *>,
        value: Flt64,
        name: String
    ): LinearInequality<Flt64> {
        return LinearInequality(
            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero),
            rhs = LinearPolynomial(emptyList(), value),
            comparison = Comparison.EQ,
            name = name
        )
    }

    private suspend fun requireMechanism(metaModel: LinearMetaModel<Flt64>): LinearMechanismModel<Flt64> {
        return when (val result = LinearMechanismModel.invoke<Flt64>(
            metaModel = metaModel,
            concurrent = false,
            blocking = true
        )) {
            is Ok -> result.value
            is Failed -> fail(result.error.message ?: "failed to build Gurobi 11 MAX mechanism model")
            is Fatal -> fail(result.errors.joinToString { it.message ?: "" })
        }
    }

    private fun solverConfig(): SolverConfig {
        return SolverConfig(
            threadNum = UInt64.one,
            dumpIntermediateModelBounds = false,
            functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
        )
    }

    private fun afterModelingCallback(shapes: MutableList<NativeModelShape>): GurobiLinearSolverCallBack {
        return GurobiLinearSolverCallBack().afterModeling { _, model, variables, _ ->
            model.update()
            shapes += NativeModelShape(
                numVars = model.get(GRB.IntAttr.NumVars),
                callbackVariables = variables.size,
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
                if (isUnavailable(result.error.code, result.error.message)) {
                    assumeTrue(false, "$label skipped: ${result.error.message}")
                }
                error("$label failed: ${result.error.code}: ${result.error.message}")
            }
            is Fatal -> {
                if (result.errors.all { isUnavailable(it.code, it.message) }) {
                    assumeTrue(false, "$label skipped: ${result.errors.joinToString { it.message ?: "" }}")
                }
                error("$label failed fatally: ${result.errors}")
            }
        }
    }

    private fun isUnavailable(code: ErrorCode, message: String?): Boolean {
        return code in unavailableCodes || message?.contains("license", ignoreCase = true) == true
    }

    private companion object {
        val unavailableCodes = setOf(
            ErrorCode.SolverNotFound,
            ErrorCode.OREngineEnvironmentLost,
            ErrorCode.OREngineConnectionOvertime
        )
    }

    private data class NativeModelShape(
        val numVars: Int,
        val callbackVariables: Int,
        val numConstrs: Int,
        val numGenConstrs: Int
    )

    private data class SlackRangeScenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>,
        val input: RealVar,
        val function: SlackRangeFunction<Flt64>
    ) : AutoCloseable {
        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private data class MixedScenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>,
        val absFunction: AbsFunction<Flt64>,
        val pwlFunction: UnivariateLinearPiecewiseFunction<Flt64>,
        val maxFunction: MaxFunction<Flt64>
    ) : AutoCloseable {
        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private data class HelperFallbackScenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>,
        val function: MaxFunction<Flt64>
    ) : AutoCloseable {
        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private data class TwoMaxScenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>,
        val first: MaxFunction<Flt64>,
        val second: MaxFunction<Flt64>
    ) : AutoCloseable {
        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }
}
