/** 大M法函数符号 / Big-M method function symbol */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
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
 * 线性多项式的有限上下界。
 * Finite lower and upper bounds of a linear polynomial.
 *
 * @property lower 下界 / lower bound
 * @property upper 上界 / upper bound
 */
data class LinearPolynomialBounds<V>(
    val lower: V,
    val upper: V
) where V : RealNumber<V>, V : NumberField<V> {
    /** 最大绝对值上界。 / Upper bound of the absolute value. */
    val absMax: V
        get() {
            val lowerAbs = lower.abs()
            val upperAbs = upper.abs()
            return if (lowerAbs geq upperAbs) lowerAbs else upperAbs
        }
}

private fun maxOf(values: Iterable<Flt64>): Flt64 {
    val iterator = values.iterator()
    require(iterator.hasNext()) { "values must not be empty" }
    var result = iterator.next()
    while (iterator.hasNext()) {
        val value = iterator.next()
        if (value gr result) {
            result = value
        }
    }
    return result
}

private fun minOf(values: Iterable<Flt64>): Flt64 {
    val iterator = values.iterator()
    require(iterator.hasNext()) { "values must not be empty" }
    var result = iterator.next()
    while (iterator.hasNext()) {
        val value = iterator.next()
        if (value ls result) {
            result = value
        }
    }
    return result
}

private fun symbolFiniteBounds(symbol: Symbol): Pair<Flt64, Flt64>? {
    val range = when (symbol) {
        is AbstractVariableItem<*, *> -> symbol.range.valueRange
        is IntermediateSymbol<*> -> SolverBoundaryCasts.rangeAsFlt64(symbol)?.valueRange
        else -> null
    } ?: return null

    val lower = range.lowerBound.value.unwrapOrNull() ?: return null
    val upper = range.upperBound.value.unwrapOrNull() ?: return null
    return lower to upper
}

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
 * 基于变量与中间符号的有限范围推导线性多项式上下界。
 * Infer finite bounds of a linear polynomial from variable and intermediate-symbol ranges.
 *
 * 若任一依赖符号缺少有限上下界，则返回 null。
 * Returns null if any dependency symbol lacks finite bounds.
 *
 * @param converter 值类型转换器 / value type converter
 * @return 线性多项式上下界，或 null / linear polynomial bounds, or null
 */
fun <V> LinearPolynomial<V>.finiteBounds(
    converter: IntoValue<V>
): LinearPolynomialBounds<V>? where V : RealNumber<V>, V : NumberField<V> {
    var lower = converter.fromValue(constant)
    var upper = converter.fromValue(constant)

    for (monomial in monomials) {
        val (symbolLower, symbolUpper) = symbolFiniteBounds(monomial.symbol) ?: return null
        val coefficient = converter.fromValue(monomial.coefficient)

        if (coefficient geq Flt64.zero) {
            lower += coefficient * symbolLower
            upper += coefficient * symbolUpper
        } else {
            lower += coefficient * symbolUpper
            upper += coefficient * symbolLower
        }
    }

    return LinearPolynomialBounds(
        lower = converter.intoValue(lower),
        upper = converter.intoValue(upper)
    )
}

/**
 * 基于变量与中间符号的有限范围推导二次多项式上下界。
 * Infer finite bounds of a quadratic polynomial from variable and intermediate-symbol ranges.
 *
 * @param converter 值类型转换器 / value type converter
 * @return 二次多项式上下界，或 null / quadratic polynomial bounds, or null
 */
fun <V> QuadraticPolynomial<V>.finiteBounds(
    converter: IntoValue<V>
): LinearPolynomialBounds<V>? where V : RealNumber<V>, V : NumberField<V> {
    var lower = converter.fromValue(constant)
    var upper = converter.fromValue(constant)

    for (monomial in monomials) {
        val coefficient = converter.fromValue(monomial.coefficient)
        val (lower1, upper1) = symbolFiniteBounds(monomial.symbol1) ?: return null
        val termBounds = if (monomial.symbol2 == null) {
            if (coefficient geq Flt64.zero) {
                coefficient * lower1 to coefficient * upper1
            } else {
                coefficient * upper1 to coefficient * lower1
            }
        } else if (monomial.symbol1 == monomial.symbol2) {
            val squareLower = if (lower1 leq Flt64.zero && upper1 geq Flt64.zero) {
                Flt64.zero
            } else {
                val lowerSquare = lower1 * lower1
                val upperSquare = upper1 * upper1
                if (lowerSquare ls upperSquare) lowerSquare else upperSquare
            }
            val squareUpper = maxOf(listOf(lower1 * lower1, upper1 * upper1))
            if (coefficient geq Flt64.zero) {
                coefficient * squareLower to coefficient * squareUpper
            } else {
                coefficient * squareUpper to coefficient * squareLower
            }
        } else {
            val (lower2, upper2) = symbolFiniteBounds(monomial.symbol2!!) ?: return null
            val products = listOf(
                coefficient * lower1 * lower2,
                coefficient * lower1 * upper2,
                coefficient * upper1 * lower2,
                coefficient * upper1 * upper2
            )
            minOf(products) to maxOf(products)
        }
        lower += termBounds.first
        upper += termBounds.second
    }

    return LinearPolynomialBounds(
        lower = converter.intoValue(lower),
        upper = converter.intoValue(upper)
    )
}

