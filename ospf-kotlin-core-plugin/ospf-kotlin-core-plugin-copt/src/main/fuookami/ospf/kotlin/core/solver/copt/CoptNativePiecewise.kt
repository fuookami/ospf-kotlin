package fuookami.ospf.kotlin.core.solver.copt

import kotlin.math.abs
import copt.COPT
import copt.Expr
import copt.Model
import copt.Var
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.intermediate.UnivariateLinearPiecewiseStructure
import fuookami.ospf.kotlin.core.solver.NativePiecewiseData
import fuookami.ospf.kotlin.core.solver.prepareNativePiecewise
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/**
 * 将 COPT PWL 结构转换为受 COPT 有限幅值约束的 primitive 数据。 /
 * Convert a PWL structure into primitive data bounded by COPT's finite sentinel.
 */
internal fun prepareCoptNativePiecewise(
    structure: UnivariateLinearPiecewiseStructure<*>
): Ret<NativePiecewiseData> {
    return prepareNativePiecewise(
        structure = structure,
        maximumMagnitude = COPT.INFINITY
    )
}

/**
 * 在 COPT SDK 写入前批量校验所有 primitive 数据。 /
 * Validate every primitive datum before writing anything through the COPT SDK.
 */
internal fun validateCoptNativePiecewiseData(
    variableKeys: Set<VariableItemKey>,
    data: List<NativePiecewiseData>,
    infinity: Double
): Try {
    if (!infinity.isFinite() || infinity <= 0.0) {
        return Failed(
            ErrorCode.OREngineModelingException,
            "COPT infinity 哨兵无效：$infinity / Invalid COPT infinity sentinel: $infinity"
        )
    }
    val resultKeys = HashSet<VariableItemKey>()
    for (piecewise in data) {
        if (piecewise.inputKey == piecewise.resultKey || !resultKeys.add(piecewise.resultKey)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "COPT PWL 输入或结果键重复：${piecewise.name} / " +
                    "COPT PWL input or result key is duplicated: ${piecewise.name}"
            )
        }
        if (piecewise.xPoints.size < 2 || piecewise.yPoints.size != piecewise.xPoints.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "COPT PWL 断点与函数值数量无效：${piecewise.name} / " +
                    "Invalid COPT PWL breakpoint and value counts: ${piecewise.name}"
            )
        }
        if (piecewise.inputKey !in variableKeys || piecewise.resultKey !in variableKeys) {
            return Failed(
                ErrorCode.IllegalArgument,
                "COPT PWL 变量缺失：${piecewise.name} / " +
                    "Missing COPT PWL variable: ${piecewise.name}"
            )
        }
        if (piecewise.xPoints.any { value -> !value.isFinite() || abs(value) >= infinity } ||
            piecewise.yPoints.any { value -> !value.isFinite() || abs(value) >= infinity }
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "COPT PWL 存在超出有限范围的数值：${piecewise.name} / " +
                    "COPT PWL contains a non-finite or sentinel-bound value: ${piecewise.name}"
            )
        }
        if ((1 until piecewise.xPoints.size).any { index ->
                piecewise.xPoints[index - 1] >= piecewise.xPoints[index]
            }
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "COPT PWL 断点必须严格递增：${piecewise.name} / " +
                    "COPT PWL breakpoints must be strictly increasing: ${piecewise.name}"
            )
        }
    }
    return ok
}

/**
 * 使用 COPT lambda、链接约束和 SOS2 写入连续一元 PWL。 /
 * Write a continuous univariate PWL with COPT lambdas, linking rows, and SOS2.
 *
 * 已写入的部分不在此处回滚，调用方必须丢弃整个 COPT 模型。 /
 * Partial writes are not rolled back here; the caller must discard the whole COPT model.
 */
internal fun addCoptNativePiecewise(
    model: Model,
    variables: Map<VariableItemKey, Var>,
    data: List<NativePiecewiseData>
): Try {
    if (!coptSupportsNativePiecewise()) {
        return Failed(
            ErrorCode.OREngineModelingException,
            "当前 COPT SDK 未暴露 addSos(Var[], double[], int): Sos / " +
                "The current COPT SDK does not expose addSos(Var[], double[], int): Sos"
        )
    }
    when (val validation = validateCoptNativePiecewiseData(variables.keys, data, COPT.INFINITY)) {
        is Ok -> {}
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
    }

    return try {
        for ((piecewiseIndex, piecewise) in data.withIndex()) {
            val input = variables[piecewise.inputKey]
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "COPT PWL 输入变量映射缺失：${piecewise.name} / " +
                        "Missing COPT PWL input variable mapping: ${piecewise.name}"
                )
            val result = variables[piecewise.resultKey]
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "COPT PWL 结果变量映射缺失：${piecewise.name} / " +
                        "Missing COPT PWL result variable mapping: ${piecewise.name}"
                )
            val lambda = piecewise.xPoints.indices.map { index ->
                model.addVar(
                    0.0,
                    1.0,
                    0.0,
                    COPT.CONTINUOUS,
                    "copt-pwl-$piecewiseIndex-lambda-$index"
                )
            }.toTypedArray()

            val nativePrefix = "copt-pwl-$piecewiseIndex"

            val sum = Expr()
            try {
                lambda.forEach { variable -> sum.addTerm(variable, 1.0) }
                model.addConstr(
                    sum,
                    COPT.EQUAL,
                    1.0,
                    "$nativePrefix-lambda-sum"
                )
            } finally {
                sum.dispose()
            }

            val xLink = Expr()
            try {
                xLink.addTerm(input, 1.0)
                piecewise.xPoints.forEachIndexed { index, point ->
                    xLink.addTerm(lambda[index], -point)
                }
                model.addConstr(
                    xLink,
                    COPT.EQUAL,
                    0.0,
                    "$nativePrefix-x-link"
                )
            } finally {
                xLink.dispose()
            }

            val yLink = Expr()
            try {
                yLink.addTerm(result, 1.0)
                piecewise.yPoints.forEachIndexed { index, point ->
                    yLink.addTerm(lambda[index], -point)
                }
                model.addConstr(
                    yLink,
                    COPT.EQUAL,
                    0.0,
                    "$nativePrefix-y-link"
                )
            } finally {
                yLink.dispose()
            }

            model.addSos(
                lambda,
                piecewise.xPoints.copyOf(),
                COPT.SOS_TYPE2
            )
        }
        ok
    } catch (error: LinkageError) {
        Failed(
            ErrorCode.OREngineModelingException,
            "COPT PWL SDK API 不可用：${error.message ?: error::class.simpleName} / " +
                "COPT PWL SDK API is unavailable: ${error.message ?: error::class.simpleName}"
        )
    } catch (error: Exception) {
        Failed(
            ErrorCode.OREngineModelingException,
            "COPT PWL 写入失败：${error.message ?: error::class.simpleName} / " +
                "COPT PWL write failed: ${error.message ?: error::class.simpleName}"
        )
    }
}
