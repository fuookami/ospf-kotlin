package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.core.intermediate_symbol.function.SemiFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

class SemiTest {
    @Test
    fun semiBounds() {
        val semi = SemiFunction(
            lb = Flt64.two,
            ub = Flt64.five,
            converter = flt64Converter,
            name = "semi"
        )
        assertTrue(semi.lb eq Flt64.two)
        assertTrue(semi.ub eq Flt64.five)
    }
}
