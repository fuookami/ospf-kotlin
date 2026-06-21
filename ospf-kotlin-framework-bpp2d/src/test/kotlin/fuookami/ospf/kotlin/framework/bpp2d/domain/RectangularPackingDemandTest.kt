package fuookami.ospf.kotlin.framework.bpp2d.domain

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.SquareMeter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RectangularPackingDemandTest {
    @Test
    fun sceneShouldExposeRealPlacementOverlapNeed() {
        val sheet = Sheet2(
            id = "sheet-1",
            width = Quantity(FltX(10.0), Meter),
            height = Quantity(FltX(6.0), Meter)
        )
        val itemA = RectangleItem2(
            id = "item-a",
            width = Quantity(FltX(4.0), Meter),
            height = Quantity(FltX(2.0), Meter),
            allowRotate = true
        )
        val itemB = RectangleItem2(
            id = "item-b",
            width = Quantity(FltX(3.0), Meter),
            height = Quantity(FltX(3.0), Meter)
        )

        val placementA = PlannedRectangle2(
            item = itemA,
            x = Quantity(FltX(1.0), Meter),
            y = Quantity(FltX(1.0), Meter)
        )
        val placementB = PlannedRectangle2(
            item = itemB,
            x = Quantity(FltX(3.0), Meter),
            y = Quantity(FltX(2.0), Meter)
        )

        val scene = PackingScene2(
            sheet = sheet,
            placements = listOf(placementA, placementB)
        )

        assertTrue(scene.allInsideSheet())
        assertEquals(listOf("item-a" to "item-b"), scene.overlappedPairs())

        val overlap = placementA.toBox2Need().intersect(placementB.toBox2Need())
        assertNotNull(overlap)
        assertTrue(overlap.area eq Quantity(FltX(2.0), SquareMeter))
    }

    @Test
    fun rotatedPlacementShouldStillRespectSheetBoundary() {
        val sheet = Sheet2(
            id = "sheet-2",
            width = Quantity(FltX(5.0), Meter),
            height = Quantity(FltX(4.0), Meter)
        )
        val item = RectangleItem2(
            id = "item-c",
            width = Quantity(FltX(4.0), Meter),
            height = Quantity(FltX(2.0), Meter),
            allowRotate = true
        )
        val validRotated = PlannedRectangle2(
            item = item,
            x = Quantity(FltX(1.0), Meter),
            y = Quantity(FltX(0.0), Meter),
            rotated = true
        )
        val outOfSheet = PlannedRectangle2(
            item = item,
            x = Quantity(FltX(4.0), Meter),
            y = Quantity(FltX(1.5), Meter)
        )

        val validScene = PackingScene2(sheet = sheet, placements = listOf(validRotated))
        val invalidScene = PackingScene2(sheet = sheet, placements = listOf(outOfSheet))

        assertTrue(validScene.allInsideSheet())
        assertFalse(invalidScene.allInsideSheet())
    }

    @Test
    fun sceneShouldReportIllegalOverlapAndRemainingArea() {
        val sheet = Sheet2(
            id = "sheet-3",
            width = Quantity(FltX(10.0), Meter),
            height = Quantity(FltX(6.0), Meter)
        )
        val itemA = RectangleItem2(
            id = "item-a",
            width = Quantity(FltX(4.0), Meter),
            height = Quantity(FltX(2.0), Meter)
        )
        val itemB = RectangleItem2(
            id = "item-b",
            width = Quantity(FltX(3.0), Meter),
            height = Quantity(FltX(2.0), Meter)
        )
        val itemC = RectangleItem2(
            id = "item-c",
            width = Quantity(FltX(2.0), Meter),
            height = Quantity(FltX(2.0), Meter)
        )

        val scene = PackingScene2(
            sheet = sheet,
            placements = listOf(
                PlannedRectangle2(item = itemA, x = Quantity(FltX(0.0), Meter), y = Quantity(FltX(0.0), Meter)),
                PlannedRectangle2(item = itemB, x = Quantity(FltX(4.0), Meter), y = Quantity(FltX(0.0), Meter)),
                PlannedRectangle2(item = itemC, x = Quantity(FltX(3.0), Meter), y = Quantity(FltX(1.0), Meter))
            )
        )

        assertTrue(scene.allInsideSheet())
        assertEquals(listOf("item-a" to "item-c", "item-b" to "item-c"), scene.illegalOverlaps())
        assertTrue(scene.usedArea() eq Quantity(FltX(18.0), SquareMeter))
        assertTrue(scene.remainingArea() eq Quantity(FltX(42.0), SquareMeter))
    }

    @Test
    fun geometryMappingShouldMatchNeedIntersectionContract() {
        val sheet = Sheet2(
            id = "sheet-4",
            width = Quantity(FltX(10.0), Meter),
            height = Quantity(FltX(6.0), Meter)
        )
        val itemA = RectangleItem2(
            id = "item-a",
            width = Quantity(FltX(4.0), Meter),
            height = Quantity(FltX(2.0), Meter)
        )
        val itemB = RectangleItem2(
            id = "item-b",
            width = Quantity(FltX(3.0), Meter),
            height = Quantity(FltX(3.0), Meter)
        )
        val placementA = PlannedRectangle2(
            item = itemA,
            x = Quantity(FltX(1.0), Meter),
            y = Quantity(FltX(1.0), Meter)
        )
        val placementB = PlannedRectangle2(
            item = itemB,
            x = Quantity(FltX(3.0), Meter),
            y = Quantity(FltX(2.0), Meter)
        )
        val scene = PackingScene2(
            sheet = sheet,
            placements = listOf(placementA, placementB)
        )

        assertTrue(scene.allInsideSheet())

        val needIntersection = placementA.toBox2Need().intersect(placementB.toBox2Need())
        assertNotNull(needIntersection)

        val geometryIntersection = placementA
            .toPlacement2Need()
            .toGeometryPlacement2()
            .intersect(placementB.toPlacement2Need().toGeometryPlacement2())
            .valueOrFail()
            .orFail()
        assertNotNull(geometryIntersection)
        val geometryArea = (geometryIntersection.width * geometryIntersection.height).orFail()

        assertTrue(needIntersection.area eq geometryArea)
        assertTrue(geometryArea eq Quantity(FltX(2.0), SquareMeter))
    }
}
