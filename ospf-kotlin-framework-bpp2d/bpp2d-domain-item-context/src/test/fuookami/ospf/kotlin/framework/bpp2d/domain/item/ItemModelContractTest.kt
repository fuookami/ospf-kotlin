package fuookami.ospf.kotlin.framework.bpp2d.domain.item

import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.service.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

class ItemModelContractTest {
    private fun rectangleItem(
        id: String,
        width: Double,
        height: Double,
        allowRotate: Boolean = false
    ): ActualItem<FltX> {
        val shape = rectangleShape2(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter
        ).valueOrFail()
        return ActualItem(
            id = ItemId(id),
            name = id,
            shape = shape,
            allowRotate = allowRotate,
            weight = FltX(1.0) * Kilogram
        )
    }

    private fun circleItem(id: String, radius: Double): ActualItem<FltX> {
        val shape = circleShape2(FltX(radius) * Meter).valueOrFail()
        return ActualItem(
            id = ItemId(id),
            name = id,
            shape = shape,
            allowRotate = true,
            weight = FltX(1.0) * Kilogram
        )
    }

    @Test
    fun rectangleItemExposesBoundingAndActualArea() {
        val item = rectangleItem("r1", 4.0, 2.0)
        assertTrue(abs(item.width.value.toDouble() - 4.0) < 1e-9)
        assertTrue(abs(item.height.value.toDouble() - 2.0) < 1e-9)
        assertTrue(abs(item.boundingArea.value.toDouble() - 8.0) < 1e-9)
        assertEquals(PackingShape2Type.Rectangle, item.shapeType)
        assertFalse(item.isCircleItem())
    }

    @Test
    fun circleItemUsesPiRSquaredArea() {
        val item = circleItem("c1", 0.5)
        assertTrue(abs(item.boundingArea.value.toDouble() - 1.0) < 1e-9)
        assertTrue(abs(item.area.value.toDouble() - Math.PI * 0.25) < 1e-9)
        assertEquals(PackingShape2Type.Circle, item.shapeType)
        assertTrue(item.isCircleItem())
    }

    @Test
    fun candidateShapesHonourRotationAllowance() {
        val rotatable = rectangleItem("r2", 3.0, 1.0, allowRotate = true)
        assertEquals(2, rotatable.candidateShapes().size)
        assertEquals(1, rotatable.candidateShapes().count { it.rotated })

        val fixed = rectangleItem("r3", 3.0, 1.0, allowRotate = false)
        assertEquals(1, fixed.candidateShapes().size)

        val circle = circleItem("c2", 1.0)
        assertEquals(1, circle.candidateShapes().size)
    }
}

class PlacementFactoryContractTest {
    @Test
    fun placementKeepsItemIdAndRotation() {
        val item = ActualItem(
            id = ItemId("i1"),
            name = "i1",
            shape = rectangleShape2(
                width = FltX(2.0) * Meter,
                height = FltX(1.0) * Meter
            ).valueOrFail(),
            allowRotate = true,
            weight = FltX(1.0) * Kilogram
        )
        val position = QuantityPoint2(
            x = FltX(1.0) * Meter,
            y = FltX(2.0) * Meter
        )

        val upright = itemPlacement2Of(item, position).valueOrFail()
        assertEquals("i1", upright.itemId)
        assertTrue(abs(upright.width.value.toDouble() - 2.0) < 1e-9)

        val rotated = itemPlacement2Of(item, position, rotated = true).valueOrFail()
        assertTrue(abs(rotated.width.value.toDouble() - 1.0) < 1e-9)
        assertTrue(abs(rotated.height.value.toDouble() - 2.0) < 1e-9)
        assertTrue(rotated.rotated)
    }

    @Test
    fun placementRejectsUnallowedRotation() {
        val item = ActualItem(
            id = ItemId("i2"),
            name = "i2",
            shape = rectangleShape2(
                width = FltX(2.0) * Meter,
                height = FltX(1.0) * Meter
            ).valueOrFail(),
            allowRotate = false,
            weight = FltX(1.0) * Kilogram
        )
        val position = QuantityPoint2(
            x = FltX.zero * Meter,
            y = FltX.zero * Meter
        )
        assertTrue(itemPlacement2Of(item, position, rotated = true) is fuookami.ospf.kotlin.utils.functional.Failed)
    }

    @Test
    fun placementGeometryChecksReturnResults() {
        val shape = rectangleShape2(
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter
        ).valueOrFail()
        val lhs = Placement2(
            shape = shape,
            position = QuantityPoint2(x = FltX(0.0) * Meter, y = FltX(0.0) * Meter)
        )
        val overlapping = Placement2(
            shape = shape,
            position = QuantityPoint2(x = FltX(1.0) * Meter, y = FltX(1.0) * Meter)
        )
        val apart = Placement2(
            shape = shape,
            position = QuantityPoint2(x = FltX(5.0) * Meter, y = FltX(5.0) * Meter)
        )

        assertTrue(lhs.overlapped(overlapping).valueOrFail())
        assertFalse(lhs.overlapped(apart).valueOrFail())

        val sheet = sheet2Shape(
            width = FltX(3.0) * Meter,
            height = FltX(3.0) * Meter
        ).valueOrFail()
        assertTrue(lhs.insideSheet(sheet).valueOrFail())
        assertFalse(apart.insideSheet(sheet).valueOrFail())
    }
}

