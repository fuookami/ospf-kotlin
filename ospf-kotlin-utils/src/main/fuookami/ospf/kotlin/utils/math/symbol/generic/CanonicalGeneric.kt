package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial

data class GenericCanonicalMonomial<T>(
    val coefficient: T,
    val factors: List<Symbol> = emptyList()
) where T : Ring<T> {
    val degree: Int
        get() = factors.size
}

data class GenericCanonicalPolynomial<T>(
    val monomials: List<GenericCanonicalMonomial<T>> = emptyList(),
    val constant: T
) where T : Ring<T>

fun <T> Iterable<GenericCanonicalMonomial<T>>.combineCanonicalTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): List<GenericCanonicalMonomial<T>> where T : Ring<T> {
    val comparator = symbolComparator ?: defaultSymbolComparator
    val coefficientOfFactors = LinkedHashMap<List<Symbol>, T>()
    for (monomial in this) {
        val normalizedFactors = monomial.factors.sortedWith(comparator)
        coefficientOfFactors[normalizedFactors] =
            (coefficientOfFactors[normalizedFactors] ?: zero) + monomial.coefficient
    }
    return coefficientOfFactors
        .asSequence()
        .filter { !isZero(it.value) }
        .map { GenericCanonicalMonomial(coefficient = it.value, factors = it.key) }
        .toList()
}

fun <T> GenericCanonicalPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): GenericCanonicalPolynomial<T> where T : Ring<T> {
    return copy(monomials = monomials.combineCanonicalTerms(zero, isZero, symbolComparator))
}

fun <T> GenericCanonicalPolynomial<T>.evaluate(
    values: Map<Symbol, T>,
    onMissing: ((Symbol) -> T?)? = null
): T? where T : Ring<T> {
    var value = constant
    for (monomial in monomials) {
        var monomialValue = monomial.coefficient
        for (symbol in monomial.factors) {
            val factor = values[symbol]
                ?: onMissing?.invoke(symbol)
                ?: return null
            monomialValue *= factor
        }
        value += monomialValue
    }
    return value
}

fun <T> GenericCanonicalPolynomial<T>.evaluateOrdered(
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
        var monomialValue = monomial.coefficient
        for (symbol in monomial.factors) {
            val index = indexOfSymbol[symbol]
                ?: throw IllegalArgumentException("Symbol ${symbol.name} not found in order.")
            monomialValue *= values[index]
        }
        value += monomialValue
    }
    return value
}

fun <T> GenericCanonicalPolynomial<T>.partialEvaluate(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): GenericCanonicalPolynomial<T> where T : Ring<T> {
    val remainedMonomials = ArrayList<GenericCanonicalMonomial<T>>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        var newCoefficient = monomial.coefficient
        val remainedFactors = ArrayList<Symbol>(monomial.factors.size)
        for (symbol in monomial.factors) {
            val factor = values[symbol]
            if (factor == null) {
                remainedFactors.add(symbol)
            } else {
                newCoefficient *= factor
            }
        }
        if (remainedFactors.isEmpty()) {
            newConstant += newCoefficient
        } else {
            remainedMonomials.add(
                GenericCanonicalMonomial(
                    coefficient = newCoefficient,
                    factors = remainedFactors
                )
            )
        }
    }
    return GenericCanonicalPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineTerms(zero, isZero, symbolComparator)
}

fun CanonicalMonomial<Flt64>.toGenericCanonicalMonomial(): GenericCanonicalMonomial<Flt64> {
    return GenericCanonicalMonomial(
        coefficient = coefficient,
        factors = factors
    )
}

fun GenericCanonicalMonomial<Flt64>.toCanonicalMonomial(): CanonicalMonomial<Flt64> {
    return CanonicalMonomial<Flt64>(
        coefficient = coefficient,
        factors = factors
    )
}

fun CanonicalPolynomial<Flt64>.toGenericCanonicalPolynomial(): GenericCanonicalPolynomial<Flt64> {
    return GenericCanonicalPolynomial(
        monomials = monomials.map { it.toGenericCanonicalMonomial() },
        constant = constant
    )
}

fun GenericCanonicalPolynomial<Flt64>.toCanonicalPolynomial(): CanonicalPolynomial<Flt64> {
    return CanonicalPolynomial<Flt64>(
        monomials = monomials.map { it.toCanonicalMonomial() },
        constant = constant
    )
}
