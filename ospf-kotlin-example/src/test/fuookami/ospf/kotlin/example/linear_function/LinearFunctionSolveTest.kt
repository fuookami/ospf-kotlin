package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.core.symbol.function.AbsFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.SlackRangeFunction
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.scip.ScipLinearSolver
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.example.solveLinearMetaModel
import fuookami.ospf.kotlin.example.core_demo.ScipAvailability
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LinearFunctionSolveTest {
    private val converter = IntoValue.Identity

    @Test
    fun absFunctionShouldSolveCorrectly() {
        assumeTrue(ScipAvailability.isAvailable(), "SCIP runtime not available in current environment")

        val x = RealVar("p11_abs_x")
        x.range.leq(Flt64(3.0))
        x.range.geq(Flt64(-3.0))
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val absSymbol = LinearFunctionSymbolAdapter(
            delegate = AbsFunction(
                polynomial = xPoly,
                converter = converter,
                name = "p11_abs"
            ),
            converter = converter
        )

        val model = LinearMetaModel<Flt64>(
            name = "p11_abs_solve",
            converter = converter
        )
        try {
            assertTrue(model.add(x) is Ok, "x should be accepted")
            assertTrue(model.add(absSymbol) is Ok, "abs symbol should be added to model")
            assertTrue(model.minimize(absSymbol) is Ok, "minimize |x| objective should be accepted")

            val solver = ScipLinearSolver()
            val result = runBlocking { solveLinearMetaModel(solver, model) }
            assertNotNull(result.value, "Solver should return a feasible solution")
            assertTrue(result.value!!.obj eq Flt64.zero, "min |x| should be 0 when x can be 0")

            model.setSolution(result.value!!.solution)
            val xVal = model.tokens.find(x)?.result
            assertNotNull(xVal, "x should appear in solution")
            assertTrue(xVal!! eq Flt64.zero, "x should be 0 at optimal (|x| minimized)")
        } finally {
            model.close()
        }
    }

    @Test
    fun slackRangeFunctionShouldSolveCorrectly() {
        assumeTrue(ScipAvailability.isAvailable(), "SCIP runtime not available in current environment")

        val x = RealVar("p11_slack_x")
        x.range.leq(Flt64(5.0))
        x.range.geq(Flt64.zero)
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val slackFn = SlackRangeFunction(
            x = xPoly,
            lb = LinearPolynomial(emptyList(), Flt64(2.0)),
            ub = LinearPolynomial(emptyList(), Flt64(4.0)),
            converter = converter,
            name = "p11_slack_range"
        )
        val slackSymbol = LinearFunctionSymbolAdapter(slackFn, converter)

        val model = LinearMetaModel<Flt64>(
            name = "p11_slack_range_solve",
            converter = converter
        )
        try {
            assertTrue(model.add(x) is Ok, "x should be accepted")
            assertTrue(model.add(slackSymbol) is Ok, "slackRange symbol should be added to model")
            assertTrue(model.minimize(slackSymbol.pos!!) is Ok, "minimize positive slack objective should be accepted")

            val solver = ScipLinearSolver()
            val result = runBlocking { solveLinearMetaModel(solver, model) }
            assertNotNull(result.value, "Solver should return a feasible solution")
            assertTrue(result.value!!.obj ls Flt64(4.0), "positive slack should be less than upper bound")

            model.setSolution(result.value!!.solution)
            val xVal = model.tokens.find(x)?.result
            assertNotNull(xVal, "x should appear in solution")
            assertTrue(xVal!! geq Flt64(2.0), "x should be >= lb=2 when slack range constrains it")
            assertTrue(xVal!! leq Flt64(4.0), "x should be <= ub=4 when slack range constrains it")
        } finally {
            model.close()
        }
    }
}
