package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 第二个条件与主条件的二值合取。 / Binary conjunction of a second condition with the primary condition.
 * @property condition 独立重化的第二个简单条件 / Independently reified second simple condition
 * @property resultVariable 保留的 AND 结果 / Retained AND result
 * @property name 链接约束名称前缀 / Link constraint name prefix
 */
data class ConjoinedIndicator<V>(
    val condition: IndicatorStructure<V>,
    val resultVariable: AbstractVariableItem<*, *>,
    val name: String
) where V : RealNumber<V>, V : NumberField<V> {
    internal fun generateConstraints(primary: AbstractVariableItem<*, *>): Ret<List<LinearInequality<V>>> {
        if (!resultVariable.type.isBinaryType ||
            setOf(primary.key, condition.resultVariable.key, resultVariable.key).size != 3 ||
            condition.conjunction != null || condition.difference != null || condition.conditionalValue != null || condition.impliedCondition != null ||
            condition.zeroBand != null || condition.equivalentResults.isNotEmpty()
        ) return Failed(ErrorCode.IllegalArgument, "合取条件结构无效。 / Invalid conjoined indicator structure.")
        val rows = when (val generated = condition.generateConstraints()) {
            is Ok -> generated.value
            is Failed -> return Failed(generated.error)
            is Fatal -> return Fatal(generated.errors)
        }
        val one = condition.converter.one
        val zero = condition.converter.zero
        return Ok(rows + listOf(
            LinearInequality(
                lhs = LinearPolynomial(listOf(LinearMonomial(one, resultVariable), LinearMonomial(-one, primary)), zero),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.LE,
                name = "${name}_link_ge"
            ),
            LinearInequality(
                lhs = LinearPolynomial(listOf(LinearMonomial(one, resultVariable), LinearMonomial(-one, condition.resultVariable)), zero),
                rhs = LinearPolynomial(emptyList(), zero),
                comparison = Comparison.LE,
                name = "${name}_link_le"
            ),
            LinearInequality(
                lhs = LinearPolynomial(listOf(LinearMonomial(one, resultVariable), LinearMonomial(-one, primary), LinearMonomial(-one, condition.resultVariable)), zero),
                rhs = LinearPolynomial(emptyList(), -one),
                comparison = Comparison.GE,
                name = "${name}_link_lb"
            )
        ))
    }
}
