@file:Suppress("unused")

package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.UInt8
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.combineTerms
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.normalize
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.toQuadraticInequality
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Quadratic
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

// ========== Flt64 convenience aliases ==========

// The math layer provides generic infix operators (eq/le/ge/lt/gt/ne)
// for LinearPolynomial<T>, QuadraticPolynomial<T>, Symbol, and Flt64.
// This file adds convenience aliases (leq/geq/neq/ls/gr) and
// core-specific cross-type operators (Int/Double/Boolean/UInt64).

// LinearPolynomial<Flt64> aliases — delegate directly to constructor to avoid
// resolution ambiguity with math-layer generic operators.
infix fun LinearPolynomial<Flt64>.leq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs, Comparison.LE)
infix fun LinearPolynomial<Flt64>.geq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs, Comparison.GE)
infix fun LinearPolynomial<Flt64>.neq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs, Comparison.NE)

infix fun LinearPolynomial<Flt64>.eq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.EQ)
infix fun LinearPolynomial<Flt64>.leq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun LinearPolynomial<Flt64>.geq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun LinearPolynomial<Flt64>.neq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.NE)

infix fun Flt64.eq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.EQ)
infix fun Flt64.leq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.LE)
infix fun Flt64.geq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.GE)
infix fun Flt64.neq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.NE)

// QuadraticPolynomial<Flt64> aliases
infix fun QuadraticPolynomial<Flt64>.leq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.LE)
infix fun QuadraticPolynomial<Flt64>.geq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.GE)
infix fun QuadraticPolynomial<Flt64>.neq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs, Comparison.NE)

infix fun QuadraticPolynomial<Flt64>.leq(rhs: Flt64): QuadraticInequality = QuadraticInequality(this, QuadraticPolynomial(emptyList(), rhs), Comparison.LE)
infix fun QuadraticPolynomial<Flt64>.geq(rhs: Flt64): QuadraticInequality = QuadraticInequality(this, QuadraticPolynomial(emptyList(), rhs), Comparison.GE)
infix fun QuadraticPolynomial<Flt64>.neq(rhs: Flt64): QuadraticInequality = QuadraticInequality(this, QuadraticPolynomial(emptyList(), rhs), Comparison.NE)

// LinearPolynomial<Flt64> vs QuadraticPolynomial<Flt64> aliases
infix fun LinearPolynomial<Flt64>.leq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this.toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun LinearPolynomial<Flt64>.geq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this.toQuadraticPolynomial(), rhs, Comparison.GE)
infix fun LinearPolynomial<Flt64>.neq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this.toQuadraticPolynomial(), rhs, Comparison.NE)

infix fun QuadraticPolynomial<Flt64>.leq(rhs: LinearPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.LE)
infix fun QuadraticPolynomial<Flt64>.geq(rhs: LinearPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.GE)
infix fun QuadraticPolynomial<Flt64>.neq(rhs: LinearPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.NE)
infix fun QuadraticPolynomial<Flt64>.ls(rhs: LinearPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.LT)
infix fun QuadraticPolynomial<Flt64>.gr(rhs: LinearPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(this, rhs.toQuadraticPolynomial(), Comparison.GT)

// ========== Symbol convenience aliases ==========

// Symbol vs Flt64 aliases
infix fun Symbol.leq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun Symbol.geq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun Symbol.neq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.NE)
infix fun Symbol.ls(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LT)
infix fun Symbol.gr(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GT)

// Flt64 vs Symbol aliases
infix fun Flt64.leq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.LE)
infix fun Flt64.geq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.GE)
infix fun Flt64.neq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.NE)
infix fun Flt64.ls(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.LT)
infix fun Flt64.gr(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.GT)

// Symbol vs Symbol aliases
infix fun Symbol.leq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.LE)
infix fun Symbol.geq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.GE)
infix fun Symbol.neq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.NE)
infix fun Symbol.ls(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.LT)
infix fun Symbol.gr(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.GT)

// ========== Symbol vs Int/Double ==========

infix fun Symbol.eq(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.EQ)
infix fun Symbol.le(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.LE)
infix fun Symbol.ge(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.GE)
infix fun Symbol.lt(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.LT)
infix fun Symbol.gt(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.GT)
infix fun Symbol.ne(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.NE)

infix fun Symbol.eq(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.EQ)
infix fun Symbol.le(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LE)
infix fun Symbol.ge(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GE)
infix fun Symbol.lt(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LT)
infix fun Symbol.gt(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GT)
infix fun Symbol.ne(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.NE)

// Int/Double vs Symbol
infix fun Int.eq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this.toDouble())), rhs.asLinearPoly(), Comparison.EQ)
infix fun Int.le(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this.toDouble())), rhs.asLinearPoly(), Comparison.LE)
infix fun Int.ge(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this.toDouble())), rhs.asLinearPoly(), Comparison.GE)
infix fun Int.lt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this.toDouble())), rhs.asLinearPoly(), Comparison.LT)
infix fun Int.gt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this.toDouble())), rhs.asLinearPoly(), Comparison.GT)

