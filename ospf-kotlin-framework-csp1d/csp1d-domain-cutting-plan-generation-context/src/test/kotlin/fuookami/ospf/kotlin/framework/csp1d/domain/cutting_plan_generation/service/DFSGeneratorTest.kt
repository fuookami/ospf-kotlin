package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.GenerationConstraints
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter

class DFSGeneratorTest {
    private val arithmetic = Flt64QuantityArithmetic

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
    fun twoProductCombination() {
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

        val generator = DFSGenerator(arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // Should have some multi-product plans
        val multiProductPlans = plans.filter { it.slices.size > 1 }
        assertTrue(multiProductPlans.isNotEmpty(), "Should have multi-product combination plans")
    }

    @Test
    fun knifeCountPruning() {
        val p1 = product("p1", listOf(Quantity(Flt64(0.3), Meter)))
        val p2 = product("p2", listOf(Quantity(Flt64(0.3), Meter)))
        val m = material()
        val constraints = GenerationConstraints<Flt64>(maxKnifeCount = UInt64(2UL))
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

        val generator = DFSGenerator(constraints = constraints, arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        for (plan in plans) {
            val totalAmount = plan.slices.sumOf { it.amount.toInt() }
            assertTrue(totalAmount <= 2, "Total amount should not exceed knife count")
        }
    }

    @Test
    fun widthConstraintPruning() {
        val p1 = product("p1", listOf(Quantity(Flt64(1.5), Meter)))
        val p2 = product("p2", listOf(Quantity(Flt64(1.5), Meter)))
        val m = material(upperBound = 2.0)
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

        val generator = DFSGenerator(arithmetic = arithmetic)
        val plans = generator.generate(input)

        // Each product alone fits (1.5 <= 2.0) but both together don't (3.0 > 2.0)
        // So we should only have single-product plans
        assertTrue(plans.isNotEmpty())
        for (plan in plans) {
            val totalWidth = plan.usedWidth
            assertTrue(
                totalWidth!!.value <= Flt64(2.0),
                "Total width should not exceed material upper bound"
            )
        }
    }

    @Test
    fun singleProductDegeneratesToNSame() {
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

        val generator = DFSGenerator(arithmetic = arithmetic)
        val plans = generator.generate(input)

        assertTrue(plans.isNotEmpty())
        // Should have single-product plans
        for (plan in plans) {
            assertTrue(plan.slices.all { it.production.id == "p1" })
        }
    }

    @Test
    fun duplicateSameUnitDemandShouldNotInflateWidthCombinationSpace() {
        val p = product("p1", listOf(Quantity(Flt64(0.5), Meter)))
        val m = material()
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))
        val singleInput = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = emptyList(),
            costars = emptyList(),
            demands = listOf(demand)
        )
        val duplicatedInput = singleInput.copy(
            demands = listOf(
                demand,
                ProductDemand.roll(p, Quantity(Flt64(7.0), RollCountUnit))
            )
        )

        val generator = DFSGenerator(arithmetic = arithmetic)
        val singleKeys = generator.generate(singleInput).map { it.canonicalKey() }.toSet()
        val duplicatedKeys = generator.generate(duplicatedInput).map { it.canonicalKey() }.toSet()

        assertEquals(singleKeys, duplicatedKeys)
    }
}
