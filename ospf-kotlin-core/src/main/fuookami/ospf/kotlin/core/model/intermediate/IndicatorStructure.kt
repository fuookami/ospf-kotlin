package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 双向正数指示结构，默认由结果 1 激活 input>=tolerance，另一分支为 input<=0。
 * Bidirectional positive indicator; result one activates input>=tolerance by default, the other branch input<=0.
 *
 * @param V 数值类型 / Numeric type
 * @property input 输入快照 / Input snapshot
 * @property resultVariable 保留的二值结果 / Retained binary result
 * @property bigM fallback 范围参数 / Fallback range parameter
 * @property tolerance 正分支下界 / Positive branch lower bound
 * @property converter 数值转换器 / Value converter
 * @property name 约束名称前缀 / Constraint name prefix
 * @property usage 使用语境 / Usage context
 * @property capturedInputBounds 自动 M 的有限范围证明 / Finite bounds proof for automatic M
 * @property positiveOnZero 为 true 时由结果 0 激活正分支 / Activate the positive branch on result zero when true
 * @property conditionBounds 可选的原条件范围，fallback 保留其非对称边界 / Optional original condition bounds retained by fallback
 * @property equivalentResults 必须保留且与结果相等的公开列 / Retained public columns equal to the result
 * @property conditionalValue 可选的条件数值结果 / Optional conditional numeric result
 * @property impliedCondition 可选的单向蕴含后件 / Optional one-way implication consequent
 * @property zeroBand 可选的零容差带；此时 tolerance 为带外边界、positiveOnZero 指定由结果 0 激活带外 / Optional zero band; tolerance is the outside boundary and positiveOnZero activates the outside on result zero
 * @property conjunction 可选的第二个条件及 AND 结果 / Optional second condition and AND result
 * @property difference 可选的互斥条件差值 / Optional exclusive condition difference
 */
