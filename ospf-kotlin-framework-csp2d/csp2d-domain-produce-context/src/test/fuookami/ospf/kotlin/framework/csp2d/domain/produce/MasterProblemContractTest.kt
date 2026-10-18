/**
 * 主问题建模契约测试。 / Master problem modelling contract test.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.produce

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
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.region2Of

class MasterProblemContractTest {

    /**
     * 构造米制量值。 / Build a metric quantity.
     *
     * @param value 数值 / value
     * @return 量值 / quantity
    */
    private fun meter(value: Double): Quantity<FltX> = FltX(value) * Meter

    /**
     * 构造线性元模型（无需求解器）。 / Build a linear meta model without a solver.
     *
     * @return 元模型 / meta model
    */
    private fun newModel(): LinearMetaModel<FltX> {
        return LinearMetaModel(
            name = "csp2d-test",
            objectCategory = ObjectCategory.Minimum,
            configuration = MetaModelConfiguration(),
            converter = IntoValue.fromConverter(FltX)
        )
    }

    private fun sheet(batches: UInt64 = UInt64.maximum): Material2<FltX> {
        return Material2(
            id = materialId2Of("SHEET"),
            name = "SHEET",
            width = meter(100.0),
            height = meter(60.0),
            availableBatches = batches
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
     * 构造一个图案：一行 count 个 40×30 的件。 / Build a pattern holding one row of count 40 by 30 pieces.
     *
     * @param material 物料 / material
     * @param count 件数 / piece count
     * @param process 工艺 / process
     * @return 图案 / pattern
    */
    private fun pattern(
        material: Material2<FltX>,
        count: Int,
        process: CuttingProcess2 = CuttingProcess2.Freeform
    ): Pattern2<FltX> {
        val shape = rectangleShape2(width = meter(40.0), height = meter(30.0)).valueOrFail()
        val placements = (0 until count).map { index ->
            Placement2(
                shape = shape,
                position = QuantityPoint2(x = meter(40.0 * index), y = meter(0.0)),
                itemId = "P1"
            )
        }
        return Pattern2(
            id = patternId2Of("PAT-$count"),
            material = material,
            region = region2Of(
                x = meter(0.0),
                y = meter(0.0),
                width = meter(100.0),
                height = material.height ?: meter(30.0)
            ),
            placements = placements,
            process = process
        )
    }

    private fun plan(material: Material2<FltX>, count: Int): CuttingPlan2<FltX> {
        val built = cuttingPlan2Of(
            id = planId2Of("PLAN-$count"),
            pattern = pattern(material, count),
            products = mapOf("P1" to product())
        ).valueOrFail()
        return built
    }

    private fun demand(count: UInt64): ProductDemand2<FltX> {
        return ProductDemand2(
            product = product(),
            demand = DemandValue2.Amount(count)
        )
    }

    @Test
    fun masterProblemRegistersEveryPipeline() {
        val material = sheet()
        val plans = listOf(plan(material, 1), plan(material, 2))
        val assignment = Csp2dPatternAssignment(plans)
        val model = newModel()

        assertTrue(assignment.register(model = model, demandUpperBound = UInt64(10)) is Ok)
        val demandRegistered = Csp2dDemandConstraint(
            plans = plans,
            assignment = assignment,
            demands = listOf(demand(UInt64(3)))
        ).invoke(model)
        val materialRegistered = Csp2dMaterialAvailabilityConstraint(
            plans = plans,
            assignment = assignment,
            materials = listOf(material)
        ).invoke(model)
        val capacityRegistered = Csp2dMachineCapacityConstraint(
            plans = plans,
            assignment = assignment,
            machines = emptyList()
        ).invoke(model)
        val objectiveRegistered = Csp2dUsageObjective(
            plans = plans,
            assignment = assignment,
            coefficient = { FltX.one }
        ).invoke(model)

        assertTrue(demandRegistered is Ok)
        assertTrue(materialRegistered is Ok)
        assertTrue(capacityRegistered is Ok)
        assertTrue(objectiveRegistered is Ok)
    }

    @Test
    fun emptyPlanSetIsRejected() {
        val assignment = Csp2dPatternAssignment(emptyList())
        assertTrue(assignment.register(model = newModel(), demandUpperBound = UInt64(1)) is Failed)
    }

    @Test
    fun unlimitedMaterialGetsNoAvailabilityConstraint() {
        // 不限量物料不注册约束；这里只验证不会失败（约束注册是幂等的空操作）。
        // Unlimited material gets no constraint; this only verifies that it does not fail (the registration is a no-op).
        val material = sheet(batches = UInt64.maximum)
        val plans = listOf(plan(material, 1))
        val assignment = Csp2dPatternAssignment(plans)
        val model = newModel()
        assignment.register(model = model, demandUpperBound = UInt64(5)).valueOrFail()
        val result = Csp2dMaterialAvailabilityConstraint(
            plans = plans,
            assignment = assignment,
            materials = listOf(material)
        ).invoke(model)
        assertTrue(result is Ok)
    }

    @Test
    fun machineWithoutCapacityIsSkipped() {
        val material = sheet()
        val plans = listOf(plan(material, 1))
        val assignment = Csp2dPatternAssignment(plans)
        val model = newModel()
        assignment.register(model = model, demandUpperBound = UInt64(5)).valueOrFail()
        val machine = Machine2<FltX>(id = machineId2Of("M1"), name = "M1")
        val result = Csp2dMachineCapacityConstraint(
            plans = plans,
            assignment = assignment,
            machines = listOf(machine)
        ).invoke(model)
        assertTrue(result is Ok)
    }

    @Test
    fun planCoverageComesFromPatternPieceCounts() {
        val material = sheet()
        val twoPiece = plan(material, 2)
        assertEquals(1, twoPiece.contributions.size)
        assertEquals(UInt64(2), twoPiece.contributions.first().amount)
        // 图案占用 2 × 1200 = 2400，料域 6000，余料 3600。 / The pattern occupies 2400 out of 6000, so the waste is 3600.
        assertEquals(2400.0, twoPiece.usedArea.value.toDouble(), 1e-9)
        assertEquals(3600.0, twoPiece.wasteArea.value.toDouble(), 1e-9)
    }

    @Test
    fun safePolynomialHandlesEmptyMonomials() {
        // 空单项式列表必须能构造出常数零，否则裁剪后的目标会崩。
        // An empty monomial list must build the zero constant, otherwise a trimmed objective would crash.
        val polynomial = safeCsp2dPolynomial(emptyList())
        assertEquals(FltX.zero, polynomial.constant)
        assertTrue(polynomial.monomials.isEmpty())
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
