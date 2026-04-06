/**
 * 类型转换
 * Type Conversion
 *
 * 提供不同类型多项式之间的转换操作。
 * 支持线性、二次和规范多项式之间的相互转换，以及不等式的转换。
 * Provides conversion operations between different types of polynomials.
 * Supports mutual conversion between linear, quadratic, and canonical polynomials,
 * as well as inequality conversions.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial

// ============================================================================
// Flt64-specific convenience wrappers for functions requiring zero/isZero parameters
// ============================================================================

private fun LinearPolynomial<Flt64>.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return subtractLinear(
        rhs = rhs,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun CanonicalPolynomial<Flt64>.toLinearPolynomialOrNull(): LinearPolynomial<Flt64>? {
    return toLinearPolynomialOrNull(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun CanonicalPolynomial<Flt64>.toQuadraticPolynomialOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64>? {
    return toQuadraticPolynomialOrNull(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

// ============================================================================
// Ret wrappers for nullable conversion functions
// ============================================================================

fun CanonicalMonomial<Flt64>.toLinearMonomialRet(): Ret<LinearMonomial<Flt64>> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical monomial to linear monomial")
    }
}

fun CanonicalMonomial<Flt64>.toQuadraticMonomialRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticMonomial<Flt64>> {
    val quadraticMonomial = toQuadraticMonomialOrNull(symbolComparator)
    return if (quadraticMonomial != null) {
        Ok(quadraticMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical monomial to quadratic monomial")
    }
}

fun QuadraticMonomial<Flt64>.toLinearMonomialRet(): Ret<LinearMonomial<Flt64>> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic monomial to linear monomial")
    }
}

fun CanonicalPolynomial<Flt64>.toLinearPolynomialRet(): Ret<LinearPolynomial<Flt64>> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical polynomial to linear polynomial")
    }
}

fun CanonicalPolynomial<Flt64>.toQuadraticPolynomialRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticPolynomial<Flt64>> {
    val quadraticPolynomial = toQuadraticPolynomialOrNull(symbolComparator)
    return if (quadraticPolynomial != null) {
        Ok(quadraticPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical polynomial to quadratic polynomial")
    }
}

fun QuadraticPolynomial<Flt64>.toLinearPolynomialRet(): Ret<LinearPolynomial<Flt64>> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic polynomial to linear polynomial")
    }
}

// ============================================================================
// Inequality conversions
// ============================================================================

fun LinearInequality.toQuadraticInequality(): QuadraticInequality {
    return QuadraticInequality(
        lhs = lhs.toQuadraticPolynomial(),
        rhs = rhs.toQuadraticPolynomial(),
        comparison = comparison
    )
}

fun LinearInequality.toCanonicalInequality(
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality {
    return CanonicalInequality(
        lhs = lhs.toCanonicalPolynomial().combineTerms(symbolComparator),
        rhs = rhs.toCanonicalPolynomial().combineTerms(symbolComparator),
        comparison = comparison
    )
}

fun QuadraticInequality.toCanonicalInequality(
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality {
    return CanonicalInequality(
        lhs = lhs.toCanonicalPolynomial(symbolComparator).combineTerms(symbolComparator),
        rhs = rhs.toCanonicalPolynomial(symbolComparator).combineTerms(symbolComparator),
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

fun CanonicalInequality.toLinearInequalityOrNull(): LinearInequality? {
    val linearLhs = lhs.toLinearPolynomialOrNull() ?: return null
    val linearRhs = rhs.toLinearPolynomialOrNull() ?: return null
    return LinearInequality(
        lhs = linearLhs,
        rhs = linearRhs,
        comparison = comparison
    )
}

fun CanonicalInequality.toQuadraticInequalityOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticInequality? {
    val quadraticLhs = lhs.toQuadraticPolynomialOrNull(symbolComparator) ?: return null
    val quadraticRhs = rhs.toQuadraticPolynomialOrNull(symbolComparator) ?: return null
    return QuadraticInequality(
        lhs = quadraticLhs,
        rhs = quadraticRhs,
        comparison = comparison
    )
}

// ============================================================================
// Inequality operations
// ============================================================================

fun LinearInequality.moveAllToLhs(combineTerms: Boolean = true): LinearInequality {
    val lhsToZeroRhs = lhs.minus(rhs)
    val normalizedLhs = if (combineTerms) {
        lhsToZeroRhs.combineTerms()
    } else {
        lhsToZeroRhs
    }
    return LinearInequality(
        lhs = normalizedLhs,
        rhs = LinearPolynomial<Flt64>(constant = Flt64.zero),
        comparison = comparison
    )
}

fun LinearInequality.normalizeToLessEqualForm(combineTerms: Boolean = true): LinearInequality {
    if (comparison == Comparison.NE) {
        return this
    }
    val lessLikeInequality = if (comparison.isGreaterLike) {
        LinearInequality(
            lhs = rhs,
            rhs = lhs,
            comparison = comparison.reverse()
        )
    } else {
        this
    }
    return lessLikeInequality.moveAllToLhs(combineTerms)
}

fun CanonicalInequality.moveAllToLhs(
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality {
    val movedLhs = lhs.subtractCanonical(
        rhs = rhs,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = if (combineTerms) symbolComparator else null
    )
    val lhsToZeroRhs = CanonicalInequality(
        lhs = movedLhs,
        rhs = CanonicalPolynomial<Flt64>(constant = Flt64.zero),
        comparison = comparison
    )
    return if (combineTerms) {
        lhsToZeroRhs.copy(lhs = lhsToZeroRhs.lhs.combineTerms(symbolComparator))
    } else {
        lhsToZeroRhs
    }
}

fun CanonicalInequality.normalizeToLessEqualForm(
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality {
    if (comparison == Comparison.NE) {
        return this
    }
    val lessLikeInequality = if (comparison.isGreaterLike) {
        CanonicalInequality(
            lhs = rhs,
            rhs = lhs,
            comparison = comparison.reverse()
        )
    } else {
        this
    }
    return lessLikeInequality.moveAllToLhs(combineTerms, symbolComparator)
}