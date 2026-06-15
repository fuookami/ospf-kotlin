package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

class CostarFillerTest {
    private val arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.one)

    private fun product(id: String, width: Quantity<Flt64>): Product<Flt64> {
        return Product(id = id, name = "product-$id", width = listOf(width))
    }

    private fun material(): Material<Flt64> {
        return Material(
            id = "m1",
            name = "material-m1",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64(0.5), Meter),
                    upperBound = Quantity(Flt64(2.0), Meter)
                ),
                step = Quantity(Flt64(0.1), Meter)
            ),
            length = Quantity(Flt64(100.0), Meter)
        )
    }

    private fun cuttingPlan(slices: List<CuttingPlanSlice<Flt64>>): CuttingPlan<Flt64> {
        return CuttingPlan(
            id = "cp1",
            material = material(),
            slices = slices,
            demandContributions = emptyList(),
            arithmetic = arithmetic
        )
    }

    @Test
    fun noCostarsDoesNotModifyPlan() {
        val p = product("p1", Quantity(Flt64(1.0), Meter))
        val plan = cuttingPlan(listOf(
            CuttingPlanSlice(p, Quantity(Flt64(1.0), Meter), UInt64.one)
        ))

        val filler = CostarFiller(arithmetic)
        val results = filler.fill(plan, emptyList())

        assertEquals(1, results.size)
        assertEquals(1, results[0].slices.size)
    }

    @Test
    fun singleCostarFillsRemainingWidth() {
        val p = product("p1", Quantity(Flt64(1.0), Meter))
        val costar = Costar(
            id = "c1",
            name = "costar-1",
            width = listOf(Quantity(Flt64(0.5), Meter))
        )
        val plan = cuttingPlan(listOf(
            CuttingPlanSlice(p, Quantity(Flt64(1.0), Meter), UInt64.one)
        ))

        val filler = CostarFiller(arithmetic)
        val results = filler.fill(plan, listOf(costar))

        // Should have at least one plan with costar slice
        val filledPlans = results.filter { it.slices.size > 1 }
        assertTrue(filledPlans.isNotEmpty(), "Should have plans with costar slices")

        val filled = filledPlans.first()
        val costarSlices = filled.slices.filter { it.production.id == "c1" }
        assertTrue(costarSlices.isNotEmpty(), "Should have costar slice")
    }

    @Test
    fun multipleCostarsCombination() {
        val p = product("p1", Quantity(Flt64(1.0), Meter))
        val costar1 = Costar(
            id = "c1",
            name = "costar-1",
            width = listOf(Quantity(Flt64(0.3), Meter))
        )
        val costar2 = Costar(
            id = "c2",
            name = "costar-2",
            width = listOf(Quantity(Flt64(0.5), Meter))
        )
        val plan = cuttingPlan(listOf(
            CuttingPlanSlice(p, Quantity(Flt64(1.0), Meter), UInt64.one)
        ))

        val filler = CostarFiller(arithmetic)
        val results = filler.fill(plan, listOf(costar1, costar2))

        assertTrue(results.isNotEmpty())
        // At least one plan should have more slices than original
        val enriched = results.filter { it.slices.size > 1 }
        assertTrue(enriched.isNotEmpty(), "Should have plans with additional costar slices")
    }

    @Test
    fun costarSlicesShouldNotChangeDemandContributions() {
        val p = product("p-demand", Quantity(Flt64(1.0), Meter))
        val costar = Costar(
            id = "c-demand",
            name = "costar-demand",
            width = listOf(Quantity(Flt64(0.5), Meter))
        )
        val contribution = CuttingPlanDemandContribution(
            product = p,
            quantity = Quantity(Flt64.one, RollCountUnit)
        )
        val plan = CuttingPlan(
            id = "cp-demand",
            material = material(),
            slices = listOf(
                CuttingPlanSlice(
                    production = p,
                    width = Quantity(Flt64(1.0), Meter),
                    amount = UInt64.one
                )
            ),
            demandContributions = listOf(contribution),
            arithmetic = arithmetic
        )

        val results = CostarFiller(arithmetic).fill(plan, listOf(costar))

        assertTrue(results.any { it.slices.any { slice -> slice.production.id == costar.id } })
        assertTrue(results.all { it.demandContributions == listOf(contribution) })
    }
}
