@file:Suppress("unused")

/** 区间距离函数符号 / Range-distance function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionStructure
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 到闭区间的精确距离：$s = max(lower-x, x-upper, 0)$。
 * Exact distance to a closed interval: $s = max(lower-x, x-upper, 0)$.
 *
 * 该模型内部使用精确最大值公式；即使目标函数不最小化结果，求解器值也不能被任意放大。
 * The implementation uses an exact maximum formulation, so the solver value cannot be inflated
 * even when the objective does not minimize it.
 *
 * @property input 输入线性多项式 / input linear polynomial
 * @property lower 区间下界 / lower bound of the interval
 * @property upper 区间上界 / upper bound of the interval
 * @param bigM 可选显式 Big-M / optional explicit Big-M
 * @property bigM 实际使用的 Big-M；null 表示使用默认策略 / effective Big-M; null uses the default policy
 * @property converter 值类型转换器 / value type converter
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 * @property resultVar 结果变量 / result variable
 * @property selectorVars 最大值选择变量 / maximum-selector variables
 * @property helperVariables 辅助变量集合 / helper variable collection
 * @property resultPolynomial 结果线性多项式 / result linear polynomial
 */
class SlackRangeFunction<V>(
    val input: LinearPolynomial<V>,
    val lower: V,
    val upper: V,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V>
        where V : RealNumber<V>, V : NumberField<V> {
    /** 显式 Big-M；为 null 时使用共享的区间推导策略 / Explicit Big-M, or null to use the shared range-derived policy. */
    val bigM: V? = bigM

    init {
        require(lower.isFinite() && upper.isFinite()) {
            "SlackRangeFunction requires finite lower and upper bounds"
        }
        require(!(lower gr upper)) { "SlackRangeFunction requires lower <= upper" }
        require(
            input.constant.isFinite() && input.monomials.all { it.coefficient.isFinite() }
        ) {
            "SlackRangeFunction input polynomial must contain finite values"
        }
        require(hasFiniteCandidates()) {
            "SlackRangeFunction derived candidates must contain finite values"
        }
        require(bigM == null || (bigM.isFinite() && bigM gr converter.zero)) {
            "SlackRangeFunction Big-M must be positive and finite"
        }
    }

    /** Validate derived candidate constants before building the Max helper. / 构建 Max 辅助函数前校验派生常数。 */
    private fun hasFiniteCandidates(): Boolean {
        return try {
            (lower - input.constant).isFinite() &&
                (input.constant - upper).isFinite() &&
                input.monomials.all { (-it.coefficient).isFinite() }
        } catch (_: RuntimeException) {
            false
        }
    }

    private val inner = MaxFunction(
        polynomials = listOf(
            LinearPolynomial(
                input.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
                lower - input.constant
            ),
            LinearPolynomial(input.monomials, input.constant - upper),
            LinearPolynomial(emptyList(), converter.zero)
        ),
        bigM = this.bigM,
        converter = converter,
        name = name,
        displayName = displayName
    )

    val resultVar: AbstractVariableItem<*, *>
        get() = inner.resultVar
    val selectorVars: List<AbstractVariableItem<*, *>>
        get() = inner.selectorVars

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = inner.helperVariables
    override val resultPolynomial: LinearPolynomial<V>
        get() = inner.resultPolynomial

    override fun deferredStructure(): DeferredFunctionStructure = inner.deferredStructure()

    override fun evaluate(values: Map<Symbol, V>): V? = inner.evaluate(values)

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try =
        inner.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try =
        inner.registerConstraints(model)

    companion object {
        /**
         * 从线性中间符号创建区间距离适配器。 / Create a range-distance adapter from a linear intermediate symbol.
         *
         * @param input 线性中间符号 / linear intermediate symbol
         * @param lower 区间下界 / lower bound of the interval
         * @param upper 区间上界 / upper bound of the interval
         * @param bigM 可选显式 Big-M / optional explicit Big-M
         * @param converter 值类型转换器 / value type converter
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return 线性函数符号适配器 / [LinearFunctionSymbolAdapter] instance
         */
        @JvmStatic
        fun <V> fromLinearIntermediateSymbol(
            input: LinearIntermediateSymbol<V>,
            lower: V,
            upper: V,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> =
            LinearFunctionSymbolAdapter(
                SlackRangeFunction(
                    input = input.toLinearPolynomial(),
                    lower = lower,
                    upper = upper,
                    bigM = bigM,
                    converter = converter,
                    name = name,
                    displayName = displayName
                ),
                converter = converter
            )
    }
}
