/** 大M法函数符号 / Big-M method function symbol */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenList
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.TokenListSnapshot
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Big-M 线性化工具 / Big-M linearization utilities
 *
 * 提供 Big-M 常量定义及非零指示约束、简单指示约束等线性化辅助函数。 / Provides Big-M constant definitions and linearization helper functions
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
 * 线性多项式的有限上下界。 / Finite lower and upper bounds of a linear polynomial.
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
            return if (lowerAbs.compareTo(upperAbs) >= 0) lowerAbs else upperAbs
        }
}

/**
 * 判断 Flt64 是否可作为已证明的有限 solver 值。 / Check whether Flt64 is a proven finite solver value.
 *
 * `Flt64.minimum/maximum` 是全范围哨兵，不是可用于证明 Big-M 的边界。 / `Flt64.minimum/maximum`
 * are full-range sentinels, not bounds that can prove a Big-M value.
 */
private fun Flt64.isUsableBigMValue(): Boolean {
    return isFinite() &&
        compareTo(Flt64.minimum) > 0 &&
        compareTo(Flt64.maximum) < 0
}

/** 安全执行 Flt64 加法。 / Safely execute Flt64 addition. */
private fun addBigMValues(lhs: Flt64, rhs: Flt64): Flt64? {
    return try {
        (lhs + rhs).takeIf { it.isUsableBigMValue() }
    } catch (_: RuntimeException) {
        null
    }
}

/** 安全执行 Flt64 乘法。 / Safely execute Flt64 multiplication. */
private fun multiplyBigMValues(lhs: Flt64, rhs: Flt64): Flt64? {
    return try {
        (lhs * rhs).takeIf { it.isUsableBigMValue() }
    } catch (_: RuntimeException) {
        null
    }
}

/** 安全地从泛型值转换为 Flt64。 / Safely convert a generic value to Flt64. */
private fun <V> fromBigMValueOrNull(
    converter: IntoValue<V>,
    value: V
): Flt64? where V : RealNumber<V>, V : NumberField<V> {
    return try {
        converter.fromValue(value).takeIf { it.isUsableBigMValue() }
    } catch (_: RuntimeException) {
        null
    }
}

/** 安全地转换为泛型值并验证回转结果。 / Safely convert to a generic value and validate the round trip. */
private fun <V> intoBigMValueOrNull(
    converter: IntoValue<V>,
    value: Flt64
): V? where V : RealNumber<V>, V : NumberField<V> {
    if (!value.isUsableBigMValue()) {
        return null
    }
    return try {
        val converted = converter.intoValue(value)
        fromBigMValueOrNull(converter, converted)?.let { converted }
    } catch (_: RuntimeException) {
        null
    }
}

/**
 * 计算可迭代值中的最大值。 / Compute the maximum value in an iterable of values.
 *
 * @param values 非空的可迭代值集合 / non-empty iterable of values
 * @return 最大值 / the maximum value
*/
private fun maxOf(values: Iterable<Flt64>): Flt64? {
    var result: Flt64? = null
    for (value in values) {
        val current = result
        result = if (current == null || value.compareTo(current) > 0) {
            value
        } else {
            current
        }
    }
    return result
}

/**
 * 计算可迭代值中的最小值。 / Compute the minimum value in an iterable of values.
 *
 * @param values 非空的可迭代值集合 / non-empty iterable of values
 * @return 最小值 / the minimum value
*/
private fun minOf(values: Iterable<Flt64>): Flt64? {
    var result: Flt64? = null
    for (value in values) {
        val current = result
        result = if (current == null || value.compareTo(current) < 0) {
            value
        } else {
            current
        }
    }
    return result
}

/**
 * 获取符号的有限上下界。 / Get the finite lower and upper bounds of a symbol.
 *
 * @param symbol 待查询的符号 / the symbol to query
 * @return 符号的有限上下界对，若符号无有限范围则返回 null / pair of finite lower and upper bounds, or null if the symbol has no finite range
*/
private fun symbolFiniteBounds(symbol: Symbol): Pair<Flt64, Flt64>? {
    return try {
        val range = when (symbol) {
            is AbstractVariableItem<*, *> -> symbol.range.valueRange
            is IntermediateSymbol<*> -> SolverBoundaryCasts.rangeAsFlt64(symbol)?.valueRange
            else -> null
        } ?: return null

        val lower = range.lowerBound.value.unwrapOrNull() ?: return null
        val upper = range.upperBound.value.unwrapOrNull() ?: return null
        if (!lower.isUsableBigMValue() || !upper.isUsableBigMValue() || lower.compareTo(upper) > 0) {
            return null
        }
        lower to upper
    } catch (_: RuntimeException) {
        null
    }
}

