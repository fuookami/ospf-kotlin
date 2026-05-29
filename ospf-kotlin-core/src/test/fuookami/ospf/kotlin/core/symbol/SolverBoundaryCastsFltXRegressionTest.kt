package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SolverBoundaryCastsFltXRegressionTest {
    @Test
    fun linearFltXSymbolCanBeFlattenedForSolverBoundary() {
        val x = RealVar("solver_boundary_linear_x")
        val symbol = LinearExpressionSymbol(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(LinearMonomial(FltX.one, x)),
                constant = FltX("2.5")
            )
        )

        val flattened = SolverBoundaryCasts.linearSolverFlattenedMonomials(symbol)
        assertEquals(Flt64.one, flattened.monomials.single().coefficient)
        assertEquals(Flt64(2.5), flattened.constant)
    }

    @Test
    fun quadraticFltXSymbolCanBeFlattenedForSolverBoundary() {
        val x = RealVar("solver_boundary_quadratic_x")
        val y = RealVar("solver_boundary_quadratic_y")
        val symbol = QuadraticExpressionSymbol(
            _utilsPolynomial = MutableQuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(FltX.one, x, y)),
                constant = FltX("3.75")
            )
        )

        val flattened = SolverBoundaryCasts.quadraticSolverFlattenedMonomials(symbol)
        assertEquals(Flt64.one, flattened.monomials.single().coefficient)
        assertEquals(Flt64(3.75), flattened.constant)
    }
}
