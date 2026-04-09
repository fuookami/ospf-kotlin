package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.adapter.toUtilsPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractQuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.adapter.toUtilsMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial as FrontendLinearMonomial
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.UInt8
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms

// ========== LinearInequality normalize extension ==========

fun MathLinearInequality.normalize(): MathLinearInequality {
    val normalizedLhs = (lhs - rhs).combineTerms()
    return copy(
        lhs = normalizedLhs,
        rhs = UtilsLinearPolynomial(emptyList(), Flt64.zero)
    )
}

fun MathQuadraticInequality.normalize(): MathQuadraticInequality {
    val normalizedLhs = (lhs - rhs).combineTerms()
    return copy(
        lhs = normalizedLhs,
        rhs = UtilsQuadraticPolynomial(emptyList(), Flt64.zero)
    )
}

// ========== Linear polynomial DSL ==========

infix fun AbstractLinearPolynomial<*>.eq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.EQ)

infix fun AbstractLinearPolynomial<*>.le(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.LE)

infix fun AbstractLinearPolynomial<*>.ge(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.GE)

infix fun AbstractLinearPolynomial<*>.lt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.LT)

infix fun AbstractLinearPolynomial<*>.gt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.GT)

infix fun AbstractLinearPolynomial<*>.ne(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.NE)

// Linear polynomial vs Flt64
infix fun AbstractLinearPolynomial<*>.eq(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), rhs), Comparison.EQ)

infix fun AbstractLinearPolynomial<*>.le(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), rhs), Comparison.LE)

infix fun AbstractLinearPolynomial<*>.ge(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), rhs), Comparison.GE)

infix fun AbstractLinearPolynomial<*>.lt(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), rhs), Comparison.LT)

infix fun AbstractLinearPolynomial<*>.gt(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), rhs), Comparison.GT)

infix fun AbstractLinearPolynomial<*>.ne(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), rhs), Comparison.NE)

// Flt64 vs Linear polynomial
infix fun Flt64.eq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), this), rhs.toUtilsPolynomial(), Comparison.EQ)

infix fun Flt64.le(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), this), rhs.toUtilsPolynomial(), Comparison.LE)

infix fun Flt64.ge(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), this), rhs.toUtilsPolynomial(), Comparison.GE)

// Backward-compat aliases for frontend naming
infix fun AbstractLinearPolynomial<*>.leq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.leq(rhs: Flt64): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: Flt64): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ne rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: Flt64): MathLinearInequality = this ne rhs
infix fun Flt64.leq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this le rhs
infix fun Flt64.geq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ge rhs

// ========== Quadratic polynomial DSL ==========

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.EQ)

infix fun AbstractQuadraticPolynomial<*>.le(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.LE)

infix fun AbstractQuadraticPolynomial<*>.ge(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.GE)

infix fun AbstractQuadraticPolynomial<*>.lt(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.LT)

infix fun AbstractQuadraticPolynomial<*>.gt(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.GT)

infix fun AbstractQuadraticPolynomial<*>.ne(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial(), Comparison.NE)

// Quadratic polynomial vs Flt64
infix fun AbstractQuadraticPolynomial<*>.eq(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), rhs), Comparison.EQ)

infix fun AbstractQuadraticPolynomial<*>.le(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), rhs), Comparison.LE)

infix fun AbstractQuadraticPolynomial<*>.ge(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), rhs), Comparison.GE)

infix fun AbstractQuadraticPolynomial<*>.lt(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), rhs), Comparison.LT)

infix fun AbstractQuadraticPolynomial<*>.gt(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), rhs), Comparison.GT)

infix fun AbstractQuadraticPolynomial<*>.ne(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), rhs), Comparison.NE)

// Flt64 vs Quadratic polynomial
infix fun Flt64.eq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), this), rhs.toUtilsPolynomial(), Comparison.EQ)

infix fun Flt64.le(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), this), rhs.toUtilsPolynomial(), Comparison.LE)

infix fun Flt64.ge(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(emptyList(), this), rhs.toUtilsPolynomial(), Comparison.GE)

