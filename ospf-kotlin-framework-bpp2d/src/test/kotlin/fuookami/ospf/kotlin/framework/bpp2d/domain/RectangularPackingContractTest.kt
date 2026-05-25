package fuookami.ospf.kotlin.framework.bpp2d.domain

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Centimeter
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.SquareMeter
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RectangularPackingContractTest {
    @Test
    fun projection2NeedShouldKeepFutureProjection2AreaContract() {
        val projection = Projection2Need(
            width = Quantity(FltX(2.0), Meter),
            height = Quantity(FltX(150.0), Centimeter)
        )

        val areaInSquareMeter = projection.area.convertTo(SquareMeter)
        assertNotNull(areaInSquareMeter)
        assertTrue(areaInSquareMeter eq Quantity(FltX(3.0), SquareMeter))
    }

    @Test
    fun placement2NeedShouldKeepFuturePlacement2BoundaryContract() {
        val placement = Placement2Need(
            x = Quantity(FltX(1.0), Meter),
            y = Quantity(FltX(50.0), Centimeter),
            projection = Projection2Need(
                width = Quantity(FltX(120.0), Centimeter),
                height = Quantity(FltX(2.0), Meter)
            )
        )

        assertTrue(placement.maxX eq Quantity(FltX(2.2), Meter))
        assertTrue(placement.maxY eq Quantity(FltX(2.5), Meter))
        val box = placement.toBox2Need()
        assertTrue(box.width eq Quantity(FltX(120.0), Centimeter))
        assertTrue(box.height eq Quantity(FltX(2.0), Meter))
    }

    @Test
    fun box2NeedShouldKeepFutureBox2OverlapIntersectContract() {
        val lhs = Box2Need(
            minX = Quantity(FltX(0.0), Meter),
            minY = Quantity(FltX(0.0), Meter),
            maxX = Quantity(FltX(250.0), Centimeter),
            maxY = Quantity(FltX(2.0), Meter)
        )
        val rhs = Box2Need(
            minX = Quantity(FltX(2.0), Meter),
            minY = Quantity(FltX(50.0), Centimeter),
            maxX = Quantity(FltX(4.0), Meter),
            maxY = Quantity(FltX(2.0), Meter)
        )
        val apart = Box2Need(
            minX = Quantity(FltX(4.1), Meter),
            minY = Quantity(FltX(0.0), Meter),
            maxX = Quantity(FltX(5.0), Meter),
            maxY = Quantity(FltX(1.0), Meter)
        )

        assertTrue(lhs.overlaps(rhs))
        val intersect = lhs.intersect(rhs)
        assertNotNull(intersect)
        assertTrue(intersect.width eq Quantity(FltX(0.5), Meter))
        assertTrue(intersect.height eq Quantity(FltX(1.5), Meter))
        assertNull(lhs.intersect(apart))
    }

    @Test
    fun needModelsShouldMapToQuantityGeometryApi() {
        val projectionNeed = Projection2Need(
            width = Quantity(FltX(2.0), Meter),
            height = Quantity(FltX(3.0), Meter)
        )
        val placementNeed = Placement2Need(
            x = Quantity(FltX(1.0), Meter),
            y = Quantity(FltX(2.0), Meter),
            projection = projectionNeed
        )
        val boxNeed = placementNeed.toBox2Need()

        val projection = projectionNeed.toGeometryProjection2()
        val placement = placementNeed.toGeometryPlacement2()
        val box = boxNeed.toGeometryBox2()

        assertTrue((projection as QuantityRectangle2).area eq Quantity(FltX(6.0), SquareMeter))
        assertTrue(placement.maxX eq Quantity(FltX(3.0), Meter))
        assertTrue(placement.maxY eq Quantity(FltX(5.0), Meter))
        assertTrue(box.maxX eq Quantity(FltX(3.0), Meter))
        assertTrue(box.maxY eq Quantity(FltX(5.0), Meter))
    }

    @Test
    fun mappedBox2ShouldKeepOverlapAndIntersectContract() {
        val lhsNeed = Box2Need(
            minX = Quantity(FltX(0.0), Meter),
            minY = Quantity(FltX(0.0), Meter),
            maxX = Quantity(FltX(250.0), Centimeter),
            maxY = Quantity(FltX(2.0), Meter)
        )
        val rhsNeed = Box2Need(
            minX = Quantity(FltX(2.0), Meter),
            minY = Quantity(FltX(50.0), Centimeter),
            maxX = Quantity(FltX(4.0), Meter),
            maxY = Quantity(FltX(2.0), Meter)
        )
        val apartNeed = Box2Need(
            minX = Quantity(FltX(4.1), Meter),
            minY = Quantity(FltX(0.0), Meter),
            maxX = Quantity(FltX(5.0), Meter),
            maxY = Quantity(FltX(1.0), Meter)
        )

        val lhs = lhsNeed.toGeometryBox2()
        val rhs = rhsNeed.toGeometryBox2()
        val apart = apartNeed.toGeometryBox2()

        assertTrue(lhs.overlapped(rhs))
        assertFalse(lhs.overlapped(apart))

        val needIntersection = lhsNeed.intersect(rhsNeed)
        assertNotNull(needIntersection)

        val geometryIntersection = lhs.intersect(rhs)
        assertNotNull(geometryIntersection)

        assertTrue(geometryIntersection.width eq needIntersection.width)
        assertTrue(geometryIntersection.height eq needIntersection.height)
        assertTrue((geometryIntersection.width * geometryIntersection.height) eq Quantity(FltX(0.75), SquareMeter))
    }
}
