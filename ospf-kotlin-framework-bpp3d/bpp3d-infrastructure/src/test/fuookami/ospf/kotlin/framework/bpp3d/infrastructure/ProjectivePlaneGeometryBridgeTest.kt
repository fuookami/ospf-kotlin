/**
 * 射影平面几何桥接测试。
 * Projective plane geometry bridge test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2 as GeometryRectangle2
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.quantity.*

class ProjectivePlaneGeometryBridgeTest {
    @Test
    fun bottomPlaneRoundTripShouldKeepPoint() {
        val point3 = QuantityPoint3(
            x = FltX(2.0) * Meter,
            y = FltX(5.0) * Meter,
            z = FltX(7.0) * Meter
        )

        val point2 = Bottom.point2(point3)
        val recovered = Bottom.point3(point2, Bottom.distance(point3))

        assertTrue(point2.x eq (FltX(7.0) * Meter))
        assertTrue(point2.y eq (FltX(2.0) * Meter))
        assertTrue(recovered.x eq point3.x)
        assertTrue(recovered.y eq point3.y)
        assertTrue(recovered.z eq point3.z)
    }

    @Test
    fun sideAndFrontPlaneRoundTripShouldKeepPoint() {
        val point3 = QuantityPoint3(
            x = FltX(2.0) * Meter,
            y = FltX(5.0) * Meter,
            z = FltX(7.0) * Meter
        )

        val sideRecovered = Side.point3(Side.point2(point3), Side.distance(point3))
        val frontRecovered = Front.point3(Front.point2(point3), Front.distance(point3))

        assertTrue(sideRecovered.x eq point3.x)
        assertTrue(sideRecovered.y eq point3.y)
        assertTrue(sideRecovered.z eq point3.z)
        assertTrue(frontRecovered.x eq point3.x)
        assertTrue(frontRecovered.y eq point3.y)
        assertTrue(frontRecovered.z eq point3.z)
    }

    @Test
    fun footprintByGeometryShouldMatchPlaneAxes() {
        val cuboid: QuantityCuboid3<FltX> = QuantityCuboid3(
            width = FltX(2.0) * Meter,
            height = FltX(3.0) * Meter,
            depth = FltX(4.0) * Meter
        )

        val bottomShape: GeometryRectangle2<FltX> = Bottom.footprintByGeometry(cuboid)
        val sideShape: GeometryRectangle2<FltX> = Side.footprintByGeometry(cuboid)
        val frontShape: GeometryRectangle2<FltX> = Front.footprintByGeometry(cuboid)

        assertTrue(bottomShape.width eq (FltX(4.0) * Meter))
        assertTrue(bottomShape.height eq (FltX(2.0) * Meter))
        assertTrue(sideShape.width eq (FltX(2.0) * Meter))
        assertTrue(sideShape.height eq (FltX(3.0) * Meter))
        assertTrue(frontShape.width eq (FltX(3.0) * Meter))
        assertTrue(frontShape.height eq (FltX(4.0) * Meter))
    }
}
