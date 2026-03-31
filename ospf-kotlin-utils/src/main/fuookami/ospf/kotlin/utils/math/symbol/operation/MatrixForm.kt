package fuookami.ospf.kotlin.utils.math.symbol.operation

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.generic.combineTerms as combineGenericLinearTerms
import fuookami.ospf.kotlin.utils.math.symbol.generic.combineTerms as combineGenericQuadraticTerms
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericLinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toGenericQuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.generic.toMatrixPair
import fuookami.ospf.kotlin.utils.math.symbol.generic.toMatrixVector
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

fun LinearPolynomial<Flt64>.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): LinearMatrixForm {
    validateOrder(order)

    val source = if (combineTerms) {
        toGenericLinearPolynomial().combineGenericLinearTerms(
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
    } else {
        toGenericLinearPolynomial()
    }
    val c = source.toMatrixVector(order)

    return LinearMatrixForm(
        c = c,
        d = constant,
        order = order
    )
}

fun linearPolynomialFromMatrixForm(
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): LinearPolynomial<Flt64> {
    validateOrder(order)
    validateLinearMatrixDimensions(c, order)

    val monomials = ArrayList<LinearMonomial<Flt64>>(order.size)
    for (i in order.indices) {
        if (c[i] != 0.0) {
            monomials.add(
                LinearMonomial<Flt64>(
                    coefficient = Flt64(c[i]),
                    symbol = order[i]
                )
            )
        }
    }
    return LinearPolynomial<Flt64>(
        monomials = monomials,
        constant = d
    )
}

fun linearPolynomialFromMatrixForm(form: LinearMatrixForm): LinearPolynomial<Flt64> {
    return linearPolynomialFromMatrixForm(
        c = form.c,
        d = form.d,
        order = form.order
    )
}

fun QuadraticPolynomial<Flt64>.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): QuadraticMatrixForm {
    validateOrder(order)

    val source = if (combineTerms) {
        toGenericQuadraticPolynomial().combineGenericQuadraticTerms(
            zero = Flt64.zero,
            isZero = { it == Flt64.zero }
        )
    } else {
        toGenericQuadraticPolynomial()
    }
    val (q, c) = source.toMatrixPair(order)

    return QuadraticMatrixForm(
        q = q,
        c = c,
        d = constant,
        order = order
    )
}

fun quadraticPolynomialFromMatrixForm(
    q: Array<DoubleArray>,
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): QuadraticPolynomial<Flt64> {
    validateOrder(order)
    validateQuadraticMatrixDimensions(q, c, order)

    val monomials = ArrayList<QuadraticMonomial<Flt64>>(order.size * (order.size + 1) / 2 + order.size)
    for (i in order.indices) {
        if (q[i][i] != 0.0) {
            monomials.add(
                QuadraticMonomial<Flt64>(
                    coefficient = Flt64(q[i][i]),
                    symbol1 = order[i],
                    symbol2 = order[i]
                )
            )
        }
        if (c[i] != 0.0) {
            monomials.add(
                QuadraticMonomial<Flt64>(
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
                    QuadraticMonomial<Flt64>(
                        coefficient = Flt64(coefficient),
                        symbol1 = order[i],
                        symbol2 = order[j]
                    )
                )
            }
        }
    }

    return QuadraticPolynomial<Flt64>(
        monomials = monomials,
        constant = d
    )
}

fun quadraticPolynomialFromMatrixForm(form: QuadraticMatrixForm): QuadraticPolynomial<Flt64> {
    return quadraticPolynomialFromMatrixForm(
        q = form.q,
        c = form.c,
        d = form.d,
        order = form.order
    )
}

fun CanonicalPolynomial<Flt64, Int32>.toMatrixForm(
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