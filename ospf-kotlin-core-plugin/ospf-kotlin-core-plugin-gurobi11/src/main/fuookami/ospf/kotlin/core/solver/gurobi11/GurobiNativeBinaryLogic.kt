package fuookami.ospf.kotlin.core.solver.gurobi11

import com.gurobi.gurobi.*
import fuookami.ospf.kotlin.core.model.intermediate.BinaryLogicOperation
import fuookami.ospf.kotlin.core.model.intermediate.BinaryLogicStructure
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/** Gurobi 11 binary AND/OR native writer. / Gurobi 11 二值 AND/OR 原生写入器。 */
internal fun addGurobiNativeBinaryLogic(
    model: GRBModel,
    variables: Map<VariableItemKey, GRBVar>,
    structures: List<BinaryLogicStructure<*>>
): Try {
    return try {
        for (structure in structures) {
            if (structure.operation == BinaryLogicOperation.Xor) return Failed(ErrorCode.IllegalArgument, "Exact-one XOR cannot map to parity XOR.")
            val result = variables[structure.resultVariable.key] ?: return Failed(ErrorCode.IllegalArgument, "Binary logic result column is missing.")
            val inputs = structure.inputs.map { variables[it.key] ?: return Failed(ErrorCode.IllegalArgument, "Binary logic input column is missing.") }.toTypedArray()
            if (inputs.isEmpty() || result.get(GRB.CharAttr.VType) != GRB.BINARY || inputs.any { it.get(GRB.CharAttr.VType) != GRB.BINARY }) {
                return Failed(ErrorCode.IllegalArgument, "Binary logic columns must be binary variables.")
            }
            when (structure.operation) {
                BinaryLogicOperation.And -> model.addGenConstrAnd(result, inputs, structure.name)
                BinaryLogicOperation.Or -> model.addGenConstrOr(result, inputs, structure.name)
                BinaryLogicOperation.Not -> {
                    val equality = GRBLinExpr()
                    equality.addTerm(1.0, result)
                    equality.addTerm(1.0, inputs.single())
                    model.addConstr(equality, GRB.EQUAL, 1.0, structure.name)
                }
                BinaryLogicOperation.Xor -> Unit
            }
        }
        model.update()
        ok
    } catch (error: Exception) {
        Failed(ErrorCode.OREngineModelingException, "Gurobi 11 binary logic write failed: ${error.message}")
    }
}
