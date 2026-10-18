/**
 * 良率上下文契约测试。 / Yield context contract test.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.yield

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

class YieldContextContractTest {

    private fun meter(value: Double): Quantity<FltX> = FltX(value) * Meter

    private fun newModel(): LinearMetaModel<FltX> {
        return LinearMetaModel(
            name = "csp2d-yield",
            objectCategory = ObjectCategory.Minimum,
            configuration = MetaModelConfiguration(),
            converter = IntoValue.fromConverter(FltX)
        )
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

    private fun plan(count: Int): CuttingPlan2<FltX> {
        val material = sheet()
        val shape = rectangleShape2(width = meter(40.0), height = meter(30.0)).valueOrFail()
        val pattern = Pattern2(
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
        return cuttingPlan2Of(
            id = planId2Of("PLAN-$count"),
            pattern = pattern,
            products = mapOf("P1" to product())
        ).valueOrFail()
    }

    private fun demand(count: UInt64): ProductDemand2<FltX> {
        return ProductDemand2(product = product(), demand = DemandValue2.Amount(count))
    }

    @Test
    fun overProductionBoundRegistersConstraint() {
        val plans = listOf(plan(2))
        val assignment = Csp2dPatternAssignment(plans)
        val model = newModel()
        assignment.register(model = model, demandUpperBound = UInt64(10)).valueOrFail()
        val result = Csp2dOverProductionBoundConstraint(
            plans = plans,
            assignment = assignment,
            demands = listOf(demand(UInt64(3))),
            config = Csp2dYieldConfig(overProductionUpperBounds = mapOf("P1" to FltX(2.0)))
        ).invoke(model)
        assertTrue(result is Ok)
    }

    @Test
    fun emptyConfigurationIsANoop() {
        val plans = listOf(plan(2))
        val assignment = Csp2dPatternAssignment(plans)
        val model = newModel()
        assignment.register(model = model, demandUpperBound = UInt64(10)).valueOrFail()
        val result = Csp2dOverProductionBoundConstraint(
            plans = plans,
            assignment = assignment,
            demands = listOf(demand(UInt64(3))),
            config = Csp2dYieldConfig()
        ).invoke(model)
        assertTrue(result is Ok)
    }

    @Test
    fun demandWithoutBoundIsSkipped() {
        val plans = listOf(plan(2))
        val assignment = Csp2dPatternAssignment(plans)
        val model = newModel()
        assignment.register(model = model, demandUpperBound = UInt64(10)).valueOrFail()
        val result = Csp2dOverProductionBoundConstraint(
            plans = plans,
            assignment = assignment,
            demands = listOf(demand(UInt64(3))),
            config = Csp2dYieldConfig(overProductionUpperBounds = mapOf("OTHER" to FltX(1.0)))
        ).invoke(model)
        assertTrue(result is Ok)
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
