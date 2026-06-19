@file:Suppress("unused")
package fuookami.ospf.kotlin.math.symbol.polynomial

import kotlin.jvm.JvmName
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.monomial.*

/**
 * 多项式快捷 DSL
 * Polynomial Quick DSL
 *
 * 提供多项式构建的泛型 DSL 扩展函数。
 * Provides generic DSL extension functions for polynomial construction.
 */

// ========== Linear polynomial construction ==========

/**
 * 从可变线性多项式构造不可变线性多项式
 * Constructs an immutable linear polynomial from a mutable one
 *
 * @param poly 可变线性多项式 / Mutable linear polynomial
 * @return 不可变线性多项式 / Immutable linear polynomial
 */
@JvmName("quickLinearPolynomialFromMutable")
fun <T : NumberField<T>> LinearPolynomial(poly: MutableLinearPolynomial<T>): LinearPolynomial<T> {
    return poly.toLinearPolynomial()
}

// ========== Linear aggregation: sum ==========

/**
 * 将线性单项式列表求和为线性多项式
 * Sums linear monomials into a linear polynomial
 *
 * @param monomials 线性单项式可迭代集合 / Iterable of linear monomials
 * @return 求和后的线性多项式 / Resulting linear polynomial
 */
@JvmName("sumLinearMonomials")
fun <T : Ring<T>> sum(monomials: Iterable<LinearMonomial<T>>): LinearPolynomial<T> {
    return LinearPolynomial(monomials.toList(), zeroOf(monomials.firstOrNull()?.coefficient ?: error("empty monomials")))
}

/**
 * 将多个线性多项式求和
 * Sums multiple linear polynomials
 *
 * @param polynomials 线性多项式可迭代集合 / Iterable of linear polynomials
 * @return 合并后的线性多项式 / Combined linear polynomial
 */
@JvmName("sumLinearPolynomials")
fun <T : Ring<T>> sum(polynomials: Iterable<LinearPolynomial<T>>): LinearPolynomial<T> {
    val monomials = ArrayList<LinearMonomial<T>>()
    var constant: T? = null
    for (polynomial in polynomials) {
        monomials.addAll(polynomial.monomials)
        constant = if (constant == null) polynomial.constant else constant + polynomial.constant
    }
    return LinearPolynomial(monomials, constant ?: error("empty polynomials"))
}

/**
 * 对元素集合应用选择器后求和为线性多项式
 * Sums elements after applying a selector to produce a linear polynomial
 *
 * @param elements 元素可迭代集合 / Iterable of elements
 * @param selector 从元素提取线性单项式的函数 / Function to extract a linear monomial from an element
 * @return 求和后的线性多项式 / Resulting linear polynomial
 */
fun <T : Ring<T>, E> sum(
    elements: Iterable<E>,
    selector: (E) -> LinearMonomial<T>
): LinearPolynomial<T> {
    val monomials = ArrayList<LinearMonomial<T>>()
    var constant: T? = null
    for (element in elements) {
        val monomial = selector(element)
        monomials.add(monomial)
        if (constant == null) constant = zeroOf(monomial.coefficient)
    }
    return LinearPolynomial(monomials, constant ?: error("empty elements"))
}

/**
 * 对元素集合应用选择器提取多项式后求和
 * Sums elements by extracting polynomials via a selector
 *
 * @param elements 元素可迭代集合 / Iterable of elements
 * @param selector 从元素提取线性多项式的函数 / Function to extract a linear polynomial from an element
 * @return 合并后的线性多项式 / Combined linear polynomial
 */
fun <T : Ring<T>, E> sumPolynomials(
    elements: Iterable<E>,
    selector: (E) -> LinearPolynomial<T>
): LinearPolynomial<T> {
    val polynomials = ArrayList<LinearPolynomial<T>>()
    for (element in elements) {
        polynomials.add(selector(element))
    }
    return sum(polynomials)
}

/**
 * 对元素集合应用选择器展平后求和为线性多项式
 * Flat-maps elements via a selector and sums the resulting monomials
 *
 * @param elements 元素可迭代集合 / Iterable of elements
 * @param selector 从元素提取线性单项式可迭代集合的函数 / Function returning an iterable of linear monomials per element
 * @return 求和后的线性多项式 / Resulting linear polynomial
 */
