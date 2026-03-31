package fuookami.ospf.kotlin.utils.math.symbol.inequality

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial

data class LinearInequality(
    val lhs: LinearPolynomial<Flt64>,
    val rhs: LinearPolynomial<Flt64>,
    val comparison: Comparison
)

private fun LinearMonomial<Flt64>.asPolynomial(): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(this), Flt64.zero)
}

private fun Flt64.asLinearPolynomial(): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), this)
}

infix fun LinearPolynomial<Flt64>.lt(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.LT)
infix fun LinearPolynomial<Flt64>.le(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.LE)
infix fun LinearPolynomial<Flt64>.eq(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.EQ)
infix fun LinearPolynomial<Flt64>.ne(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.NE)
infix fun LinearPolynomial<Flt64>.ge(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.GE)
infix fun LinearPolynomial<Flt64>.gt(rhs: LinearPolynomial<Flt64>): LinearInequality = LinearInequality(this, rhs, Comparison.GT)

infix fun LinearMonomial<Flt64>.lt(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() lt rhs.asPolynomial()
infix fun LinearMonomial<Flt64>.le(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() le rhs.asPolynomial()
infix fun LinearMonomial<Flt64>.eq(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() eq rhs.asPolynomial()
infix fun LinearMonomial<Flt64>.ne(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() ne rhs.asPolynomial()
infix fun LinearMonomial<Flt64>.ge(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() ge rhs.asPolynomial()
infix fun LinearMonomial<Flt64>.gt(rhs: LinearMonomial<Flt64>): LinearInequality = asPolynomial() gt rhs.asPolynomial()

infix fun LinearMonomial<Flt64>.lt(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() lt rhs
infix fun LinearMonomial<Flt64>.le(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() le rhs
infix fun LinearMonomial<Flt64>.eq(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() eq rhs
infix fun LinearMonomial<Flt64>.ne(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() ne rhs
infix fun LinearMonomial<Flt64>.ge(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() ge rhs
infix fun LinearMonomial<Flt64>.gt(rhs: LinearPolynomial<Flt64>): LinearInequality = asPolynomial() gt rhs

infix fun LinearPolynomial<Flt64>.lt(rhs: LinearMonomial<Flt64>): LinearInequality = this lt rhs.asPolynomial()
infix fun LinearPolynomial<Flt64>.le(rhs: LinearMonomial<Flt64>): LinearInequality = this le rhs.asPolynomial()
infix fun LinearPolynomial<Flt64>.eq(rhs: LinearMonomial<Flt64>): LinearInequality = this eq rhs.asPolynomial()
infix fun LinearPolynomial<Flt64>.ne(rhs: LinearMonomial<Flt64>): LinearInequality = this ne rhs.asPolynomial()
infix fun LinearPolynomial<Flt64>.ge(rhs: LinearMonomial<Flt64>): LinearInequality = this ge rhs.asPolynomial()
infix fun LinearPolynomial<Flt64>.gt(rhs: LinearMonomial<Flt64>): LinearInequality = this gt rhs.asPolynomial()

infix fun LinearPolynomial<Flt64>.lt(rhs: Flt64): LinearInequality = this lt rhs.asLinearPolynomial()
infix fun LinearPolynomial<Flt64>.le(rhs: Flt64): LinearInequality = this le rhs.asLinearPolynomial()
infix fun LinearPolynomial<Flt64>.eq(rhs: Flt64): LinearInequality = this eq rhs.asLinearPolynomial()
infix fun LinearPolynomial<Flt64>.ne(rhs: Flt64): LinearInequality = this ne rhs.asLinearPolynomial()
infix fun LinearPolynomial<Flt64>.ge(rhs: Flt64): LinearInequality = this ge rhs.asLinearPolynomial()
infix fun LinearPolynomial<Flt64>.gt(rhs: Flt64): LinearInequality = this gt rhs.asLinearPolynomial()

infix fun Flt64.lt(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() lt rhs
infix fun Flt64.le(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() le rhs
infix fun Flt64.eq(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() eq rhs
infix fun Flt64.ne(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() ne rhs
infix fun Flt64.ge(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() ge rhs
infix fun Flt64.gt(rhs: LinearPolynomial<Flt64>): LinearInequality = asLinearPolynomial() gt rhs

