@file:Suppress("unused")

/** 松弛范围函数符号 / Slack range function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 松弛区间函数符号 / Slack range function symbol
 *
 * 提供 [SlackRangeFunction]，为区间约束引入正/负松弛变量。
 *
 * Provides [SlackRangeFunction] for introducing positive/negative slack variables for range constraints.
*/

/**
 * 松弛范围函数：使用松弛变量将 x 约束在 [lb, ub] 范围内。
 * Slack range function: bounds x within [lb, ub] using slack variables.
 *
 * 原始语义：polyX = x + neg - pos，约束 polyX leq ub / geq lb。
 * Original semantics: polyX = x + neg - pos, with constraints polyX leq ub / geq lb.
 * neg = 下松弛（x 低于 lb），pos = 上松弛（x 高于 ub）。
 * neg = lower slack (x below lb), pos = upper slack (x above ub).
 *
 * @param x 要约束的表达式 / the expression to bound
 * @param lb 下界多项式 / lower bound polynomial
 * @param ub 上界多项式 / upper bound polynomial
 * @param type 变量类型（UInteger 或 UContinuous）/ variable type kind (UInteger or UContinuous)
 * @param constraint 是否添加 polyX leq ub / geq lb 约束 / whether to add polyX leq ub / geq lb constraints
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
*/
class SlackRangeFunction<V>(
    val x: LinearPolynomial<V>,
    val lb: LinearPolynomial<V>,
    val ub: LinearPolynomial<V>,
    val type: VariableTypeKind = UContinuous,
    val constraint: Boolean = true,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    val negVar: AbstractVariableItem<*, *> by lazy {
        if (type.isIntegerType) UIntVar("${name}_neg") else URealVar("${name}_neg")
    }
    val posVar: AbstractVariableItem<*, *> by lazy {
        if (type.isIntegerType) UIntVar("${name}_pos") else URealVar("${name}_pos")
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(negVar, posVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, posVar)), converter.zero)

    val neg: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, negVar)), converter.zero)
    }
    val pos: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, posVar)), converter.zero)
    }

    val polyX: LinearPolynomial<V> by lazy {
        val unit = converter.one
        LinearPolynomial(
            x.monomials + LinearMonomial(unit, negVar) + LinearMonomial(-unit, posVar),
            x.constant
        )
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val lbValue = lb.evaluateWith(values) ?: return null
        val ubValue = ub.evaluateWith(values) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            converter.zero
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        if (constraint) {
            val constraints = mutableListOf<LinearInequality<V>>()
            constraints += LinearInequality(polyX, ub, Comparison.LE, "${name}_ub")
            constraints += LinearInequality(polyX, lb, Comparison.GE, "${name}_lb")
            return addConstraints(model, constraints) ?: ok
        }
        return ok
    }

    companion object {
        /**
         * 使用 lb/ub 多项式的 V 泛型工厂。
         * V-generic factory with lb/ub polynomials.
         *
         * @param x 要约束的表达式 / the expression to bound
         * @param lb 下界多项式 / lower bound polynomial
         * @param ub 上界多项式 / upper bound polynomial
         * @param type 变量类型 / variable type kind
         * @param constraint 是否添加约束 / whether to add constraints
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 松弛范围函数实例 / slack range function instance
        */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            lb: LinearPolynomial<V>,
            ub: LinearPolynomial<V>,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): SlackRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SlackRangeFunction(x, lb, ub, type, constraint, converter, name, displayName)

        /**
         * 使用 LinearIntermediateSymbol 和 lb/ub 多项式的 V 泛型工厂。
         * V-generic factory with LinearIntermediateSymbol and lb/ub polynomials.
         *
         * @param x 线性中间符号 / linear intermediate symbol
         * @param lb 下界多项式 / lower bound polynomial
         * @param ub 上界多项式 / upper bound polynomial
         * @param type 变量类型 / variable type kind
         * @param constraint 是否添加约束 / whether to add constraints
         * @param converter 值类型转换器 / value type converter
         * @param name 此函数的唯一名称 / unique name for this function
         * @param displayName 可选的人类可读显示名称 / optional human-readable display name
         * @return 线性函数符号适配器 / linear function symbol adapter
        */
        @JvmStatic
        fun <V> fromLinearIntermediateSymbol(
            x: LinearIntermediateSymbol<V>,
            lb: LinearPolynomial<V>,
            ub: LinearPolynomial<V>,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(x.toLinearPolynomial(), lb, ub, type, constraint, converter, name, displayName),
            converter = converter
        )

    }
}
