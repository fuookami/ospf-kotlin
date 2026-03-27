package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

data class GenericQuadraticMonomial<T>(
    val coefficient: T,
    val symbol1: Symbol,
    val symbol2: Symbol? = null
) where T : Ring<T> {
    val isQuadratic: Boolean
        get() = symbol2 != null
}

data class GenericQuadraticPolynomial<T>(
    val monomials: List<GenericQuadraticMonomial<T>> = emptyList(),
    val constant: T
) where T : Ring<T>

private fun normalizeQuadraticSymbols(
    symbol1: Symbol,
    symbol2: Symbol?,
    symbolComparator: Comparator<Symbol>
): Pair<Symbol, Symbol?> {
    if (symbol2 == null) {
        return symbol1 to null
    }
    return if (symbolComparator.compare(symbol1, symbol2) <= 0) {
        symbol1 to symbol2
    } else {
        symbol2 to symbol1
    }
}

fun <T> Iterable<GenericQuadraticMonomial<T>>.combineQuadraticTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): List<GenericQuadraticMonomial<T>> where T : Ring<T> {
    val comparator = symbolComparator ?: defaultSymbolComparator
    val coefficientOfKey = LinkedHashMap<Pair<Symbol, Symbol?>, T>()
    for (monomial in this) {
        val key = normalizeQuadraticSymbols(monomial.symbol1, monomial.symbol2, comparator)
        coefficientOfKey[key] = (coefficientOfKey[key] ?: zero) + monomial.coefficient
    }
    return coefficientOfKey
        .asSequence()
        .filter { !isZero(it.value) }
        .map {
            GenericQuadraticMonomial(
                coefficient = it.value,
                symbol1 = it.key.first,
                symbol2 = it.key.second
            )
        }
        .toList()
}

fun <T> GenericQuadraticPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): GenericQuadraticPolynomial<T> where T : Ring<T> {
    return copy(monomials = monomials.combineQuadraticTerms(zero, isZero, symbolComparator))
}

fun <T> GenericQuadraticPolynomial<T>.evaluate(
    values: Map<Symbol, T>,
    onMissing: ((Symbol) -> T?)? = null
): T? where T : Ring<T> {
    var value = constant
    for (monomial in monomials) {
        val value1 = values[monomial.symbol1]
            ?: onMissing?.invoke(monomial.symbol1)
            ?: return null
        if (monomial.symbol2 == null) {
            value += monomial.coefficient * value1
        } else {
            val value2 = values[monomial.symbol2]
                ?: onMissing?.invoke(monomial.symbol2)
                ?: return null
            value += monomial.coefficient * value1 * value2
        }
    }
    return value
}

fun <T> GenericQuadraticPolynomial<T>.evaluateOrdered(
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
        val index1 = indexOfSymbol[monomial.symbol1]
            ?: throw IllegalArgumentException("Symbol ${monomial.symbol1.name} not found in order.")
        val value1 = values[index1]
        if (monomial.symbol2 == null) {
            value += monomial.coefficient * value1
        } else {
            val index2 = indexOfSymbol[monomial.symbol2]
                ?: throw IllegalArgumentException("Symbol ${monomial.symbol2.name} not found in order.")
            value += monomial.coefficient * value1 * values[index2]
        }
    }
    return value
}

fun <T> GenericQuadraticPolynomial<T>.partialEvaluate(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): GenericQuadraticPolynomial<T> where T : Ring<T> {
    val remainedMonomials = ArrayList<GenericQuadraticMonomial<T>>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        val value1 = values[monomial.symbol1]
        if (monomial.symbol2 == null) {
            if (value1 == null) {
                remainedMonomials.add(monomial)
            } else {
                newConstant += monomial.coefficient * value1
            }
            continue
        }

        val value2 = values[monomial.symbol2]
        when {
            value1 != null && value2 != null -> {
                newConstant += monomial.coefficient * value1 * value2
            }

            value1 != null -> {
                remainedMonomials.add(
                    GenericQuadraticMonomial(
                        coefficient = monomial.coefficient * value1,
                        symbol1 = monomial.symbol2,
                        symbol2 = null
                    )
                )
            }

            value2 != null -> {
                remainedMonomials.add(
                    GenericQuadraticMonomial(
                        coefficient = monomial.coefficient * value2,
                        symbol1 = monomial.symbol1,
                        symbol2 = null
                    )
                )
            }

            else -> {
                remainedMonomials.add(monomial)
            }
        }
    }
    return GenericQuadraticPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineTerms(zero, isZero, symbolComparator)
}

fun QuadraticMonomial<Flt64>.toGenericQuadraticMonomial(): GenericQuadraticMonomial<Flt64> {
    return GenericQuadraticMonomial(
        coefficient = coefficient,
        symbol1 = symbol1,
        symbol2 = symbol2
    )
}

fun GenericQuadraticMonomial<Flt64>.toQuadraticMonomial(): QuadraticMonomial<Flt64> {
    return QuadraticMonomial<Flt64>(
        coefficient = coefficient,
        symbol1 = symbol1,
        symbol2 = symbol2
    )
}

fun QuadraticPolynomial<Flt64>.toGenericQuadraticPolynomial(): GenericQuadraticPolynomial<Flt64> {
    return GenericQuadraticPolynomial(
        monomials = monomials.map { it.toGenericQuadraticMonomial() },
        constant = constant
    )
}

fun GenericQuadraticPolynomial<Flt64>.toQuadraticPolynomial(): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial<Flt64>(
        monomials = monomials.map { it.toQuadraticMonomial() },
        constant = constant
    )
}