// Backward-compat aliases
infix fun AbstractQuadraticPolynomial<*>.leq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this le rhs
infix fun AbstractQuadraticPolynomial<*>.geq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this ge rhs
infix fun AbstractQuadraticPolynomial<*>.leq(rhs: Flt64): MathQuadraticInequality = this le rhs
infix fun AbstractQuadraticPolynomial<*>.geq(rhs: Flt64): MathQuadraticInequality = this ge rhs
infix fun AbstractQuadraticPolynomial<*>.neq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this ne rhs
infix fun AbstractQuadraticPolynomial<*>.neq(rhs: Flt64): MathQuadraticInequality = this ne rhs
infix fun Flt64.leq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this le rhs
infix fun Flt64.geq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this ge rhs

// Linear polynomial vs Quadratic polynomial (produces quadratic)
infix fun AbstractLinearPolynomial<*>.eq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial().toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.EQ)

infix fun AbstractLinearPolynomial<*>.le(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial().toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.LE)

infix fun AbstractLinearPolynomial<*>.ge(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial().toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.GE)

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: AbstractLinearPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial().toQuadraticPolynomial(), Comparison.EQ)

infix fun AbstractQuadraticPolynomial<*>.le(rhs: AbstractLinearPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial().toQuadraticPolynomial(), Comparison.LE)

infix fun AbstractQuadraticPolynomial<*>.ge(rhs: AbstractLinearPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial().toQuadraticPolynomial(), Comparison.GE)

// UInt comparison helpers (replace frontend inequality DSL usage on UInt types)
infix fun UInt8.geq(rhs: UInt8): Boolean = this >= rhs
infix fun UInt64.geq(rhs: UInt64): Boolean = this >= rhs
infix fun UInt8.eq(rhs: UInt8): Boolean = this == rhs
infix fun UInt64.eq(rhs: UInt64): Boolean = this == rhs
infix fun UInt8.neq(rhs: UInt64): Boolean = this.toUInt64().toLong() != rhs.toLong()
infix fun UInt64.neq(rhs: UInt64): Boolean = this != rhs
infix fun UInt64.eq(rhs: Flt64): Boolean = this.toLong().toDouble() == rhs.toDouble()
infix fun UInt64.neq(rhs: Flt64): Boolean = this.toLong().toDouble() != rhs.toDouble()
infix fun Flt64.eq(rhs: UInt64): Boolean = this.toDouble() == rhs.toLong().toDouble()
infix fun Flt64.neq(rhs: UInt64): Boolean = this.toDouble() != rhs.toLong().toDouble()

// ========== Symbol / AbstractVariableItem DSL ==========
// These replace frontend.inequality infix functions on AbstractVariableItem

private fun Symbol.asUtilsLinearPoly(): UtilsLinearPolynomial<Flt64> =
    UtilsLinearPolynomial(listOf(UtilsLinearMonomial(Flt64.one, this)), Flt64.zero)

// Symbol vs Flt64
infix fun Symbol.eq(rhs: Flt64): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.EQ)
infix fun Symbol.le(rhs: Flt64): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun Symbol.ge(rhs: Flt64): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun Symbol.lt(rhs: Flt64): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.LT)
infix fun Symbol.gt(rhs: Flt64): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.GT)
infix fun Symbol.ne(rhs: Flt64): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.NE)

// Flt64 vs Symbol
infix fun Flt64.eq(rhs: Symbol): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asUtilsLinearPoly(), Comparison.EQ)
infix fun Flt64.le(rhs: Symbol): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asUtilsLinearPoly(), Comparison.LE)
infix fun Flt64.ge(rhs: Symbol): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asUtilsLinearPoly(), Comparison.GE)
infix fun Flt64.lt(rhs: Symbol): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asUtilsLinearPoly(), Comparison.LT)
infix fun Flt64.gt(rhs: Symbol): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asUtilsLinearPoly(), Comparison.GT)
infix fun Flt64.ne(rhs: Symbol): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asUtilsLinearPoly(), Comparison.NE)

// Symbol vs Symbol
infix fun Symbol.eq(rhs: Symbol): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asUtilsLinearPoly(), Comparison.EQ)
infix fun Symbol.le(rhs: Symbol): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asUtilsLinearPoly(), Comparison.LE)
infix fun Symbol.ge(rhs: Symbol): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asUtilsLinearPoly(), Comparison.GE)
infix fun Symbol.lt(rhs: Symbol): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asUtilsLinearPoly(), Comparison.LT)
infix fun Symbol.gt(rhs: Symbol): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asUtilsLinearPoly(), Comparison.GT)
infix fun Symbol.ne(rhs: Symbol): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asUtilsLinearPoly(), Comparison.NE)

