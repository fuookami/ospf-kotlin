/**
 * 求值操作
 * Evaluation Operations
 *
 * 提供多项式求值的便捷封装。
 * 支持完整求值和部分求值，处理缺失值的多种策略，适用于 F64 类型。
 * Provides convenient wrappers for polynomial evaluation.
 * Supports full evaluation and partial evaluation with various missing value strategies,
 * suitable for F64 type.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.algebra.number.Flt64 as F64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.Error as OspfError
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.adapter.MapValueProvider
import fuookami.ospf.kotlin.math.symbol.adapter.MissingValuePolicy
import fuookami.ospf.kotlin.math.symbol.adapter.ValueProvider
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial

// ============================================================================
// Helper Functions for Missing Value Handling
// ============================================================================

private fun missingValueFailed(symbol: Symbol): Ret<F64> {
    return Failed(ErrorCode.DataNotFound, "Missing value for symbol: ${symbol.name}")
}

private fun resolveValue(
    symbol: Symbol,
    provider: ValueProvider,
    policy: MissingValuePolicy
): F64? {
    val value = provider[symbol]
    return if (value != null) {
        value
    } else {
        when (policy) {
            MissingValuePolicy.AsZero -> F64.zero
            MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> null
        }
    }
}

private fun resolveValueRet(
    symbol: Symbol,
    provider: ValueProvider,
    policy: MissingValuePolicy
): Ret<F64> {
    val value = provider[symbol]
    return if (value != null) {
        Ok(value)
    } else {
        when (policy) {
            MissingValuePolicy.AsZero -> Ok(F64.zero)
            MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> missingValueFailed(symbol)
        }
    }
}

// ============================================================================
// Linear Monomial Evaluation (Typed, no Generic conversion)
// ============================================================================

fun LinearMonomial<F64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    val symbolValue = resolveValue(symbol, provider, policy)
    return if (symbolValue != null) {
        coefficient * symbolValue
    } else {
        null
    }
}

fun LinearMonomial<F64>.evaluate(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    return evaluate(MapValueProvider(values), policy)
}

fun LinearMonomial<F64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    return when (val result = resolveValueRet(symbol, provider, policy)) {
        is Ok -> Ok(coefficient * result.value)
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

fun LinearMonomial<F64>.evaluateRet(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun LinearMonomial<F64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<F64>
): F64 {
    require(order.toSet().size == order.size) {
        "Symbol order contains duplicated symbols."
    }
    require(order.size == values.size) {
        "Order and values size mismatch: order.size=${order.size}, values.size=${values.size}."
    }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    val index = indexOfSymbol[symbol]
        ?: throw IllegalArgumentException("Symbol ${symbol.name} not found in order.")
    return coefficient * values[index]
}

fun LinearMonomial<F64>.partialEvaluate(provider: ValueProvider): LinearPolynomial<F64> {
    val symbolValue = provider[symbol]
    return if (symbolValue != null) {
        LinearPolynomial(
            monomials = emptyList(),
            constant = coefficient * symbolValue
        )
    } else {
        LinearPolynomial(
            monomials = listOf(this),
            constant = F64.zero
        )
    }
}

fun LinearMonomial<F64>.partialEvaluate(values: Map<Symbol, F64>): LinearPolynomial<F64> {
    return partialEvaluate(MapValueProvider(values))
}

// ============================================================================
// Quadratic Monomial Evaluation (Typed, no Generic conversion)
// ============================================================================

fun QuadraticMonomial<F64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    val v1 = resolveValue(symbol1, provider, policy)
    if (v1 == null) return null

    if (symbol2 == null) {
        return coefficient * v1
    }

    val v2 = resolveValue(symbol2, provider, policy)
    return if (v2 != null) {
        coefficient * v1 * v2
    } else {
        null
    }
}

fun QuadraticMonomial<F64>.evaluate(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    return evaluate(MapValueProvider(values), policy)
}

fun QuadraticMonomial<F64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    val v1Result = resolveValueRet(symbol1, provider, policy)
    when (v1Result) {
        is Failed -> return Failed(v1Result.error)
        is Fatal -> return Fatal(v1Result.errors)
        is Ok -> {}
    }

    val v1 = v1Result.value!!

    if (symbol2 == null) {
        return Ok(coefficient * v1)
    }

    val v2Result = resolveValueRet(symbol2, provider, policy)
    return when (v2Result) {
        is Ok -> Ok(coefficient * v1 * v2Result.value)
        is Failed -> Failed(v2Result.error)
        is Fatal -> Fatal(v2Result.errors)
    }
}

fun QuadraticMonomial<F64>.evaluateRet(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun QuadraticMonomial<F64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<F64>
): F64 {
    require(order.toSet().size == order.size) {
        "Symbol order contains duplicated symbols."
    }
    require(order.size == values.size) {
        "Order and values size mismatch: order.size=${order.size}, values.size=${values.size}."
    }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    val i1 = indexOfSymbol[symbol1]
        ?: throw IllegalArgumentException("Symbol ${symbol1.name} not found in order.")
    var result = coefficient * values[i1]
    if (symbol2 != null) {
        val i2 = indexOfSymbol[symbol2]
            ?: throw IllegalArgumentException("Symbol ${symbol2.name} not found in order.")
        result *= values[i2]
    }
    return result
}

fun QuadraticMonomial<F64>.partialEvaluate(provider: ValueProvider): QuadraticPolynomial<F64> {
    val v1 = provider[symbol1]
    val v2 = if (symbol2 != null) provider[symbol2] else null

    return when {
        v1 != null && v2 != null -> {
            // Both symbols have values, becomes constant
            QuadraticPolynomial(
                monomials = emptyList(),
                constant = coefficient * v1 * v2
            )
        }
        v1 != null && symbol2 == null -> {
            // Only symbol1 has value and no symbol2, becomes constant
            QuadraticPolynomial(
                monomials = emptyList(),
                constant = coefficient * v1
            )
        }
        v1 != null -> {
            // Only symbol1 has value, symbol2 remains
            QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(
                    coefficient = coefficient * v1,
                    symbol1 = symbol2!!,
                    symbol2 = null
                )),
                constant = F64.zero
            )
        }
        v2 != null -> {
            // Only symbol2 has value, symbol1 remains
            QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial(
                    coefficient = coefficient * v2,
                    symbol1 = symbol1,
                    symbol2 = null
                )),
                constant = F64.zero
            )
        }
        else -> {
            // Neither symbol has value, keep as-is
            QuadraticPolynomial(
                monomials = listOf(this),
                constant = F64.zero
            )
        }
    }
}

fun QuadraticMonomial<F64>.partialEvaluate(values: Map<Symbol, F64>): QuadraticPolynomial<F64> {
    return partialEvaluate(MapValueProvider(values))
}

// ============================================================================
// Canonical Monomial Evaluation (Typed, no Generic conversion)
// ============================================================================

fun CanonicalMonomial<F64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    var result = coefficient
    for ((symbol, power) in powers) {
        val symbolValue = resolveValue(symbol, provider, policy)
        if (symbolValue == null) return null
        result *= computeRingPower(symbolValue, power.toInt(), F64.one)
    }
    return result
}

fun CanonicalMonomial<F64>.evaluate(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    return evaluate(MapValueProvider(values), policy)
}

fun CanonicalMonomial<F64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    var result = coefficient
    for ((symbol, power) in powers) {
        val symbolValueResult = resolveValueRet(symbol, provider, policy)
        when (symbolValueResult) {
            is Failed -> return Failed(symbolValueResult.error)
            is Fatal -> return Fatal(symbolValueResult.errors)
            is Ok -> result *= computeRingPower(symbolValueResult.value, power.toInt(), F64.one)
        }
    }
    return Ok(result)
}

fun CanonicalMonomial<F64>.evaluateRet(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun CanonicalMonomial<F64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<F64>
): F64 {
    require(order.toSet().size == order.size) {
        "Symbol order contains duplicated symbols."
    }
    require(order.size == values.size) {
        "Order and values size mismatch: order.size=${order.size}, values.size=${values.size}."
    }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    var result = coefficient
    for ((symbol, power) in powers) {
        val index = indexOfSymbol[symbol]
            ?: throw IllegalArgumentException("Symbol ${symbol.name} not found in order.")
        result *= computeRingPower(values[index], power.toInt(), F64.one)
    }
    return result
}

fun CanonicalMonomial<F64>.partialEvaluate(provider: ValueProvider): CanonicalMonomial<F64> {
    var newCoefficient = coefficient
    val remainedPowers = LinkedHashMap<Symbol, Int32>()
    for ((symbol, power) in powers) {
        val symbolValue = provider[symbol]
        if (symbolValue != null) {
            newCoefficient *= computeRingPower(symbolValue, power.toInt(), F64.one)
        } else {
            remainedPowers[symbol] = power
        }
    }
    return CanonicalMonomial(
        coefficient = newCoefficient,
        powers = remainedPowers
    )
}

fun CanonicalMonomial<F64>.partialEvaluate(values: Map<Symbol, F64>): CanonicalMonomial<F64> {
    return partialEvaluate(MapValueProvider(values))
}

// ============================================================================
// Linear Polynomial Evaluation (Typed, using evaluateLinear/partialEvaluateLinear)
// ============================================================================

fun LinearPolynomial<F64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    return evaluateLinear(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(symbol, provider, policy)
        }
    )
}

fun LinearPolynomial<F64>.evaluate(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    return when (policy) {
        MissingValuePolicy.AsZero -> evaluateLinear(values) { F64.zero }
        MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> evaluateLinear(values)
    }
}

fun LinearPolynomial<F64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    var failure: Ret<F64>? = null
    val value = evaluateLinear(
        values = emptyMap(),
        onMissing = { symbol ->
            when (val result = resolveValueRet(symbol, provider, policy)) {
                is Ok -> result.value
                is Failed -> {
                    if (failure == null) {
                        failure = result
                    }
                    null
                }
                is Fatal -> {
                    failure = result
                    null
                }
            }
        }
    )
    return when {
        value != null -> Ok(value)
        failure != null -> failure!!
        else -> Failed(ErrorCode.DataNotFound, "Missing value for one or more symbols.")
    }
}

fun LinearPolynomial<F64>.evaluateRet(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun LinearPolynomial<F64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<F64>
): F64 {
    return evaluateLinearOrdered(order, values)
}

fun LinearPolynomial<F64>.partialEvaluate(provider: ValueProvider): LinearPolynomial<F64> {
    val values = LinkedHashMap<Symbol, F64>()
    for (monomial in monomials) {
        provider[monomial.symbol]?.let { values[monomial.symbol] = it }
    }
    return partialEvaluateLinear(
        values = values,
        zero = F64.zero,
        isZero = { it == F64.zero }
    )
}

fun LinearPolynomial<F64>.partialEvaluate(values: Map<Symbol, F64>): LinearPolynomial<F64> {
    return partialEvaluateLinear(
        values = values,
        zero = F64.zero,
        isZero = { it == F64.zero }
    )
}

/**
 * 计算线性多项式在给定区间内的极值范围
 * Compute the extremum range of a linear polynomial within given intervals
 *
 * 根据每个符号的取值区间，计算线性多项式可能达到的最小值和最大值。
 * 对于正系数项，下界取符号下界、上界取符号上界；对于负系数项则相反。
 * 如果任何符号缺少区间信息，返回 null。
 *
 * Computes the minimum and maximum values the linear polynomial can reach,
 * given the value range for each symbol. For positive coefficients, the lower
 * bound uses the symbol's lower bound and the upper bound uses the symbol's
 * upper bound; for negative coefficients, the opposite applies.
 * Returns null if interval information is missing for any symbol.
 *
 * @param intervals 每个符号的取值区间映射 / Map of symbols to their value ranges
 * @return 极值范围（闭区间），或 null（区间信息不完整时）
 *         Extremum range (closed interval), or null if interval information is incomplete
 */
