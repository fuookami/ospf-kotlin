/**
 * 物料、产品、图案与方案契约测试。 / Material, product, pattern and plan contract test.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.material.model

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CutOrientation2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CutTree2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Region2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.region2Of
import fuookami.ospf.kotlin.framework.geometry2d.*

class Material2ContractTest {

    /**
     * 构造米制量值。 / Build a metric quantity.
     *
     * @param value 数值 / value
     * @return 量值 / quantity
    */
    private fun meter(value: Double): Quantity<Flt64> = Flt64(value) * Meter

    @Test
    fun coilAndSheetAreDistinguishedByHeight() {
        // 无板高 → 卷材；有板高 → 定尺板材。 / No height means a coil; a height means a fixed-size sheet.
        val coil = Material2(
            id = materialId2Of("COIL"),
            name = "COIL",
            width = meter(100.0)
        )
        val sheet = Material2(
            id = materialId2Of("SHEET"),
            name = "SHEET",
            width = meter(100.0),
            height = meter(60.0)
        )
        assertEquals(Material2Kind.Coil, coil.kind)
        assertTrue(coil.isCoil)
        assertEquals(Material2Kind.Sheet, sheet.kind)
        assertEquals(6000.0, sheet.sheetArea()!!.value.toDouble(), 1e-9)
        assertEquals(null, coil.sheetArea())
    }

    @Test
    fun regionUsesSheetHeightOrSegmentHeight() {
        val sheet = Material2(
            id = materialId2Of("SHEET"),
            name = "SHEET",
            width = meter(100.0),
            height = meter(60.0)
        )
        val coil = Material2(
            id = materialId2Of("COIL"),
            name = "COIL",
            width = meter(100.0)
        )
        assertEquals(6000.0, sheet.region()!!.area.value.toDouble(), 1e-9)
        // 卷材模式下不指定段高无法构造料域。 / A coil cannot build a region without a segment height.
        assertEquals(null, coil.region())
        assertEquals(8000.0, coil.region(meter(80.0))!!.area.value.toDouble(), 1e-9)
    }

    @Test
    fun materialValidationRejectsNonPositiveDimensions() {
        assertTrue(
            Material2(id = materialId2Of("M"), name = "M", width = meter(0.0)).validate() is Failed
        )
        assertTrue(
            Material2(id = materialId2Of("M"), name = "M", width = meter(10.0), height = meter(0.0))
                .validate() is Failed
        )
        assertTrue(
            Material2(id = materialId2Of("M"), name = "M", width = meter(10.0), height = meter(10.0))
                .validate() is Ok
        )
    }

    @Test
    fun remnantThresholdDecidesReusability() {
        val material = Material2(
            id = materialId2Of("M"),
            name = "M",
            width = meter(100.0),
            height = meter(60.0),
            minRemnantArea = (quantityMultiply(meter(10.0), meter(10.0)))
        )
        assertTrue(material.isUsableRemnant(quantityMultiply(meter(20.0), meter(20.0))))
        assertTrue(!material.isUsableRemnant(quantityMultiply(meter(5.0), meter(5.0))))
    }

    @Test
    fun machineWidthLimitGatesMaterial() {
        val wide = Material2(id = materialId2Of("W"), name = "W", width = meter(200.0))
        val narrow = Material2(id = materialId2Of("N"), name = "N", width = meter(80.0))
        val machine = Machine2<Flt64>(
            id = machineId2Of("CUT-1"),
            name = "CUT-1",
            maxWidth = meter(100.0)
        )
        assertTrue(!wide.enabledBy(machine))
        assertTrue(narrow.enabledBy(machine))
    }
}

class ProductDemand2ContractTest {

    /**
     * 构造米制量值。 / Build a metric quantity.
     *
     * @param value 数值 / value
     * @return 量值 / quantity
    */
    private fun meter(value: Double): Quantity<Flt64> = Flt64(value) * Meter

    /**
     * 构造产品。 / Build a product.
     *
     * @param id 产品编号 / product identifier
     * @param allowRotate 是否允许旋转 / whether rotation is allowed
     * @return 产品 / product
    */
    private fun product(id: String, allowRotate: Boolean = true): Product2<Flt64> {
        return Product2(
            id = productId2Of(id),
            name = id,
            width = meter(40.0),
            height = meter(30.0),
            allowRotate = allowRotate,
            unitWeight = Flt64(0.5) * Kilogram
        )
    }

