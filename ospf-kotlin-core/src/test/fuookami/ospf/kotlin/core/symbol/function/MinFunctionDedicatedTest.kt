package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

/** Dedicated contract test for the MinFunction symbol. */
class MinFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(MinFunction::class.java))
    }

    @Test
    fun negativeInputsUseSignedResultVariableAndPropagateBounds() {
        val x = RealVar("min_negative_x")
        val y = RealVar("min_negative_y")
        x.range.geq(Flt64(-10.0))
        x.range.leq(Flt64(-4.0))
        y.range.geq(Flt64(-6.0))
        y.range.leq(Flt64(-2.0))

        val xPolynomial = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, x)),
            Flt64.zero
        )
        val yPolynomial = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, y)),
            Flt64.zero
        )
        val function = MinFunction(
            polynomials = listOf(xPolynomial, yPolynomial),
            converter = IntoValue.Identity,
            name = "min_negative"
        )

        val result = assertIs<RealVar>(function.resultVar)
        assertEquals(Flt64(-10.0), result.lowerBound!!.value.unwrap())
        assertEquals(Flt64(-4.0), result.upperBound!!.value.unwrap())
    }
}
