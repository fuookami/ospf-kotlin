package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataF64
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
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
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticInequality
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
infix fun UtilsQuadraticPolynomial<Flt64>.lt(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.LT)
infix fun UtilsQuadraticPolynomial<Flt64>.gt(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality =
    MathQuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.GT)
infix fun UtilsQuadraticPolynomial<Flt64>.ls(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality = this lt rhs
infix fun UtilsQuadraticPolynomial<Flt64>.gr(rhs: UtilsLinearPolynomial<Flt64>): MathQuadraticInequality = this gt rhs

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

// ========== QuadraticIntermediateSymbol<*> vs Boolean ==========

infix fun QuadraticIntermediateSymbol<*>.eq(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toMathQuadraticPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.EQ)
infix fun QuadraticIntermediateSymbol<*>.le(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toMathQuadraticPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.LE)
infix fun QuadraticIntermediateSymbol<*>.ge(rhs: Boolean): MathQuadraticInequality =
    MathQuadraticInequality(toMathQuadraticPolynomial(), UtilsQuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.GE)
infix fun QuadraticIntermediateSymbol<*>.leq(rhs: Boolean): MathQuadraticInequality = this le rhs
infix fun QuadraticIntermediateSymbol<*>.geq(rhs: Boolean): MathQuadraticInequality = this ge rhs

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

// ========== LinearInequality to QuadraticConstraint direct conversion ==========

fun MathLinearInequality.toQuadraticConstraint(
    tokens: AbstractTokenTable<*>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): QuadraticConstraintImpl {
    return QuadraticConstraintImpl(
        relation = toQuadraticInequality().let { QuadraticRelationImpl(it.flattenData, it.comparison) },
        tokens = tokens,
        lazy = lazy,
        name = name,
        origin = origin,
        from = from
    )
}

// ========== Relation-based constraint creation ==========

fun LinearRelation.toConstraint(
    tokens: AbstractTokenTable<*>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): LinearConstraintImpl {
    return LinearConstraintImpl(this, tokens, lazy, name, origin, from)
}

fun QuadraticRelation.toConstraint(
    tokens: AbstractTokenTable<*>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): QuadraticConstraintImpl {
    return QuadraticConstraintImpl(this, tokens, lazy, name, origin, from)
}

fun LinearRelation.toQuadraticConstraint(
    tokens: AbstractTokenTable<*>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): QuadraticConstraintImpl {
    val normalized = normalize()
    val qMonomials = normalized.flattenData.monomials.map {
        UtilsQuadraticMonomial(it.coefficient, it.symbol, null)
    }
    val qFlattenData = QuadraticFlattenDataF64(qMonomials, normalized.flattenData.constant)
    val qRelation = QuadraticRelationImpl(qFlattenData, normalized.sign, normalized.name, normalized.displayName)
    return QuadraticConstraintImpl(qRelation, tokens, lazy, name, origin, from)
}