fun <T : Ring<T>, E> flatSum(
    elements: Iterable<E>,
    selector: (E) -> Iterable<LinearMonomial<T>>
): LinearPolynomial<T> {
    return sum(elements.flatMap(selector))
}

/**
 * 对元素集合应用可空转换后求和，过滤 null 结果
 * Sums elements after applying a nullable transform, filtering out nulls
 *
 * @param items 元素可迭代集合 / Iterable of elements
 * @param transform 可空转换函数 / Nullable transform function
 * @return 求和后的线性多项式结果 / Resulting linear polynomial result
 */
@JvmName("quickSumByNullableMonomial")
fun <T : Ring<T>, E> sum(
    items: Iterable<E>,
    transform: (E) -> LinearMonomial<T>?
): Ret<LinearPolynomial<T>> {
    return sumSafe(items, transform)
}

/**
 * 对元素集合应用可空转换后安全求和，过滤 null 结果
 * Safely sums elements after applying a nullable transform, filtering out nulls
 *
 * @param items 元素可迭代集合 / Iterable of elements
 * @param transform 可空转换函数 / Nullable transform function
 * @return 求和后的线性多项式结果 / Resulting linear polynomial result
 */
fun <T : Ring<T>, E> sumSafe(
    items: Iterable<E>,
    transform: (E) -> LinearMonomial<T>?
): Ret<LinearPolynomial<T>> {
    val list = items.mapNotNull(transform)
    if (list.isEmpty()) {
        return Failed(
            ErrorCode.DataEmpty,
            "空线性单项式列表，无法推断零值。 / Empty linear monomial list; cannot infer zero value."
        )
    }
    return Ok(sum(list))
}

/**
 * 对元素集合应用可空转换后求和，空结果返回 null
 * Sums elements after applying a nullable transform, returning null for empty results
 *
 * @param items 元素可迭代集合 / Iterable of elements
 * @param transform 可空转换函数 / Nullable transform function
 * @return 求和后的线性多项式或 null / Resulting linear polynomial, or null
 */
fun <T : Ring<T>, E> sumOrNull(
    items: Iterable<E>,
    transform: (E) -> LinearMonomial<T>?
): LinearPolynomial<T>? {
    val list = items.mapNotNull(transform)
    return if (list.isEmpty()) {
        null
    } else {
        sum(list)
    }
}

/**
 * 对元素集合应用可空展平转换后求和，过滤 null 结果
 * Flat-maps elements via a nullable transform and sums the non-null monomials
 *
 * @param list 元素可迭代集合 / Iterable of elements
 * @param transform 返回可空单项式可迭代集合的转换函数 / Transform returning iterable of nullable monomials
 * @return 求和后的线性多项式结果 / Resulting linear polynomial result
 */
@JvmName("quickFlatSumNullable")
fun <T : Ring<T>, E> flatSum(
    list: Iterable<E>,
    transform: (E) -> Iterable<LinearMonomial<T>?>
): Ret<LinearPolynomial<T>> {
    return flatSumSafe(list, transform)
}

/**
 * 对元素集合应用可空展平转换后安全求和，过滤 null 结果
 * Safely flat-maps elements via a nullable transform and sums the non-null monomials
 *
 * @param list 元素可迭代集合 / Iterable of elements
 * @param transform 返回可空单项式可迭代集合的转换函数 / Transform returning iterable of nullable monomials
 * @return 求和后的线性多项式结果 / Resulting linear polynomial result
 */
fun <T : Ring<T>, E> flatSumSafe(
    list: Iterable<E>,
    transform: (E) -> Iterable<LinearMonomial<T>?>
): Ret<LinearPolynomial<T>> {
    val monomials = mutableListOf<LinearMonomial<T>>()
    for (e in list) {
        monomials += transform(e).filterNotNull()
    }
    if (monomials.isEmpty()) {
        return Failed(
            ErrorCode.DataEmpty,
            "空线性单项式列表，无法推断零值。 / Empty linear monomial list; cannot infer zero value."
        )
    }
    return Ok(sum(monomials))
}

/**
 * 对元素集合应用可空展平转换后求和，空结果返回 null
 * Flat-maps elements via a nullable transform and returns null for empty results
 *
 * @param list 元素可迭代集合 / Iterable of elements
 * @param transform 返回可空单项式可迭代集合的转换函数 / Transform returning iterable of nullable monomials
 * @return 求和后的线性多项式或 null / Resulting linear polynomial, or null
 */
