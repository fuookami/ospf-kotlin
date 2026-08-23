/** Gurobi 稳定身份到原生 artifact 的正向投影测试。 / Gurobi stable identity to native artifact forward projection tests. */
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjective
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseVector
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.Continuous
import gurobi.GRB
import gurobi.GRBConstr
import gurobi.GRBVar

/**
 * Gurobi 线性求解器稳定身份原生投影集成测试。 / Gurobi linear solver stable identity native projection integration test.
 */
class GurobiLinearIdentityProjectionIT {
    /**
     * 稳定元素 ID 应投影为带命名空间的原生名称，model-local 元素保留展示名。
     * Stable element IDs must be projected to namespaced native names while model-local elements keep display names.
     */
    @Test
    fun stableIdentityIsProjectedToNativeNames() = runBlocking {
        val capturedVariableNames = ArrayList<String>()
        val capturedConstraintNames = ArrayList<String>()
        val callBack = GurobiLinearSolverCallBack().configuration { _, grbModel, variables, constraints ->
            grbModel.update()
            capturedVariableNames.addAll(variables.map { it.get(GRB.StringAttr.VarName) })
            capturedConstraintNames.addAll(constraints.map { it.get(GRB.StringAttr.ConstrName) })
            ok
        }

        val result = GurobiLinearSolver(callBack = callBack)(projectionModel(), null)

        assertTrue(
            result.ok,
            "Gurobi solve failed: " +
                if (result is Failed) "${result.code}: ${result.message}" else result
        )
        assertEquals("ospf-variable-fixture:variable:x", capturedVariableNames[0])
        assertEquals("localVar", capturedVariableNames[1])
        assertEquals("ospf-constraint-fixture:constraint:c0", capturedConstraintNames[0])
        assertEquals("localCons", capturedConstraintNames[1])
    }

    private fun projectionModel(): LinearTriadModel {
        val lhs = SparseMatrix<Flt64>().also { matrix ->
            matrix.addRow(
                SparseVector<Flt64>().also {
                    it.add(0, Flt64.one)
                    it.add(1, Flt64.one)
                }
            )
            matrix.addRow(
                SparseVector<Flt64>().also {
                    it.add(0, Flt64.one)
                    it.add(1, -Flt64.one)
                }
            )
        }
        val basicModel = BasicLinearTriadModel(
            variables = listOf(
                Variable(
                    index = 0,
                    lowerBound = Flt64.zero,
                    upperBound = Flt64(10.0),
                    type = Continuous,
                    origin = null,
                    name = "x",
                    id = VariableId("fixture:variable:x"),
                    identityScope = ModelElementScope.Stable,
                    identityOrigin = ModelElementOrigin("variable", "x"),
                    identityNamespace = "fixture",
                    identitySchemaVersion = "1.0",
                    identityProvenance = listOf(ModelElementOrigin("variable", "x"))
                ),
                Variable(
                    index = 1,
                    lowerBound = Flt64.zero,
                    upperBound = Flt64(10.0),
                    type = Continuous,
                    origin = null,
                    name = "localVar"
                )
            ),
            constraints = LinearConstraintBatch(
                sparseLhs = lhs,
                signs = listOf(ConstraintRelation.GreaterEqual, ConstraintRelation.LessEqual),
                rhs = listOf(Flt64.one, Flt64.zero),
                names = listOf("c0", "localCons"),
                sources = listOf(ConstraintSource.Origin, ConstraintSource.Origin),
                ids = listOf(
                    ConstraintId("fixture:constraint:c0"),
                    ConstraintId("model-local-constraint:localCons")
                ),
                identityNamespace = "fixture",
                identitySchemaVersion = "1.0",
                identityScopes = listOf(ModelElementScope.Stable, ModelElementScope.ModelLocal),
                identityOrigins = listOf(ModelElementOrigin("constraint", "c0"), null),
                identityProvenance = listOf(
                    listOf(ModelElementOrigin("constraint", "c0")),
                    emptyList()
                )
            ),
            name = "gurobi-identity-projection-it"
        )
        return LinearTriadModel(
            impl = basicModel,
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )
    }
}