    @Test
    fun productAreaAndOrientations() {
        val rotatable = product("P1")
        assertEquals(1200.0, rotatable.area.value.toDouble(), 1e-9)
        assertEquals(listOf(false, true), rotatable.orientations())
        assertEquals(listOf(false), product("P2", allowRotate = false).orientations())
    }

    @Test
    fun demandValueCarriesItsOwnUnit() {
        // 需求量用密封值类型表达，模式由值本身给出，避免「量值 + 模式」互相矛盾。
        // The demand uses a sealed value type, so the mode follows from the value and cannot contradict it.
        val amount = ProductDemand2(
            product = product("P1"),
            demand = DemandValue2.Amount(UInt64(5))
        )
        assertEquals(DemandMode2.Amount, amount.mode)
        assertEquals(UInt64(5), amount.piecesOrNull())

        val area = ProductDemand2(
            product = product("P1"),
            demand = DemandValue2.Area(quantityMultiply(meter(10.0), meter(10.0)))
        )
        assertEquals(DemandMode2.Area, area.mode)
        assertEquals(null, area.piecesOrNull())
    }

    @Test
    fun zeroAmountAndNonPositiveAreaAreRejected() {
        assertTrue(
            ProductDemand2(product("P1"), DemandValue2.Amount(UInt64.zero)).validate() is Failed
        )
        assertTrue(
            ProductDemand2(product("P1"), DemandValue2.Area(quantityMultiply(meter(0.0), meter(0.0)))).validate() is Failed
        )
        assertTrue(
            ProductDemand2(product("P1"), DemandValue2.Amount(UInt64(3))).validate() is Ok
        )
    }

    @Test
    fun duplicateProductsAreRejected() {
        val demands = ProductDemands2(
            listOf(
                ProductDemand2(product("P1"), DemandValue2.Amount(UInt64(1))),
                ProductDemand2(product("P1"), DemandValue2.Amount(UInt64(2)))
            )
        )
        assertTrue(demands.validate() is Failed)
    }
}

class Pattern2ContractTest {

    /**
     * 构造米制量值。 / Build a metric quantity.
     *
     * @param value 数值 / value
     * @return 量值 / quantity
    */
    private fun meter(value: Double): Quantity<Flt64> = Flt64(value) * Meter

    /**
     * 构造 100×60 的定尺板材。 / Build a 100 by 60 fixed-size sheet material.
     *
     * @return 物料 / material
    */
    private fun sheet(): Material2<Flt64> {
        return Material2(
            id = materialId2Of("SHEET"),
            name = "SHEET",
            width = meter(100.0),
            height = meter(60.0),
            minRemnantArea = quantityMultiply(meter(10.0), meter(10.0))
        )
    }

    /**
     * 构造矩形放置。 / Build a rectangular placement.
     *
     * @param productId 产品编号 / product identifier
     * @param x 左下角 x 坐标 / lower-left x coordinate
     * @param y 左下角 y 坐标 / lower-left y coordinate
     * @param width 宽度 / width
     * @param height 高度 / height
     * @return 放置 / placement
    */
    private fun placement(
        productId: String,
        x: Double,
        y: Double,
        width: Double,
        height: Double
    ): Placement2<Flt64> {
        val shape = rectangleShape2(
            width = Flt64(width) * Meter,
            height = Flt64(height) * Meter
        ).valueOrFail()
        return Placement2(
            shape = shape,
            position = QuantityPoint2(x = Flt64(x) * Meter, y = Flt64(y) * Meter),
            itemId = productId
        )
    }

    /**
     * 构造板材料域。 / Build the sheet region.
     *
     * @return 料域 / region
    */
    private fun region(): Region2<Flt64> {
        return region2Of(
            x = meter(0.0),
            y = meter(0.0),
            width = meter(100.0),
            height = meter(60.0)
        )
    }

    private fun zeroKerf(): Quantity<Flt64> = quantityZeroOf(meter(1.0))

