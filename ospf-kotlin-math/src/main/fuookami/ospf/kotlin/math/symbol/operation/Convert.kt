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
import fuookami.ospf.kotlin.math.algebra.number.Flt64 as F64
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
// F64-specific convenience wrappers for functions requiring zero/isZero parameters
// ============================================================================

private fun LinearPolynomial<F64>.minus(rhs: LinearPolynomial<F64>): LinearPolynomial<F64> {
    return subtractLinear(
        rhs = rhs,
        zero = F64.zero,
        isZero = { it == F64.zero }
    )
}

fun CanonicalPolynomial<F64>.toQuadraticPolynomialOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticPolynomial<F64>? {
    return toQuadraticPolynomialOrNull(
        zero = F64.zero,
        isZero = { it == F64.zero },
        symbolComparator = symbolComparator
    )
}

/**
 * 尝试转换为线性类型时的错误原因
 * Error reasons when attempting conversion to linear types
 *
 * 用于标识类型转换失败的具体原因，以便调用方精确处理。
 * Used to identify the specific reason for type conversion failures,
 * allowing callers to handle them precisely.
 */
enum class TryToLinearError {
    /** 规范单项式次数大于 1，无法转换为线性单项式 / Canonical monomial degree > 1, cannot convert to linear monomial */
    CanonicalMonomialIsNotLinear,
    /** 二次单项式次数大于 1，无法转换为线性单项式 / Quadratic monomial degree > 1, cannot convert to linear monomial */
    QuadraticMonomialIsNotLinear,
    /** 规范多项式包含高次项，无法转换为线性多项式 / Canonical polynomial contains higher-degree terms */
    CanonicalPolynomialIsNotLinear,
    /** 二次多项式包含高次项，无法转换为线性多项式 / Quadratic polynomial contains higher-degree terms */
    QuadraticPolynomialIsNotLinear,
    /** 二次不等式包含高次项，无法转换为线性不等式 / Quadratic inequality contains higher-degree terms */
    QuadraticInequalityIsNotLinear,
    /** 规范不等式包含高次项，无法转换为线性不等式 / Canonical inequality contains higher-degree terms */
    CanonicalInequalityIsNotLinear
}

/**
 * 尝试转换为二次类型时的错误原因
 * Error reasons when attempting conversion to quadratic types
 */
enum class TryToQuadraticError {
    /** 规范单项式次数大于 2，无法转换为二次单项式 / Canonical monomial degree > 2 */
    CanonicalMonomialIsNotQuadratic,
    /** 规范多项式包含次数大于 2 的项，无法转换为二次多项式 / Canonical polynomial contains terms with degree > 2 */
    CanonicalPolynomialIsNotQuadratic,
    /** 规范不等式包含次数大于 2 的项，无法转换为二次不等式 / Canonical inequality contains terms with degree > 2 */
    CanonicalInequalityIsNotQuadratic
}

/**
 * 尝试转换为规范类型时的错误原因
 * Error reasons when attempting conversion to canonical types
 */
enum class TryToCanonicalError {
    /** 不支持的转换类型 / Unsupported conversion type */
    Unsupported
}

// ============================================================================
// Ret wrappers for nullable conversion functions
// ============================================================================

fun CanonicalMonomial<F64>.toLinearMonomialRet(): Ret<LinearMonomial<F64>> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert canonical monomial to linear monomial.",
            TryToLinearError.CanonicalMonomialIsNotLinear
        )
    }
}

fun CanonicalMonomial<F64>.toQuadraticMonomialRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticMonomial<F64>> {
    val quadraticMonomial = toQuadraticMonomialOrNull(symbolComparator)
    return if (quadraticMonomial != null) {
        Ok(quadraticMonomial)
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert canonical monomial to quadratic monomial.",
            TryToQuadraticError.CanonicalMonomialIsNotQuadratic
        )
    }
}

fun QuadraticMonomial<F64>.toLinearMonomialRet(): Ret<LinearMonomial<F64>> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert quadratic monomial to linear monomial.",
            TryToLinearError.QuadraticMonomialIsNotLinear
        )
    }
}