fun <T : Ring<T>, E> flatSumOrNull(
    list: Iterable<E>,
    transform: (E) -> Iterable<LinearMonomial<T>?>
): LinearPolynomial<T>? {
    val monomials = mutableListOf<LinearMonomial<T>>()
    for (e in list) {
        monomials += transform(e).filterNotNull()
    }
    return if (monomials.isEmpty()) {
        null
    } else {
        sum(monomials)
    }
}

// ========== Quadratic aggregation: qsum ==========

/**
 * 将二次单项式列表求和为二次多项式
 * Sums quadratic monomials into a quadratic polynomial
 *
 * @param monomials 二次单项式可迭代集合 / Iterable of quadratic monomials
 * @return 求和后的二次多项式 / Resulting quadratic polynomial
 */
@JvmName("sumQuadraticMonomials")
fun <T : Ring<T>> qsum(monomials: Iterable<QuadraticMonomial<T>>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials.toList(), zeroOf(monomials.firstOrNull()?.coefficient ?: error("empty monomials")))
}

/**
 * 将多个二次多项式求和
 * Sums multiple quadratic polynomials
 *
 * @param polynomials 二次多项式可迭代集合 / Iterable of quadratic polynomials
 * @return 合并后的二次多项式 / Combined quadratic polynomial
 */
@JvmName("sumQuadraticPolynomials")
fun <T : Ring<T>> qsum(polynomials: Iterable<QuadraticPolynomial<T>>): QuadraticPolynomial<T> {
    val monomials = ArrayList<QuadraticMonomial<T>>()
    var constant: T? = null
    for (polynomial in polynomials) {
        monomials.addAll(polynomial.monomials)
        constant = if (constant == null) polynomial.constant else constant + polynomial.constant
    }
    return QuadraticPolynomial(monomials, constant ?: error("empty polynomials"))
}

/**
 * 对元素集合应用选择器后求和为二次多项式
 * Sums elements after applying a selector to produce a quadratic polynomial
 *
 * @param elements 元素可迭代集合 / Iterable of elements
 * @param selector 从元素提取二次单项式的函数 / Function to extract a quadratic monomial from an element
 * @return 求和后的二次多项式 / Resulting quadratic polynomial
 */
fun <T : Ring<T>, E> qsum(
    elements: Iterable<E>,
    selector: (E) -> QuadraticMonomial<T>
): QuadraticPolynomial<T> {
    val monomials = ArrayList<QuadraticMonomial<T>>()
    var constant: T? = null
    for (element in elements) {
        val monomial = selector(element)
        monomials.add(monomial)
        if (constant == null) constant = zeroOf(monomial.coefficient)
    }
    return QuadraticPolynomial(monomials, constant ?: error("empty elements"))
}

/**
 * 对元素集合应用选择器提取二次多项式后求和
 * Sums elements by extracting quadratic polynomials via a selector
 *
 * @param elements 元素可迭代集合 / Iterable of elements
 * @param selector 从元素提取二次多项式的函数 / Function to extract a quadratic polynomial from an element
 * @return 合并后的二次多项式 / Combined quadratic polynomial
 */
fun <T : Ring<T>, E> qsumPolynomials(
    elements: Iterable<E>,
    selector: (E) -> QuadraticPolynomial<T>
): QuadraticPolynomial<T> {
    val polynomials = ArrayList<QuadraticPolynomial<T>>()
    for (element in elements) {
        polynomials.add(selector(element))
    }
    return qsum(polynomials)
}

/**
 * 对元素集合应用可空展平转换后求和为二次多项式，过滤 null 结果
 * Flat-maps elements via a nullable transform and sums the non-null quadratic monomials
 *
 * @param list 元素可迭代集合 / Iterable of elements
 * @param transform 返回可空二次单项式可迭代集合的转换函数 / Transform returning iterable of nullable quadratic monomials
 * @return 求和后的二次多项式结果 / Resulting quadratic polynomial result
 */
fun <T : Ring<T>, E> flatQSum(
    list: Iterable<E>,
    transform: (E) -> Iterable<QuadraticMonomial<T>?>
): Ret<QuadraticPolynomial<T>> {
    return flatQSumSafe(list, transform)
}

