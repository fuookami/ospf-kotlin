package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.operator.*

private val defaultSymbolComparator: Comparator<Symbol> = Comparator { lhs, rhs ->
    val byName = lhs.name.compareTo(rhs.name)
    if (byName != 0) {
        byName
    } else {
        lhs.hashCode().compareTo(rhs.hashCode())
    }
}

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

fun Iterable<LinearMonomial>.combineTerms(): List<LinearMonomial> {
    val coefficientOfSymbol = LinkedHashMap<Symbol, Flt64>()
    for (monomial in this) {
        coefficientOfSymbol[monomial.symbol] =
            (coefficientOfSymbol[monomial.symbol] ?: Flt64.zero) + monomial.coefficient
    }
    return coefficientOfSymbol
        .asSequence()
        .filter { it.value neq Flt64.zero }
        .map { LinearMonomial(coefficient = it.value, symbol = it.key) }
        .toList()
}

fun LinearPolynomial.combineTerms(): LinearPolynomial {
    return this.copy(monomials = monomials.combineTerms())
}

fun Iterable<QuadraticMonomial>.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<QuadraticMonomial> {
    val comparator = symbolComparator ?: defaultSymbolComparator
    val coefficientOfKey = LinkedHashMap<Pair<Symbol, Symbol?>, Flt64>()
    for (monomial in this) {
        val key = normalizeQuadraticSymbols(monomial.symbol1, monomial.symbol2, comparator)
        coefficientOfKey[key] = (coefficientOfKey[key] ?: Flt64.zero) + monomial.coefficient
    }
    return coefficientOfKey
        .asSequence()
        .filter { it.value neq Flt64.zero }
        .map {
            QuadraticMonomial(
                coefficient = it.value,
                symbol1 = it.key.first,
                symbol2 = it.key.second
            )
        }
        .toList()
}

fun QuadraticPolynomial.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial {
    return this.copy(monomials = monomials.combineTerms(symbolComparator))
}

fun Iterable<CanonicalMonomial>.combineCanonicalTerms(
    symbolComparator: Comparator<Symbol>? = null
): List<CanonicalMonomial> {
    val comparator = symbolComparator ?: defaultSymbolComparator
    val coefficientOfFactors = LinkedHashMap<List<Symbol>, Flt64>()
    for (monomial in this) {
        val normalizedFactors = monomial.factors.sortedWith(comparator)
        coefficientOfFactors[normalizedFactors] =
            (coefficientOfFactors[normalizedFactors] ?: Flt64.zero) + monomial.coefficient
    }
    return coefficientOfFactors
        .asSequence()
        .filter { it.value neq Flt64.zero }
        .map { CanonicalMonomial(coefficient = it.value, factors = it.key) }
        .toList()
}

fun CanonicalPolynomial.combineTerms(
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial {
    return this.copy(monomials = monomials.combineCanonicalTerms(symbolComparator))
}
