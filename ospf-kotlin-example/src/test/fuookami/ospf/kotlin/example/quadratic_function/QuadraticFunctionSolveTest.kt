package fuookami.ospf.kotlin.example.quadratic_function

import fuookami.ospf.kotlin.core.intermediate_symbol.function.ProductFunction
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.scip.ScipQuadraticSolver
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.example.core_demo.ScipAvailability
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import fuookami.ospf.kotlin.math.symbol.inequality.ge
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

class QuadraticFunctionSolveTest {
    private fun isScipAvailable(): Boolean = ScipAvailability.isAvailable()

    @Test
    fun productFunctionSolveShouldMinimizeToZero() {
        assumeTrue(isScipAvailable(), "SCIP JNI not available")

        val x = RealVar("p12_solve_x")
        val y = RealVar("p12_solve_y")

        val left = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val right = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, y)),
            constant = Flt64.zero
        )

        val product = ProductFunction(left, right, converter = IntoValue.Identity, name = "p12_solve_product")

        val model = QuadraticMetaModel(name = "p12-product-solve")
        try {
            assertTrue(model.add(listOf(x, y)) is Ok<*, *, *>)
            assertTrue(product.registerAuxiliaryTokens(model.tokens) is Ok<*, *, *>)

            // Constraint: x + y = 10
            val sumConstraint = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(Flt64.one, x),
                    LinearMonomial(Flt64.one, y)
                ),
                constant = Flt64(-10.0)
            )
            assertTrue(model.addConstraint(sumConstraint eq Flt64.zero) is Ok<*, *, *>)

            // Minimize x*y
            assertTrue(model.minimize(product.polynomial) is Ok<*, *, *>)
            val mechanismRet = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok<*, *, *>)
            val mechanismModel = requireNotNull(mechanismRet.value)

            assertTrue(product.registerConstraints(mechanismModel) is Ok<*, *, *>)

            val solver = ScipQuadraticSolver()
            val result = runBlocking {
                val tetrad = solver.dump(mechanismModel)
                solver(tetrad)
            }
            assertTrue(result is Ok<*, *, *>, "SCIP quadratic solve should succeed")

            val output = requireNotNull(result.value)
            assertTrue(output is FeasibleSolverOutput<*>,
                "SCIP should find feasible solution")

            // x*y minimized with x+y=10 and x,y>=0 => minimum is 0 (one of x,y = 0)
            val objValue = output.obj.toDouble()
            assertTrue(objValue < 0.01,
                "objective x*y should be ~0 but was $objValue")

            // Verify variable values via model.setSolution
            model.setSolution(output.solution)
            val xVal = model.tokens.find(x)?.result?.toDouble() ?: 0.0
            val yVal = model.tokens.find(y)?.result?.toDouble() ?: 0.0
            assertTrue(xVal < 0.01 || yVal < 0.01,
                "at least one of x($xVal) or y($yVal) should be ~0")
            assertTrue(Math.abs(xVal + yVal - 10.0) < 0.01,
                "x+y should be ~10 but was ${xVal + yVal}")
        } finally {
            model.close()
        }
    }

    @Test
    fun quadraticModelWithInequalityConstraintShouldSolve() {
        assumeTrue(isScipAvailable(), "SCIP JNI not available")

        val x = RealVar("p12_ineq_x")
        val y = RealVar("p12_ineq_y")

        val model = QuadraticMetaModel(name = "p12-ineq-solve")
        try {
            assertTrue(model.add(listOf(x, y)) is Ok<*, *, *>)

            // Constraint: x + y >= 4
            val sumPoly = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(Flt64.one, x),
                    LinearMonomial(Flt64.one, y)
                ),
                constant = Flt64.zero
            )
            assertTrue(model.addConstraint(sumPoly ge Flt64(4.0)) is Ok<*, *, *>)

            // Minimize x^2 + y^2 (convex quadratic)
            val objective = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(Flt64.one, x, x),
                    QuadraticMonomial.quadratic(Flt64.one, y, y)
                ),
                constant = Flt64.zero
            )
            assertTrue(model.minimize(objective) is Ok<*, *, *>)
            val mechanismRet = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok<*, *, *>)
            val mechanismModel = requireNotNull(mechanismRet.value)

            val solver = ScipQuadraticSolver()
            val result = runBlocking {
                val tetrad = solver.dump(mechanismModel)
                solver(tetrad)
            }
            assertTrue(result is Ok<*, *, *>, "SCIP quadratic solve should succeed")

            val output = requireNotNull(result.value)
            assertTrue(output is FeasibleSolverOutput<*>,
                "SCIP should find feasible solution")

            // x^2+y^2 minimized with x+y>=4 => x=y=2, objective = 8
            val objValue = output.obj.toDouble()
            assertTrue(Math.abs(objValue - 8.0) < 0.1,
                "objective x^2+y^2 should be ~8 but was $objValue")

            model.setSolution(output.solution)
            val xVal = model.tokens.find(x)?.result?.toDouble() ?: 0.0
            val yVal = model.tokens.find(y)?.result?.toDouble() ?: 0.0
            assertTrue(Math.abs(xVal - 2.0) < 0.1,
                "x should be ~2 but was $xVal")
            assertTrue(Math.abs(yVal - 2.0) < 0.1,
                "y should be ~2 but was $yVal")
        } finally {
            model.close()
        }
    }
}
