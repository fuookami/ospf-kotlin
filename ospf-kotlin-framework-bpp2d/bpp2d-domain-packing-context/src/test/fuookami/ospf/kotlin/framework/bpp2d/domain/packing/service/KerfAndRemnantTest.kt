package fuookami.ospf.kotlin.framework.bpp2d.domain.packing.service

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Sheet
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetLayer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetType
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetTypeId
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.Sheet2Shape

class KerfContractTest {
    private fun shape(width: Double, height: Double): PackingShape2<FltX> {
        return rectangleShape2(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter
        ).valueOrFail()
    }

    private fun sheetType(width: Double, height: Double): SheetType<FltX> {
        return SheetType(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter,
            maxLoad = FltX(1000.0) * Kilogram,
            typeCode = SheetTypeId("S"),
            isMain = true
        )
    }

    @Test
    fun inflateGrowsRectangleAndCircleBySpacing() {
        val rectangle = shape(2.0, 1.0).inflatedBy(FltX(0.2) * Meter)
        assertEquals(2.2, rectangle.boundingWidth.value.toDouble(), 1e-9)
        assertEquals(1.2, rectangle.boundingHeight.value.toDouble(), 1e-9)

        val circle = circleShape2(FltX(0.5) * Meter).valueOrFail()
        val inflated = circle.inflatedBy(FltX(0.2) * Meter) as CirclePackingShape2
        // 直径 1.0 + 0.2 → 半径 0.6。 / Diameter 1.0 plus 0.2 gives a radius of 0.6.
        assertEquals(0.6, inflated.radius.value.toDouble(), 1e-9)
    }

    @Test
    fun inflateKeepsShapeWhenSpacingIsZero() {
        val original = shape(2.0, 1.0)
        val same = original.inflatedBy(quantityZeroOf(original.boundingWidth))
        assertEquals(original, same)
    }

    @Test
    fun pitchAddsKerfToBothAxes() {
        val pitch = pitchOf(shape(2.0, 1.0), FltX(0.5) * Meter)
        assertEquals(2.5, pitch.x.value.toDouble(), 1e-9)
        assertEquals(1.5, pitch.y.value.toDouble(), 1e-9)
    }

    @Test
    fun guardRejectsGapBelowKerf() {
        val guard = PackingGeometryGuard
        val type = sheetType(10.0, 10.0)
        val item = shape(2.0, 2.0)
        // 两件相距 0.1 m，kerf 0.5 m。 / Two pieces 0.1 m apart with a 0.5 m kerf.
        val lhs = LayerPlacement2(
            shape = item,
            position = QuantityPoint2(x = FltX(0.0) * Meter, y = FltX(0.0) * Meter),
            itemId = "A"
        )
        val rhs = LayerPlacement2(
            shape = item,
            position = QuantityPoint2(x = FltX(2.1) * Meter, y = FltX(0.0) * Meter),
            itemId = "B"
        )

        // 无 kerf 时合法，有 kerf 时非法。 / Legal without a kerf, illegal with one.
        assertTrue(guard.validate(sheet = type, placements = listOf(lhs, rhs)).isSuccess())
        assertTrue(
            !guard.validate(
                sheet = type,
                placements = listOf(lhs, rhs),
                kerf = FltX(0.5) * Meter
            ).isSuccess()
        )
    }

    @Test
    fun guardAcceptsGapNotBelowKerf() {
        val guard = PackingGeometryGuard
        val type = sheetType(10.0, 10.0)
        val item = shape(2.0, 2.0)
        val lhs = LayerPlacement2(
            shape = item,
            position = QuantityPoint2(x = FltX(0.0) * Meter, y = FltX(0.0) * Meter),
            itemId = "A"
        )
        val rhs = LayerPlacement2(
            shape = item,
            position = QuantityPoint2(x = FltX(2.5) * Meter, y = FltX(0.0) * Meter),
            itemId = "B"
        )
        assertTrue(
            guard.validate(
                sheet = type,
                placements = listOf(lhs, rhs),
                kerf = FltX(0.5) * Meter
            ).isSuccess()
        )
    }
}