/**
 * 在给定 Symbol -> V 值映射下计算线性多项式的值。 / Evaluate a linear polynomial given a map of Symbol -> V values.
 * 如果多项式中的任何符号不在映射中，则返回 null。 / Returns null if any symbol in the polynomial is missing from the map.
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
 * 基于变量与中间符号的有限范围推导线性多项式上下界。 / Infer finite bounds of a linear polynomial from variable and intermediate-symbol ranges.
 *
 * 若任一依赖符号缺少有限上下界，则返回 null。 / Returns null if any dependency symbol lacks finite bounds.
 *
 * @param converter 值类型转换器 / value type converter
 * @return 线性多项式上下界，或 null / linear polynomial bounds, or null
*/
fun <V> LinearPolynomial<V>.finiteBounds(
    converter: IntoValue<V>
): LinearPolynomialBounds<V>? where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val initialLower: Flt64 = fromBigMValueOrNull(converter, constant) ?: return null
        var lower: Flt64 = initialLower
        var upper: Flt64 = initialLower

        for (monomial in monomials) {
            val (symbolLower, symbolUpper) = symbolFiniteBounds(monomial.symbol) ?: return null
            val coefficient = fromBigMValueOrNull(converter, monomial.coefficient) ?: return null
            val lowerTerm: Flt64
            val upperTerm: Flt64
            if (coefficient.compareTo(Flt64.zero) >= 0) {
                lowerTerm = multiplyBigMValues(coefficient, symbolLower) ?: return null
                upperTerm = multiplyBigMValues(coefficient, symbolUpper) ?: return null
            } else {
                lowerTerm = multiplyBigMValues(coefficient, symbolUpper) ?: return null
                upperTerm = multiplyBigMValues(coefficient, symbolLower) ?: return null
            }
            lower = addBigMValues(lower, lowerTerm) ?: return null
            upper = addBigMValues(upper, upperTerm) ?: return null
            if (lower.compareTo(upper) > 0) {
                return null
            }
        }

        val lowerValue = intoBigMValueOrNull(converter, lower) ?: return null
        val upperValue = intoBigMValueOrNull(converter, upper) ?: return null
        if (lowerValue.compareTo(upperValue) > 0) {
            return null
        }
        LinearPolynomialBounds(
            lower = lowerValue,
            upper = upperValue
        )
    } catch (_: RuntimeException) {
        null
    }
}

