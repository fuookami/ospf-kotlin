/**
 * 满足数量函数符号 / Satisfied amount function symbol
 *
 * 提供 [SatisfiedAmountFunction]，统计满足的不等式数量。
 *
 * Provides [SatisfiedAmountFunction] for counting how many inequalities are satisfied.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar

/**
 * SatisfiedAmountFunction - Counts how many of a list of inequalities are satisfied.
 *
 * For each inequality, creates a BigM flag u[i] (1 if satisfied, 0 if not).
 * Output: y = sum(u[i]) (count of satisfied inequalities).
 * If `amount` is set, adds constraint y >= amount (at least `amount` must be satisfied).
 */
class SatisfiedAmountFunction<V>(
    val inequalities: List<LinearInequality<V>>,
    val amount: UInt64? = null,
    val epsilon: V,
    val bigM: V,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    private val n: Int get() = inequalities.size

    // Binary flag per inequality: u[i] = 1 if inequality i is satisfied
    private val _uVars: List<BinVar> by lazy {
        inequalities.mapIndexed { i, _ -> BinVar("${name}_u_$i") }
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = _uVars

    /**
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

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
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
        val mD = bigM
        val eps = epsilon

        for (i in inequalities.indices) {
            val ineq = inequalities[i]
            val ui = _uVars[i]
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
                    val monoWithU1 = shiftedMonos + LinearMonomial(mD, ui)
                    allConstraints += LinearInequality(
                        LinearPolynomial(monoWithU1, shiftedConst),
                        LinearPolynomial(emptyList(), mD), Comparison.LE, "${name}_sat_eq_ub1_$i"
                    )
                    val monoWithU2 = shiftedMonos + LinearMonomial(-mD, ui)
                    allConstraints += LinearInequality(
                        LinearPolynomial(monoWithU2, shiftedConst),
                        LinearPolynomial(emptyList(), -mD), Comparison.GE, "${name}_sat_eq_lb1_$i"
                    )
                    val relaxMono1 = shiftedMonos + LinearMonomial(mD, ui)
                    allConstraints += LinearInequality(
                        LinearPolynomial(relaxMono1, shiftedConst),
                        LinearPolynomial(emptyList(), eps), Comparison.GE, "${name}_sat_eq_ub2_$i"
                    )
                    val relaxMono2 = shiftedMonos + LinearMonomial(-mD, ui)
                    allConstraints += LinearInequality(
                        LinearPolynomial(relaxMono2, shiftedConst),
                        LinearPolynomial(emptyList(), -eps), Comparison.LE, "${name}_sat_eq_lb2_$i"
                    )
                }
                Comparison.LT, Comparison.GT, Comparison.NE -> {
                    throw UnsupportedOperationException(
                        "SatisfiedAmountFunction does not support ${ineq.comparison} constraints"
                    )
                }
            }
        }

        // If amount is set: sum(u[i]) >= amount
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
        operator fun <V> invoke(
            inequalities: List<LinearInequality<V>>,
            amount: UInt64? = null,
            epsilon: V,
            bigM: V,
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
