package fuookami.ospf.kotlin.framework.geometry2d

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.Centimeter
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.SquareMeter

class QuantityCoreContractTest {

    private fun fltX(value: Double) = FltX(value) * Meter
    private fun flt64(value: Double) = Flt64(value) * Meter

    @Test
    fun additionKeepsLeftUnitAndValue() {
        val lhs = FltX(2.0) * Meter
        val rhs = FltX(3.0) * Meter
        val sum = quantityPlusSafe(lhs, rhs).valueOrFail()
        assertEquals(FltX(5.0), sum.value)
        assertEquals(Meter, sum.unit)
    }

    @Test
    fun additionFailsOnDimensionMismatch() {
        val length = FltX(2.0) * Meter
        val area = (FltX(3.0) * Meter) * (FltX(4.0) * Meter)
        assertTrue(quantityPlusSafe(length, area) is fuookami.ospf.kotlin.utils.functional.Failed)
    }

    @Test
    fun multiplicationComposesAreaDimension() {
        val width = FltX(2.0) * Meter
        val height = FltX(3.0) * Meter
        val area = width * height
        assertTrue(abs(area.value.toDouble() - 6.0) < 1e-9)
        val converted = area.convertTo(SquareMeter)
        assertTrue(converted != null)
        assertTrue(abs(converted!!.value.toDouble() - 6.0) < 1e-9)
    }

    @Test
    fun productUnitComposesBasePowers() {
        // `PhysicalUnit.*` 会把导出量纲的基础幂次再乘一次，这里验证基础幂次合成正确。
        // `PhysicalUnit.*` re-applies base powers of derived dimensions; this verifies base-power composition.
        assertEquals("L^2", productUnit(Meter, Meter).quantity.dimensionSymbol())
        assertEquals(AREA_DIMENSION, productUnit(Meter, Meter).quantity)
        assertEquals("L^3", productUnit(Meter, productUnit(Meter, Meter)).quantity.dimensionSymbol())
        assertEquals("1", quotientUnit(Meter, Meter).quantity.dimensionSymbol())
    }

    @Test
    fun comparisonWorksForFloatAndFltXSpecialisations() {
        assertTrue(quantityGreater(FltX(2.0) * Meter, FltX(1.0) * Meter))
        assertTrue(quantityGreater(Flt64(2.0) * Meter, Flt64(1.0) * Meter))
        assertTrue(quantityEqual(FltX(2.0) * Meter, fltX(2.0)))
        assertFalse(quantityEqual(FltX(2.0) * Meter, FltX(3.0) * Meter))
        assertTrue(quantityOrdOrNull(flt64(1.0), flt64(2.0)) is Order.Less)
    }

    @Test
    fun genericSpecialisationKeepsFlt64PathAlive() {
        val width = flt64(2.0)
        val height = flt64(4.0)
        val area = width * height
        assertEquals(Flt64(8.0), area.value)
        assertTrue(quantityGreater(area, width * flt64(1.0)))
    }

    @Test
    fun aggregateHelpersHandleEmptyAndNonEmptySequences() {
        val empty = listOf<fuookami.ospf.kotlin.quantities.quantity.Quantity<FltX>>()
        assertEquals(null, quantitySum(empty))

        val values = listOf(fltX(1.0), fltX(2.0), fltX(3.5))
        assertEquals(FltX(6.5), quantitySum(values)!!.value)

        val largest = values.maxByQuantityOrNull { it }
        assertEquals(FltX(3.5), largest!!.value)
        val smallest = values.minByQuantityOrNull { it }
        assertEquals(FltX(1.0), smallest!!.value)
    }
}

class PackingShape2ContractTest {
    @Test
    fun rectangleAreaIsWidthTimesHeight() {
        val shape = RectanglePackingShape2(
            width = FltX(4.0) * Meter,
            height = FltX(2.5) * Meter
        )
        assertEquals(PackingShape2Type.Rectangle, shape.shapeType)
        assertTrue(abs(shape.area.value.toDouble() - 10.0) < 1e-9)
        assertTrue(abs(shape.boundingArea.value.toDouble() - 10.0) < 1e-9)
        val geometry = shape.geometry() as fuookami.ospf.kotlin.math.geometry.QuantityRectangle2
        assertTrue(abs((geometry.width * geometry.height).value.toDouble() - 10.0) < 1e-9)
        assertEquals("L^2", shape.area.unit.quantity.dimensionSymbol())
    }

    @Test
    fun circleAreaUsesPiRSquaredAndBoundingAreaUsesSquare() {
        val radius = FltX(0.5) * Meter
        val shape = CirclePackingShape2(radius = radius)

        assertEquals(PackingShape2Type.Circle, shape.shapeType)
        assertTrue(abs(shape.area.value.toDouble() - PI * 0.25) < 1e-9)
        assertTrue(abs(shape.boundingArea.value.toDouble() - 1.0) < 1e-9)
        assertTrue(abs(shape.boundingWidth.value.toDouble() - 1.0) < 1e-9)
    }

    @Test
    fun rotationCandidatesHonourAllowRotate() {
        val rectangle = RectanglePackingShape2(
            width = FltX(2.0) * Meter,
            height = FltX(1.0) * Meter
        )
        assertEquals(1, rectangle.candidateShapes(allowRotate = false).size)
        val rotated = rectangle.candidateShapes(allowRotate = true)
        assertEquals(2, rotated.size)
        assertEquals(FltX(1.0), rotated[1].shape.boundingWidth.value)
        assertEquals(FltX(2.0), rotated[1].shape.boundingHeight.value)

        val circle = CirclePackingShape2(radius = FltX(0.5) * Meter)
        assertEquals(1, circle.candidateShapes(allowRotate = true).size)

        val square = RectanglePackingShape2(
            width = FltX(1.0) * Meter,
            height = FltX(1.0) * Meter
        )
        assertEquals(1, square.candidateShapes(allowRotate = true).size)
    }

