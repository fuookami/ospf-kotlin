package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

private val canonicalSymbolComparator: java.util.Comparator<Symbol> = java.util.Comparator { lhs, rhs ->
    val byName = lhs.name.compareTo(rhs.name)
    if (byName != 0) {
        byName
    } else {
        lhs.hashCode().compareTo(rhs.hashCode())
    }
}

private fun reversedDirection(comparison: Comparison): Comparison {
    return when (comparison) {
        Comparison.LT -> Comparison.GT
        Comparison.LE -> Comparison.GE
        Comparison.EQ -> Comparison.EQ
        Comparison.NE -> Comparison.NE
        Comparison.GE -> Comparison.LE
        Comparison.GT -> Comparison.LT
    }
}

private fun LinearPolynomial.minus(rhs: LinearPolynomial): LinearPolynomial {
    return LinearPolynomial(
        monomials = monomials + rhs.monomials.map {
            LinearMonomial(
                coefficient = -it.coefficient,
                symbol = it.symbol
            )
        },
        constant = constant - rhs.constant
    )
}

fun LinearMonomial.toQuadraticMonomial(): QuadraticMonomial {
    return QuadraticMonomial(
        coefficient = coefficient,
        symbol1 = symbol,
        symbol2 = null
    )
}

fun LinearMonomial.toCanonicalMonomial(): CanonicalMonomial {
    return CanonicalMonomial(
        coefficient = coefficient,
        factors = listOf(symbol)
    )
}

fun QuadraticMonomial.toLinearMonomialOrNull(): LinearMonomial? {
    if (isQuadratic) {
        return null
    }
    return LinearMonomial(
        coefficient = coefficient,
        symbol = symbol1
    )
}

fun QuadraticMonomial.toCanonicalMonomial(
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalMonomial {
    val factors = if (symbol2 == null) {
        listOf(symbol1)
    } else {
        listOf(symbol1, symbol2)
    }
    val comparator = symbolComparator ?: canonicalSymbolComparator
    return CanonicalMonomial(
        coefficient = coefficient,
        factors = factors.sortedWith(comparator)
    )
}

fun CanonicalMonomial.toLinearMonomialOrNull(): LinearMonomial? {
    if (degree != 1) {
        return null
    }
    return LinearMonomial(
        coefficient = coefficient,
        symbol = factors.first()
    )
}

fun CanonicalMonomial.toLinearMonomialRet(): Ret<LinearMonomial> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical monomial to linear monomial")
    }
}

fun CanonicalMonomial.toQuadraticMonomialOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticMonomial? {
    if (degree == 1) {
        return QuadraticMonomial(
            coefficient = coefficient,
            symbol1 = factors.first(),
            symbol2 = null
        )
    }
    if (degree != 2) {
        return null
    }
    val comparator = symbolComparator ?: canonicalSymbolComparator
    val normalizedFactors = factors.sortedWith(comparator)
    return QuadraticMonomial(
        coefficient = coefficient,
        symbol1 = normalizedFactors.first(),
        symbol2 = normalizedFactors.last()
    )
}

fun CanonicalMonomial.toQuadraticMonomialRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticMonomial> {
    val quadraticMonomial = toQuadraticMonomialOrNull(symbolComparator)
    return if (quadraticMonomial != null) {
        Ok(quadraticMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical monomial to quadratic monomial")
    }
}

fun QuadraticMonomial.toLinearMonomialRet(): Ret<LinearMonomial> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic monomial to linear monomial")
    }
}

fun LinearPolynomial.toQuadraticPolynomial(): QuadraticPolynomial {
    return QuadraticPolynomial(
        monomials = monomials.map { it.toQuadraticMonomial() },
        constant = constant
    )
}

fun LinearPolynomial.toCanonicalPolynomial(): CanonicalPolynomial {
    return CanonicalPolynomial(
        monomials = monomials.map { it.toCanonicalMonomial() },
        constant = constant
    )
}

fun QuadraticPolynomial.toLinearPolynomialOrNull(): LinearPolynomial? {
    if (monomials.any { it.isQuadratic }) {
        return null
    }
    return LinearPolynomial(
        monomials = monomials.map {
            LinearMonomial(
                coefficient = it.coefficient,
                symbol = it.symbol1
            )
        },
        constant = constant
    )
}

