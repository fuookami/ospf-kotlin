package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.utils.functional.Failed

/**
 * Stable identities for intermediate-model transformations.
 * 中间模型变换的稳定身份回归测试。
 */
class DerivedIntermediateIdentityTest {
    @Test
    fun linearDerivedIdentitiesShouldSurviveIndependentRebuildAndKeepSourceRelations() = runBlocking {
        val first = linearModel("first-display-name")
        val second = linearModel("renamed-display-name")

        val firstTransforms = listOf(
            first.dual(),
            first.farkasDual(),
            first.feasibility(),
            first.elastic(minmaxSlack = true)
        )
        val secondTransforms = listOf(
            second.dual(),
            second.farkasDual(),
            second.feasibility(),
            second.elastic(minmaxSlack = true)
        )

        firstTransforms.zip(secondTransforms).forEach { (left, right) ->
            assertEquals(identityManifest(left), identityManifest(right))
            assertEquals("identity-test", left.identityNamespace)
            assertEquals("2.0", left.identitySchemaVersion)
            assertTrue(left.variables.all { it.id != null })
            assertTrue(left.constraints.ids.size == left.constraints.size)
            assertTrue(left.constraints.ids.map { it.value }.toSet().size == left.constraints.size)
        }

        val farkas = firstTransforms[1]
        val equalConstraintOrigin = ModelElementOrigin("domain", "balance")
        assertTrue(farkas.variables.any { it.id?.value?.contains("farkas-constraint") == true })
        assertTrue(
            farkas.variables.filter { it.id?.value?.contains("farkas-slack") == true }
                .all { it.identityOrigin == equalConstraintOrigin }
        )
        assertTrue(
            farkas.variables.filter { it.id?.value?.contains("farkas-slack") == true }
                .map { it.id!!.value }.toSet().size == 2
        )

        val feasibility = firstTransforms[2]
        assertTrue(
            feasibility.variables.drop(1)
                .mapNotNull { it.identityOrigin }
                .toSet()
                .containsAll(
                    setOf(
                        equalConstraintOrigin,
                        ModelElementOrigin("domain", "capacity")
                    )
                )
        )
        assertEquals(equalConstraintOrigin, feasibility.constraints.identityOriginAt(0))
        assertNotNull(feasibility.objective.id)
        assertEquals(ModelElementScope.Stable, feasibility.objective.identityScope)
        assertEquals(ModelElementOrigin("domain", "objective"), feasibility.objective.identityOrigin)

        assertTrue(
            firstTransforms[3].objective.identityProvenance.containsAll(
                setOf(
                    ModelElementOrigin("domain", "objective"),
                    ModelElementOrigin("domain", "x"),
                    ModelElementOrigin("domain", "balance"),
                    ModelElementOrigin("domain", "capacity")
                )
            )
        )
    }

    @Test
    fun linearDerivedIdentitiesShouldSurviveConstraintReordering() = runBlocking {
        val first = linearModel("first-order")
        val reordered = linearModel("second-order", reordered = true)

        val firstTransforms = listOf(
            first.dual(),
            first.farkasDual(),
            first.feasibility(),
            first.elastic(minmaxSlack = true)
        )
        val reorderedTransforms = listOf(
            reordered.dual(),
            reordered.farkasDual(),
            reordered.feasibility(),
            reordered.elastic(minmaxSlack = true)
        )

        firstTransforms.zip(reorderedTransforms).forEach { (left, right) ->
            assertEquals(identityManifest(left).sorted(), identityManifest(right).sorted())
        }
    }

