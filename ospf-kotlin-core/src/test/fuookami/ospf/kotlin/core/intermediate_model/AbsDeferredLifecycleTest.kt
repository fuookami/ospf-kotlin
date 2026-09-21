package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelConfiguration
import fuookami.ospf.kotlin.core.model.intermediate.AbsStructure
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.generateAbsConstraints
import fuookami.ospf.kotlin.core.model.intermediate.AbsFallbackMaterializer
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionFallbackTarget
import fuookami.ospf.kotlin.core.model.intermediate.materializeDeferredFunctionFallbacks
import fuookami.ospf.kotlin.core.model.intermediate.CoreDeferredFunctionFallbackMaterializer
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.AbsFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.RealVar

class AbsDeferredLifecycleTest {
    @Test
    fun deferredAbsRowsMatchEagerRowsAndPreserveConstantInput() {
        val inputVariable = RealVar("abs_deferred_x")
        inputVariable.range.geq(Flt64(-2.0))
        inputVariable.range.leq(Flt64(3.0))
        val input = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, inputVariable)),
            constant = Flt64.one
        )
        val function = AbsFunction(
            polynomial = input,
            converter = IntoValue.Identity,
            bigM = Flt64(6.0),
            name = "abs_deferred"
        )
        val structure = function.deferredStructure() as AbsStructure<Flt64>
        val eager = requireOk(
            generateAbsConstraints(
                input = structure.input,
                resultVariable = structure.resultVariable,
                positiveVariable = structure.positiveVariable,
                negativeVariable = structure.negativeVariable,
                signVariable = structure.signVariable,
                positiveBigM = structure.positiveBigM,
                negativeBigM = structure.negativeBigM,
                converter = structure.converter,
                name = structure.name
            )
        )
        val deferred = requireOk(AbsFallbackMaterializer.materialize(structure)).constraints

        assertEquals(eager, deferred)
        assertEquals(Flt64.one, structure.input.constant)
        assertNotSame(input.monomials, structure.input.monomials)
        assertEquals(4, deferred.size)
        assertEquals(
            listOf(
                "abs_deferred_abs_result",
                "abs_deferred_abs_decompose",
                "abs_deferred_abs_pos_ub",
                "abs_deferred_abs_neg_ub"
            ),
            deferred.map { it.name }
        )
    }

    @Test
    fun mixedPiecewiseAndAbsFallbacksShareInputWithoutCrossTypeConflicts() {
        val inputVariable = RealVar("mixed_deferred_x")
        inputVariable.range.geq(Flt64(-2.0))
        inputVariable.range.leq(Flt64(2.0))
        val input = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, inputVariable)),
            constant = Flt64.zero
        )
        val piecewise = UnivariateLinearPiecewiseFunction(
            x = input,
            breakpoints = listOf(Flt64(-2.0), Flt64.zero, Flt64(2.0)),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, Flt64.zero),
            m = Flt64(5.0),
            converter = IntoValue.Identity,
            name = "mixed_deferred_pwl"
        )
        val absolute = AbsFunction(
            polynomial = input,
            converter = IntoValue.Identity,
            bigM = Flt64(5.0),
            name = "mixed_deferred_abs"
        )
        val downstreamAbsolute = AbsFunction(
            polynomial = piecewise.resultPolynomial,
            converter = IntoValue.Identity,
            bigM = Flt64(5.0),
            name = "mixed_deferred_abs_downstream"
        )
        val target = RecordingTarget()
        val result = materializeDeferredFunctionFallbacks(
            structures = listOf(
                piecewise.deferredStructure(),
                absolute.deferredStructure(),
                downstreamAbsolute.deferredStructure()
            ),
            target = target,
            materializer = CoreDeferredFunctionFallbackMaterializer
        )

        val regions = requireOk(result)
        assertEquals(3, regions.size)
        assertEquals(17, target.constraintCount)
        assertTrue(target.names.any { it == "mixed_deferred_pwl_select_one" })
        assertTrue(target.names.any { it == "mixed_deferred_abs_abs_result" })
        assertTrue(target.names.any { it == "mixed_deferred_abs_downstream_abs_result" })
    }

    @Test
    fun eagerAutoAndDeferredAbsMechanismsProduceEquivalentTriadsWithoutSourcePollution() = runBlocking {
        val policies = listOf(
            FunctionExpansionPolicy.EAGER,
            FunctionExpansionPolicy.AUTO,
            FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
        )
        val scenarios = ArrayList<AbsScenario>(policies.size)
        for (policy in policies) {
            scenarios += buildAbsScenario(policy)
        }
        try {
            val snapshots = scenarios.map { scenario ->
                val sourceBefore = sourceSnapshot(scenario.mechanism)
                val first = requireOk(
                    LinearTriadModel.invokeResult(
                        model = scenario.mechanism,
                        dumpConstraintsToBounds = false,
                        concurrent = false
                    )
                )
                val firstSnapshot = try {
                    assertEquals(5, first.variables.size)
                    assertEquals(5, first.constraints.size)
                    normalized(first)
                } finally {
                    first.close()
                }
                val second = requireOk(
                    LinearTriadModel.invokeResult(
                        model = scenario.mechanism,
                        dumpConstraintsToBounds = false,
                        concurrent = false
                    )
                )
                try {
                    assertEquals(firstSnapshot, normalized(second))
                } finally {
                    second.close()
                }
                assertEquals(sourceBefore, sourceSnapshot(scenario.mechanism))
                firstSnapshot
            }

            assertEquals(snapshots.first(), snapshots[1])
            assertEquals(snapshots.first(), snapshots[2])
        } finally {
            scenarios.forEach { it.close() }
        }
    }

    private class RecordingTarget : DeferredFunctionFallbackTarget {
        val names = mutableListOf<String>()
        override val constraintCount: Int
            get() = names.size

    override fun append(constraints: List<LinearInequality<Flt64>>): Try {
            names += constraints.map { it.name }
            return ok
        }

        override fun rollback(constraintCount: Int): Try {
            while (names.size > constraintCount) {
                names.removeAt(names.lastIndex)
            }
            return ok
        }
    }

    private suspend fun buildAbsScenario(policy: FunctionExpansionPolicy): AbsScenario {
        val input = RealVar("abs_lifecycle_x")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(3.0))
        val function = AbsFunction(
            polynomial = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, input)),
                constant = Flt64.one
            ),
            converter = IntoValue.Identity,
            bigM = Flt64(6.0),
            name = "abs_lifecycle"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "abs-lifecycle",
            configuration = MetaModelConfiguration(
                concurrent = false,
                dumpBlocking = true,
                functionExpansionPolicy = policy
            ),
            converter = IntoValue.Identity
        )
        check(metaModel.add(input) is Ok)
        check(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
        val external = LinearInequality(
            lhs = function.resultPolynomial,
            rhs = LinearPolynomial(emptyList(), Flt64(10.0)),
            comparison = Comparison.LE,
            name = "abs_lifecycle_external"
        )
        check(metaModel.addConstraint(relation = external, name = external.name) is Ok)
        check(metaModel.minimize(function.resultPolynomial) is Ok)
        val mechanism = when (val result =
            LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )
        ) {
            is Ok -> result.value
            is Failed -> error(result.error.message)
            else -> error("unexpected fatal result")
        }
        return AbsScenario(metaModel, mechanism)
    }

    private fun sourceSnapshot(model: LinearMechanismModel<Flt64>): SourceSnapshot {
        return SourceSnapshot(
            constraintNames = model.linearConstraints.map { it.name },
            regions = model.deferredFunctionConstraintRegions.map {
                it.firstConstraintIndex to it.constraintCount
            }
        )
    }

    private fun normalized(model: LinearTriadModel): NormalizedTriad {
        val variables = model.variables.map {
            NormalizedVariable(
                name = it.name,
                type = it.type.toString(),
                lowerBound = it.lowerBound,
                upperBound = it.upperBound
            )
        }.sortedBy { it.name }
        val constraints = model.constraints.indices.map { index ->
            NormalizedConstraint(
                name = model.constraints.names[index],
                sign = model.constraints.signs[index].toString(),
                rhs = model.constraints.rhs[index],
                cells = model.constraints.lhs[index].map { cell ->
                    model.variables[cell.colIndex].name to cell.coefficient
                }.sortedBy { it.first }
            )
        }.sortedBy { it.name }
        val objective = model.objective.objective.map { cell ->
            model.variables[cell.colIndex].name to cell.coefficient
        }.sortedBy { it.first }
        return NormalizedTriad(
            variables = variables,
            constraints = constraints,
            objectiveCategory = model.objective.category.toString(),
            objectiveConstant = model.objective.constant,
            objective = objective
        )
    }

    private data class AbsScenario(
        val metaModel: LinearMetaModel<Flt64>,
        val mechanism: LinearMechanismModel<Flt64>
    ) : AutoCloseable {
        override fun close() {
            mechanism.close()
            metaModel.close()
        }
    }

    private data class SourceSnapshot(
        val constraintNames: List<String>,
        val regions: List<Pair<Int, Int>>
    )

    private data class NormalizedVariable(
        val name: String,
        val type: String,
        val lowerBound: Flt64,
        val upperBound: Flt64
    )

    private data class NormalizedConstraint(
        val name: String,
        val sign: String,
        val rhs: Flt64,
        val cells: List<Pair<String, Flt64>>
    )

    private data class NormalizedTriad(
        val variables: List<NormalizedVariable>,
        val constraints: List<NormalizedConstraint>,
        val objectiveCategory: String,
        val objectiveConstant: Flt64,
        val objective: List<Pair<String, Flt64>>
    )

    private fun <T> requireOk(result: Ret<T>): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> error(result.error.message)
            else -> error("unexpected fatal result")
        }
    }
}
