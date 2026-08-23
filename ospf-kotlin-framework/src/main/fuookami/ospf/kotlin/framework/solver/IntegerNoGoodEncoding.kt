/** Exact no-good encoding for bounded integer master assignments. / 有界整数主问题赋值的精确 no-good 编码。 */
package fuookami.ospf.kotlin.framework.solver

import java.math.BigInteger
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/** One bounded integer assignment to exclude. / 一个待排除的有界整数赋值。
 *
 * @property key Stable binding key. / 稳定绑定键。
 * @property variable Master integer variable. / 主问题整数变量。
 * @property value Assignment excluded by the encoding. / 编码排除的赋值。
 * @property domain Finite domain used to derive exact bounds. / 用于推导精确边界的有限值域。
 */
data class IntegerNoGoodVariable(
    val key: String,
    val variable: AbstractVariableItem<*, *>,
    val value: Int64,
    val domain: IntegerDomain
)

/** Linear exact encoding of one integer no-good assignment. / 使用分支指示变量线性精确编码 `x != assignment`。
 *
 * @property name Stable name prefix for generated model items. / 生成模型项的稳定名称前缀。
 * @property auxiliaryVariables Binary branch indicators introduced by the encoding. / 编码引入的二值分支指示变量。
 * @property constraints Linear inequalities implementing the no-good. / 实现 no-good 的线性不等式。
 */
data class IntegerNoGoodEncoding(
    val name: String,
    val auxiliaryVariables: List<BinVar>,
    val constraints: List<LinearInequality<Flt64>>
) {
    /** Install auxiliary variables and constraints in a mutable master. / 将辅助变量和约束安装到主问题。
     *
     * @param master Mutable master model receiving the generated items. / 接收生成模型项的可变主问题模型。
     * @return Installation result. / 安装结果。
     */
    fun install(master: LinearMetaModel<Flt64>): Try {
        for (variable in auxiliaryVariables) {
            val added = master.add(variable)
            if (added.failed) return propagate(added)
        }
        for ((index, constraint) in constraints.withIndex()) {
            val added = master.addConstraint(
                relation = constraint,
                name = "$name-$index",
                displayName = "$name-$index"
            )
            if (added.failed) return propagate(added)
        }
        return ok
    }
}

/** Builder for exact no-good constraints on finite integer domains. / 有限整数值域精确 no-good 约束构造器。 */
object IntegerNoGoodCutEncoder {
    /** Build an encoding that excludes exactly one assignment. / 构造精确排除一个赋值的编码。
     *
     * @param variables Variables and values forming the assignment to exclude. / 构成待排除赋值的变量和值。
     * @param name Stable name prefix for generated model items. / 生成模型项的稳定名称前缀。
     * @return Exact encoding or a validation error. / 精确编码或校验错误。
     */
    fun encode(
        variables: List<IntegerNoGoodVariable>,
        name: String = "integer-no-good"
    ): Ret<IntegerNoGoodEncoding> {
        if (variables.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "整数 no-good 变量不能为空 / Integer no-good variables must not be empty"
            )
        }
        val auxiliary = ArrayList<BinVar>()
        val constraints = ArrayList<LinearInequality<Flt64>>()
        val active = ArrayList<BinVar>()
        for ((index, item) in variables.withIndex()) {
            if (!item.domain.contains(item.value)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "赋值超出整数值域：${item.key}=${item.value} / Assignment is outside integer domain: ${item.key}=${item.value}"
                )
            }
            val lower = BigInteger.valueOf(item.domain.lowerBound.toLong())
            val upper = BigInteger.valueOf(item.domain.upperBound.toLong())
            val valueInteger = BigInteger.valueOf(item.value.toLong())
            val positiveM = upper - valueInteger
            val negativeM = valueInteger - lower
            if (positiveM.signum() <= 0 && negativeM.signum() <= 0) {
                continue
            }
            val value = exactFlt64(valueInteger, "no-good assignment ${item.key}")
            if (value.failed) return propagate(value)
            val positiveCoefficient = if (positiveM.signum() > 0) {
                exactFlt64(positiveM, "no-good positive Big-M ${item.key}")
            } else {
                ok(null)
            }
            if (positiveCoefficient.failed) return propagate(positiveCoefficient)
            val negativeCoefficient = if (negativeM.signum() > 0) {
                exactFlt64(negativeM, "no-good negative Big-M ${item.key}")
            } else {
                ok(null)
            }
            if (negativeCoefficient.failed) return propagate(negativeCoefficient)
            val positive = positiveCoefficient.value?.let { BinVar("$name-$index-positive") }
            val negative = negativeCoefficient.value?.let { BinVar("$name-$index-negative") }
            positive?.let {
                auxiliary += it
                active += it
            }
            negative?.let {
                auxiliary += it
                active += it
            }
            val x = item.variable

