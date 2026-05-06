@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

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
        val unit = inequalities.first().lhs.constant / inequalities.first().lhs.constant
        val zero = unit - unit
        val monos = _uVars.map { LinearMonomial(unit, it) }
        LinearPolynomial(monos, zero)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val unit = inequalities.first().lhs.constant / inequalities.first().lhs.constant
        val zero = unit - unit
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
            if (count >= amount.toInt()) unit else zero
        } else {
            var result = zero
            repeat(count) { result = result + unit }
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
        val allConstraints = mutableListOf<LinearInequality<Flt64>>()
        val mD = converter.fromValue(bigM)
        val eps = converter.fromValue(epsilon)

        for (i in inequalities.indices) {
            val ineq = inequalities[i]
            val ui = _uVars[i]
            val lhsF = ineq.lhs.asFlt64Poly(converter)
            val rhsF = ineq.rhs.asFlt64Poly(converter)
            val lhsMonos = lhsF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
            val shiftedConst = lhsF.constant - rhsF.constant

            when (ineq.comparison) {
                Comparison.LE -> {
                    val monoWithU = lhsMonos + LinearMonomial(mD, ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(monoWithU, shiftedConst),
                        LinearPolynomial(emptyList(), mD), Comparison.LE, "${name}_sat_le_ub_$i"
                    )
                    val rhsMinusLhsMonos = lhsMonos.map { LinearMonomial(-it.coefficient, it.symbol) } +
                        LinearMonomial(-mD, ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(rhsMinusLhsMonos, -shiftedConst),
                        LinearPolynomial(emptyList(), -eps), Comparison.LE, "${name}_sat_le_lb_$i"
                    )
                }
                Comparison.GE -> {
                    val rhsMinusLhsMonos = lhsMonos.map { LinearMonomial(-it.coefficient, it.symbol) } +
                        LinearMonomial(mD, ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(rhsMinusLhsMonos, -shiftedConst),
                        LinearPolynomial(emptyList(), mD), Comparison.LE, "${name}_sat_ge_ub_$i"
                    )
                    val monoWithU = lhsMonos + LinearMonomial(-mD, ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(monoWithU, shiftedConst),
                        LinearPolynomial(emptyList(), -eps), Comparison.LE, "${name}_sat_ge_lb_$i"
                    )
                }
                Comparison.EQ -> {
                    val monoWithU1 = lhsMonos + LinearMonomial(mD, ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(monoWithU1, shiftedConst),
                        LinearPolynomial(emptyList(), mD), Comparison.LE, "${name}_sat_eq_ub1_$i"
                    )
                    val monoWithU2 = lhsMonos + LinearMonomial(-mD, ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(monoWithU2, shiftedConst),
                        LinearPolynomial(emptyList(), -mD), Comparison.GE, "${name}_sat_eq_lb1_$i"
                    )
                    val relaxMono1 = lhsMonos + LinearMonomial(mD, ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(relaxMono1, shiftedConst),
                        LinearPolynomial(emptyList(), eps), Comparison.GE, "${name}_sat_eq_ub2_$i"
                    )
                    val relaxMono2 = lhsMonos + LinearMonomial(-mD, ui)
                    allConstraints += LinearInequality<Flt64>(
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
            val sumMonos = _uVars.map { LinearMonomial(Flt64.one, it) }
            allConstraints += LinearInequality<Flt64>(
                LinearPolynomial(sumMonos, Flt64.zero),
                LinearPolynomial(emptyList(), Flt64(amount.toInt().toDouble())),
                Comparison.GE, "${name}_amount"
            )
        }

        return addConstraints(model, allConstraints, converter) ?: ok
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

        operator fun invoke(
            inequalities: List<LinearInequality<Flt64>>,
            amount: UInt64? = null,
            epsilon: Flt64 = Flt64(1e-6),
            bigM: Flt64 = Flt64(BIG_M_DEFAULT),
            name: String,
            displayName: String? = null
        ): SatisfiedAmountFunction<Flt64> = SatisfiedAmountFunction(
            inequalities = inequalities,
            amount = amount,
            epsilon = epsilon,
            bigM = bigM,
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )
    }
}
