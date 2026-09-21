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
 * 条件结果：指示列为 1 时取 input，否则为 0。 / Value equals input on indicator one, otherwise zero.
 *
 * @param V 数值类型 / Numeric type
 * @property input 真分支输入 / True-branch input
 * @property resultVariable 保留的数值结果列 / Retained value result column
 * @property bounds 原真分支范围 / Original true-branch bounds
 * @property name 约束名称前缀 / Constraint name prefix
 */
data class ConditionalValue<V>(
    val input: LinearPolynomial<V>,
    val resultVariable: AbstractVariableItem<*, *>,
    val bounds: ConditionBounds<V>,
    val name: String
) where V : RealNumber<V>, V : NumberField<V> {

    internal fun generateConstraints(indicator: AbstractVariableItem<*, *>, converter: IntoValue<V>): Ret<List<LinearInequality<V>>> {
        return try {
            if (resultVariable.key == indicator.key || !indicator.type.isBinaryType ||
                !isUsableConditionBound(bounds.lower, converter) || !isUsableConditionBound(bounds.upper, converter) ||
                bounds.lower.compareTo(bounds.upper) > 0 || !hasUsableConditionFlattenedValues(input, converter)
            ) return Failed(ErrorCode.IllegalArgument, "条件结果输入或范围无效。 / Invalid conditional value input or bounds.")
            val result = LinearMonomial(converter.one, resultVariable)
            val negativeInput = input.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            val constraints = listOf(
                LinearInequality(
                    lhs = LinearPolynomial(listOf(result, LinearMonomial(-bounds.upper, indicator)), converter.zero),
                    rhs = LinearPolynomial(emptyList(), converter.zero),
                    comparison = Comparison.LE,
                    name = "${name}_zero_ub"
                ),
                LinearInequality(
                    lhs = LinearPolynomial(listOf(result, LinearMonomial(-bounds.lower, indicator)), converter.zero),
                    rhs = LinearPolynomial(emptyList(), converter.zero),
                    comparison = Comparison.GE,
                    name = "${name}_zero_lb"
                ),
                LinearInequality(
                    lhs = LinearPolynomial(negativeInput + listOf(result, LinearMonomial(-bounds.lower, indicator)), -input.constant),
                    rhs = LinearPolynomial(emptyList(), -bounds.lower),
                    comparison = Comparison.LE,
                    name = "${name}_then_ub"
                ),
                LinearInequality(
                    lhs = LinearPolynomial(negativeInput + listOf(result, LinearMonomial(-bounds.upper, indicator)), -input.constant),
                    rhs = LinearPolynomial(emptyList(), -bounds.upper),
                    comparison = Comparison.GE,
                    name = "${name}_then_lb"
                )
            )
            if (constraints.any { row ->
                !hasUsableConditionFlattenedValues(LinearPolynomial(
                    row.lhs.monomials + row.rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
                    row.lhs.constant - row.rhs.constant
                ), converter)
            }) return Failed(ErrorCode.IllegalArgument, "条件结果约束数值无效。 / Invalid conditional value constraint values.")
            Ok(constraints)
        } catch (_: RuntimeException) {
            Failed(ErrorCode.IllegalArgument, "生成条件结果约束失败。 / Failed to generate conditional value constraints.")
        }
    }
}