infix fun Double.eq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.EQ)
infix fun Double.le(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.LE)
infix fun Double.ge(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.GE)
infix fun Double.lt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.LT)
infix fun Double.gt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.GT)

// Int/Double aliases
infix fun Symbol.leq(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.LE)
infix fun Symbol.geq(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.GE)
infix fun Symbol.neq(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.NE)
infix fun Symbol.ls(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.LT)
infix fun Symbol.gr(rhs: Int): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs.toDouble())), Comparison.GT)

infix fun Symbol.leq(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LE)
infix fun Symbol.geq(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GE)
infix fun Symbol.neq(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.NE)
infix fun Symbol.ls(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LT)
infix fun Symbol.gr(rhs: Double): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GT)

// ========== Symbol vs LinearPolynomial/QuadraticPolynomial ==========

// Symbol vs LinearPolynomial<Flt64>
private fun Symbol.asLinearPoly(): LinearPolynomial<Flt64> =
    LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64.zero)

infix fun Symbol.eq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.EQ)
infix fun Symbol.le(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.LE)
infix fun Symbol.ge(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.GE)
infix fun Symbol.lt(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.LT)
infix fun Symbol.gt(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.GT)
infix fun Symbol.ne(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.NE)

infix fun Symbol.leq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.LE)
infix fun Symbol.geq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.GE)
infix fun Symbol.neq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.NE)
infix fun Symbol.ls(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.LT)
infix fun Symbol.gr(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), rhs, Comparison.GT)

// LinearPolynomial<Flt64> vs Symbol
infix fun LinearPolynomial<Flt64>.eq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.EQ)
infix fun LinearPolynomial<Flt64>.le(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.LE)
infix fun LinearPolynomial<Flt64>.ge(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.GE)
infix fun LinearPolynomial<Flt64>.lt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.LT)
infix fun LinearPolynomial<Flt64>.gt(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.GT)
infix fun LinearPolynomial<Flt64>.ne(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.NE)

infix fun LinearPolynomial<Flt64>.leq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.LE)
infix fun LinearPolynomial<Flt64>.geq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.GE)
infix fun LinearPolynomial<Flt64>.neq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(this, rhs.asLinearPoly(), Comparison.NE)

// Symbol vs QuadraticPolynomial<Flt64>
infix fun Symbol.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.EQ)
infix fun Symbol.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun Symbol.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.GE)
infix fun Symbol.ne(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.NE)

infix fun Symbol.leq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun Symbol.geq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.GE)
infix fun Symbol.neq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.NE)

// ========== Symbol vs Boolean ==========

private fun Boolean.asFlt64(): Flt64 = if (this) Flt64.one else Flt64.zero

infix fun Symbol.eq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.EQ)
infix fun Symbol.le(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)
infix fun Symbol.ge(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)
infix fun Symbol.lt(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)
infix fun Symbol.gt(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)
infix fun Symbol.ne(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)

infix fun Symbol.leq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)
infix fun Symbol.geq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)
infix fun Symbol.neq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)
infix fun Symbol.ls(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)
infix fun Symbol.gr(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)

// ========== AbstractVariableItem DSL ==========

private fun AbstractVariableItem<*, *>.asSymbolPoly(): LinearPolynomial<Flt64> =
    LinearPolynomial(listOf(LinearMonomial(Flt64.one, this as Symbol)), Flt64.zero)

infix fun AbstractVariableItem<*, *>.leq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun AbstractVariableItem<*, *>.geq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun AbstractVariableItem<*, *>.eq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.EQ)
infix fun AbstractVariableItem<*, *>.neq(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.NE)
infix fun AbstractVariableItem<*, *>.ls(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GT)
infix fun AbstractVariableItem<*, *>.le(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun AbstractVariableItem<*, *>.ge(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun AbstractVariableItem<*, *>.lt(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gt(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GT)
infix fun AbstractVariableItem<*, *>.ne(rhs: Flt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.NE)

infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.LE)
infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.GE)
infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.EQ)
infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.NE)
infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractVariableItem<*, *>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.GT)

// AbstractVariableItem vs LinearPolynomial<Flt64>
infix fun AbstractVariableItem<*, *>.leq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.LE)
infix fun AbstractVariableItem<*, *>.geq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.GE)
infix fun AbstractVariableItem<*, *>.eq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.EQ)
infix fun AbstractVariableItem<*, *>.neq(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.NE)
infix fun AbstractVariableItem<*, *>.ls(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: LinearPolynomial<Flt64>): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), rhs, Comparison.GT)

