package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.adapter.MapValueProvider
import fuookami.ospf.kotlin.utils.math.symbol.adapter.MissingValuePolicy
import fuookami.ospf.kotlin.utils.math.symbol.adapter.ValueProvider
import fuookami.ospf.kotlin.utils.math.symbol.generic.evaluate as evaluateGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.evaluateOrdered as evaluateOrderedGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.partialEvaluate as partialEvaluateGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toCanonicalPolynomial as toCanonicalPolynomialFromGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toLinearPolynomial as toLinearPolynomialFromGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toQuadraticPolynomial as toQuadraticPolynomialFromGeneric
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

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

private fun buildOrderedIndex(
    order: List<Symbol>,
    values: List<Flt64>
): Map<Symbol, Int> {
    require(order.toSet().size == order.size) {
        "Symbol order contains duplicated symbols."
    }
    require(order.size == values.size) {
        "Order and values size mismatch: order.size=${order.size}, values.size=${values.size}."
    }
    return order.withIndex().associate { it.value to it.index }
}

private fun orderedValueOf(
    symbol: Symbol,
    indexOfSymbol: Map<Symbol, Int>,
    values: List<Flt64>
): Flt64 {
    val index = indexOfSymbol[symbol]
        ?: throw IllegalArgumentException("Symbol ${symbol.name} not found in order.")
    return values[index]
}

fun LinearMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    val value = resolveValue(symbol, provider, policy) ?: return null
    return coefficient * value
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
    return when (val valueResult = resolveValueRet(symbol, provider, policy)) {
        is Ok -> Ok(coefficient * valueResult.value)
        is Failed -> Failed(valueResult.error)
        is Fatal -> Fatal(valueResult.errors)
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
    val indexOfSymbol = buildOrderedIndex(order, values)
    return coefficient * orderedValueOf(symbol, indexOfSymbol, values)
}

fun LinearMonomial<Flt64>.partialEvaluate(provider: ValueProvider): LinearPolynomial<Flt64> {
    val value = provider[symbol]
    return if (value == null) {
        LinearPolynomial<Flt64>(monomials = listOf(this))
    } else {
        LinearPolynomial<Flt64>(constant = coefficient * value)
    }
}

fun LinearMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): LinearPolynomial<Flt64> {
    return partialEvaluate(MapValueProvider(values))
}

fun QuadraticMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    val value1 = resolveValue(symbol1, provider, policy) ?: return null
    return if (symbol2 == null) {
        coefficient * value1
    } else {
        val value2 = resolveValue(symbol2, provider, policy) ?: return null
        coefficient * value1 * value2
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
    val value1 = when (val result = resolveValueRet(symbol1, provider, policy)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return if (symbol2 == null) {
        Ok(coefficient * value1)
    } else {
        val value2 = when (val result = resolveValueRet(symbol2, provider, policy)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        Ok(coefficient * value1 * value2)
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
    val indexOfSymbol = buildOrderedIndex(order, values)
    val value1 = orderedValueOf(symbol1, indexOfSymbol, values)
    return if (symbol2 == null) {
        coefficient * value1
    } else {
        coefficient * value1 * orderedValueOf(symbol2, indexOfSymbol, values)
    }
}

fun QuadraticMonomial<Flt64>.partialEvaluate(provider: ValueProvider): QuadraticPolynomial<Flt64> {
    val value1 = provider[symbol1]
    if (symbol2 == null) {
        return if (value1 == null) {
            QuadraticPolynomial<Flt64>(monomials = listOf(this))
        } else {
            QuadraticPolynomial<Flt64>(constant = coefficient * value1)
        }
    }

    val value2 = provider[symbol2]
    return when {
        value1 != null && value2 != null -> {
            QuadraticPolynomial<Flt64>(constant = coefficient * value1 * value2)
        }

        value1 != null -> {
            QuadraticPolynomial<Flt64>(
                monomials = listOf(
                    QuadraticMonomial<Flt64>(
                        coefficient = coefficient * value1,
                        symbol1 = symbol2,
                        symbol2 = null
                    )
                )
            )
        }

        value2 != null -> {
            QuadraticPolynomial<Flt64>(
                monomials = listOf(
                    QuadraticMonomial<Flt64>(
                        coefficient = coefficient * value2,
                        symbol1 = symbol1,
                        symbol2 = null
                    )
                )
            )
        }

        else -> {
            QuadraticPolynomial<Flt64>(monomials = listOf(this))
        }
    }
}

fun QuadraticMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): QuadraticPolynomial<Flt64> {
    return partialEvaluate(MapValueProvider(values))
}

fun CanonicalMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    var value = coefficient
    for (symbol in factors) {
        val factor = resolveValue(symbol, provider, policy) ?: return null
        value = value * factor
    }
    return value
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
    var value = coefficient
    for (symbol in factors) {
        val factor = when (val result = resolveValueRet(symbol, provider, policy)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        value = value * factor
    }
    return Ok(value)
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
    val indexOfSymbol = buildOrderedIndex(order, values)
    var value = coefficient
    for (symbol in factors) {
        value *= orderedValueOf(symbol, indexOfSymbol, values)
    }
    return value
}

fun CanonicalMonomial<Flt64>.partialEvaluate(provider: ValueProvider): CanonicalMonomial<Flt64> {
    var newCoefficient = coefficient
    val remainedFactors = ArrayList<Symbol>(factors.size)
    for (symbol in factors) {
        val value = provider[symbol]
        if (value == null) {
            remainedFactors.add(symbol)
        } else {
            newCoefficient *= value
        }
    }
    return CanonicalMonomial<Flt64>(
        coefficient = newCoefficient,
        factors = remainedFactors
    )
}

fun CanonicalMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): CanonicalMonomial<Flt64> {
    return partialEvaluate(MapValueProvider(values))
}

fun LinearPolynomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return toGenericLinearPolynomial().evaluateGeneric(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(
                symbol = symbol,
                provider = provider,
                policy = policy
            )
        }
    )
}

