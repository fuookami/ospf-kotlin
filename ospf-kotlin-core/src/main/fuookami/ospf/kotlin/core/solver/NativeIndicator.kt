package fuookami.ospf.kotlin.core.solver

import kotlin.math.abs
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.intermediate.IndicatorStructure
import fuookami.ospf.kotlin.core.model.intermediate.ConditionalValue
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/**
 * 已验证的双向正数指示数据。 / Validated bidirectional positive-indicator data.
 * @property terms 仿射系数 / Affine coefficients
 * @property constant 仿射常数 / Affine constant
 * @property tolerance 真分支阈值 / True-branch threshold
 * @property bigM 原 fallback 的范围参数 / Original fallback range parameter
 * @property resultKey 保留的二值结果键 / Retained binary result key
 * @property name 名称 / Name
 * @property positiveOnZero 由结果 0 激活正分支 / Activate the positive branch on result zero
 * @property lowerBound 原 fallback 输入下界 / Original fallback input lower bound
 * @property upperBound 原 fallback 输入上界 / Original fallback input upper bound
 * @property equivalentResultKeys 保留的等价结果列键 / Retained equivalent result column keys
 * @property conditionalValue 条件数值结果 / Conditional numeric result
 * @property impliedCondition 仅由前件激活的后件数据 / Consequent data activated only by the antecedent
 * @property zeroBand 零容差带及保留方向列 / Zero band and retained side column
 * @property conjunction 第二个条件及保留的 AND 结果 / Second condition and retained AND result
 * @property difference 第二个条件及互斥差值结果 / Second condition and exclusive difference result
 */
data class NativeIndicatorData(
    val terms: Map<VariableItemKey, Double>,
    val constant: Double,
    val tolerance: Double,
    val bigM: Double,
    val resultKey: VariableItemKey,
    val name: String,
    val positiveOnZero: Boolean = false,
    val lowerBound: Double = -bigM,
    val upperBound: Double = bigM,
    val equivalentResultKeys: List<VariableItemKey> = emptyList(),
    val conditionalValue: NativeConditionalValueData? = null,
    val impliedCondition: NativeConditionalValueData? = null,
    val zeroBand: NativeZeroBandData? = null,
    val conjunction: NativeConjoinedIndicatorData? = null,
    val difference: NativeDifferenceIndicatorData? = null
)

/**
 * 互斥条件差值数据。 / Exclusive condition difference data.
 * @property condition 减数条件 / Subtracted condition
 * @property resultKey 保留结果键 / Retained result key
 * @property name 链接名称 / Link name
 */
data class NativeDifferenceIndicatorData(
    val condition: NativeIndicatorData,
    val resultKey: VariableItemKey,
    val name: String
)

/**
 * 两个重化条件的合取结果。 / Conjunction result of two reified conditions.
 * @property condition 第二个条件 / Second condition
 * @property resultKey 保留的合取结果键 / Retained conjunction result key
 * @property name 链接约束名称 / Link constraint name
 */
data class NativeConjoinedIndicatorData(
    val condition: NativeIndicatorData,
    val resultKey: VariableItemKey,
    val name: String
)

/**
 * 已验证的零容差带。 / Validated zero-tolerance band.
 * @property tolerance 带内绝对值上界 / Inside-band absolute-value limit
 * @property sideKey 保留方向列的键 / Retained side column key
 */
data class NativeZeroBandData(
    val tolerance: Double,
    val sideKey: VariableItemKey
)

/**
 * 已验证的条件数值结果。 / Validated conditional numeric result.
 * @property terms 真分支仿射项 / True-branch affine terms
 * @property constant 真分支常数 / True-branch constant
 * @property resultKey 保留的数值结果键 / Retained numeric result key
 * @property lowerBound 原真分支下界 / Original true-branch lower bound
 * @property upperBound 原真分支上界 / Original true-branch upper bound
 * @property name 约束名前缀 / Constraint name prefix
 */
data class NativeConditionalValueData(
    val terms: Map<VariableItemKey, Double>,
    val constant: Double,
    val resultKey: VariableItemKey,
    val lowerBound: Double,
    val upperBound: Double,
    val name: String
)

/**
 * 校验完整图关系及原 M 的覆盖范围。 / Validate the complete graph relation and original M coverage.
 * @param structure 指示结构 / Indicator structure
 * @param maximumMagnitude 数值幅值上限 / Numeric magnitude limit
 * @return 已验证数据或失败 / Validated data or failure
 */
fun prepareNativeIndicator(
    structure: IndicatorStructure<*>,
    maximumMagnitude: Double = Double.POSITIVE_INFINITY
): Ret<NativeIndicatorData> = prepareIndicator(structure, maximumMagnitude)

