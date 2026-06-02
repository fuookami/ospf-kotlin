package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.GenerationConstraints
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter

class NSameGeneratorTest {
    private val arithmetic = Flt64QuantityArithmetic

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

        val generator = NSameGenerator(arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // Should have at least one plan with the single product
        val plan = plans.first()
        assertEquals(1, plan.slices.size)
        assertTrue(plan.slices[0].production.id == "p1")
    }

    @Test
    fun singleProductMultipleWidths() {
        val p = product("p1", listOf(
            Quantity(Flt64(0.5), Meter),
            Quantity(Flt64(1.0), Meter)
        ))
        val m = material()
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))
        val input = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = emptyList(),
            costars = emptyList(),
            demands = listOf(demand)
        )

        val generator = NSameGenerator(arithmetic = arithmetic)
        val plans = generator.generate(input)

        // Should have plans for each width
        assertTrue(plans.size >= 2)
    }

    @Test
    fun multipleProducts() {
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

        val generator = NSameGenerator(arithmetic = arithmetic)
        val plans = generator.generate(input)

        // Should have plans for both products
        assertTrue(plans.size >= 2)
        val productIds = plans.map { it.slices.first().production.id }.toSet()
        assertTrue(productIds.contains("p1"))
        assertTrue(productIds.contains("p2"))
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
        val generator = NSameGenerator(constraints = constraints, arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // Each plan should have at most 3 slices (amount per slice = 1, but knife count limits total)
        for (plan in plans) {
            val totalAmount = plan.slices.sumOf { it.amount }
            assertTrue(totalAmount <= UInt64(3UL))
        }
    }

    @Test
    fun productWidthExceedsMaterial() {
        val p = product("p1", listOf(Quantity(Flt64(3.0), Meter)))
        val m = material(upperBound = 2.0)
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))
        val input = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = emptyList(),
            costars = emptyList(),
            demands = listOf(demand)
        )

        val generator = NSameGenerator(arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isEmpty())
    }
}
