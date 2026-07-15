@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

/**
 * Flt64 转换快捷函数
 * Flt64 Conversion Convenience Functions
 *
 * 提供 Flt64 多项式和不等式的类型转换快捷函数。
 * 包括 Ret 包装的降阶转换和不等式规范化操作。
 * Provides Flt64 polynomial and inequality type conversion convenience functions.
 * Includes Ret-wrapped demotion conversions and inequality normalization operations.
*/

/** 计算两个 Flt64 线性多项式的差 / Subtract one Flt64 linear polynomial from another
 * @param rhs 被减的线性多项式 / The linear polynomial to subtract
 * @return 两个线性多项式的差 / The difference of the two linear polynomials
*/
private fun LinearPolynomial<Flt64>.minus(rhs: LinearPolynomial<Flt64>): LinearPolynomial<Flt64> {
    return subtractLinear(
        rhs = rhs,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

/**
 * 将 Flt64 规范多项式尝试转换为二次多项式
 * Try to convert a Flt64 canonical polynomial to a quadratic polynomial
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 二次多项式，若不可转换则返回 null / Quadratic polynomial, or null if not convertible
*/
fun CanonicalPolynomial<Flt64>.toQuadraticPolynomialOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64>? {
    return toQuadraticPolynomialOrNull(
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )
}

/**
 * 线性转换错误
 * Linear conversion error
 *
 * 表示降阶转换为线性多项式时的失败原因。
 * Represents the failure reason when demoting to a linear polynomial.
*/
enum class TryToLinearError {
    /** Canonical monomial is not linear / 规范单项式不是线性的 */
    CanonicalMonomialIsNotLinear,
    /** Quadratic monomial is not linear / 二次单项式不是线性的 */
    QuadraticMonomialIsNotLinear,
    /** Canonical polynomial is not linear / 规范多项式不是线性的 */
    CanonicalPolynomialIsNotLinear,
    /** Quadratic polynomial is not linear / 二次多项式不是线性的 */
    QuadraticPolynomialIsNotLinear,
    /** Quadratic inequality is not linear / 二次不等式不是线性的 */
    QuadraticInequalityIsNotLinear,
    /** Canonical inequality is not linear / 规范不等式不是线性的 */
    CanonicalInequalityIsNotLinear
}

/**
 * 二次转换错误
 * Quadratic conversion error
 *
 * 表示降阶转换为二次多项式时的失败原因。
 * Represents the failure reason when demoting to a quadratic polynomial.
*/
enum class TryToQuadraticError {
    /** Canonical monomial is not quadratic / 规范单项式不是二次的 */
    CanonicalMonomialIsNotQuadratic,
    /** Canonical polynomial is not quadratic / 规范多项式不是二次的 */
    CanonicalPolynomialIsNotQuadratic,
    /** Canonical inequality is not quadratic / 规范不等式不是二次的 */
    CanonicalInequalityIsNotQuadratic
}

/**
 * 规范转换错误
 * Canonical conversion error
 *
 * 表示转换为规范多项式时的失败原因。
 * Represents the failure reason when converting to a canonical polynomial.
*/
enum class TryToCanonicalError {
    /** Unsupported conversion / 不支持的转换 */
    Unsupported
}

/**
 * 将 Flt64 规范单项式转换为线性单项式（Ret 包装）
 * Convert a Flt64 canonical monomial to a linear monomial (Ret-wrapped)
 *
 * @return 包含线性单项式的 Ret 结果 / Ret result containing linear monomial
*/
fun CanonicalMonomial<Flt64>.toLinearMonomialRet(): Ret<LinearMonomial<Flt64>> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical monomial to linear monomial.", TryToLinearError.CanonicalMonomialIsNotLinear)
    }
}

/**
 * 将 Flt64 规范单项式转换为二次单项式（Ret 包装）
 * Convert a Flt64 canonical monomial to a quadratic monomial (Ret-wrapped)
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 包含二次单项式的 Ret 结果 / Ret result containing quadratic monomial
*/
fun CanonicalMonomial<Flt64>.toQuadraticMonomialRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticMonomial<Flt64>> {
    val quadraticMonomial = toQuadraticMonomialOrNull(symbolComparator)
    return if (quadraticMonomial != null) {
        Ok(quadraticMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical monomial to quadratic monomial.", TryToQuadraticError.CanonicalMonomialIsNotQuadratic)
    }
}

/**
 * 将 Flt64 二次单项式转换为线性单项式（Ret 包装）
 * Convert a Flt64 quadratic monomial to a linear monomial (Ret-wrapped)
 *
 * @return 包含线性单项式的 Ret 结果 / Ret result containing linear monomial
*/
fun QuadraticMonomial<Flt64>.toLinearMonomialRet(): Ret<LinearMonomial<Flt64>> {
    val linearMonomial = toLinearMonomialOrNull()
    return if (linearMonomial != null) {
        Ok(linearMonomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic monomial to linear monomial.", TryToLinearError.QuadraticMonomialIsNotLinear)
    }
}

/**
 * 将 Flt64 规范多项式转换为线性多项式（Ret 包装）
 * Convert a Flt64 canonical polynomial to a linear polynomial (Ret-wrapped)
 *
 * @return 包含线性多项式的 Ret 结果 / Ret result containing linear polynomial
*/
fun CanonicalPolynomial<Flt64>.toLinearPolynomialRet(): Ret<LinearPolynomial<Flt64>> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical polynomial to linear polynomial.", TryToLinearError.CanonicalPolynomialIsNotLinear)
    }
}

