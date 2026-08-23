package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.variable.RealVar

/**
 * 二次机制模型稳定身份传播回归测试。 / Quadratic mechanism model stable-identity propagation regression tests.
 */
class QuadraticMetaModelDumpIdentityPropagationTest {
    /**
     * Verify that an explicit identity registry propagates from a quadratic meta model to the tetrad dump.
     * 验证显式身份注册表从二次元模型传播到四元组转储。
     */
    @Test
    fun identityRegistryShouldPropagateFromQuadraticMetaModelToTetradDump() = runBlocking {
        val registry = ModelElementIdentityRegistry(
            namespace = "quadratic-identity-propagation",
            schemaVersion = "1.0"
        )
        val x = RealVar("identity-quad-x")
        val metaModel = QuadraticMetaModel(
            name = "identity-propagation-quadratic-model",
            identityRegistry = registry
        )

        try {
            assertTrue(metaModel.add(x) is Ok)
            val constraint = QuadraticInequalityOf(
                lhs = QuadraticPolynomial(
                    monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
                    constant = Flt64.zero
                ),
                rhs = QuadraticPolynomial(emptyList(), Flt64.one),
                comparison = Comparison.LE
            )
            assertTrue(
                metaModel.addConstraint(relation = constraint, name = "identity-quadratic-constraint") is Ok
            )
            assertTrue(
                metaModel.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = QuadraticFlattenData(
                        monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
                        constant = Flt64.zero
                    ),
                    name = "identity-quadratic-objective"
                ) is Ok
            )
            val variablePrimary = ModelElementOrigin("source", "identity-quad-x")
            val variableSecondary = ModelElementOrigin("source", "identity-quad-x-component")
            val constraintPrimary = ModelElementOrigin("source", "identity-quad-constraint")
            val constraintSecondary = ModelElementOrigin("source", "identity-quad-constraint-component")
            val objectivePrimary = ModelElementOrigin("source", "identity-quad-objective")
            val objectiveSecondary = ModelElementOrigin("source", "identity-quad-objective-component")
            assertTrue(
                registry.registerVariable(
                    element = x,
                    id = VariableId("source:identity-quad-x"),
                    origin = variablePrimary,
                    provenance = listOf(variableSecondary, variablePrimary)
                ) is Ok
            )
            assertTrue(
                registry.registerConstraint(
                    element = metaModel.constraints.single(),
                    id = ConstraintId("source:identity-quad-constraint"),
                    origin = constraintPrimary,
                    provenance = listOf(constraintSecondary, constraintPrimary)
                ) is Ok
            )
            assertTrue(
                registry.registerObjective(
                    element = metaModel.flattenSubObjects.single(),
                    id = ObjectiveId("source:identity-quad-objective"),
                    origin = objectivePrimary,
                    provenance = listOf(objectiveSecondary, objectivePrimary)
                ) is Ok
            )

            val solver = DumpOnlyQuadraticSolver()
            val mechanism = (solver.dump(metaModel, null, null) as Ok).value
            val tetrad = QuadraticTetradModel(
                model = mechanism,
                dumpConstraintsToBounds = false
            )

            assertEquals("quadratic-identity-propagation", tetrad.constraints.identityNamespace)
            assertEquals("source:identity-quad-x", tetrad.variables.single().id?.value)
            assertEquals(
                listOf(variablePrimary, variableSecondary),
                tetrad.variables.single().identityProvenance
            )
            assertEquals("source:identity-quad-constraint", tetrad.constraints.ids.single().value)
            assertEquals(ModelElementScope.Stable, tetrad.constraints.identityScopeAt(0))
            assertEquals(
                listOf(constraintPrimary, constraintSecondary),
                tetrad.constraints.identityProvenanceAt(0)
            )
            assertEquals("source:identity-quad-objective", tetrad.objective.id?.value)
            assertEquals(
                listOf(objectivePrimary, objectiveSecondary),
                tetrad.objective.identityProvenance
            )
            assertTrue(tetrad.identityValidation is Ok)
            mechanism.close()
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun multipleStableObjectiveIdentitiesShouldProduceAggregateStableIdentity() = runBlocking {
        val registry = ModelElementIdentityRegistry(
            namespace = "aggregate-quadratic",
            schemaVersion = "1.0"
        )
        val x = RealVar("aggregate-quadratic-x")
        val metaModel = QuadraticMetaModel(
            name = "aggregate-quadratic-model",
            identityRegistry = registry
        )

        try {
            assertTrue(metaModel.add(x) is Ok)
            assertTrue(
                metaModel.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = QuadraticFlattenData(
                        monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
                        constant = Flt64.zero
                    ),
                    name = "aggregate-objective-a"
                ) is Ok
            )
            assertTrue(
                metaModel.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = QuadraticFlattenData(
                        monomials = listOf(QuadraticMonomial.linear(Flt64(2.0), x)),
                        constant = Flt64.zero
                    ),
                    name = "aggregate-objective-b"
                ) is Ok
            )

            val firstOrigin = ModelElementOrigin("objective", "aggregate-a")
            val firstSource = ModelElementOrigin("component", "aggregate-a")
            val secondOrigin = ModelElementOrigin("objective", "aggregate-b")
            val secondSource = ModelElementOrigin("component", "aggregate-b")
            assertTrue(
                registry.registerObjective(
                    element = metaModel.flattenSubObjects[0],
                    id = ObjectiveId("objective:aggregate-a"),
                    origin = firstOrigin,
                    provenance = listOf(firstSource, firstOrigin)
                ) is Ok
            )
            assertTrue(
                registry.registerObjective(
                    element = metaModel.flattenSubObjects[1],
                    id = ObjectiveId("objective:aggregate-b"),
                    origin = secondOrigin,
                    provenance = listOf(secondSource, secondOrigin)
                ) is Ok
            )

            val solver = DumpOnlyQuadraticSolver()
            val mechanism = (solver.dump(metaModel, null, null) as Ok).value
            val tetrad = QuadraticTetradModel(
                model = mechanism,
                dumpConstraintsToBounds = false
            )
            val objectiveId = assertNotNull(tetrad.objective.id)
            val expectedProvenance = listOf(firstOrigin, firstSource, secondOrigin, secondSource)
                .sortedWith(compareBy({ it.kind }, { it.key }))

            assertEquals(ModelElementScope.Stable, tetrad.objective.identityScope)
            assertTrue(objectiveId.value.startsWith("artifact:"))
            assertEquals(expectedProvenance, tetrad.objective.identityProvenance)
            assertTrue(
                tetrad.identityValidation is Ok,
                "identity validation failed: ${tetrad.identityValidation}; objective=$objectiveId"
            )
            mechanism.close()
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun incompleteMultipleObjectiveProvenanceMustRemainModelLocal() = runBlocking {
        val registry = ModelElementIdentityRegistry(
            namespace = "incomplete-quadratic",
            schemaVersion = "1.0"
        )
        val x = RealVar("incomplete-quadratic-x")
        val metaModel = QuadraticMetaModel(
            name = "incomplete-quadratic-model",
            identityRegistry = registry
        )

        try {
            assertTrue(metaModel.add(x) is Ok)
            assertTrue(
                metaModel.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = QuadraticFlattenData(
                        monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
                        constant = Flt64.zero
                    ),
                    name = "incomplete-objective-a"
                ) is Ok
            )
            assertTrue(
                metaModel.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = QuadraticFlattenData(
                        monomials = listOf(QuadraticMonomial.linear(Flt64(2.0), x)),
                        constant = Flt64.zero
                    ),
                    name = "incomplete-objective-b"
                ) is Ok
            )

            val mechanism = (DumpOnlyQuadraticSolver().dump(metaModel, null, null) as Ok).value
            assertTrue(
                registry.registerObjective(
                    element = mechanism.objectFunction,
                    id = ObjectiveId("objective:incomplete-aggregate"),
                    origin = ModelElementOrigin("objective", "incomplete-aggregate")
                ) is Ok
            )

            val tetrad = QuadraticTetradModel(
                model = mechanism,
                dumpConstraintsToBounds = false
            )

            assertEquals(ModelElementScope.ModelLocal, tetrad.objective.identityScope)
            assertEquals(ObjectiveId("model-local-objective:0"), tetrad.objective.id)
            assertNull(tetrad.objective.identityOrigin)
            assertTrue(tetrad.objective.identityProvenance.isEmpty())
            assertTrue(tetrad.identityValidation is Ok)
            mechanism.close()
        } finally {
            metaModel.close()
        }
    }
}

/** Dump-only quadratic solver used to build the mechanism model without solving. / 仅转储的二次求解器。 */
private class DumpOnlyQuadraticSolver : AbstractQuadraticSolver {
    override val name: String = "dump-only-quadratic"

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        fail("DumpOnlyQuadraticSolver should not solve a model")
    }

    override suspend fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        fail("DumpOnlyQuadraticSolver should not solve a model")
    }
}