    @Test
    fun freeformPatternComputesAreasAndUtilization() {
        val pattern = Pattern2(
            id = patternId2Of("PAT-1"),
            material = sheet(),
            region = region(),
            placements = listOf(
                placement("P1", 0.0, 0.0, 60.0, 60.0),
                placement("P2", 60.0, 0.0, 40.0, 30.0)
            )
        )
        // 占用 3600 + 1200 = 4800，料域 6000，余料 1200。
        // Occupied 3600 plus 1200 equals 4800 out of 6000, so the remnant is 1200.
        assertEquals(4800.0, pattern.usedArea.value.toDouble(), 1e-9)
        assertEquals(1200.0, pattern.wasteArea.value.toDouble(), 1e-9)
        assertEquals(0.8, pattern.utilization().toDouble(), 1e-9)
        assertTrue(pattern.validate(zeroKerf()) is Ok)
    }

    @Test
    fun overlappingPlacementsAreRejected() {
        val pattern = Pattern2(
            id = patternId2Of("PAT-1"),
            material = sheet(),
            region = region(),
            placements = listOf(
                placement("P1", 0.0, 0.0, 60.0, 60.0),
                placement("P2", 30.0, 10.0, 40.0, 30.0)
            )
        )
        assertTrue(pattern.validate(zeroKerf()) is Failed)
    }

    @Test
    fun placementOutsideRegionIsRejected() {
        val pattern = Pattern2(
            id = patternId2Of("PAT-1"),
            material = sheet(),
            region = region(),
            placements = listOf(placement("P1", 80.0, 0.0, 40.0, 30.0))
        )
        assertTrue(pattern.validate(zeroKerf()) is Failed)
    }

    @Test
    fun guillotineProcessRequiresCutTree() {
        val pattern = Pattern2(
            id = patternId2Of("PAT-1"),
            material = sheet(),
            region = region(),
            placements = listOf(placement("P1", 0.0, 0.0, 60.0, 60.0)),
            process = CuttingProcess2.GuillotineTwoStage
        )
        assertTrue(pattern.validate(zeroKerf()) is Failed)
    }

    @Test
    fun guillotinePatternWithConsistentCutTreeIsAccepted() {
        val region = region()
        val leftRegion = region2Of(x = meter(0.0), y = meter(0.0), width = meter(60.0), height = meter(60.0))
        val rightRegion = region2Of(x = meter(60.0), y = meter(0.0), width = meter(40.0), height = meter(60.0))
        val cutTree = CutTree2.split2Of(
            region = region,
            orientation = CutOrientation2.Vertical,
            position = meter(60.0),
            stage = 1,
            first = CutTree2.Leaf2(region = leftRegion, stage = 1, pieceId = "P1"),
            second = CutTree2.Leaf2(region = rightRegion, stage = 1, pieceId = "P2"),
            kerf = zeroKerf()
        ).valueOrFail()
        val pattern = Pattern2(
            id = patternId2Of("PAT-1"),
            material = sheet(),
            region = region,
            placements = listOf(
                placement("P1", 0.0, 0.0, 60.0, 60.0),
                placement("P2", 60.0, 0.0, 40.0, 60.0)
            ),
            cutTree = cutTree,
            process = CuttingProcess2.GuillotineTwoStage
        )
        assertTrue(pattern.validate(zeroKerf()) is Ok)
    }

    @Test
    fun cutTreeLeafMismatchIsRejected() {
        val region = region()
        val leftRegion = region2Of(x = meter(0.0), y = meter(0.0), width = meter(60.0), height = meter(60.0))
        val rightRegion = region2Of(x = meter(60.0), y = meter(0.0), width = meter(40.0), height = meter(60.0))
        // 叶子标注的件与放置不一致。 / The leaves label different pieces than the placements.
        val cutTree = CutTree2.split2Of(
            region = region,
            orientation = CutOrientation2.Vertical,
            position = meter(60.0),
            stage = 1,
            first = CutTree2.Leaf2(region = leftRegion, stage = 1, pieceId = "P1"),
            second = CutTree2.Leaf2(region = rightRegion, stage = 1, pieceId = "P3"),
            kerf = zeroKerf()
        ).valueOrFail()
        val pattern = Pattern2(
            id = patternId2Of("PAT-1"),
            material = sheet(),
            region = region,
            placements = listOf(
                placement("P1", 0.0, 0.0, 60.0, 60.0),
                placement("P2", 60.0, 0.0, 40.0, 60.0)
            ),
            cutTree = cutTree,
            process = CuttingProcess2.GuillotineTwoStage
        )
        assertTrue(pattern.validate(zeroKerf()) is Failed)
    }

