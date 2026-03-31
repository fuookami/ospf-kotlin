package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.utils.math.symbol.generic.toCanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toCanonicalPolynomial as toCanonicalPolynomialFromGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.subtract as subtractGenericCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.subtract as subtractGenericLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearPolynomialOrNull
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticPolynomialOrNull
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearMonomialOrNull
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticMonomialOrNull
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalMonomial as toGenericCanonicalFromLinearGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericCanonicalMonomial as toGenericCanonicalFromQuadraticGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticMonomial as toGenericQuadraticMonomialFromLinearGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticPolynomial as toGenericQuadraticPolynomialFromLinearGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toLinearPolynomial as toLinearPolynomialFromGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toQuadraticPolynomial as toQuadraticPolynomialFromGeneric
import fuookami.ospf.kotlin.utils.math.symbol.generic.toQuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

private fun LinearPolynomial<Flt64>.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return toGenericLinearPolynomial()
        .subtractGenericLinearPolynomial(
            rhs = rhs.toGenericLinearPolynomial(),
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
        .toLinearPolynomialFromGeneric()
}

fun LinearMonomial<Flt64>.toQuadraticMonomial(): QuadraticMonomial<Flt64> {
    return toGenericLinearMonomial()
        .toGenericQuadraticMonomialFromLinearGeneric()
        .toQuadraticMonomial()
}

fun LinearMonomial<Flt64>.toCanonicalMonomial(): CanonicalMonomial<Flt64, Int32> {
    return toGenericLinearMonomial()
        .toGenericCanonicalFromLinearGeneric()
        .toCanonicalMonomial()
}

fun QuadraticMonomial<Flt64>.toLinearMonomialOrNull(): LinearMonomial<Flt64>? {
    return toGenericQuadraticMonomial()
        .toGenericLinearMonomialOrNull()
        ?.toLinearMonomial()
}

fun QuadraticMonomial<Flt64>.toCanonicalMonomial(
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalMonomial<Flt64, Int32> {
    val comparator = symbolComparator ?: defaultSymbolComparator
    return toGenericQuadraticMonomial()
        .toGenericCanonicalFromQuadraticGeneric(comparator)
        .toCanonicalMonomial()
}

fun CanonicalMonomial<Flt64, Int32>.toLinearMonomialOrNull(): LinearMonomial<Flt64>? {
    return toGenericCanonicalMonomial()
        .toGenericLinearMonomialOrNull()
        ?.toLinearMonomial()
}

fun CanonicalMonomial<Flt64, Int32>.toLinearMonomialRet(): Ret<LinearMonomial<Flt64>> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical monomial to linear monomial")
    }
}

fun CanonicalMonomial<Flt64, Int32>.toQuadraticMonomialOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticMonomial<Flt64>? {
    val comparator = symbolComparator ?: defaultSymbolComparator
    return toGenericCanonicalMonomial()
        .toGenericQuadraticMonomialOrNull(comparator)
        ?.toQuadraticMonomial()
}

fun CanonicalMonomial<Flt64, Int32>.toQuadraticMonomialRet(
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

fun LinearPolynomial<Flt64>.toQuadraticPolynomial(): QuadraticPolynomial<Flt64> {
    return toGenericLinearPolynomial()
        .toGenericQuadraticPolynomialFromLinearGeneric()
        .toQuadraticPolynomialFromGeneric()
}

fun LinearPolynomial<Flt64>.toCanonicalPolynomial(): CanonicalPolynomial<Flt64, Int32> {
    return toGenericLinearPolynomial()
        .toGenericCanonicalPolynomial()
        .toCanonicalPolynomialFromGeneric()
}

fun QuadraticPolynomial<Flt64>.toLinearPolynomialOrNull(): LinearPolynomial<Flt64>? {
    return toGenericQuadraticPolynomial()
        .toGenericLinearPolynomialOrNull()
        ?.toLinearPolynomialFromGeneric()
}

fun QuadraticPolynomial<Flt64>.toCanonicalPolynomial(
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalPolynomial<Flt64, Int32> {
    return toGenericQuadraticPolynomial()
        .toGenericCanonicalPolynomial(symbolComparator)
        .toCanonicalPolynomialFromGeneric()
}

fun CanonicalPolynomial<Flt64, Int32>.toLinearPolynomialOrNull(): LinearPolynomial<Flt64>? {
    return toGenericCanonicalPolynomial()
        .toGenericLinearPolynomialOrNull(
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
        ?.toLinearPolynomialFromGeneric()
}

fun CanonicalPolynomial<Flt64, Int32>.toLinearPolynomialRet(): Ret<LinearPolynomial<Flt64>> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical polynomial to linear polynomial")
    }
}

fun CanonicalPolynomial<Flt64, Int32>.toQuadraticPolynomialOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64>? {
    return toGenericCanonicalPolynomial()
        .toGenericQuadraticPolynomialOrNull(
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            symbolComparator = symbolComparator
        )
        ?.toQuadraticPolynomialFromGeneric()
}

fun CanonicalPolynomial<Flt64, Int32>.toQuadraticPolynomialRet(
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
    val movedLhs = lhs.toGenericCanonicalPolynomial()
        .subtractGenericCanonicalPolynomial(
            rhs = rhs.toGenericCanonicalPolynomial(),
            zero = Flt64.zero,
            isZero = { it == Flt64.zero },
            symbolComparator = if (combineTerms) symbolComparator else null
        )
        .toCanonicalPolynomialFromGeneric()
    val lhsToZeroRhs = CanonicalInequality(
        lhs = movedLhs,
        rhs = CanonicalPolynomial<Flt64, Int32>(constant = Flt64.zero),
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