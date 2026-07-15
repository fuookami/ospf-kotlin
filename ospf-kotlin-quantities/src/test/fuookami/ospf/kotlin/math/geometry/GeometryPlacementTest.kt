package fuookami.ospf.kotlin.math.geometry

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.quantities.orFail
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.valueOrFail

class GeometryPlacementTest {
    @Test
    fun placement2OverlapAndIntersectionShouldWorkForRectangleAndCircleBounding() {
        val lhs = QuantityPlacement2(
            x = 0.0 * Meter,
            y = 0.0 * Meter,
            shape = QuantityRectangle2(
                width = 4.0 * Meter,
                height = 4.0 * Meter
            )
        )
        val rhs = QuantityPlacement2(
            x = 3.0 * Meter,
            y = 3.0 * Meter,
            shape = QuantityCircle2(
                radius = 1.0 * Meter
            )
        )

        assertTrue(lhs.overlapped(rhs).valueOrFail())
        val intersection = lhs.intersect(rhs).valueOrFail().orFail()
        assertTrue(intersection.width eq (1.0 * Meter))
        assertTrue(intersection.height eq (1.0 * Meter))
    }

    @Test
    fun placement2SeparatedShouldNotOverlap() {
        val lhs = QuantityPlacement2(
            x = 0.0 * Meter,
            y = 0.0 * Meter,
            shape = QuantityRectangle2(
                width = 2.0 * Meter,
                height = 2.0 * Meter
            )
        )
        val rhs = QuantityPlacement2(
            x = 3.0 * Meter,
            y = 3.0 * Meter,
            shape = QuantityRectangle2(
                width = 1.0 * Meter,
                height = 1.0 * Meter
            )
        )

        assertTrue(!lhs.overlapped(rhs).valueOrFail())
        val intersection = lhs.intersect(rhs)
        assertTrue(intersection.ok)
        assertNull(intersection.value)
    }

    @Test
    fun placement3OverlapAndIntersectionShouldWorkForCuboidAndCylinderBounding() {
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

        assertTrue(cuboidPlacement.overlapped(cylinderPlacement).valueOrFail())
        val intersection = cuboidPlacement.intersect(cylinderPlacement).valueOrFail().orFail()
        assertTrue(intersection.width eq (1.0 * Meter))
        assertTrue(intersection.height eq (1.0 * Meter))
        assertTrue(intersection.depth eq (2.0 * Meter))
    }
}
