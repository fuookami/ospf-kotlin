package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial

data class LinearMatrixForm(
    val c: DoubleArray,
    val d: Flt64,
    val order: List<Symbol>
)

data class QuadraticMatrixForm(
    val q: Array<DoubleArray>,
    val c: DoubleArray,
    val d: Flt64,
    val order: List<Symbol>
)

private fun validateOrder(order: List<Symbol>) {
    require(order.toSet().size == order.size) { "Symbol order contains duplicated symbols." }
}

private fun validateLinearMatrixDimensions(
    c: DoubleArray,
    order: List<Symbol>
) {
    require(c.size == order.size) {
        "Linear matrix form dimension mismatch: c.size=${c.size}, order.size=${order.size}."
    }
}

private fun validateQuadraticMatrixDimensions(
    q: Array<DoubleArray>,
    c: DoubleArray,
    order: List<Symbol>
) {
    val n = order.size
    require(q.size == n) {
        "Quadratic matrix form dimension mismatch: q.size=${q.size}, order.size=$n."
    }
    require(q.all { it.size == n }) {
        "Quadratic matrix form requires square q with size order.size=$n."
    }
    require(c.size == n) {
        "Quadratic matrix form dimension mismatch: c.size=${c.size}, order.size=$n."
    }
}

fun LinearPolynomial.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): LinearMatrixForm {
    validateOrder(order)

    val n = order.size
    val c = DoubleArray(n)
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    val source = if (combineTerms) {
        combineTerms()
    } else {
        this
    }

    for (monomial in source.monomials) {
        val i = indexOfSymbol[monomial.symbol]
            ?: throw IllegalArgumentException("Symbol ${monomial.symbol.name} not found in order.")
        c[i] += monomial.coefficient.toDouble()
    }

    return LinearMatrixForm(
        c = c,
        d = source.constant,
        order = order
    )
}

fun linearPolynomialFromMatrixForm(
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): LinearPolynomial {
    validateOrder(order)
    validateLinearMatrixDimensions(c, order)

    val monomials = ArrayList<LinearMonomial>(order.size)
    for (i in order.indices) {
        if (c[i] != 0.0) {
            monomials.add(
                LinearMonomial(
                    coefficient = Flt64(c[i]),
                    symbol = order[i]
                )
            )
        }
    }
    return LinearPolynomial(
        monomials = monomials,
        constant = d
    )
}

fun linearPolynomialFromMatrixForm(form: LinearMatrixForm): LinearPolynomial {
    return linearPolynomialFromMatrixForm(
        c = form.c,
        d = form.d,
        order = form.order
    )
}

fun QuadraticPolynomial.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): QuadraticMatrixForm {
    validateOrder(order)

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

fun quadraticPolynomialFromMatrixForm(
    q: Array<DoubleArray>,
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): QuadraticPolynomial {
    validateOrder(order)
    validateQuadraticMatrixDimensions(q, c, order)

    val monomials = ArrayList<QuadraticMonomial>(order.size * (order.size + 1) / 2 + order.size)
    for (i in order.indices) {
        if (q[i][i] != 0.0) {
            monomials.add(
                QuadraticMonomial(
                    coefficient = Flt64(q[i][i]),
                    symbol1 = order[i],
                    symbol2 = order[i]
                )
            )
        }
        if (c[i] != 0.0) {
            monomials.add(
                QuadraticMonomial(
                    coefficient = Flt64(c[i]),
                    symbol1 = order[i],
                    symbol2 = null
                )
            )
        }
        for (j in (i + 1) until order.size) {
            val coefficient = q[i][j] + q[j][i]
            if (coefficient != 0.0) {
                monomials.add(
                    QuadraticMonomial(
                        coefficient = Flt64(coefficient),
                        symbol1 = order[i],
                        symbol2 = order[j]
                    )
                )
            }
        }
    }

    return QuadraticPolynomial(
        monomials = monomials,
        constant = d
    )
}

fun quadraticPolynomialFromMatrixForm(form: QuadraticMatrixForm): QuadraticPolynomial {
    return quadraticPolynomialFromMatrixForm(
        q = form.q,
        c = form.c,
        d = form.d,
        order = form.order
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

        is Fatal -> {
            throw IllegalArgumentException(
                "Cannot convert canonical polynomial to quadratic matrix form: ${quadratic.errors.joinToString { it.message }}"
            )
        }
    }
}