            // p + n <= 1; p means x >= value + 1, n means x <= value - 1.
            // These two gated bounds are exact over the declared finite domain.
            val branchIndicators = ArrayList<Pair<AbstractVariableItem<*, *>, Flt64>>()
            positive?.let { branchIndicators += it to Flt64.one }
            negative?.let { branchIndicators += it to Flt64.one }
            if (branchIndicators.size > 1) {
                constraints += inequality(
                    terms = branchIndicators,
                    comparison = Comparison.LE,
                    rhs = Flt64.one
                )
            }
            val lowerTerms = ArrayList<Pair<AbstractVariableItem<*, *>, Flt64>>()
            lowerTerms += x to Flt64.one
            positive?.let { lowerTerms += it to Flt64(-1.0) }
            negative?.let { lowerTerms += it to negativeCoefficient.value!! }
            constraints += inequality(
                terms = lowerTerms,
                comparison = Comparison.GE,
                rhs = value.value!!
            )
            val upperTerms = ArrayList<Pair<AbstractVariableItem<*, *>, Flt64>>()
            upperTerms += x to Flt64.one
            positive?.let { upperTerms += it to -positiveCoefficient.value!! }
            negative?.let { upperTerms += it to Flt64.one }
            constraints += inequality(
                terms = upperTerms,
                comparison = Comparison.LE,
                rhs = value.value!!
            )
        }
        if (active.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "所有整数变量都是固定值，无法构造 no-good / All integer variables are fixed; no-good cannot be constructed"
            )
        }
        constraints += inequality(
            terms = active.map { it to Flt64.one },
            comparison = Comparison.GE,
            rhs = Flt64.one
        )
        return ok(IntegerNoGoodEncoding(name, auxiliary, constraints))
    }

    private fun inequality(
        terms: List<Pair<AbstractVariableItem<*, *>, Flt64>>,
        comparison: Comparison,
        rhs: Flt64
    ): LinearInequality<Flt64> {
        return LinearInequality(
            lhs = LinearPolynomial(
                monomials = terms.map { (variable, coefficient) -> LinearMonomial(coefficient, variable) },
                constant = Flt64.zero
            ),
            rhs = LinearPolynomial(emptyList(), rhs),
            comparison = comparison
        )
    }

    private fun exactFlt64(value: BigInteger, context: String): Ret<Flt64?> {
        if (value.abs() > MAX_EXACT_DOUBLE_INTEGER) {
            return Failed(
                ErrorCode.Other,
                "$context 超出 Flt64 精确整数范围 / $context exceeds the exact Flt64 integer range"
            )
        }
        return ok(Flt64(java.lang.Double.valueOf(value.toString())))
    }

    private val MAX_EXACT_DOUBLE_INTEGER: BigInteger = BigInteger.valueOf(9_007_199_254_740_991L)
}

private fun <T> propagate(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(ErrorCode.ApplicationError, "整数 no-good 结果状态无效 / Invalid integer no-good result state")
    }
}
