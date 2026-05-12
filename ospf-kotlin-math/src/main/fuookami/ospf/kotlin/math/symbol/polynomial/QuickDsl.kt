/**
 * 澶氶」寮忓揩鎹?DSL
 * Polynomial Quick DSL
 *
 * 鎻愪緵澶氶」寮忔瀯寤虹殑娉涘瀷 DSL 鎵╁睍鍑芥暟銆? * Provides generic DSL extension functions for polynomial construction.
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.math.symbol.polynomial

import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial

import kotlin.jvm.JvmName

@JvmName("quickLinearPolynomialFromMutable")
fun <T : NumberField<T>> LinearPolynomial(poly: MutableLinearPolynomial<T>): LinearPolynomial<T> {
    return poly.toLinearPolynomial()
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
