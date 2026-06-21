package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

class FullSumGeneratorTest {
    private val arithmetic = (DefaultQuantityArithmetic.resolveFor(Flt64.one) as Ok).value

    private fun product(id: String, widths: List<Quantity<Flt64>>): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = widths
        )
    }

    private fun material(id: String = "m", upperBound: Double = 2.0, lowerBound: Double = 0.5): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64(lowerBound), Meter),
                    upperBound = Quantity(Flt64(upperBound), Meter)
                ),
                step = Quantity(Flt64(0.1), Meter)
            ),
            length = Quantity(Flt64(100.0), Meter)
        )
    }

    @Test
    fun singleProductSingleWidth() {
        val p = product("p1", listOf(Quantity(Flt64(0.5), Meter)))
        val m = material()
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))
        val input = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = emptyList(),
            costars = emptyList(),
            demands = listOf(demand)
        )

        val generator = FullSumGenerator(arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // Should have plans with different amounts: 0.5*1=0.5, 0.5*2=1.0, 0.5*3=1.5, 0.5*4=2.0
        assertTrue(plans.size >= 2, "Should have multiple plans with different amounts")
    }

    @Test
    fun twoProductCombination() {
        val p1 = product("p1", listOf(Quantity(Flt64(0.3), Meter)))
        val p2 = product("p2", listOf(Quantity(Flt64(0.5), Meter)))
        val m = material(upperBound = 1.5)
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

        val generator = FullSumGenerator(arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // Should have multi-product plans (p1+p2 combinations)
        val multiProductPlans = plans.filter { it.slices.size > 1 }
        assertTrue(multiProductPlans.isNotEmpty(), "Should have multi-product combination plans")
    }

    @Test
    fun knifeCountConstraint() {
        val p = product("p1", listOf(Quantity(Flt64(0.3), Meter)))
        val m = material()
        val demand = ProductDemand.roll(p, Quantity(Flt64(10.0), RollCountUnit))
        val input = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = emptyList(),
            costars = emptyList(),
            demands = listOf(demand)
        )

        val constraints = GenerationConstraints<Flt64>(maxKnifeCount = UInt64(3UL))
        val generator = FullSumGenerator(constraints = constraints, arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // Each plan should have at most 3 slices (amount per slice = 1, but knife count limits total)
        for (plan in plans) {
            val totalAmount = plan.slices.fold(0) { total, slice -> total + slice.amount.toInt() }
            assertTrue(totalAmount <= 3, "Plan total amount $totalAmount should be <= 3")
        }
    }

    @Test
    fun minKnifeCountConstraint() {
        val p1 = product("p1", listOf(Quantity(Flt64(0.3), Meter)))
        val p2 = product("p2", listOf(Quantity(Flt64(0.5), Meter)))
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

        val constraints = GenerationConstraints<Flt64>(minKnifeCount = UInt64(2UL))
        val generator = FullSumGenerator(constraints = constraints, arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // All plans should have at least 2 slices
        for (plan in plans) {
            val totalAmount = plan.slices.fold(0) { total, slice -> total + slice.amount.toInt() }
            assertTrue(totalAmount >= 2, "Plan total amount $totalAmount should be >= 2 (minKnifeCount)")
        }
    }

    @Test
    fun widthUpperBoundNotExceeded() {
        val p1 = product("p1", listOf(Quantity(Flt64(0.3), Meter)))
        val p2 = product("p2", listOf(Quantity(Flt64(0.5), Meter)))
        val m = material(upperBound = 1.0)
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

        val generator = FullSumGenerator(arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // All plans should have usedWidth <= 1.0 (material upper bound)
        for (plan in plans) {
            val usedWidth = plan.usedWidth
            assertNotNull(usedWidth)
            assertTrue(
                usedWidth!!.value <= Flt64(1.0) + Flt64(1e-6),
                "Used width ${usedWidth.value} should not exceed material upper bound 1.0"
            )
        }
    }
}
