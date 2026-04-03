package fuookami.ospf.kotlin.core.frontend.expression.adapter

import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.operation.QuadraticMatrixForm
import fuookami.ospf.kotlin.math.symbol.operation.toMatrixForm

fun AbstractQuadraticPolynomial<*>.toUtilsMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): QuadraticMatrixForm {
    return toUtilsPolynomial().toMatrixForm(order, combineTerms)
}
