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
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
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

// ========== Utils LinearPolynomial DSL ==========

// UtilsLinearPolynomial vs UtilsLinearPolynomial
infix fun UtilsLinearPolynomial<Flt64>.eq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(this, rhs, Comparison.EQ)

infix fun UtilsLinearPolynomial<Flt64>.le(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(this, rhs, Comparison.LE)

infix fun UtilsLinearPolynomial<Flt64>.ge(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(this, rhs, Comparison.GE)

infix fun UtilsLinearPolynomial<Flt64>.lt(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(this, rhs, Comparison.LT)

infix fun UtilsLinearPolynomial<Flt64>.gt(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(this, rhs, Comparison.GT)

infix fun UtilsLinearPolynomial<Flt64>.ne(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(this, rhs, Comparison.NE)

infix fun UtilsLinearPolynomial<Flt64>.leq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this le rhs
infix fun UtilsLinearPolynomial<Flt64>.geq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this ge rhs
infix fun UtilsLinearPolynomial<Flt64>.neq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this ne rhs

// UtilsLinearPolynomial vs Flt64
infix fun UtilsLinearPolynomial<Flt64>.eq(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(this, UtilsLinearPolynomial(emptyList(), rhs), Comparison.EQ)

infix fun UtilsLinearPolynomial<Flt64>.le(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(this, UtilsLinearPolynomial(emptyList(), rhs), Comparison.LE)

infix fun UtilsLinearPolynomial<Flt64>.ge(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(this, UtilsLinearPolynomial(emptyList(), rhs), Comparison.GE)

infix fun UtilsLinearPolynomial<Flt64>.lt(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(this, UtilsLinearPolynomial(emptyList(), rhs), Comparison.LT)

infix fun UtilsLinearPolynomial<Flt64>.gt(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(this, UtilsLinearPolynomial(emptyList(), rhs), Comparison.GT)

infix fun UtilsLinearPolynomial<Flt64>.ne(rhs: Flt64): MathLinearInequality =
    MathLinearInequality(this, UtilsLinearPolynomial(emptyList(), rhs), Comparison.NE)

infix fun UtilsLinearPolynomial<Flt64>.leq(rhs: Flt64): MathLinearInequality = this le rhs
infix fun UtilsLinearPolynomial<Flt64>.geq(rhs: Flt64): MathLinearInequality = this ge rhs
infix fun UtilsLinearPolynomial<Flt64>.neq(rhs: Flt64): MathLinearInequality = this ne rhs

// Flt64 vs UtilsLinearPolynomial
infix fun Flt64.eq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs, Comparison.EQ)

infix fun Flt64.le(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs, Comparison.LE)

infix fun Flt64.ge(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs, Comparison.GE)

infix fun Flt64.lt(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs, Comparison.LT)

infix fun Flt64.gt(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs, Comparison.GT)

infix fun Flt64.ne(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs, Comparison.NE)

infix fun Flt64.leq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this le rhs
infix fun Flt64.geq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this ge rhs
infix fun Flt64.neq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this ne rhs

// UtilsLinearPolynomial vs UtilsQuadraticPolynomial (produces quadratic)
infix fun UtilsLinearPolynomial<Flt64>.eq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this.toQuadraticPolynomial(), rhs, Comparison.EQ)

infix fun UtilsLinearPolynomial<Flt64>.le(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this.toQuadraticPolynomial(), rhs, Comparison.LE)

infix fun UtilsLinearPolynomial<Flt64>.ge(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this.toQuadraticPolynomial(), rhs, Comparison.GE)

infix fun UtilsLinearPolynomial<Flt64>.ne(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this.toQuadraticPolynomial(), rhs, Comparison.NE)

infix fun UtilsLinearPolynomial<Flt64>.leq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this le rhs
infix fun UtilsLinearPolynomial<Flt64>.geq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this ge rhs
infix fun UtilsLinearPolynomial<Flt64>.neq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this ne rhs

// UtilsQuadraticPolynomial vs UtilsLinearPolynomial
infix fun UtilsQuadraticPolynomial<Flt64>.eq(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.EQ)

infix fun UtilsQuadraticPolynomial<Flt64>.le(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.LE)

infix fun UtilsQuadraticPolynomial<Flt64>.ge(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.GE)

infix fun UtilsQuadraticPolynomial<Flt64>.ne(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.NE)

infix fun UtilsQuadraticPolynomial<Flt64>.leq(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality = this le rhs
infix fun UtilsQuadraticPolynomial<Flt64>.geq(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality = this ge rhs
infix fun UtilsQuadraticPolynomial<Flt64>.neq(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality = this ne rhs

// ========== Utils QuadraticPolynomial DSL ==========

// UtilsQuadraticPolynomial vs UtilsQuadraticPolynomial
infix fun UtilsQuadraticPolynomial<Flt64>.eq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs, Comparison.EQ)

infix fun UtilsQuadraticPolynomial<Flt64>.le(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs, Comparison.LE)

infix fun UtilsQuadraticPolynomial<Flt64>.ge(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs, Comparison.GE)

infix fun UtilsQuadraticPolynomial<Flt64>.lt(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs, Comparison.LT)

infix fun UtilsQuadraticPolynomial<Flt64>.gt(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs, Comparison.GT)

infix fun UtilsQuadraticPolynomial<Flt64>.ne(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs, Comparison.NE)

infix fun UtilsQuadraticPolynomial<Flt64>.leq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this le rhs
infix fun UtilsQuadraticPolynomial<Flt64>.geq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this ge rhs
infix fun UtilsQuadraticPolynomial<Flt64>.neq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this ne rhs

// UtilsQuadraticPolynomial vs Flt64
infix fun UtilsQuadraticPolynomial<Flt64>.eq(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(this, UtilsQuadraticPolynomial(emptyList(), rhs), Comparison.EQ)

infix fun UtilsQuadraticPolynomial<Flt64>.le(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(this, UtilsQuadraticPolynomial(emptyList(), rhs), Comparison.LE)

infix fun UtilsQuadraticPolynomial<Flt64>.ge(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(this, UtilsQuadraticPolynomial(emptyList(), rhs), Comparison.GE)

infix fun UtilsQuadraticPolynomial<Flt64>.lt(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(this, UtilsQuadraticPolynomial(emptyList(), rhs), Comparison.LT)

infix fun UtilsQuadraticPolynomial<Flt64>.gt(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(this, UtilsQuadraticPolynomial(emptyList(), rhs), Comparison.GT)

infix fun UtilsQuadraticPolynomial<Flt64>.ne(rhs: Flt64): MathQuadraticInequality =
    MathQuadraticInequality(this, UtilsQuadraticPolynomial(emptyList(), rhs), Comparison.NE)

infix fun UtilsQuadraticPolynomial<Flt64>.leq(rhs: Flt64): MathQuadraticInequality = this le rhs
infix fun UtilsQuadraticPolynomial<Flt64>.geq(rhs: Flt64): MathQuadraticInequality = this ge rhs
infix fun UtilsQuadraticPolynomial<Flt64>.neq(rhs: Flt64): MathQuadraticInequality = this ne rhs

// Flt64 vs UtilsQuadraticPolynomial
infix fun Flt64.eq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsQuadraticPolynomial(emptyList(), this), rhs, Comparison.EQ)

infix fun Flt64.le(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsQuadraticPolynomial(emptyList(), this), rhs, Comparison.LE)

infix fun Flt64.ge(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsQuadraticPolynomial(emptyList(), this), rhs, Comparison.GE)

// ========== Symbol DSL ==========

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

// Backward-compat aliases for Symbol naming
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

// Symbol vs UtilsLinearPolynomial
infix fun Symbol.eq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs, Comparison.EQ)
infix fun Symbol.le(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs, Comparison.LE)
infix fun Symbol.ge(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs, Comparison.GE)
infix fun Symbol.lt(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs, Comparison.LT)
infix fun Symbol.gt(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs, Comparison.GT)
infix fun Symbol.ne(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs, Comparison.NE)

infix fun Symbol.leq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this le rhs
infix fun Symbol.geq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this ge rhs
infix fun Symbol.neq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this ne rhs
infix fun Symbol.ls(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this lt rhs
infix fun Symbol.gr(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this gt rhs

// UtilsLinearPolynomial vs Symbol
infix fun UtilsLinearPolynomial<Flt64>.eq(rhs: Symbol): MathLinearInequality = MathLinearInequality(this, rhs.asUtilsLinearPoly(), Comparison.EQ)
infix fun UtilsLinearPolynomial<Flt64>.le(rhs: Symbol): MathLinearInequality = MathLinearInequality(this, rhs.asUtilsLinearPoly(), Comparison.LE)
infix fun UtilsLinearPolynomial<Flt64>.ge(rhs: Symbol): MathLinearInequality = MathLinearInequality(this, rhs.asUtilsLinearPoly(), Comparison.GE)
infix fun UtilsLinearPolynomial<Flt64>.lt(rhs: Symbol): MathLinearInequality = MathLinearInequality(this, rhs.asUtilsLinearPoly(), Comparison.LT)
infix fun UtilsLinearPolynomial<Flt64>.gt(rhs: Symbol): MathLinearInequality = MathLinearInequality(this, rhs.asUtilsLinearPoly(), Comparison.GT)
infix fun UtilsLinearPolynomial<Flt64>.ne(rhs: Symbol): MathLinearInequality = MathLinearInequality(this, rhs.asUtilsLinearPoly(), Comparison.NE)

infix fun UtilsLinearPolynomial<Flt64>.leq(rhs: Symbol): MathLinearInequality = this le rhs
infix fun UtilsLinearPolynomial<Flt64>.geq(rhs: Symbol): MathLinearInequality = this ge rhs
infix fun UtilsLinearPolynomial<Flt64>.neq(rhs: Symbol): MathLinearInequality = this ne rhs

// Symbol vs UtilsQuadraticPolynomial
infix fun Symbol.eq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = MathQuadraticInequality(asUtilsLinearPoly().toQuadraticPolynomial(), rhs, Comparison.EQ)
infix fun Symbol.le(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = MathQuadraticInequality(asUtilsLinearPoly().toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun Symbol.ge(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = MathQuadraticInequality(asUtilsLinearPoly().toQuadraticPolynomial(), rhs, Comparison.GE)
infix fun Symbol.ne(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = MathQuadraticInequality(asUtilsLinearPoly().toQuadraticPolynomial(), rhs, Comparison.NE)

infix fun Symbol.leq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this le rhs
infix fun Symbol.geq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this ge rhs
infix fun Symbol.neq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this ne rhs

// ========== AbstractVariableItem DSL (delegates to Symbol) ==========

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

infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) leq (rhs as Symbol)
infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) geq (rhs as Symbol)
infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) eq (rhs as Symbol)
infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) neq (rhs as Symbol)
infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) ls (rhs as Symbol)
infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractVariableItem<*, *>): MathLinearInequality = (this as Symbol) gr (rhs as Symbol)

// AbstractVariableItem vs UtilsLinearPolynomial
infix fun AbstractVariableItem<*, *>.leq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = (this as Symbol) leq rhs
infix fun AbstractVariableItem<*, *>.geq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = (this as Symbol) geq rhs
infix fun AbstractVariableItem<*, *>.eq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = (this as Symbol) eq rhs
infix fun AbstractVariableItem<*, *>.neq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = (this as Symbol) neq rhs
infix fun AbstractVariableItem<*, *>.ls(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = (this as Symbol) ls rhs
infix fun AbstractVariableItem<*, *>.gr(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = (this as Symbol) gr rhs

// AbstractVariableItem vs UtilsQuadraticPolynomial
infix fun AbstractVariableItem<*, *>.leq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = (this as Symbol) leq rhs
infix fun AbstractVariableItem<*, *>.geq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = (this as Symbol) geq rhs
infix fun AbstractVariableItem<*, *>.eq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = (this as Symbol) eq rhs
infix fun AbstractVariableItem<*, *>.neq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = (this as Symbol) neq rhs
infix fun AbstractVariableItem<*, *>.ls(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsQuadraticPolynomial(listOf(UtilsQuadraticMonomial(Flt64.one, this, this)), Flt64.zero), rhs, Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(UtilsQuadraticPolynomial(listOf(UtilsQuadraticMonomial(Flt64.one, this, this)), Flt64.zero), rhs, Comparison.GT)
infix fun AbstractVariableItem<*, *>.le(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = (this as Symbol) le rhs
infix fun AbstractVariableItem<*, *>.ge(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = (this as Symbol) ge rhs

// ========== Core LinearMonomial DSL ==========

private fun LinearMonomial.asPoly(): UtilsLinearPolynomial<Flt64> =
    UtilsLinearPolynomial(listOf(toUtilsMonomial()), Flt64.zero)

private fun LinearMonomial.asQuadraticPoly(): UtilsQuadraticPolynomial<Flt64> {
    val utilsMono = toUtilsMonomial()
    return UtilsQuadraticPolynomial(
        listOf(UtilsQuadraticMonomial(utilsMono.coefficient, utilsMono.symbol, utilsMono.symbol)),
        Flt64.zero
    )
}

// LinearMonomial vs Flt64
infix fun LinearMonomial.eq(rhs: Flt64): MathLinearInequality = MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.EQ)
infix fun LinearMonomial.le(rhs: Flt64): MathLinearInequality = MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun LinearMonomial.ge(rhs: Flt64): MathLinearInequality = MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun LinearMonomial.lt(rhs: Flt64): MathLinearInequality = MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.LT)
infix fun LinearMonomial.gt(rhs: Flt64): MathLinearInequality = MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.GT)
infix fun LinearMonomial.ne(rhs: Flt64): MathLinearInequality = MathLinearInequality(asPoly(), UtilsLinearPolynomial(emptyList(), rhs), Comparison.NE)

infix fun LinearMonomial.leq(rhs: Flt64): MathLinearInequality = this le rhs
infix fun LinearMonomial.geq(rhs: Flt64): MathLinearInequality = this ge rhs
infix fun LinearMonomial.neq(rhs: Flt64): MathLinearInequality = this ne rhs
infix fun LinearMonomial.ls(rhs: Flt64): MathLinearInequality = this lt rhs
infix fun LinearMonomial.gr(rhs: Flt64): MathLinearInequality = this gt rhs

// LinearMonomial vs Boolean
infix fun LinearMonomial.eq(rhs: Boolean): MathLinearInequality = this eq if (rhs) Flt64.one else Flt64.zero
infix fun LinearMonomial.le(rhs: Boolean): MathLinearInequality = this le if (rhs) Flt64.one else Flt64.zero
infix fun LinearMonomial.ge(rhs: Boolean): MathLinearInequality = this ge if (rhs) Flt64.one else Flt64.zero
infix fun LinearMonomial.leq(rhs: Boolean): MathLinearInequality = this le rhs
infix fun LinearMonomial.geq(rhs: Boolean): MathLinearInequality = this ge rhs

// Flt64 vs LinearMonomial
infix fun Flt64.eq(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.EQ)
infix fun Flt64.le(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.LE)
infix fun Flt64.ge(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.GE)
infix fun Flt64.lt(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.LT)
infix fun Flt64.gt(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.GT)
infix fun Flt64.ne(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(UtilsLinearPolynomial(emptyList(), this), rhs.asPoly(), Comparison.NE)

infix fun Flt64.leq(rhs: LinearMonomial): MathLinearInequality = this le rhs
infix fun Flt64.geq(rhs: LinearMonomial): MathLinearInequality = this ge rhs
infix fun Flt64.neq(rhs: LinearMonomial): MathLinearInequality = this ne rhs
infix fun Flt64.ls(rhs: LinearMonomial): MathLinearInequality = this lt rhs
infix fun Flt64.gr(rhs: LinearMonomial): MathLinearInequality = this gt rhs

// LinearMonomial vs Symbol
infix fun LinearMonomial.eq(rhs: Symbol): MathLinearInequality = MathLinearInequality(asPoly(), rhs.asUtilsLinearPoly(), Comparison.EQ)
infix fun LinearMonomial.le(rhs: Symbol): MathLinearInequality = MathLinearInequality(asPoly(), rhs.asUtilsLinearPoly(), Comparison.LE)
infix fun LinearMonomial.ge(rhs: Symbol): MathLinearInequality = MathLinearInequality(asPoly(), rhs.asUtilsLinearPoly(), Comparison.GE)
infix fun LinearMonomial.lt(rhs: Symbol): MathLinearInequality = MathLinearInequality(asPoly(), rhs.asUtilsLinearPoly(), Comparison.LT)
infix fun LinearMonomial.gt(rhs: Symbol): MathLinearInequality = MathLinearInequality(asPoly(), rhs.asUtilsLinearPoly(), Comparison.GT)
infix fun LinearMonomial.ne(rhs: Symbol): MathLinearInequality = MathLinearInequality(asPoly(), rhs.asUtilsLinearPoly(), Comparison.NE)

infix fun LinearMonomial.leq(rhs: Symbol): MathLinearInequality = this le rhs
infix fun LinearMonomial.geq(rhs: Symbol): MathLinearInequality = this ge rhs
infix fun LinearMonomial.neq(rhs: Symbol): MathLinearInequality = this ne rhs
infix fun LinearMonomial.ls(rhs: Symbol): MathLinearInequality = this lt rhs
infix fun LinearMonomial.gr(rhs: Symbol): MathLinearInequality = this gt rhs

// Symbol vs LinearMonomial
infix fun Symbol.eq(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asPoly(), Comparison.EQ)
infix fun Symbol.le(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asPoly(), Comparison.LE)
infix fun Symbol.ge(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asPoly(), Comparison.GE)
infix fun Symbol.lt(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asPoly(), Comparison.LT)
infix fun Symbol.gt(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asPoly(), Comparison.GT)
infix fun Symbol.ne(rhs: LinearMonomial): MathLinearInequality = MathLinearInequality(asUtilsLinearPoly(), rhs.asPoly(), Comparison.NE)

infix fun Symbol.leq(rhs: LinearMonomial): MathLinearInequality = this le rhs
infix fun Symbol.geq(rhs: LinearMonomial): MathLinearInequality = this ge rhs
infix fun Symbol.neq(rhs: LinearMonomial): MathLinearInequality = this ne rhs
infix fun Symbol.ls(rhs: LinearMonomial): MathLinearInequality = this lt rhs
infix fun Symbol.gr(rhs: LinearMonomial): MathLinearInequality = this gt rhs

// AbstractVariableItem vs LinearMonomial
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

// ========== QuadraticIntermediateSymbol vs Boolean ==========

infix fun QuadraticIntermediateSymbol.eq(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toQuadraticPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.EQ)
infix fun QuadraticIntermediateSymbol.le(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toQuadraticPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.LE)
infix fun QuadraticIntermediateSymbol.ge(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toQuadraticPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.GE)
infix fun QuadraticIntermediateSymbol.leq(rhs: Boolean): MathQuadraticInequality = this le rhs
infix fun QuadraticIntermediateSymbol.geq(rhs: Boolean): MathQuadraticInequality = this ge rhs

// ========== QuadraticMonomial vs Boolean ==========

infix fun QuadraticMonomial.eq(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.EQ)
infix fun QuadraticMonomial.le(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.LE)
infix fun QuadraticMonomial.ge(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toUtilsPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.GE)
infix fun QuadraticMonomial.leq(rhs: Boolean): MathQuadraticInequality = this le rhs
infix fun QuadraticMonomial.geq(rhs: Boolean): MathQuadraticInequality = this ge rhs

// ========== UInt comparison helpers ==========

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

// Symbol vs UInt64
infix fun Symbol.leq(rhs: UInt64): MathLinearInequality = this leq rhs.toFlt64()
infix fun Symbol.geq(rhs: UInt64): MathLinearInequality = this geq rhs.toFlt64()
infix fun Symbol.eq(rhs: UInt64): MathLinearInequality = this eq rhs.toFlt64()
infix fun UInt64.eq(rhs: Symbol): MathLinearInequality = this.toFlt64() eq rhs
infix fun UInt64.leq(rhs: Symbol): MathLinearInequality = this.toFlt64() leq rhs
infix fun UInt64.geq(rhs: Symbol): MathLinearInequality = this.toFlt64() geq rhs

// ========== LinearMonomial vs UtilsQuadraticPolynomial ==========

infix fun LinearMonomial.eq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(asPoly().toQuadraticPolynomial(), rhs, Comparison.EQ)
infix fun LinearMonomial.le(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(asPoly().toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun LinearMonomial.ge(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(asPoly().toQuadraticPolynomial(), rhs, Comparison.GE)
infix fun LinearMonomial.lt(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(asPoly().toQuadraticPolynomial(), rhs, Comparison.LT)
infix fun LinearMonomial.gt(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(asPoly().toQuadraticPolynomial(), rhs, Comparison.GT)
infix fun LinearMonomial.ne(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(asPoly().toQuadraticPolynomial(), rhs, Comparison.NE)

infix fun LinearMonomial.leq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this le rhs
infix fun LinearMonomial.geq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this ge rhs
infix fun LinearMonomial.neq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this ne rhs
infix fun LinearMonomial.ls(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this lt rhs
infix fun LinearMonomial.gr(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this gt rhs

// ========== UtilsQuadraticPolynomial vs LinearMonomial ==========

infix fun UtilsQuadraticPolynomial<Flt64>.eq(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.asQuadraticPoly(), Comparison.EQ)
infix fun UtilsQuadraticPolynomial<Flt64>.le(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.asQuadraticPoly(), Comparison.LE)
infix fun UtilsQuadraticPolynomial<Flt64>.ge(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.asQuadraticPoly(), Comparison.GE)
infix fun UtilsQuadraticPolynomial<Flt64>.lt(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.asQuadraticPoly(), Comparison.LT)
infix fun UtilsQuadraticPolynomial<Flt64>.gt(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.asQuadraticPoly(), Comparison.GT)
infix fun UtilsQuadraticPolynomial<Flt64>.ne(rhs: LinearMonomial): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.asQuadraticPoly(), Comparison.NE)

infix fun UtilsQuadraticPolynomial<Flt64>.leq(rhs: LinearMonomial): MathQuadraticInequality = this le rhs
infix fun UtilsQuadraticPolynomial<Flt64>.geq(rhs: LinearMonomial): MathQuadraticInequality = this ge rhs
infix fun UtilsQuadraticPolynomial<Flt64>.neq(rhs: LinearMonomial): MathQuadraticInequality = this ne rhs
infix fun UtilsQuadraticPolynomial<Flt64>.ls(rhs: LinearMonomial): MathQuadraticInequality = this lt rhs
infix fun UtilsQuadraticPolynomial<Flt64>.gr(rhs: LinearMonomial): MathQuadraticInequality = this gt rhs

// ========== AbstractLinearPolynomial DSL (delegates to math types) ==========

infix fun AbstractLinearPolynomial<*>.eq(rhs: Boolean): MathLinearInequality =
    toLinearPolynomial() eq if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.le(rhs: Boolean): MathLinearInequality =
    toLinearPolynomial() le if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.ge(rhs: Boolean): MathLinearInequality =
    toLinearPolynomial() ge if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.lt(rhs: Boolean): MathLinearInequality =
    toLinearPolynomial() lt if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.gt(rhs: Boolean): MathLinearInequality =
    toLinearPolynomial() gt if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.ne(rhs: Boolean): MathLinearInequality =
    toLinearPolynomial() ne if (rhs) Flt64.one else Flt64.zero
infix fun AbstractLinearPolynomial<*>.leq(rhs: Boolean): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: Boolean): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: Boolean): MathLinearInequality = this ne rhs
infix fun AbstractLinearPolynomial<*>.ls(rhs: Boolean): MathLinearInequality = this lt rhs
infix fun AbstractLinearPolynomial<*>.gr(rhs: Boolean): MathLinearInequality = this gt rhs

infix fun AbstractLinearPolynomial<*>.eq(rhs: Flt64): MathLinearInequality =
    toLinearPolynomial() eq rhs
infix fun AbstractLinearPolynomial<*>.le(rhs: Flt64): MathLinearInequality =
    toLinearPolynomial() le rhs
infix fun AbstractLinearPolynomial<*>.ge(rhs: Flt64): MathLinearInequality =
    toLinearPolynomial() ge rhs
infix fun AbstractLinearPolynomial<*>.lt(rhs: Flt64): MathLinearInequality =
    toLinearPolynomial() lt rhs
infix fun AbstractLinearPolynomial<*>.gt(rhs: Flt64): MathLinearInequality =
    toLinearPolynomial() gt rhs
infix fun AbstractLinearPolynomial<*>.ne(rhs: Flt64): MathLinearInequality =
    toLinearPolynomial() ne rhs
infix fun AbstractLinearPolynomial<*>.leq(rhs: Flt64): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: Flt64): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: Flt64): MathLinearInequality = this ne rhs

infix fun AbstractLinearPolynomial<*>.eq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    toLinearPolynomial() eq rhs
infix fun AbstractLinearPolynomial<*>.le(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    toLinearPolynomial() le rhs
infix fun AbstractLinearPolynomial<*>.ge(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    toLinearPolynomial() ge rhs
infix fun AbstractLinearPolynomial<*>.ne(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality =
    toLinearPolynomial() ne rhs
infix fun AbstractLinearPolynomial<*>.leq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this le rhs
infix fun AbstractLinearPolynomial<*>.geq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this ge rhs
infix fun AbstractLinearPolynomial<*>.neq(rhs: UtilsLinearPolynomial<Flt64>): MathLinearInequality = this ne rhs

// ========== AbstractQuadraticPolynomial DSL (delegates to math types) ==========

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: Boolean): MathQuadraticInequality =
    toQuadraticPolynomial() eq if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.le(rhs: Boolean): MathQuadraticInequality =
    toQuadraticPolynomial() le if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.ge(rhs: Boolean): MathQuadraticInequality =
    toQuadraticPolynomial() ge if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.lt(rhs: Boolean): MathQuadraticInequality =
    toQuadraticPolynomial() lt if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.gt(rhs: Boolean): MathQuadraticInequality =
    toQuadraticPolynomial() gt if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.ne(rhs: Boolean): MathQuadraticInequality =
    toQuadraticPolynomial() ne if (rhs) Flt64.one else Flt64.zero
infix fun AbstractQuadraticPolynomial<*>.leq(rhs: Boolean): MathQuadraticInequality = this le rhs
infix fun AbstractQuadraticPolynomial<*>.geq(rhs: Boolean): MathQuadraticInequality = this ge rhs
infix fun AbstractQuadraticPolynomial<*>.neq(rhs: Boolean): MathQuadraticInequality = this ne rhs

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: Flt64): MathQuadraticInequality =
    toQuadraticPolynomial() eq rhs
infix fun AbstractQuadraticPolynomial<*>.le(rhs: Flt64): MathQuadraticInequality =
    toQuadraticPolynomial() le rhs
infix fun AbstractQuadraticPolynomial<*>.ge(rhs: Flt64): MathQuadraticInequality =
    toQuadraticPolynomial() ge rhs
infix fun AbstractQuadraticPolynomial<*>.lt(rhs: Flt64): MathQuadraticInequality =
    toQuadraticPolynomial() lt rhs
infix fun AbstractQuadraticPolynomial<*>.gt(rhs: Flt64): MathQuadraticInequality =
    toQuadraticPolynomial() gt rhs
infix fun AbstractQuadraticPolynomial<*>.ne(rhs: Flt64): MathQuadraticInequality =
    toQuadraticPolynomial() ne rhs
infix fun AbstractQuadraticPolynomial<*>.leq(rhs: Flt64): MathQuadraticInequality = this le rhs
infix fun AbstractQuadraticPolynomial<*>.geq(rhs: Flt64): MathQuadraticInequality = this ge rhs
infix fun AbstractQuadraticPolynomial<*>.neq(rhs: Flt64): MathQuadraticInequality = this ne rhs

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    toQuadraticPolynomial() eq rhs
infix fun AbstractQuadraticPolynomial<*>.le(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    toQuadraticPolynomial() le rhs
infix fun AbstractQuadraticPolynomial<*>.ge(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    toQuadraticPolynomial() ge rhs
infix fun AbstractQuadraticPolynomial<*>.ne(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality =
    toQuadraticPolynomial() ne rhs
infix fun AbstractQuadraticPolynomial<*>.leq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this le rhs
infix fun AbstractQuadraticPolynomial<*>.geq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this ge rhs
infix fun AbstractQuadraticPolynomial<*>.neq(rhs: UtilsQuadraticPolynomial<Flt64>): MathQuadraticInequality = this ne rhs
