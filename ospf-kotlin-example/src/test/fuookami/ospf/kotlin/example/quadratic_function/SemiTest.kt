package fuookami.ospf.kotlin.example.quadratic_function

import fuookami.ospf.kotlin.example.test.flt64TestConverter
import fuookami.ospf.kotlin.core.symbol.function.SemiFunction
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SemiTest {
    @Test
    fun smoke() {
        val function = SemiFunction(
            lb = Flt64.one,
            ub = Flt64(4.0),
            converter = flt64TestConverter,
            name = "example_quadratic_semi"
        )
        assertTrue(function.lb eq Flt64.one)
        assertTrue(function.ub eq Flt64(4.0))
        assertTrue(function.helperVariables.isEmpty())
        assertNull(function.evaluate(emptyMap()))
    }
}