fun LinearPolynomial<F64>.evaluateIntervalExtremum(
    intervals: Map<Symbol, ValueRange<F64>>
): ValueRange<F64>? {
    var lowerBound = constant
    var upperBound = constant
    for (monomial in monomials) {
        val interval = intervals[monomial.symbol] ?: return null
        val lb = interval.lowerBound.value.unwrapOrNull() ?: return null
        val ub = interval.upperBound.value.unwrapOrNull() ?: return null
        when {
            monomial.coefficient > F64.zero -> {
                lowerBound += monomial.coefficient * lb
                upperBound += monomial.coefficient * ub
            }

            monomial.coefficient < F64.zero -> {
                lowerBound += monomial.coefficient * ub
                upperBound += monomial.coefficient * lb
            }

            else -> {}
        }
    }
    return when (
        val range = ValueRange(
            lb = lowerBound,
            ub = upperBound,
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = F64
        )
    ) {
        is Ok -> range.value
        is Failed -> null
        is Fatal -> null
    }
}

// ============================================================================
// Quadratic Polynomial Evaluation (Typed, using evaluateQuadratic/partialEvaluateQuadratic)
// ============================================================================

fun QuadraticPolynomial<F64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    return evaluateQuadratic(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(symbol, provider, policy)
        }
    )
}

