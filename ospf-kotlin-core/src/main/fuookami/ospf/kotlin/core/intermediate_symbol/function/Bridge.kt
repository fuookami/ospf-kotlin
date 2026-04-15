@file:Suppress("unused", "DEPRECATION")

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
 * Convert core expression AbstractLinearPolynomial to math LinearPolynomial<Flt64>.
 * Used by new MathFunctionSymbol implementations to accept legacy DSL polynomial types.
 */
fun AbstractLinearPolynomial<*>.asMathLinearPolynomial(): MathLinearPolynomial<Flt64> {
    val corePoly = this.toLinearPolynomial()
    val mathMonos = corePoly.monomials.map { it.asMathLinearMonomial() }
    return MathLinearPolynomial(mathMonos, corePoly.constant.asFlt64())
}

/**
 * Convert LinearIntermediateSymbol to math LinearPolynomial<Flt64>.
 * Used when framework code passes LinearIntermediateSymbol to new MathFunctionSymbol constructors.
 */
fun LinearIntermediateSymbol.asMathLinearPolynomial(): MathLinearPolynomial<Flt64> {
    val corePoly = this.toLinearPolynomial()
    val mathMonos = corePoly.monomials.map { it.asMathLinearMonomial() }
    return MathLinearPolynomial(mathMonos, corePoly.constant.asFlt64())
}

/**
 * Convert core expression LinearMonomial to math LinearMonomial<Flt64>.
 */
fun LinearMonomial.asMathLinearMonomial(): MathLinearMonomial<Flt64> {
    return MathLinearMonomial(this.coefficient, this.symbol as Symbol)
}

/**
 * Convert math LinearPolynomial<Flt64> to core expression LinearPolynomial.
 * Used when framework code needs to construct core polynomials from math types
 * (e.g., extracting slack.pos/neg from new SlackFunction).
 */
fun MathLinearPolynomial<Flt64>.asCoreLinearPolynomial(): LinearPolynomial {
    return LinearPolynomial(
        monomials = this.monomials.map { it.asCoreLinearMonomial() },
        constant = this.constant
    )
}

/**
 * Convert star-projected math LinearPolynomial<*> to core expression LinearPolynomial.
 * Needed when working with SlackFunction<*> where the type parameter is unknown.
 */
@Suppress("UNCHECKED_CAST")
@JvmName("asCoreLinearPolynomialStar")
fun MathLinearPolynomial<*>.asCoreLinearPolynomial(): LinearPolynomial {
    val poly = this as MathLinearPolynomial<Flt64>
    return LinearPolynomial(
        monomials = poly.monomials.map { it.asCoreLinearMonomial() },
        constant = poly.constant
    )
}

/**
 * Convert math LinearMonomial<Flt64> to core expression LinearMonomial.
 */
fun MathLinearMonomial<Flt64>.asCoreLinearMonomial(): LinearMonomial {
    val coreSymbol = when (val sym = this.symbol) {
        is AbstractVariableItem<*, *> -> LinearMonomial(sym).symbol
        is LinearIntermediateSymbol -> LinearMonomial(sym).symbol
        else -> throw IllegalArgumentException("Cannot convert symbol type ${sym::class} to core LinearMonomial")
    }
    return LinearMonomial(this.coefficient, coreSymbol)
}
