package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*

import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.adapter.MapValueProvider
import fuookami.ospf.kotlin.utils.math.symbol.adapter.MissingValuePolicy
import fuookami.ospf.kotlin.utils.math.symbol.adapter.ValueProvider
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

// ============================================================================
// Helper Functions for Missing Value Handling
// ============================================================================

private fun missingValueFailed(symbol: Symbol): Failed<Flt64, Error> {
    return Failed(ErrorCode.DataNotFound, "Missing value for symbol: ${symbol.name}")
}

private fun resolveValue(
    symbol: Symbol,
    provider: ValueProvider,
    policy: MissingValuePolicy
): Flt64? {
    val value = provider[symbol]
    return if (value != null) {
        value
    } else {
        when (policy) {
            MissingValuePolicy.AsZero -> Flt64.zero
            MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> null
        }
    }
}

private fun resolveValueRet(
    symbol: Symbol,
    provider: ValueProvider,
    policy: MissingValuePolicy
): Ret<Flt64> {
    val value = provider[symbol]
    return if (value != null) {
        Ok(value)
    } else {
        when (policy) {
            MissingValuePolicy.AsZero -> Ok(Flt64.zero)
            MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> missingValueFailed(symbol)
        }
    }
}

// ============================================================================
// Linear Monomial Evaluation (Typed, no Generic conversion)
// ============================================================================

fun LinearMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    val symbolValue = resolveValue(symbol, provider, policy)
    return if (symbolValue != null) {
        coefficient * symbolValue
    } else {
        null
    }
}

fun LinearMonomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

fun LinearMonomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return when (val result = resolveValueRet(symbol, provider, policy)) {
        is Ok -> Ok(coefficient * result.value)
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
    }
}

fun LinearMonomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun LinearMonomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
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

fun LinearMonomial<Flt64>.partialEvaluate(provider: ValueProvider): LinearPolynomial<Flt64> {
    val symbolValue = provider[symbol]
    return if (symbolValue != null) {
        LinearPolynomial(
            monomials = emptyList(),
            constant = coefficient * symbolValue
        )
    } else {
        LinearPolynomial(
            monomials = listOf(this),
            constant = Flt64.zero
        )
    }
}

fun LinearMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): LinearPolynomial<Flt64> {
    return partialEvaluate(MapValueProvider(values))
}

// ============================================================================
// Quadratic Monomial Evaluation (Typed, no Generic conversion)
// ============================================================================

fun QuadraticMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
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

fun QuadraticMonomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

fun QuadraticMonomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
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

fun QuadraticMonomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun QuadraticMonomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
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

fun QuadraticMonomial<Flt64>.partialEvaluate(provider: ValueProvider): QuadraticPolynomial<Flt64> {
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
                constant = Flt64.zero
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
                constant = Flt64.zero
            )
        }
        else -> {
            // Neither symbol has value, keep as-is
            QuadraticPolynomial(
                monomials = listOf(this),
                constant = Flt64.zero
            )
        }
    }
}

fun QuadraticMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): QuadraticPolynomial<Flt64> {
    return partialEvaluate(MapValueProvider(values))
}

// ============================================================================
// Canonical Monomial Evaluation (Typed, no Generic conversion)
// ============================================================================

fun CanonicalMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    var result = coefficient
    for ((symbol, power) in powers) {
        val symbolValue = resolveValue(symbol, provider, policy)
        if (symbolValue == null) return null
        result *= computeRingPower(symbolValue, power.toInt(), Flt64.one)
    }
    return result
}

fun CanonicalMonomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

fun CanonicalMonomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var result = coefficient
    for ((symbol, power) in powers) {
        val symbolValueResult = resolveValueRet(symbol, provider, policy)
        when (symbolValueResult) {
            is Failed -> return Failed(symbolValueResult.error)
            is Fatal -> return Fatal(symbolValueResult.errors)
            is Ok -> result *= computeRingPower(symbolValueResult.value, power.toInt(), Flt64.one)
        }
    }
    return Ok(result)
}

fun CanonicalMonomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun CanonicalMonomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
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
        result *= computeRingPower(values[index], power.toInt(), Flt64.one)
    }
    return result
}

fun CanonicalMonomial<Flt64>.partialEvaluate(provider: ValueProvider): CanonicalMonomial<Flt64> {
    var newCoefficient = coefficient
    val remainedPowers = LinkedHashMap<Symbol, Int32>()
    for ((symbol, power) in powers) {
        val symbolValue = provider[symbol]
        if (symbolValue != null) {
            newCoefficient *= computeRingPower(symbolValue, power.toInt(), Flt64.one)
        } else {
            remainedPowers[symbol] = power
        }
    }
    return CanonicalMonomial(
        coefficient = newCoefficient,
        powers = remainedPowers
    )
}

fun CanonicalMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): CanonicalMonomial<Flt64> {
    return partialEvaluate(MapValueProvider(values))
}

// ============================================================================
// Linear Polynomial Evaluation (Typed, using evaluateLinear/partialEvaluateLinear)
// ============================================================================

fun LinearPolynomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluateLinear(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(symbol, provider, policy)
        }
    )
}

