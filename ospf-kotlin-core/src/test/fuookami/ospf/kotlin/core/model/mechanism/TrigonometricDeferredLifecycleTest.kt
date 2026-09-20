package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.RealVar

class TrigonometricDeferredLifecycleTest {
    @Test
    fun trigonometricWrappersForwardPiecewiseStructureAndResultPolynomial() {
        val input = RealVar("trigonometric_forward_input")
        val points = listOf(
            point2(Flt64.zero, Flt64.zero),
            point2(Flt64.one, Flt64.one),
            point2(Flt64.two, Flt64.zero)
        )
        val sin = SinFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            samplingPoints = points,
            converter = IntoValue.Identity,
            name = "trigonometric_forward_sin"
        )
        val cos = CosFunction(
            x = sin.x,
            samplingPoints = points,
            converter = IntoValue.Identity,
            name = "trigonometric_forward_cos"
        )

        assertPiecewiseForwarding(
            function = sin,
            expectedResult = sin.result,
            expectedPoints = points
        )
        assertPiecewiseForwarding(
            function = cos,
            expectedResult = cos.result,
            expectedPoints = points
        )
    }

    @Test
    fun eagerAndDeferredPoliciesMaterializeEquivalentTrigonometricFallbacks() = runBlocking {
        val policies = listOf(
            FunctionExpansionPolicy.EAGER,
            FunctionExpansionPolicy.AUTO,
            FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
        )
        val scenarios = policies.map { buildScenario(it) }
        try {
            val expected = triadSnapshot(
                LinearTriadModel.invoke(
                    model = scenarios.first().mechanism,
                    dumpConstraintsToBounds = false,
                    concurrent = false
                )
            )
            for (scenario in scenarios) {
                assertEquals(2, scenario.mechanism.deferredFunctionStructures.size)
                if (scenario.policy == FunctionExpansionPolicy.EAGER) {
                    assertTrue(scenario.mechanism.deferredFunctionConstraintRegions.isNotEmpty())
                } else {
                    assertTrue(scenario.mechanism.deferredFunctionConstraintRegions.isEmpty())
                }

                val triad = LinearTriadModel.invoke(
                    model = scenario.mechanism,
                    dumpConstraintsToBounds = false,
                    concurrent = false
                )
                try {
                    assertEquals(expected, triadSnapshot(triad), "${scenario.policy} fallback differs from EAGER")
                    assertTrue(triad.deferredFunctionConstraintRegions.isNotEmpty())
                    assertTrue(triad.objective.objective.isNotEmpty())
                } finally {
                    triad.close()
                }
            }
        } finally {
            scenarios.forEach { it.close() }
        }
    }

    @Test
    fun externallyReferencedTrigonometricSelectorRejectsNativeSelection() = runBlocking {
        val scenario = buildScenario(FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST)
        try {
            val structure = scenario.sin.deferredStructure() as UnivariateLinearPiecewiseStructure<*>
            val selector = scenario.mechanism.tokens.tokens.single { it.key == structure.selectorVariables.first().key }
            selector.setResult(Flt64.zero)

            val result = LinearTriadModel.invokeResult(
                model = scenario.mechanism,
                dumpConstraintsToBounds = false,
                concurrent = false,
                nativeFunctionKeys = setOf(structure.resultVariable.key)
            )

            assertTrue(result is Failed)
            assertTrue(scenario.mechanism.deferredFunctionConstraintRegions.isEmpty())
        } finally {
            scenario.close()
        }
    }

    private fun assertPiecewiseForwarding(
        function: MathFunctionSymbol<Flt64>,
        expectedResult: LinearPolynomial<Flt64>,
        expectedPoints: List<Point<Dim2, Flt64>>
    ) {
        val resultContract = assertNotNull(function as? HasResultPolynomial<Flt64>)
        assertSame(expectedResult, resultContract.resultPolynomial)

        val structure = assertNotNull(function.deferredStructure())
        assertTrue(structure is UnivariateLinearPiecewiseStructure<*>)
        val piecewise = structure as UnivariateLinearPiecewiseStructure<*>
        assertEquals(expectedPoints.map { it[0] }, piecewise.breakpoints)
        assertEquals(expectedPoints.size - 1, piecewise.slopes.size)
        assertEquals(function.helperVariables.first().key, piecewise.resultVariable.key)
        assertEquals(
            function.helperVariables.drop(1).map { it.key },
            piecewise.selectorVariables.map { it.key }
        )
    }

    private suspend fun buildScenario(policy: FunctionExpansionPolicy): Scenario {
        val pi = Flt64(kotlin.math.PI)
        val input = RealVar("trigonometric_lifecycle_input")
        assertTrue(input.range.geq(-pi))
        assertTrue(input.range.leq(pi))
        val inputPolynomial = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, input)),
            Flt64.zero
        )
        val sin = SinFunction(
            x = inputPolynomial,
            converter = IntoValue.Identity,
            name = "trigonometric_lifecycle_sin"
        )
        val cos = CosFunction(
            x = inputPolynomial,
            converter = IntoValue.Identity,
            name = "trigonometric_lifecycle_cos"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "trigonometric-lifecycle-${policy.name}",
            configuration = MetaModelConfiguration(
                concurrent = false,
                dumpBlocking = true,
                functionExpansionPolicy = policy
            ),
            converter = IntoValue.Identity
        )

        return try {
            assertTrue(metaModel.add(input) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(sin, IntoValue.Identity)) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(cos, IntoValue.Identity)) is Ok)
            val resultConstraint = LinearInequality(
                lhs = cos.resultPolynomial,
                rhs = LinearPolynomial(emptyList(), Flt64.one),
                comparison = Comparison.LE,
                name = "trigonometric_lifecycle_cos_result"
            )
            assertTrue(metaModel.addConstraint(relation = resultConstraint, name = resultConstraint.name) is Ok)
            assertTrue(metaModel.maximize(sin.resultPolynomial) is Ok)

            val mechanism = requireOk(
                LinearMechanismModel.invoke<Flt64>(
                    metaModel = metaModel,
                    concurrent = false,
                    blocking = true
                )
            )
            Scenario(policy, metaModel, mechanism, sin)
        } catch (error: Throwable) {
            metaModel.close()
            throw error
        }
    }

    private fun triadSnapshot(model: LinearTriadModel): TriadSnapshot {
        val variableNames = model.variables.associate { it.index to it.name }
        return TriadSnapshot(
            variables = model.variables.map {
                VariableSnapshot(
                    name = it.name,
                    lowerBound = it.lowerBound,
                    upperBound = it.upperBound,
                    type = it.type.toString()
                )
            }.sortedBy { it.name },
            constraints = model.constraints.indices.map { index ->
                ConstraintSnapshot(
                    name = model.constraints.names[index],
                    sign = model.constraints.signs[index].toString(),
                    rhs = model.constraints.rhs[index],
                    cells = model.constraints.lhs[index].map { cell ->
                        variableNames.getValue(cell.colIndex) to cell.coefficient
                    }.sortedBy { it.first }
                )
            }.sortedBy { it.name },
            objective = ObjectiveSnapshot(
                category = model.objective.category.toString(),
                constant = model.objective.constant,
                cells = model.objective.objective.map { cell ->
                    variableNames.getValue(cell.colIndex) to cell.coefficient
                }.sortedBy { it.first }
            )
        )
    }

    private fun <T> requireOk(result: Ret<T>): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> error(result.error.message ?: "operation failed")
            is Fatal -> error(result.errors.joinToString { it.message ?: "" })
        }
    }

    private data class Scenario(
        val policy: FunctionExpansionPolicy,
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>,
        val sin: SinFunction<Flt64>
    ) : AutoCloseable {
        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private data class VariableSnapshot(
        val name: String,
        val lowerBound: Flt64,
        val upperBound: Flt64,
        val type: String
    )

    private data class ConstraintSnapshot(
        val name: String,
        val sign: String,
        val rhs: Flt64,
        val cells: List<Pair<String, Flt64>>
    )

    private data class ObjectiveSnapshot(
        val category: String,
        val constant: Flt64,
        val cells: List<Pair<String, Flt64>>
    )

    private data class TriadSnapshot(
        val variables: List<VariableSnapshot>,
        val constraints: List<ConstraintSnapshot>,
        val objective: ObjectiveSnapshot
    )
}
