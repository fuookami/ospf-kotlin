package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 只在前件为真时激活的后件。 / Consequent activated only by a true antecedent.
 *
 * @param V 数值类型 / Numeric type
 * @property input 归一化后件输入 / Normalized consequent input
 * @property indicatorVariable 保留的后件指示列 / Retained consequent indicator
 * @property bounds 原后件范围 / Original consequent bounds
 * @property name 约束名前缀 / Constraint name prefix
 */
data class ImpliedCondition<V>(
    val input: LinearPolynomial<V>,
    val indicatorVariable: AbstractVariableItem<*, *>,
    val bounds: ConditionBounds<V>,
    val name: String
) where V : RealNumber<V>, V : NumberField<V> {

    internal fun generateConstraints(
        antecedent: AbstractVariableItem<*, *>,
        tolerance: V,
        converter: IntoValue<V>
    ): Ret<List<LinearInequality<V>>> {
        return try {
            if (!antecedent.type.isBinaryType || !indicatorVariable.type.isBinaryType || antecedent.key == indicatorVariable.key ||
                !isUsableConditionBound(bounds.lower, converter) || !isUsableConditionBound(bounds.upper, converter) ||
                !isUsableConditionBound(tolerance, converter) || tolerance.compareTo(converter.zero) <= 0 ||
                bounds.lower.compareTo(converter.zero) > 0 || bounds.upper.compareTo(tolerance) < 0
            ) return Failed(ErrorCode.IllegalArgument, "非折叠蕴含范围无效。 / Invalid non-folded implication bounds.")
            val lowerAdjustment = tolerance - bounds.lower
            val upperAdjustment = bounds.upper
            val constraints = listOf(
                LinearInequality(
                    lhs = LinearPolynomial(input.monomials + listOf(
                        LinearMonomial(-lowerAdjustment, indicatorVariable),
                        LinearMonomial(-lowerAdjustment, antecedent)
                    ), input.constant),
                    rhs = LinearPolynomial(emptyList(), bounds.lower - lowerAdjustment),
                    comparison = Comparison.GE,
                    name = "${name}_con_gated_lower"
                ),
                LinearInequality(
                    lhs = LinearPolynomial(input.monomials + listOf(
                        LinearMonomial(-upperAdjustment, indicatorVariable),
                        LinearMonomial(upperAdjustment, antecedent)
                    ), input.constant),
                    rhs = LinearPolynomial(emptyList(), upperAdjustment),
                    comparison = Comparison.LE,
                    name = "${name}_con_gated_upper"
                ),
                LinearInequality(
                    lhs = LinearPolynomial(listOf(
                        LinearMonomial(converter.one, antecedent),
                        LinearMonomial(-converter.one, indicatorVariable)
                    ), converter.zero),
                    rhs = LinearPolynomial(emptyList(), converter.zero),
                    comparison = Comparison.LE,
                    name = "${name}_imply_link"
                )
            )
            if (constraints.any { row ->
                !hasUsableConditionSolverValues(row.lhs, converter) ||
                    !hasUsableConditionSolverValues(row.rhs, converter) ||
                    !hasUsableConditionFlattenedValues(LinearPolynomial(
                    row.lhs.monomials + row.rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
                    row.lhs.constant - row.rhs.constant
                ), converter)
            }) return Failed(ErrorCode.IllegalArgument, "蕴含门控数值无效。 / Invalid implication gate values.")
            Ok(constraints)
        } catch (_: RuntimeException) {
            Failed(ErrorCode.IllegalArgument, "生成蕴含门控失败。 / Failed to generate implication gates.")
        }
    }
}