    @Test
    fun quadraticDerivedIdentitiesShouldSurviveIndependentRebuildAndConstraintReordering() = runBlocking {
        val first = quadraticModel("quadratic-first-display-name")
        val second = quadraticModel("quadratic-renamed-display-name", reordered = true)
        val firstTransforms = listOf(
            first.dual(),
            first.farkasDual(),
            first.feasibility(),
            first.elastic()
        )
        val secondTransforms = listOf(
            second.dual(),
            second.farkasDual(),
            second.feasibility(),
            second.elastic()
        )

        firstTransforms.zip(secondTransforms).forEach { (left, right) ->
            assertEquals(identityManifest(left).sorted(), identityManifest(right).sorted())
        }

        firstTransforms.forEach { transformed ->
            assertEquals("identity-test", transformed.identityNamespace)
            assertEquals("2.0", transformed.identitySchemaVersion)
            assertTrue(transformed.variables.all { it.id != null })
            assertEquals(transformed.constraints.size, transformed.constraints.ids.size)
            assertTrue(transformed.constraints.ids.map { it.value }.toSet().size == transformed.constraints.size)
            assertNotNull(transformed.objective.id)
        }

        val dual = firstTransforms[0]
        val farkas = firstTransforms[1]
        val feasibility = firstTransforms[2]
        val elastic = firstTransforms[3]
        val boundDualIds = dual.variables.filter { it.id?.value?.contains("dual-bound") == true }
            .map { it.id!!.value }
        assertTrue(boundDualIds.size >= 2)
        assertEquals(boundDualIds.size, boundDualIds.toSet().size)
        assertTrue(farkas.variables.any { it.id?.value?.contains("farkas-constraint") == true })
        assertTrue(feasibility.variables.any { it.id?.value?.contains("artifact:feasibility") == true })
        assertTrue(elastic.variables.any { it.id?.value?.contains("elastic-constraint-slack") == true })
        assertTrue(
            elastic.objective.identityProvenance.containsAll(
                setOf(
                    ModelElementOrigin("domain", "objective"),
                    ModelElementOrigin("domain", "x"),
                    ModelElementOrigin("domain", "balance"),
                    ModelElementOrigin("domain", "capacity")
                )
            )
        )
    }

    @Test
    fun malformedStableSourceMustFailDerivedIdentityValidation() = runBlocking {
        val derived = linearModel(
            displayName = "malformed-stable-source",
            malformedStableIdentity = true
        ).dual()

        assertTrue(derived.validateIdentity() is Failed)
    }

    @Test
    fun userDefinedArtifactStableIdentityMustFailBaseModelValidation() {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64.one,
            type = Continuous,
            origin = null,
            name = "reserved-artifact",
            id = VariableId("artifact:user-defined"),
            identityScope = ModelElementScope.Stable,
            identityOrigin = ModelElementOrigin("domain", "reserved-artifact"),
            identityNamespace = "identity-test",
            identitySchemaVersion = "2.0",
            identityProvenance = listOf(ModelElementOrigin("domain", "reserved-artifact"))
        )
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(variable),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix<Flt64>(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "reserved-artifact-model"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(ObjectCategory.Minimum, emptyList())
        )

