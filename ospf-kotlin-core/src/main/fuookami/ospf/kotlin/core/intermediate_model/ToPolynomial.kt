package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality

/**
 * Interface for types that can be converted to a linear polynomial.
 */
interface ToLinearPolynomial : ToMathLinearInequality {
    fun toLinearPolynomial(): UtilsLinearPolynomial<Flt64>

    override fun toMathLinearInequality(): MathLinearInequality {
        return MathLinearInequality(toLinearPolynomial(), UtilsLinearPolynomial(emptyList(), Flt64.one), fuookami.ospf.kotlin.math.symbol.inequality.Comparison.EQ)
    }
}

/**
 * Interface for types that can be converted to a quadratic polynomial.
 */
interface ToQuadraticPolynomial : ToMathQuadraticInequality {
    fun toQuadraticPolynomial(): UtilsQuadraticPolynomial<Flt64>

    override fun toMathQuadraticInequality(): MathQuadraticInequality {
        return MathQuadraticInequality(toQuadraticPolynomial(), UtilsQuadraticPolynomial(emptyList(), Flt64.one), fuookami.ospf.kotlin.math.symbol.inequality.Comparison.EQ)
    }
}
