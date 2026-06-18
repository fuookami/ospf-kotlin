package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.GenerationConstraints
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

class NSumGeneratorTest {
    private val arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.one).value

    private fun product(id: String, widths: List<Quantity<Flt64>>): Product<Flt64> {
        return Product(id = id, name = "product-$id", width = widths)
    }

    private fun material(id: String = "m", upperBound: Double = 2.0): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64(0.5), Meter),
                    upperBound = Quantity(Flt64(upperBound), Meter)
                ),
                step = Quantity(Flt64(0.1), Meter)
            ),
            length = Quantity(Flt64(100.0), Meter)
        )
    }

    @Test
    fun depthLimitIsRespected() {
        val p1 = product("p1", listOf(Quantity(Flt64(0.3), Meter)))
        val p2 = product("p2", listOf(Quantity(Flt64(0.3), Meter)))
        val m = material()
        val input = CuttingPlanGenerationInput(
            products = listOf(p1, p2),
            materials = listOf(m),
            machines = emptyList(),
            costars = emptyList(),
            demands = listOf(
                ProductDemand.roll(p1, Quantity(Flt64(5.0), RollCountUnit)),
                ProductDemand.roll(p2, Quantity(Flt64(5.0), RollCountUnit))
            )
        )

        val generator = NSumGenerator(arithmetic = arithmetic, maxDepth = UInt64(2UL))
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        for (plan in plans) {
            val totalAmount = plan.slices.sumOf { it.amount.toInt() }
            assertTrue(totalAmount <= 2, "Total slices should not exceed maxDepth")
        }
    }

    @Test
    fun twoProductDepth2() {
        val p1 = product("p1", listOf(Quantity(Flt64(0.5), Meter)))
        val p2 = product("p2", listOf(Quantity(Flt64(0.8), Meter)))
        val m = material()
        val input = CuttingPlanGenerationInput(
            products = listOf(p1, p2),
            materials = listOf(m),
            machines = emptyList(),
            costars = emptyList(),
            demands = listOf(
                ProductDemand.roll(p1, Quantity(Flt64(5.0), RollCountUnit)),
                ProductDemand.roll(p2, Quantity(Flt64(3.0), RollCountUnit))
            )
        )

        val generator = NSumGenerator(arithmetic = arithmetic, maxDepth = UInt64(2UL))
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // Should have both single-product and multi-product plans within depth
        val multiProductPlans = plans.filter { it.slices.size > 1 }
        assertTrue(multiProductPlans.isNotEmpty(), "Should have multi-product plans")
    }

    @Test
    fun depth1DegeneratesToNSame() {
        val p = product("p1", listOf(Quantity(Flt64(0.5), Meter)))
        val m = material()
        val input = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = emptyList(),
            costars = emptyList(),
            demands = listOf(
                ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))
            )
        )

        val generator = NSumGenerator(arithmetic = arithmetic, maxDepth = UInt64.one)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // With depth 1, only single-slice plans (like NSame)
        for (plan in plans) {
            assertEquals(1, plan.slices.size)
        }
    }
}
