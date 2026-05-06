package fuookami.ospf.kotlin.math.symbol.adapter.flt64

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.toTypedMatrixForm
import fuookami.ospf.kotlin.math.symbol.operation.typedLinearPolynomialFromMatrixForm
import fuookami.ospf.kotlin.math.symbol.operation.typedQuadraticPolynomialFromMatrixForm

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

fun LinearPolynomial<Flt64>.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): LinearMatrixForm {
    val form = toTypedMatrixForm(
        order = order,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )

    return LinearMatrixForm(
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    )
}

fun linearPolynomialFromMatrixForm(
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): LinearPolynomial<Flt64> {
    return typedLinearPolynomialFromMatrixForm(
        c = c.map { Flt64(it) },
        d = d,
        order = order,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
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
    val form = toTypedMatrixForm(
        order = order,
        zero = Flt64.zero,
        splitOffDiagonal = { coefficient ->
            val half = coefficient / Flt64.two
            half to half
        },
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )

    return QuadraticMatrixForm(
        q = form.q.map { row -> row.map { it.toDouble() }.toDoubleArray() }.toTypedArray(),
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    )
}

fun CanonicalPolynomial<Flt64>.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticMatrixForm {
    val form = toTypedMatrixForm(
        order = order,
        zero = Flt64.zero,
        splitOffDiagonal = { coefficient ->
            val half = coefficient / Flt64.two
            half to half
        },
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero },
        symbolComparator = symbolComparator
    )

    return QuadraticMatrixForm(
        q = form.q.map { row -> row.map { it.toDouble() }.toDoubleArray() }.toTypedArray(),
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    )
}

fun quadraticPolynomialFromMatrixForm(
    q: Array<DoubleArray>,
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): QuadraticPolynomial<Flt64> {
    return typedQuadraticPolynomialFromMatrixForm(
        q = q.map { row -> row.map { Flt64(it) } },
        c = c.map { Flt64(it) },
        d = d,
        order = order,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        mergeOffDiagonal = { lhs, rhs -> lhs + rhs }
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
