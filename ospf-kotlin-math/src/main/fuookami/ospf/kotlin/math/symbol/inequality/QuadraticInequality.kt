/**
 * 二次不等式
 * Quadratic Inequality
 */
package fuookami.ospf.kotlin.math.symbol.inequality

import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial

data class QuadraticInequalityOf<T : Ring<T>>(
    val lhs: QuadraticPolynomial<T>,
    val rhs: QuadraticPolynomial<T>,
    val comparison: Comparison,
    val name: String = "",
    val displayName: String = ""
) {
    fun reverse(): QuadraticInequalityOf<T> {
        return QuadraticInequalityOf(
            lhs = rhs,
            rhs = lhs,
            comparison = comparison.reverse(),
            name = name,
            displayName = displayName
        )
    }
}

private fun <T : Ring<T>> QuadraticMonomial<T>.asPolynomial(): QuadraticPolynomial<T> {
    return QuadraticPolynomial(listOf(this), coefficient - coefficient)
}

private fun <T : Ring<T>> LinearMonomial<T>.asPolynomial(): QuadraticPolynomial<T> {
    return QuadraticPolynomial(
        listOf(QuadraticMonomial.linear(coefficient, symbol)),
        coefficient - coefficient
    )
}

private fun <T : Ring<T>> T.asQuadraticPolynomial(): QuadraticPolynomial<T> {
    return QuadraticPolynomial(emptyList(), this)
}

infix fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.LT)
infix fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.LE)
infix fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.EQ)
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.NE)
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.GE)
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = QuadraticInequalityOf(this, rhs, Comparison.GT)

