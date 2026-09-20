package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 两个互斥条件的有符号差值。 / Signed difference of two mutually exclusive conditions.
 * @property condition 减数条件 / Subtracted condition
 * @property resultVariable 保留的数值结果 / Retained numeric result
 * @property name 链接约束名称前缀 / Link constraint name prefix
 */
data class DifferenceIndicator<V>(
    val condition: IndicatorStructure<V>,
    val resultVariable: AbstractVariableItem<*, *>,
    val name: String
) where V : RealNumber<V>, V : NumberField<V> {
    internal fun generateConstraints(primary: AbstractVariableItem<*, *>): Ret<List<LinearInequality<V>>> {
        if (setOf(primary.key, condition.resultVariable.key, resultVariable.key).size != 3 ||
            condition.conjunction != null || condition.difference != null || condition.conditionalValue != null ||
            condition.impliedCondition != null || condition.zeroBand != null || condition.equivalentResults.isNotEmpty()
        ) return Failed(ErrorCode.IllegalArgument, "互斥差值结构无效。 / Invalid exclusive difference structure.")
        val rows = when (val generated = condition.generateConstraints()) {
            is Ok -> generated.value
            is Failed -> return Failed(generated.error)
            is Fatal -> return Fatal(generated.errors)
        }
        val one = condition.converter.one
        val zero = condition.converter.zero
        return Ok(rows + listOf(
            LinearInequality(
                lhs = LinearPolynomial(listOf(
                    LinearMonomial(one, resultVariable),
                    LinearMonomial(-one, primary),
                    LinearMonomial(one, condition.resultVariable)
                ), zero),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.EQ,
                name = "${name}_bter_result"
            ),
            LinearInequality(
                lhs = LinearPolynomial(listOf(LinearMonomial(one, primary), LinearMonomial(one, condition.resultVariable)), zero),
                rhs = LinearPolynomial(emptyList(), one),
                comparison = Comparison.LE,
                name = "${name}_bter_exclusive"
            )
        ))
    }
}
