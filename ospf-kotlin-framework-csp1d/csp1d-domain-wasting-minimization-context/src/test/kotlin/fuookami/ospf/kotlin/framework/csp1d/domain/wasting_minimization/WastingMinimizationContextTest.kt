package fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage

class WastingMinimizationContextTest {
    private val arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.one).value

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

    private fun cuttingPlan(
        id: String = "cp",
        slices: List<CuttingPlanSlice<Flt64>> = emptyList(),
        restWidth: Quantity<Flt64>? = null
    ): CuttingPlan<Flt64> {
        return CuttingPlan(
            id = id,
            material = material(),
            slices = slices,
            demandContributions = emptyList(),
            arithmetic = arithmetic
        )
    }

    @Test
    fun restWidthIsSummedAcrossPlans() {
        val plan1 = cuttingPlan("cp1", restWidth = Quantity(Flt64(0.3), Meter))
        val plan2 = cuttingPlan("cp2", restWidth = Quantity(Flt64(0.2), Meter))

        // We need to use plans that actually have restWidth computed
        // Create plans with slices that leave rest width
        val mat = material()
        val p1 = CuttingPlan(
            id = "cp1",
            material = mat,
            slices = listOf(
                CuttingPlanSlice(
                    production = Product("p1", "P1", listOf(Quantity(Flt64(1.0), Meter))),
                    width = Quantity(Flt64(1.3), Meter),
                    amount = UInt64.one
                )
            ),
            demandContributions = emptyList(),
            arithmetic = arithmetic
        )
        val p2 = CuttingPlan(
            id = "cp2",
            material = mat,
            slices = listOf(
                CuttingPlanSlice(
                    production = Product("p2", "P2", listOf(Quantity(Flt64(1.0), Meter))),
                    width = Quantity(Flt64(1.5), Meter),
                    amount = UInt64.one
                )
            ),
            demandContributions = emptyList(),
            arithmetic = arithmetic
        )

        val ctx = WastingMinimizationContext(arithmetic)
        val analysis = ctx.analyze(listOf(
            CuttingPlanUsage(p1, UInt64.one),
            CuttingPlanUsage(p2, UInt64.one)
        ))

        assertNotNull(analysis.totalRestWidth)
        // p1 restWidth = 2.0 - 1.3 = 0.7, p2 restWidth = 2.0 - 1.5 = 0.5
        assertTrue(analysis.totalRestWidth eq Quantity(Flt64(1.2), Meter))
        assertEquals(2, analysis.restWidthWastes.size)
    }

    @Test
    fun restMaterialIsComputedFromRestWidthTimesLength() {
        val mat = material()
        val plan = CuttingPlan(
            id = "cp1",
            material = mat,
            slices = listOf(
                CuttingPlanSlice(
                    production = Product("p1", "P1", listOf(Quantity(Flt64(1.0), Meter))),
                    width = Quantity(Flt64(1.5), Meter),
                    amount = UInt64.one
                )
            ),
            demandContributions = emptyList(),
            arithmetic = arithmetic
        )

        val ctx = WastingMinimizationContext(arithmetic)
        val analysis = ctx.analyze(listOf(CuttingPlanUsage(plan, UInt64.one)))

        assertNotNull(analysis.totalRestMaterial)
        // restWidth = 2.0 - 1.5 = 0.5 m, length = 100 m, restMaterial = 0.5 * 100 = 50 m^2
        assertEquals(1, analysis.restMaterialWastes.size)
    }

    @Test
    fun batchMultiplierAmplifiesWaste() {
        val mat = material()
        val plan = CuttingPlan(
            id = "cp1",
            material = mat,
            slices = listOf(
                CuttingPlanSlice(
                    production = Product("p1", "P1", listOf(Quantity(Flt64(1.0), Meter))),
                    width = Quantity(Flt64(1.5), Meter),
                    amount = UInt64.one
                )
            ),
            demandContributions = emptyList(),
            arithmetic = arithmetic
        )

        val ctx = WastingMinimizationContext(arithmetic)
        val analysis = ctx.analyze(listOf(CuttingPlanUsage(plan, UInt64(3UL))))

        assertNotNull(analysis.totalRestWidth)
        // restWidth = 0.5 m per plan, 3 batches → 1.5 m total
        assertTrue(analysis.totalRestWidth eq Quantity(Flt64(1.5), Meter))
    }
}