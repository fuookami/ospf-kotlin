@file:Suppress("unused", "EXTENSION_SHADOWED_BY_MEMBER")

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
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.math.symbol.operation.normalize
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticInequality
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Quadratic
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

@Suppress("UNCHECKED_CAST")
private fun <V, T> tokenTableAs(tokens: AbstractTokenTable<T>): AbstractTokenTable<V>
        where V : RealNumber<V>, V : NumberField<V>, T : RealNumber<T>, T : NumberField<T> {
    return tokens as AbstractTokenTable<V>
}

// ========== Flt64 convenience aliases ==========

// The math layer provides generic infix operators (eq/le/ge/lt/gt/ne)
// for LinearPolynomial<T>, QuadraticPolynomial<T>, Symbol, and Flt64.
// This file adds convenience aliases (leq/geq/neq/ls/gr) and
// core-specific cross-type operators (Int/Double/Boolean/UInt64).

// LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> aliases �?delegate directly to constructor to avoid
// resolution ambiguity with math-layer generic operators.
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.leq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs, Comparison.LE)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.geq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs, Comparison.GE)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.neq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs, Comparison.NE)

infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.eq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.EQ)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.leq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.geq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.neq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, LinearPolynomial(emptyList(), rhs), Comparison.NE)

infix fun Flt64.eq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.EQ)
infix fun Flt64.leq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.LE)
infix fun Flt64.geq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.GE)
infix fun Flt64.neq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this), rhs, Comparison.NE)

// QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> aliases
infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.leq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs, Comparison.LE)
infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.geq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs, Comparison.GE)
infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.neq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs, Comparison.NE)

infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.leq(rhs: Flt64): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, QuadraticPolynomial(emptyList(), rhs), Comparison.LE)
infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.geq(rhs: Flt64): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, QuadraticPolynomial(emptyList(), rhs), Comparison.GE)
infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.neq(rhs: Flt64): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, QuadraticPolynomial(emptyList(), rhs), Comparison.NE)

// LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> vs QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> aliases
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.leq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this.toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.geq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this.toQuadraticPolynomial(), rhs, Comparison.GE)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.neq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this.toQuadraticPolynomial(), rhs, Comparison.NE)

infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.leq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.toQuadraticPolynomial(), Comparison.LE)
infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.geq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.toQuadraticPolynomial(), Comparison.GE)
infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.neq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.toQuadraticPolynomial(), Comparison.NE)
infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.ls(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.toQuadraticPolynomial(), Comparison.LT)
infix fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.gr(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.toQuadraticPolynomial(), Comparison.GT)

// ========== Symbol convenience aliases ==========

// Symbol vs Flt64 aliases
infix fun Symbol.leq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun Symbol.geq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun Symbol.neq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.NE)
infix fun Symbol.ls(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LT)
infix fun Symbol.gr(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GT)

// Flt64 vs Symbol aliases
infix fun Flt64.leq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.LE)
infix fun Flt64.geq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.GE)
infix fun Flt64.neq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.NE)
infix fun Flt64.ls(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.LT)
infix fun Flt64.gr(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this), rhs.asLinearPoly(), Comparison.GT)

// Symbol vs Symbol aliases
infix fun Symbol.leq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.LE)
infix fun Symbol.geq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.GE)
infix fun Symbol.neq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.NE)
infix fun Symbol.ls(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.LT)
infix fun Symbol.gr(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs.asLinearPoly(), Comparison.GT)

// ========== Symbol vs Int/Double ==========

infix fun Symbol.eq(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.EQ)
infix fun Symbol.le(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LE)
infix fun Symbol.ge(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GE)
infix fun Symbol.lt(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LT)
infix fun Symbol.gt(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GT)
infix fun Symbol.ne(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.NE)

infix fun Symbol.eq(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.EQ)
infix fun Symbol.le(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LE)
infix fun Symbol.ge(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GE)
infix fun Symbol.lt(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LT)
infix fun Symbol.gt(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GT)
infix fun Symbol.ne(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.NE)

// Int/Double vs Symbol
infix fun Int.eq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.EQ)
infix fun Int.le(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.LE)
infix fun Int.ge(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.GE)
infix fun Int.lt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.LT)
infix fun Int.gt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.GT)

infix fun Double.eq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.EQ)
infix fun Double.le(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.LE)
infix fun Double.ge(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.GE)
infix fun Double.lt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.LT)
infix fun Double.gt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), Flt64(this)), rhs.asLinearPoly(), Comparison.GT)

// Int/Double aliases
infix fun Symbol.leq(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LE)
infix fun Symbol.geq(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GE)
infix fun Symbol.neq(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.NE)
infix fun Symbol.ls(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LT)
infix fun Symbol.gr(rhs: Int): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GT)

infix fun Symbol.leq(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LE)
infix fun Symbol.geq(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GE)
infix fun Symbol.neq(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.NE)
infix fun Symbol.ls(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.LT)
infix fun Symbol.gr(rhs: Double): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), Flt64(rhs)), Comparison.GT)

