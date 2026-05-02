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
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * SatisfiedAmountFunction - Counts how many of a list of inequalities are satisfied.
 *
 * For each inequality, creates a BigM flag u[i] (1 if satisfied, 0 if not).
 * Output: y = sum(u[i]) (count of satisfied inequalities).
 * If `amount` is set, adds constraint y >= amount (at least `amount` must be satisfied).
 */
class SatisfiedAmountFunction<V>(
    val inequalities: List<LinearInequality<Flt64>>,
    val amount: UInt64? = null,
    val epsilon: Flt64 = Flt64(1e-6),
    val bigM: Flt64 = Flt64(BIG_M_DEFAULT),
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
        val monos = _uVars.map { LinearMonomial(oneOf<V>(), it) }
        LinearPolynomial(monos, zeroOf<V>())
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val fltValues = values.mapValues { (_, v) -> v.asFlt64() }
        var count = 0
        for (ineq in inequalities) {
            val lhsVal = ineq.lhs.evaluateWith(fltValues)?.toDouble() ?: return null
            val rhsVal = ineq.rhs.evaluateWith(fltValues)?.toDouble() ?: return null
            val satisfied = when (ineq.comparison) {
                Comparison.LE -> lhsVal <= rhsVal + epsilon.toDouble()
                Comparison.GE -> lhsVal >= rhsVal - epsilon.toDouble()
                Comparison.EQ -> kotlin.math.abs(lhsVal - rhsVal) <= epsilon.toDouble()
                Comparison.LT -> lhsVal < rhsVal - epsilon.toDouble()
                Comparison.GT -> lhsVal > rhsVal + epsilon.toDouble()
                Comparison.NE -> kotlin.math.abs(lhsVal - rhsVal) > epsilon.toDouble()
            }
            if (satisfied) count++
        }
        val countVal = Flt64(count.toDouble())
        return if (amount != null) {
            if (count >= amount.toInt()) oneOf<V>() else zeroOf<V>()
        } else {
            @Suppress("UNCHECKED_CAST")
            countVal as V
        }
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        // Register all u variables
        when (val r = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val allConstraints = mutableListOf<LinearInequality<Flt64>>()
        val mD = bigM.toDouble()
        val eps = epsilon.toDouble()

        for (i in inequalities.indices) {
            val ineq = inequalities[i]
            val ui = _uVars[i]
            val lhsMonos = ineq.lhs.monomials.map { LinearMonomial(it.coefficient.asFlt64(), it.symbol) }
            val lhsConst = ineq.lhs.constant.toDouble()
            val rhsConst = ineq.rhs.constant.toDouble()
            val shiftedConst = lhsConst - rhsConst

            when (ineq.comparison) {
                Comparison.LE -> {
                    val monoWithU = lhsMonos + LinearMonomial(Flt64(mD), ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(monoWithU, Flt64(shiftedConst)),
                        LinearPolynomial(emptyList(), Flt64(mD)), Comparison.LE, "${name}_sat_le_ub_$i"
                    )
                    val rhsMinusLhsMonos = lhsMonos.map { LinearMonomial(-it.coefficient, it.symbol) } +
                        LinearMonomial(-Flt64(mD), ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(rhsMinusLhsMonos, Flt64(-shiftedConst)),
                        LinearPolynomial(emptyList(), Flt64(-eps)), Comparison.LE, "${name}_sat_le_lb_$i"
                    )
                }
                Comparison.GE -> {
                    val rhsMinusLhsMonos = lhsMonos.map { LinearMonomial(-it.coefficient, it.symbol) } +
                        LinearMonomial(Flt64(mD), ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(rhsMinusLhsMonos, Flt64(-shiftedConst)),
                        LinearPolynomial(emptyList(), Flt64(mD)), Comparison.LE, "${name}_sat_ge_ub_$i"
                    )
                    val monoWithU = lhsMonos + LinearMonomial(-Flt64(mD), ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(monoWithU, Flt64(shiftedConst)),
                        LinearPolynomial(emptyList(), Flt64(-eps)), Comparison.LE, "${name}_sat_ge_lb_$i"
                    )
                }
                Comparison.EQ -> {
                    val monoWithU1 = lhsMonos + LinearMonomial(Flt64(mD), ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(monoWithU1, Flt64(shiftedConst)),
                        LinearPolynomial(emptyList(), Flt64(mD)), Comparison.LE, "${name}_sat_eq_ub1_$i"
                    )
                    val monoWithU2 = lhsMonos + LinearMonomial(-Flt64(mD), ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(monoWithU2, Flt64(shiftedConst)),
                        LinearPolynomial(emptyList(), Flt64(-mD)), Comparison.GE, "${name}_sat_eq_lb1_$i"
                    )
                    val relaxMono1 = lhsMonos + LinearMonomial(Flt64(mD), ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(relaxMono1, Flt64(shiftedConst)),
                        LinearPolynomial(emptyList(), Flt64(eps)), Comparison.GE, "${name}_sat_eq_ub2_$i"
                    )
                    val relaxMono2 = lhsMonos + LinearMonomial(-Flt64(mD), ui)
                    allConstraints += LinearInequality<Flt64>(
                        LinearPolynomial(relaxMono2, Flt64(shiftedConst)),
                        LinearPolynomial(emptyList(), Flt64(-eps)), Comparison.LE, "${name}_sat_eq_lb2_$i"
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

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
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
            name = name,
            displayName = displayName
        )
    }
}