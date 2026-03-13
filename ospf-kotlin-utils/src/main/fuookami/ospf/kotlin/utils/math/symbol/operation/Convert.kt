package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.inequality.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*

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

fun QuadraticMonomial.toLinearMonomialOrNull(): LinearMonomial? {
    if (isQuadratic) {
        return null
    }
    return LinearMonomial(
        coefficient = coefficient,
        symbol = symbol1
    )
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
