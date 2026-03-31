package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.adapter.MapValueProvider
import fuookami.ospf.kotlin.utils.math.symbol.adapter.MissingValuePolicy
import fuookami.ospf.kotlin.utils.math.symbol.adapter.ValueProvider
import fuookami.ospf.kotlin.utils.math.symbol.generic.GenericCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.GenericLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.GenericQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.evaluate as evaluateGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.evaluateOrdered as evaluateOrderedGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.partialEvaluate as partialEvaluateGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toCanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toCanonicalPolynomial as toCanonicalPolynomialFromGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticMonomial
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

private fun LinearMonomial<Flt64>.toSingleGenericLinearPolynomial(): GenericLinearPolynomial<Flt64> {
    return GenericLinearPolynomial(
        monomials = listOf(toGenericLinearMonomial()),
        constant = Flt64.zero
    )
}

private fun QuadraticMonomial<Flt64>.toSingleGenericQuadraticPolynomial(): GenericQuadraticPolynomial<Flt64> {
    return GenericQuadraticPolynomial(
        monomials = listOf(toGenericQuadraticMonomial()),
        constant = Flt64.zero
    )
}

private fun CanonicalMonomial<Flt64, Int32>.toSingleGenericCanonicalPolynomial(): GenericCanonicalPolynomial<Flt64> {
    return GenericCanonicalPolynomial(
        monomials = listOf(toGenericCanonicalMonomial()),
        constant = Flt64.zero
    )
}

fun LinearMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return toSingleGenericLinearPolynomial().evaluateGeneric(
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
    var failed: Failed<Flt64, Error>? = null
    var fatal: Fatal<Flt64, Error>? = null
    val value = toSingleGenericLinearPolynomial().evaluateGeneric(
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
    return toSingleGenericLinearPolynomial().evaluateOrderedGeneric(order, values)
}

fun LinearMonomial<Flt64>.partialEvaluate(provider: ValueProvider): LinearPolynomial<Flt64> {
    val values = LinkedHashMap<Symbol, Flt64>()
    provider[symbol]?.let { values[symbol] = it }
    return toSingleGenericLinearPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
        .toLinearPolynomialFromGeneric()
}

fun LinearMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): LinearPolynomial<Flt64> {
    return partialEvaluate(MapValueProvider(values))
}

fun QuadraticMonomial<Flt64>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return toSingleGenericQuadraticPolynomial().evaluateGeneric(
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
    var failed: Failed<Flt64, Error>? = null
    var fatal: Fatal<Flt64, Error>? = null
    val value = toSingleGenericQuadraticPolynomial().evaluateGeneric(
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
    return toSingleGenericQuadraticPolynomial().evaluateOrderedGeneric(order, values)
}

fun QuadraticMonomial<Flt64>.partialEvaluate(provider: ValueProvider): QuadraticPolynomial<Flt64> {
    val values = LinkedHashMap<Symbol, Flt64>()
    provider[symbol1]?.let { values[symbol1] = it }
    symbol2?.let { symbol ->
        provider[symbol]?.let { values[symbol] = it }
    }
    return toSingleGenericQuadraticPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
        .toQuadraticPolynomialFromGeneric()
}

fun QuadraticMonomial<Flt64>.partialEvaluate(values: Map<Symbol, Flt64>): QuadraticPolynomial<Flt64> {
    return partialEvaluate(MapValueProvider(values))
}

fun CanonicalMonomial<Flt64, Int32>.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return toSingleGenericCanonicalPolynomial().evaluateGeneric(
        values = emptyMap(),
        onMissing = { symbol ->
            resolveValue(
                symbol = symbol,
                provider = provider,
                policy = policy
            )
        },
        one = Flt64.one
    )
}

fun CanonicalMonomial<Flt64, Int32>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

fun CanonicalMonomial<Flt64, Int32>.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var failed: Failed<Flt64, Error>? = null
    var fatal: Fatal<Flt64, Error>? = null
    val value = toSingleGenericCanonicalPolynomial().evaluateGeneric(
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

fun CanonicalMonomial<Flt64, Int32>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun CanonicalMonomial<Flt64, Int32>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
    return toSingleGenericCanonicalPolynomial().evaluateOrderedGeneric(order, values, Flt64.one)
}

fun CanonicalMonomial<Flt64, Int32>.partialEvaluate(provider: ValueProvider): CanonicalMonomial<Flt64, Int32> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (symbol in powers.keys) {
        provider[symbol]?.let { values[symbol] = it }
    }
    val polynomial = toSingleGenericCanonicalPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            one = Flt64.one
        )
    return polynomial.monomials.firstOrNull()?.toCanonicalMonomial()
        ?: CanonicalMonomial(
            coefficient = polynomial.constant,
            powers = emptyMap()
        )
}

fun CanonicalMonomial<Flt64, Int32>.partialEvaluate(values: Map<Symbol, Flt64>): CanonicalMonomial<Flt64, Int32> {
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

fun CanonicalPolynomial<Flt64, Int32>.evaluate(
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
        },
        one = Flt64.one
    )
}

fun CanonicalPolynomial<Flt64, Int32>.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return when (policy) {
        MissingValuePolicy.AsZero -> toGenericCanonicalPolynomial().evaluateGeneric(values, one = Flt64.one) { Flt64.zero }
        MissingValuePolicy.ReturnNull, MissingValuePolicy.Fail -> toGenericCanonicalPolynomial().evaluateGeneric(values, one = Flt64.one)
    }
}

fun CanonicalPolynomial<Flt64, Int32>.evaluateRet(
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

fun CanonicalPolynomial<Flt64, Int32>.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun CanonicalPolynomial<Flt64, Int32>.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
    return toGenericCanonicalPolynomial().evaluateOrderedGeneric(order, values, Flt64.one)
}

fun CanonicalPolynomial<Flt64, Int32>.partialEvaluate(
    provider: ValueProvider,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64, Int32> {
    val values = LinkedHashMap<Symbol, Flt64>()
    for (monomial in monomials) {
        for (symbol in monomial.powers.keys) {
            provider[symbol]?.let { values[symbol] = it }
        }
    }
    return toGenericCanonicalPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            one = Flt64.one,
            symbolComparator = symbolComparator
        )
        .toCanonicalPolynomialFromGeneric()
}

fun CanonicalPolynomial<Flt64, Int32>.partialEvaluate(
    values: Map<Symbol, Flt64>,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64, Int32> {
    return toGenericCanonicalPolynomial()
        .partialEvaluateGeneric(
            values = values,
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            one = Flt64.one,
            symbolComparator = symbolComparator
        )
        .toCanonicalPolynomialFromGeneric()
}