package fuookami.ospf.kotlin.core.solver.copt

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import copt.COPT
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/** COPT 延迟 PWL 真实 SDK 集成测试。 / Real-SDK integration tests for deferred COPT PWL. */
class CoptDeferredPiecewiseIT {
    @Test
    fun eagerAndDeferredNativeUseEquivalentResultsAndExpectedShapes() = runBlocking {
        val point = Flt64(1.5)
        val eager = buildScenario(FunctionExpansionPolicy.EAGER, point)
        val native = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST, point)
        try {
            val eagerShapes = ArrayList<NativeModelShape>()
            val eagerResult = requireAvailable(
                CoptLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.EAGER),
                    callBack = afterModelingCallback(eagerShapes)
                ).solve(eager.mechanism, IntoValue.Identity),
                "COPT eager PWL"
            )
            assertEquals(1, eagerShapes.size)
            assertEquals(
                NativeModelShape(
                    cols = 4,
                    rows = 10,
                    soss = 0
                ),
                eagerShapes.single()
            )

            val nativeShapes = ArrayList<NativeModelShape>()
            val nativeResult = requireAvailable(
                CoptLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(nativeShapes)
                ).solve(native.mechanism, IntoValue.Identity),
                "COPT native PWL"
            )
            assertEquals(1, nativeShapes.size)
            assertEquals(
                NativeModelShape(
                    cols = 5,
                    rows = 4,
                    soss = 1
                ),
                nativeShapes.single()
            )
            assertEquals(
                expected = point.toDouble(),
                actual = valueAt(
                    report = eagerResult,
                    model = eager.mechanism,
                    variable = eager.input
                ).toDouble(),
                absoluteTolerance = 1e-6
            )
            assertEquals(
                expected = point.toDouble(),
                actual = valueAt(
                    report = nativeResult,
                    model = native.mechanism,
                    variable = native.input
                ).toDouble(),
                absoluteTolerance = 1e-6
            )
            assertEquals(
                expected = 2.0,
                actual = valueAt(
                    report = eagerResult,
                    model = eager.mechanism,
                    variable = eager.function.resultVar
                ).toDouble(),
                absoluteTolerance = 1e-6
            )
            assertEquals(
                expected = 2.0,
                actual = valueAt(
                    report = nativeResult,
                    model = native.mechanism,
                    variable = native.function.resultVar
                ).toDouble(),
                absoluteTolerance = 1e-6
            )
        } finally {
            eager.close()
            native.close()
        }
    }

    @Test
    fun nativeWriterFailureDisposesAttemptAndRebuildsFallback() = runBlocking {
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST, Flt64(1.5))
        try {
            val observedModels = ArrayList<NativeModelShape>()
            var writerCalls = 0
            val solver = CoptLinearSolver(
                config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                callBack = afterModelingCallback(observedModels)
            )
            solver.nativePiecewiseWriter = { model, variables, data ->
                writerCalls++
                val nativeWrite = addCoptNativePiecewise(
                    model = model,
                    variables = variables,
                    data = data
                )
                assertTrue(nativeWrite is Ok)
                Failed(
                    ErrorCode.OREngineModelingException,
                    "intentional COPT native PWL write failure / intentional COPT native PWL write failure"
                )
            }

            val result = requireAvailable(
                solver.solve(scenario.mechanism, IntoValue.Identity),
                "COPT fallback after native PWL writer failure"
            )

            assertEquals(1, writerCalls)
            assertEquals(
                listOf(
                    NativeModelShape(
                        cols = 4,
                        rows = 10,
                        soss = 0
                    )
                ),
                observedModels
            )
            assertEquals(
                expected = 2.0,
                actual = valueAt(
                    report = result,
                    model = scenario.mechanism,
                    variable = scenario.function.resultVar
                ).toDouble(),
                absoluteTolerance = 1e-6
            )
        } finally {
            scenario.close()
        }
    }

    private suspend fun buildScenario(
        policy: FunctionExpansionPolicy,
        point: Flt64
    ): Scenario {
        val input = RealVar("copt-deferred-pwl-x-${policy.name}")
        assertTrue(input.range.geq(Flt64.zero))
        assertTrue(input.range.leq(Flt64.two))
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, input)),
                constant = Flt64.zero
            ),
            breakpoints = listOf(
                Flt64.zero,
                Flt64.one,
                Flt64.two
            ),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, Flt64(-1.0)),
            converter = IntoValue.Identity,
            name = "copt-deferred-pwl-${policy.name}"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "copt-deferred-pwl-it-${policy.name}",
            configuration = MetaModelConfiguration(functionExpansionPolicy = policy),
            converter = IntoValue.Identity
        )
        return try {
            assertTrue(metaModel.add(input) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
            val pointConstraint = LinearInequality(
                lhs = function.x,
                rhs = LinearPolynomial(emptyList(), point),
                comparison = Comparison.EQ,
                name = "copt-deferred-pwl-x-at-${point.toDouble()}"
            )
            assertTrue(metaModel.addConstraint(relation = pointConstraint, name = pointConstraint.name) is Ok)
            assertTrue(metaModel.minimize(function.resultPolynomial) is Ok)
            val mechanism = when (val result = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )) {
                is Ok -> result.value
                is Failed -> error(result.error.message ?: "COPT scenario construction failed")
                is Fatal -> error(result.errors.joinToString { it.message ?: "" })
            }
            Scenario(
                metaModel = metaModel,
                mechanism = mechanism,
                input = input,
                function = function
            )
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

    private fun afterModelingCallback(
        observedModels: MutableList<NativeModelShape>
    ): CoptLinearSolverCallBack {
        return CoptLinearSolverCallBack().afterModeling { _, model, _, _ ->
            observedModels += NativeModelShape(
                cols = model.getIntAttr(COPT.IntAttr.Cols.key),
                rows = model.getIntAttr(COPT.IntAttr.Rows.key),
                soss = model.getIntAttr(COPT.IntAttr.Soss.key)
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

    private companion object {
        val unavailableCodes = setOf(
            ErrorCode.AuthenticationError,
            ErrorCode.SolverNotFound,
            ErrorCode.OREngineEnvironmentLost,
            ErrorCode.OREngineConnectionOvertime
        )
    }

    private data class Scenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>,
        val input: RealVar,
        val function: UnivariateLinearPiecewiseFunction<Flt64>
    ) : AutoCloseable {
        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private data class NativeModelShape(
        val cols: Int,
        val rows: Int,
        val soss: Int
    )
}
