package fuookami.ospf.kotlin.framework.csp1d.domain.yield

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.*

class YieldContextTest {
    private val arithmetic: QuantityArithmetic<Flt64> = assertNotNull(DefaultQuantityArithmetic.resolveFor(Flt64.one).value)

    private fun YieldContext<Flt64>.analyzeOrFail(
        produce: Produce<Flt64>,
        demands: List<ProductDemand<Flt64>>
    ): YieldAnalysis<Flt64> {
        return analyze(produce, demands).value ?: fail("yield analysis should succeed")
    }

    private fun product(id: String = "p"): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = listOf(Quantity(Flt64.one, Meter))
        )
    }

    private fun material(id: String = "m"): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
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
        contributions: List<CuttingPlanDemandContribution<Flt64>>
    ): CuttingPlan<Flt64> {
        return CuttingPlan(
            id = id,
            material = material(),
            slices = emptyList(),
            demandContributions = contributions,
            arithmetic = arithmetic
        )
    }

    @Test
    fun underProductionWhenOutputLessThanDemand() {
        val p = product("p1")
        val plan = cuttingPlan("cp1", listOf(
            CuttingPlanDemandContribution(p, Quantity(Flt64(8.0), Kilogram))
        ))
        val produce = Produce<Flt64>(
            cuttingPlans = listOf(CuttingPlanUsage(plan, UInt64.one)),
            materialUsages = emptyList(),
            machineUsages = emptyList()
        )
        val demands = listOf(
            ProductDemand.weight(p, Quantity(Flt64(10.0), Kilogram))
        )

        val ctx = YieldContext(arithmetic)
        val analysis = ctx.analyzeOrFail(produce, demands)

        assertEquals(1, analysis.underProductions.size)
        assertEquals(0, analysis.overProductions.size)
        assertTrue(analysis.underProductions[0].shortfall eq Quantity(Flt64(2.0), Kilogram))
    }

    @Test
    fun overProductionWhenOutputExceedsDemand() {
        val p = product("p1")
        val plan = cuttingPlan("cp1", listOf(
            CuttingPlanDemandContribution(p, Quantity(Flt64(12.0), Kilogram))
        ))
        val produce = Produce<Flt64>(
            cuttingPlans = listOf(CuttingPlanUsage(plan, UInt64.one)),
            materialUsages = emptyList(),
            machineUsages = emptyList()
        )
        val demands = listOf(
            ProductDemand.weight(p, Quantity(Flt64(10.0), Kilogram))
        )

        val ctx = YieldContext(arithmetic)
        val analysis = ctx.analyzeOrFail(produce, demands)

        assertEquals(0, analysis.underProductions.size)
        assertEquals(1, analysis.overProductions.size)
        assertTrue(analysis.overProductions[0].surplus eq Quantity(Flt64(2.0), Kilogram))
    }

    @Test
    fun sameUnitContributionsAreAggregated() {
        val p = product("p1")
        val plan1 = cuttingPlan("cp1", listOf(
            CuttingPlanDemandContribution(p, Quantity(Flt64(3.0), Kilogram))
        ))
        val plan2 = cuttingPlan("cp2", listOf(
            CuttingPlanDemandContribution(p, Quantity(Flt64(4.0), Kilogram))
        ))
        val produce = Produce<Flt64>(
            cuttingPlans = listOf(
                CuttingPlanUsage(plan1, UInt64.one),
                CuttingPlanUsage(plan2, UInt64.one)
            ),
            materialUsages = emptyList(),
            machineUsages = emptyList()
        )
        val demands = listOf(
            ProductDemand.weight(p, Quantity(Flt64(10.0), Kilogram))
        )

        val ctx = YieldContext(arithmetic)
        val analysis = ctx.analyzeOrFail(produce, demands)

        assertEquals(1, analysis.outputs.size)
        assertTrue(analysis.outputs[0].totalQuantity eq Quantity(Flt64(7.0), Kilogram))
        assertEquals(1, analysis.underProductions.size)
        assertTrue(analysis.underProductions[0].shortfall eq Quantity(Flt64(3.0), Kilogram))
    }

    @Test
    fun differentUnitsAreNotMixed() {
        val p = product("p1")
        val plan = cuttingPlan("cp1", listOf(
            CuttingPlanDemandContribution(p, Quantity(Flt64(8.0), Kilogram))
        ))
        val produce = Produce<Flt64>(
            cuttingPlans = listOf(CuttingPlanUsage(plan, UInt64.one)),
            materialUsages = emptyList(),
            machineUsages = emptyList()
        )

        // 同一产品不同单位需求应分别分析 / Same-product demands with different units are analyzed separately
        val rollRequirement = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))
        val weightRequirement = ProductDemand.weight(p, Quantity(Flt64(10.0), Kilogram))

        val ctx = YieldContext(arithmetic)
        val analysis = ctx.analyzeOrFail(produce, listOf(rollRequirement, weightRequirement))

        // 卷数单位没有贡献，应全量欠产 / Roll-count unit has no contribution, so it is fully under-produced
        assertEquals(2, analysis.underProductions.size)
        val rollUnder = analysis.underProductions.first { it.demand.mode == DemandMode.Roll }
        assertTrue(rollUnder.shortfall eq Quantity(Flt64(5.0), RollCountUnit))

        // 重量产出 8 kg 小于需求 10 kg，欠产 2 kg / Weight output 8 kg is below 10 kg demand
        val weightUnder = analysis.underProductions.first { it.demand.mode == DemandMode.Weight }
        assertTrue(weightUnder.shortfall eq Quantity(Flt64(2.0), Kilogram))
    }

    @Test
    fun noContributionForUnitMeansFullUnderProduction() {
        val p = product("p1")
        val plan = cuttingPlan("cp1", listOf(
            CuttingPlanDemandContribution(p, Quantity(Flt64(5.0), Kilogram))
        ))
        val produce = Produce<Flt64>(
            cuttingPlans = listOf(CuttingPlanUsage(plan, UInt64.one)),
            materialUsages = emptyList(),
            machineUsages = emptyList()
        )
        val sheetRequirement = ProductDemand.sheet(p, Quantity(Flt64(100.0), SheetCountUnit))

        val ctx = YieldContext(arithmetic)
        val analysis = ctx.analyzeOrFail(produce, listOf(sheetRequirement))

        assertEquals(1, analysis.underProductions.size)
        assertTrue(analysis.underProductions[0].shortfall eq Quantity(Flt64(100.0), SheetCountUnit))
    }

    @Test
    fun demandAggregationKeyDistinguishesByUnit() {
        val key1 = DemandAggregationKey<Flt64>("p1", Kilogram)
        val key2 = DemandAggregationKey<Flt64>("p1", RollCountUnit)
        val key3 = DemandAggregationKey<Flt64>("p1", Kilogram)

        assertTrue(key1 != key2)
        assertTrue(key1 == key3)
    }
}
