package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality

/**
 * Interface for types that can be converted to a linear polynomial.
 * Extracted here to break the expression package dependency chain.
 */
interface ToLinearPolynomial<Poly : AbstractLinearPolynomial<Poly>> : ToMathLinearInequality {
    fun toLinearPolynomial(): Poly

    override fun toMathLinearInequality(): MathLinearInequality {
        return toLinearPolynomial() eq true
    }
}

/**
 * Interface for types that can be converted to a quadratic polynomial.
 * Extracted here to break the expression package dependency chain.
 */
interface ToQuadraticPolynomial<Poly : AbstractQuadraticPolynomial<Poly>> : ToMathQuadraticInequality {
    fun toQuadraticPolynomial(): Poly

    override fun toMathQuadraticInequality(): MathQuadraticInequality {
        return toQuadraticPolynomial() eq true
    }
}
