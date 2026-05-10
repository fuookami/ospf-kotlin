@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf

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
 * Add a list of Flt64-typed constraints to a V-typed MechanismModel, converting via IntoValue.
 * Returns null on success, or the error result on failure.
 *
 * This is the solver-boundary bridge: function symbols internally construct
 * LinearInequality<Flt64> constraints, but the model is V-typed. The converter
 * maps Flt64 coefficients to V.
 */
internal fun <V> addConstraints(model: AbstractLinearMechanismModel<V>, constraints: List<LinearInequality<Flt64>>, converter: IntoValue<V>): Try? where V : RealNumber<V>, V : NumberField<V> {
    for (c in constraints) {
        val vLhs = c.lhs.asVPoly(converter)
        val vRhs = c.rhs.asVPoly(converter)
        val vInequality = LinearInequality(vLhs, vRhs, c.comparison, c.name)
        when (val r = model.addConstraint(relation = vInequality, name = vInequality.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }
    }
    return null
}

private fun <V> LinearPolynomial<Flt64>.asVPoly(converter: IntoValue<V>): LinearPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(converter.intoValue(it.coefficient), it.symbol) },
        converter.intoValue(constant)
    )
}

/**
 * Add a list of Flt64-typed quadratic constraints to a V-typed QuadraticMechanismModel, converting via IntoValue.
 * Returns null on success, or the error result on failure.
 *
 * Solver-boundary bridge: quadratic function symbols internally construct
 * QuadraticInequalityOf<Flt64> constraints, but the model is V-typed.
 */
