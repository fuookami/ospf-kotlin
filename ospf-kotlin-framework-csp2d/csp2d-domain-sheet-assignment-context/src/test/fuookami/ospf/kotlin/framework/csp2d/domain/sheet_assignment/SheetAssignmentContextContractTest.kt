/**
 * 板长分配上下文契约测试。 / Sheet assignment context contract test.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.sheet_assignment

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

class SheetAssignmentContextContractTest {

    private fun meter(value: Double): Quantity<FltX> = FltX(value) * Meter

    private fun newModel(): LinearMetaModel<FltX> {
        return LinearMetaModel(
            name = "csp2d-sheet",
            objectCategory = ObjectCategory.Minimum,
            configuration = MetaModelConfiguration(),
            converter = IntoValue.fromConverter(FltX)
        )
    }

    private fun coil(): Material2<FltX> {
        return Material2(id = materialId2Of("COIL"), name = "COIL", width = meter(100.0))
    }

    private fun sheet(): Material2<FltX> {
        return Material2(
            id = materialId2Of("SHEET"),
            name = "SHEET",
            width = meter(100.0),
            height = meter(60.0)
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

    /**
     * 构造一个占用高度为 regionHeight 的图案。 / Build a pattern occupying the given region height.
     *
     * @param material 物料 / material
     * @param count 件数 / piece count
     * @param regionHeight 料域高度（卷材段高）/ region height (coil segment height)
     * @return 方案 / plan
    */
    private fun plan(material: Material2<FltX>, count: Int, regionHeight: Double): CuttingPlan2<FltX> {
        val shape = rectangleShape2(width = meter(40.0), height = meter(30.0)).valueOrFail()
        val pattern = Pattern2(
            id = patternId2Of("PAT-$count-$regionHeight"),
            material = material,
            region = region2Of(
                x = meter(0.0),
                y = meter(0.0),
                width = meter(100.0),
                height = meter(regionHeight)
            ),
            placements = (0 until count).map { index ->
                Placement2(
                    shape = shape,
                    position = QuantityPoint2(x = meter(40.0 * index), y = meter(0.0)),
                    itemId = "P1"
                )
            }
        )
        return cuttingPlan2Of(
            id = planId2Of("PLAN-$count-$regionHeight"),
            pattern = pattern,
            products = mapOf("P1" to product())
        ).valueOrFail()
    }

    @Test
    fun consumedLengthSumsSegmentHeightsForCoils() {
        val material = coil()
        val plans = listOf(plan(material, 2, 30.0), plan(material, 1, 30.0))
        val result = csp2dSheetAssignmentOf(
            plans = plans,
            usages = listOf(2, 1),
            config = Csp2dSheetAssignmentConfig()
        ).valueOrFail()
        // 2 次 × 30 + 1 次 × 30 = 90；板材张数 3。 / Two uses of 30 plus one use of 30 is 90, with three sheets.
        assertEquals(90.0, result.consumedLength.toDouble(), 1e-9)
        assertEquals(3, result.sheetCount)
        assertTrue(!result.overLengthDetected)
    }

    @Test
    fun fixedSheetContributesCountOnly() {
        val material = sheet()
        val plans = listOf(plan(material, 2, 60.0))
        val result = csp2dSheetAssignmentOf(
            plans = plans,
            usages = listOf(3),
            config = Csp2dSheetAssignmentConfig()
        ).valueOrFail()
        // 定尺板材不计入消耗长度，只计张数。 / A fixed sheet contributes count only, not consumed length.
        assertEquals(0.0, result.consumedLength.toDouble(), 1e-9)
        assertEquals(3, result.sheetCount)
    }

    @Test
    fun overLengthIsDetectedAgainstLimit() {
        val material = coil()
        val plans = listOf(plan(material, 2, 30.0))
        val result = csp2dSheetAssignmentOf(
            plans = plans,
            usages = listOf(4),
            config = Csp2dSheetAssignmentConfig(maxConsumedLength = FltX(100.0))
        ).valueOrFail()
        assertEquals(120.0, result.consumedLength.toDouble(), 1e-9)
        assertEquals(20.0, result.overLength.toDouble(), 1e-9)
        assertTrue(result.overLengthDetected)
    }

    @Test
    fun usageCountMismatchIsRejected() {
        val plans = listOf(plan(coil(), 1, 30.0))
        assertTrue(
            csp2dSheetAssignmentOf(
                plans = plans,
                usages = listOf(1, 2),
                config = Csp2dSheetAssignmentConfig()
            ) is Failed
        )
    }

    @Test
    fun consumedLengthConstraintAndObjectivesRegister() {
        val material = coil()
        val plans = listOf(plan(material, 2, 30.0))
        val assignment = Csp2dPatternAssignment(plans)
        val model = newModel()
        assignment.register(model = model, demandUpperBound = UInt64(10)).valueOrFail()

        assertTrue(
            Csp2dConsumedLengthConstraint(
                plans = plans,
                assignment = assignment,
                config = Csp2dSheetAssignmentConfig(maxConsumedLength = FltX(100.0))
            ).invoke(model) is Ok
        )
        assertTrue(
            Csp2dConsumedLengthObjective(plans = plans, assignment = assignment).invoke(model) is Ok
        )
        assertTrue(
            Csp2dSheetCountObjective(plans = plans, assignment = assignment).invoke(model) is Ok
        )
    }

    @Test
    fun modeDetection() {
        assertTrue(isCoilMode(coil()))
        assertTrue(!isCoilMode(sheet()))
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
