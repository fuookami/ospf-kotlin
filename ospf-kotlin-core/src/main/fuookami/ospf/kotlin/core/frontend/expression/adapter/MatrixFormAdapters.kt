package fuookami.ospf.kotlin.core.frontend.expression.adapter

import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.operation.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*

fun AbstractQuadraticPolynomial<*>.toUtilsMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): QuadraticMatrixForm {
    return toUtilsPolynomial().toMatrixForm(order, combineTerms)
}