        assertTrue(model.validateIdentity() is Failed)
    }

    @Test
    fun derivedObjectivesShouldKeepConstraintIdentityMetadataWhenObjectiveMetadataIsAbsent() = runBlocking {
        val linearSource = linearModel("linear-objective-fallback").copy(
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = listOf(LinearObjectiveCell(0, Flt64.one)),
                id = ObjectiveId("objective:cost"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("domain", "objective")
            )
        )
        val quadraticSource = quadraticModel("quadratic-objective-fallback").copy(
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = listOf(QuadraticObjectiveCell(0, 0, Flt64.one)),
                id = ObjectiveId("objective:cost"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("domain", "objective")
            )
        )

        listOf(linearSource.dual(), linearSource.farkasDual(), linearSource.feasibility(), linearSource.elastic())
            .forEach { derived ->
                assertEquals("identity-test", derived.objective.identityNamespace)
                assertEquals("2.0", derived.objective.identitySchemaVersion)
            }
        listOf(quadraticSource.dual(), quadraticSource.farkasDual(), quadraticSource.feasibility(), quadraticSource.elastic())
            .forEach { derived ->
                assertEquals("identity-test", derived.objective.identityNamespace)
                assertEquals("2.0", derived.objective.identitySchemaVersion)
            }
    }

    private fun identityManifest(model: LinearTriadModelView): List<String> = buildList {
        addAll(model.variables.map {
            listOf(
                "v", it.id?.value, it.identityScope.name, it.identityOrigin?.kind,
                it.identityOrigin?.key, it.identityNamespace, it.identitySchemaVersion,
                provenanceText(it.identityProvenance, it.identityOrigin)
            ).joinToString("|")
        })
        addAll(model.constraints.indices.map { index ->
            listOf(
                "c", model.constraints.ids.getOrNull(index)?.value,
                model.constraints.identityScopeAt(index).name,
                model.constraints.identityOriginAt(index)?.kind,
                model.constraints.identityOriginAt(index)?.key,
                model.constraints.identityNamespace, model.constraints.identitySchemaVersion,
                provenanceText(
                    model.constraints.identityProvenanceAt(index),
                    model.constraints.identityOriginAt(index)
                )
            ).joinToString("|")
        })
        add(
            listOf(
                "o", model.objective.id?.value, model.objective.identityScope.name,
                model.objective.identityOrigin?.kind, model.objective.identityOrigin?.key,
                model.objective.identityNamespace, model.objective.identitySchemaVersion,
                provenanceText(model.objective.identityProvenance, model.objective.identityOrigin)
            ).joinToString("|")
        )
    }

    private fun identityManifest(model: QuadraticTetradModel): List<String> = buildList {
        addAll(model.variables.map {
            listOf(
                "v", it.id?.value, it.identityScope.name, it.identityOrigin?.kind,
                it.identityOrigin?.key, it.identityNamespace, it.identitySchemaVersion,
                provenanceText(it.identityProvenance, it.identityOrigin)
            ).joinToString("|")
        })
        addAll(model.constraints.indices.map { index ->
            listOf(
                "c", model.constraints.ids.getOrNull(index)?.value,
                model.constraints.identityScopeAt(index).name,
                model.constraints.identityOriginAt(index)?.kind,
                model.constraints.identityOriginAt(index)?.key,
                model.constraints.identityNamespace, model.constraints.identitySchemaVersion,
                provenanceText(
                    model.constraints.identityProvenanceAt(index),
                    model.constraints.identityOriginAt(index)
                )
            ).joinToString("|")
        })
        add(
            listOf(
                "o", model.objective.id?.value, model.objective.identityScope.name,
                model.objective.identityOrigin?.kind, model.objective.identityOrigin?.key,
                model.objective.identityNamespace, model.objective.identitySchemaVersion,
                provenanceText(model.objective.identityProvenance, model.objective.identityOrigin)
            ).joinToString("|")
        )
    }

    private fun linearModel(
        displayName: String,
        reordered: Boolean = false,
        malformedStableIdentity: Boolean = false
    ): LinearTriadModel {
        val variableId = if (malformedStableIdentity) {
            VariableId("variable:malformed")
        } else {
            VariableId("variable:x")
        }
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = Continuous,
            origin = null,
            name = displayName,
            id = variableId,
            identityScope = ModelElementScope.Stable,
            identityOrigin = if (malformedStableIdentity) null else ModelElementOrigin("domain", "x"),
            identityNamespace = if (malformedStableIdentity) null else "identity-test",
            identitySchemaVersion = if (malformedStableIdentity) null else "2.0",
            identityProvenance = if (malformedStableIdentity) {
                emptyList()
            } else {
                listOf(ModelElementOrigin("domain", "x"))
            }
        )
        val order = if (reordered) listOf(1, 0) else listOf(0, 1)
        val constraintIds = listOf("constraint:balance", "constraint:capacity")
        val constraintOrigins = listOf(
            ModelElementOrigin("domain", "balance"),
            ModelElementOrigin("domain", "capacity")
        )
        val constraints = LinearConstraintBatch(
            sparseLhs = SparseMatrix<Flt64>().also { matrix ->
                order.forEach { matrix.addRow(SparseVector<Flt64>().also { it.add(0, Flt64.one) }) }
            },
            signs = order.map { if (it == 0) ConstraintRelation.Equal else ConstraintRelation.LessEqual },
            rhs = order.map { if (it == 0) Flt64.one else Flt64(8.0) },
            names = order.map { "$displayName-$it" },
            sources = order.map { ConstraintSource.Origin },
            ids = order.map { ConstraintId(constraintIds[it]) },
            identityNamespace = "identity-test",
            identitySchemaVersion = "2.0",
            identityScopes = order.map { ModelElementScope.Stable },
            identityOrigins = order.map { constraintOrigins[it] },
            identityProvenance = order.map { listOf(constraintOrigins[it]) }
        )
        return LinearTriadModel(
            impl = BasicLinearTriadModel(listOf(variable), constraints, displayName),
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = listOf(LinearObjectiveCell(0, Flt64.one)),
                id = ObjectiveId("objective:cost"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("domain", "objective"),
                identityNamespace = "identity-test",
                identitySchemaVersion = "2.0",
                identityProvenance = listOf(ModelElementOrigin("domain", "objective"))
            )
        )
    }

    private fun quadraticModel(
        displayName: String,
        reordered: Boolean = false
    ): QuadraticTetradModel {
        val variable = Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = Continuous,
            origin = null,
            name = displayName,
            id = VariableId("variable:x"),
            identityScope = ModelElementScope.Stable,
            identityOrigin = ModelElementOrigin("domain", "x"),
            identityNamespace = "identity-test",
            identitySchemaVersion = "2.0",
            identityProvenance = listOf(ModelElementOrigin("domain", "x"))
        )
        val order = if (reordered) listOf(1, 0) else listOf(0, 1)
        val constraintOrigins = listOf(
            ModelElementOrigin("domain", "balance"),
            ModelElementOrigin("domain", "capacity")
        )
        val constraints = QuadraticConstraintBatch(
            sparseLhs = SparseQuadraticMatrix().also { matrix ->
                order.forEach { constraintIndex ->
                    matrix.addRow(SparseQuadraticVector().also {
                        if (constraintIndex == 0) {
                            it.add(0, null, Flt64.one)
                        } else {
                            it.add(0, 0, Flt64.one)
                        }
                    })
                }
            },
            signs = order.map { if (it == 0) ConstraintRelation.Equal else ConstraintRelation.LessEqual },
            rhs = order.map { if (it == 0) Flt64.one else Flt64(8.0) },
            names = order.map { "$displayName-$it" },
            sources = order.map { ConstraintSource.Origin },
            ids = order.map { index ->
                ConstraintId(if (index == 0) "constraint:balance" else "constraint:capacity")
            },
            identityNamespace = "identity-test",
            identitySchemaVersion = "2.0",
            identityScopes = order.map { ModelElementScope.Stable },
            identityOrigins = order.map { constraintOrigins[it] },
            identityProvenance = order.map { listOf(constraintOrigins[it]) }
        )
        return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(listOf(variable), constraints, displayName),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = listOf(QuadraticObjectiveCell(0, 0, Flt64.one)),
                id = ObjectiveId("objective:cost"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("domain", "objective"),
                identityNamespace = "identity-test",
                identitySchemaVersion = "2.0",
                identityProvenance = listOf(ModelElementOrigin("domain", "objective"))
            )
        )
    }

    private fun provenanceText(
        provenance: List<ModelElementOrigin>,
        origin: ModelElementOrigin?
    ): String {
        return (provenance + listOfNotNull(origin))
            .distinct()
            .sortedWith(compareBy({ it.kind }, { it.key }))
            .joinToString(",") { "${it.kind}:${it.key}" }
    }
}
