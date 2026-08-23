package fuookami.ospf.kotlin.framework.solver.remote.adapter.ospf

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.Objective
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.utils.functional.ok

class OspfRemoteModelSerializerTest {
    @Test
    fun serializesLinearTriadModelView() {
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(
                    variable(index = 0, name = "x", type = Binary),
                    variable(index = 1, name = "y", type = Continuous)
                ),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix<Flt64>().also {
                        it.addRow(
                            SparseVector<Flt64>().also { row ->
                                row.add(index = 0, value = Flt64(2.0))
                                row.add(index = 1, value = Flt64(3.0))
                            }
                        )
                    },
                    signs = listOf(ConstraintRelation.LessEqual),
                    rhs = listOf(Flt64(5.0)),
                    names = listOf("cap"),
                    sources = listOf(ConstraintSource.Origin),
                    ids = listOf(ConstraintId("constraint:capacity")),
                    identityNamespace = "fixture-model",
                    identitySchemaVersion = "1.0",
                    identityScopes = listOf(ModelElementScope.Stable),
                    identityOrigins = listOf(ModelElementOrigin("capacity", "capacity")),
                    identityProvenance = listOf(
                        listOf(
                            ModelElementOrigin("capacity", "capacity"),
                            ModelElementOrigin("source", "capacity")
                        )
                    )
                ),
                name = "linear-model"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(
                category = ObjectCategory.Minimum,
                objective = listOf(
                    LinearObjectiveCell(colIndex = 0, coefficient = Flt64(4.0)),
                    LinearObjectiveCell(colIndex = 1, coefficient = Flt64(6.0))
                ),
                constant = Flt64(1.0),
                id = ObjectiveId("objective:cost"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("objective", "total"),
                identityNamespace = "fixture-model",
                identitySchemaVersion = "1.0",
                identityProvenance = listOf(
                    ModelElementOrigin("objective", "total"),
                    ModelElementOrigin("source", "objective")
                )
            )
        )

        val serialized = serializedModel(model)

