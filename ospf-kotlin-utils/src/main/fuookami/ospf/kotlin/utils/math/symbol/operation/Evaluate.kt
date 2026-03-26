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
        is Fatal -> Fatal(valueResult.errors)
    }
}

fun LinearMonomial.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun LinearMonomial.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
    val indexOfSymbol = buildOrderedIndex(order, values)
    return coefficient * orderedValueOf(symbol, indexOfSymbol, values)
}

fun LinearMonomial.partialEvaluate(provider: ValueProvider): LinearPolynomial {
    val value = provider[symbol]
    return if (value == null) {
        LinearPolynomial(monomials = listOf(this))
    } else {
        LinearPolynomial(constant = coefficient * value)
    }
}

fun LinearMonomial.partialEvaluate(values: Map<Symbol, Flt64>): LinearPolynomial {
    return partialEvaluate(MapValueProvider(values))
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

fun QuadraticMonomial.evaluateRet(
    values: Map<Symbol, Flt64>,
    policy: MissingValuePolicy = MissingValuePolicy.Fail
): Ret<Flt64> {
    return evaluateRet(MapValueProvider(values), policy)
}

fun QuadraticMonomial.evaluateOrdered(
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

fun QuadraticMonomial.partialEvaluate(provider: ValueProvider): QuadraticPolynomial {
    val value1 = provider[symbol1]
    if (symbol2 == null) {
        return if (value1 == null) {
            QuadraticPolynomial(monomials = listOf(this))
        } else {
            QuadraticPolynomial(constant = coefficient * value1)
        }
    }

    val value2 = provider[symbol2]
    return when {
        value1 != null && value2 != null -> {
            QuadraticPolynomial(constant = coefficient * value1 * value2)
        }

        value1 != null -> {
            QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial(
                        coefficient = coefficient * value1,
                        symbol1 = symbol2,
                        symbol2 = null
                    )
                )
            )
        }

        value2 != null -> {
            QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial(
                        coefficient = coefficient * value2,
                        symbol1 = symbol1,
                        symbol2 = null
                    )
                )
            )
        }

        else -> {
            QuadraticPolynomial(monomials = listOf(this))
        }
    }
}

fun QuadraticMonomial.partialEvaluate(values: Map<Symbol, Flt64>): QuadraticPolynomial {
    return partialEvaluate(MapValueProvider(values))
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
            is Fatal -> return Fatal(result.errors)
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

fun CanonicalMonomial.evaluateOrdered(
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

fun CanonicalMonomial.partialEvaluate(provider: ValueProvider): CanonicalMonomial {
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
    return CanonicalMonomial(
        coefficient = newCoefficient,
        factors = remainedFactors
    )
}

fun CanonicalMonomial.partialEvaluate(values: Map<Symbol, Flt64>): CanonicalMonomial {
    return partialEvaluate(MapValueProvider(values))
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
            is Fatal -> return Fatal(result.errors)
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

fun LinearPolynomial.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
    val indexOfSymbol = buildOrderedIndex(order, values)
    var value = constant
    for (monomial in monomials) {
        value += monomial.coefficient * orderedValueOf(monomial.symbol, indexOfSymbol, values)
    }
    return value
}

fun LinearPolynomial.partialEvaluate(provider: ValueProvider): LinearPolynomial {
    val remainedMonomials = ArrayList<LinearMonomial>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        val value = provider[monomial.symbol]
        if (value == null) {
            remainedMonomials.add(monomial)
        } else {
            newConstant += monomial.coefficient * value
        }
    }
    return LinearPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineTerms()
}

fun LinearPolynomial.partialEvaluate(values: Map<Symbol, Flt64>): LinearPolynomial {
    return partialEvaluate(MapValueProvider(values))
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
            is Fatal -> return Fatal(result.errors)
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

fun QuadraticPolynomial.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
    val indexOfSymbol = buildOrderedIndex(order, values)
    var value = constant
    for (monomial in monomials) {
        val value1 = orderedValueOf(monomial.symbol1, indexOfSymbol, values)
        if (monomial.symbol2 == null) {
            value += monomial.coefficient * value1
        } else {
            value += monomial.coefficient * value1 * orderedValueOf(monomial.symbol2, indexOfSymbol, values)
        }
    }
    return value
}

fun QuadraticPolynomial.partialEvaluate(provider: ValueProvider): QuadraticPolynomial {
    val remainedMonomials = ArrayList<QuadraticMonomial>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        val partialMonomial = monomial.partialEvaluate(provider)
        remainedMonomials.addAll(partialMonomial.monomials)
        newConstant += partialMonomial.constant
    }
    return QuadraticPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineTerms()
}

fun QuadraticPolynomial.partialEvaluate(values: Map<Symbol, Flt64>): QuadraticPolynomial {
    return partialEvaluate(MapValueProvider(values))
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
            is Fatal -> return Fatal(result.errors)
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

fun CanonicalPolynomial.evaluateOrdered(
    order: List<Symbol>,
    values: List<Flt64>
): Flt64 {
    val indexOfSymbol = buildOrderedIndex(order, values)
    var value = constant
    for (monomial in monomials) {
        var monomialValue = monomial.coefficient
        for (symbol in monomial.factors) {
            monomialValue *= orderedValueOf(symbol, indexOfSymbol, values)
        }
        value += monomialValue
    }
    return value
}

fun CanonicalPolynomial.partialEvaluate(
    provider: ValueProvider,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial {
    val remainedMonomials = ArrayList<CanonicalMonomial>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        val partialMonomial = monomial.partialEvaluate(provider)
        if (partialMonomial.degree == 0) {
            newConstant += partialMonomial.coefficient
        } else {
            remainedMonomials.add(partialMonomial)
        }
    }
    return CanonicalPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineTerms(symbolComparator)
}

fun CanonicalPolynomial.partialEvaluate(
    values: Map<Symbol, Flt64>,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial {
    return partialEvaluate(
        provider = MapValueProvider(values),
        symbolComparator = symbolComparator
    )
}
