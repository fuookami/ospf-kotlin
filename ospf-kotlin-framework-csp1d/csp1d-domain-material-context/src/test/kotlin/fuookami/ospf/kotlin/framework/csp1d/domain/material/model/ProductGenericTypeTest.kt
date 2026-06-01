package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.KilogramPerSquareMeter
import fuookami.ospf.kotlin.quantities.unit.Meter

class ProductGenericTypeTest {
    @Test
    fun productFlt64ShouldCompileAndInferWeight() {
        val product = Product(
            id = "p64",
            name = "P64",
            width = listOf(
                Quantity(Flt64(1.0), Meter),
                Quantity(Flt64(1.2), Meter)
            ),
            length = Quantity(Flt64(100.0), Meter),
            unitWeight = Quantity(Flt64(2.0), KilogramPerSquareMeter)
        )

        val weight = product.weight
        assertNotNull(weight)
        assertTrue(weight eq Quantity(Flt64(240.0), Kilogram))
    }

    @Test
    fun productFltXShouldCompileAndInferWeight() {
        val product = Product(
            id = "px",
            name = "PX",
            width = listOf(
                Quantity(FltX("0.9"), Meter),
                Quantity(FltX("1.1"), Meter)
            ),
            length = Quantity(FltX("80"), Meter),
            unitWeight = Quantity(FltX("1.5"), KilogramPerSquareMeter)
        )

        val weight = product.weight
        assertNotNull(weight)
        assertTrue(weight eq Quantity(FltX("132"), Kilogram))
    }

    @Test
    fun widthRangeShouldValidateUnitConsistency() {
        val range = WidthRange(
            width = QuantityRange(
                lowerBound = Quantity(Flt64(0.5), Meter),
                upperBound = Quantity(Flt64(2.0), Meter)
            ),
            step = Quantity(Flt64(0.1), Meter)
        )
        assertEquals(true, range.width.contains(Quantity(Flt64(1.0), Meter)))
    }
}