/**
 * 基于变量与中间符号的有限范围推导二次多项式上下界。 / Infer finite bounds of a quadratic polynomial from variable and intermediate-symbol ranges.
 *
 * @param converter 值类型转换器 / value type converter
 * @return 二次多项式上下界，或 null / quadratic polynomial bounds, or null
*/
fun <V> QuadraticPolynomial<V>.finiteBounds(
    converter: IntoValue<V>
): LinearPolynomialBounds<V>? where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val initialLower: Flt64 = fromBigMValueOrNull(converter, constant) ?: return null
        var lower: Flt64 = initialLower
        var upper: Flt64 = initialLower

        for (monomial in monomials) {
            val coefficient = fromBigMValueOrNull(converter, monomial.coefficient) ?: return null
            val (lower1, upper1) = symbolFiniteBounds(monomial.symbol1) ?: return null
            val termBounds = if (monomial.symbol2 == null) {
                if (coefficient.compareTo(Flt64.zero) >= 0) {
                    val lowerTerm = multiplyBigMValues(coefficient, lower1) ?: return null
                    val upperTerm = multiplyBigMValues(coefficient, upper1) ?: return null
                    lowerTerm to upperTerm
                } else {
                    val lowerTerm = multiplyBigMValues(coefficient, upper1) ?: return null
                    val upperTerm = multiplyBigMValues(coefficient, lower1) ?: return null
                    lowerTerm to upperTerm
                }
            } else if (monomial.symbol1 == monomial.symbol2) {
                val lowerSquare = multiplyBigMValues(lower1, lower1) ?: return null
                val upperSquare = multiplyBigMValues(upper1, upper1) ?: return null
                val squareLower = if (lower1.compareTo(Flt64.zero) <= 0 && upper1.compareTo(Flt64.zero) >= 0) {
                    Flt64.zero
                } else if (lowerSquare.compareTo(upperSquare) <= 0) {
                    lowerSquare
                } else {
                    upperSquare
                }
                val squareUpper = maxOf(listOf(lowerSquare, upperSquare)) ?: return null
                if (coefficient.compareTo(Flt64.zero) >= 0) {
                    val lowerTerm = multiplyBigMValues(coefficient, squareLower) ?: return null
                    val upperTerm = multiplyBigMValues(coefficient, squareUpper) ?: return null
                    lowerTerm to upperTerm
                } else {
                    val lowerTerm = multiplyBigMValues(coefficient, squareUpper) ?: return null
                    val upperTerm = multiplyBigMValues(coefficient, squareLower) ?: return null
                    lowerTerm to upperTerm
                }
            } else {
                val symbol2 = monomial.symbol2 ?: return null
                val (lower2, upper2) = symbolFiniteBounds(symbol2) ?: return null
                val products = listOf(
                    multiplyBigMValues(
                        multiplyBigMValues(coefficient, lower1) ?: return null,
                        lower2
                    ) ?: return null,
                    multiplyBigMValues(
                        multiplyBigMValues(coefficient, lower1) ?: return null,
                        upper2
                    ) ?: return null,
                    multiplyBigMValues(
                        multiplyBigMValues(coefficient, upper1) ?: return null,
                        lower2
                    ) ?: return null,
                    multiplyBigMValues(
                        multiplyBigMValues(coefficient, upper1) ?: return null,
                        upper2
                    ) ?: return null
                )
                val lowerTerm = minOf(products) ?: return null
                val upperTerm = maxOf(products) ?: return null
                lowerTerm to upperTerm
            }
            lower = addBigMValues(lower, termBounds.first) ?: return null
            upper = addBigMValues(upper, termBounds.second) ?: return null
            if (lower.compareTo(upper) > 0) {
                return null
            }
        }

        val lowerValue = intoBigMValueOrNull(converter, lower) ?: return null
        val upperValue = intoBigMValueOrNull(converter, upper) ?: return null
        if (lowerValue.compareTo(upperValue) > 0) {
            return null
        }
        LinearPolynomialBounds(
            lower = lowerValue,
            upper = upperValue
        )
    } catch (_: RuntimeException) {
        null
    }
}

/**
 * 将一个候选 Big-M 调整为至少 [BIG_M_MIN]。 / Clamp a candidate Big-M to at least [BIG_M_MIN].
*/
fun <V> ensurePositiveBigM(
    value: V,
    converter: IntoValue<V>
): V where V : RealNumber<V>, V : NumberField<V> {
    val minimum = converter.intoValue(Flt64(BIG_M_MIN))
    return if (value.compareTo(minimum) >= 0) value else minimum
}

/** 从已验证边界安全解析 Big-M 候选。 / Resolve a Big-M candidate safely from validated bounds. */
private fun <V> finiteBigMOrNull(
    bounds: LinearPolynomialBounds<V>?,
    converter: IntoValue<V>
): V? where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (bounds == null) {
            return null
        }
        val lower = fromBigMValueOrNull(converter, bounds.lower) ?: return null
        val upper = fromBigMValueOrNull(converter, bounds.upper) ?: return null
        val lowerAbs = lower.abs().takeIf { it.isUsableBigMValue() } ?: return null
        val upperAbs = upper.abs().takeIf { it.isUsableBigMValue() } ?: return null
        val candidate = if (lowerAbs.compareTo(upperAbs) >= 0) lowerAbs else upperAbs
        val candidateValue = intoBigMValueOrNull(converter, candidate) ?: return null
        val normalized = ensurePositiveBigM(candidateValue, converter)
        val normalizedSolverValue = fromBigMValueOrNull(converter, normalized) ?: return null
        if (normalizedSolverValue.compareTo(Flt64.zero) <= 0) {
            null
        } else {
            normalized
        }
    } catch (_: RuntimeException) {
        null
    }
}

private fun <V> relaxBigM(
    bigM: V,
    margin: V
): V where V : RealNumber<V>, V : NumberField<V> {
    return bigM + margin.abs()
}

/**
 * 校验旧版指示约束 helper 的 solver 边界值。 / Validate solver-boundary values used by legacy indicator helpers.
 *
 * 这些 helper 保留旧的 List 返回签名，因此必须在构造前后拒绝非有限值，不能把错误延迟到模型写入阶段。
 * The helpers retain their legacy List return type, so non-finite values must be rejected before and after
 * construction instead of being deferred until model insertion.
 */
