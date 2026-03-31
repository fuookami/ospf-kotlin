package fuookami.ospf.kotlin.utils.math.symbol.generic

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol

fun GenericLinearPolynomial<Flt64>.toMatrixVector(order: List<Symbol>): DoubleArray {
    require(order.toSet().size == order.size) { "Symbol order contains duplicated symbols." }
    val c = DoubleArray(order.size)
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    for (monomial in monomials) {
        val i = indexOfSymbol[monomial.symbol]
            ?: throw IllegalArgumentException("Symbol ${monomial.symbol.name} not found in order.")
        c[i] += monomial.coefficient.toDouble()
    }
    return c
}

fun GenericQuadraticPolynomial<Flt64>.toMatrixPair(
    order: List<Symbol>
): Pair<Array<DoubleArray>, DoubleArray> {
    require(order.toSet().size == order.size) { "Symbol order contains duplicated symbols." }
    val n = order.size
    val q = Array(n) { DoubleArray(n) }
    val c = DoubleArray(n)
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    for (monomial in monomials) {
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
    return q to c
}