/**
 * 将一个候选 Big-M 调整为至少 [BIG_M_MIN]。
 * Clamp a candidate Big-M to at least [BIG_M_MIN].
 */
fun <V> ensurePositiveBigM(
    value: V,
    converter: IntoValue<V>
): V where V : RealNumber<V>, V : NumberField<V> {
    val minimum = converter.intoValue(Flt64(BIG_M_MIN))
    return if (value geq minimum) value else minimum
}

private fun <V> relaxBigM(
    bigM: V,
    margin: V
): V where V : RealNumber<V>, V : NumberField<V> {
    return bigM + margin.abs()
}

/**
 * 线性多项式默认 Big-M：优先使用有限范围的最大绝对值。
 * Default Big-M for a linear polynomial: finite-range absolute maximum first.
 */
fun <V> LinearPolynomial<V>.defaultBigM(
    converter: IntoValue<V>,
    fallback: V = converter.intoValue(Flt64(BIG_M_DEFAULT))
): V where V : RealNumber<V>, V : NumberField<V> {
    return finiteBounds(converter)?.absMax?.let { ensurePositiveBigM(it, converter) } ?: fallback
}

/**
 * 二次多项式默认 Big-M：优先使用有限范围的最大绝对值。
 * Default Big-M for a quadratic polynomial: finite-range absolute maximum first.
 */
fun <V> QuadraticPolynomial<V>.defaultBigM(
    converter: IntoValue<V>,
    fallback: V = converter.intoValue(Flt64(BIG_M_DEFAULT))
): V where V : RealNumber<V>, V : NumberField<V> {
    return finiteBounds(converter)?.absMax?.let { ensurePositiveBigM(it, converter) } ?: fallback
}

/**
 * 多个线性多项式默认 Big-M：取各自有限范围最大绝对值的最大值。
 * Default Big-M for linear polynomials: max absolute bound across all inputs.
 */
fun <V> Iterable<LinearPolynomial<V>>.defaultBigM(
    converter: IntoValue<V>,
    fallback: V = converter.intoValue(Flt64(BIG_M_DEFAULT))
): V where V : RealNumber<V>, V : NumberField<V> {
    var result: V? = null
    for (poly in this) {
        val candidate = poly.finiteBounds(converter)?.absMax ?: return fallback
        result = if (result == null || candidate gr result) {
            candidate
        } else {
            result
        }
    }
    return result?.let { ensurePositiveBigM(it, converter) } ?: fallback
}

/**
 * 构建线性不等式两侧差值 lhs-rhs。
 * Build the lhs-rhs difference polynomial for a linear inequality.
 */
fun <V> LinearInequality<V>.differencePolynomial(): LinearPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        lhs.monomials + rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        lhs.constant - rhs.constant
    )
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
 * Add a list of V-generic constraints directly to a V-generic MechanismModel.
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
 * Add a list of V-generic quadratic constraints directly to a V-generic QuadraticMechanismModel.
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
 * intermediate-symbol constraints parameterized as V inside generic paths.
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
    val bandM = relaxBigM(bigM, tolerance)
    val outM = relaxBigM(bigM, strictBoundary)

    // band_ub: poly - M*ind <= tol / 上界带：多项式 - M*指示变量 <= 容差
    val ubMonos = polyMonos + LinearMonomial(-bandM, indVar)
    constraints += LinearInequality(
        LinearPolynomial(ubMonos, poly.constant),
        LinearPolynomial(emptyList(), tolerance), Comparison.LE, "${namePrefix}_band_ub")

    // band_lb: poly + M*ind >= -tol / 下界带：多项式 + M*指示变量 >= -容差
    val lbMonos = polyMonos + LinearMonomial(bandM, indVar)
    constraints += LinearInequality(
        LinearPolynomial(lbMonos, poly.constant),
        LinearPolynomial(emptyList(), -tolerance), Comparison.GE, "${namePrefix}_band_lb")

    // out_lb: poly - M*ind - M*side >= strict_boundary - 2M / 外部下界：多项式 - M*指示变量 - M*辅助变量 >= 严格边界 - 2M
    val outLbMonos = polyMonos + LinearMonomial(-outM, indVar) + LinearMonomial(-outM, sideVar)
    constraints += LinearInequality(
        LinearPolynomial(outLbMonos, poly.constant),
        LinearPolynomial(emptyList(), strictBoundary - outM - outM),
        Comparison.GE, "${namePrefix}_out_lb")

    // out_ub: poly + M*ind - M*side <= -strict_boundary + M / 外部上界：多项式 + M*指示变量 - M*辅助变量 <= -严格边界 + M
    val outUbMonos = polyMonos + LinearMonomial(outM, indVar) + LinearMonomial(-outM, sideVar)
    constraints += LinearInequality(
        LinearPolynomial(outUbMonos, poly.constant),
        LinearPolynomial(emptyList(), -strictBoundary + outM),
        Comparison.LE, "${namePrefix}_out_ub")

    return constraints
}

