@file:Suppress("unused")

/** 绝对值函数符号 / Absolute value function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 绝对值函数符号 / Absolute value function symbol
 *
 * 提供 [AbsFunction]，实现 y = |x| 的线性化建模。
 *
 * Provides [AbsFunction] for linearized modeling of y = |x|.
 */

/**
 * 绝对值函数 / Absolute value function
 *
 * 实现 y = |x|，其中 x = pos - neg（pos, neg >= 0），y = pos + neg，互补性 pos * neg = 0 在最优解处自然满足。
 *
 * Implements y = |x| where x = pos - neg (pos, neg >= 0), y = pos + neg, complementarity pos * neg = 0 holds at optimality.
 *
 * @property polynomial 输入线性多项式 / Input linear polynomial
 * @property resultVar 结果 / Result
 * @property posVar 正部 / Positive part
 * @property negVar 负部 / Negative part
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认 1e6）/ Big-M bound (default 1e6)
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class AbsFunction<V>(
    val polynomial: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    override var name: String = "abs",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val resultVar: AbstractVariableItem<*, *> = URealVar("${name}_abs")
    val posVar: AbstractVariableItem<*, *> = URealVar("${name}_abs_pos")
    val negVar: AbstractVariableItem<*, *> = URealVar("${name}_abs_neg")

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, posVar, negVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val v = polynomial.evaluateWith(values) ?: return null
        return if (v ls converter.zero) -v else v
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // result = pos + neg / 结果 = 正部 + 负部
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, resultVar),
                LinearMonomial(-one, posVar),
                LinearMonomial(-one, negVar)
            ), zero),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_abs_result")

        // poly = pos - neg / 输入 = 正部 - 负部
        val polyMonos = polynomial.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += LinearInequality(
            LinearPolynomial(polyMonos + listOf(
                LinearMonomial(-one, posVar),
                LinearMonomial(one, negVar)
            ), polynomial.constant),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_abs_decompose")

        // pos <= M（已由 URealVar 上界保证）
        // neg <= M（已由 URealVar 上界保证）
        // 互补性：在 LP 中求解器不会隐式强制 pos * neg = 0，需要明确约束；
        // 在 MIP 中可通过二进制变量强制互补性。
        // Complementarity: in LP the solver does not implicitly enforce pos * neg = 0, explicit constraints are needed;
        // in MIP complementarity can be enforced via binary variables.

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /**
         * 创建绝对值函数实例 / Create an absolute value function instance
         * @param polynomial 输入线性多项式 / input linear polynomial
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [AbsFunction] 实例 / [AbsFunction] instance
         */
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): AbsFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            AbsFunction(polynomial, converter, bigM, name = name, displayName = displayName)
    }
}
