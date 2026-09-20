package fuookami.ospf.kotlin.core.solver

import kotlin.math.abs
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.intermediate.UnivariateLinearPiecewiseStructure
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/**
 * 原生 PWL 所需的已转换 primitive 数据。 / Converted primitive data required by native PWL.
 *
 * @property inputKey 输入变量键 / Input variable key
 * @property resultKey 结果变量键 / Result variable key
 * @property xPoints 原生 PWL 输入断点 / Native PWL input breakpoints
 * @property yPoints 原生 PWL 输出断点 / Native PWL output breakpoints
 * @property name 原生 PWL 名称 / Native PWL name
 */
public data class NativePiecewiseData(
    val inputKey: VariableItemKey,
    val resultKey: VariableItemKey,
    val xPoints: DoubleArray,
    val yPoints: DoubleArray,
    val name: String
)

/**
 * 将 PWL 结构校验并转换为 solver 无关的 primitive 数据。 /
 * Validate a PWL structure and convert it into solver-independent primitive data.
 *
 * Solver bounds and fallback selector usage are deliberately excluded here. /
 * 此处有意不处理求解器边界和 fallback 选择变量使用情况。
 *
 * @param structure 待转换的分段线性结构 / Piecewise-linear structure to convert
 * @param maximumMagnitude 允许的数值最大幅值 / Maximum allowed numeric magnitude
 * @return 已校验的 primitive 数据或错误 / Validated primitive data or an error
 */
public fun prepareNativePiecewise(
    structure: UnivariateLinearPiecewiseStructure<*>,
    maximumMagnitude: Double = Double.POSITIVE_INFINITY
): Ret<NativePiecewiseData> {
    return prepareNativePiecewiseTyped(structure, maximumMagnitude)
}

private fun <V> prepareNativePiecewiseTyped(
    structure: UnivariateLinearPiecewiseStructure<V>,
    maximumMagnitude: Double
): Ret<NativePiecewiseData> where V : RealNumber<V>, V : NumberField<V> {
    fun invalid(reason: String): Ret<NativePiecewiseData> {
        return Failed(
            ErrorCode.IllegalArgument,
            "原生 PWL 数据无效：$reason / Invalid native PWL data: $reason"
        )
    }

    if (maximumMagnitude.isNaN() || maximumMagnitude <= 0.0) {
        return invalid("最大幅值必须为正数 / maximum magnitude must be positive")
    }

    val converter = structure.converter ?: return invalid("缺少值转换器 / value converter is missing")
    return try {
        val input = structure.input.monomials.singleOrNull()
            ?: return invalid("输入不是单变量 / input is not a single variable")
        val inputVariable = input.symbol as? AbstractVariableItem<*, *>
            ?: return invalid("输入符号不是变量 / input symbol is not a variable")
        val inputCoefficient = converter.fromValue(input.coefficient).toSolverDouble(
            fieldName = "${structure.name}.inputCoefficient"
        )
        val inputConstant = converter.fromValue(structure.input.constant).toSolverDouble(
            fieldName = "${structure.name}.inputConstant"
        )
        val xPoints = structure.breakpoints.mapIndexed { index, value ->
            converter.fromValue(value).toSolverDouble(
                fieldName = "${structure.name}.breakpoints[$index]"
            )
        }.toDoubleArray()
        val slopes = structure.slopes.mapIndexed { index, value ->
            converter.fromValue(value).toSolverDouble(
                fieldName = "${structure.name}.slopes[$index]"
            )
        }.toDoubleArray()
        val intercepts = structure.intercepts.mapIndexed { index, value ->
            converter.fromValue(value).toSolverDouble(
                fieldName = "${structure.name}.intercepts[$index]"
            )
        }.toDoubleArray()
        fun usable(value: Double): Boolean {
            return value.isFinite() && abs(value) < maximumMagnitude
        }

        if (!usable(inputCoefficient) || inputCoefficient != 1.0 ||
            !usable(inputConstant) || inputConstant != 0.0
        ) {
            return invalid("输入必须是 x = 1 * variable + 0 / input must be x = 1 * variable + 0")
        }
        if (xPoints.size < 2 || slopes.size != xPoints.size - 1 || intercepts.size != slopes.size) {
            return invalid("断点、斜率和截距数量不匹配 / breakpoint, slope, and intercept counts do not match")
        }
        if (xPoints.any { !usable(it) } || slopes.any { !usable(it) } || intercepts.any { !usable(it) }) {
            return invalid("存在非有限数值 / a non-finite numeric value is present")
        }
        if ((1 until xPoints.size).any { index -> xPoints[index - 1] >= xPoints[index] }) {
            return invalid("断点必须严格递增 / breakpoints must be strictly increasing")
        }

        val yPoints = DoubleArray(xPoints.size)
        for (index in slopes.indices) {
            val lowerValue = slopes[index] * xPoints[index] + intercepts[index]
            val upperValue = slopes[index] * xPoints[index + 1] + intercepts[index]
            if (!usable(lowerValue) || !usable(upperValue)) {
                return invalid("分段端点不是有限数值 / a segment endpoint is not finite")
            }
            if (index > 0 && lowerValue != yPoints[index]) {
                return invalid("分段不连续 / piecewise relation is discontinuous")
            }
            if (index == 0) {
                yPoints[0] = lowerValue
            }
            yPoints[index + 1] = upperValue
        }

        Ok(
            NativePiecewiseData(
                inputKey = inputVariable.key,
                resultKey = structure.resultVariable.key,
                xPoints = xPoints,
                yPoints = yPoints,
                name = structure.name
            )
        )
    } catch (error: RuntimeException) {
        invalid(
            "数值转换失败：${error.message ?: error::class.simpleName} / " +
                "numeric conversion failed: ${error.message ?: error::class.simpleName}"
        )
    }
}