// Symbol vs Int/Double
infix fun Symbol.eq(rhs: Int): MathLinearInequality = this eq Flt64(rhs.toDouble())
infix fun Symbol.le(rhs: Int): MathLinearInequality = this le Flt64(rhs.toDouble())
infix fun Symbol.ge(rhs: Int): MathLinearInequality = this ge Flt64(rhs.toDouble())
infix fun Symbol.lt(rhs: Int): MathLinearInequality = this lt Flt64(rhs.toDouble())
infix fun Symbol.gt(rhs: Int): MathLinearInequality = this gt Flt64(rhs.toDouble())
infix fun Symbol.ne(rhs: Int): MathLinearInequality = this ne Flt64(rhs.toDouble())

infix fun Symbol.eq(rhs: Double): MathLinearInequality = this eq Flt64(rhs)
infix fun Symbol.le(rhs: Double): MathLinearInequality = this le Flt64(rhs)
infix fun Symbol.ge(rhs: Double): MathLinearInequality = this ge Flt64(rhs)
infix fun Symbol.lt(rhs: Double): MathLinearInequality = this lt Flt64(rhs)
infix fun Symbol.gt(rhs: Double): MathLinearInequality = this gt Flt64(rhs)
infix fun Symbol.ne(rhs: Double): MathLinearInequality = this ne Flt64(rhs)

// Int/Double vs Symbol
infix fun Int.eq(rhs: Symbol): MathLinearInequality = Flt64(this.toDouble()) eq rhs
infix fun Int.le(rhs: Symbol): MathLinearInequality = Flt64(this.toDouble()) le rhs
infix fun Int.ge(rhs: Symbol): MathLinearInequality = Flt64(this.toDouble()) ge rhs
infix fun Int.lt(rhs: Symbol): MathLinearInequality = Flt64(this.toDouble()) lt rhs
infix fun Int.gt(rhs: Symbol): MathLinearInequality = Flt64(this.toDouble()) gt rhs

infix fun Double.eq(rhs: Symbol): MathLinearInequality = Flt64(this) eq rhs
infix fun Double.le(rhs: Symbol): MathLinearInequality = Flt64(this) le rhs
infix fun Double.ge(rhs: Symbol): MathLinearInequality = Flt64(this) ge rhs
infix fun Double.lt(rhs: Symbol): MathLinearInequality = Flt64(this) lt rhs
infix fun Double.gt(rhs: Symbol): MathLinearInequality = Flt64(this) gt rhs

// Backward-compat aliases for frontend naming on Symbol
infix fun Symbol.leq(rhs: Flt64): MathLinearInequality = this le rhs
infix fun Symbol.geq(rhs: Flt64): MathLinearInequality = this ge rhs
infix fun Symbol.neq(rhs: Flt64): MathLinearInequality = this ne rhs
infix fun Symbol.ls(rhs: Flt64): MathLinearInequality = this lt rhs
infix fun Symbol.gr(rhs: Flt64): MathLinearInequality = this gt rhs
infix fun Flt64.leq(rhs: Symbol): MathLinearInequality = this le rhs
infix fun Flt64.geq(rhs: Symbol): MathLinearInequality = this ge rhs
infix fun Flt64.neq(rhs: Symbol): MathLinearInequality = this ne rhs
infix fun Flt64.ls(rhs: Symbol): MathLinearInequality = this lt rhs
infix fun Flt64.gr(rhs: Symbol): MathLinearInequality = this gt rhs

infix fun Symbol.leq(rhs: Symbol): MathLinearInequality = this le rhs
infix fun Symbol.geq(rhs: Symbol): MathLinearInequality = this ge rhs
infix fun Symbol.neq(rhs: Symbol): MathLinearInequality = this ne rhs
infix fun Symbol.ls(rhs: Symbol): MathLinearInequality = this lt rhs
infix fun Symbol.gr(rhs: Symbol): MathLinearInequality = this gt rhs

