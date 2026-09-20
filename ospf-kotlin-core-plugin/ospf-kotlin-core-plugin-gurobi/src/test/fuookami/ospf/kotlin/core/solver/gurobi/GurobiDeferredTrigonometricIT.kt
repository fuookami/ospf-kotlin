package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import gurobi.GRB
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.geometry.Point as GeometryPoint
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.model.intermediate.UnivariateLinearPiecewiseStructure
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar

class GurobiDeferredTrigonometricIT {
    @Test
    fun deferredSineNativeMatchesEagerForMaximumAndResultConstraint() = runBlocking {
        val point = Flt64(kotlin.math.PI / 4.0)
        val samplingPoints = nativeSamplingPoints(TrigonometricKind.Sin)
        val eager = buildScenario(
            kind = TrigonometricKind.Sin,
            policy = FunctionExpansionPolicy.EAGER,
            point = point,
            objective = Objective.Maximum,
            samplingPoints = samplingPoints,
            resultConstraint = true
        )
        val deferred = buildScenario(
            kind = TrigonometricKind.Sin,
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = point,
            objective = Objective.Maximum,
            samplingPoints = samplingPoints,
            resultConstraint = true
        )
        try {
            val eagerShapes = ArrayList<NativeModelShape>()
            val eagerReport = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.EAGER),
                    callBack = afterModelingCallback(eagerShapes)
                ).solve(eager.mechanism, IntoValue.Identity),
                "Gurobi eager sine PWL"
            )
            val deferredShapes = ArrayList<NativeModelShape>()
            val deferredReport = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(deferredShapes)
                ).solve(deferred.mechanism, IntoValue.Identity),
                "Gurobi deferred sine native PWL"
            )

            assertEquals(NativeModelShape(numVars = 3, numConstrs = 7, numGenConstrs = 0), eagerShapes.single())
            assertEquals(NativeModelShape(numVars = 2, numConstrs = 2, numGenConstrs = 1), deferredShapes.single())
            assertEquals(point, valueAt(eagerReport, eager.mechanism, eager.input))
            assertEquals(point, valueAt(deferredReport, deferred.mechanism, deferred.input))
            assertClose(
                expected = valueAt(eagerReport, eager.mechanism, eager.resultVariable),
                actual = valueAt(deferredReport, deferred.mechanism, deferred.resultVariable)
            )
            assertClose(
                expected = Flt64(0.5),
                actual = valueAt(deferredReport, deferred.mechanism, deferred.resultVariable)
            )
        } finally {
            eager.close()
            deferred.close()
        }
    }

    @Test
    fun deferredCosineNativeSupportsMinimumObjective() = runBlocking {
        val point = Flt64(kotlin.math.PI / 4.0)
        val scenario = buildScenario(
            kind = TrigonometricKind.Cos,
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = point,
            objective = Objective.Minimum,
            samplingPoints = nativeSamplingPoints(TrigonometricKind.Cos)
        )
        try {
            val shapes = ArrayList<NativeModelShape>()
            val report = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(shapes)
                ).solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi deferred cosine native PWL"
            )

            assertEquals(NativeModelShape(numVars = 2, numConstrs = 1, numGenConstrs = 1), shapes.single())
            assertEquals(scenario.point, valueAt(report, scenario.mechanism, scenario.input))
            assertClose(
                expected = Flt64(0.5),
                actual = valueAt(report, scenario.mechanism, scenario.resultVariable)
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun externallyRestrictedDefaultSineSelectorKeepsFallback() = runBlocking {
        val scenario = buildScenario(
            kind = TrigonometricKind.Sin,
            policy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST,
            point = Flt64.zero,
            objective = Objective.Minimum,
            samplingPoints = null,
            restrictSelector = true
        )
        try {
            val shapes = ArrayList<NativeModelShape>()
            val report = requireAvailable(
                GurobiLinearSolver(
                    config = solverConfig(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
                    callBack = afterModelingCallback(shapes)
                ).solve(scenario.mechanism, IntoValue.Identity),
                "Gurobi default sine selector fallback"
            )

            assertEquals(NativeModelShape(numVars = 6, numConstrs = 19, numGenConstrs = 0), shapes.single())
            assertClose(Flt64.zero, valueAt(report, scenario.mechanism, scenario.input))
            assertClose(
                expected = Flt64.zero,
                actual = valueAt(report, scenario.mechanism, scenario.resultVariable)
            )
        } finally {
            scenario.close()
        }
    }

    private suspend fun buildScenario(
        kind: TrigonometricKind,
        policy: FunctionExpansionPolicy,
        point: Flt64,
        objective: Objective,
        samplingPoints: List<GeometryPoint<Dim2, Flt64>>?,
        resultConstraint: Boolean = false,
        restrictSelector: Boolean = false
    ): Scenario {
        val pi = Flt64(kotlin.math.PI)
        val input = RealVar("gurobi_trig_input_${kind.name}_${policy.name}_${point.toDouble()}")
        val lower = samplingPoints?.first()?.get(0) ?: -pi
        val upper = samplingPoints?.last()?.get(0) ?: pi
        assertTrue(input.range.geq(lower))
        assertTrue(input.range.leq(upper))
        val inputPolynomial = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, input)),
            Flt64.zero
        )
        val function = createFunction(
            kind = kind,
            input = inputPolynomial,
            samplingPoints = samplingPoints,
            name = "gurobi_trig_${kind.name.lowercase()}_${policy.name}"
        )
        val resultPolynomial = (function as HasResultPolynomial<Flt64>).resultPolynomial
        val structure = function.deferredStructure() as UnivariateLinearPiecewiseStructure<*>
        val metaModel = LinearMetaModel<Flt64>(
            name = "gurobi-deferred-trig-${kind.name.lowercase()}-${policy.name}",
            configuration = MetaModelConfiguration(functionExpansionPolicy = policy),
            converter = IntoValue.Identity
        )

        return try {
            assertTrue(metaModel.add(input) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
            val pointConstraint = LinearInequality(
                lhs = inputPolynomial,
                rhs = LinearPolynomial(emptyList(), point),
                comparison = Comparison.EQ,
                name = "gurobi_trig_input_at_${point.toDouble()}"
            )
            assertTrue(metaModel.addConstraint(relation = pointConstraint, name = pointConstraint.name) is Ok)

            if (resultConstraint) {
                val relation = LinearInequality(
                    lhs = resultPolynomial,
                    rhs = LinearPolynomial(emptyList(), Flt64(0.75)),
                    comparison = Comparison.LE,
                    name = "gurobi_trig_result_upper_bound"
                )
                assertTrue(metaModel.addConstraint(relation = relation, name = relation.name) is Ok)
            }
            if (restrictSelector) {
                val relation = LinearInequality(
                    lhs = LinearPolynomial(
                        listOf(LinearMonomial(Flt64.one, structure.selectorVariables.first())),
                        Flt64.zero
                    ),
                    rhs = LinearPolynomial(emptyList(), Flt64.zero),
                    comparison = Comparison.LE,
                    name = "gurobi_trig_external_selector"
                )
                assertTrue(metaModel.addConstraint(relation = relation, name = relation.name) is Ok)
            }
            when (objective) {
                Objective.Minimum -> assertTrue(metaModel.minimize(resultPolynomial) is Ok)
                Objective.Maximum -> assertTrue(metaModel.maximize(resultPolynomial) is Ok)
            }

            val mechanism = when (val result = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )) {
                is Ok -> result.value
                is Failed -> fail(result.error.message ?: "failed to build trigonometric mechanism model")
                is Fatal -> fail(result.errors.joinToString { it.message ?: "" })
            }
            Scenario(
                metaModel = metaModel,
                mechanism = mechanism,
                point = point,
                input = input,
                resultVariable = structure.resultVariable
            )
        } catch (error: Throwable) {
            metaModel.close()
            throw error
        }
    }

    private fun createFunction(
        kind: TrigonometricKind,
        input: LinearPolynomial<Flt64>,
        samplingPoints: List<GeometryPoint<Dim2, Flt64>>?,
        name: String
    ): MathFunctionSymbol<Flt64> {
        return when (kind) {
            TrigonometricKind.Sin -> if (samplingPoints == null) {
                SinFunction(
                    x = input,
                    converter = IntoValue.Identity,
                    name = name
                )
            } else {
                SinFunction(
                    x = input,
                    samplingPoints = samplingPoints,
                    converter = IntoValue.Identity,
                    name = name
                )
            }
            TrigonometricKind.Cos -> if (samplingPoints == null) {
                CosFunction(
                    x = input,
                    converter = IntoValue.Identity,
                    name = name
                )
            } else {
                CosFunction(
                    x = input,
                    samplingPoints = samplingPoints,
                    converter = IntoValue.Identity,
                    name = name
                )
            }
        }
    }

    private fun nativeSamplingPoints(kind: TrigonometricKind): List<GeometryPoint<Dim2, Flt64>> {
        val halfPi = Flt64(kotlin.math.PI / 2.0)
        return when (kind) {
            TrigonometricKind.Sin -> listOf(
                point2(Flt64.zero, Flt64.zero),
                point2(halfPi, Flt64.one)
            )
            TrigonometricKind.Cos -> listOf(
                point2(Flt64.zero, Flt64.one),
                point2(halfPi, Flt64.zero)
            )
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

    private fun assertClose(expected: Flt64, actual: Flt64) {
        assertEquals(expected.toDouble(), actual.toDouble(), 1.0e-6)
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
                if (result.errors.isNotEmpty() && result.errors.all { it.code in unavailableCodes }) {
                    assumeTrue(false, "$label skipped: ${result.errors.joinToString { it.message ?: "" }}")
                }
                error("$label failed fatally: ${result.errors}")
            }
        }
    }

    private companion object {
        val unavailableCodes = setOf(
            ErrorCode.SolverNotFound,
            ErrorCode.OREngineEnvironmentLost,
            ErrorCode.OREngineConnectionOvertime
        )
    }

    private enum class Objective {
        Minimum,
        Maximum
    }

    private enum class TrigonometricKind {
        Sin,
        Cos
    }

    private data class Scenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>,
        val point: Flt64,
        val input: RealVar,
        val resultVariable: AbstractVariableItem<*, *>
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
