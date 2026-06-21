package fuookami.ospf.kotlin.math.geometry

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.valueOrFail
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class GeometryCylinder3Test {
    @Test
    fun axisPermutation3ShouldRemapCylinderAxis() {
        val cylinder = QuantityCylinder3(
            radius = 1.0 * Meter,
            height = 5.0 * Meter,
            axis = Axis3.X
        )

        val remapped = cylinder.permute(QuantityAxisPermutation3.YXZ).valueOrFail()
        assertTrue(remapped.axis == Axis3.Y)
        assertTrue(remapped.boundingCuboid.width eq (2.0 * Meter))
        assertTrue(remapped.boundingCuboid.height eq (5.0 * Meter))
        assertTrue(remapped.boundingCuboid.depth eq (2.0 * Meter))
    }

    @Test
    fun cylinderVolumeAndBoundingBoxShouldBeStable() {
        val cylinder = QuantityCylinder3(
            radius = 1.0 * Meter,
            height = 3.0 * Meter,
            axis = Axis3.Z
        )

        assertTrue(cylinder.volume(Flt64(2.0)) eq (6.0 * CubicMeter))
        val box = cylinder.boundingBoxAtOrigin()
        assertTrue(box.width eq (2.0 * Meter))
        assertTrue(box.height eq (2.0 * Meter))
        assertTrue(box.depth eq (3.0 * Meter))
    }

    @Test
    fun projectionShouldReturnCircleOrRectangleByPlane() {
        val cylinder = QuantityCylinder3(
            radius = 1.0 * Meter,
            height = 3.0 * Meter,
            axis = Axis3.Z
        )

        val xy = cylinder.projectionOn(AxisPlane3.XY)
        val xz = cylinder.projectionOn(AxisPlane3.XZ)
        val yz = cylinder.projectionOn(AxisPlane3.YZ)

        assertTrue(xy is QuantityCircle2)
        assertTrue((xy as QuantityCircle2).diameter eq (2.0 * Meter))
        assertTrue(xz is QuantityRectangle2)
        assertTrue((xz as QuantityRectangle2).width eq (2.0 * Meter))
        assertTrue((xz as QuantityRectangle2).height eq (3.0 * Meter))
        assertTrue(yz is QuantityRectangle2)
        assertTrue((yz as QuantityRectangle2).width eq (2.0 * Meter))
        assertTrue((yz as QuantityRectangle2).height eq (3.0 * Meter))
    }

    @Test
    fun cylinderCuboidPlacementRelationShouldProvideMinimalUsableCheck() {
        val cylinderPlacement = QuantityPlacement3(
            x = 1.0 * Meter,
            y = 1.0 * Meter,
            z = 0.0 * Meter,
            shape = QuantityCylinder3(
                radius = 1.0 * Meter,
                height = 2.0 * Meter,
                axis = Axis3.Z
            )
        )
        val cuboidPlacement = QuantityPlacement3(
            x = 0.0 * Meter,
            y = 0.0 * Meter,
            z = 0.0 * Meter,
            shape = QuantityCuboid3(
                width = 2.0 * Meter,
                height = 2.0 * Meter,
                depth = 2.0 * Meter
            )
        )
        val farPlacement = QuantityPlacement3(
            x = 10.0 * Meter,
            y = 10.0 * Meter,
            z = 10.0 * Meter,
            shape = QuantityCuboid3(
                width = 1.0 * Meter,
                height = 1.0 * Meter,
                depth = 1.0 * Meter
            )
        )

        assertTrue(cylinderPlacement.overlapped(cuboidPlacement).value!!)
        assertTrue(!cylinderPlacement.overlapped(farPlacement).value!!)
    }
}
