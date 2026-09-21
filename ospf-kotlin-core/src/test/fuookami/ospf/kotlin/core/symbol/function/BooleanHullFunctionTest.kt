package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertEquals
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.BinVar

class BooleanHullFunctionTest {
    @Test
    fun booleanAndOrNotAvoidNonzeroIndicatorHelpers() {
        val x = BinVar("boolean_hull_x")
        val y = BinVar("boolean_hull_y")
        val xPolynomial = polynomialOf(x)
        val yPolynomial = polynomialOf(y)

        val and = AndFunction(
            polynomials = listOf(xPolynomial, yPolynomial),
            converter = IntoValue.Identity,
            name = "boolean_hull_and"
        )
        val or = OrFunction(
            polynomials = listOf(xPolynomial, yPolynomial),
            converter = IntoValue.Identity,
            name = "boolean_hull_or"
        )
        val not = NotFunction(
            polynomial = xPolynomial,
            converter = IntoValue.Identity,
            name = "boolean_hull_not"
        )

        assertEquals(1, and.helperVariables.size)
        assertEquals(1, or.helperVariables.size)
        assertEquals(1, not.helperVariables.size)
    }

    @Test
    fun nonBooleanInputsKeepGeneralIndicatorHelpers() {
        val x = BinVar("general_indicator_x")
        val polynomial = LinearPolynomial(
            listOf(LinearMonomial(Flt64(2.0), x)),
            Flt64.zero
        )

        val and = AndFunction(
            polynomials = listOf(polynomial),
            converter = IntoValue.Identity,
            name = "general_indicator_and"
        )

        assertEquals(3, and.helperVariables.size)
    }

    private fun polynomialOf(variable: BinVar): LinearPolynomial<Flt64> = LinearPolynomial(
        listOf(LinearMonomial(Flt64.one, variable)),
        Flt64.zero
    )
}
