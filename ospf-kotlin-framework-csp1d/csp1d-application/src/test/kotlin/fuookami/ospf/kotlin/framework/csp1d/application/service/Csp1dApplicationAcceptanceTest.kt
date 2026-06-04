package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.solver.Flt64FeasibleSolverOutput
import fuookami.ospf.kotlin.framework.solver.Flt64LinearMetaModel
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.RollCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.SheetCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dConfiguration
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem

/**
 * CSP1D 验收测试使用的 fake solver / Fake solver for CSP1D acceptance tests
 *
 * 按 tokensInSolver.size 生成全 1.0 解向量，使 MILP 求解能产生非空 Produce。
 * Generate an all-1.0 solution vector by tokensInSolver.size so the MILP path can produce non-empty output.
 */
private class Csp1dFakeSolver : ColumnGenerationSolver {
    override val name: String = "csp1d-fake"

    override suspend fun solveMILP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Flt64FeasibleSolverOutput> {
        val size = metaModel.tokens.tokensInSolver.size
        val solution: Solution<Flt64> = (0 until size).map { Flt64(1.0) }
        return Ok(
            FeasibleSolverOutput(
                obj = Flt64.zero,
                solution = solution,
                time = Duration.ZERO,
                possibleBestObj = Flt64.zero,
                gap = Flt64.zero
            )
        )
    }

    override suspend fun solveLP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        val size = metaModel.tokens.tokensInSolver.size
        val solution: Solution<Flt64> = (0 until size).map { Flt64(1.0) }
        return Ok(
            ColumnGenerationSolver.LPResult(
                result = FeasibleSolverOutput(
                    obj = Flt64.zero,
                    solution = solution,
                    time = Duration.ZERO,
                    possibleBestObj = Flt64.zero,
                    gap = Flt64.zero
                ),
                dualSolution = emptyMap()
            )
        )
    }
}

class Csp1dApplicationAcceptanceTest {
    private val fakeSolver = Csp1dFakeSolver()