fun LinearPolynomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return when (policy) {
        MissingValuePolicy.AsZero -> evaluateLinear(values) { Flt64.zero }
        MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> evaluateLinear(values)
    }
}

fun LinearPolynomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var failed: Failed<Flt64, Error>? = null
    var fatal: Fatal<Flt64, Error>? = null
    val value = evaluateLinear(
        values = emptyMap(),
        onMissing = { symbol ->
            when (val result = resolveValueRet(symbol, provider, policy)) {
                is Ok -> result.value
                is Failed -> {
                    failed = result
                    null
                }
                is Fatal -> {
                    fatal = result
                    null
                }
            }
        }
    )
    return when {
        value != null -> Ok(value)
        fatal != null -> Fatal(fatal!!.errors)
        failed != null -> Failed(failed!!.error)
        else -> Failed(ErrorCode.DataNotFound, "Missing value for one or more symbols.")
    }
}

fun LinearPolynomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun LinearPolynomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
    return evaluateLinearOrdered(order, values)
}

fun LinearPolynomial<Flt64>.partialEvaluate(provider: ValueProvider): LinearPolynomial<Flt64> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (monomial in monomials) {
        provider[monomial.symbol]?.let { values[monomial.symbol] = it }
    }
    return partialEvaluateLinear(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun LinearPolynomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): LinearPolynomial<Flt64> {
    return partialEvaluateLinear(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

// ============================================================================
// Quadratic Polynomial Evaluation (Typed, using evaluateQuadratic/partialEvaluateQuadratic)
// ============================================================================

fun QuadraticPolynomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluateQuadratic(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(symbol, provider, policy)
        }
    )
}

fun QuadraticPolynomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return when (policy) {
        MissingValuePolicy.AsZero -> evaluateQuadratic(values) { Flt64.zero }
        MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> evaluateQuadratic(values)
    }
}

fun QuadraticPolynomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var failed: Failed<Flt64, Error>? = null
    var fatal: Fatal<Flt64, Error>? = null
    val value = evaluateQuadratic(
        values = emptyMap(),
        onMissing = { symbol ->
            when (val result = resolveValueRet(symbol, provider, policy)) {
                is Ok -> result.value
                is Failed -> {
                    failed = result
                    null
                }
                is Fatal -> {
                    fatal = result
                    null
                }
            }
        }
    )
    return when {
        value != null -> Ok(value)
        fatal != null -> Fatal(fatal!!.errors)
        failed != null -> Failed(failed!!.error)
        else -> Failed(ErrorCode.DataNotFound, "Missing value for one or more symbols.")
    }
}

fun QuadraticPolynomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun QuadraticPolynomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
    return evaluateQuadraticOrdered(order, values)
}

fun QuadraticPolynomial<Flt64>.partialEvaluate(provider: ValueProvider): QuadraticPolynomial<Flt64> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (monomial in monomials) {
        provider[monomial.symbol1]?.let { values[monomial.symbol1] = it }
        monomial.symbol2?.let { symbol ->
            provider[symbol]?.let { values[symbol] = it }
        }
    }
    return partialEvaluateQuadratic(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun QuadraticPolynomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): QuadraticPolynomial<Flt64> {
    return partialEvaluateQuadratic(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

// ============================================================================
// Canonical Polynomial Evaluation (Typed, using evaluateCanonical/partialEvaluateCanonical)
// ============================================================================

fun CanonicalPolynomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluateCanonical(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(symbol, provider, policy)
        },
        one = Flt64.one
    )
}

fun CanonicalPolynomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return when (policy) {
        MissingValuePolicy.AsZero -> evaluateCanonical(
            values = values,
            onMissing = { Flt64.zero },
            one = Flt64.one
        )
        MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> evaluateCanonical(values, one = Flt64.one)
    }
}

fun CanonicalPolynomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var failed: Failed<Flt64, Error>? = null
    var fatal: Fatal<Flt64, Error>? = null
    val value = evaluateCanonical(
        values = emptyMap(),
        onMissing = { symbol ->
            when (val result = resolveValueRet(symbol, provider, policy)) {
                is Ok -> result.value
                is Failed -> {
                    failed = result
                    null
                }
                is Fatal -> {
                    fatal = result
                    null
                }
            }
        },
        one = Flt64.one
    )
    return when {
        value != null -> Ok(value)
        fatal != null -> Fatal(fatal!!.errors)
        failed != null -> Failed(failed!!.error)
        else -> Failed(ErrorCode.DataNotFound, "Missing value for one or more symbols.")
    }
}

fun CanonicalPolynomial<Flt64>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun CanonicalPolynomial<Flt64>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
    return evaluateCanonicalOrdered(order, values, Flt64.one)
}

fun CanonicalPolynomial<Flt64>.partialEvaluate(
    provider: ValueProvider,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (monomial in monomials) {
        for (symbol in monomial.powers.keys) {
            provider[symbol]?.let { values[symbol] = it }
        }
    }
    return partialEvaluateCanonical(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        one = Flt64.one,
        symbolComparator = symbolComparator
    )
}

fun CanonicalPolynomial<Flt64>.partialEvaluate(
    values: Map<Symbol, Flt64>,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64> {
    return partialEvaluateCanonical(
        values = values,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        one = Flt64.one,
        symbolComparator = symbolComparator
    )
}