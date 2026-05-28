package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GenericShadowPriceMapProofTest {
    private data class FltXBox(
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : GenericCuboid<FltXBox, FltX> {
        override val self: FltXBox
            get() = this
    }

    private data class FltXArgs(
        override val cuboid: FltXBox
    ) : GenericBPP3DShadowPriceArguments<FltX, FltXBox>

    private class FltXShadowPriceMap :
        GenericBPP3DShadowPriceMap<FltXArgs, FltX, FltXBox>()

    @Test
    fun genericShadowPriceTypesShouldSupportFltX() {
        val cuboid = FltXBox(
            width = Quantity(FltX(1.0), Meter),
            height = Quantity(FltX(2.0), Meter),
            depth = Quantity(FltX(3.0), Meter),
            weight = Quantity(FltX(4.0), Kilogram)
        )
        val args = FltXArgs(cuboid)
        val map = FltXShadowPriceMap()

        assertEquals(cuboid, args.cuboid)
        assertNotNull(map)
    }
}