/**
 * 为零值检测构建指示约束。
 * Build indicator constraints for detecting a zero polynomial value.
 *
 * 当 `indicator = 1` 时：poly 在容差范围内。
 * When `indicator = 1`: poly is within tolerance.
 * 当 `indicator = 0` 时：poly 至少偏离 [strictBoundary]。
 * When `indicator = 0`: poly deviates by at least [strictBoundary].
 */
fun <V> zeroIndicatorConstraints(
    poly: LinearPolynomial<V>,
    indicator: AbstractVariableItem<*, *>,
    sideVar: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    strictBoundary: V,
    namePrefix: String
): List<LinearInequality<V>> where V : RealNumber<V>, V : NumberField<V> {
    val constraints = mutableListOf<LinearInequality<V>>()
    val polyMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
    val bandM = relaxBigM(bigM, tolerance)
    val outM = relaxBigM(bigM, strictBoundary)

    // poly <= tolerance + M*(1-indicator) / 上界带：indicator=1 时 poly <= tolerance
    constraints += LinearInequality(
        LinearPolynomial(polyMonos + LinearMonomial(bandM, indicator), poly.constant),
        LinearPolynomial(emptyList(), tolerance + bandM),
        Comparison.LE, "${namePrefix}_zero_band_ub")

    // poly >= -tolerance - M*(1-indicator) / 下界带：indicator=1 时 poly >= -tolerance
    constraints += LinearInequality(
        LinearPolynomial(polyMonos + LinearMonomial(-bandM, indicator), poly.constant),
        LinearPolynomial(emptyList(), -tolerance - bandM),
        Comparison.GE, "${namePrefix}_zero_band_lb")

    // poly >= strictBoundary - M*indicator - M*(1-side) / 正向违反：indicator=0 且 side=1 时生效
    constraints += LinearInequality(
        LinearPolynomial(polyMonos + LinearMonomial(outM, indicator) + LinearMonomial(-outM, sideVar), poly.constant),
        LinearPolynomial(emptyList(), strictBoundary - outM),
        Comparison.GE, "${namePrefix}_zero_out_lb")

    // poly <= -strictBoundary + M*indicator + M*side / 负向违反：indicator=0 且 side=0 时生效
    constraints += LinearInequality(
        LinearPolynomial(polyMonos + LinearMonomial(-outM, indicator) + LinearMonomial(-outM, sideVar), poly.constant),
        LinearPolynomial(emptyList(), -strictBoundary),
        Comparison.LE, "${namePrefix}_zero_out_ub")

    return constraints
}

/**
 * 为正数检测构建指示约束。
 * Build indicator constraints for detecting a positive polynomial value.
 *
 * 当 `indicator = 1` 时：poly >= tolerance。
 * When `indicator = 1`: poly >= tolerance.
 * 当 `indicator = 0` 时：poly <= 0。
 * When `indicator = 0`: poly <= 0.
 */
fun <V> positiveIndicatorConstraints(
    poly: LinearPolynomial<V>,
    indicator: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    namePrefix: String
): List<LinearInequality<V>> where V : RealNumber<V>, V : NumberField<V> {
    val constraints = mutableListOf<LinearInequality<V>>()
    val polyMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
    val lowerRelaxM = relaxBigM(bigM, tolerance)

    // poly <= M * indicator / 上界：indicator=0 时 poly <= 0
    constraints += LinearInequality(
        LinearPolynomial(polyMonos + LinearMonomial(-bigM, indicator), poly.constant),
        LinearPolynomial(emptyList(), poly.constant - poly.constant),
        Comparison.LE, "${namePrefix}_positive_ub")

    // poly >= tolerance - M * (1 - indicator) / 下界：indicator=1 时 poly >= tolerance
    constraints += LinearInequality(
        LinearPolynomial(polyMonos + LinearMonomial(-lowerRelaxM, indicator), poly.constant),
        LinearPolynomial(emptyList(), tolerance - lowerRelaxM),
        Comparison.GE, "${namePrefix}_positive_lb")

    return constraints
}

