@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.UInt8
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.le as mathLe
import fuookami.ospf.kotlin.math.symbol.inequality.ge as mathGe
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

infix fun Flt64.lt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), this), rhs.toUtilsPolynomial(), Comparison.LT)

infix fun Flt64.gt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), this), rhs.toUtilsPolynomial(), Comparison.GT)

infix fun Flt64.ne(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(emptyList(), this), rhs.toUtilsPolynomial(), Comparison.NE)

// Symbol vs math LinearPolynomial
infix fun Symbol.leq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(UtilsLinearMonomial(Flt64.one, this)), Flt64.zero), rhs, Comparison.LE)
infix fun Symbol.geq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(UtilsLinearMonomial(Flt64.one, this)), Flt64.zero), rhs, Comparison.GE)
infix fun UtilsLinearPolynomial<Flt64>.leq(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(this, UtilsLinearPolynomial(listOf(UtilsLinearMonomial(Flt64.one, rhs)), Flt64.zero), Comparison.LE)
infix fun UtilsLinearPolynomial<Flt64>.geq(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(this, UtilsLinearPolynomial(listOf(UtilsLinearMonomial(Flt64.one, rhs)), Flt64.zero), Comparison.GE)

// math LinearPolynomial vs math LinearPolynomial
infix fun UtilsLinearPolynomial<Flt64>.leq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(this, rhs, Comparison.LE)
infix fun UtilsLinearPolynomial<Flt64>.geq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(this, rhs, Comparison.GE)
infix fun UtilsLinearPolynomial<Flt64>.eq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(this, rhs, Comparison.EQ)

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

infix fun AbstractLinearPolynomial<*>.ne(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial().toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.NE)

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: AbstractLinearPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial().toQuadraticPolynomial(), Comparison.EQ)

infix fun AbstractQuadraticPolynomial<*>.le(rhs: AbstractLinearPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial().toQuadraticPolynomial(), Comparison.LE)

infix fun AbstractQuadraticPolynomial<*>.ge(rhs: AbstractLinearPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial().toQuadraticPolynomial(), Comparison.GE)

infix fun AbstractQuadraticPolynomial<*>.ne(rhs: AbstractLinearPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), rhs.toUtilsPolynomial().toQuadraticPolynomial(), Comparison.NE)

// Backward-compat aliases for cross-type
infix fun AbstractLinearPolynomial<*>.leq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this ne rhs
infix fun AbstractQuadraticPolynomial<*>.leq(rhs: AbstractLinearPolynomial<*>): MathQuadraticInequality = this le rhs
infix fun AbstractQuadraticPolynomial<*>.geq(rhs: AbstractLinearPolynomial<*>): MathQuadraticInequality = this ge rhs
infix fun AbstractQuadraticPolynomial<*>.neq(rhs: AbstractLinearPolynomial<*>): MathQuadraticInequality = this ne rhs

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

infix fun LinearMonomial.eq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.EQ)
infix fun LinearMonomial.le(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.LE)
infix fun LinearMonomial.ge(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.GE)
infix fun LinearMonomial.lt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.LT)
infix fun LinearMonomial.gt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.GT)
infix fun LinearMonomial.ne(rhs: AbstractLinearPolynomial<*>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.toUtilsPolynomial(), Comparison.NE)

infix fun LinearMonomial.leq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this le rhs
infix fun LinearMonomial.geq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ge rhs
infix fun LinearMonomial.neq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ne rhs
infix fun LinearMonomial.ls(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this lt rhs
infix fun LinearMonomial.gr(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this gt rhs

// AbstractLinearPolynomial vs LinearMonomial
infix fun AbstractLinearPolynomial<*>.eq(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.EQ)
infix fun AbstractLinearPolynomial<*>.le(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.LE)
infix fun AbstractLinearPolynomial<*>.ge(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.GE)
infix fun AbstractLinearPolynomial<*>.lt(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.LT)
infix fun AbstractLinearPolynomial<*>.gt(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.GT)
infix fun AbstractLinearPolynomial<*>.ne(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.NE)

infix fun AbstractLinearPolynomial<*>.leq(rhs: LinearMonomial): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: LinearMonomial): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: LinearMonomial): MathLinearInequality = this ne rhs
infix fun AbstractLinearPolynomial<*>.ls(rhs: LinearMonomial): MathLinearInequality = this lt rhs
infix fun AbstractLinearPolynomial<*>.gr(rhs: LinearMonomial): MathLinearInequality = this gt rhs

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