    @Test
    fun milpShouldSolveRollDemandWithoutPoitDependency() = runBlocking {
        val product = product(
            id = "p-roll",
            width = 1.2
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(
                material(
                    id = "m-roll",
                    lowerWidth = 0.8,
                    upperWidth = 2.0,
                    machineId = "machine-roll"
                )
            ),
            machines = listOf(
                machine(
                    id = "machine-roll",
                    capacity = 800.0
                )
            ),
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

        val solution = Csp1dMilp<Flt64>(fakeSolver).solve(problem)

        assertTrue(solution.produce.cuttingPlans.isNotEmpty())
        assertTrue(solution.produce.unmetDemands.isEmpty())
        assertEquals("p-roll", solution.produce.cuttingPlans.first().plan.demandContributions.first().product.id)
    }

    @Test
    fun milpShouldCoverCostarRestWidthAndMachineCapacity() = runBlocking {
        val product = product(
            id = "p-capacity",
            width = 1.3
        )
        val material = material(
            id = "m-capacity",
            lowerWidth = 1.0,
            upperWidth = 2.0,
            machineId = "machine-capacity"
        )
        val machine = machine(
            id = "machine-capacity",
            capacity = 500.0
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = listOf(machine),
            costars = listOf(
                Costar(
                    id = "c-side",
                    name = "side-coproduct",
                    width = listOf(
                        Quantity(Flt64(0.2), Meter)
                    ),
                    length = Quantity(Flt64(100.0), Meter)
                )
            ),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64(4.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 32,
                maxPricingPlans = 8,
                iterationLimit = 4
            )
        )

        val solution = Csp1dMilp<Flt64>(fakeSolver).solve(problem)
        val selectedPlan = solution.produce.cuttingPlans.first().plan
        val restWidth = selectedPlan.restWidth
        val machineCapacityUsage = solution.produce.machineUsages.firstOrNull {
            it.machine.id == "machine-capacity"
        }?.used

        assertNotNull(restWidth)
        assertTrue(restWidth eq Quantity(Flt64(0.7), Meter))
        assertNotNull(machineCapacityUsage)
        assertTrue(machineCapacityUsage eq Quantity(Flt64(500.0), Kilogram))
    }

    @Test
    fun columnGenerationShouldSolveInitialPlans() = runBlocking {
        val product = product(
            id = "p-cg",
            width = 1.0
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(
                material(
                    id = "m-cg",
                    lowerWidth = 0.8,
                    upperWidth = 1.5
                )
            ),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64(2.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = 8,
                maxPricingPlans = 8,
                iterationLimit = 4
            )
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            solver = fakeSolver
        )

        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // C3 仅求解初始方案池，C4 再接入 shadow price/pricing / C3 only solves initial plans; C4 adds shadow price/pricing
        assertEquals(UInt64(trace.initialPlanCount.toULong()), trace.finalPlanCount)
        assertTrue(trace.pricedPlanCount.isEmpty())
        assertTrue(solution.generatedPlans.isNotEmpty())
        // 验证 trace 新字段 / Verify new trace fields
        assertNotNull(trace.terminationReason)
        assertNotNull(trace.iterations)
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

    /**
     * 验证 yield 建模在 MILP solver 上正确注册欠产/超产松弛变量并影响求解结果 / Verify yield modeling registers under/over production slack variables correctly and affects solve results
     */
    @Test
    fun milpWithYieldConfigShouldProduceYieldResult() = runBlocking {
        val product = product(
            id = "p-yield",
            width = 0.8
        )
        val material = material(
            id = "m-yield",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val demandKey = ProductDemandShadowPriceKey(
            productId = product.id,
            unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.name ?: demand.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(demandKey to Flt64(10.0)),
            overProductionPenalty = mapOf(demandKey to Flt64(5.0))
        )

        val input = ProduceInput(
            cuttingPlans = listOf(
                simpleCuttingPlan(
                    product = product,
                    material = material,
                    rollContribution = Flt64(1.0)
                )
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            yieldConfig = yieldConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        assertNotNull(milpResult.yieldResult, "Yield result should not be null when yieldConfig is provided")
        assertTrue(milpResult.produce.cuttingPlans.isNotEmpty(), "Should have cutting plan usage")
    }

    /**
     * 验证 yieldConfig 为 null 时 MILP solver 无 yield 建模，向后兼容 / Verify no yield modeling when yieldConfig is null, backward compatibility
     */
    @Test
    fun milpWithoutYieldConfigShouldNotProduceYieldResult() = runBlocking {
        val product = product(
            id = "p-no-yield",
            width = 0.8
        )
        val material = material(
            id = "m-no-yield",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )

        val input = ProduceInput(
            cuttingPlans = listOf(
                simpleCuttingPlan(
                    product = product,
                    material = material,
                    rollContribution = Flt64(1.0)
                )
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(fakeSolver).solve(input)

        assertNotNull(milpResult, "MILP result should not be null")
        assertEquals(null, milpResult.yieldResult, "Yield result should be null when yieldConfig is not provided")
    }

    /**
     * 验证 yield 建模按 product + unit 口径区分不同需求单位 / Verify yield modeling distinguishes different demand units by product + unit
     */
    @Test
    fun yieldConfigDistinguishesDifferentDemandUnits() {
        val rollKey = ProductDemandShadowPriceKey(
            productId = "p-multi",
            unitSymbol = RollCountUnit.symbol ?: RollCountUnit.name ?: RollCountUnit.toString()
        )
        val sheetKey = ProductDemandShadowPriceKey(
            productId = "p-multi",
            unitSymbol = SheetCountUnit.symbol ?: SheetCountUnit.name ?: SheetCountUnit.toString()
        )

        // 不同单位的惩罚系数可以不同 / Different penalty coefficients for different units
        val config = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(
                rollKey to Flt64(10.0),
                sheetKey to Flt64(8.0)
            ),
            overProductionPenalty = mapOf(
                rollKey to Flt64(5.0),
                sheetKey to Flt64(3.0)
            )
        )

        assertNotEquals(rollKey, sheetKey, "Roll and sheet keys should be different")
        assertEquals(Flt64(10.0), config.underProductionPenalty[rollKey])
        assertEquals(Flt64(8.0), config.underProductionPenalty[sheetKey])
    }

    /**
     * 验证 wasteConfig 为 null 时 MILP solver 无废弃建模，向后兼容 / Verify no waste modeling when wasteConfig is null, backward compatibility
     */
    @Test
    fun milpWithoutWasteConfigShouldNotProduceWasteResult() = runBlocking {
        val product = product(
            id = "p-no-waste",
            width = 0.8
        )
        val material = material(
            id = "m-no-waste",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )

        val input = ProduceInput(
            cuttingPlans = listOf(
                simpleCuttingPlan(
                    product = product,
                    material = material,
                    rollContribution = Flt64(1.0)
                )
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(fakeSolver).solve(input)

        assertNotNull(milpResult, "MILP result should not be null")
        assertEquals(null, milpResult.wasteResult, "Waste result should be null when wasteConfig is not provided")
    }

    /**
     * 验证 wasteConfig 带 trimWidthPenalty 时 MILP solver 正确提取余宽浪费 / Verify waste modeling extracts trim width when wasteConfig is provided
     */
    @Test
    fun milpWithTrimWidthPenaltyShouldProduceWasteResult() = runBlocking {
        val product = product(
            id = "p-trim",
            width = 0.8
        )
        val material = material(
            id = "m-trim",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )

        val wasteConfig = WasteMinimizationConfig<Flt64>(
            trimWidthPenalty = Flt64(2.0)
        )

        val input = ProduceInput(
            cuttingPlans = listOf(
                simpleCuttingPlan(
                    product = product,
                    material = material,
                    rollContribution = Flt64(1.0)
                )
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            wasteConfig = wasteConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertEquals(
            Flt64(0.7),
            milpResult.wasteResult!!.totalTrimWidth,
            "Total trim width should match rest width times selected batch count"
        )
    }

    /**
     * 验证 wasteConfig 带 materialCostPenalty 时 MILP solver 正确提取物料成本 / Verify waste modeling extracts material costs when materialCostPenalty is provided
     */
    @Test
    fun milpWithMaterialCostPenaltyShouldProduceWasteResult() = runBlocking {
        val product = product(
            id = "p-cost",
            width = 0.8
        )
        val material = material(
            id = "m-cost",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )

        val wasteConfig = WasteMinimizationConfig<Flt64>(
            materialCostPenalty = mapOf("m-cost" to Flt64(5.0))
        )

        val input = ProduceInput(
            cuttingPlans = listOf(
                simpleCuttingPlan(
                    product = product,
                    material = material,
                    rollContribution = Flt64(1.0)
                )
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            wasteConfig = wasteConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertTrue(milpResult.wasteResult!!.materialCosts.isNotEmpty(), "Material costs should not be empty when materialCostPenalty is provided")
        assertEquals("m-cost", milpResult.wasteResult!!.materialCosts.first().materialId)
        assertEquals(Flt64(5.0), milpResult.wasteResult!!.materialCosts.first().cost)
    }

    /**
     * 验证仅配置超产面积惩罚时也会注册超产松弛变量 / Verify over-production slack is registered when only over-production area penalty is configured
     */
    @Test
    fun milpWithOnlyOverProductionAreaPenaltyShouldProduceArea() = runBlocking {
        val product = product(
            id = "p-area-only",
            width = 0.8
        )
        val material = material(
            id = "m-area-only",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val yieldConfig = YieldModelingConfig<Flt64>()
        val wasteConfig = WasteMinimizationConfig<Flt64>(
            overProductionAreaPenalty = Flt64(1.0)
        )

        val input = ProduceInput(
            cuttingPlans = listOf(
                simpleCuttingPlan(
                    product = product,
                    material = material,
                    rollContribution = Flt64(1.0)
                )
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            yieldConfig = yieldConfig,
            wasteConfig = wasteConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertEquals(
            Flt64(0.8),
            milpResult.wasteResult!!.overProductionArea,
            "Over-production area should match over slack times first product width"
        )
    }

    /**
     * 验证仅配置超产上限时也会注册超产松弛变量 / Verify over-production slack is registered when only upper bound is configured
     */
    @Test
    fun milpWithOnlyOverProductionUpperBoundShouldProduceOverYieldResult() = runBlocking {
        val product = product(
            id = "p-over-bound",
            width = 0.8
        )
        val material = material(
            id = "m-over-bound",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val demandKey = ProductDemandShadowPriceKey(
            productId = product.id,
            unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.name ?: demand.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            overProductionUpperBound = mapOf(demandKey to Flt64(2.0))
        )

        val input = ProduceInput(
            cuttingPlans = listOf(
                simpleCuttingPlan(
                    product = product,
                    material = material,
                    rollContribution = Flt64(1.0)
                )
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            yieldConfig = yieldConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        assertNotNull(milpResult.yieldResult, "Yield result should not be null when yieldConfig is provided")
        assertEquals(Flt64.one, milpResult.yieldResult!!.overProductions.first().amount)
    }

    /**
     * 验证 yield 和 waste 联合建模时 MILP solver 正确提取两个结果 / Verify both yield and waste results are extracted when both configs are provided
     */
    @Test
    fun milpWithYieldAndWasteConfigShouldProduceBothResults() = runBlocking {
        val product = product(
            id = "p-both",
            width = 0.8
        )
        val material = material(
            id = "m-both",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val demandKey = ProductDemandShadowPriceKey(
            productId = product.id,
            unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.name ?: demand.quantity.unit.toString()
        )
        val yieldConfig = YieldModelingConfig<Flt64>(
            underProductionPenalty = mapOf(demandKey to Flt64(10.0)),
            overProductionPenalty = mapOf(demandKey to Flt64(5.0))
        )
        val wasteConfig = WasteMinimizationConfig<Flt64>(
            trimWidthPenalty = Flt64(2.0),
            overProductionAreaPenalty = Flt64(1.0)
        )

        val input = ProduceInput(
            cuttingPlans = listOf(
                simpleCuttingPlan(
                    product = product,
                    material = material,
                    rollContribution = Flt64(1.0)
                )
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = emptyList()
        )

        val milpResult = Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            yieldConfig = yieldConfig,
            wasteConfig = wasteConfig
        )

        assertNotNull(milpResult, "MILP result should not be null")
        assertNotNull(milpResult.yieldResult, "Yield result should not be null when yieldConfig is provided")
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertEquals(Flt64(0.7), milpResult.wasteResult!!.totalTrimWidth)
        assertEquals(Flt64(0.8), milpResult.wasteResult!!.overProductionArea)
    }

    private fun simpleCuttingPlan(
        product: Product<Flt64>,
        material: Material<Flt64>,
        rollContribution: Flt64
    ): CuttingPlan<Flt64> {
        return CuttingPlan(
            id = "plan-${product.id}-${material.id}",
            material = material,
            slices = listOf(
                CuttingPlanSlice(
                    production = product,
                    amount = UInt64.one,
                    width = product.width.first()
                )
            ),
            demandContributions = listOf(
                CuttingPlanDemandContribution(
                    product = product,
                    quantity = Quantity(rollContribution, RollCountUnit)
                )
            ),
            machineId = null
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
