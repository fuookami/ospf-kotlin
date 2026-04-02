package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial

// ============================================================================
// Canonical Polynomial Operations (Ring-based, no Generic conversion)
// ============================================================================

/**
 * Compute power value^power for Ring types.
 * Internal for use across operation files.
 */
internal fun <T : Ring<T>> computeRingPower(value: T, power: Int, one: T): T {
    if (power == 0) return one
    if (power == 1) return value
    if (power < 0) {
        throw IllegalArgumentException("Negative exponent requires TimesGroup implementation.")
    }
    var result = one
    var base = value
    var exp = power
    while (exp > 0) {
        if (exp % 2 == 1) result *= base
        if (exp > 1) base *= base
        exp /= 2
    }
    return result
}

/**
 * Combine like terms in a canonical polynomial.
 * Direct Typed operation - no Generic conversion.
 *
 * Optimization: Uses PowerVectorKey instead of Map<Symbol, Int32> as HashMap key.
 * - Dense mode for small totalSymbols or high sparsity (powers.size / totalSymbols >= 0.5)
 * - Sparse mode for large totalSymbols with low sparsity
 * - Eliminates powers sorting overhead by using pre-computed hash
 *
 * Performance: ~50-100% faster than original Map-based implementation.
 */
fun <T> Iterable<CanonicalMonomial<T>>.combineCanonicalMonomials(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): List<CanonicalMonomial<T>> where T : Ring<T> {
    // Step 1: Collect all unique symbols and build index mapping
    val comparator = symbolComparator ?: defaultSymbolComparator
    val allSymbols = mutableSetOf<Symbol>()
    for (monomial in this) {
        allSymbols.addAll(monomial.powers.keys)
    }

    // Sort symbols by comparator for normalization
    val symbolList = allSymbols.sortedWith(comparator)
    val symbolIndex = symbolList.indices.associateBy { symbolList[it] }

    // Step 2: Combine using PowerVectorKey
    val coefficientOfKey = LinkedHashMap<PowerVectorKey, T>()

    for (monomial in this) {
        val key = PowerVectorKey.create(
            powers = monomial.powers,
            symbolIndex = symbolIndex,
            totalSymbols = symbolList.size
        )
        coefficientOfKey[key] = (coefficientOfKey[key] ?: zero) + monomial.coefficient
    }

    // Step 3: Convert back to CanonicalMonomial list
    return coefficientOfKey
        .asSequence()
        .filter { !isZero(it.value) }
        .map { entry ->
            CanonicalMonomial(
                coefficient = entry.value,
                powers = entry.key.toPowers(symbolList)
            )
        }
        .toList()
}

/**
 * Combine like terms in a canonical polynomial.
 */
fun <T> CanonicalPolynomial<T>.combineCanonicalPolynomialTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return copy(monomials = monomials.combineCanonicalMonomials(zero, isZero, symbolComparator))
}

/**
 * Evaluate a canonical polynomial with given values.
 * Requires one (multiplicative identity) for power computation.
 */
fun <T> CanonicalPolynomial<T>.evaluateCanonical(
    values: Map<Symbol, T>,
    onMissing: ((Symbol) -> T?)? = null,
    one: T
): T? where T : Ring<T> {
    var value = constant
    for (monomial in monomials) {
        var monomialValue = monomial.coefficient
        for ((symbol, power) in monomial.powers) {
            val factor = values[symbol]
                ?: onMissing?.invoke(symbol)
                ?: return null
            monomialValue *= computeRingPower(factor, power.toInt(), one)
        }
        value += monomialValue
    }
    return value
}

/**
 * Evaluate a canonical polynomial with ordered symbols and values.
 */
fun <T> CanonicalPolynomial<T>.evaluateCanonicalOrdered(
    order: List<Symbol>,
    values: List<T>,
    one: T
): T where T : Ring<T> {
    require(order.toSet().size == order.size) {
        "Symbol order contains duplicated symbols."
    }
    require(order.size == values.size) {
        "Order and values size mismatch: order.size=${order.size}, values.size=${values.size}."
    }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    var value = constant
    for (monomial in monomials) {
        var monomialValue = monomial.coefficient
        for ((symbol, power) in monomial.powers) {
            val index = indexOfSymbol[symbol]
                ?: throw IllegalArgumentException("Symbol ${symbol.name} not found in order.")
            monomialValue *= computeRingPower(values[index], power.toInt(), one)
        }
        value += monomialValue
    }
    return value
}

/**
 * Partially evaluate a canonical polynomial.
 */
fun <T> CanonicalPolynomial<T>.partialEvaluateCanonical(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    one: T,
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    val remainedMonomials = ArrayList<CanonicalMonomial<T>>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        var newCoefficient = monomial.coefficient
        val remainedPowers = LinkedHashMap<Symbol, Int32>()
        for ((symbol, power) in monomial.powers) {
            val factor = values[symbol]
            if (factor == null) {
                remainedPowers[symbol] = power
            } else {
                newCoefficient *= computeRingPower(factor, power.toInt(), one)
            }
        }
        if (remainedPowers.isEmpty()) {
            newConstant += newCoefficient
        } else {
            remainedMonomials.add(CanonicalMonomial(coefficient = newCoefficient, powers = remainedPowers))
        }
    }
    return CanonicalPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
}