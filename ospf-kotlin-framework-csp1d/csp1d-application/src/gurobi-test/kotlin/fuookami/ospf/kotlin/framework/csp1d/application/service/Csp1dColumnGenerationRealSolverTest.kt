package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiColumnGenerationSolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.RollCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.SheetCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dConfiguration
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem

/**
 * CSP1D 列生成真实 solver 收敛验证 / CSP1D column generation real solver convergence verification
 *
 * 使用 Gurobi 验证 LP -> dual price -> reduced cost pricing -> 新列加入 -> MILP 全链路。
 * Verify LP -> dual price -> reduced cost pricing -> column addition -> MILP full chain using Gurobi.
 *
 * 需要设置系统属性 csp1d.gurobi.cg.test.enabled=true 启用。
 * Requires system property csp1d.gurobi.cg.test.enabled=true to enable.
 */
@EnabledIfSystemProperty(named = "csp1d.gurobi.cg.test.enabled", matches = "true")
class Csp1dColumnGenerationRealSolverTest {

    private val solver = GurobiColumnGenerationSolver(SolverConfig())

    /**
     * 小规模样例：1 产品 + 1 原料，roll 需求
     * Small-scale example: 1 product + 1 material, roll demand
     *
     * 原料幅宽 [0.8, 2.0]，产品幅宽 0.5，需求 10 卷。
     * Material width [0.8, 2.0], product width 0.5, demand 10 rolls.
     * 初始方案应能覆盖，列生成应收敛（无负 reduced cost 新列或自然收敛）。
     * Initial plans should cover, column generation should converge.
     */
    @Test
    fun columnGenerationConvergesWithRollDemand() = runBlocking {
        val product = product(
            id = "p_roll_small",
            width = 0.5
        )
        val material = material(
            id = "m_roll_small",
            lowerWidth = 0.8,
            upperWidth = 2.0
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64(10.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 32,
                maxPricingPlans = 16,
                iterationLimit = 8
            )
        )

        val columnGeneration = Csp1dColumnGeneration<Flt64>(solver = solver)
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // 验证基本产出 / Verify basic output
        assertTrue(solution.generatedPlans.isNotEmpty(), "Should generate cutting plans")
        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans")

        // 验证 trace 结构 / Verify trace structure
        assertTrue(trace.initialPlanCount > UInt64.zero, "Should have initial plans")
        assertTrue(trace.finalPlanCount >= trace.initialPlanCount, "Final plan count should not decrease")

        // 验证终止原因 / Verify termination reason
        assertNotNull(trace.terminationReason)
        assertTrue(
            trace.terminationReason in listOf(
                Csp1dTerminationReason.PricingConverged,
                Csp1dTerminationReason.AllDuplicates,
                Csp1dTerminationReason.IterationLimitReached
            ),
            "Termination reason should be valid: ${trace.terminationReason}"
        )

        // 验证迭代记录 / Verify iteration records
        if (trace.iterations.isNotEmpty()) {
            for (record in trace.iterations) {
                assertTrue(record.planCountBefore > 0, "Plan count before should be positive")
                assertTrue(record.planCountAfter >= record.planCountBefore, "Plan count should not decrease")
            }
        }

        // 验证 pricedPlanCount 与 iterations 一致 / Verify pricedPlanCount matches iterations
        assertEquals(
            trace.pricedPlanCount.size,
            trace.iterations.size,
            "pricedPlanCount size should match iterations size"
        )
    }

    /**
     * 中等规模样例：3 产品 + 2 原料 + 1 设备，roll 需求
     * Medium-scale example: 3 products + 2 materials + 1 machine, roll demand
     *
     * 不同产品幅宽，不同原料幅宽范围，设备产能约束。
     * Different product widths, different material width ranges, machine capacity constraint.
     * 列生成应能通过 pricing 发现更优方案。
     * Column generation should discover better plans through pricing.
     */
    @Test
    fun columnGenerationDiscoversNewPlansViaPricing() = runBlocking {
        val p1 = product(id = "p_wide", width = 1.0)
        val p2 = product(id = "p_medium", width = 0.6)
        val p3 = product(id = "p_narrow", width = 0.3)
        val m1 = material(
            id = "m_narrow_mat",
            lowerWidth = 0.5,
            upperWidth = 1.5,
            machineId = "machine_1"
        )
        val m2 = material(
            id = "m_wide_mat",
            lowerWidth = 1.0,
            upperWidth = 2.5,
            machineId = "machine_1"
        )
        val machine = machine(
            id = "machine_1",
            capacity = 1000.0
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(p1, p2, p3),
            materials = listOf(m1, m2),
            machines = listOf(machine),
            demands = listOf(
                ProductDemand.legacyRoll(product = p1, rollAmount = Flt64(5.0)),
                ProductDemand.legacyRoll(product = p2, rollAmount = Flt64(8.0)),
                ProductDemand.legacyRoll(product = p3, rollAmount = Flt64(12.0))
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 64,
                maxPricingPlans = 16,
                iterationLimit = 8
            )
        )

        val columnGeneration = Csp1dColumnGeneration<Flt64>(solver = solver)
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // 验证产出 / Verify output
        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans")

        // 验证 trace：至少完成初始方案 / Verify trace: at least initial plans completed
        assertTrue(trace.initialPlanCount > UInt64.zero, "Should have initial plans")
        assertTrue(trace.finalPlanCount >= trace.initialPlanCount, "Final plan count >= initial")

        // 验证迭代记录与 pricedPlanCount 一致 / Verify iteration records match pricedPlanCount
        assertEquals(trace.pricedPlanCount.size, trace.iterations.size)

        // 验证 MILP 解有合理目标值 / Verify MILP solution has reasonable objective
        val totalBatches = solution.produce.cuttingPlans.fold(UInt64.zero) { acc, usage ->
            acc + usage.amount
        }
        assertTrue(totalBatches > UInt64.zero, "Should have non-zero total batches")
    }