// AbstractLinearPolynomial vs Boolean
infix fun AbstractLinearPolynomial<*>.eq(rhs: Boolean): MathLinearInequality = this eq if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.le(rhs: Boolean): MathLinearInequality = this le if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.ge(rhs: Boolean): MathLinearInequality = this ge if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.lt(rhs: Boolean): MathLinearInequality = this lt if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.gt(rhs: Boolean): MathLinearInequality = this gt if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.ne(rhs: Boolean): MathLinearInequality = this ne if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.leq(rhs: Boolean): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: Boolean): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: Boolean): MathLinearInequality = this ne rhs
infix fun AbstractLinearPolynomial<*>.ls(rhs: Boolean): MathLinearInequality = this lt rhs
infix fun AbstractLinearPolynomial<*>.gr(rhs: Boolean): MathLinearInequality = this gt rhs

// AbstractQuadraticPolynomial vs Boolean
infix fun AbstractQuadraticPolynomial<*>.eq(rhs: Boolean): MathQuadraticInequality = this eq if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.le(rhs: Boolean): MathQuadraticInequality = this le if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.ge(rhs: Boolean): MathQuadraticInequality = this ge if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.lt(rhs: Boolean): MathQuadraticInequality = this lt if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.gt(rhs: Boolean): MathQuadraticInequality = this gt if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.ne(rhs: Boolean): MathQuadraticInequality = this ne if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.leq(rhs: Boolean): MathQuadraticInequality = this le rhs
infix fun AbstractQuadraticPolynomial<*>.geq(rhs: Boolean): MathQuadraticInequality = this ge rhs
infix fun AbstractQuadraticPolynomial<*>.neq(rhs: Boolean): MathQuadraticInequality = this ne rhs
infix fun AbstractQuadraticPolynomial<*>.ls(rhs: Boolean): MathQuadraticInequality = this lt rhs
infix fun AbstractQuadraticPolynomial<*>.gr(rhs: Boolean): MathQuadraticInequality = this gt rhs

// ========== Math LinearMonomial vs Flt64 ==========
private fun LinearMonomial.asPoly(): UtilsLinearPolynomial<Flt64> =
    UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero)

