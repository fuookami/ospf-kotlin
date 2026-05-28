package fuookami.ospf.kotlin.math.geometry

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.quantity.*

class GeometryCrossDomainReuseTest {
    private data class Bpp2dLikePlacement<V : fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber<V>>(
        val placement: QuantityPlacement2<V>
    )

    private data class Csp2dLikeRegion<V : fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber<V>>(
        val region: QuantityBox2<V>
    )

    @Test
    fun bpp2dLikeConsumerShouldUseGeometryKernel() {
        val item = Bpp2dLikePlacement(
            placement = QuantityPlacement2(
                x = 0.0 * Meter,
                y = 0.0 * Meter,
                shape = QuantityRectangle2(
                    width = 4.0 * Meter,
                    height = 2.0 * Meter
                )
            )
        )
        val other = Bpp2dLikePlacement(
            placement = QuantityPlacement2(
                x = 3.0 * Meter,
                y = 1.0 * Meter,
                shape = QuantityRectangle2(
                    width = 2.0 * Meter,
                    height = 2.0 * Meter
                )
            )
        )

        assertTrue(item.placement.overlapped(other.placement))
        val intersect = item.placement.intersect(other.placement)
        assertNotNull(intersect)
        assertTrue(intersect.width eq (1.0 * Meter))
    }

    @Test
    fun csp2dLikeConsumerShouldUseGeometryKernelWithFltX() {
        val stock = Csp2dLikeRegion(
            region = QuantityBox2(
                x = Quantity(FltX.zero, Meter),
                y = Quantity(FltX.zero, Meter),
                shape = QuantityRectangle2(
                    width = Quantity(FltX(10.0), Meter),
                    height = Quantity(FltX(5.0), Meter)
                )
            )
        )
        val cut = Csp2dLikeRegion(
            region = QuantityBox2(
                x = Quantity(FltX(8.0), Meter),
                y = Quantity(FltX(1.0), Meter),
                shape = QuantityRectangle2(
                    width = Quantity(FltX(3.0), Meter),
                    height = Quantity(FltX(3.0), Meter)
                )
            )
        )

        assertTrue(stock.region.overlapped(cut.region))
        val intersect = stock.region.intersect(cut.region)
        assertNotNull(intersect)
        assertTrue(intersect.width eq Quantity(FltX(2.0), Meter))
    }
}
