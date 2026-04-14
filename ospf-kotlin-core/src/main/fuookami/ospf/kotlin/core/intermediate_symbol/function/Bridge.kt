@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.expression.monomial.LinearMonomial as CoreLinearMonomial
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
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
fun CoreLinearMonomial.asMathLinearMonomial(): MathLinearMonomial<Flt64> {
    return MathLinearMonomial(this.coefficient, this.symbol as Symbol)
}
