package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial

data class GenericLinearMonomial<T>(
    val coefficient: T,
    val symbol: Symbol
) where T : Ring<T>

data class GenericLinearPolynomial<T>(
    val monomials: List<GenericLinearMonomial<T>> = emptyList(),
    val constant: T
) where T : Ring<T>

fun <T> Iterable<GenericLinearMonomial<T>>.combineLinearTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): List<GenericLinearMonomial<T>> where T : Ring<T> {
    val coefficientOfSymbol = LinkedHashMap<Symbol, T>()
    for (monomial in this) {
        coefficientOfSymbol[monomial.symbol] =
            (coefficientOfSymbol[monomial.symbol] ?: zero) + monomial.coefficient
    }
    return coefficientOfSymbol
        .asSequence()
        .filter { !isZero(it.value) }
        .map { GenericLinearMonomial(coefficient = it.value, symbol = it.key) }
        .toList()
}

fun <T> GenericLinearPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): GenericLinearPolynomial<T> where T : Ring<T> {
    return copy(monomials = monomials.combineLinearTerms(zero, isZero))
}

fun <T> GenericLinearPolynomial<T>.evaluate(
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

fun <T> GenericLinearPolynomial<T>.evaluateOrdered(
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

fun <T> GenericLinearPolynomial<T>.partialEvaluate(
    values: Map<Symbol, T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): GenericLinearPolynomial<T> where T : Ring<T> {
    val remainedMonomials = ArrayList<GenericLinearMonomial<T>>(monomials.size)
    var newConstant = constant
    for (monomial in monomials) {
        val symbolValue = values[monomial.symbol]
        if (symbolValue == null) {
            remainedMonomials.add(monomial)
        } else {
            newConstant += monomial.coefficient * symbolValue
        }
    }
    return GenericLinearPolynomial(
        monomials = remainedMonomials,
        constant = newConstant
    ).combineTerms(zero, isZero)
}

fun <T> LinearMonomial<T>.toGenericLinearMonomial(): GenericLinearMonomial<T>
        where T : Ring<T> {
    return GenericLinearMonomial(
        coefficient = coefficient,
        symbol = symbol
    )
}

fun <T> LinearPolynomial<T>.toGenericLinearPolynomial(): GenericLinearPolynomial<T>
        where T : Ring<T> {
    return GenericLinearPolynomial(
        monomials = monomials.map { it.toGenericLinearMonomial() },
        constant = constant
    )
}

fun GenericLinearMonomial<Flt64>.toLinearMonomial(): LinearMonomial<Flt64> {
    return LinearMonomial<Flt64>(
        coefficient = coefficient,
        symbol = symbol
    )
}

fun GenericLinearPolynomial<Flt64>.toLinearPolynomial(): LinearPolynomial<Flt64> {
    return LinearPolynomial<Flt64>(
        monomials = monomials.map { it.toLinearMonomial() },
        constant = constant
    )
}