infix fun Symbol.leq(rhs: Int): MathLinearInequality = this le rhs
infix fun Symbol.geq(rhs: Int): MathLinearInequality = this ge rhs
infix fun Symbol.neq(rhs: Int): MathLinearInequality = this ne rhs
infix fun Symbol.ls(rhs: Int): MathLinearInequality = this lt rhs
infix fun Symbol.gr(rhs: Int): MathLinearInequality = this gt rhs

infix fun Symbol.leq(rhs: Double): MathLinearInequality = this le rhs
infix fun Symbol.geq(rhs: Double): MathLinearInequality = this ge rhs
infix fun Symbol.neq(rhs: Double): MathLinearInequality = this ne rhs
infix fun Symbol.ls(rhs: Double): MathLinearInequality = this lt rhs
infix fun Symbol.gr(rhs: Double): MathLinearInequality = this gt rhs

// AbstractVariableItem aliases (delegates to Symbol since AVI extends Symbol)
infix fun AbstractVariableItem<*, *>.leq(rhs: Flt64): MathLinearInequality = (this as Symbol) leq rhs
infix fun AbstractVariableItem<*, *>.geq(rhs: Flt64): MathLinearInequality = (this as Symbol) geq rhs
infix fun AbstractVariableItem<*, *>.eq(rhs: Flt64): MathLinearInequality = (this as Symbol) eq rhs
infix fun AbstractVariableItem<*, *>.neq(rhs: Flt64): MathLinearInequality = (this as Symbol) neq rhs
infix fun AbstractVariableItem<*, *>.ls(rhs: Flt64): MathLinearInequality = (this as Symbol) ls rhs
infix fun AbstractVariableItem<*, *>.gr(rhs: Flt64): MathLinearInequality = (this as Symbol) gr rhs
infix fun AbstractVariableItem<*, *>.le(rhs: Flt64): MathLinearInequality = (this as Symbol) le rhs
infix fun AbstractVariableItem<*, *>.ge(rhs: Flt64): MathLinearInequality = (this as Symbol) ge rhs
infix fun AbstractVariableItem<*, *>.lt(rhs: Flt64): MathLinearInequality = (this as Symbol) lt rhs
infix fun AbstractVariableItem<*, *>.gt(rhs: Flt64): MathLinearInequality = (this as Symbol) gt rhs
infix fun AbstractVariableItem<*, *>.ne(rhs: Flt64): MathLinearInequality = (this as Symbol) ne rhs

// ========== Symbol/AbstractVariableItem vs Symbol/AbstractVariableItem ==========
infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) leq (rhs as Symbol)
infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) geq (rhs as Symbol)
infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) eq (rhs as Symbol)
infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) neq (rhs as Symbol)
infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) ls (rhs as Symbol)
infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) gr (rhs as Symbol)

// ========== AbstractLinearPolynomial vs Symbol ==========
infix fun AbstractLinearPolynomial<*>.eq(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.asUtilsLinearPoly(), Comparison.EQ)
infix fun AbstractLinearPolynomial<*>.le(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.asUtilsLinearPoly(), Comparison.LE)
infix fun AbstractLinearPolynomial<*>.ge(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.asUtilsLinearPoly(), Comparison.GE)
infix fun AbstractLinearPolynomial<*>.lt(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.asUtilsLinearPoly(), Comparison.LT)
infix fun AbstractLinearPolynomial<*>.gt(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.asUtilsLinearPoly(), Comparison.GT)
infix fun AbstractLinearPolynomial<*>.ne(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), rhs.asUtilsLinearPoly(), Comparison.NE)

infix fun AbstractLinearPolynomial<*>.leq(rhs: Symbol): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: Symbol): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: Symbol): MathLinearInequality = this ne rhs
infix fun AbstractLinearPolynomial<*>.ls(rhs: Symbol): MathLinearInequality = this lt rhs
infix fun AbstractLinearPolynomial<*>.gr(rhs: Symbol): MathLinearInequality = this gt rhs

// Symbol vs AbstractLinearPolynomial
infix fun Symbol.eq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), rhs.toUtilsPolynomial(), Comparison.EQ)
infix fun Symbol.le(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), rhs.toUtilsPolynomial(), Comparison.LE)
infix fun Symbol.ge(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), rhs.toUtilsPolynomial(), Comparison.GE)
infix fun Symbol.lt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), rhs.toUtilsPolynomial(), Comparison.LT)
infix fun Symbol.gt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), rhs.toUtilsPolynomial(), Comparison.GT)
infix fun Symbol.ne(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), rhs.toUtilsPolynomial(), Comparison.NE)

