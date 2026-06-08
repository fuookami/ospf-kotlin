@file:Suppress("unused")

/** 满足数量函数符号 / Satisfied amount function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 满足数量函数符号 / Satisfied amount function symbol
 *
 * 提供 [SatisfiedAmountFunction]，统计满足的不等式数量。
 *
 * Provides [SatisfiedAmountFunction] for counting how many inequalities are satisfied.
 */

/**
 * 满足数量函数：统计列表中有多少不等式被满足。
 * SatisfiedAmountFunction - Counts how many of a list of inequalities are satisfied.
 *
 * 对每个不等式，创建 BigM 标志 u[i]（满足时为 1，不满足时为 0）。
 * For each inequality, creates a BigM flag u[i] (1 if satisfied, 0 if not).
 * 输出：y = sum(u[i])（满足的不等式数量）。
 * Output: y = sum(u[i]) (count of satisfied inequalities).
 * 若设置了 `amount`，则添加约束 y >= amount（至少 `amount` 个必须满足）。
 * If `amount` is set, adds constraint y >= amount (at least `amount` must be satisfied).
 *
 * @property inequalities 要统计的线性不等式列表 / list of linear inequalities to count
 * @property amount 可选的最小满足数量 / optional minimum satisfied count
 * @property epsilon 零容差 / zero tolerance
 * @property bigM Big-M 界限（默认从每条 lhs-rhs 范围推导）/ Big-M bound (inferred from each lhs-rhs range by default)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class SatisfiedAmountFunction<V>(
    val inequalities: List<LinearInequality<V>>,
    val amount: UInt64? = null,
    val epsilon: V,
    val bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    private val n: Int get() = inequalities.size

    // Binary flag per inequality: u[i] = 1 if inequality i is satisfied / 每个不等式的二值标志：不等式 i 满足时 u[i] = 1
    private val _uVars: List<BinVar> by lazy {
        inequalities.mapIndexed { i, _ -> BinVar("${name}_u_$i") }
    }

    private val _eqSideVars: List<BinVar?> by lazy {
        inequalities.mapIndexed { i, inequality ->
            if (inequality.comparison == Comparison.EQ) BinVar("${name}_eq_side_$i") else null
        }
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = _uVars + _eqSideVars.filterNotNull()

    /**
     * 结果多项式：sum(u[i]) - 满足的不等式数量。
     * Result polynomial: sum(u[i]) - the count of satisfied inequalities.
     */
    val result: LinearPolynomial<V> by lazy {
        val one = converter.one
        val zero = converter.zero
        val monos = _uVars.map { LinearMonomial(one, it) }
        LinearPolynomial(monos, zero)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val one = converter.one
        val zero = converter.zero
        val epsF = converter.fromValue(epsilon)
        var count = 0
        for (ineq in inequalities) {
            val lhsVal = ineq.lhs.evaluateWith(values)?.let { converter.fromValue(it) } ?: return null
            val rhsVal = ineq.rhs.evaluateWith(values)?.let { converter.fromValue(it) } ?: return null
            val satisfied = when (ineq.comparison) {
                Comparison.LE -> lhsVal <= rhsVal + epsF
                Comparison.GE -> lhsVal >= rhsVal - epsF
                Comparison.EQ -> (lhsVal - rhsVal).abs() <= epsF
                Comparison.LT -> lhsVal < rhsVal - epsF
                Comparison.GT -> lhsVal > rhsVal + epsF
                Comparison.NE -> (lhsVal - rhsVal).abs() > epsF
            }
            if (satisfied) count++
        }
        return if (amount != null) {
            if (count >= amount.toInt()) one else zero
        } else {
            var result = zero
            repeat(count) { result = result + one }
            result
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
        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()
        val eps = epsilon

        for (i in inequalities.indices) {
            val ineq = inequalities[i]
            val ui = _uVars[i]
            val mD = bigM ?: ineq.differencePolynomial().defaultBigM(converter)
            val lhsMonos = ineq.lhs.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
            val rhsMonos = ineq.rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
            val shiftedMonos = lhsMonos + rhsMonos
            val shiftedConst = ineq.lhs.constant - ineq.rhs.constant

            when (ineq.comparison) {
                Comparison.LE -> {
                    val monoWithU = shiftedMonos + LinearMonomial(mD, ui)
                    allConstraints += LinearInequality(
                        LinearPolynomial(monoWithU, shiftedConst),
                        LinearPolynomial(emptyList(), mD), Comparison.LE, "${name}_sat_le_ub_$i"
                    )
                    val rhsMinusLhsMonos = shiftedMonos.map { LinearMonomial(-it.coefficient, it.symbol) } +
                        LinearMonomial(-mD, ui)
                    allConstraints += LinearInequality(
                        LinearPolynomial(rhsMinusLhsMonos, -shiftedConst),
                        LinearPolynomial(emptyList(), -eps), Comparison.LE, "${name}_sat_le_lb_$i"
                    )
                }
                Comparison.GE -> {
                    val rhsMinusLhsMonos = shiftedMonos.map { LinearMonomial(-it.coefficient, it.symbol) } +
                        LinearMonomial(mD, ui)
                    allConstraints += LinearInequality(
                        LinearPolynomial(rhsMinusLhsMonos, -shiftedConst),
                        LinearPolynomial(emptyList(), mD), Comparison.LE, "${name}_sat_ge_ub_$i"
                    )
                    val monoWithU = shiftedMonos + LinearMonomial(-mD, ui)
                    allConstraints += LinearInequality(
                        LinearPolynomial(monoWithU, shiftedConst),
                        LinearPolynomial(emptyList(), -eps), Comparison.LE, "${name}_sat_ge_lb_$i"
                    )
                }
                Comparison.EQ -> {
                    allConstraints += zeroIndicatorConstraints(
                        poly = LinearPolynomial(shiftedMonos, shiftedConst),
                        indicator = ui,
                        sideVar = _eqSideVars[i]!!,
                        bigM = mD,
                        tolerance = eps,
                        strictBoundary = eps,
                        namePrefix = "${name}_sat_eq_$i"
                    )
                }
                Comparison.LT, Comparison.GT, Comparison.NE -> {
                    throw UnsupportedOperationException(
                        "SatisfiedAmountFunction does not support ${ineq.comparison} constraints"
                    )
                }
            }
        }

        // If amount is set: sum(u[i]) >= amount / 若设置了 amount：sum(u[i]) >= amount
        if (amount != null) {
            val sumMonos = _uVars.map { LinearMonomial(one, it) }
            val amountValue = repeatAdd(one, amount.toInt())
            allConstraints += LinearInequality(
                LinearPolynomial(sumMonos, zero),
                LinearPolynomial(emptyList(), amountValue),
                Comparison.GE, "${name}_amount"
            )
        }

        return addConstraints(model, allConstraints) ?: ok
    }
    companion object {
        /** 创建 [SatisfiedAmountFunction] 实例。 / Create a [SatisfiedAmountFunction] instance. */
        operator fun <V> invoke(
            inequalities: List<LinearInequality<V>>,
            amount: UInt64? = null,
            epsilon: V,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountFunction<V> where V : RealNumber<V>, V : NumberField<V> = SatisfiedAmountFunction(
            inequalities = inequalities,
            amount = amount,
            epsilon = epsilon,
            bigM = bigM,
            converter = converter,
            name = name,
            displayName = displayName
        )
    }
}