fun QuadraticPolynomial.toCanonicalPolynomial(
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial {
    return CanonicalPolynomial(
        monomials = monomials.map { it.toCanonicalMonomial(symbolComparator) },
        constant = constant
    )
}

fun CanonicalPolynomial.toLinearPolynomialOrNull(): LinearPolynomial? {
    val linearMonomials = ArrayList<LinearMonomial>(monomials.size)
    var canonicalConstant = constant
    for (monomial in monomials) {
        when (monomial.degree) {
            0 -> {
                canonicalConstant += monomial.coefficient
            }

            1 -> {
                linearMonomials.add(
                    LinearMonomial(
                        coefficient = monomial.coefficient,
                        symbol = monomial.factors.first()
                    )
                )
            }

            else -> {
                return null
            }
        }
    }
    return LinearPolynomial(
        monomials = linearMonomials.combineTerms(),
        constant = canonicalConstant
    )
}

fun CanonicalPolynomial.toLinearPolynomialRet(): Ret<LinearPolynomial> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical polynomial to linear polynomial")
    }
}

fun CanonicalPolynomial.toQuadraticPolynomialOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticPolynomial? {
    val quadraticMonomials = ArrayList<QuadraticMonomial>(monomials.size)
    var canonicalConstant = constant
    for (monomial in monomials) {
        when (monomial.degree) {
            0 -> {
                canonicalConstant += monomial.coefficient
            }

            1, 2 -> {
                quadraticMonomials.add(
                    monomial.toQuadraticMonomialOrNull(symbolComparator) ?: return null
                )
            }

            else -> {
                return null
            }
        }
    }
    return QuadraticPolynomial(
        monomials = quadraticMonomials.combineTerms(symbolComparator),
        constant = canonicalConstant
    )
}

fun CanonicalPolynomial.toQuadraticPolynomialRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticPolynomial> {
    val quadraticPolynomial = toQuadraticPolynomialOrNull(symbolComparator)
    return if (quadraticPolynomial != null) {
        Ok(quadraticPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical polynomial to quadratic polynomial")
    }
}

fun QuadraticPolynomial.toLinearPolynomialRet(): Ret<LinearPolynomial> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic polynomial to linear polynomial")
    }
}

fun LinearInequality.toQuadraticInequality(): QuadraticInequality {
    return QuadraticInequality(
        lhs = lhs.toQuadraticPolynomial(),
        rhs = rhs.toQuadraticPolynomial(),
        comparison = comparison
    )
}

fun QuadraticInequality.toLinearInequalityOrNull(): LinearInequality? {
    val linearLhs = lhs.toLinearPolynomialOrNull() ?: return null
    val linearRhs = rhs.toLinearPolynomialOrNull() ?: return null
    return LinearInequality(
        lhs = linearLhs,
        rhs = linearRhs,
        comparison = comparison
    )
}

fun QuadraticInequality.toLinearInequalityRet(): Ret<LinearInequality> {
    val linearInequality = toLinearInequalityOrNull()
    return if (linearInequality != null) {
        Ok(linearInequality)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic inequality to linear inequality")
    }
}

fun LinearInequality.moveAllToLhs(combineTerms: Boolean = true): LinearInequality {
    val lhsToZeroRhs = lhs.minus(rhs)
    val normalizedLhs = if (combineTerms) {
        lhsToZeroRhs.combineTerms()
    } else {
        lhsToZeroRhs
    }
    return LinearInequality(
        lhs = normalizedLhs,
        rhs = LinearPolynomial(constant = Flt64.zero),
        comparison = comparison
    )
}

fun LinearInequality.normalizeToLessEqualForm(combineTerms: Boolean = true): LinearInequality {
    val lessLikeInequality = when (comparison) {
        Comparison.GT, Comparison.GE -> {
            LinearInequality(
                lhs = rhs,
                rhs = lhs,
                comparison = reversedDirection(comparison)
            )
        }

        else -> this
    }
    return lessLikeInequality.moveAllToLhs(combineTerms)
}
