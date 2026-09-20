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

/** Gurobi deferred PWL 原生 lowering 集成测试 / Gurobi deferred PWL native-lowering integration tests. */
class GurobiDeferredPiecewiseIT {
    @Test
    fun deferredNativeMatchesEagerAndKeepsNativeModelSmall() = runBlocking {
        for ((point, expected) in points) {
            val eager = buildScenario(FunctionExpansionPolicy.EAGER, point)
            try {
                val eagerShapes = ArrayList<NativeModelShape>()
                val eagerResult = requireAvailable(
                    GurobiLinearSolver(
                        config = solverConfig(FunctionExpansionPolicy.EAGER),
                        callBack = afterModelingCallback(eagerShapes)
                    ).solve(eager.mechanism, IntoValue.Identity),
                    "Gurobi eager PWL at $point"
                )
                assertEquals(1, eagerShapes.size)
                assertEquals(4, eagerShapes.single().numVars)
                assertEquals(0, eagerShapes.single().numGenConstrs)
                assertEquals(10, eagerShapes.single().numConstrs)

                for (policy in nativePolicies) {
                    val native = buildScenario(policy, point)
                    try {
                        val nativeShapes = ArrayList<NativeModelShape>()
                        val nativeResult = requireAvailable(
                            GurobiLinearSolver(
                                config = solverConfig(policy),
                                callBack = afterModelingCallback(nativeShapes)
                            ).solve(native.mechanism, IntoValue.Identity),
                            "Gurobi native PWL at $point with $policy"
                        )

                        assertEquals(1, nativeShapes.size)
                        val nativeShape = nativeShapes.single()
                        assertEquals(eagerShapes.single().numVars - 2, nativeShape.numVars)
                        assertEquals(eagerShapes.single().numConstrs - 9, nativeShape.numConstrs)
                        assertEquals(1, nativeShape.numGenConstrs)
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
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST, Flt64(1.5))
        try {
            val originalKeys = scenario.mechanism.tokens.tokens.map { it.key }
            val originalRows = rowSnapshot(scenario.mechanism)
            repeat(2) { attempt ->
                requireAvailable(
                    GurobiLinearSolver(config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST))
                        .solve(scenario.mechanism, IntoValue.Identity),
                    "Gurobi repeated native PWL solve #${attempt + 1}"
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
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST, Flt64(1.5))
        try {
            val observedModels = ArrayList<NativeModelShape>()
            var writerCalls = 0
            val solver = GurobiLinearSolver(
                config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                callBack = afterModelingCallback(observedModels)
            )
            solver.nativePiecewiseWriter = { model, variables, structures ->
                writerCalls++
                val nativeWrite = addGurobiNativePiecewise(model, variables, structures)
                assertTrue(nativeWrite is Ok, "The injected writer must first add real native PWL data")
                Failed(ErrorCode.Other, "intentional native PWL failure / intentional native PWL failure")
            }

            val result = requireAvailable(
                solver.solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi fallback after native PWL writer failure"
            )

            assertEquals(1, writerCalls)
            assertEquals(1, observedModels.size)
            assertEquals(
                NativeModelShape(numVars = 4, numConstrs = 10, numGenConstrs = 0),
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
        val scenario = buildScenario(
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = Flt64(1.5),
            restrictSelector = true
        )
        try {
            val observedModels = ArrayList<NativeModelShape>()
            val result = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(observedModels)
                ).solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi selector-restricted PWL fallback"
            )

            assertEquals(1, observedModels.size)
            val shape = observedModels.single()
            assertEquals(4, shape.numVars)
            assertEquals(0, shape.numGenConstrs)
            assertEquals(11, shape.numConstrs)
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
            name = "gurobi-deferred-pwl-it-${point.toDouble()}-${policy.name}",
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

    private fun afterModelingCallback(observedModels: MutableList<NativeModelShape>): GurobiLinearSolverCallBack {
        return GurobiLinearSolverCallBack().afterModeling { _, model, _, _ ->
            model.update()
            observedModels += NativeModelShape(
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
        val numGenConstrs: Int
    )

    private data class RowSnapshot(
        val name: String,
        val sign: ConstraintRelation,
        val rhs: Flt64,
        val lhs: List<Pair<VariableItemKey, Flt64>>
    )
}
