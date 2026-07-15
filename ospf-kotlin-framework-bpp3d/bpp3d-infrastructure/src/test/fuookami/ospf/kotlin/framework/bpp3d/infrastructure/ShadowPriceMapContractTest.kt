/**
 * 影子价格映射契约测试。
 * Shadow price map contract test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity

class ShadowPriceMapContractTest {
    private data class FltXBox(
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>,
        override val enabledOrientations: List<Orientation> = Orientation.entries
    ) : Cuboid<FltXBox, FltX> {
        override val self: FltXBox
            get() = this
    }

    private data class FltXArgs(
        override val cuboid: FltXBox
    ) : AbstractBPP3DShadowPriceArguments<FltX, FltXBox>

    private class FltXShadowPriceMap :
        AbstractBPP3DShadowPriceMap<FltXArgs, FltX, FltXBox>()

    @Test
    fun shadowPriceTypesShouldSupportFltX() {
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
