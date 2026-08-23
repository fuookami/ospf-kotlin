package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.Shape2
import fuookami.ospf.kotlin.math.algebra.concept.CompanionConstantProviderResolver
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.variable.*

class LinearMetaModelDumpExplicitConstantsPathTest {
    companion object {
        private val propertyKey = CompanionConstantProviderResolver.reflectionFallbackEnabledProperty
        private var previousValue: String? = null

        @JvmStatic
        @BeforeAll
        fun disableReflectionFallback() {
            previousValue = System.getProperty(propertyKey)
            System.clearProperty(propertyKey)
        }

        @JvmStatic
        @AfterAll
        fun restoreReflectionFallback() {
            if (previousValue == null) {
                System.clearProperty(propertyKey)
            } else {
                System.setProperty(propertyKey, previousValue)
            }
        }
    }

    @Test
    fun linearMetaModelDumpShouldUseExplicitFlt64ConstantsForVariableBounds() = runBlocking {
        val x = URealVariable2("x", Shape2(1, 1))
        val xItem = x[0, 0]
        xItem.range.geq(Flt64.zero)
        xItem.range.leq(Flt64(200.0))

        val metaModel = LinearMetaModel(
            name = "bound-explicit-constants-dump",
            objectCategory = ObjectCategory.Minimum
        )

        try {
            assertTrue(metaModel.add(x) is Ok)

            val solver = DumpOnlyLinearSolver()
            val mechanismResult = solver.dump(
                model = metaModel,
                registrationStatusCallBack = null,
                dumpingStatusCallBack = null
            )
            assertTrue(mechanismResult is Ok, "linear meta model dump should succeed")

            val mechanismModel = assertNotNull(mechanismResult.value)
            val triadModel = solver.dump(mechanismModel)
            val variable = assertNotNull(triadModel.variables.singleOrNull { it.name == xItem.name })
            assertEquals(Flt64.zero, variable.lowerBound)
            assertEquals(Flt64(200.0), variable.upperBound)
            mechanismModel.close()
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun identityRegistryShouldPropagateFromMetaModelToTriadDump() = runBlocking {
        val registry = ModelElementIdentityRegistry(namespace = "identity-propagation", schemaVersion = "1.0")
        val x = RealVar("identity-x")
        val metaModel = LinearMetaModel(
            name = "identity-propagation-model",
            identityRegistry = registry
        )

        try {
            assertTrue(metaModel.add(x) is Ok)
            assertTrue(
                metaModel.addConstraint(
                    relation = x leq Flt64.one,
                    name = "identity-constraint"
                ) is Ok
            )
            assertTrue(
                metaModel.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = LinearFlattenData(
                        monomials = listOf(LinearMonomial(Flt64.one, x)),
                        constant = Flt64.zero
                    ),
                    name = "identity-objective"
                ) is Ok
            )
            val variablePrimary = ModelElementOrigin("source", "identity-x")
            val variableSecondary = ModelElementOrigin("source", "identity-x-component")
            val constraintPrimary = ModelElementOrigin("source", "identity-constraint")
            val constraintSecondary = ModelElementOrigin("source", "identity-constraint-component")
            val objectivePrimary = ModelElementOrigin("source", "identity-objective")
            val objectiveSecondary = ModelElementOrigin("source", "identity-objective-component")
            assertTrue(
                registry.registerVariable(
                    element = x,
                    id = VariableId("source:identity-x"),
                    origin = variablePrimary,
                    provenance = listOf(variableSecondary, variablePrimary)
                ) is Ok
            )
            assertTrue(
                registry.registerConstraint(
                    element = metaModel.constraints.single(),
                    id = ConstraintId("source:identity-constraint"),
                    origin = constraintPrimary,
                    provenance = listOf(constraintSecondary, constraintPrimary)
                ) is Ok
            )
            assertTrue(
                registry.registerObjective(
                    element = metaModel.flattenSubObjects.single(),
                    id = ObjectiveId("source:identity-objective"),
                    origin = objectivePrimary,
                    provenance = listOf(objectiveSecondary, objectivePrimary)
                ) is Ok
            )

            val solver = DumpOnlyLinearSolver()
            val mechanism = (solver.dump(metaModel, null, null) as Ok).value
            val triad = LinearTriadModel(
                model = mechanism,
                dumpConstraintsToBounds = false
            )

            assertEquals("identity-propagation", triad.constraints.identityNamespace)
            assertEquals("source:identity-x", triad.variables.single().id?.value)
            assertEquals(
                listOf(variablePrimary, variableSecondary),
                triad.variables.single().identityProvenance
            )
            assertEquals("source:identity-constraint", triad.constraints.ids.single().value)
            assertEquals(ModelElementScope.Stable, triad.constraints.identityScopeAt(0))
            assertEquals(
                listOf(constraintPrimary, constraintSecondary),
                triad.constraints.identityProvenanceAt(0)
            )
            assertEquals("source:identity-objective", triad.objective.id?.value)
            assertEquals(
                listOf(objectivePrimary, objectiveSecondary),
                triad.objective.identityProvenance
            )
            assertTrue(triad.identityValidation is Ok)
            mechanism.close()
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun multipleStableObjectiveIdentitiesShouldProduceAggregateStableIdentity() = runBlocking {
        val registry = ModelElementIdentityRegistry(namespace = "aggregate-linear", schemaVersion = "1.0")
        val x = RealVar("aggregate-linear-x")
        val metaModel = LinearMetaModel(
            name = "aggregate-linear-model",
            identityRegistry = registry
        )

        try {
            assertTrue(metaModel.add(x) is Ok)
            assertTrue(
                metaModel.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = LinearFlattenData(
                        monomials = listOf(LinearMonomial(Flt64.one, x)),
                        constant = Flt64.zero
                    ),
                    name = "aggregate-objective-a"
                ) is Ok
            )
            assertTrue(
                metaModel.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = LinearFlattenData(
                        monomials = listOf(LinearMonomial(Flt64(2.0), x)),
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

            val solver = DumpOnlyLinearSolver()
            val mechanism = (solver.dump(metaModel, null, null) as Ok).value
            val triad = LinearTriadModel(
                model = mechanism,
                dumpConstraintsToBounds = false
            )
            val objectiveId = assertNotNull(triad.objective.id)
            val expectedProvenance = listOf(firstOrigin, firstSource, secondOrigin, secondSource)
                .sortedWith(compareBy({ it.kind }, { it.key }))

            assertEquals(ModelElementScope.Stable, triad.objective.identityScope)
            assertTrue(objectiveId.value.startsWith("artifact:"))
            assertEquals(expectedProvenance, triad.objective.identityProvenance)
            assertTrue(
                triad.identityValidation is Ok,
                "identity validation failed: ${triad.identityValidation}; objective=$objectiveId"
            )
            mechanism.close()
        } finally {
            metaModel.close()
        }
    }

    @Test
    fun incompleteMultipleObjectiveProvenanceMustRemainModelLocal() = runBlocking {
        val registry = ModelElementIdentityRegistry(namespace = "incomplete-linear", schemaVersion = "1.0")
        val x = RealVar("incomplete-linear-x")
        val metaModel = LinearMetaModel(
            name = "incomplete-linear-model",
            identityRegistry = registry
        )

        try {
            assertTrue(metaModel.add(x) is Ok)
            assertTrue(
                metaModel.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = LinearFlattenData(
                        monomials = listOf(LinearMonomial(Flt64.one, x)),
                        constant = Flt64.zero
                    ),
                    name = "incomplete-objective-a"
                ) is Ok
            )
            assertTrue(
                metaModel.addObject(
                    category = ObjectCategory.Minimum,
                    flattenData = LinearFlattenData(
                        monomials = listOf(LinearMonomial(Flt64(2.0), x)),
                        constant = Flt64.zero
                    ),
                    name = "incomplete-objective-b"
                ) is Ok
            )

            val mechanism = (DumpOnlyLinearSolver().dump(metaModel, null, null) as Ok).value
            assertTrue(
                registry.registerObjective(
                    element = mechanism.objectFunction,
                    id = ObjectiveId("objective:incomplete-aggregate"),
                    origin = ModelElementOrigin("objective", "incomplete-aggregate")
                ) is Ok
            )

            val triad = LinearTriadModel(
                model = mechanism,
                dumpConstraintsToBounds = false
            )

            assertEquals(ModelElementScope.ModelLocal, triad.objective.identityScope)
            assertEquals(ObjectiveId("model-local-objective:0"), triad.objective.id)
            assertNull(triad.objective.identityOrigin)
            assertTrue(triad.objective.identityProvenance.isEmpty())
            assertTrue(triad.identityValidation is Ok)
            mechanism.close()
        } finally {
            metaModel.close()
        }
    }
}

private class DumpOnlyLinearSolver : AbstractLinearSolver {
    override val name: String = "dump-only-linear"

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        fail("DumpOnlyLinearSolver should not solve a model")
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        fail("DumpOnlyLinearSolver should not solve a model")
    }
}
