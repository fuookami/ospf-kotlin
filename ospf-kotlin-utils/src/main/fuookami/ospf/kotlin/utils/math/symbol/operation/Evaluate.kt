package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.adapter.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*

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

fun LinearMonomial.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    val value = resolveValue(symbol, provider, policy) ?: return null
    return coefficient * value
}

fun LinearMonomial.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

fun LinearMonomial.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return when (val valueResult = resolveValueRet(symbol, provider, policy)) {
        is Ok -> Ok(coefficient * valueResult.value)
        is Failed -> Failed(valueResult.error)
    }
}

fun LinearMonomial.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun QuadraticMonomial.evaluate(
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

fun QuadraticMonomial.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

fun QuadraticMonomial.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    val value1 = when (val result = resolveValueRet(symbol1, provider, policy)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
    }
    return if (symbol2 == null) {
        Ok(coefficient * value1)
    } else {
        val value2 = when (val result = resolveValueRet(symbol2, provider, policy)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
        }
        Ok(coefficient * value1 * value2)
    }
}

fun QuadraticMonomial.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun CanonicalMonomial.evaluate(
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

fun CanonicalMonomial.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

fun CanonicalMonomial.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var value = coefficient
    for (symbol in factors) {
        val factor = when (val result = resolveValueRet(symbol, provider, policy)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
        }
        value = value * factor
    }
    return Ok(value)
}

fun CanonicalMonomial.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun LinearPolynomial.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    var value = constant
    for (monomial in monomials) {
        val monomialValue = monomial.evaluate(provider, policy) ?: return null
        value += monomialValue
    }
    return value
}

fun LinearPolynomial.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

fun LinearPolynomial.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var value = constant
    for (monomial in monomials) {
        when (val result = monomial.evaluateRet(provider, policy)) {
            is Ok -> value += result.value
            is Failed -> return Failed(result.error)
        }
    }
    return Ok(value)
}

fun LinearPolynomial.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun QuadraticPolynomial.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    var value = constant
    for (monomial in monomials) {
        val monomialValue = monomial.evaluate(provider, policy) ?: return null
        value += monomialValue
    }
    return value
}

fun QuadraticPolynomial.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

fun QuadraticPolynomial.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var value = constant
    for (monomial in monomials) {
        when (val result = monomial.evaluateRet(provider, policy)) {
            is Ok -> value += result.value
            is Failed -> return Failed(result.error)
        }
    }
    return Ok(value)
}

fun QuadraticPolynomial.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun CanonicalPolynomial.evaluate(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    var value = constant
    for (monomial in monomials) {
        val monomialValue = monomial.evaluate(provider, policy) ?: return null
        value += monomialValue
    }
    return value
}

fun CanonicalPolynomial.evaluate(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.ReturnNull
): Flt64? {
    return evaluate(MapValueProvider(values), policy)
}

fun CanonicalPolynomial.evaluateRet(
    provider: ValueProvider,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    var value = constant
    for (monomial in monomials) {
        when (val result = monomial.evaluateRet(provider, policy)) {
            is Ok -> value += result.value
            is Failed -> return Failed(result.error)
        }
    }
    return Ok(value)
}

fun CanonicalPolynomial.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}
