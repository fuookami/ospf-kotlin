package fuookami.ospf.kotlin.core.solver

import kotlin.math.abs
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.intermediate.AbsStructure
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/**
 * 原生绝对值关系的数据。 / Data for a native absolute-value relation.
 *
 * @property inputKey 输入变量键 / Input variable key
 * @property resultKey 结果变量键 / Result variable key
 * @property positiveBigM fallback 正部上界 / Fallback positive-part bound
 * @property negativeBigM fallback 负部上界 / Fallback negative-part bound
 * @property name 函数名称 / Function name
 */
data class NativeAbsData(
    val inputKey: VariableItemKey,
    val resultKey: VariableItemKey,
    val positiveBigM: Double,
    val negativeBigM: Double,
    val name: String
)

/**
 * 校验并转换绝对值 primitive；辅助变量归属由编号前检查。 /
 * Validate and convert an absolute-value primitive; helper ownership is checked before indexing.
 *
 * @param structure 绝对值结构 / Absolute-value structure
 * @param maximumMagnitude SDK 支持的数值幅值上限 / Numeric magnitude limit supported by the SDK
 * @return primitive 数据或转换错误 / Primitive data or a conversion error
 */
fun prepareNativeAbs(
    structure: AbsStructure<*>,
    maximumMagnitude: Double = Double.POSITIVE_INFINITY
): Ret<NativeAbsData> {
    return prepareNativeAbsTyped(structure, maximumMagnitude)
}

private fun <V> prepareNativeAbsTyped(
    structure: AbsStructure<V>,
    maximumMagnitude: Double
): Ret<NativeAbsData> where V : RealNumber<V>, V : NumberField<V> {
    fun invalid(reason: String): Ret<NativeAbsData> = Failed(
        ErrorCode.IllegalArgument,
        "原生 ABS 数据无效：$reason / Invalid native ABS data: $reason"
    )
    if (maximumMagnitude.isNaN() || maximumMagnitude <= 0.0) {
        return invalid("maximum magnitude must be positive")
    }
    return try {
        val input = structure.input.monomials.singleOrNull()
            ?: return invalid("input must be a single variable")
        val variable = input.symbol as? AbstractVariableItem<*, *>
            ?: return invalid("input symbol must be a variable")
        val converter = structure.converter
        if (input.coefficient.compareTo(converter.one) != 0 ||
            structure.input.constant.compareTo(converter.zero) != 0
        ) {
            return invalid("input must be 1 * variable + 0")
        }
        if (converter.fromValue(input.coefficient).toSolverDouble(
                fieldName = "${structure.name}.inputCoefficient"
            ) != 1.0 || converter.fromValue(structure.input.constant).toSolverDouble(
                fieldName = "${structure.name}.inputConstant"
            ) != 0.0
        ) {
            return invalid("converted input must be 1 * variable + 0")
        }
        val keys = listOf(
            variable.key,
            structure.resultVariable.key,
            structure.positiveVariable.key,
            structure.negativeVariable.key,
            structure.signVariable.key
        )
        if (keys.toSet().size != keys.size) {
            return invalid("input, result, and helpers must have distinct keys")
        }
        val positiveBigM = converter.fromValue(structure.positiveBigM).toSolverDouble(
            fieldName = "${structure.name}.positiveBigM"
        )
        val negativeBigM = converter.fromValue(structure.negativeBigM).toSolverDouble(
            fieldName = "${structure.name}.negativeBigM"
        )
        if (listOf(positiveBigM, negativeBigM).any {
                !it.isFinite() || it < 0.0 || abs(it) >= maximumMagnitude
            }
        ) {
            return invalid("fallback bounds must be finite, nonnegative, and representable")
        }
        Ok(
            NativeAbsData(
                inputKey = variable.key,
                resultKey = structure.resultVariable.key,
                positiveBigM = positiveBigM,
                negativeBigM = negativeBigM,
                name = structure.name
            )
        )
    } catch (error: RuntimeException) {
        invalid("numeric conversion failed: ${error.message ?: error::class.simpleName}")
    }
}
