@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Big-M 线性化工具 / Big-M linearization utilities
 *
 * 提供 Big-M 常量定义及非零指示约束、简单指示约束等线性化辅助函数。
 *
 * Provides Big-M constant definitions and linearization helper functions
 * for nonzero indicator constraints and simple indicator constraints.
 */

/** Default Big-M constant for linearization. */
const val BIG_M_DEFAULT: Double = 1_000_000.0

/** Minimum viable Big-M value. */
const val BIG_M_MIN: Double = 1.0

/** Tolerance for treating a value as zero. */
const val NONZERO_TOLERANCE: Double = 1e-10

/** Strict boundary for nonzero detection (tolerance + epsilon margin). */
val STRICT_BOUNDARY: Double = NONZERO_TOLERANCE * 16 + Math.pow(2.0, -52.0) * 16

/**
 * Evaluate a linear polynomial given a map of Symbol -> V values.
 * Returns null if any symbol in the polynomial is missing from the map.
 */
fun <V> LinearPolynomial<V>.evaluateWith(values: Map<Symbol, V>): V? where V : RealNumber<V>, V : NumberField<V> {
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
internal fun <V> addConstraints(model: AbstractLinearMetaModel<V>, constraints: List<LinearInequality<V>>): Try? where V : RealNumber<V>, V : NumberField<V> {
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
 * Add a list of V-typed constraints directly to a V-typed MechanismModel.
 * Returns null on success, or the error result on failure.
 */
internal fun <V> addConstraints(model: AbstractLinearMechanismModel<V>, constraints: List<LinearInequality<V>>): Try? where V : RealNumber<V>, V : NumberField<V> {
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
 * Add a list of V-typed quadratic constraints directly to a V-typed QuadraticMechanismModel.
 * Returns null on success, or the error result on failure.
 */
internal fun <V> addQuadraticConstraints(model: AbstractQuadraticMechanismModel<V>, constraints: List<QuadraticInequalityOf<V>>): Try? where V : RealNumber<V>, V : NumberField<V> {
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
 * This avoids the V -> Flt64 -> V conversion round-trip and keeps
 * intermediate-symbol constraints typed as V inside generic paths.
 */
fun <V> nonzeroIndicatorConstraints(
    poly: LinearPolynomial<V>,
    indVar: AbstractVariableItem<*, *>,
    sideVar: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    strictBoundary: V,
    namePrefix: String
): List<LinearInequality<V>> where V : RealNumber<V>, V : NumberField<V> {
    val constraints = mutableListOf<LinearInequality<V>>()
    val polyMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) }

    // band_ub: poly - M*ind <= tol
    val ubMonos = polyMonos + LinearMonomial(-bigM, indVar)
    constraints += LinearInequality(
        LinearPolynomial(ubMonos, poly.constant),
        LinearPolynomial(emptyList(), tolerance), Comparison.LE, "${namePrefix}_band_ub")

    // band_lb: poly + M*ind >= -tol
    val lbMonos = polyMonos + LinearMonomial(bigM, indVar)
    constraints += LinearInequality(
        LinearPolynomial(lbMonos, poly.constant),
        LinearPolynomial(emptyList(), -tolerance), Comparison.GE, "${namePrefix}_band_lb")

    // out_lb: poly - M*ind - M*side >= strict_boundary - 2M
    val outLbMonos = polyMonos + LinearMonomial(-bigM, indVar) + LinearMonomial(-bigM, sideVar)
    constraints += LinearInequality(
        LinearPolynomial(outLbMonos, poly.constant),
        LinearPolynomial(emptyList(), strictBoundary - bigM - bigM),
        Comparison.GE, "${namePrefix}_out_lb")

    // out_ub: poly + M*ind - M*side <= -strict_boundary + M
    val outUbMonos = polyMonos + LinearMonomial(bigM, indVar) + LinearMonomial(-bigM, sideVar)
    constraints += LinearInequality(
        LinearPolynomial(outUbMonos, poly.constant),
        LinearPolynomial(emptyList(), -strictBoundary + bigM),
        Comparison.LE, "${namePrefix}_out_ub")

    return constraints
}

/**
 * Build indicator constraints for a simple inequality (LE or GE).
 *
 * For LE: when indicator=1, poly <= rhs is enforced.
 * For GE: when indicator=1, poly >= rhs is enforced.
 */
fun <V> simpleIndicatorConstraints(
    ineq: LinearInequality<V>,
    indicator: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    strictBoundary: V,
    namePrefix: String
): List<LinearInequality<V>> where V : RealNumber<V>, V : NumberField<V> {
    val zero = ineq.lhs.constant - ineq.lhs.constant
    val constraints = mutableListOf<LinearInequality<V>>()
    val diffMonos = ineq.lhs.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        ineq.rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
    val shiftedConst = ineq.lhs.constant - ineq.rhs.constant

    when (ineq.comparison) {
        Comparison.LE -> {
            // lb: poly - rhs + M*ind >= 0
            constraints += LinearInequality(
                LinearPolynomial(diffMonos + LinearMonomial(bigM, indicator), shiftedConst),
                LinearPolynomial(emptyList(), zero), Comparison.GE, "${namePrefix}_lb")
            // ub: poly - rhs <= M (always true for reasonable M)
            constraints += LinearInequality(
                LinearPolynomial(diffMonos, shiftedConst),
                LinearPolynomial(emptyList(), bigM), Comparison.LE, "${namePrefix}_ub")
        }
        Comparison.GE -> {
            // lb: poly - rhs >= -M (always possible)
            constraints += LinearInequality(
                LinearPolynomial(diffMonos, shiftedConst),
                LinearPolynomial(emptyList(), -bigM), Comparison.GE, "${namePrefix}_lb")
            // ub: poly - rhs <= 0 (enforced when indicator=1)
            constraints += LinearInequality(
                LinearPolynomial(diffMonos, shiftedConst),
                LinearPolynomial(emptyList(), zero), Comparison.LE, "${namePrefix}_ub")
        }
        Comparison.EQ -> {
            val sideVar = BinVar("${namePrefix}_side")
            constraints += nonzeroIndicatorConstraints(
                LinearPolynomial(diffMonos, shiftedConst),
                indicator, sideVar, bigM, tolerance, strictBoundary, namePrefix
            )
        }
        Comparison.LT, Comparison.GT, Comparison.NE -> {
            throw UnsupportedOperationException("Indicator constraints not supported for ${ineq.comparison}")
        }
    }

    return constraints
}

internal fun <V> repeatAdd(
    one: V,
    count: Int
): V where V : RealNumber<V>, V : NumberField<V> {
    require(count >= 0) { "count must be non-negative" }
    var result = one - one
    repeat(count) {
        result += one
    }
    return result
}
