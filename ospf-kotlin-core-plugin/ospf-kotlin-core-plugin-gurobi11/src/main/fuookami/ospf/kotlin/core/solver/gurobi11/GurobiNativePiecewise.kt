package fuookami.ospf.kotlin.core.solver.gurobi11

import com.gurobi.gurobi.GRB
import com.gurobi.gurobi.GRBModel
import com.gurobi.gurobi.GRBVar
import fuookami.ospf.kotlin.core.model.intermediate.UnivariateLinearPiecewiseStructure
import fuookami.ospf.kotlin.core.solver.NativePiecewiseData
import fuookami.ospf.kotlin.core.solver.prepareNativePiecewise
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/** Gurobi 11 原生 PWL 数据别名。 / Gurobi 11 native PWL data alias. */
internal typealias GurobiNativePiecewiseData = NativePiecewiseData

private val GUROBI11_MAXIMUM_MAGNITUDE = GRB.INFINITY

/**
 * 使用 core primitive 准备 Gurobi 11 PWL 数据。 / Prepare Gurobi 11 PWL data through the core primitive.
 *
 * @param structure 待准备的分段线性结构 / Piecewise-linear structure to prepare
 * @return 已校验的原生数据或错误 / Validated native data or an error
 */
internal fun prepareGurobiNativePiecewise(
    structure: UnivariateLinearPiecewiseStructure<*>
): Ret<GurobiNativePiecewiseData> {
    return prepareNativePiecewise(
        structure = structure,
        maximumMagnitude = GUROBI11_MAXIMUM_MAGNITUDE
    )
}

/** 隔离 Gurobi 11 SDK 写入，供失败路径测试注入。 / Isolate Gurobi 11 SDK writes for failure-path tests. */
internal fun interface GurobiNativePiecewiseWriter {
    fun add(data: GurobiNativePiecewiseData)
}

/**
 * 先完整校验，再按顺序写入所有 PWL。 / Validate all PWL data before writing them in order.
 *
 * 已完成的 SDK 写入不在此处回滚，调用方必须在失败时释放整个模型。 /
 * Completed SDK writes are not rolled back here; the caller must dispose the whole model on failure.
 *
 * @param variableKeys solver 模型中的变量键 / Variable keys in the solver model
 * @param structures 待写入的函数结构 / Function structures to write
 * @param writer 原生 SDK 写入器 / Native SDK writer
 * @return 全部成功或首个错误 / Success or the first error
 */
internal fun writeGurobiNativePiecewise(
    variableKeys: Set<VariableItemKey>,
    structures: List<UnivariateLinearPiecewiseStructure<*>>,
    writer: GurobiNativePiecewiseWriter
): Try {
    val prepared = ArrayList<GurobiNativePiecewiseData>(structures.size)
    for (structure in structures) {
        when (val result = prepareGurobiNativePiecewise(structure)) {
            is Ok -> prepared.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    for (data in prepared) {
        if (data.inputKey !in variableKeys || data.resultKey !in variableKeys) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Gurobi 11 PWL 变量缺失：${data.name} / Missing Gurobi 11 PWL variable: ${data.name}"
            )
        }
    }
    for (data in prepared) {
        try {
            writer.add(data)
        } catch (error: LinkageError) {
            return Failed(
                ErrorCode.OREngineModelingException,
                "Gurobi 11 PWL API 不可用：${error.message ?: error::class.simpleName} / " +
                    "Gurobi 11 PWL API is unavailable: ${error.message ?: error::class.simpleName}"
            )
        } catch (error: Exception) {
            return Failed(
                ErrorCode.OREngineModelingException,
                "Gurobi 11 PWL 写入失败：${error.message ?: error::class.simpleName} / " +
                    "Gurobi 11 PWL write failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }
    return ok
}

/**
 * 使用 Gurobi 11 SDK 写入连续 PWL 约束，并保留结果变量。 / Write continuous PWL constraints through the Gurobi 11 SDK while preserving result variables.
 *
 * @param model Gurobi 11 模型 / Gurobi 11 model
 * @param variables solver 变量键映射 / Solver variable-key mapping
 * @param structures 待写入的函数结构 / Function structures to write
 * @return 全部成功或 SDK 错误 / Success or an SDK error
 */
internal fun addGurobiNativePiecewise(
    model: GRBModel,
    variables: Map<VariableItemKey, GRBVar>,
    structures: List<UnivariateLinearPiecewiseStructure<*>>
): Try {
    return writeGurobiNativePiecewise(
        variableKeys = variables.keys,
        structures = structures,
        writer = GurobiNativePiecewiseWriter { data ->
            model.addGenConstrPWL(
                variables.getValue(data.inputKey),
                variables.getValue(data.resultKey),
                data.xPoints,
                data.yPoints,
                data.name
            )
        }
    )
}
