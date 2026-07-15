package fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.Meter

/**
 * Wasting minimization context unit tests.
 * 浪费最小化上下文单元测试
 */
class WastingMinimizationContextTest {
    private val arithmetic: QuantityArithmetic<Flt64> = assertNotNull(DefaultQuantityArithmetic.resolveFor(Flt64.one).value)

    /** 执行浪费最小化分析，失败时抛出断言错误 / Execute waste minimization analysis, throw on failure */
    private fun WastingMinimizationContext<Flt64>.analyzeOrFail(
        selectedPlans: List<CuttingPlanUsage<Flt64>>
    ): WasteAnalysis<Flt64> {
        return analyze(selectedPlans).value ?: fail("waste analysis should succeed")
    }

    /**
     * Create a test material with default dimensions.
     * 创建默认尺寸的测试物料
     *
     * @return Test material instance / 测试物料实例
     */
    private fun material(): Material<Flt64> {
        return Material(
            id = MaterialIdImpl("m1"),
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

    /**
     * Create a test cutting plan with optional slices and rest width.
     * 创建带可选切片和余宽的测试切割方案
     *
     * @param id Cutting plan identifier / 切割方案标识
     * @param slices Slices in the plan / 方案中的切片
     * @param restWidth Rest width override / 余宽覆盖值
     * @return Test cutting plan instance / 测试切割方案实例
     */
    private fun cuttingPlan(
        id: String = "cp",
        slices: List<CuttingPlanSlice<Flt64>> = emptyList(),
        restWidth: Quantity<Flt64>? = null
    ): CuttingPlan<Flt64> {
        return CuttingPlan(
            id = cuttingPlanIdOf(id),
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
            id = cuttingPlanIdOf("cp1"),
            material = mat,
            slices = listOf(
                CuttingPlanSlice(
                    production = Product(productIdOf("p1"), "P1", listOf(Quantity(Flt64(1.0), Meter))),
                    width = Quantity(Flt64(1.3), Meter),
                    amount = UInt64.one
                )
            ),
            demandContributions = emptyList(),
            arithmetic = arithmetic
        )
        val p2 = CuttingPlan(
            id = cuttingPlanIdOf("cp2"),
            material = mat,
            slices = listOf(
                CuttingPlanSlice(
                    production = Product(productIdOf("p2"), "P2", listOf(Quantity(Flt64(1.0), Meter))),
                    width = Quantity(Flt64(1.5), Meter),
                    amount = UInt64.one
                )
            ),
            demandContributions = emptyList(),
            arithmetic = arithmetic
        )

        val ctx = WastingMinimizationContext(arithmetic)
        val analysis = ctx.analyzeOrFail(listOf(
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
            id = cuttingPlanIdOf("cp1"),
            material = mat,
            slices = listOf(
                CuttingPlanSlice(
                    production = Product(productIdOf("p1"), "P1", listOf(Quantity(Flt64(1.0), Meter))),
                    width = Quantity(Flt64(1.5), Meter),
                    amount = UInt64.one
                )
            ),
            demandContributions = emptyList(),
            arithmetic = arithmetic
        )

        val ctx = WastingMinimizationContext(arithmetic)
        val analysis = ctx.analyzeOrFail(listOf(CuttingPlanUsage(plan, UInt64.one)))

        assertNotNull(analysis.totalRestMaterial)
        // restWidth = 2.0 - 1.5 = 0.5 m, length = 100 m, restMaterial = 0.5 * 100 = 50 m^2
        assertEquals(1, analysis.restMaterialWastes.size)
    }

    @Test
    fun batchMultiplierAmplifiesWaste() {
        val mat = material()
        val plan = CuttingPlan(
            id = cuttingPlanIdOf("cp1"),
            material = mat,
            slices = listOf(
                CuttingPlanSlice(
                    production = Product(productIdOf("p1"), "P1", listOf(Quantity(Flt64(1.0), Meter))),
                    width = Quantity(Flt64(1.5), Meter),
                    amount = UInt64.one
                )
            ),
            demandContributions = emptyList(),
            arithmetic = arithmetic
        )

        val ctx = WastingMinimizationContext(arithmetic)
        val analysis = ctx.analyzeOrFail(listOf(CuttingPlanUsage(plan, UInt64(3UL))))

        assertNotNull(analysis.totalRestWidth)
        // restWidth = 0.5 m per plan, 3 batches → 1.5 m total
        assertTrue(analysis.totalRestWidth eq Quantity(Flt64(1.5), Meter))
    }
}
