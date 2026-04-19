@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_model.*
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
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
fun LinearIntermediateSymbol<*>.asMathLinearPolynomial(): MathLinearPolynomial<Flt64> {
    return (this as LinearIntermediateSymbol<Flt64>).toLinearPolynomial()
}

/**
 * Convert core expression LinearMonomial to math LinearMonomial<Flt64>.
 */
fun LinearMonomial.asMathLinearMonomial(): MathLinearMonomial<Flt64> {
    return MathLinearMonomial(this.coefficient, this.symbol as Symbol)
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

/**
 * Convert math LinearMonomial<Flt64> to core expression LinearMonomial.
 */
fun MathLinearMonomial<Flt64>.asCoreLinearMonomial(): LinearMonomial {
    val coreSymbol = when (val sym = this.symbol) {
        is AbstractVariableItem<*, *> -> LinearMonomial(sym).symbol
        is LinearIntermediateSymbol<*> -> LinearMonomial(sym as LinearIntermediateSymbol<Flt64>).symbol
        else -> throw IllegalArgumentException("Cannot convert symbol type ${sym::class} to core LinearMonomial")
    }
    return LinearMonomial(this.coefficient, coreSymbol)
}
