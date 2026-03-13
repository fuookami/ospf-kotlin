package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.functional.*

data class QuadraticMatrixForm(
    val q: Array<DoubleArray>,
    val c: DoubleArray,
    val d: Flt64,
    val order: List<Symbol>
)

fun QuadraticPolynomial.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): QuadraticMatrixForm {
    require(order.toSet().size == order.size) { "Symbol order contains duplicated symbols." }

    val n = order.size
    val q = Array(n) { DoubleArray(n) }
    val c = DoubleArray(n)
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    val source = if (combineTerms) {
        combineTerms()
    } else {
        this
    }

    for (monomial in source.monomials) {
        if (monomial.isQuadratic) {
            val symbol2 = monomial.symbol2!!
            val i = indexOfSymbol[monomial.symbol1]
                ?: throw IllegalArgumentException("Symbol ${monomial.symbol1.name} not found in order.")
            val j = indexOfSymbol[symbol2]
                ?: throw IllegalArgumentException("Symbol ${symbol2.name} not found in order.")
            val coefficient = monomial.coefficient.toDouble()
            if (i == j) {
                q[i][j] += coefficient
            } else {
                val half = coefficient / 2.0
                q[i][j] += half
                q[j][i] += half
            }
        } else {
            val i = indexOfSymbol[monomial.symbol1]
                ?: throw IllegalArgumentException("Symbol ${monomial.symbol1.name} not found in order.")
            c[i] += monomial.coefficient.toDouble()
        }
    }

    return QuadraticMatrixForm(
        q = q,
        c = c,
        d = source.constant,
        order = order
    )
}

fun CanonicalPolynomial.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticMatrixForm {
    val source = if (combineTerms) {
        this.combineTerms(symbolComparator)
    } else {
        this
    }
    return when (val quadratic = source.toQuadraticPolynomialRet(symbolComparator)) {
        is Ok -> {
            quadratic.value.toMatrixForm(order = order, combineTerms = false)
        }

        is Failed -> {
            throw IllegalArgumentException(
                "Cannot convert canonical polynomial to quadratic matrix form: ${quadratic.error.message}"
            )
        }
    }
}