/**
 * 将 Flt64 规范多项式转换为二次多项式（Ret 包装）
 * Convert a Flt64 canonical polynomial to a quadratic polynomial (Ret-wrapped)
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 包含二次多项式的 Ret 结果 / Ret result containing quadratic polynomial
*/
fun CanonicalPolynomial<Flt64>.toQuadraticPolynomialRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticPolynomial<Flt64>> {
    val quadraticPolynomial = toQuadraticPolynomialOrNull(symbolComparator)
    return if (quadraticPolynomial != null) {
        Ok(quadraticPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical polynomial to quadratic polynomial.", TryToQuadraticError.CanonicalPolynomialIsNotQuadratic)
    }
}

/**
 * 将 Flt64 二次多项式转换为线性多项式（Ret 包装）
 * Convert a Flt64 quadratic polynomial to a linear polynomial (Ret-wrapped)
 *
 * @return 包含线性多项式的 Ret 结果 / Ret result containing linear polynomial
*/
fun QuadraticPolynomial<Flt64>.toLinearPolynomialRet(): Ret<LinearPolynomial<Flt64>> {
    val linearPolynomial = toLinearPolynomialOrNull()
    return if (linearPolynomial != null) {
        Ok(linearPolynomial)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic polynomial to linear polynomial.", TryToLinearError.QuadraticPolynomialIsNotLinear)
    }
}

/**
 * 将 Flt64 线性不等式升阶为二次不等式
 * Promote a Flt64 linear inequality to a quadratic inequality
 *
 * @return 二次不等式 / Quadratic inequality
*/
fun LinearInequality<Flt64>.toQuadraticInequality(): QuadraticInequalityOf<Flt64> {
    return QuadraticInequalityOf(
        lhs = lhs.toQuadraticPolynomial(),
        rhs = rhs.toQuadraticPolynomial(),
        comparison = comparison
    )
}

/**
 * 将 Flt64 线性不等式升阶为规范不等式
 * Promote a Flt64 linear inequality to a canonical inequality
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 规范不等式 / Canonical inequality
*/
fun LinearInequality<Flt64>.toCanonicalInequality(
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality<Flt64> {
    return CanonicalInequality<Flt64>(
        lhs = lhs.toCanonicalPolynomial().combineTerms(symbolComparator),
        rhs = rhs.toCanonicalPolynomial().combineTerms(symbolComparator),
        comparison = comparison
    )
}

/**
 * 将 Flt64 二次不等式升阶为规范不等式
 * Promote a Flt64 quadratic inequality to a canonical inequality
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 规范不等式 / Canonical inequality
*/
fun QuadraticInequalityOf<Flt64>.toCanonicalInequality(
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality<Flt64> {
    return CanonicalInequality<Flt64>(
        lhs = lhs.toCanonicalPolynomial(symbolComparator).combineTerms(symbolComparator),
        rhs = rhs.toCanonicalPolynomial(symbolComparator).combineTerms(symbolComparator),
        comparison = comparison
    )
}

/**
 * 将 Flt64 二次不等式尝试降阶为线性不等式
 * Try to demote a Flt64 quadratic inequality to a linear inequality
 *
 * @return 线性不等式，若不可转换则返回 null / Linear inequality, or null if not convertible
*/
fun QuadraticInequalityOf<Flt64>.toLinearInequalityOrNull(): LinearInequality<Flt64>? {
    val linearLhs = lhs.toLinearPolynomialOrNull() ?: return null
    val linearRhs = rhs.toLinearPolynomialOrNull() ?: return null
    return LinearInequality(lhs = linearLhs, rhs = linearRhs, comparison = comparison)
}

/**
 * 将 Flt64 二次不等式降阶为线性不等式（Ret 包装）
 * Demote a Flt64 quadratic inequality to a linear inequality (Ret-wrapped)
 *
 * @return 包含线性不等式的 Ret 结果 / Ret result containing linear inequality
*/
fun QuadraticInequalityOf<Flt64>.toLinearInequalityRet(): Ret<LinearInequality<Flt64>> {
    val linearInequality = toLinearInequalityOrNull()
    return if (linearInequality != null) {
        Ok(linearInequality)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert quadratic inequality to linear inequality.", TryToLinearError.QuadraticInequalityIsNotLinear)
    }
}

/**
 * 将 Flt64 规范不等式尝试降阶为线性不等式
 * Try to demote a Flt64 canonical inequality to a linear inequality
 *
 * @return 线性不等式，若不可转换则返回 null / Linear inequality, or null if not convertible
*/
fun CanonicalInequality<Flt64>.toLinearInequalityOrNull(): LinearInequality<Flt64>? {
    val linearLhs = lhs.toLinearPolynomialOrNull() ?: return null
    val linearRhs = rhs.toLinearPolynomialOrNull() ?: return null
    return LinearInequality(lhs = linearLhs, rhs = linearRhs, comparison = comparison)
}

/**
 * 将 Flt64 规范不等式尝试降阶为二次不等式
 * Try to demote a Flt64 canonical inequality to a quadratic inequality
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 二次不等式，若不可转换则返回 null / Quadratic inequality, or null if not convertible
*/
fun CanonicalInequality<Flt64>.toQuadraticInequalityOrNull(
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticInequalityOf<Flt64>? {
    val quadraticLhs = lhs.toQuadraticPolynomialOrNull(symbolComparator) ?: return null
    val quadraticRhs = rhs.toQuadraticPolynomialOrNull(symbolComparator) ?: return null
    return QuadraticInequalityOf(lhs = quadraticLhs, rhs = quadraticRhs, comparison = comparison)
}

/**
 * 将 Flt64 规范不等式降阶为线性不等式（Ret 包装）
 * Demote a Flt64 canonical inequality to a linear inequality (Ret-wrapped)
 *
 * @return 包含线性不等式的 Ret 结果 / Ret result containing linear inequality
*/
fun CanonicalInequality<Flt64>.toLinearInequalityRet(): Ret<LinearInequality<Flt64>> {
    val linearInequality = toLinearInequalityOrNull()
    return if (linearInequality != null) {
        Ok(linearInequality)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical inequality to linear inequality.", TryToLinearError.CanonicalInequalityIsNotLinear)
    }
}

/**
 * 将 Flt64 规范不等式降阶为二次不等式（Ret 包装）
 * Demote a Flt64 canonical inequality to a quadratic inequality (Ret-wrapped)
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 包含二次不等式的 Ret 结果 / Ret result containing quadratic inequality
*/
fun CanonicalInequality<Flt64>.toQuadraticInequalityRet(
    symbolComparator: java.util.Comparator<Symbol>? = null
): Ret<QuadraticInequalityOf<Flt64>> {
    val quadraticInequality = toQuadraticInequalityOrNull(symbolComparator)
    return if (quadraticInequality != null) {
        Ok(quadraticInequality)
    } else {
        Failed(ErrorCode.IllegalArgument, "Cannot convert canonical inequality to quadratic inequality.", TryToQuadraticError.CanonicalInequalityIsNotQuadratic)
    }
}

/**
 * 将 Flt64 线性不等式所有项移至左侧
 * Move all terms of a Flt64 linear inequality to the left-hand side
 *
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 左侧归一化的不等式 / Normalized inequality with all terms on LHS
*/
fun LinearInequality<Flt64>.moveAllToLhs(combineTerms: Boolean = true): LinearInequality<Flt64> {
    val lhsToZeroRhs = lhs.minus(rhs)
    val normalizedLhs = if (combineTerms) {
        lhsToZeroRhs.combineTerms()
    } else {
        lhsToZeroRhs
    }
    return LinearInequality(lhs = normalizedLhs, rhs = LinearPolynomial<Flt64>(constant = Flt64.zero), comparison = comparison)
}

/**
 * 将 Flt64 线性不等式规范化为小于等于形式
 * Normalize a Flt64 linear inequality to less-than-or-equal form
 *
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @return 规范化的不等式 / Normalized inequality
*/
fun LinearInequality<Flt64>.normalizeToLessEqualForm(combineTerms: Boolean = true): LinearInequality<Flt64> {
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

/**
 * 将 Flt64 规范不等式所有项移至左侧
 * Move all terms of a Flt64 canonical inequality to the left-hand side
 *
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 左侧归一化的不等式 / Normalized inequality with all terms on LHS
*/
fun CanonicalInequality<Flt64>.moveAllToLhs(
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality<Flt64> {
    val movedLhs = lhs.subtractCanonical(
        rhs = rhs,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        symbolComparator = if (combineTerms) symbolComparator else null
    )
    val lhsToZeroRhs = CanonicalInequality<Flt64>(lhs = movedLhs, rhs = CanonicalPolynomial<Flt64>(constant = Flt64.zero), comparison = comparison)
    return if (combineTerms) {
        lhsToZeroRhs.copy(lhs = lhsToZeroRhs.lhs.combineTerms(symbolComparator))
    } else {
        lhsToZeroRhs
    }
}

/**
 * 将 Flt64 规范不等式规范化为小于等于形式
 * Normalize a Flt64 canonical inequality to less-than-or-equal form
 *
 * @param combineTerms 是否合并同类项 / Whether to combine like terms
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return 规范化的不等式 / Normalized inequality
*/
fun CanonicalInequality<Flt64>.normalizeToLessEqualForm(
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): CanonicalInequality<Flt64> {
    if (comparison == Comparison.NE) {
        return this
    }
    val lessLikeInequality = if (comparison.isGreaterLike) {
        CanonicalInequality<Flt64>(lhs = rhs, rhs = lhs, comparison = comparison.reverse())
    } else {
        this
    }
    return lessLikeInequality.moveAllToLhs(combineTerms, symbolComparator)
}