        assertEquals("linear-model", serialized.name)
        assertEquals(SerializedVariableType.BINARY, serialized.variables[0].type)
        assertEquals(SerializedVariableType.CONTINUOUS, serialized.variables[1].type)
        assertEquals(SerializedConstraintSign.LESS_EQUAL, serialized.constraints.single().sign)
        assertEquals(Flt64(5.0), serialized.constraints.single().rhs)
        assertEquals(listOf(0, 1), serialized.constraints.single().cells.map { it.colIndex })
        assertEquals(SerializedObjectiveCategory.MINIMIZE, serialized.objective.category)
        assertEquals(Flt64(1.0), serialized.objective.constant)
        assertEquals("fixture-model", serialized.identityNamespace)
        assertEquals("1.0", serialized.identitySchemaVersion)
        assertEquals("variable:x", serialized.variables[0].identityId)
        assertEquals("STABLE", serialized.variables[0].identityScope)
        assertEquals("capacity", serialized.constraints.single().identityOriginKind)
        assertEquals("constraint:capacity", serialized.constraints.single().identityId)
        assertEquals(
            listOf(
                SerializedModelElementOrigin("capacity", "capacity"),
                SerializedModelElementOrigin("source", "capacity")
            ),
            serialized.constraints.single().identityProvenance
        )
        assertEquals("objective:cost", serialized.objective.identityId)
        assertEquals("total", serialized.objective.identityOriginKey)
        assertEquals(
            listOf(
                SerializedModelElementOrigin("objective", "total"),
                SerializedModelElementOrigin("source", "objective")
            ),
            serialized.objective.identityProvenance
        )
        assertEquals(
            listOf(
                SerializedModelElementOrigin("source", "x"),
                SerializedModelElementOrigin("variable", "x")
            ),
            serialized.variables[0].identityProvenance
        )
    }

    @Test
    fun serializesQuadraticTetradModelView() {
        val model = QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = listOf(
                    variable(index = 0, name = "x", type = Integer),
                    variable(index = 1, name = "y", type = Continuous)
                ),
                constraints = QuadraticConstraintBatch(
                    sparseLhs = SparseQuadraticMatrix().also {
                        it.addRow(
                            SparseQuadraticVector().also { row ->
                                row.add(colIndex1 = 0, colIndex2 = null, coefficient = Flt64(2.0))
                                row.add(colIndex1 = 0, colIndex2 = 1, coefficient = Flt64(3.0))
                            }
                        )
                    },
                    signs = listOf(ConstraintRelation.GreaterEqual),
                    rhs = listOf(Flt64(7.0)),
                    names = listOf("quad-cap"),
                    sources = listOf(ConstraintSource.Origin),
                    ids = listOf(ConstraintId("constraint:quad-cap")),
                    identityNamespace = "fixture-model",
                    identitySchemaVersion = "1.0",
                    identityScopes = listOf(ModelElementScope.Stable),
                    identityOrigins = listOf(ModelElementOrigin("capacity", "quad-cap")),
                    identityProvenance = listOf(
                        listOf(
                            ModelElementOrigin("capacity", "quad-cap"),
                            ModelElementOrigin("source", "quad-cap")
                        )
                    )
                ),
                name = "quadratic-model"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(
                category = ObjectCategory.Maximum,
                objective = listOf(
                    QuadraticObjectiveCell(colIndex1 = 0, colIndex2 = null, coefficient = Flt64(4.0)),
                    QuadraticObjectiveCell(colIndex1 = 0, colIndex2 = 1, coefficient = Flt64(5.0))
                ),
                constant = Flt64(2.0),
                id = ObjectiveId("objective:quad-profit"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("objective", "quad-profit"),
                identityNamespace = "fixture-model",
                identitySchemaVersion = "1.0",
                identityProvenance = listOf(
                    ModelElementOrigin("objective", "quad-profit"),
                    ModelElementOrigin("source", "quad-profit")
                )
            )
        )

        val serialized = serializedModel(model)

        assertEquals("quadratic-model", serialized.name)
        assertEquals(SerializedVariableType.INTEGER, serialized.variables[0].type)
        assertEquals(SerializedConstraintSign.GREATER_EQUAL, serialized.quadraticConstraints.single().sign)
        assertEquals(1, serialized.quadraticConstraints.single().linearCells.size)
        assertEquals(1, serialized.quadraticConstraints.single().quadraticCells.size)
        assertEquals(SerializedObjectiveCategory.MAXIMIZE, serialized.objective.category)
        assertEquals(1, serialized.objective.linearCells.size)
        assertEquals(1, serialized.objective.quadraticCells.size)
        assertEquals(Flt64(2.0), serialized.objective.constant)
        assertEquals(2, serialized.quadraticConstraints.single().identityProvenance.size)
        assertEquals(2, serialized.objective.identityProvenance.size)
    }

    @Test
    fun serializesModelLocalScopeWithProtocolSpelling() {
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(
                    Variable(
                        index = 0,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.one,
                        type = Binary,
                        origin = null,
                        name = "local"
                    )
                ),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "model-local"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(ObjectCategory.Minimum, emptyList())
        )

        val serialized = serializedModel(model)

        assertEquals("MODEL_LOCAL", serialized.variables.single().identityScope)
        assertEquals(null, serialized.variables.single().identityId)
    }

    @Test
    fun rejectsMalformedStableObjectiveEvenWhenModelValidationIsOverridden() {
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "malformed-stable"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(
                category = ObjectCategory.Minimum,
                objective = emptyList(),
                id = ObjectiveId("objective:malformed"),
                identityScope = ModelElementScope.Stable
            ),
            identityValidation = ok
        )

        val result = OspfRemoteModelSerializer.serialize(model)
        assertTrue(result.failed)
    }

    @Test
    fun rejectsUserDefinedArtifactStableVariableEvenWhenModelValidationIsOverridden() {
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(
                    Variable(
                        index = 0,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.one,
                        type = Binary,
                        origin = null,
                        name = "artifact-variable",
                        id = VariableId("artifact:user-defined"),
                        identityScope = ModelElementScope.Stable,
                        identityOrigin = ModelElementOrigin("domain", "artifact-variable"),
                        identityNamespace = "fixture-model",
                        identitySchemaVersion = "1.0",
                        identityProvenance = listOf(ModelElementOrigin("domain", "artifact-variable"))
                    )
                ),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "reserved-artifact"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(ObjectCategory.Minimum, emptyList()),
            identityValidation = ok
        )

        val result = OspfRemoteModelSerializer.serialize(model)
        assertTrue(result.failed)
    }

    @Test
    fun rejectsArtifactIdentityGeneratedForAnotherElementKind() {
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(
                    Variable(
                        index = 0,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.one,
                        type = Binary,
                        origin = null,
                        name = "cross-kind-artifact",
                        id = VariableId("artifact:derived:constraint:1h61:1h62"),
                        identityScope = ModelElementScope.Stable,
                        identityOrigin = ModelElementOrigin("domain", "cross-kind-artifact"),
                        identityNamespace = "fixture-model",
                        identitySchemaVersion = "1.0",
                        identityProvenance = listOf(
                            ModelElementOrigin("domain", "cross-kind-artifact")
                        )
                    )
                ),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "cross-kind-artifact"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(ObjectCategory.Minimum, emptyList()),
            identityValidation = ok
        )

        val result = OspfRemoteModelSerializer.serialize(model)
        assertTrue(result.failed)
    }

    @Test
    fun rejectsMalformedStableQuadraticObjectiveAsStructuredFailure() {
        val model = QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = emptyList(),
                constraints = QuadraticConstraintBatch(
                    sparseLhs = SparseQuadraticMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "malformed-quadratic-stable"
            ),
            tokensInSolver = emptyList(),
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList(),
                id = ObjectiveId("objective:malformed-quadratic"),
                identityScope = ModelElementScope.Stable
            ),
            identityValidation = ok
        )

        val result = OspfRemoteModelSerializer.serialize(model)
        assertTrue(result.failed)
    }

    @Test
    fun rejectsConflictingRootAndElementIdentityMetadata() {
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(
                    variable(
                        index = 0,
                        name = "conflicting-namespace",
                        type = Binary,
                        identityNamespace = "other-model"
                    )
                ),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList(),
                    identityNamespace = "fixture-model",
                    identitySchemaVersion = "1.0"
                ),
                name = "conflicting-root"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(ObjectCategory.Minimum, emptyList()),
            identityValidation = ok
        )

        assertTrue(OspfRemoteModelSerializer.serialize(model).failed)
    }

    @Test
    fun rejectsDuplicateIdsAcrossVariablesAndObjective() {
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(variable(0, "shared", Binary)),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList(),
                    identityNamespace = "fixture-model",
                    identitySchemaVersion = "1.0"
                ),
                name = "duplicate-global-id"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(
                category = ObjectCategory.Minimum,
                objective = emptyList(),
                id = ObjectiveId("variable:shared"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("objective", "shared"),
                identityNamespace = "fixture-model",
                identitySchemaVersion = "1.0",
                identityProvenance = listOf(ModelElementOrigin("objective", "shared"))
            ),
            identityValidation = ok
        )

        assertTrue(OspfRemoteModelSerializer.serialize(model).failed)
    }

    @Test
    fun rejectsPrimaryOriginOutsideCompleteProvenance() {
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList(),
                    identityNamespace = "fixture-model",
                    identitySchemaVersion = "1.0"
                ),
                name = "origin-provenance-mismatch"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(
                category = ObjectCategory.Minimum,
                objective = emptyList(),
                id = ObjectiveId("objective:mismatch"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("objective", "primary"),
                identityNamespace = "fixture-model",
                identitySchemaVersion = "1.0",
                identityProvenance = listOf(ModelElementOrigin("objective", "different"))
            ),
            identityValidation = ok
        )

        assertTrue(OspfRemoteModelSerializer.serialize(model).failed)
    }

    @Test
    fun rejectsIdentityListsWithIncorrectConstraintCount() {
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix<Flt64>().also {
                        it.addRow(SparseVector<Flt64>())
                    },
                    signs = listOf(ConstraintRelation.Equal),
                    rhs = listOf(Flt64.zero),
                    names = listOf("invalid-metadata"),
                    sources = listOf(ConstraintSource.Origin),
                    ids = listOf(ConstraintId("constraint:invalid-metadata")),
                    identityNamespace = "fixture-model",
                    identitySchemaVersion = "1.0",
                    identityScopes = listOf(ModelElementScope.Stable, ModelElementScope.ModelLocal)
                ),
                name = "invalid-identity-list"
            ),
            tokensInSolver = emptyList(),
            objective = Objective(ObjectCategory.Minimum, emptyList()),
            identityValidation = ok
        )

        assertTrue(OspfRemoteModelSerializer.serialize(model).failed)
    }

    private fun serializedModel(model: LinearTriadModelView): SerializedLinearModel {
        val result = OspfRemoteModelSerializer.serialize(model)
        assertTrue(result.ok)
        return result.value!!
    }

    private fun serializedModel(model: QuadraticTetradModelView): SerializedQuadraticModel {
        val result = OspfRemoteModelSerializer.serialize(model)
        assertTrue(result.ok)
        return result.value!!
    }

    private fun variable(
        index: Int,
        name: String,
        type: fuookami.ospf.kotlin.core.variable.VariableType<*>,
        identityNamespace: String = "fixture-model",
        identityId: String = "variable:$name"
    ): Variable {
        return Variable(
            index = index,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = type,
            origin = null,
            name = name,
            id = VariableId(identityId),
            identityScope = ModelElementScope.Stable,
            identityOrigin = ModelElementOrigin("variable", name),
            identityNamespace = identityNamespace,
            identitySchemaVersion = "1.0",
            identityProvenance = listOf(
                ModelElementOrigin("variable", name),
                ModelElementOrigin("source", name)
            )
        )
    }
}
