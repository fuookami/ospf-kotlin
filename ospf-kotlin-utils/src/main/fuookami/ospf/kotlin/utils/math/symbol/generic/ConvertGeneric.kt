package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol

fun <T> GenericLinearMonomial<T>.toGenericQuadraticMonomial(): GenericQuadraticMonomial<T>
        where T : Ring<T> {
    return GenericQuadraticMonomial(
        coefficient = coefficient,
        symbol1 = symbol,
        symbol2 = null
    )
}

fun <T> GenericLinearMonomial<T>.toGenericCanonicalMonomial(): GenericCanonicalMonomial<T>
        where T : Ring<T> {
    return GenericCanonicalMonomial(
        coefficient = coefficient,
        powers = mapOf(symbol to 1)
    )
}

fun <T> GenericQuadraticMonomial<T>.toGenericLinearMonomialOrNull(): GenericLinearMonomial<T>?
        where T : Ring<T> {
    if (isQuadratic) {
        return null
    }
    return GenericLinearMonomial(
        coefficient = coefficient,
        symbol = symbol1
    )
}

fun <T> GenericQuadraticMonomial<T>.toGenericCanonicalMonomial(
    symbolComparator: Comparator<Symbol>? = null
): GenericCanonicalMonomial<T> where T : Ring<T> {
    val powers = if (symbol2 == null) {
        mapOf(symbol1 to 1)
    } else {
        mapOf(symbol1 to 1, symbol2 to 1)
    }
    return GenericCanonicalMonomial(
        coefficient = coefficient,
        powers = powers
    )
}

fun <T> GenericCanonicalMonomial<T>.toGenericLinearMonomialOrNull(): GenericLinearMonomial<T>?
        where T : Ring<T> {
    if (degree != 1) {
        return null
    }
    // 找到指数为1的符号
    val entry = powers.entries.firstOrNull { it.value == 1 } ?: return null
    return GenericLinearMonomial(
        coefficient = coefficient,
        symbol = entry.key
    )
}

fun <T> GenericCanonicalMonomial<T>.toGenericQuadraticMonomialOrNull(
    symbolComparator: Comparator<Symbol>? = null
): GenericQuadraticMonomial<T>? where T : Ring<T> {
    if (degree == 1) {
        val entry = powers.entries.firstOrNull { it.value == 1 } ?: return null
        return GenericQuadraticMonomial(
            coefficient = coefficient,
            symbol1 = entry.key,
            symbol2 = null
        )
    }
    if (degree != 2) {
        return null
    }
    // 处理 x^2 或 x*y 的情况
    val entries = powers.entries.toList()
    if (entries.size == 1 && entries[0].value == 2) {
        // x^2 的情况
        return GenericQuadraticMonomial(
            coefficient = coefficient,
            symbol1 = entries[0].key,
            symbol2 = entries[0].key
        )
    } else if (entries.size == 2 && entries.all { it.value == 1 }) {
        // x*y 的情况
        val sortedEntries = if (symbolComparator != null) {
            entries.sortedWith(compareBy { symbolComparator.compare(it.key, it.key) })
        } else {
            entries
        }
        return GenericQuadraticMonomial(
            coefficient = coefficient,
            symbol1 = sortedEntries[0].key,
            symbol2 = sortedEntries[1].key
        )
    }
    return null
}

fun <T> GenericLinearPolynomial<T>.toGenericQuadraticPolynomial(): GenericQuadraticPolynomial<T>
        where T : Ring<T> {
    return GenericQuadraticPolynomial(
        monomials = monomials.map { it.toGenericQuadraticMonomial() },
        constant = constant
    )
}

fun <T> GenericLinearPolynomial<T>.toGenericCanonicalPolynomial(): GenericCanonicalPolynomial<T>
        where T : Ring<T> {
    return GenericCanonicalPolynomial(
        monomials = monomials.map { it.toGenericCanonicalMonomial() },
        constant = constant
    )
}

fun <T> GenericQuadraticPolynomial<T>.toGenericLinearPolynomialOrNull(): GenericLinearPolynomial<T>?
        where T : Ring<T> {
    if (monomials.any { it.isQuadratic }) {
        return null
    }
    return GenericLinearPolynomial(
        monomials = monomials.map {
            GenericLinearMonomial(
                coefficient = it.coefficient,
                symbol = it.symbol1
            )
        },
        constant = constant
    )
}

fun <T> GenericQuadraticPolynomial<T>.toGenericCanonicalPolynomial(
    symbolComparator: Comparator<Symbol>? = null
): GenericCanonicalPolynomial<T> where T : Ring<T> {
    return GenericCanonicalPolynomial(
        monomials = monomials.map { it.toGenericCanonicalMonomial(symbolComparator) },
        constant = constant
    )
}

fun <T> GenericCanonicalPolynomial<T>.toGenericLinearPolynomialOrNull(
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): GenericLinearPolynomial<T>? where T : Ring<T> {
    val linearMonomials = ArrayList<GenericLinearMonomial<T>>(monomials.size)
    var canonicalConstant = constant
    for (monomial in monomials) {
        when (monomial.degree) {
            0 -> {
                canonicalConstant += monomial.coefficient
            }

            1 -> {
                val entry = monomial.powers.entries.firstOrNull { it.value == 1 } ?: return null
                linearMonomials.add(
                    GenericLinearMonomial(
                        coefficient = monomial.coefficient,
                        symbol = entry.key
                    )
                )
            }

            else -> {
                return null
            }
        }
    }
    return GenericLinearPolynomial(
        monomials = linearMonomials,
        constant = canonicalConstant
    ).combineTerms(zero, isZero)
}

fun <T> GenericCanonicalPolynomial<T>.toGenericQuadraticPolynomialOrNull(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): GenericQuadraticPolynomial<T>? where T : Ring<T> {
    val quadraticMonomials = ArrayList<GenericQuadraticMonomial<T>>(monomials.size)
    var canonicalConstant = constant
    for (monomial in monomials) {
        when (monomial.degree) {
            0 -> {
                canonicalConstant += monomial.coefficient
            }

            1, 2 -> {
                quadraticMonomials.add(
                    monomial.toGenericQuadraticMonomialOrNull(symbolComparator) ?: return null
                )
            }

            else -> {
                return null
            }
        }
    }
    return GenericQuadraticPolynomial(
        monomials = quadraticMonomials,
        constant = canonicalConstant
    ).combineTerms(zero, isZero, symbolComparator)
}

fun <T> GenericLinearPolynomial<T>.subtract(
    rhs: GenericLinearPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): GenericLinearPolynomial<T> where T : Ring<T> {
    return GenericLinearPolynomial(
        monomials = monomials + rhs.monomials.map {
            GenericLinearMonomial(
                coefficient = -it.coefficient,
                symbol = it.symbol
            )
        },
        constant = constant - rhs.constant
    ).combineTerms(zero, isZero)
}

fun <T> GenericCanonicalPolynomial<T>.subtract(
    rhs: GenericCanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): GenericCanonicalPolynomial<T> where T : Ring<T> {
    return GenericCanonicalPolynomial(
        monomials = monomials + rhs.monomials.map {
            GenericCanonicalMonomial(
                coefficient = -it.coefficient,
                powers = it.powers
            )
        },
        constant = constant - rhs.constant
    ).combineTerms(zero, isZero, symbolComparator)
}