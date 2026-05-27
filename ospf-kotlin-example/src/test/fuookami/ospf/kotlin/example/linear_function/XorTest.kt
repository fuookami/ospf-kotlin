package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.core.symbol.function.XorFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class XorTest {
    @Test
    fun xorEvaluate() {
        val x = BinVar("x")
        val y = BinVar("y")
        val px = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val py = LinearPolynomial(listOf(LinearMonomial(Flt64.one, y)), Flt64.zero)
        val xor = XorFunction(listOf(px, py), converter = IntoValue.Identity, name = "xor")

        val r10 = xor.evaluate(mapOf(x to Flt64.one, y to Flt64.zero))
        val r11 = xor.evaluate(mapOf(x to Flt64.one, y to Flt64.one))
        assertTrue(r10 != null && (r10 eq Flt64.one))
        assertTrue(r11 != null && (r11 eq Flt64.zero))
    }
}
