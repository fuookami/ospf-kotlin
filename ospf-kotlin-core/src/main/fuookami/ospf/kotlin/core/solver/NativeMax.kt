package fuookami.ospf.kotlin.core.solver

import kotlin.math.abs
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.intermediate.MaxStructure
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * MAX 的仿射输入。 / An affine input to MAX.
 *
 * @property terms 按变量键合并的系数 / Coefficients combined by variable key
 * @property constant 常数项 / Constant term
 * @property lowerBound 当前输入下界 / Current input lower bound
 * @property upperBound 当前输入上界 / Current input upper bound
 */
data class NativeMaxInputData(
    val terms: Map<VariableItemKey, Double>,
    val constant: Double,
    val lowerBound: Double,
    val upperBound: Double
)

/**
 * 已校验的 MAX 原语数据。 / Validated MAX primitive data.
 *
 * @property inputs 有限仿射输入 / Finite affine inputs
 * @property resultKey 对外结果键 / Public result key
 * @property selectorKeys 待恢复的选择变量键 / Selector keys to restore
 * @property bigMValues 覆盖输入域的 fallback Big-M / Fallback Big-M values covering the input domain
 * @property name 函数名 / Function name
 * @property minimum 是否计算最小值 / Whether to compute the minimum
 */
data class NativeMaxData(
    val inputs: List<NativeMaxInputData>,
    val resultKey: VariableItemKey,
    val selectorKeys: List<VariableItemKey>,
    val bigMValues: List<Double>,
    val name: String,
    val minimum: Boolean = false
)

/**
 * 校验 MAX 数据及原 Big-M 对输入域的覆盖。 / Validate MAX data and original Big-M coverage.
 *
 * @param structure MAX 结构 / MAX structure
 * @param maximumMagnitude SDK 数值幅值上限 / SDK numeric magnitude limit
 * @return 原语数据或错误 / Primitive data or an error
 */
fun prepareNativeMax(
    structure: MaxStructure<*>,
    maximumMagnitude: Double = Double.POSITIVE_INFINITY
): Ret<NativeMaxData> {
    return prepareNativeMaxTyped(structure, maximumMagnitude)
}

private fun <V> prepareNativeMaxTyped(
    structure: MaxStructure<V>,
    maximumMagnitude: Double
): Ret<NativeMaxData> where V : RealNumber<V>, V : NumberField<V> {
    fun invalid(reason: String): Ret<NativeMaxData> = Failed(
        ErrorCode.IllegalArgument,
        "原生 MAX 数据无效：$reason / Invalid native MAX data: $reason"
    )
    fun usable(value: Double): Boolean = value.isFinite() && abs(value) < maximumMagnitude

    if (maximumMagnitude.isNaN() || maximumMagnitude <= 0.0) {
        return invalid("maximum magnitude must be positive")
    }
    val count = structure.inputs.size
    if (count == 0 || structure.selectorVariables.size != count || structure.bigMValues.size != count) {
        return invalid("input, selector, and Big-M counts must match and be nonempty")
    }
    val selectorKeys = structure.selectorVariables.map { it.key }
    val declarationKeys = selectorKeys + structure.resultVariable.key
    if (declarationKeys.size != declarationKeys.toSet().size) {
        return invalid("result and selector keys must be distinct")
    }
    val converter = structure.converter
    return try {
        val inputs = ArrayList<NativeMaxInputData>(count)
        for ((index, polynomial) in structure.inputs.withIndex()) {
            val constant = converter.fromValue(polynomial.constant).toSolverDouble(
                fieldName = "${structure.name}.inputs[$index].constant"
            )
            if (!usable(constant)) {
                return invalid("input constant is not finite or representable")
            }
            val terms = LinkedHashMap<VariableItemKey, Double>()
            val variables = LinkedHashMap<VariableItemKey, AbstractVariableItem<*, *>>()
            for (monomial in polynomial.monomials) {
                val variable = monomial.symbol as? AbstractVariableItem<*, *>
                    ?: return invalid("input contains an unexpanded symbol")
                if (variable.key in declarationKeys) {
                    return invalid("input references its result or selector")
                }
                val coefficient = converter.fromValue(monomial.coefficient).toSolverDouble(
                    fieldName = "${structure.name}.inputs[$index].coefficient"
                )
                val combined = (terms[variable.key] ?: 0.0) + coefficient
                if (!usable(coefficient) || !usable(combined)) {
                    return invalid("input coefficient is not finite or representable")
                }
                terms[variable.key] = combined
                variables[variable.key] = variable
            }
            val nonzeroTerms = terms.filterValues { it != 0.0 }
            var lowerBound = constant
            var upperBound = constant
            for ((key, coefficient) in nonzeroTerms) {
                val variable = variables.getValue(key)
                val lower = variable.lowerBound?.value?.unwrap()?.toSolverDouble(
                    fieldName = "${structure.name}.inputs[$index].lowerBound"
                ) ?: return invalid("input lower bound is missing")
                val upper = variable.upperBound?.value?.unwrap()?.toSolverDouble(
                    fieldName = "${structure.name}.inputs[$index].upperBound"
                ) ?: return invalid("input upper bound is missing")
                if (!usable(lower) || !usable(upper) || lower > upper) {
                    return invalid("input variable bounds are invalid")
                }
                lowerBound += coefficient * if (coefficient >= 0.0) lower else upper
                upperBound += coefficient * if (coefficient >= 0.0) upper else lower
                if (!usable(lowerBound) || !usable(upperBound)) {
                    return invalid("affine input bounds are not finite or representable")
                }
            }
            inputs += NativeMaxInputData(
                terms = nonzeroTerms,
                constant = constant,
                lowerBound = lowerBound,
                upperBound = upperBound
            )
        }
        val maximumUpper = inputs.maxOf { it.upperBound }
        val bigMValues = structure.bigMValues.mapIndexed { index, value ->
            converter.fromValue(value).toSolverDouble(
                fieldName = "${structure.name}.bigM[$index]"
            )
        }
        for ((index, bigM) in bigMValues.withIndex()) {
            val required = maximumUpper - inputs[index].lowerBound
            if (!usable(bigM) || bigM <= 0.0 || !required.isFinite() || bigM < required) {
                return invalid("fallback Big-M does not cover the current input domain")
            }
        }
        Ok(
            NativeMaxData(
                inputs = inputs,
                resultKey = structure.resultVariable.key,
                selectorKeys = selectorKeys,
                bigMValues = bigMValues,
                name = structure.name,
                minimum = structure.minimum
            )
        )
    } catch (error: RuntimeException) {
        invalid("numeric conversion failed: ${error.message ?: error::class.simpleName}")
    }
}