// ========== Symbol vs LinearPolynomial/QuadraticPolynomial ==========

// Symbol vs LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>
private fun Symbol.asLinearPoly(): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> =
    LinearPolynomial(listOf(LinearMonomial(Flt64.one, this)), Flt64.zero)

infix fun Symbol.eq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.EQ)
infix fun Symbol.le(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.LE)
infix fun Symbol.ge(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.GE)
infix fun Symbol.lt(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.LT)
infix fun Symbol.gt(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.GT)
infix fun Symbol.ne(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.NE)

infix fun Symbol.leq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.LE)
infix fun Symbol.geq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.GE)
infix fun Symbol.neq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.NE)
infix fun Symbol.ls(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.LT)
infix fun Symbol.gr(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), rhs, Comparison.GT)

// LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> vs Symbol
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.eq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.asLinearPoly(), Comparison.EQ)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.le(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.asLinearPoly(), Comparison.LE)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.ge(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.asLinearPoly(), Comparison.GE)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.lt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.asLinearPoly(), Comparison.LT)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.gt(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.asLinearPoly(), Comparison.GT)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.ne(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.asLinearPoly(), Comparison.NE)

infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.leq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.asLinearPoly(), Comparison.LE)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.geq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.asLinearPoly(), Comparison.GE)
infix fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.neq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(this, rhs.asLinearPoly(), Comparison.NE)

// Symbol vs QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>
infix fun Symbol.eq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.EQ)
infix fun Symbol.le(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun Symbol.ge(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.GE)
infix fun Symbol.ne(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.NE)

infix fun Symbol.leq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun Symbol.geq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.GE)
infix fun Symbol.neq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly().toQuadraticPolynomial(), rhs, Comparison.NE)

// ========== Symbol vs Boolean ==========

private fun Boolean.asFlt64(): Flt64 = if (this) Flt64.one else Flt64.zero

infix fun Symbol.eq(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.EQ)
infix fun Symbol.le(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)
infix fun Symbol.ge(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)
infix fun Symbol.lt(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)
infix fun Symbol.gt(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)
infix fun Symbol.ne(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)

infix fun Symbol.leq(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)
infix fun Symbol.geq(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)
infix fun Symbol.neq(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)
infix fun Symbol.ls(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)
infix fun Symbol.gr(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)

// ========== AbstractVariableItem DSL ==========

private fun AbstractVariableItem<*, *>.asSymbolPoly(): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> =
    LinearPolynomial(listOf(LinearMonomial(Flt64.one, this as Symbol)), Flt64.zero)

infix fun AbstractVariableItem<*, *>.leq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun AbstractVariableItem<*, *>.geq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun AbstractVariableItem<*, *>.eq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.EQ)
infix fun AbstractVariableItem<*, *>.neq(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.NE)
infix fun AbstractVariableItem<*, *>.ls(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GT)
infix fun AbstractVariableItem<*, *>.le(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LE)
infix fun AbstractVariableItem<*, *>.ge(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GE)
infix fun AbstractVariableItem<*, *>.lt(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gt(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.GT)
infix fun AbstractVariableItem<*, *>.ne(rhs: Flt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs), Comparison.NE)

infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractVariableItem<*, *>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.LE)
infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractVariableItem<*, *>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.GE)
infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractVariableItem<*, *>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.EQ)
infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractVariableItem<*, *>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.NE)
infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractVariableItem<*, *>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractVariableItem<*, *>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs.asSymbolPoly(), Comparison.GT)

// AbstractVariableItem vs LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>
infix fun AbstractVariableItem<*, *>.leq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs, Comparison.LE)
infix fun AbstractVariableItem<*, *>.geq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs, Comparison.GE)
infix fun AbstractVariableItem<*, *>.eq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs, Comparison.EQ)
infix fun AbstractVariableItem<*, *>.neq(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs, Comparison.NE)
infix fun AbstractVariableItem<*, *>.ls(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs, Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), rhs, Comparison.GT)

