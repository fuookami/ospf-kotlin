package fuookami.ospf.kotlin.math.geometry

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.valueOrFail

class GeometryGenericFltXPathTest {
    @Test
    fun cuboidAndBox3ShouldSupportFltX() {
        val cuboid = QuantityCuboid3(
            width = Quantity(FltX(2.0), Meter),
            height = Quantity(FltX(3.0), Meter),
            depth = Quantity(FltX(4.0), Meter)
        )
        val box = cuboid.at(
            x = Quantity(FltX.zero, Meter),
            y = Quantity(FltX(1.0), Meter),
            z = Quantity(FltX(2.0), Meter)
        )

        assertTrue(cuboid.volume eq Quantity(FltX(24.0), CubicMeter))
        assertTrue(box.maxX().valueOrFail() eq Quantity(FltX(2.0), Meter))
        assertTrue(box.maxY().valueOrFail() eq Quantity(FltX(4.0), Meter))
        assertTrue(box.maxZ().valueOrFail() eq Quantity(FltX(6.0), Meter))
    }

    @Test
    fun cylinderShouldSupportFltX() {
        val cylinder = QuantityCylinder3(
            radius = Quantity(FltX(1.0), Meter),
            height = Quantity(FltX(3.0), Meter),
            axis = Axis3.Z
        )

        assertTrue(cylinder.baseArea(FltX(2.0)) eq Quantity(FltX(2.0), SquareMeter))
        assertTrue(cylinder.volume(FltX(2.0)) eq Quantity(FltX(6.0), CubicMeter))

        val projection = cylinder.projectionOn(AxisPlane3.XY)
        assertTrue(projection is QuantityCircle2<*>)
        assertTrue((projection as QuantityCircle2<FltX>).diameter eq Quantity(FltX(2.0), Meter))
    }

    @Test
    fun placement2AndPlacement3ShouldSupportFltX() {
        val placement2A = QuantityPlacement2(
            x = Quantity(FltX.zero, Meter),
            y = Quantity(FltX.zero, Meter),
            shape = QuantityRectangle2(
                width = Quantity(FltX(4.0), Meter),
                height = Quantity(FltX(3.0), Meter)
            )
        )
        val placement2B = QuantityPlacement2(
            x = Quantity(FltX(3.0), Meter),
            y = Quantity(FltX(1.0), Meter),
            shape = QuantityRectangle2(
                width = Quantity(FltX(2.0), Meter),
                height = Quantity(FltX(2.0), Meter)
            )
        )
        assertTrue(placement2A.overlapped(placement2B).valueOrFail())
        assertTrue(placement2A.maxX().valueOrFail() eq Quantity(FltX(4.0), Meter))
        assertTrue(placement2A.maxY().valueOrFail() eq Quantity(FltX(3.0), Meter))

        val placement3A = QuantityPlacement3(
            x = Quantity(FltX.zero, Meter),
            y = Quantity(FltX.zero, Meter),
            z = Quantity(FltX.zero, Meter),
            shape = QuantityCylinder3(
                radius = Quantity(FltX(1.0), Meter),
                height = Quantity(FltX(2.0), Meter),
                axis = Axis3.Z
            )
        )
        val placement3B = QuantityPlacement3(
            x = Quantity(FltX(1.0), Meter),
            y = Quantity(FltX(1.0), Meter),
            z = Quantity(FltX.zero, Meter),
            shape = QuantityCuboid3(
                width = Quantity(FltX(2.0), Meter),
                height = Quantity(FltX(2.0), Meter),
                depth = Quantity(FltX(2.0), Meter)
            )
        )
        assertTrue(placement3A.overlapped(placement3B).valueOrFail())
        assertTrue(placement3A.maxZ().valueOrFail() eq Quantity(FltX(2.0), Meter))
    }
}
