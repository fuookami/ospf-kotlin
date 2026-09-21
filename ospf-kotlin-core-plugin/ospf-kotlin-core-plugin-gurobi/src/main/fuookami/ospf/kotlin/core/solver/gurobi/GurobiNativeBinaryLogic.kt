package fuookami.ospf.kotlin.core.solver.gurobi

import gurobi.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.intermediate.BinaryLogicOperation
import fuookami.ospf.kotlin.core.model.intermediate.BinaryLogicStructure
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** Gurobi binary AND/OR native writer. / Gurobi 二值 AND/OR 原生写入器。 */
internal fun addGurobiNativeBinaryLogic(
    model: GRBModel,
    variables: Map<VariableItemKey, GRBVar>,
    structures: List<BinaryLogicStructure<*>>
): Try {
    return try {
        for (structure in structures) {
            if (structure.operation == BinaryLogicOperation.Xor) {
                return Failed(ErrorCode.IllegalArgument, "XOR 是恰好一个语义，不能映射为 Gurobi parity XOR。 / Exact-one XOR cannot map to Gurobi parity XOR.")
            }
            val result = variables[structure.resultVariable.key]
                ?: return Failed(ErrorCode.IllegalArgument, "逻辑结果列缺失。 / Binary logic result column is missing.")
            val inputs = structure.inputs.map { variables[it.key] ?: return Failed(
                ErrorCode.IllegalArgument, "逻辑输入列缺失。 / Binary logic input column is missing."
            ) }.toTypedArray()
            if (inputs.isEmpty() || result.get(GRB.CharAttr.VType) != GRB.BINARY ||
                inputs.any { it.get(GRB.CharAttr.VType) != GRB.BINARY }
            ) return Failed(ErrorCode.IllegalArgument, "逻辑列必须为二值变量。 / Binary logic columns must be binary variables.")
            when (structure.operation) {
                BinaryLogicOperation.And -> model.addGenConstrAnd(
                    result,
                    inputs,
                    structure.name
                )
                BinaryLogicOperation.Or -> model.addGenConstrOr(
                    result,
                    inputs,
                    structure.name
                )
                BinaryLogicOperation.Not -> {
                    val equality = GRBLinExpr()
                    equality.addTerm(1.0, result)
                    equality.addTerm(1.0, inputs.single())
                    model.addConstr(
                        equality,
                        GRB.EQUAL,
                        1.0,
                        structure.name
                    )
                }
                BinaryLogicOperation.Xor -> Unit
            }
        }
        model.update()
        ok
    } catch (error: Exception) {
        Failed(ErrorCode.OREngineModelingException, "Gurobi 二值逻辑写入失败：${error.message} / Gurobi binary logic write failed")
    }
}
