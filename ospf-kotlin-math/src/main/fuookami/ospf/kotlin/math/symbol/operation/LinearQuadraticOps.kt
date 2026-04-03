package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial

// ============================================================================
// Linear Polynomial Operations (Ring-based, no Generic conversion)
// ============================================================================

/**
 * Combine like terms in a linear polynomial.
 */
fun <T> LinearPolynomial<T>.combineLinearTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    val coefficientOfSymbol = LinkedHashMap<Symbol, T>()
    for (monomial in monomials) {
        coefficientOfSymbol[monomial.symbol] =
            (coefficientOfSymbol[monomial.symbol] ?: zero) + monomial.coefficient
    }
    val newMonomials = coefficientOfSymbol
        .asSequence()
        .filter { !isZero(it.value) }
        .map { LinearMonomial(coefficient = it.value, symbol = it.key) }
        .toList()
    return LinearPolynomial(monomials = newMonomials, constant = constant)
}

/**
 * Evaluate a linear polynomial with given values.
 */
fun <T> LinearPolynomial<T>.evaluateLinear(
    values: Map<Symbol, T>,
    onMissing: ((Symbol) -> T?)? = null
): T? where T : Ring<T> {
    var value = constant
    for (monomial in monomials) {
        val symbolValue = values[monomial.symbol]
            ?: onMissing?.invoke(monomial.symbol)
            ?: return null
        value += monomial.coefficient * symbolValue
    }
    return value
}

/**
 * Evaluate a linear polynomial with ordered symbols and values.
 */
fun <T> LinearPolynomial<T>.evaluateLinearOrdered(
    order: List<Symbol>,
    values: List<T>
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
        val index = indexOfSymbol[monomial.symbol]
            ?: throw IllegalArgumentException("Symbol ${monomial.symbol.name} not found in order.")
        value += monomial.coefficient * values[index]
    }
    return value
}

/**
 * Partially evaluate a linear polynomial.
 */
fun <T> LinearPolynomial<T>.partialEvaluateLinear(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    val remainedMonomials = ArrayList<LinearMonomial<T>>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        val symbolValue = values[monomial.symbol]
        if (symbolValue == null) {
            remainedMonomials.add(monomial)
        } else {
            newConstant += monomial.coefficient * symbolValue
        }
    }
    return LinearPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineLinearTerms(zero, isZero)
}

// ============================================================================
// Quadratic Polynomial Operations (Ring-based, no Generic conversion)
// ============================================================================

/**
 * Combine like terms in a quadratic polynomial.
 */
fun <T> QuadraticPolynomial<T>.combineQuadraticTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T> where T : Ring<T> {
    val comparator = symbolComparator ?: defaultSymbolComparator
    val coefficientOfKey = LinkedHashMap<Pair<Symbol, Symbol?>, T>()

    fun normalizeKey(s1: Symbol, s2: Symbol?): Pair<Symbol, Symbol?> {
        if (s2 == null) return s1 to null
        return if (comparator.compare(s1, s2) <= 0) s1 to s2 else s2 to s1
    }

    for (monomial in monomials) {
        val key = normalizeKey(monomial.symbol1, monomial.symbol2)
        coefficientOfKey[key] = (coefficientOfKey[key] ?: zero) + monomial.coefficient
    }

    val newMonomials = coefficientOfKey
        .asSequence()
        .filter { !isZero(it.value) }
        .map { QuadraticMonomial(coefficient = it.value, symbol1 = it.key.first, symbol2 = it.key.second) }
        .toList()
    return QuadraticPolynomial(monomials = newMonomials, constant = constant)
}

/**
 * Evaluate a quadratic polynomial with given values.
 */
fun <T> QuadraticPolynomial<T>.evaluateQuadratic(
    values: Map<Symbol, T>,
    onMissing: ((Symbol) -> T?)? = null
): T? where T : Ring<T> {
    var value = constant
    for (monomial in monomials) {
        val v1 = values[monomial.symbol1] ?: onMissing?.invoke(monomial.symbol1) ?: return null
        var term = monomial.coefficient * v1
        if (monomial.symbol2 != null) {
            val v2 = values[monomial.symbol2] ?: onMissing?.invoke(monomial.symbol2) ?: return null
            term *= v2
        }
        value += term
    }
    return value
}

/**
 * Evaluate a quadratic polynomial with ordered symbols and values.
 */
fun <T> QuadraticPolynomial<T>.evaluateQuadraticOrdered(
    order: List<Symbol>,
    values: List<T>
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
        val i1 = indexOfSymbol[monomial.symbol1]
            ?: throw IllegalArgumentException("Symbol ${monomial.symbol1.name} not found in order.")
        var term = monomial.coefficient * values[i1]
        if (monomial.symbol2 != null) {
            val i2 = indexOfSymbol[monomial.symbol2]
                ?: throw IllegalArgumentException("Symbol ${monomial.symbol2.name} not found in order.")
            term *= values[i2]
        }
        value += term
    }
    return value
}

/**
 * Partially evaluate a quadratic polynomial.
 */
fun <T> QuadraticPolynomial<T>.partialEvaluateQuadratic(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T> where T : Ring<T> {
    val remainedMonomials = ArrayList<QuadraticMonomial<T>>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        val v1 = values[monomial.symbol1]
        val v2 = if (monomial.symbol2 != null) values[monomial.symbol2] else null

        when {
            v1 != null && v2 != null -> {
                // Both symbols have values, term becomes constant
                newConstant += monomial.coefficient * v1 * v2
            }
            v1 != null && monomial.symbol2 == null -> {
                // Only symbol1 has value and there's no symbol2 (linear term in quadratic form)
                newConstant += monomial.coefficient * v1
            }
            v1 != null -> {
                // Only symbol1 has value, symbol2 remains
                remainedMonomials.add(QuadraticMonomial(
                    coefficient = monomial.coefficient * v1,
                    symbol1 = monomial.symbol2!!,
                    symbol2 = null
                ))
            }
            v2 != null -> {
                // Only symbol2 has value, symbol1 remains
                remainedMonomials.add(QuadraticMonomial(
                    coefficient = monomial.coefficient * v2,
                    symbol1 = monomial.symbol1,
                    symbol2 = null
                ))
            }
            else -> {
                // Neither symbol has a value, keep monomial as-is
                remainedMonomials.add(monomial)
            }
        }
    }
    return QuadraticPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineQuadraticTerms(zero, isZero, symbolComparator)
}