    @Test
    fun planDerivesContributionsFromPattern() {
        val pattern = Pattern2(
            id = patternId2Of("PAT-1"),
            material = sheet(),
            region = region(),
            placements = listOf(
                placement("P1", 0.0, 0.0, 60.0, 60.0),
                placement("P1", 60.0, 0.0, 40.0, 30.0)
            )
        )
        val products = mapOf(
            "P1" to Product2(
                id = productId2Of("P1"),
                name = "P1",
                width = meter(40.0),
                height = meter(30.0)
            )
        )
        val plan = cuttingPlan2Of(
            id = planId2Of("PLAN-1"),
            pattern = pattern,
            products = products
        ).valueOrFail()
        assertEquals(1, plan.contributions.size)
        assertEquals(UInt64(2), plan.contributions.first().amount)
        assertTrue(plan.validate(zeroKerf()) is Ok)
    }

    @Test
    fun planWithUnknownProductIsRejected() {
        val pattern = Pattern2(
            id = patternId2Of("PAT-1"),
            material = sheet(),
            region = region(),
            placements = listOf(placement("UNKNOWN", 0.0, 0.0, 60.0, 60.0))
        )
        assertTrue(cuttingPlan2Of(planId2Of("PLAN-1"), pattern, emptyMap()) is Failed)
    }

    @Test
    fun fitsInRespectsRotation() {
        val region = region2Of(x = meter(0.0), y = meter(0.0), width = meter(50.0), height = meter(80.0))
        // 40×60 直接放不下（60 > 50），旋转后可放入。 / 40 by 60 does not fit directly; rotation makes it fit.
        assertTrue(!fitsIn(region, meter(60.0), meter(40.0), allowRotate = false))
        assertTrue(fitsIn(region, meter(60.0), meter(40.0), allowRotate = true))
    }

    @Test
    fun remnantThresholdHelper() {
        assertTrue(remnantReaches(quantityMultiply(meter(2.0), meter(2.0)), null))
        assertTrue(!remnantReaches(quantityMultiply(meter(0.0), meter(0.0)), null))
        assertTrue(remnantReaches(quantityMultiply(meter(5.0), meter(5.0)), quantityMultiply(meter(3.0), meter(3.0))))
        assertTrue(!remnantReaches(quantityMultiply(meter(2.0), meter(2.0)), quantityMultiply(meter(3.0), meter(3.0))))
    }

    @Test
    fun patternWorksForFltXToo() {
        // 泛型门禁：同一套模型必须同时支持 Flt64 与 FltX。 / Genericity: the same model must support Flt64 and FltX.
        val material = Material2<FltX>(
            id = materialId2Of("SHEET"),
            name = "SHEET",
            width = FltX(100.0) * Meter,
            height = FltX(60.0) * Meter
        )
        val shape = rectangleShape2(
            width = FltX(50.0) * Meter,
            height = FltX(50.0) * Meter
        ).valueOrFail()
        val pattern = Pattern2(
            id = patternId2Of("PAT-X"),
            material = material,
            region = region2Of(
                x = FltX(0.0) * Meter,
                y = FltX(0.0) * Meter,
                width = FltX(100.0) * Meter,
                height = FltX(60.0) * Meter
            ),
            placements = listOf(
                Placement2(
                    shape = shape,
                    position = QuantityPoint2(x = FltX(0.0) * Meter, y = FltX(0.0) * Meter),
                    itemId = "P1"
                )
            )
        )
        assertTrue(pattern.validate(quantityZeroOf(FltX(1.0) * Meter)) is Ok)
    }
}

/**
 * 解包 Ret 结果。 / Unwrap a Ret result.
 *
 * @param T 结果类型 / result type
 * @param message 失败信息 / failure message
 * @return 成功值 / the successful value
*/
private fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> kotlin.test.fail("$message: ${error.message}")
        is Fatal -> kotlin.test.fail("$message: ${errors.joinToString { it.message }}")
    }
}
