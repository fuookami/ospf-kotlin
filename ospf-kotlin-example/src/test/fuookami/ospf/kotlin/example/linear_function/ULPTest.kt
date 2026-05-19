package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.core.intermediate_symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.geometry.point2

class ULPTest {
    @Test
    fun univariateFromPointsEvaluate() {
        val x = URealVar("x")
        val px = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val ulp = UnivariateLinearPiecewiseFunction.fromPoints(
            x = px,
            points = listOf(
                point2(),
                point2(x = Flt64.one, y = Flt64.two),
                point2(x = Flt64.two, y = Flt64.one)
            ),
            converter = IntoValue.Identity,
            name = "y"
        )

        val yAt1 = ulp.evaluate(mapOf(x to Flt64.one))
        val yAt2 = ulp.evaluate(mapOf(x to Flt64.two))
        assertTrue(yAt1 != null && (yAt1 eq Flt64.two))
        assertTrue(yAt2 != null && (yAt2 eq Flt64.one))
    }
}