fun CanonicalPolynomial<F64>.toLinearPolynomialRet(): Ret<LinearPolynomial<F64>> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert canonical polynomial to linear polynomial.",
            TryToLinearError.CanonicalPolynomialIsNotLinear
        )
    }
}

fun CanonicalPolynomial<F64>.toQuadraticPolynomialRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticPolynomial<F64>> {
    val quadraticPolynomial = toQuadraticPolynomialOrNull(symbolComparator)
    return if (quadraticPolynomial != null) {
        Ok(quadraticPolynomial)
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert canonical polynomial to quadratic polynomial.",
            TryToQuadraticError.CanonicalPolynomialIsNotQuadratic
        )
    }
}

fun QuadraticPolynomial<F64>.toLinearPolynomialRet(): Ret<LinearPolynomial<F64>> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert quadratic polynomial to linear polynomial.",
            TryToLinearError.QuadraticPolynomialIsNotLinear
        )
    }
}

// ============================================================================
// Inequality conversions
// ============================================================================

fun LinearInequality<F64>.toQuadraticInequality(): QuadraticInequality {
    return QuadraticInequality(
        lhs = lhs.toQuadraticPolynomial(),
        rhs = rhs.toQuadraticPolynomial(),
        comparison = comparison
    )
}

fun LinearInequality<F64>.toCanonicalInequality(
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

fun QuadraticInequality.toLinearInequalityOrNull(): LinearInequality<F64>? {
    val linearLhs = lhs.toLinearPolynomialOrNull() ?: return null
    val linearRhs = rhs.toLinearPolynomialOrNull() ?: return null
    return LinearInequality(
        lhs = linearLhs,
        rhs = linearRhs,
        comparison = comparison
    )
}

fun QuadraticInequality.toLinearInequalityRet(): Ret<LinearInequality<F64>> {
    val linearInequality = toLinearInequalityOrNull()
    return if (linearInequality != null) {
        Ok(linearInequality)
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert quadratic inequality to linear inequality.",
            TryToLinearError.QuadraticInequalityIsNotLinear
        )
    }
}

fun CanonicalInequality.toLinearInequalityOrNull(): LinearInequality<F64>? {
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

fun CanonicalInequality.toLinearInequalityRet(): Ret<LinearInequality<F64>> {
    val linearInequality = toLinearInequalityOrNull()
    return if (linearInequality != null) {
        Ok(linearInequality)
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert canonical inequality to linear inequality.",
            TryToLinearError.CanonicalInequalityIsNotLinear
        )
    }
}

fun CanonicalInequality.toQuadraticInequalityRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticInequality> {
    val quadraticInequality = toQuadraticInequalityOrNull(symbolComparator)
    return if (quadraticInequality != null) {
        Ok(quadraticInequality)
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "Cannot convert canonical inequality to quadratic inequality.",
            TryToQuadraticError.CanonicalInequalityIsNotQuadratic
        )
    }
}

// ============================================================================
// Inequality operations
// ============================================================================

fun LinearInequality<F64>.moveAllToLhs(combineTerms: Boolean = true): LinearInequality<F64> {
    val lhsToZeroRhs = lhs.minus(rhs)
    val normalizedLhs = if (combineTerms) {
        lhsToZeroRhs.combineTerms()
    } else {
        lhsToZeroRhs
    }
    return LinearInequality(
        lhs = normalizedLhs,
        rhs = LinearPolynomial<F64>(constant = F64.zero),
        comparison = comparison
    )
}

fun LinearInequality<F64>.normalizeToLessEqualForm(combineTerms: Boolean = true): LinearInequality<F64> {
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
        zero = F64.zero,
        isZero = { it == F64.zero },
        symbolComparator = if (combineTerms) symbolComparator else null
    )
    val lhsToZeroRhs = CanonicalInequality(
        lhs = movedLhs,
        rhs = CanonicalPolynomial<F64>(constant = F64.zero),
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
