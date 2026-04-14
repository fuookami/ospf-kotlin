@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * IfThenFunction: Logical implication - if premise is satisfied, then conclusion must be satisfied.
 *
 * Uses BigM formulation with binary flags pu (premise satisfied) and qu (conclusion satisfied).
 * When constraint=true, enforces pu <= qu (if premise holds, conclusion must hold).
 */
class IfThenFunction<T : Field<T>>(
    val premise: MathLinearInequality,
    val conclusion: MathLinearInequality,
    val constraint: Boolean = true,
    val epsilon: Flt64 = Flt64(1e-6),
    val bigM: Flt64 = Flt64(BIG_M_DEFAULT),
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    val pu: AbstractVariableItem<*, *> = BinVar("${name}_pu")
    val qu: AbstractVariableItem<*, *> = BinVar("${name}_qu")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(pu, qu)

    override fun evaluate(values: Map<Symbol, T>): T? {
        val premiseHolds = checkInequality(premise, values) ?: return null
        val conclusionHolds = checkInequality(conclusion, values) ?: return null
        return if (!premiseHolds || conclusionHolds) oneOf<T>() else zeroOf<T>()
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        when (val r = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val allConstraints = mutableListOf<MathLinearInequality>()

        allConstraints += bigMInequalityConstraints(premise, pu, bigM, epsilon, "${name}_premise")
        allConstraints += bigMInequalityConstraints(conclusion, qu, bigM, epsilon, "${name}_conclusion")

        if (constraint) {
            // pu <= qu  (if premise is true, conclusion must be true)
            allConstraints += MathLinearInequality(
                LinearPolynomial(
                    listOf(LinearMonomial(Flt64.one, pu), LinearMonomial(-Flt64.one, qu)),
                    Flt64.zero
                ),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE,
                "${name}_implication"
            )
        }

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
        operator fun invoke(
            premise: MathLinearInequality,
            conclusion: MathLinearInequality,
            constraint: Boolean = true,
            epsilon: Flt64 = Flt64(1e-6),
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): IfThenFunction<Flt64> = IfThenFunction(
            premise = premise,
            conclusion = conclusion,
            constraint = constraint,
            epsilon = epsilon,
            bigM = bigM ?: Flt64(BIG_M_DEFAULT),
            name = name,
            displayName = displayName
        )
    }
}

/**
 * Build BigM indicator constraints for a MathLinearInequality with a binary flag.
 *
 * When flag=1: the inequality is enforced.
 * When flag=0: the inequality can be violated (relaxed by BigM).
 */
private fun bigMInequalityConstraints(
    ineq: MathLinearInequality,
    flag: AbstractVariableItem<*, *>,
    bigM: Flt64,
    epsilon: Flt64,
    namePrefix: String
): List<MathLinearInequality> {
    val mD = bigM.toDouble()
    val epsD = epsilon.toDouble()
    val constraints = mutableListOf<MathLinearInequality>()

    val lhsMonos = ineq.lhs.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
    val rhsMonos = ineq.rhs.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
    val shiftedConst = (ineq.lhs.constant - ineq.rhs.constant).toDouble()

    val diffMonos = lhsMonos + rhsMonos.map { LinearMonomial(it.coefficient.unaryMinus(), it.symbol) }

    when (ineq.comparison) {
        Comparison.LE -> {
            // flag=1: lhs <= rhs  ->  lhs - rhs <= 0
            constraints += MathLinearInequality(
                LinearPolynomial(diffMonos + LinearMonomial(Flt64(mD), flag), Flt64(shiftedConst - mD)),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE,
                "${namePrefix}_enforce"
            )
            // flag=0: lhs > rhs   ->  rhs - lhs <= M - epsilon
            constraints += MathLinearInequality(
                LinearPolynomial(
                    rhsMonos.map { LinearMonomial(it.coefficient, it.symbol) } +
                        lhsMonos.map { LinearMonomial(it.coefficient.unaryMinus(), it.symbol) } +
                        LinearMonomial(Flt64(-(mD + epsD)), flag),
                    Flt64(-shiftedConst + epsD)
                ),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE,
                "${namePrefix}_relax"
            )
        }

        Comparison.GE -> {
            // flag=1: lhs >= rhs  ->  rhs - lhs <= 0
            constraints += MathLinearInequality(
                LinearPolynomial(
                    rhsMonos.map { LinearMonomial(it.coefficient, it.symbol) } +
                        lhsMonos.map { LinearMonomial(it.coefficient.unaryMinus(), it.symbol) } +
                        LinearMonomial(Flt64(mD), flag),
                    Flt64(-shiftedConst - mD)
                ),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE,
                "${namePrefix}_enforce"
            )
            // flag=0: lhs < rhs   ->  lhs - rhs <= M - epsilon
            constraints += MathLinearInequality(
                LinearPolynomial(diffMonos + LinearMonomial(Flt64(-(mD + epsD)), flag), Flt64(shiftedConst + epsD)),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE,
                "${namePrefix}_relax"
            )
        }

        Comparison.EQ -> {
            // flag=1: lhs == rhs  ->  lhs - rhs <= 0  AND  rhs - lhs <= 0
            constraints += MathLinearInequality(
                LinearPolynomial(diffMonos + LinearMonomial(Flt64(mD), flag), Flt64(shiftedConst - mD)),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE,
                "${namePrefix}_enforce_ub"
            )
            constraints += MathLinearInequality(
                LinearPolynomial(
                    rhsMonos.map { LinearMonomial(it.coefficient, it.symbol) } +
                        lhsMonos.map { LinearMonomial(it.coefficient.unaryMinus(), it.symbol) } +
                        LinearMonomial(Flt64(mD), flag),
                    Flt64(-shiftedConst - mD)
                ),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE,
                "${namePrefix}_enforce_lb"
            )
            // flag=0: can deviate (just need one relaxation direction)
            constraints += MathLinearInequality(
                LinearPolynomial(diffMonos + LinearMonomial(Flt64(-(mD + epsD)), flag), Flt64(shiftedConst + epsD)),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE,
                "${namePrefix}_relax"
            )
        }

        Comparison.LT, Comparison.GT, Comparison.NE -> {
            throw UnsupportedOperationException(
                "IfThenFunction does not support strict inequality: ${ineq.comparison}"
            )
        }
    }

    return constraints
}

/**
 * Check whether a MathLinearInequality holds given resolved symbol values.
 */
private fun checkInequality(ineq: MathLinearInequality, values: Map<Symbol, *>): Boolean? {
    val lhsVal = evalFlt64Poly(ineq.lhs, values) ?: return null
    val rhsVal = ineq.rhs.constant.toDouble()
    val eps = 1e-10
    return when (ineq.comparison) {
        Comparison.LE -> lhsVal <= rhsVal + eps
        Comparison.GE -> lhsVal + eps >= rhsVal
        Comparison.EQ -> kotlin.math.abs(lhsVal - rhsVal) <= eps
        Comparison.LT, Comparison.GT, Comparison.NE -> false
    }
}

/**
 * Evaluate a Flt64 linear polynomial given a map of Symbol -> T values.
 */
private fun evalFlt64Poly(poly: LinearPolynomial<Flt64>, values: Map<Symbol, *>): Double? {
    var sum = 0.0
    for (m in poly.monomials) {
        val sv = values[m.symbol] ?: return null
        val v = when (sv) {
            is Flt64 -> sv.toDouble()
            is Number -> sv.toDouble()
            else -> return null
        }
        sum += m.coefficient.toDouble() * v
    }
    return sum + poly.constant.toDouble()
}
