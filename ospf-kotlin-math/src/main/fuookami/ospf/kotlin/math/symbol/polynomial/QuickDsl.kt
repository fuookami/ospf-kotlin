/**
 * 多项式快捷 DSL
 * Polynomial Quick DSL
 *
 * 提供多项式构建的泛型 DSL 扩展函数。
 * Provides generic DSL extension functions for polynomial construction.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial

import kotlin.jvm.JvmName

// ========== Linear polynomial construction ==========

@JvmName("quickLinearPolynomialFromMutable")
fun <T : NumberField<T>> LinearPolynomial(poly: MutableLinearPolynomial<T>): LinearPolynomial<T> {
    return poly.toLinearPolynomial()
}

// ========== Linear aggregation: sum ==========

@JvmName("sumLinearMonomials")
fun <T : Ring<T>> sum(monomials: Iterable<LinearMonomial<T>>): LinearPolynomial<T> {
    return LinearPolynomial(monomials.toList(), zeroOf(monomials.firstOrNull()?.coefficient ?: error("empty monomials")))
}

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

fun <T : Ring<T>, E> flatSum(
    elements: Iterable<E>,
    selector: (E) -> Iterable<LinearMonomial<T>>
): LinearPolynomial<T> {
    return sum(elements.flatMap(selector))
}

@JvmName("quickSumByNullableMonomial")
fun <T : Ring<T>, E> sum(
    items: Iterable<E>,
    transform: (E) -> LinearMonomial<T>?
): LinearPolynomial<T> {
    val list = items.mapNotNull(transform)
    if (list.isEmpty()) {
        throw IllegalArgumentException("sum(items, transform): empty monomial list")
    }
    return sum(list)
}

@JvmName("quickFlatSumNullable")
fun <T : Ring<T>, E> flatSum(
    list: Iterable<E>,
    transform: (E) -> Iterable<LinearMonomial<T>?>
): LinearPolynomial<T> {
    val monomials = mutableListOf<LinearMonomial<T>>()
    for (e in list) {
        monomials += transform(e).filterNotNull()
    }
    if (monomials.isEmpty()) {
        throw IllegalArgumentException("flatSum(list, transform): empty monomial list")
    }
    return sum(monomials)
}

// ========== Quadratic aggregation: qsum ==========

@JvmName("sumQuadraticMonomials")
fun <T : Ring<T>> qsum(monomials: Iterable<QuadraticMonomial<T>>): QuadraticPolynomial<T> {
    return QuadraticPolynomial(monomials.toList(), zeroOf(monomials.firstOrNull()?.coefficient ?: error("empty monomials")))
}

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

fun <T : Ring<T>, E> flatQSum(
    list: Iterable<E>,
    transform: (E) -> Iterable<QuadraticMonomial<T>?>
): QuadraticPolynomial<T> {
    val monomials = mutableListOf<QuadraticMonomial<T>>()
    for (e in list) {
        monomials += transform(e).filterNotNull()
    }
    if (monomials.isEmpty()) {
        throw IllegalArgumentException("flatQSum(list, transform): empty monomial list")
    }
    return qsum(monomials)
}

@JvmName("quickQSumByNullableMonomial")
fun <T : Ring<T>, E> qsum(
    items: Iterable<E>,
    transform: (E) -> QuadraticMonomial<T>?
): QuadraticPolynomial<T> {
    val list = items.mapNotNull(transform)
    if (list.isEmpty()) {
        throw IllegalArgumentException("qsum(items, transform): empty monomial list")
    }
    return qsum(list)
}

// ========== Internal helper ==========

internal fun <T : Ring<T>> zeroOf(value: T): T = value - value