infix fun LinearMonomial.eq(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.EQ)
infix fun LinearMonomial.le(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun LinearMonomial.ge(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun LinearMonomial.lt(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.LT)
infix fun LinearMonomial.gt(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.GT)
infix fun LinearMonomial.ne(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.NE)

infix fun LinearMonomial.leq(rhs: Flt64): MathLinearInequality = this le rhs
infix fun LinearMonomial.geq(rhs: Flt64): MathLinearInequality = this ge rhs
infix fun LinearMonomial.neq(rhs: Flt64): MathLinearInequality = this ne rhs
infix fun LinearMonomial.ls(rhs: Flt64): MathLinearInequality = this lt rhs
infix fun LinearMonomial.gr(rhs: Flt64): MathLinearInequality = this gt rhs

// Math LinearMonomial vs Boolean
infix fun LinearMonomial.eq(rhs: Boolean): MathLinearInequality = this eq if (rhs) Flt64.one else Flt64.zero
infix fun LinearMonomial.le(rhs: Boolean): MathLinearInequality = this le if (rhs) Flt64.one else Flt64.zero
infix fun LinearMonomial.ge(rhs: Boolean): MathLinearInequality = this ge if (rhs) Flt64.one else Flt64.zero
infix fun LinearMonomial.leq(rhs: Boolean): MathLinearInequality = this le rhs
infix fun LinearMonomial.geq(rhs: Boolean): MathLinearInequality = this ge rhs

// Flt64 vs Math LinearMonomial
infix fun Flt64.eq(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.EQ)
infix fun Flt64.le(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.LE)
infix fun Flt64.ge(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.GE)
infix fun Flt64.lt(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.LT)
infix fun Flt64.gt(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.GT)
infix fun Flt64.ne(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.NE)

infix fun Flt64.leq(rhs: LinearMonomial): MathLinearInequality = this le rhs
infix fun Flt64.geq(rhs: LinearMonomial): MathLinearInequality = this ge rhs
infix fun Flt64.neq(rhs: LinearMonomial): MathLinearInequality = this ne rhs
infix fun Flt64.ls(rhs: LinearMonomial): MathLinearInequality = this lt rhs
infix fun Flt64.gr(rhs: LinearMonomial): MathLinearInequality = this gt rhs


// QuadraticIntermediateSymbol vs Boolean
infix fun QuadraticIntermediateSymbol.eq(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toQuadraticPolynomial().toUtilsPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.EQ)
infix fun QuadraticIntermediateSymbol.le(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toQuadraticPolynomial().toUtilsPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.LE)
infix fun QuadraticIntermediateSymbol.ge(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toQuadraticPolynomial().toUtilsPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.GE)
infix fun QuadraticIntermediateSymbol.leq(rhs: Boolean): MathQuadraticInequality = this le rhs
infix fun QuadraticIntermediateSymbol.geq(rhs: Boolean): MathQuadraticInequality = this ge rhs

// QuadraticMonomial vs Boolean
infix fun QuadraticMonomial.eq(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.EQ)
infix fun QuadraticMonomial.le(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.LE)
infix fun QuadraticMonomial.ge(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.GE)
infix fun QuadraticMonomial.leq(rhs: Boolean): MathQuadraticInequality = this le rhs
infix fun QuadraticMonomial.geq(rhs: Boolean): MathQuadraticInequality = this ge rhs

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

// ========== Symbol vs LinearMonomial ==========
infix fun Symbol.eq(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.EQ)
infix fun Symbol.le(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.LE)
infix fun Symbol.ge(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.GE)
infix fun Symbol.lt(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.LT)
infix fun Symbol.gt(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.GT)
infix fun Symbol.ne(rhs: LinearMonomial): MathLinearInequality =
    MathLinearInequality(asUtilsLinearPoly(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero), Comparison.NE)

infix fun Symbol.leq(rhs: LinearMonomial): MathLinearInequality = this le rhs
infix fun Symbol.geq(rhs: LinearMonomial): MathLinearInequality = this ge rhs
infix fun Symbol.neq(rhs: LinearMonomial): MathLinearInequality = this ne rhs
infix fun Symbol.ls(rhs: LinearMonomial): MathLinearInequality = this lt rhs
infix fun Symbol.gr(rhs: LinearMonomial): MathLinearInequality = this gt rhs

// ========== AbstractVariableItem vs LinearMonomial ==========
infix fun AbstractVariableItem<*, *>.eq(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) eq rhs
infix fun AbstractVariableItem<*, *>.le(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) le rhs
infix fun AbstractVariableItem<*, *>.ge(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) ge rhs
infix fun AbstractVariableItem<*, *>.lt(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) lt rhs
infix fun AbstractVariableItem<*, *>.gt(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) gt rhs
infix fun AbstractVariableItem<*, *>.ne(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) ne rhs

infix fun AbstractVariableItem<*, *>.leq(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) leq rhs
infix fun AbstractVariableItem<*, *>.geq(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) geq rhs
infix fun AbstractVariableItem<*, *>.neq(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) neq rhs
infix fun AbstractVariableItem<*, *>.ls(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) ls rhs
infix fun AbstractVariableItem<*, *>.gr(rhs: LinearMonomial): MathLinearInequality = (this as Symbol) gr rhs

// ========== AbstractQuadraticPolynomial vs LinearMonomial ==========
infix fun AbstractQuadraticPolynomial<*>.eq(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), Comparison.EQ)
infix fun AbstractQuadraticPolynomial<*>.le(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), Comparison.LE)
infix fun AbstractQuadraticPolynomial<*>.ge(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), Comparison.GE)
infix fun AbstractQuadraticPolynomial<*>.lt(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), Comparison.LT)
infix fun AbstractQuadraticPolynomial<*>.gt(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), Comparison.GT)
infix fun AbstractQuadraticPolynomial<*>.ne(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsLinearPolynomial(listOf(rhs.toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), Comparison.NE)

// Backward-compat aliases
infix fun AbstractQuadraticPolynomial<*>.leq(rhs: LinearMonomial): MathQuadraticInequality = this le rhs
infix fun AbstractQuadraticPolynomial<*>.geq(rhs: LinearMonomial): MathQuadraticInequality = this ge rhs
infix fun AbstractQuadraticPolynomial<*>.neq(rhs: LinearMonomial): MathQuadraticInequality = this ne rhs
infix fun AbstractQuadraticPolynomial<*>.ls(rhs: LinearMonomial): MathQuadraticInequality = this lt rhs
infix fun AbstractQuadraticPolynomial<*>.gr(rhs: LinearMonomial): MathQuadraticInequality = this gt rhs

// ========== LinearMonomial vs AbstractQuadraticPolynomial ==========
infix fun LinearMonomial.eq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.EQ)
infix fun LinearMonomial.le(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.LE)
infix fun LinearMonomial.ge(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.GE)
infix fun LinearMonomial.lt(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.LT)
infix fun LinearMonomial.gt(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.GT)
infix fun LinearMonomial.ne(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero).toQuadraticPolynomial(), rhs.toUtilsPolynomial(), Comparison.NE)

