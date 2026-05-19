package fuookami.ospf.kotlin.example.quadratic_function

import fuookami.ospf.kotlin.core.intermediate_symbol.function.SemiFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SemiTest {
    private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

    @Test
    fun smoke() {
        val function = SemiFunction(
            lb = Flt64.one,
            ub = Flt64(4.0),
            converter = flt64Converter,
            name = "example_quadratic_semi"
        )
        assertTrue(function.lb eq Flt64.one)
        assertTrue(function.ub eq Flt64(4.0))
        assertTrue(function.helperVariables.isEmpty())
        assertNull(function.evaluate(emptyMap()))
    }
}