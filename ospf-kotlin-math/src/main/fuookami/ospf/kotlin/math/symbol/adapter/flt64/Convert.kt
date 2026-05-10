@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.adapter.flt64

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
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.*

private fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.minus(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return subtractLinear(
        rhs = rhs,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toQuadraticPolynomialOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
    return toQuadraticPolynomialOrNull(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

enum class TryToLinearError {
    CanonicalMonomialIsNotLinear,
    QuadraticMonomialIsNotLinear,
    CanonicalPolynomialIsNotLinear,
    QuadraticPolynomialIsNotLinear,
    QuadraticInequalityIsNotLinear,
    CanonicalInequalityIsNotLinear
}

enum class TryToQuadraticError {
    CanonicalMonomialIsNotQuadratic,
    CanonicalPolynomialIsNotQuadratic,
    CanonicalInequalityIsNotQuadratic
}

enum class TryToCanonicalError {
    Unsupported
}

fun CanonicalMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toLinearMonomialRet(): Ret<LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical monomial to linear monomial.", TryToLinearError.CanonicalMonomialIsNotLinear)
    }
}

fun CanonicalMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toQuadraticMonomialRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    val quadraticMonomial = toQuadraticMonomialOrNull(symbolComparator)
    return if (quadraticMonomial != null) {
        Ok(quadraticMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical monomial to quadratic monomial.", TryToQuadraticError.CanonicalMonomialIsNotQuadratic)
    }
}

fun QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toLinearMonomialRet(): Ret<LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic monomial to linear monomial.", TryToLinearError.QuadraticMonomialIsNotLinear)
    }
}

fun CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toLinearPolynomialRet(): Ret<LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical polynomial to linear polynomial.", TryToLinearError.CanonicalPolynomialIsNotLinear)
    }
}

fun CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toQuadraticPolynomialRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    val quadraticPolynomial = toQuadraticPolynomialOrNull(symbolComparator)
    return if (quadraticPolynomial != null) {
        Ok(quadraticPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical polynomial to quadratic polynomial.", TryToQuadraticError.CanonicalPolynomialIsNotQuadratic)
    }
}

fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toLinearPolynomialRet(): Ret<LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic polynomial to linear polynomial.", TryToLinearError.QuadraticPolynomialIsNotLinear)
    }
}

fun LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toQuadraticInequality(): QuadraticInequality {
    return QuadraticInequality(
        lhs = lhs.toQuadraticPolynomial(),
        rhs = rhs.toQuadraticPolynomial(),
        comparison = comparison
    )
}

fun LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toCanonicalInequality(
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        lhs = lhs.toCanonicalPolynomial().combineTerms(symbolComparator),
        rhs = rhs.toCanonicalPolynomial().combineTerms(symbolComparator),
        comparison = comparison
    )
}

fun QuadraticInequality.toCanonicalInequality(
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        lhs = lhs.toCanonicalPolynomial(symbolComparator).combineTerms(symbolComparator),
        rhs = rhs.toCanonicalPolynomial(symbolComparator).combineTerms(symbolComparator),
        comparison = comparison
    )
}

fun QuadraticInequality.toLinearInequalityOrNull(): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
    val linearLhs = lhs.toLinearPolynomialOrNull() ?: return null
    val linearRhs = rhs.toLinearPolynomialOrNull() ?: return null
    return LinearInequality(lhs = linearLhs, rhs = linearRhs, comparison = comparison)
}

fun QuadraticInequality.toLinearInequalityRet(): Ret<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    val linearInequality = toLinearInequalityOrNull()
    return if (linearInequality != null) {
        Ok(linearInequality)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic inequality to linear inequality.", TryToLinearError.QuadraticInequalityIsNotLinear)
    }
}

fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toLinearInequalityOrNull(): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
    val linearLhs = lhs.toLinearPolynomialOrNull() ?: return null
    val linearRhs = rhs.toLinearPolynomialOrNull() ?: return null
    return LinearInequality(lhs = linearLhs, rhs = linearRhs, comparison = comparison)
}

fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toQuadraticInequalityOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticInequality? {
    val quadraticLhs = lhs.toQuadraticPolynomialOrNull(symbolComparator) ?: return null
    val quadraticRhs = rhs.toQuadraticPolynomialOrNull(symbolComparator) ?: return null
    return QuadraticInequality(lhs = quadraticLhs, rhs = quadraticRhs, comparison = comparison)
}

fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toLinearInequalityRet(): Ret<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
    val linearInequality = toLinearInequalityOrNull()
    return if (linearInequality != null) {
        Ok(linearInequality)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical inequality to linear inequality.", TryToLinearError.CanonicalInequalityIsNotLinear)
    }
}

fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toQuadraticInequalityRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticInequality> {
    val quadraticInequality = toQuadraticInequalityOrNull(symbolComparator)
    return if (quadraticInequality != null) {
        Ok(quadraticInequality)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical inequality to quadratic inequality.", TryToQuadraticError.CanonicalInequalityIsNotQuadratic)
    }
}

fun LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.moveAllToLhs(combineTerms: Boolean = true): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val lhsToZeroRhs = lhs.minus(rhs)
    val normalizedLhs = if (combineTerms) {
        lhsToZeroRhs.combineTerms()
    } else {
        lhsToZeroRhs
    }
    return LinearInequality(lhs = normalizedLhs, rhs = LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>(constant = Flt64.zero), comparison = comparison)
}

fun LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.normalizeToLessEqualForm(combineTerms: Boolean = true): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    if (comparison == Comparison.NE) {
        return this
    }
    val lessLikeInequality = if (comparison.isGreaterLike) {
        LinearInequality(lhs = rhs, rhs = lhs, comparison = comparison.reverse())
    } else {
        this
    }
    return lessLikeInequality.moveAllToLhs(combineTerms)
}

fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.moveAllToLhs(
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val movedLhs = lhs.subtractCanonical(
        rhs = rhs,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = if (combineTerms) symbolComparator else null
    )
    val lhsToZeroRhs = CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(lhs = movedLhs, rhs = CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>(constant = Flt64.zero), comparison = comparison)
    return if (combineTerms) {
        lhsToZeroRhs.copy(lhs = lhsToZeroRhs.lhs.combineTerms(symbolComparator))
    } else {
        lhsToZeroRhs
    }
}

fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.normalizeToLessEqualForm(
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    if (comparison == Comparison.NE) {
        return this
    }
    val lessLikeInequality = if (comparison.isGreaterLike) {
        CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(lhs = rhs, rhs = lhs, comparison = comparison.reverse())
    } else {
        this
    }
    return lessLikeInequality.moveAllToLhs(combineTerms, symbolComparator)
}
