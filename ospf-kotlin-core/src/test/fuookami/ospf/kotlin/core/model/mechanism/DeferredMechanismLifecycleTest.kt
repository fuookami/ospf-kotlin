package fuookami.ospf.kotlin.core.model.mechanism

import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.VariableItemKey

class DeferredMechanismLifecycleTest {
    @Test
    fun nativeSelectionOmitsOnlyOwnedSelectorsBeforeIndexing() = runBlocking {
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST)
        try {
            val original = mechanismState(scenario.mechanism)
            val nativeResult = LinearTriadModel.invokeResult(
                model = scenario.mechanism,
                dumpConstraintsToBounds = false,
                concurrent = false,
                nativeFunctionKeys = setOf(scenario.function.resultVar.key)
            )
            val native = when (nativeResult) {
                is Ok -> nativeResult.value
                is Failed -> fail(nativeResult.error.message)
                is Fatal -> fail(nativeResult.errors.joinToString { it.message })
            }
            native.use {
                assertEquals(listOf(scenario.input.key, scenario.function.resultVar.key), it.tokensInSolver.map { token -> token.key })
                assertEquals(listOf(0, 1), it.variables.map { variable -> variable.index })
                assertEquals(2, it.constraints.size)
                assertTrue(it.deferredFunctionConstraintRegions.isEmpty())
            }
            assertEquals(original, mechanismState(scenario.mechanism))
            val fallback = LinearTriadModel.invokeResult(scenario.mechanism)
            assertTrue(fallback is Ok)
            fallback.value.use {
                assertEquals(4, it.variables.size)
                assertTrue(it.deferredFunctionConstraintRegions.isNotEmpty())
            }
            assertEquals(original, mechanismState(scenario.mechanism))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun nativeSelectionRejectsExternallyRestrictedSelectors() = runBlocking {
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST)
        try {
            val selector = scenario.function.selectorVars.first()
            val token = scenario.mechanism.tokens.tokens.single { it.key == selector.key }
            token.setResult(Flt64.one)
            val result = LinearTriadModel.invokeResult(
                model = scenario.mechanism,
                nativeFunctionKeys = setOf(scenario.function.resultVar.key)
            )
            assertTrue(result is Failed)
            assertTrue(scenario.mechanism.deferredFunctionConstraintRegions.isEmpty())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun eagerAutoAndDeferredProduceEquivalentMaterializedTriads() = runBlocking {
        val policies = listOf(
            FunctionExpansionPolicy.EAGER,
            FunctionExpansionPolicy.AUTO,
            FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
        )
        val scenarios = policies.map { buildScenario(it) }

        try {
            val eager = scenarios.first { it.policy == FunctionExpansionPolicy.EAGER }
            val eagerTriad = LinearTriadModel.invoke(
                model = eager.mechanism,
                dumpConstraintsToBounds = false,
                concurrent = false
            )
            try {
                val expected = triadSnapshot(eagerTriad)
                for (scenario in scenarios) {
                    assertEquals(scenario.policy, scenario.mechanism.functionExpansionPolicy)
                    val helperKeys = scenario.function.helperVariables.map { it.key }.toSet()
                    assertTrue(
                        helperKeys.all { key -> scenario.mechanism.tokens.tokens.any { it.key == key } },
                        "All piecewise helper tokens must remain registered for ${scenario.policy}"
                    )
                    if (scenario.policy == FunctionExpansionPolicy.EAGER) {
                        assertTrue(hasPiecewiseConstraints(scenario.mechanism, scenario.function))
                    } else {
                        assertTrue(scenario.mechanism.deferredFunctionStructures.isNotEmpty())
                        assertTrue(scenario.mechanism.deferredFunctionConstraintRegions.isEmpty())
                        assertFalse(hasPiecewiseConstraints(scenario.mechanism, scenario.function))
                    }

                    val triad = LinearTriadModel.invoke(
                        model = scenario.mechanism,
                        dumpConstraintsToBounds = false,
                        concurrent = false
                    )
                    try {
                        assertEquals(expected, triadSnapshot(triad), "${scenario.policy} triad differs from EAGER")
                        val basicResult = BasicLinearTriadModel.from(
                            model = scenario.mechanism,
                            tokenIndexMap = scenario.mechanism.tokens.tokensInSolver
                                .withIndex()
                                .associate { (index, token) -> token to index }
                        )
                        val basic = when (basicResult) {
                            is Ok -> basicResult.value
                            is Failed -> fail(basicResult.error.message)
                            is Fatal -> fail(basicResult.errors.joinToString { it.message })
                        }
                        basic.use {
                            assertEquals(
                                triadSnapshot(triad).constraints,
                                constraintRows(it.constraints),
                                "${scenario.policy} Basic.from rows differ from LinearTriadModel"
                            )
                        }
                        assertTrue(triad.identityValidation is Ok)
                        val resultIndex = triad.tokensInSolver.indexOfFirst { it.key == scenario.function.resultVar.key }
                        assertTrue(resultIndex >= 0, "The piecewise result token must be numbered")
                        assertEquals(
                            resultIndex,
                            triad.variables.single { it.name == scenario.function.resultVar.name }.index
                        )
                        assertTrue(
                            triad.objective.objective.any {
                                it.colIndex == resultIndex && it.coefficient == Flt64.one
                            },
                            "The objective must reference the numbered piecewise result variable"
                        )
                    } finally {
                        triad.close()
                    }
                }
            } finally {
                eagerTriad.close()
            }
        } finally {
            scenarios.forEach { it.close() }
        }
    }

    @Test
    fun repeatedDeferredDumpStaysIsolatedAndSupportsFixedVariablesAndBounds() = runBlocking {
        val eager = buildScenario(FunctionExpansionPolicy.EAGER)
        val deferred = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST)

        try {
            val originalState = mechanismState(deferred.mechanism)
            val firstDump = LinearTriadModel.invoke(
                model = deferred.mechanism,
                dumpConstraintsToBounds = false,
                concurrent = false
            )
            val secondDump = LinearTriadModel.invoke(
                model = deferred.mechanism,
                dumpConstraintsToBounds = false,
                concurrent = false
            )
            try {
                assertEquals(triadSnapshot(firstDump), triadSnapshot(secondDump))
                assertEquals(originalState, mechanismState(deferred.mechanism))
                assertTrue(deferred.mechanism.deferredFunctionConstraintRegions.isEmpty())
            } finally {
                firstDump.close()
                secondDump.close()
            }

            for (dumpBounds in listOf(false, true)) {
                val fixedEager = LinearTriadModel.invoke(
                    model = eager.mechanism,
                    fixedVariables = mapOf(eager.function.resultVar to Flt64.one),
                    dumpConstraintsToBounds = dumpBounds,
                    concurrent = false
                )
                val fixedDeferred = LinearTriadModel.invoke(
                    model = deferred.mechanism,
                    fixedVariables = mapOf(deferred.function.resultVar to Flt64.one),
                    dumpConstraintsToBounds = dumpBounds,
                    concurrent = false
                )
                try {
                    assertEquals(
                        triadSnapshot(fixedEager),
                        triadSnapshot(fixedDeferred),
                        "fixedVariables/dumpConstraintsToBounds=$dumpBounds must be policy-independent"
                    )
                    assertTrue(fixedDeferred.identityValidation is Ok)
                    assertTrue(fixedDeferred.tokensInSolver.none { it.key == deferred.function.resultVar.key })
                    assertTrue(fixedDeferred.variables.none { it.name == deferred.function.resultVar.name })
                } finally {
                    fixedEager.close()
                    fixedDeferred.close()
                }
            }
            assertEquals(originalState, mechanismState(deferred.mechanism))
        } finally {
            eager.close()
            deferred.close()
        }
    }

    @Test
    fun expandedAutomaticInputBoundsFailThroughIdentityValidationWithoutPollution() = runBlocking {
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST)

        try {
            val originalState = mechanismState(scenario.mechanism)
            scenario.input.range.set(closedRange(0.0, 3.0))

            val basicResult = BasicLinearTriadModel.from(
                model = scenario.mechanism,
                tokenIndexMap = scenario.mechanism.tokens.tokensInSolver
                    .withIndex()
                    .associate { (index, token) -> token to index }
            )
            assertTrue(basicResult is Failed, "Basic.from must fail when automatic-M bounds expand")

            val triad = LinearTriadModel.invoke(
                model = scenario.mechanism,
                dumpConstraintsToBounds = false,
                concurrent = false
            )
            try {
                assertTrue(triad.identityValidation is Failed)

                val dual = triad.dual()
                val farkasDual = triad.farkasDual()
                val feasibility = triad.feasibility()
                val elastic = triad.elastic(
                    minmaxSlack = false,
                    minSlackAmount = null
                )
                try {
                    assertNotSame(triad, dual)
                    assertNotSame(triad, farkasDual)
                    assertNotSame(triad, feasibility)
                    assertNotSame(triad, elastic)
                    assertTrue(dual.identityValidation is Failed)
                    assertTrue(farkasDual.identityValidation is Failed)
                    assertTrue(feasibility.identityValidation is Failed)
                    assertTrue(elastic.identityValidation is Failed)
                } finally {
                    dual.close()
                    farkasDual.close()
                    feasibility.close()
                    elastic.close()
                }

                val output = ByteArrayOutputStream()
                val writer = OutputStreamWriter(output)
                val exportResult = try {
                    triad.exportLP(writer)
                } finally {
                    writer.close()
                }
                assertTrue(exportResult is Failed)
                assertEquals(0, output.size())
            } finally {
                triad.close()
            }

            val invokeResult = LinearTriadModel.invokeResult(
                model = scenario.mechanism,
                dumpConstraintsToBounds = false,
                concurrent = false
            )
            assertTrue(invokeResult is Failed)

            val dumped = LifecycleDumpOnlyLinearSolver().dumpResult(scenario.mechanism)
            assertTrue(dumped is Failed)
            assertEquals(originalState, mechanismState(scenario.mechanism))
            assertTrue(scenario.mechanism.deferredFunctionConstraintRegions.isEmpty())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun tightenedAutomaticInputBoundsAreAcceptedWhileFallbackUsesSnapshotBounds() = runBlocking {
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST)

        try {
            val baseline = LinearTriadModel.invoke(
                model = scenario.mechanism,
                dumpConstraintsToBounds = false,
                concurrent = false
            )
            val baselineSnapshot = try {
                triadSnapshot(baseline)
            } finally {
                baseline.close()
            }

            scenario.input.range.set(closedRange(0.0, 1.0))
            val tightened = LinearTriadModel.invoke(
                model = scenario.mechanism,
                dumpConstraintsToBounds = false,
                concurrent = false
            )
            try {
                assertTrue(tightened.identityValidation is Ok)
                assertEquals(baselineSnapshot.constraints, triadSnapshot(tightened).constraints)
                assertEquals(baselineSnapshot.objective, triadSnapshot(tightened).objective)
                assertEquals(baselineSnapshot.variableShapes, variableShapes(tightened))
                val inputVariable = tightened.variables.single { it.name == scenario.input.name }
                assertEquals(Flt64.zero, inputVariable.lowerBound)
                assertEquals(Flt64.one, inputVariable.upperBound)
            } finally {
                tightened.close()
            }
            assertTrue(scenario.mechanism.deferredFunctionConstraintRegions.isEmpty())
        } finally {
            scenario.close()
        }
    }

    private suspend fun buildScenario(policy: FunctionExpansionPolicy): Scenario {
        val input = RealVar("deferred_lifecycle_x")
        input.range.set(closedRange(0.0, 2.0))
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, input)),
                constant = Flt64.zero
            ),
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64.two),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, -Flt64.one),
            converter = IntoValue.Identity,
            name = "deferred_lifecycle_pwl"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "deferred-mechanism-lifecycle",
            configuration = MetaModelConfiguration(
                concurrent = false,
                dumpBlocking = true,
                functionExpansionPolicy = policy
            ),
            converter = IntoValue.Identity
        )

        try {
            assertTrue(metaModel.add(input) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
            val lowerBound = LinearInequality(
                lhs = function.x,
                rhs = LinearPolynomial(emptyList(), Flt64.zero),
                comparison = Comparison.GE,
                name = "deferred_lifecycle_lower"
            )
            val upperBound = LinearInequality(
                lhs = function.x,
                rhs = LinearPolynomial(emptyList(), Flt64.two),
                comparison = Comparison.LE,
                name = "deferred_lifecycle_upper"
            )
            assertTrue(metaModel.addConstraint(relation = lowerBound, name = lowerBound.name) is Ok)
            assertTrue(metaModel.addConstraint(relation = upperBound, name = upperBound.name) is Ok)
            assertTrue(metaModel.minimize(function.resultPolynomial) is Ok)

            val mechanismResult = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
            val mechanism = when (mechanismResult) {
                is Ok -> mechanismResult.value
                is Failed -> fail(mechanismResult.error.message)
                is Fatal -> fail(mechanismResult.errors.joinToString { it.message })
            }
            return Scenario(policy, input, function, metaModel, mechanism)
        } catch (error: Throwable) {
            metaModel.close()
            throw error
        }
    }

    private fun hasPiecewiseConstraints(
        model: LinearMechanismModel<Flt64>,
        function: UnivariateLinearPiecewiseFunction<Flt64>
    ): Boolean {
        return model.linearConstraints.any { it.name.startsWith("${function.name}_") }
    }

    private fun triadSnapshot(model: LinearTriadModel): TriadSnapshot {
        return TriadSnapshot(
            variables = model.variables.map {
                VariableSnapshot(
                    index = it.index,
                    name = it.name,
                    lowerBound = it.lowerBound,
                    upperBound = it.upperBound,
                    type = it.type::class.simpleName ?: it.type.toString()
                )
            },
            variableShapes = variableShapes(model),
            constraints = constraintRows(model.constraints),
            objective = ObjectiveSnapshot(
                category = model.objective.category,
                constant = model.objective.constant,
                    cells = model.objective.objective.sortedBy { it.colIndex }.map { cell ->
                    CellSnapshot(cell.colIndex, cell.coefficient)
                }
            )
        )
    }

    private fun constraintRows(constraints: LinearConstraintBatch): List<ConstraintRowSnapshot> {
        return constraints.indices.map { index ->
            ConstraintRowSnapshot(
                name = constraints.names[index],
                sign = constraints.signs[index],
                rhs = constraints.rhs[index],
                cells = constraints.lhs[index].sortedBy { it.colIndex }.map { cell ->
                    CellSnapshot(cell.colIndex, cell.coefficient)
                }
            )
        }
    }

    private fun variableShapes(model: LinearTriadModel): List<VariableShape> {
        return model.variables.map {
            VariableShape(
                index = it.index,
                name = it.name,
                type = it.type::class.simpleName ?: it.type.toString()
            )
        }
    }

    private fun mechanismState(model: LinearMechanismModel<Flt64>): MechanismState {
        return MechanismState(
            tokenKeys = model.tokens.tokens.map { it.key },
            constraintNames = model.linearConstraints.map { it.name },
            deferredRegionIndices = model.deferredFunctionConstraintRegions.map {
                it.firstConstraintIndex to it.constraintCount
            }
        )
    }

    private fun closedRange(lower: Double, upper: Double): ValueRange<Flt64> {
        return ValueRange(
            lb = Flt64(lower),
            ub = Flt64(upper),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        ).value!!
    }

    private data class Scenario(
        val policy: FunctionExpansionPolicy,
        val input: RealVar,
        val function: UnivariateLinearPiecewiseFunction<Flt64>,
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>
    ) : AutoCloseable {
        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private data class MechanismState(
        val tokenKeys: List<VariableItemKey>,
        val constraintNames: List<String>,
        val deferredRegionIndices: List<Pair<Int, Int>>
    )

    private data class CellSnapshot(
        val colIndex: Int,
        val coefficient: Flt64
    )

    private data class ConstraintRowSnapshot(
        val name: String,
        val sign: ConstraintRelation,
        val rhs: Flt64,
        val cells: List<CellSnapshot>
    )

    private data class ObjectiveSnapshot(
        val category: ObjectCategory,
        val constant: Flt64,
        val cells: List<CellSnapshot>
    )

    private data class VariableSnapshot(
        val index: Int,
        val name: String,
        val lowerBound: Flt64,
        val upperBound: Flt64,
        val type: String
    )

    private data class VariableShape(
        val index: Int,
        val name: String,
        val type: String
    )

    private data class TriadSnapshot(
        val variables: List<VariableSnapshot>,
        val variableShapes: List<VariableShape>,
        val constraints: List<ConstraintRowSnapshot>,
        val objective: ObjectiveSnapshot
    )
}

private class LifecycleDumpOnlyLinearSolver : AbstractLinearSolver {
    override val name: String = "deferred-lifecycle-dump-only"

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        fail("LifecycleDumpOnlyLinearSolver should not solve a model")
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        fail("LifecycleDumpOnlyLinearSolver should not solve a model")
    }
}