private fun <V> prepareIndicator(
    structure: IndicatorStructure<V>,
    maximumMagnitude: Double
): Ret<NativeIndicatorData> where V : RealNumber<V>, V : NumberField<V> {
    fun invalid(): Ret<NativeIndicatorData> = Failed(
        ErrorCode.IllegalArgument,
        "原生指示结构数值或范围无效。 / Invalid native indicator values or bounds."
    )
    fun usable(value: Double) = value.isFinite() && abs(value) < minOf(maximumMagnitude, Double.MAX_VALUE)
    return try {
        when (val validation = structure.generateConstraints()) {
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
            is Ok -> Unit
        }
        val converter = structure.converter
        val constant = converter.fromValue(structure.input.constant).toSolverDouble(fieldName = "indicator.constant")
        val tolerance = converter.fromValue(structure.tolerance).toSolverDouble(fieldName = "indicator.tolerance")
        val bigM = converter.fromValue(structure.bigM).toSolverDouble(fieldName = "indicator.bigM")
        val allowedLower = structure.conditionBounds?.let {
            converter.fromValue(it.lower).toSolverDouble(fieldName = "indicator.conditionLower")
        } ?: -bigM
        val allowedUpper = structure.conditionBounds?.let {
            converter.fromValue(it.upper).toSolverDouble(fieldName = "indicator.conditionUpper")
        } ?: bigM
        if (!usable(allowedLower) || !usable(allowedUpper) || allowedLower > allowedUpper) return invalid()
        if (!usable(constant) || !usable(tolerance) || !usable(bigM) || tolerance <= 0.0 || bigM <= 0.0 ||
            !usable(tolerance - constant) || !usable(-constant)
        ) return invalid()
        val terms = linkedMapOf<VariableItemKey, Double>()
        val zeroBand = structure.zeroBand?.let { band ->
            val bandTolerance = converter.fromValue(band.tolerance).toSolverDouble(fieldName = "indicator.bandTolerance")
            if (!usable(bandTolerance) || bandTolerance < 0.0 || bandTolerance >= tolerance ||
                !usable(tolerance + bandTolerance) || !usable(-tolerance - constant) ||
                !usable(bandTolerance - constant) || !usable(-bandTolerance - constant)
            ) return invalid()
            NativeZeroBandData(bandTolerance, band.sideVariable.key)
        }
        val resultKeys = (structure.equivalentResults.map { it.key } + structure.resultVariable.key +
            listOfNotNull(zeroBand?.sideKey, structure.conjunction?.condition?.resultVariable?.key, structure.conjunction?.resultVariable?.key,
                structure.difference?.condition?.resultVariable?.key, structure.difference?.resultVariable?.key)).toSet()
        val difference = structure.difference?.let { combined ->
            val condition = when (val prepared = prepareNativeIndicator(combined.condition, maximumMagnitude)) {
                is Ok -> prepared.value
                is Failed -> return Failed(prepared.error)
                is Fatal -> return Fatal(prepared.errors)
            }
            if (condition.terms.keys.any { it in resultKeys }) return invalid()
            NativeDifferenceIndicatorData(
                condition = condition,
                resultKey = combined.resultVariable.key,
                name = combined.name
            )
        }
        val conjunction = structure.conjunction?.let { combined ->
            val condition = when (val prepared = prepareNativeIndicator(combined.condition, maximumMagnitude)) {
                is Ok -> prepared.value
                is Failed -> return Failed(prepared.error)
                is Fatal -> return Fatal(prepared.errors)
            }
            if (condition.terms.keys.any { it in resultKeys }) return invalid()
            NativeConjoinedIndicatorData(
                condition = condition,
                resultKey = combined.resultVariable.key,
                name = combined.name
            )
        }
        val conditionalValue = structure.conditionalValue?.let { value ->
            if (value.resultVariable.key in resultKeys) return invalid()
            when (val prepared = prepareConditionalValue(value, converter, maximumMagnitude)) {
                is Ok -> prepared.value
                is Failed -> return Failed(prepared.error)
                is Fatal -> return Fatal(prepared.errors)
            }
        }
        val variables = linkedMapOf<VariableItemKey, AbstractVariableItem<*, *>>()
        val impliedCondition = structure.impliedCondition?.let { value ->
            if (structure.positiveOnZero || value.indicatorVariable.key in resultKeys ||
                value.indicatorVariable.key == conditionalValue?.resultKey
            ) return invalid()
            when (val prepared = prepareConditionalValue(
                value = ConditionalValue(
                    input = value.input,
                    resultVariable = value.indicatorVariable,
                    bounds = value.bounds,
                    name = value.name
                ),
                converter = converter,
                maximumMagnitude = maximumMagnitude
            )) {
                is Ok -> prepared.value
                is Failed -> return Failed(prepared.error)
                is Fatal -> return Fatal(prepared.errors)
            }
        }
        if (impliedCondition != null && (!usable(tolerance - impliedCondition.constant) ||
            impliedCondition.terms.keys.any { it in resultKeys || it == conditionalValue?.resultKey })
        ) return invalid()
        for (term in structure.input.monomials) {
            val variable = term.symbol as? AbstractVariableItem<*, *> ?: return invalid()
            if (variable.key in resultKeys || variable.key == conditionalValue?.resultKey ||
                variable.key == impliedCondition?.resultKey
            ) return invalid()
            val coefficient = converter.fromValue(term.coefficient).toSolverDouble(fieldName = "indicator.coefficient")
            val combined = (terms[variable.key] ?: 0.0) + coefficient
            if (!usable(coefficient) || !usable(combined)) return invalid()
            terms[variable.key] = combined
            variables[variable.key] = variable
        }
        var lower = constant
        var upper = constant
        for ((key, coefficient) in terms.filterValues { it != 0.0 }) {
            val variable = variables.getValue(key)
            val lb = variable.lowerBound?.value?.unwrap()?.toSolverDouble(fieldName = "indicator.lower") ?: return invalid()
            val ub = variable.upperBound?.value?.unwrap()?.toSolverDouble(fieldName = "indicator.upper") ?: return invalid()
            if (!usable(lb) || !usable(ub) || lb > ub) return invalid()
            lower += coefficient * if (coefficient > 0) lb else ub
            upper += coefficient * if (coefficient > 0) ub else lb
            if (!usable(lower) || !usable(upper)) return invalid()
        }
        if (lower < allowedLower || upper > allowedUpper) return invalid()
        Ok(NativeIndicatorData(
            terms = terms.filterValues { it != 0.0 },
            constant = constant,
            tolerance = tolerance,
            bigM = bigM,
            resultKey = structure.resultVariable.key,
            name = structure.name,
            positiveOnZero = structure.positiveOnZero,
            lowerBound = allowedLower,
            upperBound = allowedUpper,
            equivalentResultKeys = structure.equivalentResults.map { it.key },
            conditionalValue = conditionalValue,
            impliedCondition = impliedCondition,
            zeroBand = zeroBand,
            conjunction = conjunction,
            difference = difference
        ))
    } catch (_: RuntimeException) {
        invalid()
    }
}

