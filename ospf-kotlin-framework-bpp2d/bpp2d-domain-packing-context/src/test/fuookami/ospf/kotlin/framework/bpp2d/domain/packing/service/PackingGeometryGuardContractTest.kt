package fuookami.ospf.kotlin.framework.bpp2d.domain.packing.service

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.packing.*

class PackingGeometryGuardContractTest {

    @Test
    fun placementOutsideSheetIsRejected() {
        val sheet = sheetShapeOf(width = 10.0, height = 10.0)
        val placements = listOf(
            rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 2.0, itemId = "r1"),
            rectanglePlacementOf(x = 9.0, y = 0.0, width = 2.0, height = 2.0, itemId = "r2")
        )

        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest"
        )

        assertTrue(result is Failed)
        val message = result.errorMessageOrFail()
        assertTrue(message.contains("type=outside_sheet"), message)
        assertTrue(message.contains("bin=sheet-1"), message)
        assertTrue(message.contains("item[1]"), message)
    }

    @Test
    fun overlappingRectanglesAreRejected() {
        val sheet = sheetShapeOf(width = 10.0, height = 10.0)
        val placements = listOf(
            rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 2.0, itemId = "r1"),
            rectanglePlacementOf(x = 1.0, y = 1.0, width = 2.0, height = 2.0, itemId = "r2")
        )

        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest"
        )

        assertTrue(result is Failed)
        val message = result.errorMessageOrFail()
        assertTrue(message.contains("type=overlap"), message)
        assertTrue(message.contains("item[0]"), message)
        assertTrue(message.contains("item[1]"), message)
    }

    @Test
    fun tangentCirclesAreAccepted() {
        val sheet = sheetShapeOf(width = 2.0, height = 1.0)
        val placements = listOf(
            circlePlacementOf(x = 0.0, y = 0.0, radius = 0.5, itemId = "c1"),
            circlePlacementOf(x = 1.0, y = 0.0, radius = 0.5, itemId = "c2")
        )

        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest"
        )

        assertTrue(result is Ok, "tangent circles must not be treated as an overlap")
    }

    @Test
    fun slightlyOverlappingCirclesAreRejected() {
        val sheet = sheetShapeOf(width = 2.0, height = 1.0)
        val placements = listOf(
            circlePlacementOf(x = 0.0, y = 0.0, radius = 0.5, itemId = "c1"),
            circlePlacementOf(x = 0.9, y = 0.0, radius = 0.5, itemId = "c2")
        )

        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest"
        )

        assertTrue(result is Failed)
        assertTrue(result.errorMessageOrFail().contains("type=overlap"))
    }

    @Test
    fun circleOverlappingRectangleIsRejected() {
        val sheet = sheetShapeOf(width = 4.0, height = 4.0)
        val placements = listOf(
            rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 2.0, itemId = "r1"),
            circlePlacementOf(x = 1.5, y = 0.5, radius = 0.5, itemId = "c1")
        )

        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest"
        )

        assertTrue(result is Failed)
        val message = result.errorMessageOrFail()
        assertTrue(message.contains("type=overlap"), message)
        assertTrue(message.contains("rectangle"), message)
        assertTrue(message.contains("circle"), message)
    }

    @Test
    fun circleTouchingRectangleEdgeIsAccepted() {
        val sheet = sheetShapeOf(width = 4.0, height = 4.0)
        val placements = listOf(
            rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 2.0, itemId = "r1"),
            circlePlacementOf(x = 2.0, y = 0.0, radius = 0.5, itemId = "c1")
        )

        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest"
        )

        assertTrue(result is Ok, "a circle tangent to a rectangle edge must not be treated as an overlap")
    }

    @Test
    fun rotatedRectangleWithoutAllowanceIsRejected() {
        val sheet = sheetShapeOf(width = 10.0, height = 10.0)
        val placements = listOf(
            rectanglePlacementOf(
                x = 0.0,
                y = 0.0,
                width = 1.0,
                height = 3.0,
                rotated = true,
                itemId = "r1"
            )
        )

        val rotated = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest",
            rotationRule = PackingRotationRule.SymmetricOnly
        )
        assertTrue(rotated is Failed)
        val message = rotated.errorMessageOrFail()
        assertTrue(message.contains("type=illegal_rotation"), message)
        assertTrue(message.contains("item=r1"), message)

        val allowed = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest",
            rotationRule = PackingRotationRule.AllowAll
        )
        assertTrue(allowed is Ok)
    }

    @Test
    fun validRectanglesTouchingEdgesAreAccepted() {
        val sheet = sheetShapeOf(width = 4.0, height = 2.0)
        val placements = listOf(
            rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 2.0, itemId = "r1"),
            rectanglePlacementOf(x = 2.0, y = 0.0, width = 2.0, height = 2.0, itemId = "r2")
        )

        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest"
        )
        assertTrue(result is Ok, "edge contact must not be treated as an overlap")
    }

    @Test
    fun mixedShapesWithoutOverlapAreAccepted() {
        val sheet = sheetShapeOf(width = 4.0, height = 2.0)
        val placements = listOf(
            rectanglePlacementOf(x = 0.0, y = 0.0, width = 2.0, height = 2.0, itemId = "r1"),
            circlePlacementOf(x = 2.0, y = 0.0, radius = 0.5, itemId = "c1"),
            circlePlacementOf(x = 3.0, y = 0.0, radius = 0.5, itemId = "c2")
        )

        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest"
        )
        assertTrue(result is Ok)
    }

    @Test
    fun requireSheetGeometryDoesNotCheckRotation() {
        val sheet = sheetShapeOf(width = 10.0, height = 10.0)
        val placements = listOf(
            rectanglePlacementOf(
                x = 0.0,
                y = 0.0,
                width = 1.0,
                height = 3.0,
                rotated = true,
                itemId = "r1"
            )
        )

        val result = PackingGeometryGuard.requireSheetGeometry(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest"
        )
        assertTrue(result is Ok)
    }

    @Test
    fun emptyPlacementListIsAccepted() {
        val sheet = sheetShapeOf(width = 1.0, height = 1.0)
        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = emptyList(),
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest"
        )
        assertTrue(result is Ok)
    }

    @Test
    fun rotationInvariantShapesStayLegalUnderSymmetricRule() {
        val sheet = sheetShapeOf(width = 4.0, height = 4.0)
        val placements = listOf(
            rectanglePlacementOf(
                x = 0.0,
                y = 0.0,
                width = 2.0,
                height = 2.0,
                rotated = true,
                itemId = "square"
            ),
            circlePlacementOf(
                x = 2.0,
                y = 0.0,
                radius = 1.0,
                rotated = true,
                itemId = "circle"
            )
        )

        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest",
            rotationRule = PackingRotationRule.SymmetricOnly
        )
        assertTrue(result is Ok)
    }

    @Test
    fun itemCatalogRuleHonoursAllowRotate() {
        val sheet = sheetShapeOf(width = 10.0, height = 10.0)
        val placements = listOf(
            rectanglePlacementOf(
                x = 0.0,
                y = 0.0,
                width = 1.0,
                height = 3.0,
                rotated = true,
                itemId = "r1"
            )
        )
        val fixedRule = PackingRotationRule.byItemCatalog(
            mapOf(ItemId("r1") to rectangleItemOf(id = "r1", allowRotate = false))
        )

        val fixed = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest",
            rotationRule = fixedRule
        )
        assertTrue(fixed is Failed)

        val rotatableRule = PackingRotationRule.byItemCatalog(
            mapOf(ItemId("r1") to rectangleItemOf(id = "r1", allowRotate = true))
        )
        val rotatable = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest",
            rotationRule = rotatableRule
        )
        assertTrue(rotatable is Ok)
    }

    @Test
    fun unknownItemsAreTreatedAsAllowingRotation() {
        val sheet = sheetShapeOf(width = 10.0, height = 10.0)
        val placements = listOf(
            rectanglePlacementOf(
                x = 0.0,
                y = 0.0,
                width = 1.0,
                height = 3.0,
                rotated = true,
                itemId = "unknown"
            )
        )
        val rule = PackingRotationRule.byItemCatalog(
            mapOf(ItemId("r1") to rectangleItemOf(id = "r1", allowRotate = false))
        )

        val result = PackingGeometryGuard.validate(
            sheet = sheet,
            placements = placements,
            sheetName = "sheet-1",
            source = "PackingGeometryGuardContractTest",
            rotationRule = rule
        )
        assertTrue(result is Ok)
    }

    @Test
    fun sheetGeometryExposesWidthHeightAndArea() {
        val sheet = sheetShapeOf(width = 3.0, height = 3.0)
        assertEquals(3.0, sheet.width.value.toDouble(), 1e-9)
        assertEquals(3.0, sheet.height.value.toDouble(), 1e-9)
        assertEquals(9.0, sheet.area.value.toDouble(), 1e-9)
    }
}
