/**
 * 可变合并运算
 * Mutable Combine Operations
 *
 * 提供可变多项式的原地同类项合并操作。
 * 支持快速累积模式：先使甌+= 累积，最后一次性合并。
 * Provides in-place like-term combination operations for mutable polynomials.
 * Supports FastSum pattern: accumulate with +=, then combine once at the end.
*/
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.unaryMinus
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*

// ============================================================================
// Mutable Linear Polynomial Combine Operations
// ============================================================================

/**
 * 原地合并可变线性多项式中的同类项
 * Combine like terms in a mutable linear polynomial in-place.
 *
 * 这是 FastSum 模式：先用 += 累积，最后一次性合并同类项。
 * This is the FastSum pattern: accumulate with +=, then combine once at the end.
 *
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
*/
fun <T : NumberField<T>> MutableLinearPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
) {
    val combinedMonomials = _monomials.combineLinearMonomials(zero, isZero)
    _monomials.clear()
    _monomials.addAll(combinedMonomials)
}

/**
 * 累加多项式并合并同类项（一步操作）
 * Add a polynomial and combine terms in one operation.
 *
 * @param rhs 要累加的多项式 / Polynomial to add
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
*/
fun <T : NumberField<T>> MutableLinearPolynomial<T>.addAssignAndCombine(
    rhs: LinearPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
) {
    // Add rhs monomials / 添加右侧多项式的单项式
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
    // Combine / 合并同类项
    this.combineTerms(zero, isZero)
}

/**
 * 减去多项式并合并同类项（一步操作）
 * Subtract a polynomial and combine terms in one operation.
 *
 * @param rhs 要减去的多项式 / Polynomial to subtract
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
*/
fun <T : NumberField<T>> MutableLinearPolynomial<T>.minusAssignAndCombine(
    rhs: LinearPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
) {
    // Subtract rhs monomials / 减去右侧多项式的单项式
    _monomials.addAll(rhs.monomials.map { -it })
    _constant -= rhs.constant
    // Combine / 合并同类项
    this.combineTerms(zero, isZero)
}

// ============================================================================
// Mutable Quadratic Polynomial Combine Operations
// ============================================================================

/**
 * 原地合并可变二次多项式中的同类项
 * Combine like terms in a mutable quadratic polynomial in-place.
 *
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
*/
fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    val combinedMonomials = _monomials.combineQuadraticMonomials(zero, isZero, symbolComparator)
    _monomials.clear()
    _monomials.addAll(combinedMonomials)
}

/**
 * 累加多项式并合并同类项（一步操作）
 * Add a polynomial and combine terms in one operation.
 *
 * @param rhs 要累加的多项式 / Polynomial to add
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
*/
fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.addAssignAndCombine(
    rhs: QuadraticPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    // Add rhs monomials / 添加右侧多项式的单项式
    _monomials.addAll(rhs.monomials)
    _constant += rhs.constant
    // Combine / 合并同类项
    this.combineTerms(zero, isZero, symbolComparator)
}

/**
 * 减去多项式并合并同类项（一步操作）
 * Subtract a polynomial and combine terms in one operation.
 *
 * @param rhs 要减去的多项式 / Polynomial to subtract
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
*/
fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.minusAssignAndCombine(
    rhs: QuadraticPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    // Subtract rhs monomials / 减去右侧多项式的单项式
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
    // Combine / 合并同类项
    this.combineTerms(zero, isZero, symbolComparator)
}

// ============================================================================
// Mutable Canonical Polynomial Combine Operations
// ============================================================================

/**
 * 原地合并可变规范多项式中的同类项
 * Combine like terms in a mutable canonical polynomial in-place.
 *
 * 使用 PowerVectorKey 进行高效合并（参见 CanonicalOps.kt）。
 * Uses PowerVectorKey for optimal performance (see CanonicalOps.kt).
 *
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
*/
fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    // Use the same efficient algorithm as CanonicalOps.kt / 使用与 CanonicalOps.kt 相同的高效算法
    val combined = _monomials.combineCanonicalMonomials(zero, isZero, symbolComparator)
    _monomials.clear()
    _monomials.addAll(combined)
}

/**
 * 累加多项式并合并同类项（一步操作）
 * Add a polynomial and combine terms in one operation.
 *
 * @param rhs 要累加的多项式 / Polynomial to add
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
*/
fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.addAssignAndCombine(
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    // Add rhs monomials / 添加右侧多项式的单项式
    _monomials.addAll(rhs.monomials)
    _constant = _constant + rhs.constant
    // Combine / 合并同类项
    this.combineTerms(zero, isZero, symbolComparator)
}

/**
 * 减去多项式并合并同类项（一步操作）
 * Subtract a polynomial and combine terms in one operation.
 *
 * @param rhs 要减去的多项式 / Polynomial to subtract
 * @param zero 系数类型的零值 / Zero value for the coefficient type
 * @param isZero 判断值是否为零的谓词 / Predicate to check if a value is zero
 * @param symbolComparator 符号排序比较器（可选） / Comparator for symbol ordering (optional)
*/
fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.minusAssignAndCombine(
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
) {
    // Subtract rhs monomials / 减去右侧多项式的单项式
    _monomials.addAll(rhs.monomials.map { -it })
    _constant = _constant - rhs.constant
    // Combine / 合并同类项
    this.combineTerms(zero, isZero, symbolComparator)
}
