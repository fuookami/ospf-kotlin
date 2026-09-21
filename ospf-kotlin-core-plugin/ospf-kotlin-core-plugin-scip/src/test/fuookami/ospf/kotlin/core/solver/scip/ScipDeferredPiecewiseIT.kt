package fuookami.ospf.kotlin.core.solver.scip

import java.nio.file.Files
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import jscip.SCIP_Vartype
import jscip.Scip
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
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
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** SCIP deferred PWL 原生 lowering 集成测试 / SCIP deferred PWL native-lowering integration tests. */
class ScipDeferredPiecewiseIT {
    @Test
    fun deferredNativeMatchesEagerAndKeepsNativeModelSmall() = runBlocking {
        assumeNativeSupported()
        for ((point, expected) in points) {
            val eager = buildScenario(FunctionExpansionPolicy.EAGER, point)
            try {
                val eagerShapes = ArrayList<NativeModelShape>()
                val eagerResult = requireAvailable(
                    ScipLinearSolver(
                        config = solverConfig(FunctionExpansionPolicy.EAGER),
                        callBack = afterModelingCallback(eagerShapes)
                    ).solve(eager.mechanism, IntoValue.Identity),
                    "SCIP eager PWL at $point"
                )
                assertEquals(1, eagerShapes.size)
                assertEquals(
                    NativeModelShape(numVars = 4, numConstrs = 10, numBinaryVars = 2),
                    eagerShapes.single()
                )

                for (policy in nativePolicies) {
                    val native = buildScenario(policy, point)
                    try {
                        val nativeShapes = ArrayList<NativeModelShape>()
                        val nativeResult = requireAvailable(
                            ScipLinearSolver(
                                config = solverConfig(policy),
                                callBack = afterModelingCallback(nativeShapes)
                            ).solve(native.mechanism, IntoValue.Identity),
                            "SCIP native PWL at $point with $policy"
                        )

                        assertEquals(1, nativeShapes.size)
                        assertEquals(
                            NativeModelShape(numVars = 5, numConstrs = 5, numBinaryVars = 0),
                            nativeShapes.single()
                        )
                        assertEquals(point, valueAt(nativeResult, native.mechanism, native.input))
                        assertEquals(point, valueAt(eagerResult, eager.mechanism, eager.input))
                        assertEquals(
                            valueAt(eagerResult, eager.mechanism, eager.input),
                            valueAt(nativeResult, native.mechanism, native.input)
                        )
                        assertEquals(
                            expected,
                            valueAt(eagerResult, eager.mechanism, eager.function.resultVar)
                        )
                        assertEquals(
                            expected,
                            valueAt(nativeResult, native.mechanism, native.function.resultVar)
                        )

                        assertEquals(
                            native.metaModel.tokens.tokensInSolver.map { it.key },
                            native.mechanism.tokens.tokensInSolver.map { it.key }
                        )
                        assertEquals(native.metaModel.tokens.tokensInSolver.size, nativeResult.values.size)
                        native.metaModel.tokens.setSolution(nativeResult.values)
                        assertEquals(point, native.metaModel.tokens.find(native.input)?.result)
                        assertEquals(expected, native.metaModel.tokens.find(native.function.resultVar)?.result)
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
    fun repeatedNativeSolveDoesNotMutateMechanismRowsOrTokens() = runBlocking {
        assumeNativeSupported()
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST, Flt64(1.5))
        try {
            val originalKeys = scenario.mechanism.tokens.tokens.map { it.key }
            val originalRows = rowSnapshot(scenario.mechanism)
            repeat(2) { attempt ->
                requireAvailable(
                    ScipLinearSolver(config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST))
                        .solve(scenario.mechanism, IntoValue.Identity),
                    "SCIP repeated native PWL solve #${attempt + 1}"
                )
                assertEquals(originalKeys, scenario.mechanism.tokens.tokens.map { it.key })
                assertEquals(originalRows, rowSnapshot(scenario.mechanism))
            }
            assertEquals(originalKeys.size, scenario.mechanism.tokens.tokens.size)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun nativeWriterFailureDisposesAttemptAndRebuildsFallback() = runBlocking {
        assumeNativeSupported()
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST, Flt64(1.5))
        try {
            val observedModels = ArrayList<NativeModelShape>()
            var writerCalls = 0
            val solver = ScipLinearSolver(
                config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                callBack = afterModelingCallback(observedModels)
            )
            solver.nativePiecewiseWriter = { scip, variables, structures ->
                writerCalls++
                val nativeWrite = addScipNativePiecewise(scip, variables, structures)
                assertTrue(nativeWrite is Ok, "The injected writer must first add real native SOS2 data")
                Failed(
                    ErrorCode.Other,
                    "intentional native PWL failure / intentional native PWL failure"
                )
            }

            val result = requireAvailable(
                solver.solve(scenario.mechanism, IntoValue.Identity),
                "SCIP fallback after native PWL writer failure"
            )

            assertEquals(1, writerCalls)
            assertEquals(1, observedModels.size)
            assertEquals(
                NativeModelShape(numVars = 4, numConstrs = 10, numBinaryVars = 2),
                observedModels.single()
            )
            assertEquals(Flt64(1.5), valueAt(result, scenario.mechanism, scenario.input))
            assertEquals(Flt64(2.0), valueAt(result, scenario.mechanism, scenario.function.resultVar))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun externallyRestrictedSelectorStaysOnFallback() = runBlocking {
        assumeNativeSupported()
        val scenario = buildScenario(
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = Flt64(1.5),
            restrictSelector = true
        )
        try {
            val observedModels = ArrayList<NativeModelShape>()
            val result = requireAvailable(
                ScipLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(observedModels)
                ).solve(scenario.mechanism, IntoValue.Identity),
                "SCIP selector-restricted PWL fallback"
            )

            assertEquals(1, observedModels.size)
            assertEquals(
                NativeModelShape(numVars = 4, numConstrs = 11, numBinaryVars = 2),
                observedModels.single()
            )
            assertEquals(Flt64(1.5), valueAt(result, scenario.mechanism, scenario.input))
            assertEquals(Flt64(2.0), valueAt(result, scenario.mechanism, scenario.function.resultVar))
        } finally {
            scenario.close()
        }
    }

    private suspend fun buildScenario(
        policy: FunctionExpansionPolicy,
        point: Flt64,
        restrictSelector: Boolean = false
    ): Scenario {
        val input = RealVar("deferred_pwl_x_${point.toDouble()}_${policy.name}")
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
            name = "deferred_pwl_${point.toDouble()}_${policy.name}"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "scip-deferred-pwl-it-${point.toDouble()}-${policy.name}",
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
                name = "deferred_pwl_x_at_${point.toDouble()}"
            )
            assertTrue(metaModel.addConstraint(relation = pointConstraint, name = pointConstraint.name) is Ok)

            if (restrictSelector) {
                val selector = function.selectorVars.first()
                val selectorConstraint = LinearInequality(
                    lhs = LinearPolynomial(
                        monomials = listOf(LinearMonomial(Flt64.one, selector)),
                        constant = Flt64.zero
                    ),
                    rhs = LinearPolynomial(emptyList(), Flt64.zero),
                    comparison = Comparison.LE,
                    name = "deferred_pwl_selector_reference"
                )
                assertTrue(metaModel.addConstraint(relation = selectorConstraint, name = selectorConstraint.name) is Ok)
            }
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
            threadNum = UInt64.one,
            dumpIntermediateModelBounds = false,
            functionExpansionPolicy = policy
        )
    }

    private fun afterModelingCallback(observedModels: MutableList<NativeModelShape>): ScipSolverCallBack {
        return ScipSolverCallBack().afterModeling { _, scip, _, _ ->
            observedModels += NativeModelShape(
                numVars = scip.getNVars(),
                numConstrs = countOriginalConstraints(scip),
                numBinaryVars = scip.getVars().count { it.type == SCIP_Vartype.SCIP_VARTYPE_BINARY }
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

    private fun rowSnapshot(model: LinearMechanismModel<Flt64>): List<RowSnapshot> {
        return model.constraints.map { constraint ->
            val linear = constraint as LinearConstraintImpl<Flt64>
            RowSnapshot(
                name = linear.name,
                sign = linear.sign,
                rhs = linear.rhs,
                lhs = linear.lhs.map { it.token.key to it.coefficient }
            )
        }
    }

    private fun countOriginalConstraints(scip: Scip): Int {
        val path = Files.createTempFile("ospf-scip-deferred-pwl-", ".cip")
        return try {
            scip.writeOrigProblem(path.toString())
            val lines = Files.readAllLines(path)
            lines.firstNotNullOfOrNull { line ->
                constraintCountPattern.matchEntire(line)?.groupValues?.get(1)?.toIntOrNull()
            } ?: countCipConstraints(lines) ?: error(
                "SCIP original problem did not expose a constraint count / " +
                    "SCIP 原模型未暴露可读取的约束数量"
            )
        } finally {
            Files.deleteIfExists(path)
        }
    }

    private fun countCipConstraints(lines: List<String>): Int? {
        val start = lines.indexOfFirst { it.trim().equals("CONSTRAINTS", ignoreCase = true) }
        if (start < 0) {
            return null
        }
        val end = (start + 1 until lines.size).firstOrNull {
            lines[it].trim().equals("END", ignoreCase = true)
        } ?: return null
        return lines.subList(start + 1, end).count { it.trimStart().startsWith("[") }
    }

    private fun assumeNativeSupported() {
        assumeTrue(
            scipSupportsNativePiecewise(),
            "SCIP native SOS2 capability or native library is unavailable / " +
                "SCIP 原生 SOS2 能力或原生库不可用"
        )
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
            ErrorCode.OREngineConnectionOvertime
        )
        val constraintCountPattern = Regex("""^\s*Constraints\s*:\s*(\d+)\s*$""")
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
        val numConstrs: Int,
        val numBinaryVars: Int
    )

    private data class RowSnapshot(
        val name: String,
        val sign: ConstraintRelation,
        val rhs: Flt64,
        val lhs: List<Pair<VariableItemKey, Flt64>>
    )
}
