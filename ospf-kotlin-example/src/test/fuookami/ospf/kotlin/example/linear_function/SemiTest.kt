package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.example.test.flt64TestConverter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.eq

import fuookami.ospf.kotlin.core.symbol.function.SemiFunction

/** Tests the semi-continuous function bound properties. */
class SemiTest {
    @Test
    fun semiBounds() {
        val semi = SemiFunction(
            lb = Flt64.two,
            ub = Flt64.five,
            converter = flt64TestConverter,
            name = "semi"
        )
        assertTrue(semi.lb eq Flt64.two)
        assertTrue(semi.ub eq Flt64.five)
    }
}