    @Test
    fun factoriesRejectNonPositiveSizes() {
        assertTrue(rectangleShape2(FltX.zero * Meter, FltX(1.0) * Meter) is fuookami.ospf.kotlin.utils.functional.Failed)
        assertTrue(circleShape2(FltX.zero * Meter) is fuookami.ospf.kotlin.utils.functional.Failed)
        assertTrue(rectangleShape2(FltX(1.0) * Meter, FltX(1.0) * Meter) is fuookami.ospf.kotlin.utils.functional.Ok)
    }
}

class Box2GeometryContractTest {
    private fun rectangleBox(
        x: Double,
        y: Double,
        width: Double,
        height: Double
    ): Box2Geometry<FltX> {
        return Box2Geometry.of(
            x = FltX(x) * Meter,
            y = FltX(y) * Meter,
            width = FltX(width) * Meter,
            height = FltX(height) * Meter
        )
    }

    @Test
    fun rectangleRectangleOverlapTreatsEdgeContactAsDisjoint() {
        val lhs = rectangleBox(0.0, 0.0, 2.0, 2.0)
        val overlapping = rectangleBox(1.0, 1.0, 2.0, 2.0)
        val touching = rectangleBox(2.0, 0.0, 2.0, 2.0)

        assertTrue(lhs.overlapped(overlapping).valueOrFail())
        assertFalse(lhs.overlapped(touching).valueOrFail())
    }

    @Test
    fun circleCircleOverlapUsesCentreDistance() {
        val lhs = Box2Geometry(
            x = FltX(0.0) * Meter,
            y = FltX(0.0) * Meter,
            shape = CirclePackingShape2(FltX(1.0) * Meter).geometry()
        )
        val near = Box2Geometry(
            x = FltX(1.5) * Meter,
            y = FltX(0.0) * Meter,
            shape = CirclePackingShape2(FltX(1.0) * Meter).geometry()
        )
        val far = Box2Geometry(
            x = FltX(2.5) * Meter,
            y = FltX(0.0) * Meter,
            shape = CirclePackingShape2(FltX(1.0) * Meter).geometry()
        )

        assertTrue(lhs.overlapped(near).valueOrFail())
        assertFalse(lhs.overlapped(far).valueOrFail())
    }

    @Test
    fun circleRectangleOverlapIsDetected() {
        val circle = Box2Geometry(
            x = FltX(0.0) * Meter,
            y = FltX(0.0) * Meter,
            shape = CirclePackingShape2(FltX(1.0) * Meter).geometry()
        )
        val inside = rectangleBox(0.5, 0.5, 0.5, 0.5)
        val outside = rectangleBox(2.0, 2.0, 1.0, 1.0)

        assertTrue(circle.overlapped(inside).valueOrFail())
        assertFalse(circle.overlapped(outside).valueOrFail())
    }

    @Test
    fun insideChecksAgainstSheetBounds() {
        val sheet = sheet2Shape(
            width = FltX(10.0) * Meter,
            height = FltX(6.0) * Meter
        ).valueOrFail()
        val inner = rectangleBox(1.0, 1.0, 8.0, 4.0)
        val edgeTouching = rectangleBox(0.0, 0.0, 10.0, 6.0)
        val outOfBounds = rectangleBox(9.5, 1.0, 1.0, 1.0)

        assertTrue(inner.inside(sheet.originBox()).valueOrFail())
        assertTrue(edgeTouching.inside(sheet.originBox()).valueOrFail())
        assertFalse(outOfBounds.inside(sheet.originBox()).valueOrFail())
    }

    @Test
    fun sheetUsesCentimetreConversion() {
        val sheet = sheet2Shape(
            width = FltX(1.0) * Meter,
            height = FltX(100.0) * Centimeter
        ).valueOrFail()
        val fits = sheet.enabled(
            boxWidth = FltX(90.0) * Centimeter,
            boxHeight = FltX(50.0) * Centimeter
        ).valueOrFail()
        assertTrue(fits)

        // 100 cm 与 1 m 是同量纲同数值，故 101 cm 超出。
        val tooWide = sheet.enabled(
            boxWidth = FltX(101.0) * Centimeter,
            boxHeight = FltX(50.0) * Centimeter
        ).valueOrFail()
        assertFalse(tooWide)
    }

}

/** 期望的面积量纲 `L²` / Expected area dimension `L²`. */
private val AREA_DIMENSION = fuookami.ospf.kotlin.quantities.dimension.StandardFundamentalQuantityDimension.Length.lengthSquared()

/**
 * 构造 `L²` 量纲。 / Build the `L²` dimension.
 *
 * @return `L²` 量纲 / `L²` dimension
*/
private fun fuookami.ospf.kotlin.quantities.dimension.FundamentalQuantityDimension.lengthSquared() =
    fuookami.ospf.kotlin.quantities.dimension.DerivedQuantity(
        quantity = fuookami.ospf.kotlin.quantities.dimension.FundamentalQuantity(
            dimension = this,
            index = 2
        ),
        name = "area",
        symbol = "S"
    )