internal fun <V> addQuadraticConstraints(model: AbstractQuadraticMechanismModel<V>, constraints: List<QuadraticInequalityOf<Flt64>>, converter: IntoValue<V>): Try? where V : RealNumber<V>, V : NumberField<V> {
    for (c in constraints) {
        val vLhs = c.lhs.asVQuadraticPoly(converter)
        val vRhs = c.rhs.asVQuadraticPoly(converter)
        val vInequality = QuadraticInequalityOf(vLhs, vRhs, c.comparison, c.name, c.displayName)
        when (val r = model.addConstraint(relation = vInequality, name = vInequality.name)) {
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
 * Returns a list of named LinearInequality<Flt64> constraints (Flt64-typed for model compatibility).
 */
fun <V> nonzeroIndicatorConstraints(
    poly: LinearPolynomial<V>,
    indVar: AbstractVariableItem<*, *>,
    sideVar: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    strictBoundary: V,
    converter: IntoValue<V>,
    namePrefix: String
): List<LinearInequality<Flt64>> where V : RealNumber<V>, V : NumberField<V> {
    val constraints = mutableListOf<LinearInequality<Flt64>>()
    val polyF = poly.asFlt64Poly(converter)
    val mF = converter.fromValue(bigM)
    val tolF = converter.fromValue(tolerance)
    val sbF = converter.fromValue(strictBoundary)

    // band_ub: poly - M*ind <= tol
    val ubMonos = polyF.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(-mF, indVar)
    constraints += LinearInequality<Flt64>(
        LinearPolynomial(ubMonos, polyF.constant),
        LinearPolynomial(emptyList(), tolF), Comparison.LE, "${namePrefix}_band_ub")

    // band_lb: poly + M*ind >= -tol
    val lbMonos = polyF.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(mF, indVar)
    constraints += LinearInequality<Flt64>(
        LinearPolynomial(lbMonos, polyF.constant),
        LinearPolynomial(emptyList(), -tolF), Comparison.GE, "${namePrefix}_band_lb")

    // out_lb: poly - M*ind - M*side >= strict_boundary - 2M
    val outLbMonos = polyF.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(-mF, indVar) + LinearMonomial(-mF, sideVar)
    constraints += LinearInequality<Flt64>(
        LinearPolynomial(outLbMonos, polyF.constant),
        LinearPolynomial(emptyList(), sbF - mF - mF),
        Comparison.GE, "${namePrefix}_out_lb")

    // out_ub: poly + M*ind - M*side <= -strict_boundary + M
    val outUbMonos = polyF.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(mF, indVar) + LinearMonomial(-mF, sideVar)
    constraints += LinearInequality<Flt64>(
        LinearPolynomial(outUbMonos, polyF.constant),
        LinearPolynomial(emptyList(), -sbF + mF),
        Comparison.LE, "${namePrefix}_out_ub")

    return constraints
}

/**
 * Build indicator constraints for a simple inequality (LE or GE).
 *
 * For LE: when indicator=1, poly <= rhs is enforced.
 * For GE: when indicator=1, poly >= rhs is enforced.
 *
 * Returns LinearInequality<Flt64> constraints (Flt64-typed for model compatibility).
 */
fun <V> simpleIndicatorConstraints(
    ineq: LinearInequality<V>,
    indicator: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    strictBoundary: V,
    converter: IntoValue<V>,
    namePrefix: String
): List<LinearInequality<Flt64>> where V : RealNumber<V>, V : NumberField<V> {
    val constraints = mutableListOf<LinearInequality<Flt64>>()
    val mF = converter.fromValue(bigM)
    val tolF = converter.fromValue(tolerance)
    val sbF = converter.fromValue(strictBoundary)
    val lhsF = ineq.lhs.asFlt64Poly(converter)
    val rhsF = ineq.rhs.asFlt64Poly(converter)
    val polyMonos = lhsF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
    val shiftedConst = lhsF.constant - rhsF.constant

    when (ineq.comparison) {
        Comparison.LE -> {
            // lb: poly - rhs + M*ind >= 0
            constraints += LinearInequality<Flt64>(
                LinearPolynomial(polyMonos + LinearMonomial(mF, indicator), shiftedConst),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${namePrefix}_lb")
            // ub: poly - rhs <= M (always true for reasonable M)
            constraints += LinearInequality<Flt64>(
                LinearPolynomial(polyMonos, shiftedConst),
                LinearPolynomial(emptyList(), mF), Comparison.LE, "${namePrefix}_ub")
        }
        Comparison.GE -> {
            // lb: poly - rhs >= -M (always possible)
            constraints += LinearInequality<Flt64>(
                LinearPolynomial(polyMonos, shiftedConst),
                LinearPolynomial(emptyList(), -mF), Comparison.GE, "${namePrefix}_lb")
            // ub: poly - rhs <= 0 (enforced when indicator=1)
            constraints += LinearInequality<Flt64>(
                LinearPolynomial(polyMonos, shiftedConst),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${namePrefix}_ub")
        }
        Comparison.EQ -> {
            // For EQ, use the full 4-constraint nonzero indicator pattern
            val sideVar = BinVar("${namePrefix}_side")
            constraints += nonzeroIndicatorConstraints(
                LinearPolynomial(polyMonos, shiftedConst),
                indicator, sideVar, mF, tolF, sbF, namePrefix)
        }
        Comparison.LT, Comparison.GT, Comparison.NE -> {
            throw UnsupportedOperationException("Indicator constraints not supported for ${ineq.comparison}")
        }
    }

    return constraints
}

/**
 * Flt64-specific overload of nonzeroIndicatorConstraints for convenience.
 * Used when inputs are already Flt64-typed.
 */
@Deprecated("Use the generic V-typed version with an IntoValue<V> converter.", level = DeprecationLevel.WARNING)
fun nonzeroIndicatorConstraints(
    poly: LinearPolynomial<Flt64>,
    indVar: AbstractVariableItem<*, *>,
    sideVar: AbstractVariableItem<*, *>,
    bigM: Flt64,
    tolerance: Flt64,
    strictBoundary: Flt64,
    namePrefix: String
): List<LinearInequality<Flt64>> {
    val constraints = mutableListOf<LinearInequality<Flt64>>()
    val mD = bigM

    // band_ub: poly - M*ind <= tol
    val ubMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(-mD, indVar)
    constraints += LinearInequality<Flt64>(
        LinearPolynomial(ubMonos, poly.constant),
        LinearPolynomial(emptyList(), tolerance), Comparison.LE, "${namePrefix}_band_ub")

    // band_lb: poly + M*ind >= -tol
    val lbMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(mD, indVar)
    constraints += LinearInequality<Flt64>(
        LinearPolynomial(lbMonos, poly.constant),
        LinearPolynomial(emptyList(), -tolerance), Comparison.GE, "${namePrefix}_band_lb")

    // out_lb: poly - M*ind - M*side >= strict_boundary - 2M
    val outLbMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(-mD, indVar) + LinearMonomial(-mD, sideVar)
    constraints += LinearInequality<Flt64>(
        LinearPolynomial(outLbMonos, poly.constant),
        LinearPolynomial(emptyList(), strictBoundary - mD - mD),
        Comparison.GE, "${namePrefix}_out_lb")

    // out_ub: poly + M*ind - M*side <= -strict_boundary + M
    val outUbMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        LinearMonomial(mD, indVar) + LinearMonomial(-mD, sideVar)
    constraints += LinearInequality<Flt64>(
        LinearPolynomial(outUbMonos, poly.constant),
        LinearPolynomial(emptyList(), -strictBoundary + mD),
        Comparison.LE, "${namePrefix}_out_ub")

    return constraints
}

/**
 * Flt64-specific overload of simpleIndicatorConstraints for convenience.
 * Used when inputs are already Flt64-typed.
 */
@Deprecated("Use the generic V-typed version with an IntoValue<V> converter.", level = DeprecationLevel.WARNING)
fun simpleIndicatorConstraints(
    ineq: LinearInequality<Flt64>,
    indicator: AbstractVariableItem<*, *>,
    bigM: Flt64,
    tolerance: Flt64,
    strictBoundary: Flt64,
    namePrefix: String
): List<LinearInequality<Flt64>> {
    val constraints = mutableListOf<LinearInequality<Flt64>>()
    val mD = bigM
    val polyMonos = ineq.lhs.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
    val shiftedConst = ineq.lhs.constant - ineq.rhs.constant

    when (ineq.comparison) {
        Comparison.LE -> {
            constraints += LinearInequality<Flt64>(
                LinearPolynomial(polyMonos + LinearMonomial(mD, indicator), shiftedConst),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.GE, "${namePrefix}_lb")
            constraints += LinearInequality<Flt64>(
                LinearPolynomial(polyMonos, shiftedConst),
                LinearPolynomial(emptyList(), mD), Comparison.LE, "${namePrefix}_ub")
        }
        Comparison.GE -> {
            constraints += LinearInequality<Flt64>(
                LinearPolynomial(polyMonos, shiftedConst),
                LinearPolynomial(emptyList(), -mD), Comparison.GE, "${namePrefix}_lb")
            constraints += LinearInequality<Flt64>(
                LinearPolynomial(polyMonos, shiftedConst),
                LinearPolynomial(emptyList(), Flt64.zero), Comparison.LE, "${namePrefix}_ub")
        }
        Comparison.EQ -> {
            val sideVar = BinVar("${namePrefix}_side")
            constraints += nonzeroIndicatorConstraints(
                LinearPolynomial(polyMonos, shiftedConst),
                indicator, sideVar, mD, tolerance, strictBoundary, namePrefix)
        }
        Comparison.LT, Comparison.GT, Comparison.NE -> {
            throw UnsupportedOperationException("Indicator constraints not supported for ${ineq.comparison}")
        }
    }

    return constraints
}
