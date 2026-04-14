package fuookami.ospf.kotlin.core.expression.adapter

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.expression.polynomial.AbstractLinearPolynomial as CoreAbstractLinearPolynomial
import fuookami.ospf.kotlin.core.expression.polynomial.AbstractQuadraticPolynomial as CoreAbstractQuadraticPolynomial
import fuookami.ospf.kotlin.core.expression.polynomial.LinearPolynomial as CoreLinearPolynomial
import fuookami.ospf.kotlin.core.expression.polynomial.QuadraticPolynomial as CoreQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial

fun CoreAbstractLinearPolynomial<*>.toUtilsPolynomial(): UtilsLinearPolynomial<Flt64> {
    return UtilsLinearPolynomial(
        monomials = monomials.map { it.toUtilsMonomial() },
        constant = constant
    )
}

fun CoreAbstractQuadraticPolynomial<*>.toUtilsPolynomial(): UtilsQuadraticPolynomial<Flt64> {
    return UtilsQuadraticPolynomial(
        monomials = monomials.map { it.toUtilsMonomial() },
        constant = constant
    )
}

fun UtilsLinearPolynomial<Flt64>.toCorePolynomialRet(): Ret<CoreLinearPolynomial> {
    val coreMonomials = ArrayList<fuookami.ospf.kotlin.core.expression.monomial.LinearMonomial>()
    for (monomial in monomials) {
        when (val result = monomial.toCoreMonomialRet()) {
            is Ok -> coreMonomials.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return Ok(
        CoreLinearPolynomial(
            monomials = coreMonomials,
            constant = constant
        )
    )
}

@Deprecated(
    message = "Use toCorePolynomialRet() to keep adapter failures explicit."
)
fun UtilsLinearPolynomial<Flt64>.toCorePolynomialOrNull(): CoreLinearPolynomial? {
    return when (val result = toCorePolynomialRet()) {
        is Ok -> result.value
        is Failed -> null
        is Fatal -> null
    }
}

fun UtilsQuadraticPolynomial<Flt64>.toCorePolynomialRet(): Ret<CoreQuadraticPolynomial> {
    val coreMonomials = ArrayList<fuookami.ospf.kotlin.core.expression.monomial.QuadraticMonomial>()
    for (monomial in monomials) {
        when (val result = monomial.toCoreMonomialRet()) {
            is Ok -> coreMonomials.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return Ok(
        CoreQuadraticPolynomial(
            monomials = coreMonomials,
            constant = constant
        )
    )
}

@Deprecated(
    message = "Use toCorePolynomialRet() to keep adapter failures explicit."
)
fun UtilsQuadraticPolynomial<Flt64>.toCorePolynomialOrNull(): CoreQuadraticPolynomial? {
    return when (val result = toCorePolynomialRet()) {
        is Ok -> result.value
        is Failed -> null
        is Fatal -> null
    }
}




