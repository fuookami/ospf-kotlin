package fuookami.ospf.kotlin.core.solver.gurobi

import gurobi.GRB
import gurobi.GRBModel
import gurobi.GRBVar
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.intermediate.UnivariateLinearPiecewiseStructure
import fuookami.ospf.kotlin.core.solver.NativePiecewiseData
import fuookami.ospf.kotlin.core.solver.prepareNativePiecewise
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/** Gurobi 原生 PWL 数据兼容别名。 / Compatibility alias for Gurobi native PWL data. */
internal typealias GurobiNativePiecewiseData = NativePiecewiseData

/**
 * 将 PWL 结构校验并转换为 Gurobi 无关的 primitive 数据。 /
 * Validate a PWL structure and convert it into Gurobi-independent primitive data.
 *
 * Solver bounds and fallback selector usage are deliberately excluded here. /
 * 此处有意不处理求解器边界和 fallback 选择变量使用情况。
 *
 * @param structure 待转换的分段线性结构 / Piecewise-linear structure to convert
 * @return 已校验的 primitive 数据或错误 / Validated primitive data or an error
 */
internal fun prepareGurobiNativePiecewise(
    structure: UnivariateLinearPiecewiseStructure<*>
): Ret<GurobiNativePiecewiseData> {
    return prepareNativePiecewise(structure, maximumMagnitude = GRB.INFINITY)
}

/** 小型原生写入注入点，用于隔离 SDK 调用和失败测试。 / Small native-write seam for SDK isolation and failure tests. */
internal fun interface GurobiNativePiecewiseWriter {
    fun add(data: GurobiNativePiecewiseData)
}

/**
 * 校验所有结构和变量键后，按顺序写入每个 PWL。 /
 * Validate every structure and variable key, then write each PWL in order.
 *
 * This function does not roll back prior SDK writes; its caller owns whole-model disposal on failure. /
 * 此函数不回滚已经完成的 SDK 写入；失败时由调用方负责丢弃整个模型。
 *
 * @param variableKeys solver model 中可用变量键 / Variable keys available in the solver model
 * @param structures 待写入的分段线性结构 / Piecewise-linear structures to write
 * @param writer 原生写入操作 / Native write operation
 * @return 全部写入成功或首个失败 / Success or the first failure
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
                "Gurobi PWL 变量缺失：${data.name} / Missing Gurobi PWL variable: ${data.name}"
            )
        }
    }
    for (data in prepared) {
        try {
            writer.add(data)
        } catch (error: LinkageError) {
            return Failed(
                ErrorCode.OREngineModelingException,
                "Gurobi PWL API 不可用：${error.message ?: error::class.simpleName} / " +
                    "Gurobi PWL API is unavailable: ${error.message ?: error::class.simpleName}"
            )
        } catch (error: Exception) {
            return Failed(
                ErrorCode.OREngineModelingException,
                "Gurobi PWL 写入失败：${error.message ?: error::class.simpleName} / " +
                    "Gurobi PWL write failed: ${error.message ?: error::class.simpleName}"
            )
        }
    }
    return ok
}

/**
 * 使用 Gurobi SDK 写入连续 PWL 约束，并保留结果变量关系。 /
 * Write continuous PWL constraints through the Gurobi SDK while preserving the result-variable relation.
 *
 * @param model Gurobi 模型 / Gurobi model
 * @param variables solver 变量键映射 / Solver variable-key mapping
 * @param structures 待写入的分段线性结构 / Piecewise-linear structures to write
 * @return 全部写入成功或 SDK 错误 / Success or an SDK error
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