infix fun Symbol.leq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this le rhs
infix fun Symbol.geq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ge rhs
infix fun Symbol.neq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ne rhs
infix fun Symbol.ls(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this lt rhs
infix fun Symbol.gr(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this gt rhs

// AbstractVariableItem vs AbstractLinearPolynomial
infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = (this as Symbol) leq rhs
infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = (this as Symbol) geq rhs
infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = (this as Symbol) eq rhs
infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = (this as Symbol) neq rhs
infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = (this as Symbol) ls rhs
infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = (this as Symbol) gr rhs

// AbstractLinearPolynomial vs AbstractVariableItem
infix fun AbstractLinearPolynomial<*>.leq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = this leq (rhs as Symbol)
infix fun AbstractLinearPolynomial<*>.geq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = this geq (rhs as Symbol)
infix fun AbstractLinearPolynomial<*>.eq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = this eq (rhs as Symbol)
infix fun AbstractLinearPolynomial<*>.neq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = this neq (rhs as Symbol)
infix fun AbstractLinearPolynomial<*>.ls(rhs: AbstractVariableItem<*, *>): MathLinearInequality = this ls (rhs as Symbol)
infix fun AbstractLinearPolynomial<*>.gr(rhs: AbstractVariableItem<*, *>): MathLinearInequality = this gt (rhs as Symbol)

// ========== LinearMonomial vs AbstractLinearPolynomial ==========
private fun LinearMonomial<Flt64>.asUtilsLinearMonomial(): UtilsLinearMonomial<Flt64> =
    UtilsLinearMonomial(coefficient, symbol)

infix fun LinearMonomial<Flt64>.eq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(asUtilsLinearMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.EQ)
infix fun LinearMonomial<Flt64>.le(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(asUtilsLinearMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.LE)
infix fun LinearMonomial<Flt64>.ge(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(asUtilsLinearMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.GE)
infix fun LinearMonomial<Flt64>.lt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(asUtilsLinearMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.LT)
infix fun LinearMonomial<Flt64>.gt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(asUtilsLinearMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.GT)
infix fun LinearMonomial<Flt64>.ne(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(asUtilsLinearMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.NE)

infix fun LinearMonomial<Flt64>.leq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this le rhs
infix fun LinearMonomial<Flt64>.geq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ge rhs
infix fun LinearMonomial<Flt64>.neq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ne rhs
infix fun LinearMonomial<Flt64>.ls(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this lt rhs
infix fun LinearMonomial<Flt64>.gr(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this gt rhs

// AbstractLinearPolynomial vs LinearMonomial
infix fun AbstractLinearPolynomial<*>.eq(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.asUtilsLinearMonomial()), Flt64.zero), Comparison.EQ)
infix fun AbstractLinearPolynomial<*>.le(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.asUtilsLinearMonomial()), Flt64.zero), Comparison.LE)
infix fun AbstractLinearPolynomial<*>.ge(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.asUtilsLinearMonomial()), Flt64.zero), Comparison.GE)
infix fun AbstractLinearPolynomial<*>.lt(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.asUtilsLinearMonomial()), Flt64.zero), Comparison.LT)
infix fun AbstractLinearPolynomial<*>.gt(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.asUtilsLinearMonomial()), Flt64.zero), Comparison.GT)
infix fun AbstractLinearPolynomial<*>.ne(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.asUtilsLinearMonomial()), Flt64.zero), Comparison.NE)

infix fun AbstractLinearPolynomial<*>.leq(rhs: LinearMonomial<Flt64>): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: LinearMonomial<Flt64>): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: LinearMonomial<Flt64>): MathLinearInequality = this ne rhs
infix fun AbstractLinearPolynomial<*>.ls(rhs: LinearMonomial<Flt64>): MathLinearInequality = this lt rhs
infix fun AbstractLinearPolynomial<*>.gr(rhs: LinearMonomial<Flt64>): MathLinearInequality = this gt rhs