private fun <V> isUsableLegacyIndicatorValue(value: V): Boolean
    where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!value.isFinite()) {
            return false
        }
        val solverValue = value.toFltX()
        val nan = solverValue.constants.nan
        solverValue.isFinite() &&
            (nan == null || solverValue != nan) &&
            solverValue.compareTo(FltX.minimum) > 0 &&
            solverValue.compareTo(FltX.maximum) < 0
    } catch (_: RuntimeException) {
        false
    }
}

private fun <V> hasUsableLegacyIndicatorPolynomial(
    polynomial: LinearPolynomial<V>
): Boolean where V : RealNumber<V>, V : NumberField<V> {
    return try {
        if (!isUsableLegacyIndicatorValue(polynomial.constant)) {
            return false
        }
        val zero = polynomial.constant - polynomial.constant
        val effectiveCoefficients = LinkedHashMap<Symbol, V>()
        for (monomial in polynomial.monomials) {
            if (!isUsableLegacyIndicatorValue(monomial.coefficient)) {
                return false
            }
            val coefficient = (effectiveCoefficients[monomial.symbol] ?: zero) + monomial.coefficient
            if (!isUsableLegacyIndicatorValue(coefficient)) {
                return false
            }
            effectiveCoefficients[monomial.symbol] = coefficient
        }
        true
    } catch (_: RuntimeException) {
        false
    }
}

private fun <V> validateLegacyIndicatorInputs(
    poly: LinearPolynomial<V>,
    bigM: V,
    tolerance: V,
    strictBoundary: V? = null
) where V : RealNumber<V>, V : NumberField<V> {
    require(hasUsableLegacyIndicatorPolynomial(poly)) {
        "indicator polynomial contains a non-finite or unrepresentable solver value"
    }
    require(isUsableLegacyIndicatorValue(bigM)) {
        "indicator Big-M must be finite and solver-representable"
    }
    val zero = bigM - bigM
    require(bigM.compareTo(zero) > 0) {
        "indicator Big-M must be greater than zero"
    }
    require(isUsableLegacyIndicatorValue(tolerance) && tolerance.compareTo(zero) >= 0) {
        "indicator tolerance must be finite and non-negative"
    }
    strictBoundary?.let {
        require(isUsableLegacyIndicatorValue(it) && it.compareTo(zero) > 0) {
            "indicator strict boundary must be finite and greater than zero"
        }
    }
}

private fun <V> validateLegacyIndicatorConstraints(
    constraints: List<LinearInequality<V>>
) where V : RealNumber<V>, V : NumberField<V> {
    require(constraints.all { constraint ->
        hasUsableLegacyIndicatorPolynomial(constraint.lhs) &&
            hasUsableLegacyIndicatorPolynomial(constraint.rhs)
    }) {
        "indicator helper produced a non-finite or unrepresentable constraint"
    }
}

private fun <V> legacyIndicatorFailure(
    operation: String,
    error: RuntimeException
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> {
    val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
    return Failed(
        ErrorCode.IllegalArgument,
        "$operation 失败：$detail / $operation failed: $detail"
    )
}

private inline fun <V> checkedLegacyIndicatorConstraints(
    operation: String,
    builder: () -> List<LinearInequality<V>>
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        Ok(builder())
    } catch (error: RuntimeException) {
        legacyIndicatorFailure(operation, error)
    }
}

/**
 * 线性多项式默认 Big-M：优先使用有限范围的最大绝对值。 / Default Big-M for a linear polynomial: finite-range absolute maximum first.
*/
fun <V> LinearPolynomial<V>.defaultBigM(
    converter: IntoValue<V>,
    fallback: V = converter.intoValue(Flt64(BIG_M_DEFAULT))
): V where V : RealNumber<V>, V : NumberField<V> {
    return finiteBigMOrNull(finiteBounds(converter), converter) ?: fallback
}

/**
 * 二次多项式默认 Big-M：优先使用有限范围的最大绝对值。 / Default Big-M for a quadratic polynomial: finite-range absolute maximum first.
*/
fun <V> QuadraticPolynomial<V>.defaultBigM(
    converter: IntoValue<V>,
    fallback: V = converter.intoValue(Flt64(BIG_M_DEFAULT))
): V where V : RealNumber<V>, V : NumberField<V> {
    return finiteBigMOrNull(finiteBounds(converter), converter) ?: fallback
}