internal fun <V> prepareConditionalValue(
    value: ConditionalValue<V>,
    converter: IntoValue<V>,
    maximumMagnitude: Double
): Ret<NativeConditionalValueData> where V : RealNumber<V>, V : NumberField<V> {
    fun invalid(): Ret<NativeConditionalValueData> = Failed(ErrorCode.IllegalArgument, "条件结果的原生范围证明无效。 / Invalid native conditional value bounds proof.")
    fun usable(number: Double) = number.isFinite() && abs(number) < minOf(maximumMagnitude, Double.MAX_VALUE)
    val lower = converter.fromValue(value.bounds.lower).toSolverDouble(fieldName = "conditionalValue.lower")
    val upper = converter.fromValue(value.bounds.upper).toSolverDouble(fieldName = "conditionalValue.upper")
    val constant = converter.fromValue(value.input.constant).toSolverDouble(fieldName = "conditionalValue.constant")
    if (!usable(lower) || !usable(upper) || !usable(constant) || lower > upper) return invalid()
    val terms = linkedMapOf<VariableItemKey, Double>()
    val variables = linkedMapOf<VariableItemKey, AbstractVariableItem<*, *>>()
    for (term in value.input.monomials) {
        val variable = term.symbol as? AbstractVariableItem<*, *> ?: return invalid()
        if (variable.key == value.resultVariable.key) return invalid()
        val coefficient = converter.fromValue(term.coefficient).toSolverDouble(fieldName = "conditionalValue.coefficient")
        val combined = (terms[variable.key] ?: 0.0) + coefficient
        if (!usable(coefficient) || !usable(combined)) return invalid()
        terms[variable.key] = combined
        variables[variable.key] = variable
    }
    var actualLower = constant
    var actualUpper = constant
    for ((key, coefficient) in terms.filterValues { it != 0.0 }) {
        val variable = variables.getValue(key)
        val lb = variable.lowerBound?.value?.unwrap()?.toSolverDouble(fieldName = "conditionalValue.variableLower") ?: return invalid()
        val ub = variable.upperBound?.value?.unwrap()?.toSolverDouble(fieldName = "conditionalValue.variableUpper") ?: return invalid()
        if (!usable(lb) || !usable(ub) || lb > ub) return invalid()
        actualLower += coefficient * if (coefficient > 0) lb else ub
        actualUpper += coefficient * if (coefficient > 0) ub else lb
        if (!usable(actualLower) || !usable(actualUpper)) return invalid()
    }
    if (actualLower < lower || actualUpper > upper) return invalid()
    return Ok(NativeConditionalValueData(
        terms = terms.filterValues { it != 0.0 },
        constant = constant,
        resultKey = value.resultVariable.key,
        lowerBound = lower,
        upperBound = upper,
        name = value.name
    ))
}
