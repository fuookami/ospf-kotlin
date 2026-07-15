@file:Suppress("unused")

/** 四舍五入函数符号 / Rounding function symbol */
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
 * 四舍五入函数符号 / Rounding function symbol
 *
 * 提供 [RoundingFunction]，实现 y = round(x) 的线性化建模。
 *
 * Provides [RoundingFunction] for linearized modeling of y = round(x).
*/

/**
 * 四舍五入函数：y = round(x)。
 * Rounding function: y = round(x).
 *
 * 使用整数变量 k、连续小数变量 b 和二值变量 r 处理 0.5 的情况。
 * Uses integer variable k, continuous fractional variable b, and binary r to handle the 0.5 case.
 *
 * @property x 输入线性多项式 / the input linear polynomial
 * @param bigM 小数指示 Big-M（默认 1）/ Big-M for fractional indicator (default 1)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
*/
class RoundingFunction<V>(
    val x: LinearPolynomial<V>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "round",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM?.let { if (it geq converter.one) it else converter.one } ?: converter.one

    val kVar: AbstractVariableItem<*, *> = IntVar("${name}_k")
    val rVar: AbstractVariableItem<*, *> = BinVar("${name}_r")
    val bVar: AbstractVariableItem<*, *> = URealVar("${name}_b")
    val resultVar: AbstractVariableItem<*, *> = IntVar("${name}_round")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(kVar, rVar, bVar, resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xVal = x.evaluateWith(values) ?: return null
        return converter.intoValue(converter.fromValue(xVal).round())
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
        val half = converter.intoValue(Flt64(0.5))
        val eps = converter.intoValue(Flt64(NONZERO_TOLERANCE))
        val bigMValue = bigM
        val allConstraints = mutableListOf<LinearInequality<V>>()
        val xMonos = x.monomials.map { LinearMonomial(it.coefficient, it.symbol) }

        // k = floor(x), same as FloorFunction constraints / k = floor(x)，与 FloorFunction 约束相同
        // k <= x / k <= x
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-one, kVar), x.constant),
            LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_round_k_lb")

        // x < k + 1 => x <= k + 1 - eps / x < k + 1，即 x <= k + 1 - eps
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos + LinearMonomial(-one, kVar), x.constant),
            LinearPolynomial(emptyList(), one - eps), Comparison.LE, "${name}_round_k_ub")

        // b = x - k (fractional part) / b = x - k（小数部分）
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, bVar),
                LinearMonomial(one, kVar)
            ) + xMonos.map { LinearMonomial(-it.coefficient, it.symbol) },
                -x.constant),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_round_decompose")

        // b < 1 => b <= 1 - eps / 小数部分上界：b < 1，即 b <= 1 - eps
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(one, bVar)), zero),
            LinearPolynomial(emptyList(), one - eps), Comparison.LE, "${name}_round_b_ub")

        // r = 1 if b >= 0.5 (round up) / b >= 0.5 时 r = 1（向上取整）
        // b >= 0.5*r / b >= 0.5*r
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, bVar),
                LinearMonomial(-half, rVar)
            ), zero),
            LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_round_r_lb")

        // b <= 0.5 - eps + M*r / r=0 时 b 必须小于 0.5
        // b <= 0.5 - eps + M*r / when r=0, b must be below 0.5
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, bVar),
                LinearMonomial(-bigMValue, rVar)
            ), zero),
            LinearPolynomial(emptyList(), half - eps), Comparison.LE, "${name}_round_r_ub")

        // result = k + r / 结果 = k + r
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(
                LinearMonomial(one, resultVar),
                LinearMonomial(-one, kVar),
                LinearMonomial(-one, rVar)
            ), zero),
            LinearPolynomial(emptyList(), zero), Comparison.EQ, "${name}_round_result")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /** 创建 [RoundingFunction] 实例。 / Create a [RoundingFunction] instance. */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): RoundingFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            RoundingFunction(x, bigM, converter, name = name, displayName = displayName)
    }
}