fun QuadraticPolynomial<F64>.evaluate(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    return when (policy) {
        MissingValuePolicy.AsZero -> evaluateQuadratic(values) { F64.zero }
        MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> evaluateQuadratic(values)
    }
}

fun QuadraticPolynomial<F64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    var failure: Ret<F64>? = null
    val value = evaluateQuadratic(
        values = emptyMap(),
        onMissing = { symbol ->
            when (val result = resolveValueRet(symbol, provider, policy)) {
                is Ok -> result.value
                is Failed -> {
                    if (failure == null) {
                        failure = result
                    }
                    null
                }
                is Fatal -> {
                    failure = result
                    null
                }
            }
        }
    )
    return when {
        value != null -> Ok(value)
        failure != null -> failure!!
        else -> Failed(ErrorCode.DataNotFound, "Missing value for one or more symbols.")
    }
}

fun QuadraticPolynomial<F64>.evaluateRet(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun QuadraticPolynomial<F64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<F64>
): F64 {
    return evaluateQuadraticOrdered(order, values)
}

fun QuadraticPolynomial<F64>.partialEvaluate(provider: ValueProvider): QuadraticPolynomial<F64> {
    val values = LinkedHashMap<Symbol, F64>()
    for (monomial in monomials) {
        provider[monomial.symbol1]?.let { values[monomial.symbol1] = it }
        monomial.symbol2?.let { symbol ->
            provider[symbol]?.let { values[symbol] = it }
        }
    }
    return partialEvaluateQuadratic(
        values = values,
        zero = F64.zero,
        isZero = { it == F64.zero }
    )
}

