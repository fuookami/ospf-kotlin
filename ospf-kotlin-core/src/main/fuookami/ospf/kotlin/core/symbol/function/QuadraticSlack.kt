@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator

/**
 * 二次绝对松弛函数：$s = |L(x)-R(x)|$。
 * Quadratic absolute-slack function: $s = |L(x)-R(x)|$.
 *
 * 两个正部函数分别精确表达差值的正、负部分，因此不依赖目标函数压低松弛量。
 * Two exact positive-part functions model the positive and negative sides of the difference,
 * so correctness does not depend on minimizing the slack in the objective.
 *
 * @property left 左侧二次多项式 / left quadratic polynomial
 * @property right 右侧二次多项式 / right quadratic polynomial
 * @property bigM 可选显式 Big-M / optional explicit Big-M
 * @property converter 值类型转换器 / value type converter
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 * @property positivePart 差值正部 / positive part of the difference
 * @property negativePart 反向差值正部 / positive part of the reverse difference
 * @property polynomial 结果二次多项式 / result quadratic polynomial
 */
class QuadraticSlackFunction<V>(
    val left: QuadraticPolynomial<V>,
    val right: QuadraticPolynomial<V>,
    val bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V>
        where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    private val difference = subtract(left, right)
    private val reverseDifference = subtract(right, left)
    val positivePart = QuadraticPositivePartFunction(
        input = difference,
        bigM = bigM,
        converter = converter,
        name = "${name}_positive"
    )
    val negativePart = QuadraticPositivePartFunction(
        input = reverseDifference,
        bigM = bigM,
        converter = converter,
        name = "${name}_negative"
    )

    override val identifier: UInt64 by lazy { IdentifierGenerator.gen() }
    override val index: Int = 0
    override val category: Category = Linear
    override val operationCategory: Category = Quadratic
    override val parent: IntermediateSymbol<out V>? = null
    override val dependencies: Set<IntermediateSymbol<out V>>
        get() = positivePart.dependencies + negativePart.dependencies
    override val cached: Boolean = false
    override val range: ExpressionRange<V>
        get() = SolverBoundaryCasts.fullExpressionRange()

    override val polynomial: QuadraticPolynomial<V>
        get() = QuadraticPolynomial(
            positivePart.polynomial.monomials + negativePart.polynomial.monomials,
            positivePart.polynomial.constant + negativePart.polynomial.constant
        )

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(
        polynomial.monomials,
        polynomial.constant
    )

    override fun flush(force: Boolean) {
        positivePart.flush(force)
        negativePart.flush(force)
    }

    override fun prepare(
        values: Map<Symbol, V>?,
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>
    ): V? = combine(
        positivePart.prepare(values, tokenTable, converter),
        negativePart.prepare(values, tokenTable, converter)
    )

    override fun evaluate(
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = combine(
        positivePart.evaluate(tokenTable, converter, zeroIfNone),
        negativePart.evaluate(tokenTable, converter, zeroIfNone)
    )

    override fun evaluate(
        results: List<V>,
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = combine(
        positivePart.evaluate(results, tokenTable, converter, zeroIfNone),
        negativePart.evaluate(results, tokenTable, converter, zeroIfNone)
    )

    override fun evaluate(
        values: Map<Symbol, V>,
        tokenTable: AbstractTokenTable<V>?,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = combine(
        positivePart.evaluate(values, tokenTable, converter, zeroIfNone),
        negativePart.evaluate(values, tokenTable, converter, zeroIfNone)
    )

    private fun combine(positive: V?, negative: V?): V? =
        if (positive == null || negative == null) null else positive + negative

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        val first = positivePart.registerAuxiliaryTokens(tokens)
        return if (first is Ok) negativePart.registerAuxiliaryTokens(tokens) else first
    }

    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        val first = positivePart.registerConstraints(model)
        return if (first is Ok) negativePart.registerConstraints(model) else first
    }

    companion object {
        private fun <V> subtract(
            left: QuadraticPolynomial<V>,
            right: QuadraticPolynomial<V>
        ): QuadraticPolynomial<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticPolynomial(
                left.monomials + right.monomials.map {
                    QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2)
                },
                left.constant - right.constant
            )
    }
}
