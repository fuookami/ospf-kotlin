package fuookami.ospf.kotlin.framework.solver.remote.adapter.ospf

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.Objective
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.BasicQuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjectiveCell
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticObjectiveCell
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseQuadraticMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseQuadraticVector
import fuookami.ospf.kotlin.core.model.intermediate.SparseVector
import fuookami.ospf.kotlin.core.variable.Binary
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.core.variable.Integer
import fuookami.ospf.kotlin.framework.solver.remote.domain.SerializedConstraintSign
import fuookami.ospf.kotlin.framework.solver.remote.domain.SerializedObjectiveCategory
import fuookami.ospf.kotlin.framework.solver.remote.domain.SerializedVariableType
import fuookami.ospf.kotlin.math.algebra.number.Flt64

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
                    sources = listOf(ConstraintSource.Origin)
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
                constant = Flt64(1.0)
            )
        )

        val serialized = OspfRemoteModelSerializer.serialize(model)

        assertEquals("linear-model", serialized.name)
        assertEquals(SerializedVariableType.BINARY, serialized.variables[0].type)
        assertEquals(SerializedVariableType.CONTINUOUS, serialized.variables[1].type)
        assertEquals(SerializedConstraintSign.LESS_EQUAL, serialized.constraints.single().sign)
        assertEquals(Flt64(5.0), serialized.constraints.single().rhs)
        assertEquals(listOf(0, 1), serialized.constraints.single().cells.map { it.colIndex })
        assertEquals(SerializedObjectiveCategory.MINIMIZE, serialized.objective.category)
        assertEquals(Flt64(1.0), serialized.objective.constant)
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
                    sources = listOf(ConstraintSource.Origin)
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
                constant = Flt64(2.0)
            )
        )

        val serialized = OspfRemoteModelSerializer.serialize(model)

        assertEquals("quadratic-model", serialized.name)
        assertEquals(SerializedVariableType.INTEGER, serialized.variables[0].type)
        assertEquals(SerializedConstraintSign.GREATER_EQUAL, serialized.quadraticConstraints.single().sign)
        assertEquals(1, serialized.quadraticConstraints.single().linearCells.size)
        assertEquals(1, serialized.quadraticConstraints.single().quadraticCells.size)
        assertEquals(SerializedObjectiveCategory.MAXIMIZE, serialized.objective.category)
        assertEquals(1, serialized.objective.linearCells.size)
        assertEquals(1, serialized.objective.quadraticCells.size)
        assertEquals(Flt64(2.0), serialized.objective.constant)
    }

    private fun variable(
        index: Int,
        name: String,
        type: fuookami.ospf.kotlin.core.variable.VariableType<*>
    ): Variable {
        return Variable(
            index = index,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = type,
            origin = null,
            name = name
        )
    }
}