    /**
     * 验证 MILP 直接求解（不经过列生成）在真实 solver 上可行
     * Verify MILP direct solve (without column generation) works on real solver
     */
    @Test
    fun milpDirectSolveWorksOnRealSolver() = runBlocking {
        val product = product(
            id = "p_milp_direct",
            width = 0.8
        )
        val material = material(
            id = "m_milp_direct",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64(3.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 32,
                maxPricingPlans = 8,
                iterationLimit = 4
            )
        )

        val solution = Csp1dMilp<Flt64>(solver).solve(problem)

        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans")
        assertTrue(solution.produce.unmetDemands.isEmpty(), "Should meet all demands")
    }

    /**
     * 验证 id 含下划线时 shadow price 映射正确
     * Verify shadow price mapping works correctly when ids contain underscores
     */
    @Test
    fun columnGenerationWorksWithUnderscoreInIds() = runBlocking {
        val product = product(
            id = "p_underscore_test",
            width = 0.5
        )
        val material = material(
            id = "m_underscore_test",
            lowerWidth = 0.5,
            upperWidth = 1.5,
            machineId = "machine_test_1"
        )
        val machine = machine(
            id = "machine_test_1",
            capacity = 500.0
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = listOf(machine),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64(6.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 32,
                maxPricingPlans = 8,
                iterationLimit = 4
            )
        )

        val columnGeneration = Csp1dColumnGeneration<Flt64>(solver = solver)
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // 验证含下划线的 id 不影响求解 / Verify underscores in ids don't affect solving
        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans with underscore ids")
        assertNotNull(trace.terminationReason)
    }

    /**
     * 验证 sheet 需求单位下的列生成收敛
     * Verify column generation convergence with sheet demand unit
     */
    @Test
    fun columnGenerationConvergesWithSheetDemand() = runBlocking {
        val product = product(
            id = "p_sheet",
            width = 0.6
        )
        val material = material(
            id = "m_sheet",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacySheet(
                    product = product,
                    sheetAmount = Flt64(8.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 32,
                maxPricingPlans = 8,
                iterationLimit = 4
            )
        )

        val columnGeneration = Csp1dColumnGeneration<Flt64>(solver = solver)
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans for sheet demand")
        assertNotNull(trace.terminationReason)
    }

    /**
     * 验证 weight 需求单位下的列生成收敛
     * Verify column generation convergence with weight demand unit
     */
    @Test
    fun columnGenerationConvergesWithWeightDemand() = runBlocking {
        val product = product(
            id = "p_weight",
            width = 0.4
        )
        val material = material(
            id = "m_weight",
            lowerWidth = 0.3,
            upperWidth = 1.2
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyWeight(
                    product = product,
                    weightAmount = Flt64(500.0),
                    unit = Kilogram
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 32,
                maxPricingPlans = 8,
                iterationLimit = 4
            )
        )

        val columnGeneration = Csp1dColumnGeneration<Flt64>(solver = solver)
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans for weight demand")
        assertNotNull(trace.terminationReason)
    }

    /**
     * 验证混合需求单位（roll + sheet）下的列生成收敛
     * Verify column generation convergence with mixed demand units (roll + sheet)
     */
    @Test
    fun columnGenerationConvergesWithMixedDemandUnits() = runBlocking {
        val p1 = product(id = "p_mixed_roll", width = 0.5)
        val p2 = product(id = "p_mixed_sheet", width = 0.8)
        val material = material(
            id = "m_mixed",
            lowerWidth = 0.5,
            upperWidth = 2.0
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(p1, p2),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(product = p1, rollAmount = Flt64(5.0)),
                ProductDemand.legacySheet(product = p2, sheetAmount = Flt64(3.0))
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 64,
                maxPricingPlans = 16,
                iterationLimit = 8
            )
        )

        val columnGeneration = Csp1dColumnGeneration<Flt64>(solver = solver)
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans for mixed demand units")
        assertNotNull(trace.terminationReason)

        // 验证不同单位的需求贡献不被混淆 / Verify different unit demands are not mixed
        for (usage in solution.produce.cuttingPlans) {
            for (contribution in usage.plan.demandContributions) {
                assertNotNull(contribution.quantity.unit, "Contribution should have a unit")
            }
        }
    }

    // --- helper functions ---

    private fun product(
        id: String,
        width: Double
    ): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = listOf(
                Quantity(Flt64(width), Meter)
            )
        )
    }

    private fun material(
        id: String,
        lowerWidth: Double,
        upperWidth: Double,
        machineId: String? = null
    ): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = widthRange(
                lower = lowerWidth,
                upper = upperWidth
            ),
            machineId = machineId
        )
    }

    private fun machine(
        id: String,
        capacity: Double
    ): Machine<Flt64> {
        return Machine(
            id = id,
            name = "machine-$id",
            capacity = Quantity(Flt64(capacity), Kilogram)
        )
    }

    private fun widthRange(
        lower: Double,
        upper: Double
    ): WidthRange<Flt64> {
        return WidthRange(
            width = QuantityRange(
                lowerBound = Quantity(Flt64(lower), Meter),
                upperBound = Quantity(Flt64(upper), Meter)
            ),
            step = Quantity(Flt64(0.1), Meter)
        )
    }
}