// ========== Symbol vs Boolean ==========
infix fun Symbol.eq(rhs: Boolean): MathLinearInequality = this eq if (rhs) Flt64.one else Flt64.zero
infix fun Symbol.le(rhs: Boolean): MathLinearInequality = this le if (rhs) Flt64.one else Flt64.zero
infix fun Symbol.ge(rhs: Boolean): MathLinearInequality = this ge if (rhs) Flt64.one else Flt64.zero
infix fun Symbol.lt(rhs: Boolean): MathLinearInequality = this lt if (rhs) Flt64.one else Flt64.zero
infix fun Symbol.gt(rhs: Boolean): MathLinearInequality = this gt if (rhs) Flt64.one else Flt64.zero
infix fun Symbol.ne(rhs: Boolean): MathLinearInequality = this ne if (rhs) Flt64.one else Flt64.zero

infix fun Symbol.leq(rhs: Boolean): MathLinearInequality = this le rhs
infix fun Symbol.geq(rhs: Boolean): MathLinearInequality = this ge rhs
infix fun Symbol.neq(rhs: Boolean): MathLinearInequality = this ne rhs
infix fun Symbol.ls(rhs: Boolean): MathLinearInequality = this lt rhs
infix fun Symbol.gr(rhs: Boolean): MathLinearInequality = this gt rhs

// AbstractVariableItem vs Boolean
infix fun AbstractVariableItem<*, *>.eq(rhs: Boolean): MathLinearInequality = (this as Symbol) eq rhs
infix fun AbstractVariableItem<*, *>.le(rhs: Boolean): MathLinearInequality = (this as Symbol) le rhs
infix fun AbstractVariableItem<*, *>.ge(rhs: Boolean): MathLinearInequality = (this as Symbol) ge rhs
infix fun AbstractVariableItem<*, *>.lt(rhs: Boolean): MathLinearInequality = (this as Symbol) lt rhs
infix fun AbstractVariableItem<*, *>.gt(rhs: Boolean): MathLinearInequality = (this as Symbol) gt rhs
infix fun AbstractVariableItem<*, *>.ne(rhs: Boolean): MathLinearInequality = (this as Symbol) ne rhs
infix fun AbstractVariableItem<*, *>.leq(rhs: Boolean): MathLinearInequality = (this as Symbol) leq rhs
infix fun AbstractVariableItem<*, *>.geq(rhs: Boolean): MathLinearInequality = (this as Symbol) geq rhs
infix fun AbstractVariableItem<*, *>.neq(rhs: Boolean): MathLinearInequality = (this as Symbol) neq rhs
infix fun AbstractVariableItem<*, *>.ls(rhs: Boolean): MathLinearInequality = (this as Symbol) ls rhs
infix fun AbstractVariableItem<*, *>.gr(rhs: Boolean): MathLinearInequality = (this as Symbol) gr rhs

// ========== Math LinearMonomial<Flt64> vs Flt64 ==========
private fun LinearMonomial<Flt64>.asPoly(): UtilsLinearPolynomial<Flt64> =
    UtilsLinearPolynomial(listOf(this), Flt64.zero)

infix fun LinearMonomial<Flt64>.eq(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.EQ)
infix fun LinearMonomial<Flt64>.le(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun LinearMonomial<Flt64>.ge(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun LinearMonomial<Flt64>.lt(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.LT)
infix fun LinearMonomial<Flt64>.gt(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.GT)
infix fun LinearMonomial<Flt64>.ne(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.NE)

infix fun LinearMonomial<Flt64>.leq(rhs: Flt64): MathLinearInequality = this le rhs
infix fun LinearMonomial<Flt64>.geq(rhs: Flt64): MathLinearInequality = this ge rhs
infix fun LinearMonomial<Flt64>.neq(rhs: Flt64): MathLinearInequality = this ne rhs
infix fun LinearMonomial<Flt64>.ls(rhs: Flt64): MathLinearInequality = this lt rhs
infix fun LinearMonomial<Flt64>.gr(rhs: Flt64): MathLinearInequality = this gt rhs

// Flt64 vs Math LinearMonomial<Flt64>
infix fun Flt64.eq(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.EQ)
infix fun Flt64.le(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.LE)
infix fun Flt64.ge(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.GE)
infix fun Flt64.lt(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.LT)
infix fun Flt64.gt(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.GT)
infix fun Flt64.ne(rhs: LinearMonomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.NE)

infix fun Flt64.leq(rhs: LinearMonomial<Flt64>): MathLinearInequality = this le rhs
infix fun Flt64.geq(rhs: LinearMonomial<Flt64>): MathLinearInequality = this ge rhs
infix fun Flt64.neq(rhs: LinearMonomial<Flt64>): MathLinearInequality = this ne rhs
infix fun Flt64.ls(rhs: LinearMonomial<Flt64>): MathLinearInequality = this lt rhs
infix fun Flt64.gr(rhs: LinearMonomial<Flt64>): MathLinearInequality = this gt rhs

// ========== Frontend LinearMonomial vs AbstractLinearPolynomial ==========
infix fun FrontendLinearMonomial.eq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.EQ)
infix fun FrontendLinearMonomial.le(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.LE)
infix fun FrontendLinearMonomial.ge(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.GE)
infix fun FrontendLinearMonomial.lt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.LT)
infix fun FrontendLinearMonomial.gt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.GT)
infix fun FrontendLinearMonomial.ne(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.NE)

