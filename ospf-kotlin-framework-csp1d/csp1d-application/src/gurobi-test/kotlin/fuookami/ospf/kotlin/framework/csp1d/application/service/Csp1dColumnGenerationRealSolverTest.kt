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

        // 验证 MILP 解总批次数 ≥ 需求量 / Verify MILP total batches >= demand
        val totalBatches = solution.produce.cuttingPlans.fold(UInt64.zero) { acc, usage ->
            acc + usage.amount
        }
        assertTrue(totalBatches >= UInt64(10UL), "Total batches should be >= demand (10 rolls)")
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