data class IndicatorStructure<V>(
    val input: LinearPolynomial<V>,
    val resultVariable: AbstractVariableItem<*, *>,
    val bigM: V,
    val tolerance: V,
    val converter: IntoValue<V>,
    val name: String,
    val usage: FunctionUsageSummary = FunctionUsageSummary(),
    val capturedInputBounds: LinearPolynomialBounds<V>? = null,
    val positiveOnZero: Boolean = false,
    val conditionBounds: ConditionBounds<V>? = null,
    val equivalentResults: List<AbstractVariableItem<*, *>> = emptyList(),
    val conditionalValue: ConditionalValue<V>? = null,
    val impliedCondition: ImpliedCondition<V>? = null,
    val zeroBand: ZeroBand<V>? = null,
    val conjunction: ConjoinedIndicator<V>? = null,
    val difference: DifferenceIndicator<V>? = null
) : DeferredFunctionStructure where V : RealNumber<V>, V : NumberField<V> {

    /** 原生路径必须保留的全部结果列。 / All result columns that native lowering must retain. */
    val retainedResultVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVariable) + equivalentResults +
            listOfNotNull(conditionalValue?.resultVariable, impliedCondition?.indicatorVariable, zeroBand?.sideVariable,
                conjunction?.condition?.resultVariable, conjunction?.resultVariable,
                difference?.condition?.resultVariable, difference?.resultVariable)

    internal fun generateConstraints(): Ret<List<LinearInequality<V>>> {
        if ((conjunction != null || difference != null) && (zeroBand != null || conditionalValue != null || impliedCondition != null || equivalentResults.isNotEmpty() || conjunction != null && difference != null)) {
            return Failed(ErrorCode.IllegalArgument, "合取条件不支持附加结果。 / Conjoined conditions do not support additional results.")
        }
        val conjunctionConstraints = when (val generated = conjunction?.generateConstraints(resultVariable)) {
            is Ok -> generated.value
            is Failed -> return Failed(generated.error)
            is Fatal -> return Fatal(generated.errors)
            null -> emptyList()
        }
        val differenceConstraints = when (val generated = difference?.generateConstraints(resultVariable)) {
            is Ok -> generated.value
            is Failed -> return Failed(generated.error)
            is Fatal -> return Fatal(generated.errors)
            null -> emptyList()
        }
        if (zeroBand != null && (conditionBounds != null || conditionalValue != null || impliedCondition != null ||
                equivalentResults.any { it.key == zeroBand.sideVariable.key })
        ) return Failed(ErrorCode.IllegalArgument, "零容差带结构组合无效。 / Invalid zero-band structure combination.")
        val impliedConstraints = when (val constraints = impliedCondition?.generateConstraints(resultVariable, tolerance, converter)) {
            is Ok -> constraints.value
            is Failed -> return Failed(constraints.error)
            is Fatal -> return Fatal(constraints.errors)
            null -> emptyList()
        }
        val valueConstraints = when (val constraints = conditionalValue?.generateConstraints(resultVariable, converter)) {
            is Ok -> constraints.value
            is Failed -> return Failed(constraints.error)
            is Fatal -> return Fatal(constraints.errors)
            null -> emptyList()
        }
        if (equivalentResults.any { !it.type.isBinaryType || it.key == resultVariable.key } ||
            equivalentResults.map { it.key }.toSet().size != equivalentResults.size
        ) {
            return Failed(ErrorCode.IllegalArgument, "等价结果列无效。 / Invalid equivalent result columns.")
        }
        return when (val constraints = generateIndicatorConstraints()) {
            is Failed -> Failed(constraints.error)
            is Fatal -> Fatal(constraints.errors)
            is Ok -> Ok(constraints.value + valueConstraints + impliedConstraints + conjunctionConstraints + differenceConstraints + equivalentResults.mapIndexed { index, variable ->
                LinearInequality(
                    lhs = LinearPolynomial(listOf(
                        LinearMonomial(converter.one, resultVariable),
                        LinearMonomial(-converter.one, variable)
                    ), converter.zero),
                    rhs = LinearPolynomial(emptyList(), converter.zero),
                    comparison = Comparison.EQ,
                    name = "${name}_equivalent_$index"
                )
            })
        }
    }

    private fun generateIndicatorConstraints(): Ret<List<LinearInequality<V>>> {
        return try {
            if (!resultVariable.type.isBinaryType ||
                !isUsableConditionBound(tolerance, converter) || tolerance.compareTo(converter.zero) <= 0 ||
                !hasUsableConditionSolverValues(input, converter)
            ) {
                return Failed(ErrorCode.IllegalArgument, "指示结构输入无效。 / Invalid indicator structure input.")
            }
            val captured = capturedInputBounds
            if (captured != null) {
                val current = input.finiteBounds(converter)
                    ?: return Failed(ErrorCode.IllegalArgument, "指示结构范围证明丢失。 / Indicator bounds proof is unavailable.")
                if (current.lower.compareTo(captured.lower) < 0 || current.upper.compareTo(captured.upper) > 0) {
                    return Failed(ErrorCode.IllegalArgument, "指示结构范围超出原证明。 / Indicator bounds exceed the captured proof.")
                }
            }
            conditionBounds?.let { bounds ->
                return relationIndicatorConstraints(
                    poly = input,
                    indicator = resultVariable,
                    relation = if (positiveOnZero) Comparison.LE else Comparison.GT,
                    bounds = bounds,
                    strictBoundary = tolerance,
                    namePrefix = name
                )
            }
            zeroBand?.let { band ->
                if (!band.sideVariable.type.isBinaryType || band.sideVariable.key == resultVariable.key ||
                    !isUsableConditionBound(band.tolerance, converter) || band.tolerance.compareTo(converter.zero) < 0 ||
                    band.tolerance.compareTo(tolerance) >= 0
                ) return Failed(ErrorCode.IllegalArgument, "零容差带或方向列无效。 / Invalid zero band or side column.")
                return if (positiveOnZero) safeZeroIndicatorConstraints(
                    poly = input,
                    indicator = resultVariable,
                    sideVar = band.sideVariable,
                    bigM = bigM,
                    tolerance = band.tolerance,
                    strictBoundary = tolerance,
                    namePrefix = name
                ) else safeNonzeroIndicatorConstraints(
                    poly = input,
                    indVar = resultVariable,
                    sideVar = band.sideVariable,
                    bigM = bigM,
                    tolerance = band.tolerance,
                    strictBoundary = tolerance,
                    namePrefix = name
                )
            }
            val constraints = safePositiveIndicatorConstraints(
                poly = input,
                indicator = resultVariable,
                bigM = bigM,
                tolerance = tolerance,
                namePrefix = name
            )
            if (!positiveOnZero) return constraints
            when (constraints) {
                is Failed -> Failed(constraints.error)
                is Fatal -> Fatal(constraints.errors)
                is Ok -> {
                    val inverted = listOf(
                        LinearInequality(
                            lhs = LinearPolynomial(input.monomials + LinearMonomial(bigM, resultVariable), input.constant),
                            rhs = LinearPolynomial(emptyList(), bigM),
                            comparison = Comparison.LE,
                            name = "${name}_satisfied"
                        ),
                        LinearInequality(
                            lhs = LinearPolynomial(input.monomials + LinearMonomial(bigM + tolerance, resultVariable), input.constant),
                            rhs = LinearPolynomial(emptyList(), tolerance),
                            comparison = Comparison.GE,
                            name = "${name}_violated"
                        )
                    )
                    if (inverted.any { !hasUsableConditionSolverValues(it.lhs, converter) }) {
                        Failed(ErrorCode.IllegalArgument, "指示极性变换产生无效数值。 / Invalid values after indicator polarity transformation.")
                    } else Ok(inverted)
                }
            }
        } catch (_: RuntimeException) {
            Failed(ErrorCode.IllegalArgument, "生成指示约束失败。 / Failed to generate indicator constraints.")
        }
    }

    internal fun materializeFallbackConstraints(): Ret<DeferredFunctionFallbackConstraints> {
        return when (val constraints = generateConstraints()) {
            is Ok -> convertPwlConstraintsToFlt64(constraints.value, converter)
            is Failed -> Failed(constraints.error)
            is Fatal -> Fatal(constraints.errors)
        }
    }
}
