/**
 * 余料上下文契约测试。 / Waste context contract test.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.wasting_minimization

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelConfiguration
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp2d.domain.produce.Csp2dPatternAssignment
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.region2Of

class WasteContextContractTest {

    private fun meter(value: Double): Quantity<FltX> = FltX(value) * Meter

    private fun newModel(): LinearMetaModel<FltX> {
        return LinearMetaModel(
            name = "csp2d-waste",
            objectCategory = ObjectCategory.Minimum,
            configuration = MetaModelConfiguration(),
            converter = IntoValue.fromConverter(FltX)
        )
    }

    /**
     * 构造带余料阈值的物料。 / Build a material with a remnant threshold.
     *
     * @param thresholdArea 余料阈值面积，为空表示无阈值 / remnant threshold area, null meaning no threshold
     * @return 物料 / material
    */
    private fun sheet(thresholdArea: Double? = null): Material2<FltX> {
        return Material2(
            id = materialId2Of("SHEET"),
            name = "SHEET",
            width = meter(100.0),
            height = meter(60.0),
            minRemnantArea = thresholdArea?.let { quantityMultiply(meter(it), meter(it)) }
        )
    }

    private fun product(): Product2<FltX> {
        return Product2(
            id = productId2Of("P1"),
            name = "P1",
            width = meter(40.0),
            height = meter(30.0)
        )
    }

    private fun pattern(material: Material2<FltX>, count: Int): Pattern2<FltX> {
        val shape = rectangleShape2(width = meter(40.0), height = meter(30.0)).valueOrFail()
        return Pattern2(
            id = patternId2Of("PAT-$count"),
            material = material,
            region = region2Of(
                x = meter(0.0),
                y = meter(0.0),
                width = meter(100.0),
                height = meter(60.0)
            ),
            placements = (0 until count).map { index ->
                Placement2(
                    shape = shape,
                    position = QuantityPoint2(x = meter(40.0 * index), y = meter(0.0)),
                    itemId = "P1"
                )
            }
        )
    }

    private fun plan(material: Material2<FltX>, count: Int): CuttingPlan2<FltX> {
        return cuttingPlan2Of(
            id = planId2Of("PLAN-$count"),
            pattern = pattern(material, count),
            products = mapOf("P1" to product())
        ).valueOrFail()
    }

    @Test
    fun wasteObjectiveRegisters() {
        val material = sheet()
        val plans = listOf(plan(material, 1), plan(material, 2))
        val assignment = Csp2dPatternAssignment(plans)
        val model = newModel()
        assignment.register(model = model, demandUpperBound = UInt64(10)).valueOrFail()
        assertTrue(Csp2dWasteMinimization(plans = plans, assignment = assignment).invoke(model) is Ok)
    }

    @Test
    fun summarizerMarksRemnantUsabilityByThreshold() {
        // 2 件占 2400，料域 6000 → 余料 3600；阈值 50×50 = 2500 → 可用。
        // Two pieces occupy 2400 of 6000, so the remnant is 3600; a threshold of 50 by 50 is 2500, so it is usable.
        val material = sheet(thresholdArea = 50.0)
        val patterns = listOf(pattern(material, 2))
        val summaries = Csp2dRemnantSummarizer(listOf(material)).summarize(patterns)
        assertEquals(1, summaries.size)
        assertEquals(3600.0, summaries.first().wasteArea.value.toDouble(), 1e-9)
        assertTrue(summaries.first().usable)

        // 阈值 100×100 = 10000 > 3600 → 不可用。 / A threshold of 100 by 100 is 10000, above 3600, so it is not usable.
        val strict = sheet(thresholdArea = 100.0)
        val strictSummaries = Csp2dRemnantSummarizer(listOf(strict)).summarize(listOf(pattern(strict, 2)))
        assertTrue(!strictSummaries.first().usable)
    }

    @Test
    fun reusableAreaSumsOnlyUsableRemnants() {
        val material = sheet(thresholdArea = 50.0)
        val patterns = listOf(pattern(material, 2), pattern(material, 5))
        val summarizer = Csp2dRemnantSummarizer(listOf(material))
        val summaries = summarizer.summarize(patterns)
        // 2 件余料 3600 可用；5 件占 6000，余料 0 不可用。 / 3600 is usable, while five pieces leave no remnant.
        val sample = quantityMultiply(meter(1.0), meter(1.0))
        val total = summarizer.reusableArea(summaries, sample)
        assertEquals(3600.0, total.value.toDouble(), 1e-9)
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