/**
 * 多个线性多项式默认 Big-M：取各自有限范围最大绝对值的最大值。 / Default Big-M for linear polynomials: max absolute bound across all inputs.
*/
fun <V> Iterable<LinearPolynomial<V>>.defaultBigM(
    converter: IntoValue<V>,
    fallback: V = converter.intoValue(Flt64(BIG_M_DEFAULT))
): V where V : RealNumber<V>, V : NumberField<V> {
    return try {
        var result: V? = null
        var resultSolverValue: Flt64? = null
        for (poly in this) {
            val candidate = finiteBigMOrNull(poly.finiteBounds(converter), converter) ?: return fallback
            val candidateSolverValue = fromBigMValueOrNull(converter, candidate) ?: return fallback
            val currentSolverValue = resultSolverValue
            if (currentSolverValue == null || candidateSolverValue.compareTo(currentSolverValue) > 0) {
                result = candidate
                resultSolverValue = candidateSolverValue
            }
        }
        result ?: fallback
    } catch (_: RuntimeException) {
        fallback
    }
}

/**
 * 构建线性不等式两侧差值 lhs-rhs。 / Build the lhs-rhs difference polynomial for a linear inequality.
 *
 * 此 API 保持非空返回签名；若底层数值运算产生溢出，结果中的非有限值会由
 * [finiteBounds] 以 null 拒绝，而不会被饱和值或默认 Big-M 掩盖。 / This API keeps its
 * non-null return signature; if underlying arithmetic overflows, [finiteBounds] rejects
 * the resulting non-finite value with null instead of hiding it behind saturation or a default Big-M.
 */
fun <V> LinearInequality<V>.differencePolynomial(): LinearPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        lhs.monomials + rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
        lhs.constant - rhs.constant
    )
}

/**
 * 返回约束写入失败及回滚失败的组合结果。 / Combines a constraint-write failure with a rollback failure.
 */
private fun rollbackFailure(failure: Try, rollback: Try): Try {
    if (rollback is Ok) {
        return failure
    }

    val errors = ArrayList<Error<ErrorCode>>()
    when (failure) {
        is Ok -> {}
        is Failed -> errors.add(failure.error)
        is Fatal -> errors.addAll(failure.errors)
    }
    when (rollback) {
        is Ok -> {}
        is Failed -> errors.add(rollback.error)
        is Fatal -> errors.addAll(rollback.errors)
    }
    return Fatal(errors)
}

/** 将约束写入异常转换为失败结果 / Convert a constraint-write exception into a failure result. */
private fun constraintWriteFailure(operation: String, error: RuntimeException): Try {
    val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
    return Failed(
        ErrorCode.ApplicationError,
        "$operation 失败：$detail / $operation failed: $detail"
    )
}

/** 安全执行约束回滚 / Execute constraint rollback without leaking runtime exceptions. */
private fun rollbackConstraintsSafely(rollback: () -> Try): Try {
    return try {
        rollback()
    } catch (error: RuntimeException) {
        constraintWriteFailure(
            operation = "回滚约束 / Roll back constraints",
            error = error
        )
    }
}

private data class TokenStateCheckpoint<V : RealNumber<V>>(
    val tokenList: AbstractMutableTokenList<V>?,
    val state: TokenListSnapshot<V>?
)

/**
 * 读取模型 token 状态快照；可变列表不支持快照时直接失败。
 * Read a model token-state snapshot; fail before writing when a mutable list cannot be snapshotted.
 */
@Suppress("UNCHECKED_CAST")
private fun <V> snapshotModelTokens(
    tokens: AbstractTokenTable<V>
): Ret<TokenStateCheckpoint<V>> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        val tokenList = tokens.tokenList as? AbstractMutableTokenList<V>
        if (tokenList == null) {
            Ok(TokenStateCheckpoint(tokenList = null, state = null))
        } else {
            val state = tokenList.snapshotState()
            if (state == null) {
                return Failed(
                    ErrorCode.ApplicationError,
                    "模型 token 列表不支持事务快照。 / The model token list does not support transactional snapshots."
                )
            }
            Ok(TokenStateCheckpoint(tokenList = tokenList, state = state))
        }
    } catch (error: RuntimeException) {
        val detail = error.message?.takeIf { it.isNotBlank() }
            ?: error::class.simpleName
            ?: "unknown runtime error"
        Failed(
            ErrorCode.ApplicationError,
            "读取 token 状态快照失败：$detail / Failed to read token state snapshot: $detail"
        )
    }
}