/**
 * 为非负检测构建指示约束。
 * Build indicator constraints for detecting a nonnegative polynomial value.
 *
 * 当 `indicator = 1` 时：poly >= 0。
 * When `indicator = 1`: poly >= 0.
 * 当 `indicator = 0` 时：poly <= -tolerance。
 * When `indicator = 0`: poly <= -tolerance.
 */
fun <V> nonnegativeIndicatorConstraints(
    poly: LinearPolynomial<V>,
    indicator: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    namePrefix: String
): List<LinearInequality<V>> where V : RealNumber<V>, V : NumberField<V> {
    val constraints = mutableListOf<LinearInequality<V>>()
    val polyMonos = poly.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
    val upperRelaxM = relaxBigM(bigM, tolerance)

    // poly >= -M * (1 - indicator) / 下界：indicator=1 时 poly >= 0
    constraints += LinearInequality(
        LinearPolynomial(polyMonos + LinearMonomial(-bigM, indicator), poly.constant),
        LinearPolynomial(emptyList(), -bigM),
        Comparison.GE, "${namePrefix}_nonnegative_lb")

    // poly <= -tolerance + M * indicator / 上界：indicator=0 时 poly <= -tolerance
    constraints += LinearInequality(
        LinearPolynomial(polyMonos + LinearMonomial(-upperRelaxM, indicator), poly.constant),
        LinearPolynomial(emptyList(), -tolerance),
        Comparison.LE, "${namePrefix}_nonnegative_ub")

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
    namePrefix: String,
    sideVar: AbstractVariableItem<*, *>? = null
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> {
    val zero = ineq.lhs.constant - ineq.lhs.constant
    val constraints = mutableListOf<LinearInequality<V>>()
    val diffMonos = ineq.lhs.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
        ineq.rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
    val shiftedConst = ineq.lhs.constant - ineq.rhs.constant
    val strictRelaxM = relaxBigM(bigM, strictBoundary)

    when (ineq.comparison) {
        Comparison.LE -> {
            // satisfied: diff <= tolerance + M*(1-indicator)
            // 满足：indicator=1 时 diff <= tolerance
            constraints += LinearInequality(
                LinearPolynomial(diffMonos + LinearMonomial(bigM, indicator), shiftedConst),
                LinearPolynomial(emptyList(), tolerance + bigM), Comparison.LE, "${namePrefix}_sat")
            // violated: diff >= strictBoundary - M*indicator
            // 违反：indicator=0 时 diff >= strictBoundary
            constraints += LinearInequality(
                LinearPolynomial(diffMonos + LinearMonomial(strictRelaxM, indicator), shiftedConst),
                LinearPolynomial(emptyList(), strictBoundary), Comparison.GE, "${namePrefix}_violated")
        }
        Comparison.GE -> {
            // satisfied: diff >= -tolerance - M*(1-indicator)
            // 满足：indicator=1 时 diff >= -tolerance
            constraints += LinearInequality(
                LinearPolynomial(diffMonos + LinearMonomial(-bigM, indicator), shiftedConst),
                LinearPolynomial(emptyList(), -tolerance - bigM), Comparison.GE, "${namePrefix}_sat")
            // violated: diff <= -strictBoundary + M*indicator
            // 违反：indicator=0 时 diff <= -strictBoundary
            constraints += LinearInequality(
                LinearPolynomial(diffMonos + LinearMonomial(-strictRelaxM, indicator), shiftedConst),
                LinearPolynomial(emptyList(), -strictBoundary), Comparison.LE, "${namePrefix}_violated")
        }
        Comparison.EQ -> {
            val eqSideVar = sideVar ?: BinVar("${namePrefix}_side")
            constraints += zeroIndicatorConstraints(
                LinearPolynomial(diffMonos, shiftedConst),
                indicator, eqSideVar, bigM, tolerance, strictBoundary, namePrefix
            )
        }
        Comparison.LT, Comparison.GT, Comparison.NE -> {
            return Failed(ErrorCode.ApplicationError, "Indicator constraints not supported for ${ineq.comparison}")
        }
    }

    return Ok(constraints)
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
