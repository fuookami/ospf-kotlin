package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.Test
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

/** [MaxFunction] 契约测试。 / Dedicated contract tests. */
class MaxFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(MaxFunction::class.java))
    }

    @Test
    fun negativeInputsUseSignedResultVariableAndPropagateBounds() {
        val x = RealVar("max_negative_x")
        val y = RealVar("max_negative_y")
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
        val function = MaxFunction(
            polynomials = listOf(xPolynomial, yPolynomial),
            converter = IntoValue.Identity,
            name = "max_negative"
        )

        val result = assertIs<RealVar>(function.resultVar)
        assertEquals(Flt64(-6.0), result.lowerBound!!.value.unwrap())
        assertEquals(Flt64(-2.0), result.upperBound!!.value.unwrap())
    }
}