/** 安全恢复模型 token 状态 / Safely restore model token state. */
private fun <V> restoreModelTokens(
    checkpoint: TokenStateCheckpoint<V>
): Try where V : RealNumber<V>, V : NumberField<V> {
    val tokenList = checkpoint.tokenList
    val state = checkpoint.state
    if (tokenList == null && state == null) {
        return ok
    }
    if (tokenList == null || state == null) {
        return Failed(
            ErrorCode.ApplicationError,
            "模型 token 状态快照不完整。 / The model token-state snapshot is incomplete."
        )
    }
    return try {
        tokenList.restoreState(state)
    } catch (error: RuntimeException) {
        constraintWriteFailure(
            operation = "恢复 token 状态 / Restore token state",
            error = error
        )
    }
}

/** 同时回滚约束和 token 状态 / Roll back both constraints and token state. */
private fun <V> rollbackModelRegistration(
    failure: Try,
    rollbackConstraints: () -> Try,
    tokenCheckpoint: TokenStateCheckpoint<V>
): Try where V : RealNumber<V>, V : NumberField<V> {
    val constraintRollback = rollbackConstraintsSafely(rollbackConstraints)
    val tokenRollback = restoreModelTokens(tokenCheckpoint)
    return rollbackFailure(
        rollbackFailure(failure, constraintRollback),
        tokenRollback
    )
}

/**
 * 将约束列表原子地添加到模型中，失败时回滚本次调用。 / Atomically adds constraints to the model and rolls back this call on failure.
 * 成功时返回 null，失败时返回错误结果。 / Returns null on success, or the error result on failure.
 */
