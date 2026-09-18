package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertEquals
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

class AbsFunctionBoundsTest {
    @Test
    fun finiteInputBoundsTightenAbsoluteValueParts() {
        val x = RealVar("abs_bounds_x")
        x.range.geq(Flt64(-3.0))
        x.range.leq(Flt64(7.0))

        val function = AbsFunction(
            polynomial = LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, x)),
                Flt64.zero
            ),
            converter = IntoValue.Identity,
            name = "abs_bounds"
        )

        assertEquals(Flt64.zero, function.resultVar.lowerBound!!.value.unwrap())
        assertEquals(Flt64(7.0), function.resultVar.upperBound!!.value.unwrap())
        assertEquals(Flt64(7.0), function.posVar.upperBound!!.value.unwrap())
        assertEquals(Flt64(3.0), function.negVar.upperBound!!.value.unwrap())
    }
}
