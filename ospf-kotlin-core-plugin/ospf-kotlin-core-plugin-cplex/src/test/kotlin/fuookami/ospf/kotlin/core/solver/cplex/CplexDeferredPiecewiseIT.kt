package fuookami.ospf.kotlin.core.solver.cplex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/** CPLEX deferred SOS2 PWL 真实 SDK 集成测试。 / Real-SDK integration tests for deferred CPLEX SOS2 PWL. */
class CplexDeferredPiecewiseIT {
    @Test
    fun eagerAndNativePathsMatchValuesAndModelScale() = runBlocking {
        for ((point, expected) in points) {
            val eager = buildScenario(FunctionExpansionPolicy.EAGER, point)
            try {
                val eagerShapes = ArrayList<NativeModelShape>()
                val eagerResult = requireAvailable("CPLEX eager PWL at $point") {
                    CplexLinearSolver(
                        config = solverConfig(FunctionExpansionPolicy.EAGER),
                        callBack = afterModelingCallback(eagerShapes)
                    ).solve(eager.mechanism, IntoValue.Identity)
                }
                assertEquals(NativeModelShape(numVars = 4, numRows = 10, sos2 = 0), eagerShapes.single())

                for (policy in nativePolicies) {
                    val native = buildScenario(policy, point)
                    try {
                        val nativeShapes = ArrayList<NativeModelShape>()
                        val nativeResult = requireAvailable("CPLEX native PWL at $point with $policy") {
                            CplexLinearSolver(
                                config = solverConfig(policy),
                                callBack = afterModelingCallback(nativeShapes)
                            ).solve(native.mechanism, IntoValue.Identity)
                        }
                        assertEquals(
                            NativeModelShape(numVars = 5, numRows = 4, sos2 = 1),
                            nativeShapes.single()
                        )
                        assertValueEquals(point, valueAt(nativeResult, native.mechanism, native.input))
                        assertValueEquals(point, valueAt(eagerResult, eager.mechanism, eager.input))
                        assertValueEquals(expected, valueAt(eagerResult, eager.mechanism, eager.function.resultVar))
                        assertValueEquals(expected, valueAt(nativeResult, native.mechanism, native.function.resultVar))
                        assertEquals(native.mechanism.tokens.tokensInSolver.size, nativeResult.values.size)
                    } finally {
                        native.close()
                    }
                }
            } finally {
                eager.close()
            }
        }
    }

    @Test
    fun nativeWriterFailureDisposesAttemptAndRebuildsFallback() = runBlocking {
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST, Flt64(1.5))
        try {
            val observedShapes = ArrayList<NativeModelShape>()
            var writerCalls = 0
            val solver = CplexLinearSolver(
                config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                callBack = afterModelingCallback(observedShapes)
            )
            solver.nativePiecewiseWriter = { model, variables, data ->
                writerCalls++
                val nativeWrite = addCplexNativePiecewise(model, variables, data)
                assertTrue(nativeWrite is Ok)
                Failed(ErrorCode.Other, "intentional native PWL failure / intentional native PWL failure")
            }

            val report = requireAvailable("CPLEX fallback after native writer failure") {
                solver.solve(scenario.mechanism, IntoValue.Identity)
            }

            assertEquals(1, writerCalls)
            assertEquals(NativeModelShape(numVars = 4, numRows = 10, sos2 = 0), observedShapes.single())
            assertValueEquals(Flt64(1.5), valueAt(report, scenario.mechanism, scenario.input))
            assertValueEquals(Flt64(2.0), valueAt(report, scenario.mechanism, scenario.function.resultVar))
        } finally {
            scenario.close()
        }
    }

    private suspend fun buildScenario(
        policy: FunctionExpansionPolicy,
        point: Flt64
    ): Scenario {
        val input = RealVar("cplex_deferred_pwl_x_${point.toDouble()}_${policy.name}")
        assertTrue(input.range.geq(Flt64.zero))
        assertTrue(input.range.leq(Flt64.two))
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, input)),
                constant = Flt64.zero
            ),
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64.two),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, Flt64(-1.0)),
            converter = IntoValue.Identity,
            name = "cplex_deferred_pwl_${point.toDouble()}_${policy.name}"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "cplex-deferred-pwl-it-${point.toDouble()}-${policy.name}",
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
                name = "cplex_deferred_pwl_x_at_${point.toDouble()}"
            )
            assertTrue(metaModel.addConstraint(relation = pointConstraint, name = pointConstraint.name) is Ok)
            assertTrue(metaModel.minimize(function.resultPolynomial) is Ok)
            val mechanism = when (val result = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )) {
                is Ok -> result.value
                is Failed -> fail(result.error.message)
                is Fatal -> fail(result.errors.joinToString { it.message ?: "" })
            }
            Scenario(metaModel, mechanism, input, function)
        } catch (error: Throwable) {
            metaModel.close()
            throw error
        }
    }

    private fun solverConfig(policy: FunctionExpansionPolicy): SolverConfig {
        return SolverConfig(
            time = 30.seconds,
            threadNum = UInt64.one,
            dumpIntermediateModelBounds = false,
            functionExpansionPolicy = policy
        )
    }

    private fun afterModelingCallback(observedShapes: MutableList<NativeModelShape>): CplexSolverCallBack {
        return CplexSolverCallBack().afterModeling { _, cplex, _, _ ->
            observedShapes += NativeModelShape(
                numVars = cplex.getNcols(),
                numRows = cplex.getNrows(),
                sos2 = cplex.getNSOS2()
            )
            ok
        }
    }

    private fun assertValueEquals(expected: Flt64, actual: Flt64) {
        assertEquals(expected.toDouble(), actual.toDouble(), 1e-6)
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

    private suspend fun <T> requireAvailable(label: String, action: suspend () -> Ret<T>): T {
        return try {
            requireAvailableResult(label, action())
        } catch (error: Throwable) {
            if (isUnavailableThrowable(error)) {
                assumeTrue(false, "$label skipped: ${error.message}")
            }
            throw error
        }
    }

    private fun <T> requireAvailableResult(label: String, result: Ret<T>): T {
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
        return code in unavailableCodes || message.orEmpty().lowercase().let {
            "license" in it || "licence" in it || "native library" in it || "unsatisfiedlinkerror" in it
        }
    }

    private fun isUnavailableThrowable(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is UnsatisfiedLinkError) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private companion object {
        val points = listOf(
            Flt64(0.5) to Flt64(0.5),
            Flt64.one to Flt64.one,
            Flt64(1.5) to Flt64(2.0)
        )
        val nativePolicies = listOf(
            FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            FunctionExpansionPolicy.AUTO
        )
        val unavailableCodes = setOf(
            ErrorCode.SolverNotFound,
            ErrorCode.OREngineEnvironmentLost,
            ErrorCode.OREngineConnectionOvertime,
            ErrorCode.AuthenticationError
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
        val numVars: Int,
        val numRows: Int,
        val sos2: Int
    )
}
