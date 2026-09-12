@file:Suppress("unused")

/** 半连续函数符号 / Semi-continuous function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 半连续变量函数符号 / Semi-continuous variable function symbol
 *
 * 提供 [SemiFunction]，建模 y = 0 或 lb <= y <= ub 的半连续变量。
 *
 * Provides [SemiFunction] for modeling semi-continuous variables where y = 0 or lb <= y <= ub.
*/

/**
 * 半连续变量函数。 / Semi-continuous variable function.
 *
 * 建模 y = 0 或 lb <= y <= ub 的半连续变量。 / Models y where either y = 0 or lb <= y <= ub.
 * 使用结果变量、激活指示变量和两条线性边界约束。 / Uses a result variable,
 * an activation indicator, and two linear domain constraints.
 *
 * @param lb 激活时的下界 / lower bound when active
 * @param ub 激活时的上界（默认 1e6，或通过工厂从变量范围推导）/ upper bound when active (default 1e6, or inferred from variable range through factory)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
*/
class SemiFunction<V>(
    lb: V? = null,
    ub: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "semi",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultVariable, HasResultPolynomial<V>
        where V : RealNumber<V>, V : NumberField<V> {
    val lb: V = lb ?: converter.zero
    val ub: V = ub ?: converter.intoValue(Flt64(1e6))

    init {
        require(this.lb.isFinite() && this.ub.isFinite()) {
            "SemiFunction bounds must be finite"
        }
        require(this.lb leq this.ub) {
            "SemiFunction lower bound must be less than or equal to upper bound"
        }
    }

    override val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_result")
    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_indicator")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, indicatorVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(
            listOf(LinearMonomial(converter.one, resultVar)), converter.zero
        )

    override fun evaluate(values: Map<Symbol, V>): V? {
        val indicator = values[indicatorVar] ?: return null
        val result = values[resultVar] ?: return null
        return if (indicator eq converter.zero) converter.zero else {
            val value = result
            if (value ls lb) lb else if (value gr ub) ub else value
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
        val upperConstraint = LinearInequality(
            LinearPolynomial(
                listOf(
                    LinearMonomial(converter.one, resultVar),
                    LinearMonomial(-ub, indicatorVar)
                ), converter.zero
            ),
            LinearPolynomial(emptyList(), converter.zero),
            Comparison.LE,
            "${name}_semi_upper"
        )
        val lowerConstraint = LinearInequality(
            LinearPolynomial(
                listOf(
                    LinearMonomial(converter.one, resultVar),
                    LinearMonomial(-lb, indicatorVar)
                ), converter.zero
            ),
            LinearPolynomial(emptyList(), converter.zero),
            Comparison.GE,
            "${name}_semi_lower"
        )
        return addConstraints(model, listOf(upperConstraint, lowerConstraint)) ?: ok
    }
    companion object {
        /** 创建 [SemiFunction] 实例。 / Create a [SemiFunction] instance. */
        operator fun <V> invoke(
            lb: V? = null,
            ub: V? = null,
            converter: IntoValue<V>,
            name: String = "semi",
            displayName: String? = null
        ): SemiFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SemiFunction(lb, ub, converter, name, displayName)

        /**
         * 从变量有限边界创建 [SemiFunction] 实例。 / Create a [SemiFunction] instance from finite variable bounds.
         *
         * @param variable 用于推导边界的变量 / variable whose bounds are used for inference
         * @param lb 显式激活下界，优先于变量下界 / explicit active lower bound, preferred over variable lower bound
         * @param ub 显式激活上界，优先于变量上界 / explicit active upper bound, preferred over variable upper bound
         * @param converter 值类型转换器 / value type converter
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [SemiFunction] 实例 / [SemiFunction] instance
        */
        fun <V> from(
            variable: AbstractVariableItem<*, *>,
            lb: V? = null,
            ub: V? = null,
            converter: IntoValue<V>,
            name: String = "semi",
            displayName: String? = null
        ): SemiFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            val range = variable.range.valueRange
            val inferredLb = range?.lowerBound?.value?.unwrapOrNull()?.let { converter.intoValue(it) }
            val inferredUb = range?.upperBound?.value?.unwrapOrNull()?.let { converter.intoValue(it) }
            return SemiFunction(
                lb = lb ?: inferredLb,
                ub = ub ?: inferredUb,
                converter = converter,
                name = name,
                displayName = displayName
            )
        }
    }
}
