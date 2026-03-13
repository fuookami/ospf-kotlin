package fuookami.ospf.kotlin.core.frontend.expression.adapter

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractLinearPolynomial as CoreAbstractLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractQuadraticPolynomial as CoreAbstractQuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial as CoreLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.QuadraticPolynomial as CoreQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial

fun CoreAbstractLinearPolynomial<*>.toUtilsPolynomial(): UtilsLinearPolynomial {
    return UtilsLinearPolynomial(
        monomials = monomials.map { it.toUtilsMonomial() },
        constant = constant
    )
}

fun CoreAbstractQuadraticPolynomial<*>.toUtilsPolynomial(): UtilsQuadraticPolynomial {
    return UtilsQuadraticPolynomial(
        monomials = monomials.map { it.toUtilsMonomial() },
        constant = constant
    )
}

fun UtilsLinearPolynomial.toCorePolynomialRet(): Ret<CoreLinearPolynomial> {
    val coreMonomials = ArrayList<fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial>()
    for (monomial in monomials) {
        when (val result = monomial.toCoreMonomialRet()) {
            is Ok -> coreMonomials.add(result.value)
            is Failed -> return Failed(result.error)
        }
    }
    return Ok(
        CoreLinearPolynomial(
            monomials = coreMonomials,
            constant = constant
        )
    )
}

fun UtilsLinearPolynomial.toCorePolynomialOrNull(): CoreLinearPolynomial? {
    return when (val result = toCorePolynomialRet()) {
        is Ok -> result.value
        is Failed -> null
    }
}

fun UtilsQuadraticPolynomial.toCorePolynomialRet(): Ret<CoreQuadraticPolynomial> {
    val coreMonomials = ArrayList<fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomial>()
    for (monomial in monomials) {
        when (val result = monomial.toCoreMonomialRet()) {
            is Ok -> coreMonomials.add(result.value)
            is Failed -> return Failed(result.error)
        }
    }
    return Ok(
        CoreQuadraticPolynomial(
            monomials = coreMonomials,
            constant = constant
        )
    )
}

fun UtilsQuadraticPolynomial.toCorePolynomialOrNull(): CoreQuadraticPolynomial? {
    return when (val result = toCorePolynomialRet()) {
        is Ok -> result.value
        is Failed -> null
    }
}
