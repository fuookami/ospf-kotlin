package fuookami.ospf.kotlin.core.solver.gurobi11

import com.gurobi.gurobi.GRB
import com.gurobi.gurobi.GRBModel
import com.gurobi.gurobi.GRBVar
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.intermediate.AbsStructure
import fuookami.ospf.kotlin.core.solver.NativeAbsData
import fuookami.ospf.kotlin.core.solver.prepareNativeAbs
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** Gurobi 11 原生 ABS 数据别名。 / Gurobi 11 native ABS data alias. */
internal typealias GurobiNativeAbsData = NativeAbsData

/** 使用 Gurobi 11 的数值边界准备 ABS 数据。 / Prepare ABS data with Gurobi 11 numeric limits. */
internal fun prepareGurobiNativeAbs(
    structure: AbsStructure<*>
): Ret<GurobiNativeAbsData> {
    return prepareNativeAbs(
        structure = structure,
        maximumMagnitude = GRB.INFINITY
    )
}

/** 隔离 Gurobi 11 SDK ABS 写入，供失败路径测试注入。 / Isolate Gurobi 11 SDK ABS writes for failure-path tests. */
internal fun interface GurobiNativeAbsWriter {
    fun add(data: GurobiNativeAbsData)
}

/** 先完整校验 ABS 数据，再按顺序写入 Gurobi 11。 / Validate all ABS data before writing it to Gurobi 11. */
internal fun writeGurobiNativeAbs(
    variableKeys: Set<VariableItemKey>,
    structures: List<AbsStructure<*>>,
    writer: GurobiNativeAbsWriter
): Try {
    val prepared = ArrayList<GurobiNativeAbsData>(structures.size)
    for (structure in structures) {
        when (val result = prepareGurobiNativeAbs(structure)) {
            is Ok -> prepared += result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    for (data in prepared) {
        if (data.inputKey !in variableKeys || data.resultKey !in variableKeys) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Gurobi 11 ABS 变量缺失：${data.name} / Missing Gurobi 11 ABS variable: ${data.name}"
            )
        }
    }
    for (data in prepared) {
        try {
            writer.add(data)
        } catch (error: LinkageError) {
            return Failed(
                ErrorCode.OREngineModelingException,
                "Gurobi 11 ABS API 不可用：${error.message ?: error::class.simpleName} / " +
                    "Gurobi 11 ABS API is unavailable: ${error.message ?: error::class.simpleName}"
            )
        } catch (error: Exception) {
            return Failed(
                ErrorCode.OREngineModelingException,
                "Gurobi 11 ABS 写入失败：${error.message ?: error::class.simpleName} / " +
                    "Gurobi 11 ABS write failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }
    return ok
}

/** 使用 Gurobi 11 精确 addGenConstrAbs 签名写入 ABS。 / Write ABS with Gurobi 11's exact addGenConstrAbs signature. */
internal fun addGurobiNativeAbs(
    model: GRBModel,
    variables: Map<VariableItemKey, GRBVar>,
    structures: List<AbsStructure<*>>
): Try {
    return writeGurobiNativeAbs(
        variableKeys = variables.keys,
        structures = structures,
        writer = GurobiNativeAbsWriter { data ->
            model.addGenConstrAbs(
                variables.getValue(data.resultKey),
                variables.getValue(data.inputKey),
                data.name
            )
        }
    )
}