/**
 * 对元素集合应用可空展平转换后安全求和为二次多项式，过滤 null 结果
 * Safely flat-maps elements via a nullable transform and sums the non-null quadratic monomials
 *
 * @param list 元素可迭代集合 / Iterable of elements
 * @param transform 返回可空二次单项式可迭代集合的转换函数 / Transform returning iterable of nullable quadratic monomials
 * @return 求和后的二次多项式结果 / Resulting quadratic polynomial result
 */
fun <T : Ring<T>, E> flatQSumSafe(
    list: Iterable<E>,
    transform: (E) -> Iterable<QuadraticMonomial<T>?>
): Ret<QuadraticPolynomial<T>> {
    val monomials = mutableListOf<QuadraticMonomial<T>>()
    for (e in list) {
        monomials += transform(e).filterNotNull()
    }
    if (monomials.isEmpty()) {
        return Failed(
            ErrorCode.DataEmpty,
            "空二次单项式列表，无法推断零值。 / Empty quadratic monomial list; cannot infer zero value."
        )
    }
    return Ok(qsum(monomials))
}

/**
 * 对元素集合应用可空展平转换后求和为二次多项式，空结果返回 null
 * Flat-maps elements via a nullable transform and returns null for empty quadratic results
 *
 * @param list 元素可迭代集合 / Iterable of elements
 * @param transform 返回可空二次单项式可迭代集合的转换函数 / Transform returning iterable of nullable quadratic monomials
 * @return 求和后的二次多项式或 null / Resulting quadratic polynomial, or null
 */
fun <T : Ring<T>, E> flatQSumOrNull(
    list: Iterable<E>,
    transform: (E) -> Iterable<QuadraticMonomial<T>?>
): QuadraticPolynomial<T>? {
    val monomials = mutableListOf<QuadraticMonomial<T>>()
    for (e in list) {
        monomials += transform(e).filterNotNull()
    }
    return if (monomials.isEmpty()) {
        null
    } else {
        qsum(monomials)
    }
}

/**
 * 对元素集合应用可空转换后求和为二次多项式，过滤 null 结果
 * Sums elements after applying a nullable transform for quadratic monomials, filtering out nulls
 *
 * @param items 元素可迭代集合 / Iterable of elements
 * @param transform 可空转换函数 / Nullable transform function
 * @return 求和后的二次多项式结果 / Resulting quadratic polynomial result
 */
@JvmName("quickQSumByNullableMonomial")
fun <T : Ring<T>, E> qsum(
    items: Iterable<E>,
    transform: (E) -> QuadraticMonomial<T>?
): Ret<QuadraticPolynomial<T>> {
    return qsumSafe(items, transform)
}

/**
 * 对元素集合应用可空转换后安全求和为二次多项式，过滤 null 结果
 * Safely sums elements after applying a nullable transform for quadratic monomials, filtering out nulls
 *
 * @param items 元素可迭代集合 / Iterable of elements
 * @param transform 可空转换函数 / Nullable transform function
 * @return 求和后的二次多项式结果 / Resulting quadratic polynomial result
 */
fun <T : Ring<T>, E> qsumSafe(
    items: Iterable<E>,
    transform: (E) -> QuadraticMonomial<T>?
): Ret<QuadraticPolynomial<T>> {
    val list = items.mapNotNull(transform)
    if (list.isEmpty()) {
        return Failed(
            ErrorCode.DataEmpty,
            "空二次单项式列表，无法推断零值。 / Empty quadratic monomial list; cannot infer zero value."
        )
    }
    return Ok(qsum(list))
}

/**
 * 对元素集合应用可空转换后求和为二次多项式，空结果返回 null
 * Sums elements after applying a nullable transform for quadratic monomials, returning null for empty results
 *
 * @param items 元素可迭代集合 / Iterable of elements
 * @param transform 可空转换函数 / Nullable transform function
 * @return 求和后的二次多项式或 null / Resulting quadratic polynomial, or null
 */
fun <T : Ring<T>, E> qsumOrNull(
    items: Iterable<E>,
    transform: (E) -> QuadraticMonomial<T>?
): QuadraticPolynomial<T>? {
    val list = items.mapNotNull(transform)
    return if (list.isEmpty()) {
        null
    } else {
        qsum(list)
    }
}

// ========== Internal helper ==========

/**
 * 获取给定类型 T 的零值
 * Returns the zero value for the given type T
 *
 * @param value 用于推断类型的值 / Value used to infer the type
 * @return 该类型的零值 / Zero value of the type
 */
internal fun <T : Ring<T>> zeroOf(value: T): T = value - value