// AbstractVariableItem vs QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>
infix fun AbstractVariableItem<*, *>.leq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun AbstractVariableItem<*, *>.geq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.GE)
infix fun AbstractVariableItem<*, *>.eq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.EQ)
infix fun AbstractVariableItem<*, *>.neq(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.NE)
infix fun AbstractVariableItem<*, *>.ls(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> =
    QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(QuadraticPolynomial(listOf(QuadraticMonomial(Flt64.one, this, this)), Flt64.zero), rhs, Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> =
    QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(QuadraticPolynomial(listOf(QuadraticMonomial(Flt64.one, this, this)), Flt64.zero), rhs, Comparison.GT)
infix fun AbstractVariableItem<*, *>.le(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.LE)
infix fun AbstractVariableItem<*, *>.ge(rhs: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly().toQuadraticPolynomial(), rhs, Comparison.GE)

// AbstractVariableItem vs Boolean
infix fun AbstractVariableItem<*, *>.eq(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.EQ)
infix fun AbstractVariableItem<*, *>.le(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)
infix fun AbstractVariableItem<*, *>.ge(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)
infix fun AbstractVariableItem<*, *>.lt(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gt(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)
infix fun AbstractVariableItem<*, *>.ne(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)
infix fun AbstractVariableItem<*, *>.leq(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LE)
infix fun AbstractVariableItem<*, *>.geq(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GE)
infix fun AbstractVariableItem<*, *>.neq(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.NE)
infix fun AbstractVariableItem<*, *>.ls(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.LT)
infix fun AbstractVariableItem<*, *>.gr(rhs: Boolean): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asSymbolPoly(), LinearPolynomial(emptyList(), rhs.asFlt64()), Comparison.GT)

// ========== QuadraticIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64> vs Boolean ==========

infix fun QuadraticIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>.eq(rhs: Boolean): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> =
    QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(toQuadraticPolynomial(), QuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.EQ)
infix fun QuadraticIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>.le(rhs: Boolean): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> =
    QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(toQuadraticPolynomial(), QuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.LE)
infix fun QuadraticIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>.ge(rhs: Boolean): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> =
    QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(toQuadraticPolynomial(), QuadraticPolynomial(emptyList(), if (rhs) Flt64.one else Flt64.zero), Comparison.GE)
infix fun QuadraticIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>.leq(rhs: Boolean): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = this le rhs
infix fun QuadraticIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>.geq(rhs: Boolean): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> = this ge rhs

// ========== UInt comparison helpers ==========

infix fun UInt8.geq(rhs: UInt8): Boolean = this >= rhs
infix fun UInt64.geq(rhs: UInt64): Boolean = this >= rhs
infix fun UInt8.eq(rhs: UInt8): Boolean = this == rhs
infix fun UInt64.eq(rhs: UInt64): Boolean = this == rhs
infix fun UInt8.neq(rhs: UInt64): Boolean = this.toUInt64().toLong() != rhs.toLong()
infix fun UInt64.neq(rhs: UInt64): Boolean = this != rhs
infix fun UInt64.eq(rhs: Flt64): Boolean = this.toFlt64() == rhs
infix fun UInt64.neq(rhs: Flt64): Boolean = this.toFlt64() != rhs
infix fun Flt64.eq(rhs: UInt64): Boolean = this == rhs.toFlt64()
infix fun Flt64.neq(rhs: UInt64): Boolean = this != rhs.toFlt64()

// Symbol vs UInt64
infix fun Symbol.leq(rhs: UInt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.toFlt64()), Comparison.LE)
infix fun Symbol.geq(rhs: UInt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.toFlt64()), Comparison.GE)
infix fun Symbol.eq(rhs: UInt64): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(asLinearPoly(), LinearPolynomial(emptyList(), rhs.toFlt64()), Comparison.EQ)
infix fun UInt64.eq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this.toFlt64()), rhs.asLinearPoly(), Comparison.EQ)
infix fun UInt64.leq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this.toFlt64()), rhs.asLinearPoly(), Comparison.LE)
infix fun UInt64.geq(rhs: Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> = LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(LinearPolynomial(emptyList(), this.toFlt64()), rhs.asLinearPoly(), Comparison.GE)

// ========== LinearInequality to Constraint<Flt64, Quadratic> direct conversion ==========

internal fun <T> LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toQuadraticConstraint(
    tokens: AbstractTokenTable<T>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): QuadraticConstraintImpl<fuookami.ospf.kotlin.math.algebra.number.Flt64> where T : RealNumber<T>, T : NumberField<T> {
    return QuadraticConstraintImpl(
        relation = toQuadraticInequality().let { QuadraticRelationImpl(it.flattenData, it.comparison) },
        tokens = tokenTableAs<Flt64, T>(tokens),
        converter = IntoValue.Identity,
        lazy = lazy,
        name = name,
        origin = origin,
        from = from
    )
}

// ========== Relation-based constraint creation ==========

internal fun <V> LinearRelation<V>.toConstraint(
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): LinearConstraintImpl<V> where V : RealNumber<V>, V : NumberField<V> {
    return LinearConstraintImpl(this, tokens, converter, lazy, name, origin, from)
}

internal fun <V> QuadraticRelation<V>.toConstraint(
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
): QuadraticConstraintImpl<V> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticConstraintImpl(this, tokens, converter, lazy, name, origin, from)
}

internal fun <V> LinearRelation<V>.toQuadraticConstraint(
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
    val qFlattenData = QuadraticFlattenData<V>(qMonomials, normalized.flattenData.constant)
    val qRelation = QuadraticRelationImpl(qFlattenData, normalized.sign, normalized.name, normalized.displayName)
    return QuadraticConstraintImpl(qRelation, tokens, converter, lazy, name, origin, from)
}