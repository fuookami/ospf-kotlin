@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.eq
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial

/**
 * Convert LinearIntermediateSymbol to math LinearPolynomial<Flt64>.
 * toLinearPolynomial() already returns the math type directly.
 */
@Suppress("UNCHECKED_CAST")
fun LinearIntermediateSymbolF64.asMathLinearPolynomial(): MathLinearPolynomial<Flt64> {
    return (this as LinearIntermediateSymbolF64).toMathLinearPolynomial()
}

/**
 * Convert math LinearPolynomial<Flt64> to core expression LinearMonomial list + constant.
 * Used when framework code needs to construct core polynomials from math types
 * (e.g., extracting slack.pos/neg from new SlackFunction).
 */
fun MathLinearPolynomial<Flt64>.asCoreLinearPolynomial(): MathLinearPolynomial<Flt64> {
    return this
}

/**
 * Convert star-projected math LinearPolynomial<*> to core expression MathLinearPolynomial<Flt64>.
 * Needed when working with SlackFunction<*> where the type parameter is unknown.
 */
@Suppress("UNCHECKED_CAST")
@JvmName("asCoreLinearPolynomialStar")
fun MathLinearPolynomial<*>.asCoreLinearPolynomial(): MathLinearPolynomial<Flt64> {
    return this as MathLinearPolynomial<Flt64>
}