infix fun <T : Ring<T>> QuadraticMonomial<T>.lt(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() lt rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticMonomial<T>.le(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() le rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticMonomial<T>.eq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() eq rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticMonomial<T>.ne(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() ne rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticMonomial<T>.ge(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() ge rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticMonomial<T>.gt(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = asPolynomial() gt rhs.asPolynomial()

infix fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs.toQuadraticPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this le rhs.toQuadraticPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this eq rhs.toQuadraticPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs.toQuadraticPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs.toQuadraticPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs.toQuadraticPolynomial()

infix fun <T : Ring<T>> LinearPolynomial<T>.lt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() lt rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.le(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() le rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.eq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() eq rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.ne(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() ne rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.ge(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() ge rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.gt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = toQuadraticPolynomial() gt rhs

infix fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this lt rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this le rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this eq rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ne rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ge rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this gt rhs.asPolynomial()

infix fun <T : Ring<T>> QuadraticMonomial<T>.lt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() lt rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.le(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() le rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.eq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() eq rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.ne(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() ne rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.ge(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() ge rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.gt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() gt rhs

infix fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this lt rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this le rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this eq rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this ne rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this ge rhs.asPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this gt rhs.asPolynomial()

infix fun <T : Ring<T>> LinearMonomial<T>.lt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() lt rhs
infix fun <T : Ring<T>> LinearMonomial<T>.le(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() le rhs
infix fun <T : Ring<T>> LinearMonomial<T>.eq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() eq rhs
infix fun <T : Ring<T>> LinearMonomial<T>.ne(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() ne rhs
infix fun <T : Ring<T>> LinearMonomial<T>.ge(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() ge rhs
infix fun <T : Ring<T>> LinearMonomial<T>.gt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asPolynomial() gt rhs

infix fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: T): QuadraticInequalityOf<T> = this lt rhs.asQuadraticPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: T): QuadraticInequalityOf<T> = this le rhs.asQuadraticPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: T): QuadraticInequalityOf<T> = this eq rhs.asQuadraticPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: T): QuadraticInequalityOf<T> = this ne rhs.asQuadraticPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: T): QuadraticInequalityOf<T> = this ge rhs.asQuadraticPolynomial()
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: T): QuadraticInequalityOf<T> = this gt rhs.asQuadraticPolynomial()

infix fun <T : Ring<T>> T.lt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() lt rhs
infix fun <T : Ring<T>> T.le(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() le rhs
infix fun <T : Ring<T>> T.eq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() eq rhs
infix fun <T : Ring<T>> T.ne(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() ne rhs
infix fun <T : Ring<T>> T.ge(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() ge rhs
infix fun <T : Ring<T>> T.gt(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = asQuadraticPolynomial() gt rhs

// ========== Alias names (leq/geq/neq/ls/gr) matching core convention ==========

infix fun <T : Ring<T>> QuadraticPolynomial<T>.leq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.geq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.neq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ls(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gr(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

infix fun <T : Ring<T>> QuadraticMonomial<T>.leq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this le rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.geq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ge rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.neq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ne rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.ls(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this lt rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.gr(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this gt rhs

infix fun <T : Ring<T>> QuadraticPolynomial<T>.leq(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.geq(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.neq(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ls(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gr(rhs: LinearPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

infix fun <T : Ring<T>> LinearPolynomial<T>.leq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.geq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.neq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.ls(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
infix fun <T : Ring<T>> LinearPolynomial<T>.gr(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

infix fun <T : Ring<T>> QuadraticPolynomial<T>.leq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this le rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.geq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ge rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.neq(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this ne rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ls(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this lt rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gr(rhs: QuadraticMonomial<T>): QuadraticInequalityOf<T> = this gt rhs

infix fun <T : Ring<T>> QuadraticMonomial<T>.leq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.geq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.neq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.ls(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
infix fun <T : Ring<T>> QuadraticMonomial<T>.gr(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

infix fun <T : Ring<T>> QuadraticPolynomial<T>.leq(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this le rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.geq(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this ge rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.neq(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this ne rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ls(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this lt rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gr(rhs: LinearMonomial<T>): QuadraticInequalityOf<T> = this gt rhs

infix fun <T : Ring<T>> LinearMonomial<T>.leq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
infix fun <T : Ring<T>> LinearMonomial<T>.geq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
infix fun <T : Ring<T>> LinearMonomial<T>.neq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
infix fun <T : Ring<T>> LinearMonomial<T>.ls(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
infix fun <T : Ring<T>> LinearMonomial<T>.gr(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

infix fun <T : Ring<T>> QuadraticPolynomial<T>.leq(rhs: T): QuadraticInequalityOf<T> = this le rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.geq(rhs: T): QuadraticInequalityOf<T> = this ge rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.neq(rhs: T): QuadraticInequalityOf<T> = this ne rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.ls(rhs: T): QuadraticInequalityOf<T> = this lt rhs
infix fun <T : Ring<T>> QuadraticPolynomial<T>.gr(rhs: T): QuadraticInequalityOf<T> = this gt rhs

infix fun <T : Ring<T>> T.leq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this le rhs
infix fun <T : Ring<T>> T.geq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ge rhs
infix fun <T : Ring<T>> T.neq(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this ne rhs
infix fun <T : Ring<T>> T.ls(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this lt rhs
infix fun <T : Ring<T>> T.gr(rhs: QuadraticPolynomial<T>): QuadraticInequalityOf<T> = this gt rhs

fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.LT, name, displayName)
fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.LE, name, displayName)
fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.EQ, name, displayName)
fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.NE, name, displayName)
fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.GE, name, displayName)
fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: QuadraticPolynomial<T>, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs, Comparison.GT, name, displayName)

fun <T : Ring<T>> QuadraticPolynomial<T>.lt(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.LT, name, displayName)
fun <T : Ring<T>> QuadraticPolynomial<T>.le(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.LE, name, displayName)
fun <T : Ring<T>> QuadraticPolynomial<T>.eq(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.EQ, name, displayName)
fun <T : Ring<T>> QuadraticPolynomial<T>.ne(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.NE, name, displayName)
fun <T : Ring<T>> QuadraticPolynomial<T>.ge(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.GE, name, displayName)
fun <T : Ring<T>> QuadraticPolynomial<T>.gt(rhs: T, name: String, displayName: String = ""): QuadraticInequalityOf<T> =
    QuadraticInequalityOf(this, rhs.asQuadraticPolynomial(), Comparison.GT, name, displayName)