// AbstractVariableItem vs QuadraticPolynomial<Flt64>
infix fun AbstractVariableItem<*, *>.leq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun AbstractVariableItem<*, *>.geq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.GE)
infix fun AbstractVariableItem<*, *>.eq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.EQ)
infix fun AbstractVariableItem<*, *>.neq(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.NE)
infix fun AbstractVariableItem<*, *>.ls(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality =
    QuadraticInequality(QuadraticPolynomial(listOf(QuadraticMonomial(Flt64.one, this, this)), Flt64.zero), rhs, Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality =
    QuadraticInequality(QuadraticPolynomial(listOf(QuadraticMonomial(Flt64.one, this, this)), Flt64.zero), rhs, Comparison.GT)
infix fun AbstractVariableItem<*, *>.le(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun AbstractVariableItem<*, *>.ge(rhs: QuadraticPolynomial<Flt64>): QuadraticInequality = QuadraticInequality(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.GE)

// AbstractVariableItem vs Boolean
infix fun AbstractVariableItem<*, *>.eq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.EQ)
infix fun AbstractVariableItem<*, *>.le(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)
infix fun AbstractVariableItem<*, *>.ge(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)
infix fun AbstractVariableItem<*, *>.lt(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gt(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)
infix fun AbstractVariableItem<*, *>.ne(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)
infix fun AbstractVariableItem<*, *>.leq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)
infix fun AbstractVariableItem<*, *>.geq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)
infix fun AbstractVariableItem<*, *>.neq(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)
infix fun AbstractVariableItem<*, *>.ls(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: Boolean): LinearInequality<Flt64> = LinearInequality<Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)

// ========== QuadraticIntermediateSymbol<Flt64> vs Boolean ==========

infix fun QuadraticIntermediateSymbol<Flt64>.eq(rhs: Boolean): QuadraticInequality =
    QuadraticInequality(toQuadraticPolynomial(), QuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.EQ)
infix fun QuadraticIntermediateSymbol<Flt64>.le(rhs: Boolean): QuadraticInequality =
    QuadraticInequality(toQuadraticPolynomial(), QuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.LE)
infix fun QuadraticIntermediateSymbol<Flt64>.ge(rhs: Boolean): QuadraticInequality =
    QuadraticInequality(toQuadraticPolynomial(), QuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.GE)
infix fun QuadraticIntermediateSymbol<Flt64>.leq(rhs: Boolean): QuadraticInequality = this le rhs
infix fun QuadraticIntermediateSymbol<Flt64>.geq(rhs: Boolean): QuadraticInequality = this ge rhs

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
infix fun Symbol.leq(rhs: UInt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.toFlt64()), Comparison.LE)
infix fun Symbol.geq(rhs: UInt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.toFlt64()), Comparison.GE)
infix fun Symbol.eq(rhs: UInt64): LinearInequality<Flt64> = LinearInequality<Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.toFlt64()), Comparison.EQ)
infix fun UInt64.eq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this.toFlt64()), rhs.asLinearPoly(), Comparison.EQ)
infix fun UInt64.leq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this.toFlt64()), rhs.asLinearPoly(), Comparison.LE)
infix fun UInt64.geq(rhs: Symbol): LinearInequality<Flt64> = LinearInequality<Flt64>(LinearPolynomial(emptyList(), this.toFlt64()), rhs.asLinearPoly(), Comparison.GE)

// ========== LinearInequality to Constraint<Flt64, Quadratic> direct conversion ==========

fun <V> LinearInequality<Flt64>.toQuadraticConstraint(
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): QuadraticConstraintImpl<V> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticConstraintImpl(
        relation = toQuadraticInequality().let { QuadraticRelationImpl(it.flattenData, it.comparison) },
        tokens = tokens,
        converter = converter,
        lazy = lazy,
        name = name,
        origin = origin,
        from = from
    )
}

// ========== Relation-based constraint creation ==========

fun <V> LinearRelation.toConstraint(
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): LinearConstraintImpl<V> where V : RealNumber<V>, V : NumberField<V> {
    return LinearConstraintImpl(this, tokens, converter, lazy, name, origin, from)
}

fun <V> QuadraticRelation.toConstraint(
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): QuadraticConstraintImpl<V> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticConstraintImpl(this, tokens, converter, lazy, name, origin, from)
}

fun <V> LinearRelation.toQuadraticConstraint(
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): QuadraticConstraintImpl<V> where V : RealNumber<V>, V : NumberField<V> {
    val normalized = normalize()
    val qMonomials = normalized.flattenData.monomials.map {
        QuadraticMonomial(it.coefficient, it.symbol, null)
    }
    val qFlattenData = QuadraticFlattenData<Flt64>(qMonomials, normalized.flattenData.constant)
    val qRelation = QuadraticRelationImpl(qFlattenData, normalized.sign, normalized.name, normalized.displayName)
    return QuadraticConstraintImpl(qRelation, tokens, converter, lazy, name, origin, from)
}