infix fun FrontendLinearMonomial.leq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this le rhs
infix fun FrontendLinearMonomial.geq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ge rhs
infix fun FrontendLinearMonomial.neq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ne rhs
infix fun FrontendLinearMonomial.ls(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this lt rhs
infix fun FrontendLinearMonomial.gr(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this gt rhs

// AbstractLinearPolynomial vs Frontend LinearMonomial
infix fun AbstractLinearPolynomial<*>.eq(rhs: FrontendLinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.EQ)
infix fun AbstractLinearPolynomial<*>.le(rhs: FrontendLinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.LE)
infix fun AbstractLinearPolynomial<*>.ge(rhs: FrontendLinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.GE)
infix fun AbstractLinearPolynomial<*>.lt(rhs: FrontendLinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.LT)
infix fun AbstractLinearPolynomial<*>.gt(rhs: FrontendLinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.GT)
infix fun AbstractLinearPolynomial<*>.ne(rhs: FrontendLinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.NE)

infix fun AbstractLinearPolynomial<*>.leq(rhs: FrontendLinearMonomial): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: FrontendLinearMonomial): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: FrontendLinearMonomial): MathLinearInequality = this ne rhs
infix fun AbstractLinearPolynomial<*>.ls(rhs: FrontendLinearMonomial): MathLinearInequality = this lt rhs
infix fun AbstractLinearPolynomial<*>.gr(rhs: FrontendLinearMonomial): MathLinearInequality = this gt rhs

// ========== AbstractVariableItem vs AbstractQuadraticPolynomial ==========
infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality((this as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.EQ)
infix fun AbstractVariableItem<*, *>.le(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality((this as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.LE)
infix fun AbstractVariableItem<*, *>.ge(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality((this as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.GE)
infix fun AbstractVariableItem<*, *>.lt(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality((this as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gt(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality((this as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.GT)
infix fun AbstractVariableItem<*, *>.ne(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality((this as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.NE)

infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this le rhs
infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this ge rhs
infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this ne rhs
infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this lt rhs
infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this gt rhs

// AbstractQuadraticPolynomial vs AbstractVariableItem
infix fun AbstractQuadraticPolynomial<*>.eq(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), (rhs as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), Comparison.EQ)
infix fun AbstractQuadraticPolynomial<*>.le(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), (rhs as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), Comparison.LE)
infix fun AbstractQuadraticPolynomial<*>.ge(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), (rhs as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), Comparison.GE)
infix fun AbstractQuadraticPolynomial<*>.lt(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), (rhs as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), Comparison.LT)
infix fun AbstractQuadraticPolynomial<*>.gt(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), (rhs as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), Comparison.GT)
infix fun AbstractQuadraticPolynomial<*>.ne(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), (rhs as Symbol).asUtilsLinearPoly().toQuadraticPolynomial(), Comparison.NE)

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality = this le rhs
infix fun AbstractQuadraticPolynomial<*>.geq(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality = this ge rhs
infix fun AbstractQuadraticPolynomial<*>.neq(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality = this ne rhs
infix fun AbstractQuadraticPolynomial<*>.ls(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality = this lt rhs
infix fun AbstractQuadraticPolynomial<*>.gr(rhs: AbstractVariableItem<*, *>): MathQuadraticInequality = this gt rhs
