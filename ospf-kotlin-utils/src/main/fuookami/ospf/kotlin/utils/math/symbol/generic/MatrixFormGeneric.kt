package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.math.symbol.Symbol

data class GenericLinearMatrixForm<T>(
    val c: List<T>,
    val d: T,
    val order: List<Symbol>
) where T : Ring<T>

data class GenericQuadraticMatrixForm<T>(
    val q: List<List<T>>,
    val c: List<T>,
    val d: T,
    val order: List<Symbol>
) where T : Ring<T>

fun <T> GenericLinearPolynomial<T>.toGenericMatrixForm(
    order: List<Symbol>,
    zero: T
): GenericLinearMatrixForm<T> where T : Ring<T> {
    require(order.toSet().size == order.size) { "Symbol order contains duplicated symbols." }
    val c = MutableList(order.size) { zero }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    for (monomial in monomials) {
        val i = indexOfSymbol[monomial.symbol]
            ?: throw IllegalArgumentException("Symbol ${monomial.symbol.name} not found in order.")
        c[i] = c[i] + monomial.coefficient
    }
    return GenericLinearMatrixForm(
        c = c,
        d = constant,
        order = order
    )
}

fun <T> GenericQuadraticPolynomial<T>.toGenericMatrixForm(
    order: List<Symbol>,
    zero: T,
    splitOffDiagonal: (T) -> Pair<T, T>
): GenericQuadraticMatrixForm<T> where T : Ring<T> {
    require(order.toSet().size == order.size) { "Symbol order contains duplicated symbols." }
    val n = order.size
    val q = MutableList(n) { MutableList(n) { zero } }
    val c = MutableList(n) { zero }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    for (monomial in monomials) {
        if (monomial.isQuadratic) {
            val symbol2 = monomial.symbol2!!
            val i = indexOfSymbol[monomial.symbol1]
                ?: throw IllegalArgumentException("Symbol ${monomial.symbol1.name} not found in order.")
            val j = indexOfSymbol[symbol2]
                ?: throw IllegalArgumentException("Symbol ${symbol2.name} not found in order.")
            if (i == j) {
                q[i][j] = q[i][j] + monomial.coefficient
            } else {
                val (left, right) = splitOffDiagonal(monomial.coefficient)
                q[i][j] = q[i][j] + left
                q[j][i] = q[j][i] + right
            }
        } else {
            val i = indexOfSymbol[monomial.symbol1]
                ?: throw IllegalArgumentException("Symbol ${monomial.symbol1.name} not found in order.")
            c[i] = c[i] + monomial.coefficient
        }
    }
    return GenericQuadraticMatrixForm(
        q = q,
        c = c,
        d = constant,
        order = order
    )
}

fun GenericLinearPolynomial<Flt64>.toMatrixVector(order: List<Symbol>): DoubleArray {
    return toGenericMatrixForm(
        order = order,
        zero = Flt64.zero
    ).c.map { it.toDouble() }.toDoubleArray()
}

fun GenericQuadraticPolynomial<Flt64>.toMatrixPair(
    order: List<Symbol>
): Pair<Array<DoubleArray>, DoubleArray> {
    val form = toGenericMatrixForm(
        order = order,
        zero = Flt64.zero,
        splitOffDiagonal = { coefficient ->
            val half = coefficient / Flt64.two
            half to half
        }
    )
    return form.q.map { row -> row.map { it.toDouble() }.toDoubleArray() }.toTypedArray() to
            form.c.map { it.toDouble() }.toDoubleArray()
}





