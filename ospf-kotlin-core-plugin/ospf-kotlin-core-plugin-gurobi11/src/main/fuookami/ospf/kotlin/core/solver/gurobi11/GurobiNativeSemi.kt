package fuookami.ospf.kotlin.core.solver.gurobi11

import com.gurobi.gurobi.GRB
import com.gurobi.gurobi.GRBModel
import com.gurobi.gurobi.GRBVar
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.intermediate.SemiStructure
import fuookami.ospf.kotlin.core.variable.VariableItemKey

internal data class GurobiNativeSemiData(
    val resultKey: VariableItemKey,
    val lowerBound: Double,
    val upperBound: Double,
    val name: String
)

internal fun prepareGurobiNativeSemi(structure: SemiStructure<*>): Ret<GurobiNativeSemiData> {
    return try {
        val lower = structure.converter.fromValue(structure.lowerBound).toDouble()
        val upper = structure.converter.fromValue(structure.upperBound).toDouble()
        if (!lower.isFinite() || !upper.isFinite() || lower <= 0.0 || lower > upper) {
            Failed(ErrorCode.IllegalArgument, "Gurobi 11 SEMICONT bounds are invalid / Gurobi 11 SEMICONT 边界无效")
        } else {
            Ok(
                GurobiNativeSemiData(
                    resultKey = structure.resultVariable.key,
                    lowerBound = lower,
                    upperBound = upper,
                    name = structure.name
                )
            )
        }
    } catch (error: RuntimeException) {
        Failed(ErrorCode.IllegalArgument, "Gurobi 11 SEMICONT data conversion failed: ${error.message} / Gurobi 11 SEMICONT 数据转换失败：${error.message}")
    }
}

internal fun addGurobiNativeSemi(
    model: GRBModel,
    variables: Map<VariableItemKey, GRBVar>,
    structures: List<SemiStructure<*>>
): Try {
    val prepared = ArrayList<GurobiNativeSemiData>(structures.size)
    for (structure in structures) {
        when (val result = prepareGurobiNativeSemi(structure)) {
            is Ok -> prepared += result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    for (data in prepared) {
        val variable = variables[data.resultKey] ?: return Failed(
            ErrorCode.IllegalArgument,
            "Gurobi 11 SEMICONT result variable is missing / Gurobi 11 SEMICONT 结果变量缺失"
        )
        try {
            variable.set(GRB.DoubleAttr.LB, data.lowerBound)
            variable.set(GRB.DoubleAttr.UB, data.upperBound)
            variable.set(GRB.CharAttr.VType, GRB.SEMICONT)
        } catch (error: Exception) {
            return Failed(
                ErrorCode.OREngineModelingException,
                "Gurobi 11 SEMICONT write failed: ${error.message} / Gurobi 11 SEMICONT 写入失败：${error.message}"
            )
        }
    }
    model.update()
    return ok
}
