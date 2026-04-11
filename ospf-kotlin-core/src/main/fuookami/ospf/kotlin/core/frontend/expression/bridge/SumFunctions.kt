package fuookami.ospf.kotlin.core.frontend.expression.bridge

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit

// sum functions

@JvmName("sumLinearMonomials")
fun sum(monomials: Iterable<LinearMonomial<Flt64>>): LinearPolynomial<Flt64> {
    return LinearPolynomial(monomials = monomials.toList(), constant = Flt64.zero)
}

@JvmName("sumLinearPolynomials")
fun sum(polynomials: Iterable<LinearPolynomial<Flt64>>): LinearPolynomial<Flt64> {
    val monomials = ArrayList<LinearMonomial<Flt64>>()
    var constant = Flt64.zero
    for (polynomial in polynomials) {
        monomials.addAll(polynomial.monomials)
        constant += polynomial.constant
    }
    return LinearPolynomial(monomials = monomials, constant = constant)
}

@JvmName("sumMapMonomials")
fun <T> sum(
    objs: Iterable<T>,
    extractor: (T) -> LinearMonomial<Flt64>?
): LinearPolynomial<Flt64> {
    return sum(objs.mapNotNull(extractor))
}

@JvmName("sumMapMonomialLists")
fun <T> flatSum(
    objs: Iterable<T>,
    extractor: (T) -> Iterable<LinearMonomial<Flt64>?>
): LinearPolynomial<Flt64> {
    return sum(objs.flatMap(extractor).filterNotNull())
}

// quantity sum functions

@JvmName("sumQuantityLinearMonomials")
fun qtySum(monomials: Iterable<Quantity<LinearMonomial<Flt64>>>): Quantity<LinearPolynomial<Flt64>> {
    val quantityMonomials = monomials.toList()
    return if (quantityMonomials.isEmpty()) {
        Quantity(LinearPolynomial(constant = Flt64.zero), NoneUnit)
    } else {
        quantityMonomials.subList(1, quantityMonomials.size)
            .fold(Quantity(LinearPolynomial(monomials = listOf(quantityMonomials.first().value), constant = Flt64.zero), quantityMonomials.first().unit)) { acc, quantity ->
                acc + quantity
            }
    }
}

@JvmName("sumQuantityLinearPolynomials")
fun qtySum(polynomials: Iterable<Quantity<LinearPolynomial<Flt64>>>): Quantity<LinearPolynomial<Flt64>> {
    val quantityPolynomials = polynomials.toList()
    return if (quantityPolynomials.isEmpty()) {
        Quantity(LinearPolynomial(constant = Flt64.zero), NoneUnit)
    } else {
        quantityPolynomials.subList(1, quantityPolynomials.size)
            .fold(Quantity(quantityPolynomials.first().value, quantityPolynomials.first().unit)) { acc, quantity ->
                acc + quantity
            }
    }
}

@JvmName("sumMapQuantityMonomials")
fun <T> qtySum(
    objs: Iterable<T>,
    extractor: (T) -> Quantity<LinearMonomial<Flt64>>?
): Quantity<LinearPolynomial<Flt64>> {
    return qtySum(objs.mapNotNull(extractor))
}

@JvmName("sumMapQuantityMonomialLists")
fun <T> flatQtySum(
    objs: Iterable<T>,
    extractor: (T) -> Iterable<Quantity<LinearMonomial<Flt64>>?>
): Quantity<LinearPolynomial<Flt64>> {
    return qtySum(objs.flatMap(extractor).filterNotNull())
}