// Backward-compat aliases
infix fun LinearMonomial.leq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this le rhs
infix fun LinearMonomial.geq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this ge rhs
infix fun LinearMonomial.neq(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this ne rhs
infix fun LinearMonomial.ls(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this lt rhs
infix fun LinearMonomial.gr(rhs: AbstractQuadraticPolynomial<*>): MathQuadraticInequality = this gt rhs

// ========== LinearMonomial vs Symbol ==========

infix fun LinearMonomial.eq(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.asUtilsLinearPoly(), Comparison.EQ)

infix fun LinearMonomial.le(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.asUtilsLinearPoly(), Comparison.LE)

infix fun LinearMonomial.ge(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.asUtilsLinearPoly(), Comparison.GE)

infix fun LinearMonomial.lt(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.asUtilsLinearPoly(), Comparison.LT)

infix fun LinearMonomial.gt(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.asUtilsLinearPoly(), Comparison.GT)

infix fun LinearMonomial.ne(rhs: Symbol): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero), rhs.asUtilsLinearPoly(), Comparison.NE)

// Backward-compat aliases
infix fun LinearMonomial.leq(rhs: Symbol): MathLinearInequality = this le rhs
infix fun LinearMonomial.geq(rhs: Symbol): MathLinearInequality = this ge rhs
infix fun LinearMonomial.neq(rhs: Symbol): MathLinearInequality = this ne rhs
infix fun LinearMonomial.ls(rhs: Symbol): MathLinearInequality = this lt rhs
infix fun LinearMonomial.gr(rhs: Symbol): MathLinearInequality = this gt rhs

// ========== UInt64/UInt8 comparisons (for SatisfiedAmountInequality) ==========

infix fun AbstractLinearPolynomial<*>.eq(rhs: UInt64): MathLinearInequality = this eq rhs.toFlt64()
infix fun AbstractLinearPolynomial<*>.le(rhs: UInt64): MathLinearInequality = this le rhs.toFlt64()
infix fun AbstractLinearPolynomial<*>.ge(rhs: UInt64): MathLinearInequality = this ge rhs.toFlt64()
infix fun AbstractLinearPolynomial<*>.lt(rhs: UInt64): MathLinearInequality = this lt rhs.toFlt64()
infix fun AbstractLinearPolynomial<*>.gt(rhs: UInt64): MathLinearInequality = this gt rhs.toFlt64()
infix fun AbstractLinearPolynomial<*>.ne(rhs: UInt64): MathLinearInequality = this ne rhs.toFlt64()

infix fun AbstractLinearPolynomial<*>.leq(rhs: UInt64): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: UInt64): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: UInt64): MathLinearInequality = this ne rhs
infix fun AbstractLinearPolynomial<*>.ls(rhs: UInt64): MathLinearInequality = this lt rhs
infix fun AbstractLinearPolynomial<*>.gr(rhs: UInt64): MathLinearInequality = this gt rhs

infix fun UInt64.eq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this.toFlt64() eq rhs
infix fun UInt64.le(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this.toFlt64() le rhs
infix fun UInt64.ge(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this.toFlt64() ge rhs
infix fun UInt64.lt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this.toFlt64() lt rhs
infix fun UInt64.gt(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this.toFlt64() gt rhs
infix fun UInt64.ne(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this.toFlt64() ne rhs

infix fun UInt64.leq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this le rhs
infix fun UInt64.geq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ge rhs
infix fun UInt64.neq(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this ne rhs

// ========== Symbol / LinearIntermediateSymbol vs UInt64 ==========

infix fun Symbol.leq(rhs: UInt64): MathLinearInequality = this leq rhs.toFlt64()
infix fun Symbol.geq(rhs: UInt64): MathLinearInequality = this geq rhs.toFlt64()
infix fun Symbol.eq(rhs: UInt64): MathLinearInequality = this eq rhs.toFlt64()
infix fun UInt64.eq(rhs: Symbol): MathLinearInequality = this.toFlt64() eq rhs
infix fun UInt64.leq(rhs: Symbol): MathLinearInequality = this.toFlt64() leq rhs
infix fun UInt64.geq(rhs: Symbol): MathLinearInequality = this.toFlt64() geq rhs
infix fun UInt64.ls(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this lt rhs
infix fun UInt64.gr(rhs: AbstractLinearPolynomial<*>): MathLinearInequality = this gt rhs