class ItemServiceContractTest {
    private fun item(id: String, width: Double, height: Double, batch: String? = null): ActualItem<FltX> {
        return ActualItem(
            id = ItemId(id),
            name = id,
            shape = rectangleShape2(
                width = FltX(width) * Meter,
                height = FltX(height) * Meter
            ).valueOrFail(),
            allowRotate = false,
            weight = FltX(1.0) * Kilogram,
            batchNo = batch?.let { BatchNo(it) }
        )
    }

    @Test
    fun mergerAccumulatesSameShapeDemands() {
        val merger = ItemMerger<FltX>()
        val merged = merger.merge(
            listOf(
                item("a", 2.0, 1.0) to UInt64(2),
                item("b", 2.0, 1.0) to UInt64(3),
                item("c", 3.0, 1.0) to UInt64(1)
            )
        )
        assertEquals(2, merged.size)
        val twoByOne = merged.first { abs(it.first.width.value.toDouble() - 2.0) < 1e-9 }
        assertEquals(UInt64(5), twoByOne.second)
    }

    @Test
    fun mergerKeepsDifferentBatchesApart() {
        val merger = ItemMerger<FltX>()
        val merged = merger.merge(
            listOf(
                item("a", 2.0, 1.0, batch = "b1") to UInt64(1),
                item("b", 2.0, 1.0, batch = "b2") to UInt64(1)
            )
        )
        assertEquals(2, merged.size)
    }

    @Test
    fun loadingOrderSortsBottomThenLeft() {
        val calculator = LoadingOrderCalculator<FltX>()
        val shape = rectangleShape2(
            width = FltX(1.0) * Meter,
            height = FltX(1.0) * Meter
        ).valueOrFail()
        val topLeft = Placement2(
            shape = shape,
            position = QuantityPoint2(x = FltX(0.0) * Meter, y = FltX(5.0) * Meter)
        )
        val bottomRight = Placement2(
            shape = shape,
            position = QuantityPoint2(x = FltX(5.0) * Meter, y = FltX(0.0) * Meter)
        )
        val bottomLeft = Placement2(
            shape = shape,
            position = QuantityPoint2(x = FltX(0.0) * Meter, y = FltX(0.0) * Meter)
        )

        val orders = calculator.invoke(listOf(topLeft, bottomRight, bottomLeft))
        assertEquals(UInt64(1), orders[bottomLeft])
        assertEquals(UInt64(2), orders[bottomRight])
        assertEquals(UInt64(3), orders[topLeft])
    }
}

class SheetAndLayerContractTest {
    @Test
    fun sheetTypeComputesAreaAndFit() {
        val type = SheetType(
            width = FltX(10.0) * Meter,
            height = FltX(6.0) * Meter,
            maxLoad = FltX(100.0) * Kilogram,
            typeCode = SheetTypeId("S1"),
            isMain = true
        )
        assertTrue(abs(type.area.value.toDouble() - 60.0) < 1e-9)
        val shape = rectangleShape2(
            width = FltX(12.0) * Meter,
            height = FltX(1.0) * Meter
        ).valueOrFail()
        assertFalse(type.enabled(shape).valueOrFail())
    }

    @Test
    fun layerAggregatesAmountAreaAndUtilization() {
        val type = SheetType(
            width = FltX(10.0) * Meter,
            height = FltX(10.0) * Meter,
            maxLoad = FltX(100.0) * Kilogram,
            typeCode = SheetTypeId("S1")
        )
        val shape = rectangleShape2(
            width = FltX(2.0) * Meter,
            height = FltX(1.0) * Meter
        ).valueOrFail()
        val units = listOf(
            Placement2(
                shape = shape,
                position = QuantityPoint2(x = FltX(0.0) * Meter, y = FltX(0.0) * Meter),
                itemId = "i1"
            ),
            Placement2(
                shape = shape,
                position = QuantityPoint2(x = FltX(2.0) * Meter, y = FltX(0.0) * Meter),
                itemId = "i1"
            )
        )
        val layer = Layer(
            iteration = 0L,
            from = Layer::class,
            sheet = type,
            shape = sheet2Shape(
                width = FltX(4.0) * Meter,
                height = FltX(1.0) * Meter
            ).valueOrFail(),
            units = units
        )

        assertEquals(UInt64(2), layer.amount)
        assertTrue(abs(layer.area.value.toDouble() - 4.0) < 1e-9)
        assertTrue(abs(layer.utilization().toDouble() - 1.0) < 1e-9)
        assertEquals(UInt64(2), layer.itemAmounts[ItemId("i1")])
    }

    @Test
    fun aggregationTracksRestDemands() {
        val type = SheetType(
            width = FltX(10.0) * Meter,
            height = FltX(6.0) * Meter,
            maxLoad = FltX(100.0) * Kilogram,
            typeCode = SheetTypeId("S1"),
            isMain = true
        )
        val aggregation = Aggregation<FltX>(
            itemDemands = mapOf(ItemId("i1") to UInt64(5)),
            sheetTypes = mapOf(type to UInt64(10))
        )
        assertEquals(UInt64(5), aggregation.restItems[ItemId("i1")])
        assertNotNull(aggregation.mainSheet)

        aggregation.useItems(mapOf(ItemId("i1") to UInt64(2)))
        assertEquals(UInt64(3), aggregation.restItems[ItemId("i1")])

        aggregation.useSheets(mapOf(type to UInt64(4)))
        assertEquals(UInt64(6), aggregation.restSheets[type])
    }
}