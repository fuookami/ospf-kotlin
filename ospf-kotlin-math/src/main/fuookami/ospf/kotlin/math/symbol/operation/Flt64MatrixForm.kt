package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial

data class Flt64LinearMatrixForm(
    val c: DoubleArray,
    val d: Flt64,
    val order: List<Symbol>
)

data class Flt64QuadraticMatrixForm(
    val q: Array<DoubleArray>,
    val c: DoubleArray,
    val d: Flt64,
    val order: List<Symbol>
)

fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlt64MatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): Flt64LinearMatrixForm {
    val form = toMatrixForm(
        order = order,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )

    return Flt64LinearMatrixForm(
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    )
}

fun flt64LinearPolynomialFromMatrixForm(
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return linearPolynomialFromMatrixForm(
        c = c.map { Flt64(it) },
        d = d,
        order = order,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun flt64LinearPolynomialFromMatrixForm(form: Flt64LinearMatrixForm): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return flt64LinearPolynomialFromMatrixForm(
        c = form.c,
        d = form.d,
        order = form.order
    )
}

fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlt64MatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): Flt64QuadraticMatrixForm {
    val form = toMatrixForm(
        order = order,
        zero = Flt64.zero,
        splitOffDiagonal = { coefficient ->
            val half = coefficient / Flt64.two
            half to half
        },
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )

    return Flt64QuadraticMatrixForm(
        q = form.q.map { row -> row.map { it.toDouble() }.toDoubleArray() }.toTypedArray(),
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    )
}

fun CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlt64MatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): Flt64QuadraticMatrixForm {
    val form = toMatrixForm(
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

    return Flt64QuadraticMatrixForm(
        q = form.q.map { row -> row.map { it.toDouble() }.toDoubleArray() }.toTypedArray(),
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    )
}

fun flt64QuadraticPolynomialFromMatrixForm(
    q: Array<DoubleArray>,
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return quadraticPolynomialFromMatrixForm(
        q = q.map { row -> row.map { Flt64(it) } },
        c = c.map { Flt64(it) },
        d = d,
        order = order,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        mergeOffDiagonal = { lhs, rhs -> lhs + rhs }
    )
}

fun flt64QuadraticPolynomialFromMatrixForm(form: Flt64QuadraticMatrixForm): QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return flt64QuadraticPolynomialFromMatrixForm(
        q = form.q,
        c = form.c,
        d = form.d,
        order = form.order
    )
}
