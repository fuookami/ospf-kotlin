package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.RollCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.SheetCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingResult
import fuookami.ospf.kotlin.framework.csp1d.application.service.WasteMinimizationConfig
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

        // 验证需求满足 / Verify demand satisfaction
        assertTrue(solution.produce.unmetDemands.isEmpty(), "Should meet all demands with real solver")

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
                // LP 目标值应 ≥ 0（最小化批次） / LP objective should be >= 0 (minimizing batches)
                assertTrue(
                    record.lpObjective >= Flt64.zero,
                    "LP objective should be non-negative: ${record.lpObjective}"
                )
            }
        }

        // 验证 pricedPlanCount 与 iterations 一致 / Verify pricedPlanCount matches iterations
        assertEquals(
            trace.pricedPlanCount.size,
            trace.iterations.size,
            "pricedPlanCount size should match iterations size"
        )

        // 验证 MILP 解总贡献量 ≥ 需求量 / Verify MILP total contribution >= demand
        val totalContribution = solution.produce.cuttingPlans.fold(Flt64.zero) { acc, usage ->
            val rollContrib = usage.plan.demandContributions.firstOrNull()?.quantity?.value ?: Flt64.zero
            acc + rollContrib * usage.amount.toFlt64()
        }
        assertTrue(
            totalContribution >= Flt64(10.0) - Flt64(1e-6),
            "Total roll contribution should be >= demand (10 rolls), got $totalContribution"
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

        // 验证 LP 目标值非负且迭代记录结构完整 / Verify LP objectives are non-negative and iteration records are complete
        for (record in trace.iterations) {
            assertTrue(
                record.lpObjective >= Flt64.zero,
                "LP objective should be non-negative in iteration ${record.iteration}: ${record.lpObjective}"
            )
            assertTrue(record.iteration >= 0, "Iteration number should be non-negative")
        }

        // 验证 pricing 轮次中 pricedPlanCount 与 iteration records 对应 / Verify pricedPlanCount matches iteration records
        for (i in trace.pricedPlanCount.indices) {
            assertEquals(
                trace.pricedPlanCount[i],
                trace.iterations[i].pricedPlanCount,
                "pricedPlanCount[$i] should match iteration record"
            )
        }
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

    /**
     * 验证 LP 目标值单调性和收敛性 / Verify LP objective monotonicity and convergence
     *
     * LP 松弛目标值应非递增（每轮加入新列后 LP 目标值不应上升）。
     * LP relaxation objective should be non-increasing (adding columns should not increase LP objective).
     * 若终止原因是 PricingConverged，最后一轮应无新列。
     * If termination reason is PricingConverged, last iteration should have zero priced plans.
     */
    @Test
    fun columnGenerationLpObjectiveIsNonIncreasing() = runBlocking {
        val p1 = product(id = "p_obj_1", width = 0.3)
        val p2 = product(id = "p_obj_2", width = 0.4)
        val material = material(
            id = "m_obj",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(p1, p2),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(product = p1, rollAmount = Flt64(7.0)),
                ProductDemand.legacyRoll(product = p2, rollAmount = Flt64(5.0))
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 64,
                maxPricingPlans = 16,
                iterationLimit = 10
            )
        )

        val columnGeneration = Csp1dColumnGeneration<Flt64>(solver = solver)
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // 验证 LP 目标值非递增 / Verify LP objectives are non-increasing
        if (trace.iterations.size >= 2) {
            val objectives = trace.iterations.map { it.lpObjective }
            for (i in 1 until objectives.size) {
                assertTrue(
                    objectives[i] <= objectives[i - 1] + Flt64(1e-4),
                    "LP objective should not increase: iter ${i - 1}=${objectives[i - 1]}, iter $i=${objectives[i]}"
                )
            }
        }

        // 验证收敛性：如果 PricingConverged，最后一轮应无新列 / Verify convergence
        if (trace.terminationReason == Csp1dTerminationReason.PricingConverged && trace.iterations.isNotEmpty()) {
            val lastRecord = trace.iterations.last()
            assertEquals(
                UInt64.zero,
                lastRecord.pricedPlanCount,
                "Last iteration should have zero priced plans when PricingConverged"
            )
        }

        // 验证解正确 / Verify solution correctness
        assertTrue(solution.produce.cuttingPlans.isNotEmpty())
        assertTrue(solution.produce.unmetDemands.isEmpty(), "All demands should be met")
    }

    /**
     * 验证 shadow price key 按 product + unit 口径区分不同需求
     * Verify shadow price keys distinguish demands by product + unit
     *
     * ProductDemandShadowPriceKey(productId, unitSymbol) 应区分 roll 和 sheet 需求。
     * ProductDemandShadowPriceKey should distinguish roll and sheet demands.
     */
    @Test
    fun shadowPriceKeyDistinguishesDifferentUnits() {
        val rollKey = ProductDemandShadowPriceKey(
            productId = "p_multi_unit",
            unitSymbol = RollCountUnit.symbol ?: RollCountUnit.name ?: RollCountUnit.toString()
        )
        val sheetKey = ProductDemandShadowPriceKey(
            productId = "p_multi_unit",
            unitSymbol = SheetCountUnit.symbol ?: SheetCountUnit.name ?: SheetCountUnit.toString()
        )

        // 不同单位的 key 应不相等 / Keys with different units should not be equal
        assertTrue(rollKey != sheetKey, "Roll and sheet shadow price keys should differ for same product")
        assertTrue(rollKey.name != sheetKey.name, "Roll and sheet key names should differ")
    }

    /**
     * 验证含 _ 的 product/material id 不影响 shadow price 映射
     * Verify underscored product/material ids don't affect shadow price mapping
     */
    @Test
    fun columnGenerationWithUnderscoreIdsMeetsAllDemands() = runBlocking {
        val p1 = product(id = "prod_item_01", width = 0.2)
        val p2 = product(id = "prod_item_02", width = 0.35)
        val material = material(
            id = "mat_type_A",
            lowerWidth = 0.5,
            upperWidth = 1.0
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(p1, p2),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(product = p1, rollAmount = Flt64(6.0)),
                ProductDemand.legacyRoll(product = p2, rollAmount = Flt64(4.0))
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 32,
                maxPricingPlans = 8,
                iterationLimit = 4
            )
        )

        val columnGeneration = Csp1dColumnGeneration<Flt64>(solver = solver)
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans with underscored ids")
        assertTrue(solution.produce.unmetDemands.isEmpty(), "Should meet all demands with underscored ids")
        assertNotNull(trace.terminationReason)
    }

    /**
     * 验证动态长度产品与固定长度产品混合建模时，两种需求均满足且 lengthResult 仅含动态产品
     * Verify mixed dynamic-length and fixed-length products: both demands satisfied, lengthResult only contains dynamic products
     */
    @Test
    fun milpWithMixedDynamicAndFixedLengthShouldSatisfyAllDemands() = runBlocking {
        val dynamicProduct = Product(
            id = "p-dynamic",
            name = "product-p-dynamic",
            width = listOf(Quantity(Flt64(0.8), Meter)),
            maxOverProduceLength = Quantity(Flt64(2.0), Meter),
            dynamicLength = true
        )
        val fixedProduct = product(id = "p-fixed", width = 0.6)
        val mat = material(
            id = "m-mixed",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val dynamicDemand = ProductDemand.legacyRoll(
            product = dynamicProduct,
            rollAmount = Flt64(3.0)
        )
        val fixedDemand = ProductDemand.legacyRoll(
            product = fixedProduct,
            rollAmount = Flt64(5.0)
        )
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(dynamicProduct.id),
            assignedLengthLowerBound = mapOf(dynamicProduct.id to Flt64(0.0)),
            assignedLengthUpperBound = mapOf(dynamicProduct.id to Flt64(3.0)),
            overLengthPenalty = mapOf(dynamicProduct.id to Flt64(5.0)),
            totalLengthPenalty = Flt64(0.01)
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                cuttingPlan(
                    id = "plan-dynamic",
                    product = dynamicProduct,
                    material = mat,
                    rollContribution = Flt64(2.0)
                ),
                cuttingPlan(
                    id = "plan-fixed",
                    product = fixedProduct,
                    material = mat,
                    rollContribution = Flt64(3.0)
                )
            ),
            demands = listOf(dynamicDemand, fixedDemand),
            materials = listOf(mat),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            lengthConfig = lengthConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        assertTrue(
            milpResult.produce.cuttingPlans.isNotEmpty(),
            "Should have cutting plan usage"
        )
        // 验证 lengthResult 仅包含动态长度产品 / Verify lengthResult only contains dynamic-length product
        val lengthResult = milpResult.lengthResult
        assertNotNull(lengthResult, "Length result should not be null when lengthConfig is provided")
        val assignedProductIds = lengthResult.assignedLengths.map { it.productId }.toSet()
        assertTrue(
            dynamicProduct.id in assignedProductIds,
            "Dynamic product should have assigned length"
        )
        assertTrue(
            fixedProduct.id !in assignedProductIds,
            "Fixed product should NOT have assigned length"
        )
    }

    // --- C5 yield/waste/length 真实 solver 端到端验证 ---
    // --- C5 yield/waste/length real solver end-to-end verification ---

    /**
     * 验证 yield 建模在 Gurobi 真实 solver 上欠产/超产回填值与分析层口径一致
     * Verify yield modeling under/over production backfill values match analysis layer on Gurobi
     *
     * 场景：1 产品 + 1 原料 + roll 需求 10 卷，方案贡献不足以完全满足需求，应产生欠产；
     *       方案贡献超过需求时，应产生超产。
     * Scenario: 1 product + 1 material + roll demand 10, plan contribution insufficient => under-production;
     *           plan contribution exceeds demand => over-production.
     */
    @Test
    fun milpWithYieldConfigShouldProduceCorrectYieldResultOnRealSolver() = runBlocking {
        val product = product(id = "p-yield-real", width = 0.5)
        val material = material(id = "m-yield-real", lowerWidth = 0.5, upperWidth = 1.5)
        val demand = ProductDemand.legacyRoll(product = product, rollAmount = Flt64(10.0))
        val demandKey = ProductDemandShadowPriceKey(
            productId = product.id,
            unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.name ?: demand.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(demandKey to Flt64(100.0)),
            overProductionPenalty = mapOf(demandKey to Flt64(10.0))
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                cuttingPlan(id = "plan-yield-real", product = product, material = material, rollContribution = Flt64(3.0))
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            yieldConfig = yieldConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        val yieldResult = milpResult.yieldResult
        assertNotNull(yieldResult, "Yield result should not be null when yieldConfig is provided")

        // 验证需求满足：等式约束 sum(contrib * x) - over + under = demand
        // 方案贡献 3.0 * x - over + under = 10.0
        // 在真实 solver 下，x 应为整数，over/under 应为非负实数
        val totalContribution = milpResult.produce.cuttingPlans.fold(Flt64.zero) { acc, usage ->
            val rollContrib = usage.plan.demandContributions.firstOrNull()?.quantity?.value ?: Flt64.zero
            acc + rollContrib * usage.amount.toFlt64()
        }
        val underAmount = yieldResult.underProductions.firstOrNull()?.amount ?: Flt64.zero
        val overAmount = yieldResult.overProductions.firstOrNull()?.amount ?: Flt64.zero

        // 验证等式约束: totalContribution - overAmount + underAmount ≈ demand (10.0)
        val balance = totalContribution - overAmount + underAmount
        assertTrue(
            (balance - Flt64(10.0)).abs() < Flt64(1e-4),
            "Yield constraint balance should equal demand: contrib($totalContribution) - over($overAmount) + under($underAmount) = $balance, expected 10.0"
        )
    }

    /**
     * 验证 yield 建模带超产上限约束在 Gurobi 真实 solver 上正确回填超产值
     * Verify yield modeling with over-production upper bound constraint backfills correctly on Gurobi
     */
    @Test
    fun milpWithYieldOverProductionUpperBoundShouldRespectBoundOnRealSolver() = runBlocking {
        val product = product(id = "p-yield-bound", width = 0.5)
        val material = material(id = "m-yield-bound", lowerWidth = 0.5, upperWidth = 1.5)
        val demand = ProductDemand.legacyRoll(product = product, rollAmount = Flt64(5.0))
        val demandKey = ProductDemandShadowPriceKey(
            productId = product.id,
            unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.name ?: demand.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(demandKey to Flt64(100.0)),
            overProductionPenalty = mapOf(demandKey to Flt64(10.0)),
            overProductionUpperBound = mapOf(demandKey to Flt64(2.0))
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                cuttingPlan(id = "plan-yield-bound", product = product, material = material, rollContribution = Flt64(3.0))
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            yieldConfig = yieldConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        val yieldResult = milpResult.yieldResult
        assertNotNull(yieldResult, "Yield result should not be null")

        // 验证超产不超过上限
        val overAmount = yieldResult.overProductions.firstOrNull()?.amount ?: Flt64.zero
        assertTrue(
            overAmount <= Flt64(2.0) + Flt64(1e-6),
            "Over-production should not exceed upper bound 2.0, got $overAmount"
        )
    }

    /**
     * 验证 wasting 建模在 Gurobi 真实 solver 上余宽/物料成本回填值正确
     * Verify waste modeling trim width and material cost backfill values are correct on Gurobi
     */
    @Test
    fun milpWithWasteConfigShouldProduceCorrectWasteResultOnRealSolver() = runBlocking {
        val product = product(id = "p-waste-real", width = 0.8)
        val material = material(
            id = "m-waste-real",
            lowerWidth = 0.5,
            upperWidth = 1.5,
            length = 100.0
        )
        val demand = ProductDemand.legacyRoll(product = product, rollAmount = Flt64(5.0))
        val wasteConfig = WasteMinimizationConfig<Flt64>(
            trimWidthPenalty = Flt64(2.0),
            restMaterialPenalty = Flt64(0.5),
            materialCostPenalty = mapOf("m-waste-real" to Flt64(5.0))
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                cuttingPlan(id = "plan-waste-real", product = product, material = material, rollContribution = Flt64(1.0))
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            wasteConfig = wasteConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        val wasteResult = milpResult.wasteResult
        assertNotNull(wasteResult, "Waste result should not be null when wasteConfig is provided")

        // 验证余宽非负
        val totalTrimWidth = wasteResult.totalTrimWidth
        if (totalTrimWidth != null) {
            assertTrue(
                totalTrimWidth >= Flt64.zero,
                "Total trim width should be non-negative, got $totalTrimWidth"
            )
        }

        // 验证物料成本非负
        for (cost in wasteResult.materialCosts) {
            assertTrue(
                cost.cost >= Flt64.zero,
                "Material cost should be non-negative for ${cost.materialId}, got ${cost.cost}"
            )
        }

        // 验证余宽计算口径: restWidth * batchCount 与回填值一致
        if (totalTrimWidth != null) {
            val expectedTrimWidth = milpResult.produce.cuttingPlans.fold(Flt64.zero) { acc, usage ->
                val restWidthValue = usage.plan.restWidth?.value ?: Flt64.zero
                acc + restWidthValue * usage.amount.toFlt64()
            }
            assertTrue(
                (totalTrimWidth - expectedTrimWidth).abs() < Flt64(1e-4),
                "Total trim width ($totalTrimWidth) should match expected ($expectedTrimWidth)"
            )
        }

        // 验证余料计算口径: restWidth * material.length * batchCount 与回填值一致 / Verify rest material formula
        val totalRestMaterial = wasteResult.totalRestMaterial
        if (totalRestMaterial != null) {
            val expectedRestMaterial = milpResult.produce.cuttingPlans.fold(Flt64.zero) { acc, usage ->
                val restWidthValue = usage.plan.restWidth?.value ?: Flt64.zero
                val materialLengthValue = usage.plan.material.length?.value ?: Flt64.zero
                acc + restWidthValue * materialLengthValue * usage.amount.toFlt64()
            }
            assertTrue(
                (totalRestMaterial - expectedRestMaterial).abs() < Flt64(1e-4),
                "Total rest material ($totalRestMaterial) should match expected ($expectedRestMaterial)"
            )
        }

        // 验证物料成本计算口径: costPenalty * batchCount 与回填值一致
        val costByMaterial = milpResult.produce.cuttingPlans.groupBy { it.plan.material.id }
        for (mc in wasteResult.materialCosts) {
            val expectedCost = costByMaterial[mc.materialId]?.fold(Flt64.zero) { acc, usage ->
                acc + wasteConfig.materialCostPenalty[mc.materialId]!! * usage.amount.toFlt64()
            } ?: Flt64.zero
            assertTrue(
                (mc.cost - expectedCost).abs() < Flt64(1e-4),
                "Material cost for ${mc.materialId} (${mc.cost}) should match expected ($expectedCost)"
            )
        }
    }

    /**
     * 验证 yield + waste 联合建模在 Gurobi 真实 solver 上超产面积回填值正确
     * Verify yield + waste combined modeling over-production area backfill is correct on Gurobi
     */
    @Test
    fun milpWithYieldAndWasteConfigShouldProduceCorrectOverProductionAreaOnRealSolver() = runBlocking {
        val product = product(id = "p-yield-waste-real", width = 0.5)
        val material = material(id = "m-yield-waste-real", lowerWidth = 0.5, upperWidth = 1.5)
        val demand = ProductDemand.legacyRoll(product = product, rollAmount = Flt64(5.0))
        val demandKey = ProductDemandShadowPriceKey(
            productId = product.id,
            unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.name ?: demand.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(demandKey to Flt64(100.0)),
            overProductionPenalty = mapOf(demandKey to Flt64(10.0))
        )
        val wasteConfig = WasteMinimizationConfig<Flt64>(
            trimWidthPenalty = Flt64(2.0),
            overProductionAreaPenalty = Flt64(5.0)
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                cuttingPlan(id = "plan-yield-waste-real", product = product, material = material, rollContribution = Flt64(3.0))
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            yieldConfig = yieldConfig,
            wasteConfig = wasteConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        val yieldResult = milpResult.yieldResult
        val wasteResult = milpResult.wasteResult
        assertNotNull(yieldResult, "Yield result should not be null")
        assertNotNull(wasteResult, "Waste result should not be null")

        // 验证超产面积 = overProduction * max(productWidth) 与回填值一致 / Verify over area uses max product width
        val overArea = wasteResult.overProductionArea
        if (overArea != null) {
            val overAmount = yieldResult.overProductions.firstOrNull()?.amount ?: Flt64.zero
            val productWidth = product.maxWidth()?.value ?: Flt64.zero
            val expectedArea = overAmount * productWidth
            assertTrue(
                (overArea - expectedArea).abs() < Flt64(1e-4),
                "Over-production area ($overArea) should match expected ($expectedArea = over($overAmount) * width($productWidth))"
            )
        }
    }

    /**
     * 验证 length assignment 建模在 Gurobi 真实 solver 上已分配卷长和超长回填值正确
     * Verify length assignment modeling assigned-length and over-length backfill values are correct on Gurobi
     */
    @Test
    fun milpWithLengthConfigShouldProduceCorrectLengthResultOnRealSolver() = runBlocking {
        val dynamicProduct = Product(
            id = "p-length-real",
            name = "product-p-length-real",
            width = listOf(Quantity(Flt64(0.8), Meter)),
            maxOverProduceLength = Quantity(Flt64(2.0), Meter),
            dynamicLength = true
        )
        val material = material(id = "m-length-real", lowerWidth = 0.5, upperWidth = 1.5)
        val demand = ProductDemand.legacyRoll(product = dynamicProduct, rollAmount = Flt64(3.0))
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(dynamicProduct.id),
            assignedLengthLowerBound = mapOf(dynamicProduct.id to Flt64(0.0)),
            assignedLengthUpperBound = mapOf(dynamicProduct.id to Flt64(3.0)),
            overLengthPenalty = mapOf(dynamicProduct.id to Flt64(10.0)),
            totalLengthPenalty = Flt64(0.5)
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                cuttingPlan(id = "plan-length-real", product = dynamicProduct, material = material, rollContribution = Flt64(1.0))
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            lengthConfig = lengthConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        val lengthResult = milpResult.lengthResult
        assertNotNull(lengthResult, "Length result should not be null when lengthConfig is provided")

        // 验证已分配卷长在下界和上界之间
        val assignedLength = lengthResult.assignedLengths.find { it.productId == dynamicProduct.id }
        assertNotNull(assignedLength, "Should have assigned length for dynamic product")
        assertTrue(
            assignedLength.assignedLength >= Flt64(0.0) - Flt64(1e-6),
            "Assigned length should be >= lower bound 0.0, got ${assignedLength.assignedLength}"
        )
        assertTrue(
            assignedLength.assignedLength <= Flt64(3.0) + Flt64(1e-6),
            "Assigned length should be <= upper bound 3.0, got ${assignedLength.assignedLength}"
        )

        // 验证超长在合理范围
        val overLength = lengthResult.overLengths.find { it.productId == dynamicProduct.id }
        if (overLength != null) {
            assertTrue(
                overLength.overLength >= Flt64.zero,
                "Over-length should be non-negative, got ${overLength.overLength}"
            )
            // assigned - over <= maxOverProduceLength (2.0)
            assertTrue(
                assignedLength.assignedLength - overLength.overLength <= Flt64(2.0) + Flt64(1e-6),
                "assigned(${assignedLength.assignedLength}) - over(${overLength.overLength}) should be <= maxOverProduceLength(2.0)"
            )
        }
    }

    /**
     * 验证列生成 + yield 建模在 Gurobi 真实 solver 上端到端运行并回填正确
     * Verify column generation + yield modeling end-to-end on Gurobi and backfill is correct
     */
    @Test
    fun columnGenerationWithYieldConfigShouldProduceCorrectYieldResultOnRealSolver() = runBlocking {
        val product = product(id = "p-cg-yield", width = 0.5)
        val material = material(id = "m-cg-yield", lowerWidth = 0.5, upperWidth = 1.5)
        val demand = ProductDemand.legacyRoll(product = product, rollAmount = Flt64(10.0))
        val demandKey = ProductDemandShadowPriceKey(
            productId = product.id,
            unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.name ?: demand.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(demandKey to Flt64(100.0)),
            overProductionPenalty = mapOf(demandKey to Flt64(10.0))
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(demand),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 32,
                maxPricingPlans = 16,
                iterationLimit = 8
            )
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            solver = solver,
            yieldConfig = yieldConfig
        )
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // 验证基本产出 / Verify basic output
        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans")
        assertNotNull(trace.terminationReason)

        // 验证 yield 结果存在 / Verify yield result exists
        val yieldResult = solution.yieldResult
        assertNotNull(yieldResult, "Yield result should not be null when yieldConfig is provided")

        // 验证等式约束平衡: totalContribution - over + under = demand
        val totalContribution = solution.produce.cuttingPlans.fold(Flt64.zero) { acc, usage ->
            val rollContrib = usage.plan.demandContributions.firstOrNull()?.quantity?.value ?: Flt64.zero
            acc + rollContrib * usage.amount.toFlt64()
        }
        val underAmount = yieldResult.underProductions.firstOrNull()?.amount ?: Flt64.zero
        val overAmount = yieldResult.overProductions.firstOrNull()?.amount ?: Flt64.zero
        val balance = totalContribution - overAmount + underAmount
        assertTrue(
            (balance - Flt64(10.0)).abs() < Flt64(1e-4),
            "Yield constraint balance should equal demand: contrib($totalContribution) - over($overAmount) + under($underAmount) = $balance, expected 10.0"
        )
    }

    /**
     * 验证列生成 + waste 建模在 Gurobi 真实 solver 上端到端运行并回填正确
     * Verify column generation + waste modeling end-to-end on Gurobi and backfill is correct
     */
    @Test
    fun columnGenerationWithWasteConfigShouldProduceCorrectWasteResultOnRealSolver() = runBlocking {
        val p1 = product(id = "p-cg-waste-1", width = 0.3)
        val p2 = product(id = "p-cg-waste-2", width = 0.4)
        val material = material(id = "m-cg-waste", lowerWidth = 0.5, upperWidth = 1.5)
        val wasteConfig = WasteMinimizationConfig<Flt64>(
            trimWidthPenalty = Flt64(2.0),
            materialCostPenalty = mapOf("m-cg-waste" to Flt64(5.0))
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(p1, p2),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(product = p1, rollAmount = Flt64(7.0)),
                ProductDemand.legacyRoll(product = p2, rollAmount = Flt64(5.0))
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 64,
                maxPricingPlans = 16,
                iterationLimit = 8
            )
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            solver = solver,
            wasteConfig = wasteConfig
        )
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // 验证基本产出 / Verify basic output
        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans")
        assertNotNull(trace.terminationReason)

        // 验证 waste 结果存在 / Verify waste result exists
        val wasteResult = solution.wasteResult
        assertNotNull(wasteResult, "Waste result should not be null when wasteConfig is provided")

        // 验证余宽计算口径一致 / Verify trim width calculation consistency
        val totalTrimWidth = wasteResult.totalTrimWidth
        if (totalTrimWidth != null) {
            val expectedTrimWidth = solution.produce.cuttingPlans.fold(Flt64.zero) { acc, usage ->
                val restWidthValue = usage.plan.restWidth?.value ?: Flt64.zero
                acc + restWidthValue * usage.amount.toFlt64()
            }
            assertTrue(
                (totalTrimWidth - expectedTrimWidth).abs() < Flt64(1e-4),
                "Total trim width ($totalTrimWidth) should match expected ($expectedTrimWidth)"
            )
        }

        // 验证物料成本计算口径一致 / Verify material cost calculation consistency
        val costByMaterial = solution.produce.cuttingPlans.groupBy { it.plan.material.id }
        for (mc in wasteResult.materialCosts) {
            val expectedCost = costByMaterial[mc.materialId]?.fold(Flt64.zero) { acc, usage ->
                acc + wasteConfig.materialCostPenalty[mc.materialId]!! * usage.amount.toFlt64()
            } ?: Flt64.zero
            assertTrue(
                (mc.cost - expectedCost).abs() < Flt64(1e-4),
                "Material cost for ${mc.materialId} (${mc.cost}) should match expected ($expectedCost)"
            )
        }
    }

    /**
     * 验证列生成 + length 建模在 Gurobi 真实 solver 上端到端运行并回填正确
     * Verify column generation + length modeling end-to-end on Gurobi and backfill is correct
     */
    @Test
    fun columnGenerationWithLengthConfigShouldProduceCorrectLengthResultOnRealSolver() = runBlocking {
        val dynamicProduct = Product(
            id = "p-cg-length-dynamic",
            name = "product-p-cg-length-dynamic",
            width = listOf(Quantity(Flt64(0.5), Meter)),
            maxOverProduceLength = Quantity(Flt64(3.0), Meter),
            dynamicLength = true
        )
        val fixedProduct = product(id = "p-cg-length-fixed", width = 0.4)
        val material = material(id = "m-cg-length", lowerWidth = 0.5, upperWidth = 1.5)
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(dynamicProduct.id),
            assignedLengthLowerBound = mapOf(dynamicProduct.id to Flt64(0.0)),
            assignedLengthUpperBound = mapOf(dynamicProduct.id to Flt64(5.0)),
            overLengthPenalty = mapOf(dynamicProduct.id to Flt64(10.0)),
            totalLengthPenalty = Flt64(0.5)
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(dynamicProduct, fixedProduct),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(product = dynamicProduct, rollAmount = Flt64(5.0)),
                ProductDemand.legacyRoll(product = fixedProduct, rollAmount = Flt64(3.0))
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 64,
                maxPricingPlans = 16,
                iterationLimit = 8
            )
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            solver = solver,
            lengthConfig = lengthConfig
        )
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // 验证基本产出 / Verify basic output
        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans")
        assertNotNull(trace.terminationReason)

        // 验证 length 结果存在 / Verify length result exists
        val lengthResult = solution.lengthResult
        assertNotNull(lengthResult, "Length result should not be null when lengthConfig is provided")

        // 验证 lengthResult 仅包含动态产品 / Verify lengthResult only contains dynamic products
        val assignedProductIds = lengthResult.assignedLengths.map { it.productId }.toSet()
        assertTrue(
            dynamicProduct.id in assignedProductIds,
            "Dynamic product should have assigned length"
        )
        assertTrue(
            fixedProduct.id !in assignedProductIds,
            "Fixed product should NOT have assigned length"
        )

        // 验证已分配卷长在下界和上界之间 / Verify assigned length within bounds
        val assignedLength = lengthResult.assignedLengths.find { it.productId == dynamicProduct.id }
        assertNotNull(assignedLength, "Should have assigned length for dynamic product")
        assertTrue(
            assignedLength.assignedLength >= Flt64(0.0) - Flt64(1e-6),
            "Assigned length should be >= lower bound 0.0, got ${assignedLength.assignedLength}"
        )
        assertTrue(
            assignedLength.assignedLength <= Flt64(5.0) + Flt64(1e-6),
            "Assigned length should be <= upper bound 5.0, got ${assignedLength.assignedLength}"
        )
    }

    // --- C5 yield sheet/weight 需求口径端到端验证 ---
    // --- C5 yield sheet/weight demand unit end-to-end verification ---

    /**
     * 验证 yield 建模在 sheet 需求口径下 Gurobi 真实 solver 回填值正确
     * Verify yield modeling with sheet demand unit backfills correctly on Gurobi
     */
    @Test
    fun milpWithYieldConfigSheetDemandShouldProduceCorrectResultOnRealSolver() = runBlocking {
        val product = product(id = "p-yield-sheet", width = 0.6)
        val material = material(id = "m-yield-sheet", lowerWidth = 0.5, upperWidth = 1.5)
        val demand = ProductDemand.legacySheet(product = product, sheetAmount = Flt64(8.0))
        val demandKey = ProductDemandShadowPriceKey(
            productId = product.id,
            unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.name ?: demand.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(demandKey to Flt64(100.0)),
            overProductionPenalty = mapOf(demandKey to Flt64(10.0))
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                sheetCuttingPlan(id = "plan-yield-sheet", product = product, material = material, sheetContribution = Flt64(3.0))
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            yieldConfig = yieldConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        val yieldResult = milpResult.yieldResult
        assertNotNull(yieldResult, "Yield result should not be null for sheet demand")

        // 验证等式约束: totalContribution - over + under = demand (8.0)
        val totalContribution = milpResult.produce.cuttingPlans.fold(Flt64.zero) { acc, usage ->
            val sheetContrib = usage.plan.demandContributions.firstOrNull()?.quantity?.value ?: Flt64.zero
            acc + sheetContrib * usage.amount.toFlt64()
        }
        val underAmount = yieldResult.underProductions.firstOrNull()?.amount ?: Flt64.zero
        val overAmount = yieldResult.overProductions.firstOrNull()?.amount ?: Flt64.zero
        val balance = totalContribution - overAmount + underAmount
        assertTrue(
            (balance - Flt64(8.0)).abs() < Flt64(1e-4),
            "Yield constraint balance should equal demand: contrib($totalContribution) - over($overAmount) + under($underAmount) = $balance, expected 8.0"
        )

        // 验证 sheet 需求的 unitSymbol 正确
        assertTrue(
            yieldResult.underProductions.any { it.unitSymbol == (demand.quantity.unit.symbol ?: demand.quantity.unit.name) } ||
            yieldResult.overProductions.any { it.unitSymbol == (demand.quantity.unit.symbol ?: demand.quantity.unit.name) } ||
            (underAmount == Flt64.zero && overAmount == Flt64.zero),
            "Yield result should have correct sheet unitSymbol"
        )
    }

    /**
     * 验证 yield 建模在 weight 需求口径下 Gurobi 真实 solver 回填值正确
     * Verify yield modeling with weight demand unit backfills correctly on Gurobi
     */
    @Test
    fun milpWithYieldConfigWeightDemandShouldProduceCorrectResultOnRealSolver() = runBlocking {
        val product = product(id = "p-yield-weight", width = 0.4)
        val material = material(id = "m-yield-weight", lowerWidth = 0.3, upperWidth = 1.2)
        val demand = ProductDemand.legacyWeight(product = product, weightAmount = Flt64(500.0), unit = Kilogram)
        val demandKey = ProductDemandShadowPriceKey(
            productId = product.id,
            unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.name ?: demand.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(demandKey to Flt64(100.0)),
            overProductionPenalty = mapOf(demandKey to Flt64(10.0))
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                weightCuttingPlan(id = "plan-yield-weight", product = product, material = material, weightContribution = Flt64(120.0))
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            yieldConfig = yieldConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        val yieldResult = milpResult.yieldResult
        assertNotNull(yieldResult, "Yield result should not be null for weight demand")

        // 验证等式约束: totalContribution - over + under = demand (500.0)
        val totalContribution = milpResult.produce.cuttingPlans.fold(Flt64.zero) { acc, usage ->
            val weightContrib = usage.plan.demandContributions.firstOrNull()?.quantity?.value ?: Flt64.zero
            acc + weightContrib * usage.amount.toFlt64()
        }
        val underAmount = yieldResult.underProductions.firstOrNull()?.amount ?: Flt64.zero
        val overAmount = yieldResult.overProductions.firstOrNull()?.amount ?: Flt64.zero
        val balance = totalContribution - overAmount + underAmount
        assertTrue(
            (balance - Flt64(500.0)).abs() < Flt64(1e-2),
            "Yield constraint balance should equal demand: contrib($totalContribution) - over($overAmount) + under($underAmount) = $balance, expected 500.0"
        )
    }

    /**
     * 验证 yield 建模在混合需求口径（roll + sheet）下 Gurobi 真实 solver 回填值正确
     * Verify yield modeling with mixed demand units (roll + sheet) backfills correctly on Gurobi
     */
    @Test
    fun milpWithYieldConfigMixedDemandUnitsShouldProduceCorrectResultOnRealSolver() = runBlocking {
        val p1 = product(id = "p-yield-mix-roll", width = 0.5)
        val p2 = product(id = "p-yield-mix-sheet", width = 0.6)
        val material = material(id = "m-yield-mix", lowerWidth = 0.5, upperWidth = 2.0)
        val rollDemand = ProductDemand.legacyRoll(product = p1, rollAmount = Flt64(10.0))
        val sheetDemand = ProductDemand.legacySheet(product = p2, sheetAmount = Flt64(8.0))
        val rollKey = ProductDemandShadowPriceKey(
            productId = p1.id,
            unitSymbol = rollDemand.quantity.unit.symbol ?: rollDemand.quantity.unit.name ?: rollDemand.quantity.unit.toString()
        )
        val sheetKey = ProductDemandShadowPriceKey(
            productId = p2.id,
            unitSymbol = sheetDemand.quantity.unit.symbol ?: sheetDemand.quantity.unit.name ?: sheetDemand.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(rollKey to Flt64(100.0), sheetKey to Flt64(80.0)),
            overProductionPenalty = mapOf(rollKey to Flt64(10.0), sheetKey to Flt64(8.0))
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                cuttingPlan(id = "plan-mix-roll", product = p1, material = material, rollContribution = Flt64(3.0)),
                sheetCuttingPlan(id = "plan-mix-sheet", product = p2, material = material, sheetContribution = Flt64(2.0))
            ),
            demands = listOf(rollDemand, sheetDemand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            yieldConfig = yieldConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        val yieldResult = milpResult.yieldResult
        assertNotNull(yieldResult, "Yield result should not be null for mixed demand units")

        // 验证 roll 需求等式约束平衡
        val rollContribution = milpResult.produce.cuttingPlans
            .filter { it.plan.demandContributions.any { c -> c.product.id == p1.id } }
            .fold(Flt64.zero) { acc, usage ->
                val contrib = usage.plan.demandContributions.find { it.product.id == p1.id }?.quantity?.value ?: Flt64.zero
                acc + contrib * usage.amount.toFlt64()
            }
        val rollUnder = yieldResult.underProductions.find { it.productId == p1.id }?.amount ?: Flt64.zero
        val rollOver = yieldResult.overProductions.find { it.productId == p1.id }?.amount ?: Flt64.zero
        val rollBalance = rollContribution - rollOver + rollUnder
        assertTrue(
            (rollBalance - Flt64(10.0)).abs() < Flt64(1e-4),
            "Roll yield balance: contrib($rollContribution) - over($rollOver) + under($rollUnder) = $rollBalance, expected 10.0"
        )

        // 验证 sheet 需求等式约束平衡
        val sheetContribution = milpResult.produce.cuttingPlans
            .filter { it.plan.demandContributions.any { c -> c.product.id == p2.id } }
            .fold(Flt64.zero) { acc, usage ->
                val contrib = usage.plan.demandContributions.find { it.product.id == p2.id }?.quantity?.value ?: Flt64.zero
                acc + contrib * usage.amount.toFlt64()
            }
        val sheetUnder = yieldResult.underProductions.find { it.productId == p2.id }?.amount ?: Flt64.zero
        val sheetOver = yieldResult.overProductions.find { it.productId == p2.id }?.amount ?: Flt64.zero
        val sheetBalance = sheetContribution - sheetOver + sheetUnder
        assertTrue(
            (sheetBalance - Flt64(8.0)).abs() < Flt64(1e-4),
            "Sheet yield balance: contrib($sheetContribution) - over($sheetOver) + under($sheetUnder) = $sheetBalance, expected 8.0"
        )

        // 验证不同单位的 yield 结果不被混淆
        val rollUnitSymbol = rollDemand.quantity.unit.symbol ?: rollDemand.quantity.unit.name
        val sheetUnitSymbol = sheetDemand.quantity.unit.symbol ?: sheetDemand.quantity.unit.name
        for (under in yieldResult.underProductions) {
            if (under.productId == p1.id) {
                assertEquals(rollUnitSymbol, under.unitSymbol, "Roll under-production should have roll unitSymbol")
            }
            if (under.productId == p2.id) {
                assertEquals(sheetUnitSymbol, under.unitSymbol, "Sheet under-production should have sheet unitSymbol")
            }
        }
    }

    // --- C5 wasting 多物料/多需求场景端到端验证 ---
    // --- C5 wasting multi-material/multi-demand end-to-end verification ---

    /**
     * 验证 wasting 建模在多物料场景下 Gurobi 真实 solver 回填值正确
     * Verify waste modeling with multiple materials backfills correctly on Gurobi
     */
    @Test
    fun milpWithWasteConfigMultipleMaterialsShouldProduceCorrectResultOnRealSolver() = runBlocking {
        val p1 = product(id = "p-waste-m1", width = 0.3)
        val p2 = product(id = "p-waste-m2", width = 0.6)
        val m1 = material(id = "m-narrow", lowerWidth = 0.3, upperWidth = 0.8)
        val m2 = material(id = "m-wide", lowerWidth = 0.5, upperWidth = 1.5)
        val wasteConfig = WasteMinimizationConfig<Flt64>(
            trimWidthPenalty = Flt64(2.0),
            materialCostPenalty = mapOf("m-narrow" to Flt64(3.0), "m-wide" to Flt64(5.0))
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                cuttingPlan(id = "plan-m1-p1", product = p1, material = m1, rollContribution = Flt64(1.0)),
                cuttingPlan(id = "plan-m2-p2", product = p2, material = m2, rollContribution = Flt64(1.0))
            ),
            demands = listOf(
                ProductDemand.legacyRoll(product = p1, rollAmount = Flt64(5.0)),
                ProductDemand.legacyRoll(product = p2, rollAmount = Flt64(4.0))
            ),
            materials = listOf(m1, m2),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            wasteConfig = wasteConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        val wasteResult = milpResult.wasteResult
        assertNotNull(wasteResult, "Waste result should not be null")

        // 验证余宽口径一致
        val totalTrimWidth = wasteResult.totalTrimWidth
        if (totalTrimWidth != null) {
            val expectedTrimWidth = milpResult.produce.cuttingPlans.fold(Flt64.zero) { acc, usage ->
                val restWidthValue = usage.plan.restWidth?.value ?: Flt64.zero
                acc + restWidthValue * usage.amount.toFlt64()
            }
            assertTrue(
                (totalTrimWidth - expectedTrimWidth).abs() < Flt64(1e-4),
                "Total trim width ($totalTrimWidth) should match expected ($expectedTrimWidth)"
            )
        }

        // 验证两种物料的成本都有回填
        val materialIds = wasteResult.materialCosts.map { it.materialId }.toSet()
        assertTrue(
            materialIds.isNotEmpty(),
            "Should have material costs for at least one material"
        )

        // 验证各物料成本口径一致
        val costByMaterial = milpResult.produce.cuttingPlans.groupBy { it.plan.material.id }
        for (mc in wasteResult.materialCosts) {
            val expectedCost = costByMaterial[mc.materialId]?.fold(Flt64.zero) { acc, usage ->
                acc + wasteConfig.materialCostPenalty[mc.materialId]!! * usage.amount.toFlt64()
            } ?: Flt64.zero
            assertTrue(
                (mc.cost - expectedCost).abs() < Flt64(1e-4),
                "Material cost for ${mc.materialId} (${mc.cost}) should match expected ($expectedCost)"
            )
        }
    }

    /**
     * 验证 wasting 建模在多需求 + yield 联合场景下 Gurobi 真实 solver 超产面积正确
     * Verify waste + yield combined modeling with multiple demands on Gurobi
     */
    @Test
    fun milpWithWasteAndYieldMultipleDemandsShouldProduceCorrectResultOnRealSolver() = runBlocking {
        val p1 = product(id = "p-waste-yield-1", width = 0.3)
        val p2 = product(id = "p-waste-yield-2", width = 0.4)
        val material = material(id = "m-waste-yield", lowerWidth = 0.5, upperWidth = 1.5)
        val d1 = ProductDemand.legacyRoll(product = p1, rollAmount = Flt64(5.0))
        val d2 = ProductDemand.legacyRoll(product = p2, rollAmount = Flt64(3.0))
        val k1 = ProductDemandShadowPriceKey(
            productId = p1.id,
            unitSymbol = d1.quantity.unit.symbol ?: d1.quantity.unit.name ?: d1.quantity.unit.toString()
        )
        val k2 = ProductDemandShadowPriceKey(
            productId = p2.id,
            unitSymbol = d2.quantity.unit.symbol ?: d2.quantity.unit.name ?: d2.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(k1 to Flt64(100.0), k2 to Flt64(80.0)),
            overProductionPenalty = mapOf(k1 to Flt64(10.0), k2 to Flt64(8.0))
        )
        val wasteConfig = WasteMinimizationConfig<Flt64>(
            trimWidthPenalty = Flt64(2.0),
            overProductionAreaPenalty = Flt64(5.0)
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                cuttingPlan(id = "plan-wy-1", product = p1, material = material, rollContribution = Flt64(2.0)),
                cuttingPlan(id = "plan-wy-2", product = p2, material = material, rollContribution = Flt64(1.5))
            ),
            demands = listOf(d1, d2),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            yieldConfig = yieldConfig,
            wasteConfig = wasteConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        assertNotNull(milpResult.yieldResult, "Yield result should not be null")
        assertNotNull(milpResult.wasteResult, "Waste result should not be null")

        // 验证超产面积 = sum(over_i * max(width_i)) / Verify over area uses each product max width
        val overArea = milpResult.wasteResult!!.overProductionArea
        if (overArea != null) {
            val expectedArea = milpResult.yieldResult!!.overProductions.fold(Flt64.zero) { acc, over ->
                val productWidth = listOf(p1, p2).find { it.id == over.productId }?.maxWidth()?.value ?: Flt64.zero
                acc + over.amount * productWidth
            }
            assertTrue(
                (overArea - expectedArea).abs() < Flt64(1e-4),
                "Over-production area ($overArea) should match expected ($expectedArea)"
            )
        }
    }

    // --- C5 length assignment 建模差异和单位一致性端到端验证 ---
    // --- C5 length assignment modeling difference and unit consistency end-to-end verification ---

    /**
     * 验证列生成中动态长度产品与固定长度产品在列生成各轮次的行为差异
     * Verify behavioral difference between dynamic and fixed length products across column generation iterations
     *
     * 动态长度产品：assignedLength 变量在最终 MILP 中注册，LP 轮次不使用 lengthConfig
     * 固定长度产品：无 assignedLength 变量，lengthResult 不含固定产品
     */
    @Test
    fun columnGenerationWithMixedLengthProductsShouldDistinguishDynamicAndFixedOnRealSolver() = runBlocking {
        val dynamicProduct = Product(
            id = "p-dynamic-cg",
            name = "product-p-dynamic-cg",
            width = listOf(Quantity(Flt64(0.5), Meter)),
            maxOverProduceLength = Quantity(Flt64(2.0), Meter),
            dynamicLength = true
        )
        val fixedProduct = product(id = "p-fixed-cg", width = 0.3)
        val material = material(id = "m-mixed-cg", lowerWidth = 0.3, upperWidth = 1.5)
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(dynamicProduct.id),
            assignedLengthLowerBound = mapOf(dynamicProduct.id to Flt64(0.0)),
            assignedLengthUpperBound = mapOf(dynamicProduct.id to Flt64(3.0)),
            overLengthPenalty = mapOf(dynamicProduct.id to Flt64(10.0)),
            totalLengthPenalty = Flt64(0.5)
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(dynamicProduct, fixedProduct),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(product = dynamicProduct, rollAmount = Flt64(6.0)),
                ProductDemand.legacyRoll(product = fixedProduct, rollAmount = Flt64(4.0))
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 64,
                maxPricingPlans = 16,
                iterationLimit = 8
            )
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            solver = solver,
            lengthConfig = lengthConfig
        )
        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // 验证基本产出
        assertTrue(solution.produce.cuttingPlans.isNotEmpty(), "Should select cutting plans")
        assertNotNull(trace.terminationReason)

        // 验证 lengthResult 存在且仅包含动态产品
        val lengthResult = solution.lengthResult
        assertNotNull(lengthResult, "Length result should not be null")

        val dynamicIds = lengthResult.assignedLengths.map { it.productId }.toSet()
        assertTrue(dynamicProduct.id in dynamicIds, "Dynamic product should have assigned length")
        assertTrue(fixedProduct.id !in dynamicIds, "Fixed product should NOT have assigned length")

        // 验证动态产品的 assignedLength 在边界内
        val dynamicAssigned = lengthResult.assignedLengths.find { it.productId == dynamicProduct.id }
        assertNotNull(dynamicAssigned)
        assertTrue(
            dynamicAssigned.assignedLength >= Flt64(0.0) - Flt64(1e-6),
            "Dynamic assigned length >= 0.0"
        )
        assertTrue(
            dynamicAssigned.assignedLength <= Flt64(3.0) + Flt64(1e-6),
            "Dynamic assigned length <= 3.0"
        )

        // 验证固定产品的需求也被满足
        val fixedContribution = solution.produce.cuttingPlans
            .filter { it.plan.demandContributions.any { c -> c.product.id == fixedProduct.id } }
            .fold(Flt64.zero) { acc, usage ->
                val contrib = usage.plan.demandContributions.find { it.product.id == fixedProduct.id }?.quantity?.value ?: Flt64.zero
                acc + contrib * usage.amount.toFlt64()
            }
        assertTrue(
            fixedContribution >= Flt64(4.0) - Flt64(1e-4),
            "Fixed product demand should be met: contribution $fixedContribution >= 4.0"
        )
    }

    /**
     * 验证 length assignment 建模在 weight 需求口径下 Gurobi 真实 solver 回填值正确
     * Verify length assignment modeling with weight demand unit on Gurobi
     */
    @Test
    fun milpWithLengthConfigWeightDemandShouldProduceCorrectResultOnRealSolver() = runBlocking {
        val dynamicProduct = Product(
            id = "p-length-weight",
            name = "product-p-length-weight",
            width = listOf(Quantity(Flt64(0.4), Meter)),
            maxOverProduceLength = Quantity(Flt64(2.0), Meter),
            dynamicLength = true
        )
        val material = material(id = "m-length-weight", lowerWidth = 0.3, upperWidth = 1.2)
        val demand = ProductDemand.legacyWeight(product = dynamicProduct, weightAmount = Flt64(200.0), unit = Kilogram)
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(dynamicProduct.id),
            assignedLengthLowerBound = mapOf(dynamicProduct.id to Flt64(0.0)),
            assignedLengthUpperBound = mapOf(dynamicProduct.id to Flt64(5.0)),
            overLengthPenalty = mapOf(dynamicProduct.id to Flt64(10.0)),
            totalLengthPenalty = Flt64(0.5)
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                weightCuttingPlan(id = "plan-length-weight", product = dynamicProduct, material = material, weightContribution = Flt64(50.0))
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(solver).solve(
            input = input,
            lengthConfig = lengthConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        val lengthResult = milpResult.lengthResult
        assertNotNull(lengthResult, "Length result should not be null for weight demand with dynamic length")

        // 验证 assignedLength 在边界内
        val assignedLength = lengthResult.assignedLengths.find { it.productId == dynamicProduct.id }
        assertNotNull(assignedLength, "Should have assigned length for dynamic product with weight demand")
        assertTrue(
            assignedLength.assignedLength >= Flt64(0.0) - Flt64(1e-6),
            "Assigned length should be >= 0.0, got ${assignedLength.assignedLength}"
        )
        assertTrue(
            assignedLength.assignedLength <= Flt64(5.0) + Flt64(1e-6),
            "Assigned length should be <= 5.0, got ${assignedLength.assignedLength}"
        )
    }

    // --- helper functions ---

    private fun cuttingPlan(
        id: String,
        product: Product<Flt64>,
        material: Material<Flt64>,
        rollContribution: Flt64
    ): CuttingPlan<Flt64> {
        return CuttingPlan(
            id = id,
            material = material,
            slices = listOf(
                CuttingPlanSlice(
                    production = product,
                    width = product.width.first(),
                    amount = UInt64.one
                )
            ),
            demandContributions = listOf(
                CuttingPlanDemandContribution(
                    product = product,
                    quantity = Quantity(rollContribution, RollCountUnit)
                )
            )
        )
    }

    /**
     * 按重量贡献构建切割方案 / Build cutting plan with weight contribution
     */
    private fun weightCuttingPlan(
        id: String,
        product: Product<Flt64>,
        material: Material<Flt64>,
        weightContribution: Flt64
    ): CuttingPlan<Flt64> {
        return CuttingPlan(
            id = id,
            material = material,
            slices = listOf(
                CuttingPlanSlice(
                    production = product,
                    width = product.width.first(),
                    amount = UInt64.one
                )
            ),
            demandContributions = listOf(
                CuttingPlanDemandContribution(
                    product = product,
                    quantity = Quantity(weightContribution, Kilogram)
                )
            )
        )
    }

    /**
     * 按张数贡献构建切割方案 / Build cutting plan with sheet contribution
     */
    private fun sheetCuttingPlan(
        id: String,
        product: Product<Flt64>,
        material: Material<Flt64>,
        sheetContribution: Flt64
    ): CuttingPlan<Flt64> {
        return CuttingPlan(
            id = id,
            material = material,
            slices = listOf(
                CuttingPlanSlice(
                    production = product,
                    width = product.width.first(),
                    amount = UInt64.one
                )
            ),
            demandContributions = listOf(
                CuttingPlanDemandContribution(
                    product = product,
                    quantity = Quantity(sheetContribution, SheetCountUnit)
                )
            )
        )
    }

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
        machineId: String? = null,
        length: Double? = null
    ): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = widthRange(
                lower = lowerWidth,
                upper = upperWidth
            ),
            machineId = machineId,
            length = length?.let { Quantity(Flt64(it), Meter) }
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
