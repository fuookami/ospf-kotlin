@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try

/** Default Big-M constant for linearization. */
const val BIG_M_DEFAULT: Double = 1_000_000.0

/** Minimum viable Big-M value. */
const val BIG_M_MIN: Double = 1.0

/** Tolerance for treating a value as zero. */
const val NONZERO_TOLERANCE: Double = 1e-10

/** Strict boundary for nonzero detection (tolerance + epsilon margin). */
val STRICT_BOUNDARY: Double = NONZERO_TOLERANCE * 16 + Math.pow(2.0, -52.0) * 16

/**
 * Evaluate a linear polynomial given a map of Symbol -> T values.
 * Returns null if any symbol in the polynomial is missing from the map.
 */
fun <T : Ring<T>> LinearPolynomial<T>.evaluateWith(values: Map<Symbol, T>): T? {
    var result = constant
    for (m in monomials) {
        val sv = values[m.symbol] ?: return null
        result += m.coefficient * sv
    }
    return result
}

/**
 * Add a list of constraints to the model, returning early on failure.
 * Returns null on success, or the error result on failure.
 */
internal fun addConstraints(model: AbstractLinearMetaModelF64, constraints: List<MathLinearInequality>): Try? {
    for (c in constraints) {
        when (val r = model.addConstraint(relation = c, name = c.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }
    }
    return null
}

/**
 * Build the 4 nonzero-indicator constraints for a polynomial.
 *
 * When `indicator = 1`: polynomial is constrained to be near zero (within tolerance).
 * When `indicator = 0`: polynomial can be nonzero (relaxed by Big-M).
 * The `sideVar` distinguishes positive vs negative deviation for equality checks.
 *
 * Returns a list of named LinearInequality constraints.
 */
fun nonzeroIndicatorConstraints(
    poly: LinearPolynomial<F64>,
    indVar: AbstractVariableItem<*, *>,
    sideVar: AbstractVariableItem<*, *>,
    bigM: Flt64,
    namePrefix: String
): List<MathLinearInequality> {
    val mD = bigM.toDouble()
    val constraints = mutableListOf<MathLinearInequality>()

    // band_ub: poly - M*ind <= tol
    val ubMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(bigM.unaryMinus(), indVar)
    constraints += MathLinearInequality(
        LinearPolynomial(ubMonos, poly.constant),
        LinearPolynomial(emptyList(), Flt64(NONZERO_TOLERANCE)), Comparison.LE, "${namePrefix}_band_ub")

    // band_lb: poly + M*ind >= -tol
    val lbMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(bigM, indVar)
    constraints += MathLinearInequality(
        LinearPolynomial(lbMonos, poly.constant),
        LinearPolynomial(emptyList(), Flt64(-NONZERO_TOLERANCE)), Comparison.GE, "${namePrefix}_band_lb")

    // out_lb: poly - M*ind - M*side >= strict_boundary - 2M
    val outLbMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(bigM.unaryMinus(), indVar) + LinearMonomial(bigM.unaryMinus(), sideVar)
    constraints += MathLinearInequality(
        LinearPolynomial(outLbMonos, poly.constant),
        LinearPolynomial(emptyList(), Flt64(STRICT_BOUNDARY - 2.0 * mD)),
        Comparison.GE, "${namePrefix}_out_lb")

    // out_ub: poly + M*ind - M*side <= -strict_boundary + M
    val outUbMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(bigM, indVar) + LinearMonomial(bigM.unaryMinus(), sideVar)
    constraints += MathLinearInequality(
        LinearPolynomial(outUbMonos, poly.constant),
        LinearPolynomial(emptyList(), Flt64(-STRICT_BOUNDARY + mD)),
        Comparison.LE, "${namePrefix}_out_ub")

    return constraints
}

/**
 * Build indicator constraints for a simple inequality (LE or GE).
 *
 * For LE: when indicator=1, poly <= rhs is enforced.
 * For GE: when indicator=1, poly >= rhs is enforced.
 */
fun simpleIndicatorConstraints(
    ineq: MathLinearInequality,
    indicator: AbstractVariableItem<*, *>,
    bigM: Flt64,
    namePrefix: String
): List<MathLinearInequality> {
    val mD = bigM.toDouble()
    val constraints = mutableListOf<MathLinearInequality>()
    val polyMonos = ineq.lhs.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
    val shiftedConst = ineq.lhs.constant.toDouble() - ineq.rhs.constant.toDouble()

    when (ineq.comparison) {
        Comparison.LE -> {
            // lb: poly - rhs + M*ind >= 0
            constraints += MathLinearInequality(
                LinearPolynomial(polyMonos + LinearMonomial(bigM, indicator), Flt64(shiftedConst)),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${namePrefix}_lb")
            // ub: poly - rhs <= M (always true for reasonable M)
            constraints += MathLinearInequality(
                LinearPolynomial(polyMonos, Flt64(shiftedConst)),
                LinearPolynomial(emptyList(), bigM), Comparison.LE, "${namePrefix}_ub")
        }
        Comparison.GE -> {
            // lb: poly - rhs >= -M (always possible)
            constraints += MathLinearInequality(
                LinearPolynomial(polyMonos, Flt64(shiftedConst)),
                LinearPolynomial(emptyList(), Flt64(-mD)), Comparison.GE, "${namePrefix}_lb")
            // ub: poly - rhs <= 0 (enforced when indicator=1)
            constraints += MathLinearInequality(
                LinearPolynomial(polyMonos, Flt64(shiftedConst)),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${namePrefix}_ub")
        }
        Comparison.EQ -> {
            // For EQ, use the full 4-constraint nonzero indicator pattern
            val sideVar = BinVar("${namePrefix}_side")
            constraints += nonzeroIndicatorConstraints(
                LinearPolynomial(polyMonos, Flt64(shiftedConst)),
                indicator, sideVar, bigM, namePrefix)
        }
        Comparison.LT, Comparison.GT, Comparison.NE -> {
            throw UnsupportedOperationException("Indicator constraints not supported for ${ineq.comparison}")
        }
    }

    return constraints
}
