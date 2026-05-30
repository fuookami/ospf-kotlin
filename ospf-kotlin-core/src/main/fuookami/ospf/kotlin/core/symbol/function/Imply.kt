@file:Suppress("unused")

/** 蕴含函数符号 / Implication function symbol */
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
 * 蕴含函数符号 / Implication function symbol
 *
 * 提供 [ImplyFunction]，实现 (antecedent > 0 => consequent > 0) 的线性化建模。
 *
 * Provides [ImplyFunction] for linearized modeling of (antecedent > 0 => consequent > 0).
 */

/**
 * 蕴含函数：若 antecedent > 0 则 consequent > 0。
 * Implication function: `if antecedent > 0 then consequent > 0`.
 *
 * 对前件和后件均使用非零指示变量，
 * Uses nonzero indicators for both antecedent and consequent,
 * 通过链接约束：indicator_antecedent <= indicator_consequent。
 * with a linking constraint: indicator_antecedent <= indicator_consequent.
 *
 * @property antecedent 前件（条件）线性多项式 / the antecedent (condition) linear polynomial
 * @property consequent 后件线性多项式 / the consequent linear polynomial
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认 1e6）/ Big-M bound (default 1e6)
 * @param tolerance 零容差（默认 1e-6）/ zero tolerance (default 1e-6)
 * @param strictBoundary 严格边界值（默认 0.5）/ strict boundary value (default 0.5)
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class ImplyFunction<V>(
    val antecedent: LinearPolynomial<V>,
    val consequent: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "imply",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    val antecedentIndicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_ant_nz")
    val antecedentSideVar: AbstractVariableItem<*, *> = BinVar("${name}_ant_side")
    val consequentIndicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_con_nz")
    val consequentSideVar: AbstractVariableItem<*, *> = BinVar("${name}_con_side")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(antecedentIndicatorVar, antecedentSideVar, consequentIndicatorVar, consequentSideVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val antValue = antecedent.evaluateWith(values) ?: return null
        val conValue = consequent.evaluateWith(values) ?: return null
        // Implication: if antecedent > 0, then consequent must be > 0
        // 蕴含：若前件 > 0，则后件必须 > 0
        // Returns 1 if the implication holds, 0 otherwise
        // 蕴含成立返回 1，否则返回 0
        val antecedentPositive = antValue gr converter.zero
        val consequentPositive = conValue gr converter.zero
        return if (!antecedentPositive || consequentPositive) {
            converter.one
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
        val mVal = bigM
        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Nonzero indicators / 非零指示约束
        allConstraints += nonzeroIndicatorConstraints(antecedent, antecedentIndicatorVar, antecedentSideVar, mVal, tolerance, strictBoundary, "${name}_ant")
        allConstraints += nonzeroIndicatorConstraints(consequent, consequentIndicatorVar, consequentSideVar, mVal, tolerance, strictBoundary, "${name}_con")

        // Implication: antecedent_indicator <= consequent_indicator
        // 蕴含约束：前件指示变量 <= 后件指示变量
        // If antecedent is nonzero, consequent must also be nonzero
        // 若前件非零，则后件也必须非零
        allConstraints += LinearInequality(
            LinearPolynomial(
                listOf(
                    LinearMonomial(one, antecedentIndicatorVar),
                    LinearMonomial(-one, consequentIndicatorVar)
                ),
                zero
            ),
            LinearPolynomial(emptyList(), zero),
            Comparison.LE, "${name}_imply_link"
        )

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /**
         * 创建蕴含函数实例 / Create an implication function instance
         * @param antecedent 前件线性多项式 / antecedent linear polynomial
         * @param consequent 后件线性多项式 / consequent linear polynomial
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [ImplyFunction] 实例 / [ImplyFunction] instance
         */
        operator fun <V> invoke(
            antecedent: LinearPolynomial<V>,
            consequent: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): ImplyFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            ImplyFunction(antecedent, consequent, converter, bigM, name = name, displayName = displayName)
    }
}
