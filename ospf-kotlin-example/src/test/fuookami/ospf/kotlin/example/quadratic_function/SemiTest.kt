package fuookami.ospf.kotlin.example.quadratic_function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.symbol.function.SemiFunction
import fuookami.ospf.kotlin.example.test.flt64TestConverter

/** Tests the quadratic semi-continuous function bounds and empty-map evaluation. */
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
        assertEquals(2, function.helperVariables.size)
        assertNull(function.evaluate(emptyMap()))
    }
}
