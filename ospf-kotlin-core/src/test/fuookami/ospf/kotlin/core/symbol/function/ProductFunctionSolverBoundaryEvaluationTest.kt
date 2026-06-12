package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar

class ProductFunctionSolverBoundaryEvaluationTest {
    @Test
    fun solverBoundaryEvaluateByResultsAndValuesShouldWorkForFourNumberTypes() {
        runCase(GenericNumberCases.flt64)
        runCase(GenericNumberCases.fltX)
        runCase(GenericNumberCases.rtn64)
        runCase(GenericNumberCases.rtnX)
    }

    private fun <V> runCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val x = RealVar("${numberCase.name.lowercase()}_solver_boundary_x")
        val y = RealVar("${numberCase.name.lowercase()}_solver_boundary_y")
        val left = LinearPolynomial(
            monomials = listOf(LinearMonomial(numberCase.one, x)),
            constant = numberCase.two
        )
        val right = LinearPolynomial(
            monomials = listOf(LinearMonomial(numberCase.one, y)),
            constant = -numberCase.one
        )
        val function = ProductFunction(
            left = left,
            right = right,
            converter = numberCase.converter,
            name = "product_solver_boundary_${numberCase.name.lowercase()}"
        )

        val tokenTable = AutoTokenTable<V>(Quadratic, false)
        tokenTable.add(listOf(x, y))
        val results = listOf(numberCase.two, numberCase.five)
        val values = mapOf<Symbol, V>(
            x to numberCase.two,
            y to numberCase.five
        )
        val expected = Flt64(16.0)

        val evalByResults = function.evaluate(results, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByResults, "${numberCase.name}: solver-boundary evaluate(results) should not be null")
        assertEquals(expected, numberCase.converter.fromValue(evalByResults))

        val evalByValues = function.evaluate(values, tokenTable, numberCase.converter, zeroIfNone = false)
        assertNotNull(evalByValues, "${numberCase.name}: solver-boundary evaluate(values) should not be null")
        assertEquals(expected, numberCase.converter.fromValue(evalByValues))
    }
}