fun LinearPolynomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return when (policy) {
        MissingValuePolicy.AsZero -> toGenericLinearPolynomial().evaluateGeneric(values) { Flt64.zero }
        MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> toGenericLinearPolynomial().evaluateGeneric(values)
    }
}

fun LinearPolynomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var failed: Failed<Flt64, Error>? = null
    var fatal: Fatal<Flt64, Error>? = null
    val value = toGenericLinearPolynomial().evaluateGeneric(
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
    return toGenericLinearPolynomial().evaluateOrderedGeneric(order, values)
}

fun LinearPolynomial<Flt64>.partialEvaluate(provider: ValueProvider): LinearPolynomial<Flt64> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (monomial in monomials) {
        provider[monomial.symbol]?.let { values[monomial.symbol] = it }
    }
    return toGenericLinearPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
        .toLinearPolynomialFromGeneric()
}

fun LinearPolynomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): LinearPolynomial<Flt64> {
    return toGenericLinearPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
        .toLinearPolynomialFromGeneric()
}

fun QuadraticPolynomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return toGenericQuadraticPolynomial().evaluateGeneric(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(
                symbol = symbol,
                provider = provider,
                policy = policy
            )
        }
    )
}

fun QuadraticPolynomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return when (policy) {
        MissingValuePolicy.AsZero -> toGenericQuadraticPolynomial().evaluateGeneric(values) { Flt64.zero }
        MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> toGenericQuadraticPolynomial().evaluateGeneric(values)
    }
}

fun QuadraticPolynomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var failed: Failed<Flt64, Error>? = null
    var fatal: Fatal<Flt64, Error>? = null
    val value = toGenericQuadraticPolynomial().evaluateGeneric(
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
    return toGenericQuadraticPolynomial().evaluateOrderedGeneric(order, values)
}

fun QuadraticPolynomial<Flt64>.partialEvaluate(provider: ValueProvider): QuadraticPolynomial<Flt64> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (monomial in monomials) {
        provider[monomial.symbol1]?.let { values[monomial.symbol1] = it }
        monomial.symbol2?.let { symbol ->
            provider[symbol]?.let { values[symbol] = it }
        }
    }
    return toGenericQuadraticPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
        .toQuadraticPolynomialFromGeneric()
}

fun QuadraticPolynomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): QuadraticPolynomial<Flt64> {
    return toGenericQuadraticPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
        .toQuadraticPolynomialFromGeneric()
}

fun CanonicalPolynomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return toGenericCanonicalPolynomial().evaluateGeneric(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(
                symbol = symbol,
                provider = provider,
                policy = policy
            )
        }
    )
}

fun CanonicalPolynomial<Flt64>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return when (policy) {
        MissingValuePolicy.AsZero -> toGenericCanonicalPolynomial().evaluateGeneric(values) { Flt64.zero }
        MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> toGenericCanonicalPolynomial().evaluateGeneric(values)
    }
}

fun CanonicalPolynomial<Flt64>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var failed: Failed<Flt64, Error>? = null
    var fatal: Fatal<Flt64, Error>? = null
    val value = toGenericCanonicalPolynomial().evaluateGeneric(
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
    return toGenericCanonicalPolynomial().evaluateOrderedGeneric(order, values)
}

fun CanonicalPolynomial<Flt64>.partialEvaluate(
    provider: ValueProvider,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (monomial in monomials) {
        for (symbol in monomial.factors) {
            provider[symbol]?.let { values[symbol] = it }
        }
    }
    return toGenericCanonicalPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            symbolComparator = symbolComparator
        )
        .toCanonicalPolynomialFromGeneric()
}

fun CanonicalPolynomial<Flt64>.partialEvaluate(
    values: Map<Symbol, Flt64>,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64> {
    return toGenericCanonicalPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            symbolComparator = symbolComparator
        )
        .toCanonicalPolynomialFromGeneric()
}
