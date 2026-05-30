@file:Suppress("unused")

/** 最小最大值函数符号 / Min-max function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
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
 * 最小-最大值函数符号 / MinMax function symbols
 *
 * 提供 [MinMaxFunction] 和 [MaxMinFunction]，用于优化上下文中的最小-最大值建模。
 *
 * Provides [MinMaxFunction] and [MaxMinFunction] for min-max value modeling in optimization contexts.
 */

/**
 * 最小-最大值函数：result = max(polynomials[0], polynomials[1], ...)。
 * MinMax function: result = max(polynomials[0], polynomials[1], ...).
 *
 * 命名为"MinMax"是因为在优化上下文中它计算最大值的最小值。
 * Named "MinMax" because it computes the minimum of the maximum values
 * in optimization contexts. Delegates to MaxFunction internally.
 * 内部委托给 MaxFunction。
 *
 * @property polynomials 输入线性多项式列表 / list of input linear polynomials
 * @param bigM Big-M 界限（默认 1e6）/ Big-M bound (default 1e6)
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class MinMaxFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val inner = MaxFunction(polynomials, bigM, converter, name)

    val resultVar: AbstractVariableItem<*, *>
        get() = inner.resultVar
    val selectorVars: List<AbstractVariableItem<*, *>>
        get() = inner.selectorVars

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = inner.helperVariables

    override val resultPolynomial: LinearPolynomial<V>
        get() = inner.resultPolynomial

    override fun evaluate(values: Map<Symbol, V>): V? {
        return inner.evaluate(values)
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return inner.registerAuxiliaryTokens(tokens)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return inner.registerConstraints(model)
    }
    companion object {
        /** 创建 [MinMaxFunction] 实例。 / Create a [MinMaxFunction] instance. */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): MinMaxFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MinMaxFunction(polynomials, bigM, converter, name, displayName)

        /**
         * 类型化符号工厂：将中间符号列表转换为线性多项式。
         * Typed symbol factory: converts intermediate symbols to linear polynomials.
         */
        @JvmStatic
        @JvmName("fromSymbols")
        fun <V> fromSymbols(
            polynomials: List<LinearIntermediateSymbol<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> = LinearFunctionSymbolAdapter(
            MinMaxFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                converter = converter,
                name = name,
                displayName = displayName
            ),
            converter = converter
        )
    }
}

/**
 * 最大-最小值函数：result = min(polynomials[0], polynomials[1], ...)。
 * MaxMin function: result = min(polynomials[0], polynomials[1], ...).
 *
 * 命名为"MaxMin"是因为在优化上下文中它计算最小值的最大值。
 * Named "MaxMin" because it computes the maximum of the minimum values
 * in optimization contexts. Delegates to MinFunction internally.
 * 内部委托给 MinFunction。
 *
 * @property polynomials 输入线性多项式列表 / list of input linear polynomials
 * @param bigM Big-M 界限（默认 1e6）/ Big-M bound (default 1e6)
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class MaxMinFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val inner = MinFunction(polynomials, bigM, converter, name)

    val resultVar: AbstractVariableItem<*, *>
        get() = inner.resultVar
    val selectorVars: List<AbstractVariableItem<*, *>>
        get() = inner.selectorVars

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = inner.helperVariables

    override val resultPolynomial: LinearPolynomial<V>
        get() = inner.resultPolynomial

    override fun evaluate(values: Map<Symbol, V>): V? {
        return inner.evaluate(values)
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return inner.registerAuxiliaryTokens(tokens)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return inner.registerConstraints(model)
    }
    companion object {
        /** 创建 [MaxMinFunction] 实例。 / Create a [MaxMinFunction] instance. */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): MaxMinFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            MaxMinFunction(polynomials, bigM, converter, name, displayName)

        /**
         * 类型化符号工厂：将中间符号列表转换为线性多项式。
         * Typed symbol factory: converts intermediate symbols to linear polynomials.
         */
        @JvmStatic
        @JvmName("fromSymbols")
        fun <V> fromSymbols(
            polynomials: List<LinearIntermediateSymbol<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> = LinearFunctionSymbolAdapter(
            MaxMinFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                converter = converter,
                name = name,
                displayName = displayName
            ),
            converter = converter
        )
    }
}