class FreeSpaceAnalyzerContractTest {
    private fun sheetType(width: Double, height: Double): SheetType<FltX> {
        return SheetType(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter,
            maxLoad = FltX(1000.0) * Kilogram,
            typeCode = SheetTypeId("S"),
            isMain = true
        )
    }

    private fun sheetOf(
        type: SheetType<FltX>,
        placements: List<LayerPlacement2<FltX>>
    ): Sheet<FltX> {
        val layer = Layer(
            iteration = 0L,
            from = Layer::class,
            sheet = type,
            shape = Sheet2Shape(width = type.width, height = type.height),
            units = placements
        )
        return Sheet(
            type = type,
            layers = listOf(
                SheetLayer(
                    type = type,
                    shape = Sheet2Shape(width = type.width, height = type.height),
                    layers = listOf(layer)
                )
            )
        )
    }

    @Test
    fun emptySheetIsOneFullRemnant() {
        val type = sheetType(10.0, 6.0)
        val remnants = FreeSpaceAnalyzer().analyze(listOf(sheetOf(type, emptyList())))
        assertEquals(1, remnants.size)
        assertEquals(60.0, remnants.first().area.value.toDouble(), 1e-9)
    }

    @Test
    fun remnantsDoNotOverlapOccupiedArea() {
        val type = sheetType(10.0, 6.0)
        val shape = rectangleShape2(
            width = FltX(4.0) * Meter,
            height = FltX(2.0) * Meter
        ).valueOrFail()
        val placement = LayerPlacement2(
            shape = shape,
            position = QuantityPoint2(x = FltX(0.0) * Meter, y = FltX(0.0) * Meter),
            itemId = "A"
        )
        val remnants = FreeSpaceAnalyzer().analyze(listOf(sheetOf(type, listOf(placement))))

        assertTrue(remnants.isNotEmpty())
        val totalRemnantArea = remnants.sumOf { it.area.value.toDouble() }
        // 已用 8 m²，余料合计应为 52 m²。 / 8 m² occupied, so the remnants total 52 m².
        assertEquals(52.0, totalRemnantArea, 1e-9)

        val occupied = placement.box()
        for (remnant in remnants) {
            assertTrue(
                !remnant.rectangle.overlapped(occupied).valueOrFail(),
                "余料不应与已用区域重叠 / a remnant must not overlap the occupied area"
            )
        }
    }

    @Test
    fun remnantsAreSortedByAreaDescending() {
        val type = sheetType(10.0, 6.0)
        val shape = rectangleShape2(
            width = FltX(1.0) * Meter,
            height = FltX(1.0) * Meter
        ).valueOrFail()
        val placements = listOf(
            LayerPlacement2(
                shape = shape,
                position = QuantityPoint2(x = FltX(4.0) * Meter, y = FltX(2.0) * Meter),
                itemId = "A"
            )
        )
        val remnants = FreeSpaceAnalyzer().analyze(listOf(sheetOf(type, placements)))
        for (index in 1 until remnants.size) {
            assertTrue(
                remnants[index - 1].area.value >= remnants[index].area.value,
                "余料应按面积降序 / remnants must be sorted by area descending"
            )
        }
    }
}

private fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is fuookami.ospf.kotlin.utils.functional.Failed ->
            kotlin.test.fail("$message: ${error.message}")

        is fuookami.ospf.kotlin.utils.functional.Fatal ->
            kotlin.test.fail("$message: ${errors.joinToString { it.message }}")
    }
}
/**
 * 判断结果为成功。 / Check whether the result is a success.
 *
 * @return 是否成功 / whether successful
*/
private fun fuookami.ospf.kotlin.utils.functional.Try.isSuccess(): Boolean {
    return this is fuookami.ospf.kotlin.utils.functional.Ok
}