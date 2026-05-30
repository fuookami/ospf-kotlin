@file:Suppress("unused")

/** Sigmoid 函数符号 / Sigmoid function symbol */
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
 * Sigmoid/阶跃函数符号 / Sigmoid/step function symbol
 *
 * 提供 [SigmoidFunction]，实现 y = (condition > 0 ? 1 : 0) 的线性化建模。
 *
 * Provides [SigmoidFunction] for linearized modeling of y = 1 if condition > 0, else 0.
 */

/**
 * Sigmoid/阶跃函数：当 condition > 0 时 y = 1，否则为 0。
 * Sigmoid/step function: y = 1 if condition > 0, else 0.
 *
 * 使用 Big-M 线性化与非零指示变量。
 * Uses Big-M linearization with nonzero indicators.
 *
 * @property condition 条件线性多项式 / condition linear polynomial
 * @param bigM Big-M 界限（默认 1e6）/ Big-M bound (default 1e6)
 * @param tolerance 零容差（默认 1e-6）/ zero tolerance (default 1e-6)
 * @param strictBoundary 严格边界值（默认 0.5）/ strict boundary value (default 0.5)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class SigmoidFunction<V>(
    val condition: LinearPolynomial<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "sigmoid",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_sig_ind")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_sig_side")

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, indicatorVar)), converter.zero)

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVar, sideVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val condValue = condition.evaluateWith(values) ?: return null
        return if (condValue gr converter.zero) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Nonzero indicator: indicator = 1 iff condition != 0 / 非零指示约束：当且仅当条件 != 0 时指示变量 = 1
        allConstraints += nonzeroIndicatorConstraints(condition, indicatorVar, sideVar, bigM, tolerance, strictBoundary, "${name}_sig_nz")

        // indicator serves as the result: indicator = 1 when condition > 0 / 指示变量即为结果：条件 > 0 时指示变量 = 1

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /** 创建 [SigmoidFunction] 实例。 / Create a [SigmoidFunction] instance. */
        operator fun <V> invoke(
            condition: LinearPolynomial<V>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "sigmoid",
            displayName: String? = null
        ): SigmoidFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SigmoidFunction(condition, bigM, converter = converter, name = name, displayName = displayName)
    }
}
