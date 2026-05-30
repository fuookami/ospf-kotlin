/** 大M法函数符号 / Big-M method function symbol */
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

/** 默认 Big-M 线性化常量。 / Default Big-M constant for linearization. */
const val BIG_M_DEFAULT: Double = 1_000_000.0

/** 最小可用 Big-M 值。 / Minimum viable Big-M value. */
const val BIG_M_MIN: Double = 1.0

/** 将值视为零的容差。 / Tolerance for treating a value as zero. */
const val NONZERO_TOLERANCE: Double = 1e-10

/** 非零检测的严格边界（容差 + epsilon 边距）。 / Strict boundary for nonzero detection (tolerance + epsilon margin). */
val STRICT_BOUNDARY: Double = NONZERO_TOLERANCE * 16 + Math.pow(2.0, -52.0) * 16

/**
 * 在给定 Symbol -> V 值映射下计算线性多项式的值。
 * Evaluate a linear polynomial given a map of Symbol -> V values.
 * 如果多项式中的任何符号不在映射中，则返回 null。
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
 * 将约束列表添加到模型中，失败时提前返回。
 * Add a list of constraints to the model, returning early on failure.
 * 成功时返回 null，失败时返回错误结果。
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
 * 将 V 类型约束列表直接添加到 V 类型机制模型中。
 * Add a list of V-typed constraints directly to a V-typed MechanismModel.
 * 成功时返回 null，失败时返回错误结果。
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
 * 将 V 类型二次约束列表直接添加到 V 类型二次机制模型中。
 * Add a list of V-typed quadratic constraints directly to a V-typed QuadraticMechanismModel.
 * 成功时返回 null，失败时返回错误结果。
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
 * 为多项式构建 4 个非零指示约束。
 * Build the 4 nonzero-indicator constraints for a polynomial.
 *
 * 当 `indicator = 1` 时：多项式被约束为接近零（在容差范围内）。
 * When `indicator = 1`: polynomial is constrained to be near zero (within tolerance).
 * 当 `indicator = 0` 时：多项式可以非零（通过 Big-M 放松）。
 * When `indicator = 0`: polynomial can be nonzero (relaxed by Big-M).
 * `sideVar` 用于区分正负偏差以进行等式检查。
 * The `sideVar` distinguishes positive vs negative deviation for equality checks.
 *
 * 这避免了 V -> Flt64 -> V 的往返转换，并在泛型路径中保持中间符号约束为 V 类型。
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

    // band_ub: poly - M*ind <= tol / 上界带：多项式 - M*指示变量 <= 容差
    val ubMonos = polyMonos + LinearMonomial(-bigM, indVar)
    constraints += LinearInequality(
        LinearPolynomial(ubMonos, poly.constant),
        LinearPolynomial(emptyList(), tolerance), Comparison.LE, "${namePrefix}_band_ub")

    // band_lb: poly + M*ind >= -tol / 下界带：多项式 + M*指示变量 >= -容差
    val lbMonos = polyMonos + LinearMonomial(bigM, indVar)
    constraints += LinearInequality(
        LinearPolynomial(lbMonos, poly.constant),
        LinearPolynomial(emptyList(), -tolerance), Comparison.GE, "${namePrefix}_band_lb")

    // out_lb: poly - M*ind - M*side >= strict_boundary - 2M / 外部下界：多项式 - M*指示变量 - M*辅助变量 >= 严格边界 - 2M
    val outLbMonos = polyMonos + LinearMonomial(-bigM, indVar) + LinearMonomial(-bigM, sideVar)
    constraints += LinearInequality(
        LinearPolynomial(outLbMonos, poly.constant),
        LinearPolynomial(emptyList(), strictBoundary - bigM - bigM),
        Comparison.GE, "${namePrefix}_out_lb")

    // out_ub: poly + M*ind - M*side <= -strict_boundary + M / 外部上界：多项式 + M*指示变量 - M*辅助变量 <= -严格边界 + M
    val outUbMonos = polyMonos + LinearMonomial(bigM, indVar) + LinearMonomial(-bigM, sideVar)
    constraints += LinearInequality(
        LinearPolynomial(outUbMonos, poly.constant),
        LinearPolynomial(emptyList(), -strictBoundary + bigM),
        Comparison.LE, "${namePrefix}_out_ub")

    return constraints
}

/**
 * 为简单不等式（LE 或 GE）构建指示约束。
 * Build indicator constraints for a simple inequality (LE or GE).
 *
 * 对于 LE：当 indicator=1 时，强制 poly <= rhs。
 * For LE: when indicator=1, poly <= rhs is enforced.
 * 对于 GE：当 indicator=1 时，强制 poly >= rhs。
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
            // lb: poly - rhs + M*ind >= 0 / 下界：多项式 - 右侧 + M*指示变量 >= 0
            constraints += LinearInequality(
                LinearPolynomial(diffMonos + LinearMonomial(bigM, indicator), shiftedConst),
                LinearPolynomial(emptyList(), zero), Comparison.GE, "${namePrefix}_lb")
            // ub: poly - rhs <= M (always true for reasonable M) / 上界：多项式 - 右侧 <= M（对合理 M 值恒成立）
            constraints += LinearInequality(
                LinearPolynomial(diffMonos, shiftedConst),
                LinearPolynomial(emptyList(), bigM), Comparison.LE, "${namePrefix}_ub")
        }
        Comparison.GE -> {
            // lb: poly - rhs >= -M (always possible) / 下界：多项式 - 右侧 >= -M（始终可行）
            constraints += LinearInequality(
                LinearPolynomial(diffMonos, shiftedConst),
                LinearPolynomial(emptyList(), -bigM), Comparison.GE, "${namePrefix}_lb")
            // ub: poly - rhs <= 0 (enforced when indicator=1) / 上界：多项式 - 右侧 <= 0（指示变量=1 时强制生效）
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