fun QuadraticPolynomial<F64>.partialEvaluate(values: Map<Symbol, F64>): QuadraticPolynomial<F64> {
    return partialEvaluateQuadratic(
        values = values,
        zero = F64.zero,
        isZero = { it == F64.zero }
    )
}

// ============================================================================
// Canonical Polynomial Evaluation (Typed, using evaluateCanonical/partialEvaluateCanonical)
// ============================================================================

fun CanonicalPolynomial<F64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    return evaluateCanonical(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(symbol, provider, policy)
        },
        one = F64.one
    )
}

fun CanonicalPolynomial<F64>.evaluate(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): F64? {
    return when (policy) {
        MissingValuePolicy.AsZero -> evaluateCanonical(
            values = values,
            onMissing = { F64.zero },
            one = F64.one
        )
        MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> evaluateCanonical(values, one = F64.one)
    }
}

fun CanonicalPolynomial<F64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    var failure: Ret<F64>? = null
    val value = evaluateCanonical(
        values = emptyMap(),
        onMissing = { symbol ->
            when (val result = resolveValueRet(symbol, provider, policy)) {
                is Ok -> result.value
                is Failed -> {
                    if (failure == null) {
                        failure = result
                    }
                    null
                }
                is Fatal -> {
                    failure = result
                    null
                }
            }
        },
        one = F64.one
    )
    return when {
        value != null -> Ok(value)
        failure != null -> failure!!
        else -> Failed(ErrorCode.DataNotFound, "Missing value for one or more symbols.")
    }
}

fun CanonicalPolynomial<F64>.evaluateRet(
    values: Map<Symbol, F64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<F64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun CanonicalPolynomial<F64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<F64>
): F64 {
    return evaluateCanonicalOrdered(order, values, F64.one)
}

fun CanonicalPolynomial<F64>.partialEvaluate(
    provider: ValueProvider,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial<F64> {
    val values = LinkedHashMap<Symbol, F64>()
    for (monomial in monomials) {
        for (symbol in monomial.powers.keys) {
            provider[symbol]?.let { values[symbol] = it }
        }
    }
    return partialEvaluateCanonical(
        values = values,
        zero = F64.zero,
        isZero = { it == F64.zero },
        one = F64.one,
        symbolComparator = symbolComparator
    )
}

fun CanonicalPolynomial<F64>.partialEvaluate(
    values: Map<Symbol, F64>,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial<F64> {
    return partialEvaluateCanonical(
        values = values,
        zero = F64.zero,
        isZero = { it == F64.zero },
        one = F64.one,
        symbolComparator = symbolComparator
    )
}