internal fun <V> addConstraints(model: AbstractLinearMetaModel<V>, constraints: List<LinearInequality<V>>): Try? where V : RealNumber<V>, V : NumberField<V> {
    val originalConstraintCount = try {
        model.constraints.size
    } catch (error: RuntimeException) {
        return constraintWriteFailure(
            operation = "读取约束数量 / Read constraint count",
            error = error
        )
    }
    val tokenCheckpoint = when (val result = snapshotModelTokens(model.tokens)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    for (c in constraints) {
        val result = try {
            model.addConstraint(relation = c, name = c.name)
        } catch (error: RuntimeException) {
            return rollbackModelRegistration(
                failure = constraintWriteFailure(
                    operation = "写入约束 / Write constraint",
                    error = error
                ),
                rollbackConstraints = {
                    model.rollbackConstraintsTo(originalConstraintCount)
                },
                tokenCheckpoint = tokenCheckpoint
            )
        }
        when (val r = result) {
            is Ok -> {}
            is Failed -> {
                return rollbackModelRegistration(
                    failure = Failed(r.error),
                    rollbackConstraints = {
                        model.rollbackConstraintsTo(originalConstraintCount)
                    },
                    tokenCheckpoint = tokenCheckpoint
                )
            }
            is Fatal -> {
                return rollbackModelRegistration(
                    failure = Fatal(r.errors),
                    rollbackConstraints = {
                        model.rollbackConstraintsTo(originalConstraintCount)
                    },
                    tokenCheckpoint = tokenCheckpoint
                )
            }
        }
    }
    return null
}

/**
 * 将 V 类型约束列表原子地添加到 V 类型机制模型中。 / Atomically adds V-generic constraints to a V-generic MechanismModel.
 * 成功时返回 null，失败时返回错误结果。 / Returns null on success, or the error result on failure.
 */
internal fun <V> addConstraints(model: AbstractLinearMechanismModel<V>, constraints: List<LinearInequality<V>>): Try? where V : RealNumber<V>, V : NumberField<V> {
    val originalConstraintCount = try {
        model.constraints.size
    } catch (error: RuntimeException) {
        return constraintWriteFailure(
            operation = "读取约束数量 / Read constraint count",
            error = error
        )
    }
    val tokenCheckpoint = when (val result = snapshotModelTokens(model.tokens)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    for (c in constraints) {
        val result = try {
            model.addConstraint(relation = c, name = c.name)
        } catch (error: RuntimeException) {
            return rollbackModelRegistration(
                failure = constraintWriteFailure(
                    operation = "写入约束 / Write constraint",
                    error = error
                ),
                rollbackConstraints = {
                    model.rollbackConstraintsTo(originalConstraintCount)
                },
                tokenCheckpoint = tokenCheckpoint
            )
        }
        when (val r = result) {
            is Ok -> {}
            is Failed -> {
                return rollbackModelRegistration(
                    failure = Failed(r.error),
                    rollbackConstraints = {
                        model.rollbackConstraintsTo(originalConstraintCount)
                    },
                    tokenCheckpoint = tokenCheckpoint
                )
            }
            is Fatal -> {
                return rollbackModelRegistration(
                    failure = Fatal(r.errors),
                    rollbackConstraints = {
                        model.rollbackConstraintsTo(originalConstraintCount)
                    },
                    tokenCheckpoint = tokenCheckpoint
                )
            }
        }
    }
    return null
}

/**
 * 将 V 类型二次约束列表原子地添加到 V 类型二次机制模型中。 / Atomically adds V-generic quadratic constraints to a V-generic QuadraticMechanismModel.
 * 成功时返回 null，失败时返回错误结果。 / Returns null on success, or the error result on failure.
 */
internal fun <V> addQuadraticConstraints(model: AbstractQuadraticMechanismModel<V>, constraints: List<QuadraticInequalityOf<V>>): Try? where V : RealNumber<V>, V : NumberField<V> {
    val originalConstraintCount = try {
        model.constraints.size
    } catch (error: RuntimeException) {
        return constraintWriteFailure(
            operation = "读取约束数量 / Read constraint count",
            error = error
        )
    }
    val tokenCheckpoint = when (val result = snapshotModelTokens(model.tokens)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    for (c in constraints) {
        val result = try {
            model.addConstraint(relation = c, name = c.name)
        } catch (error: RuntimeException) {
            return rollbackModelRegistration(
                failure = constraintWriteFailure(
                    operation = "写入二次约束 / Write quadratic constraint",
                    error = error
                ),
                rollbackConstraints = {
                    model.rollbackConstraintsTo(originalConstraintCount)
                },
                tokenCheckpoint = tokenCheckpoint
            )
        }
        when (val r = result) {
            is Ok -> {}
            is Failed -> {
                return rollbackModelRegistration(
                    failure = Failed(r.error),
                    rollbackConstraints = {
                        model.rollbackConstraintsTo(originalConstraintCount)
                    },
                    tokenCheckpoint = tokenCheckpoint
                )
            }
            is Fatal -> {
                return rollbackModelRegistration(
                    failure = Fatal(r.errors),
                    rollbackConstraints = {
                        model.rollbackConstraintsTo(originalConstraintCount)
                    },
                    tokenCheckpoint = tokenCheckpoint
                )
            }
        }
    }
    return null
}

/**
 * 为多项式构建 4 个非零指示约束。 / Build the 4 nonzero-indicator constraints for a polynomial.
 *
 * 当 `indicator = 1` 时：多项式被约束为接近零（在容差范围内）。 / When `indicator = 1`: polynomial is constrained to be near zero (within tolerance).
 * 当 `indicator = 0` 时：多项式可以非零（通过 Big-M 放松）。 / When `indicator = 0`: polynomial can be nonzero (relaxed by Big-M).
 * `sideVar` 用于区分正负偏差以进行等式检查。 / The `sideVar` distinguishes positive vs negative deviation for equality checks.
 *
 * 这避免了 V -> Flt64 -> V 的往返转换，并在泛型路径中保持中间符号约束为 V 类型。 / This avoids the V -> Flt64 -> V conversion round-trip and keeps
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
    validateLegacyIndicatorInputs(
        poly = poly,
        bigM = bigM,
        tolerance = tolerance,
        strictBoundary = strictBoundary
    )
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

    validateLegacyIndicatorConstraints(constraints)
    return constraints
}

/** 安全构建旧版非零指示约束。 / Safely build legacy nonzero-indicator constraints. */
internal fun <V> safeNonzeroIndicatorConstraints(
    poly: LinearPolynomial<V>,
    indVar: AbstractVariableItem<*, *>,
    sideVar: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    strictBoundary: V,
    namePrefix: String
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> =
    checkedLegacyIndicatorConstraints("构建非零指示约束 / Build nonzero-indicator constraints") {
        nonzeroIndicatorConstraints(
            poly = poly,
            indVar = indVar,
            sideVar = sideVar,
            bigM = bigM,
            tolerance = tolerance,
            strictBoundary = strictBoundary,
            namePrefix = namePrefix
        )
    }

/**
 * 为零值检测构建指示约束。 / Build indicator constraints for detecting a zero polynomial value.
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
    validateLegacyIndicatorInputs(
        poly = poly,
        bigM = bigM,
        tolerance = tolerance,
        strictBoundary = strictBoundary
    )
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

    validateLegacyIndicatorConstraints(constraints)
    return constraints
}

/** 安全构建旧版零值指示约束。 / Safely build legacy zero-indicator constraints. */
internal fun <V> safeZeroIndicatorConstraints(
    poly: LinearPolynomial<V>,
    indicator: AbstractVariableItem<*, *>,
    sideVar: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    strictBoundary: V,
    namePrefix: String
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> =
    checkedLegacyIndicatorConstraints("构建零值指示约束 / Build zero-indicator constraints") {
        zeroIndicatorConstraints(
            poly = poly,
            indicator = indicator,
            sideVar = sideVar,
            bigM = bigM,
            tolerance = tolerance,
            strictBoundary = strictBoundary,
            namePrefix = namePrefix
        )
    }

/**
 * 为正数检测构建指示约束。 / Build indicator constraints for detecting a positive polynomial value.
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
    validateLegacyIndicatorInputs(
        poly = poly,
        bigM = bigM,
        tolerance = tolerance
    )
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

    validateLegacyIndicatorConstraints(constraints)
    return constraints
}

/** 安全构建旧版正数指示约束。 / Safely build legacy positive-indicator constraints. */
internal fun <V> safePositiveIndicatorConstraints(
    poly: LinearPolynomial<V>,
    indicator: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    namePrefix: String
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> =
    checkedLegacyIndicatorConstraints("构建正数指示约束 / Build positive-indicator constraints") {
        positiveIndicatorConstraints(
            poly = poly,
            indicator = indicator,
            bigM = bigM,
            tolerance = tolerance,
            namePrefix = namePrefix
        )
    }

/**
 * 为非负检测构建指示约束。 / Build indicator constraints for detecting a nonnegative polynomial value.
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
    validateLegacyIndicatorInputs(
        poly = poly,
        bigM = bigM,
        tolerance = tolerance
    )
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

    validateLegacyIndicatorConstraints(constraints)
    return constraints
}

/** 安全构建旧版非负指示约束。 / Safely build legacy nonnegative-indicator constraints. */
internal fun <V> safeNonnegativeIndicatorConstraints(
    poly: LinearPolynomial<V>,
    indicator: AbstractVariableItem<*, *>,
    bigM: V,
    tolerance: V,
    namePrefix: String
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> =
    checkedLegacyIndicatorConstraints("构建非负指示约束 / Build nonnegative-indicator constraints") {
        nonnegativeIndicatorConstraints(
            poly = poly,
            indicator = indicator,
            bigM = bigM,
            tolerance = tolerance,
            namePrefix = namePrefix
        )
    }

/**
 * 为简单不等式（LE 或 GE）构建指示约束。 / Build indicator constraints for a simple inequality (LE or GE).
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
    return try {
        val zero = ineq.lhs.constant - ineq.lhs.constant
        val constraints = mutableListOf<LinearInequality<V>>()
        val diffMonos = ineq.lhs.monomials.map { LinearMonomial(it.coefficient, it.symbol) } +
            ineq.rhs.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
        val shiftedConst = ineq.lhs.constant - ineq.rhs.constant
        validateLegacyIndicatorInputs(
            poly = LinearPolynomial(diffMonos, shiftedConst),
            bigM = bigM,
            tolerance = tolerance,
            strictBoundary = strictBoundary
        )
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
                when (val result = safeZeroIndicatorConstraints(
                    poly = LinearPolynomial(diffMonos, shiftedConst),
                    indicator = indicator,
                    sideVar = eqSideVar,
                    bigM = bigM,
                    tolerance = tolerance,
                    strictBoundary = strictBoundary,
                    namePrefix = namePrefix
                )) {
                    is Ok -> constraints += result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            Comparison.LT, Comparison.GT, Comparison.NE -> {
                return Failed(ErrorCode.ApplicationError, "Indicator constraints not supported for ${ineq.comparison}")
            }
        }

        validateLegacyIndicatorConstraints(constraints)
        Ok(constraints)
    } catch (error: RuntimeException) {
        legacyIndicatorFailure("构建简单指示约束 / Build simple indicator constraints", error)
    }
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
