package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.*
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearInequalityConstraint
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.framework.csp1d.application.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceInput
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.solver.*

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
        return Ok(fakeFeasibleOutput(metaModel))
    }

    override suspend fun solveLP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        return Ok(
            ColumnGenerationSolver.LPResult(
                result = fakeFeasibleOutput(metaModel),
                dualSolution = emptyMap()
            )
        )
    }
}

private class Csp1dFailingMilpSolver : ColumnGenerationSolver {
    override val name: String = "csp1d-failing-milp"

    override suspend fun solveMILP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Flt64FeasibleSolverOutput> {
        throw IllegalStateException("forced final MILP failure")
    }

    override suspend fun solveLP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        return Ok(
            ColumnGenerationSolver.LPResult(
                result = fakeFeasibleOutput(metaModel),
                dualSolution = emptyMap()
            )
        )
    }
}

/**
 * LP 求解失败但 MILP 成功的 fake solver / Fake solver that fails LP but succeeds MILP
 */
private class Csp1dFailingLpSolver : ColumnGenerationSolver {
    override val name: String = "csp1d-failing-lp"

    override suspend fun solveMILP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Flt64FeasibleSolverOutput> {
        return Ok(fakeFeasibleOutput(metaModel))
    }

    override suspend fun solveLP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        throw IllegalStateException("forced LP failure")
    }
}

private class Csp1dInitialResultCapturingSolver : ColumnGenerationSolver {
    override val name: String = "csp1d-initial-result-capturing"
    var lastInitialResults: Map<String, Flt64> = emptyMap()

    override suspend fun solveMILP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Flt64FeasibleSolverOutput> {
        lastInitialResults = metaModel.tokens.tokensInSolver.mapNotNull { token ->
            token.resultFlt64?.let { value -> token.name to value }
        }.toMap()
        return Ok(fakeFeasibleOutput(metaModel))
    }

    override suspend fun solveLP(
        name: String,
        metaModel: Flt64LinearMetaModel,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
        return Ok(
            ColumnGenerationSolver.LPResult(
                result = fakeFeasibleOutput(metaModel),
                dualSolution = emptyMap()
            )
        )
    }
}

private fun fakeFeasibleOutput(metaModel: Flt64LinearMetaModel): Flt64FeasibleSolverOutput {
    val size = metaModel.tokens.tokensInSolver.size
    val solution: Solution<Flt64> = (0 until size).map { Flt64(1.0) }
    return FeasibleSolverOutput(
        obj = Flt64.zero,
        solution = solution,
        time = Duration.ZERO,
        possibleBestObj = Flt64.zero,
        gap = Flt64.zero
    )
}

private class CapturingPricingGenerator : Csp1dPricingGenerator<Flt64> {
    var lastInput: Csp1dPricingInput<Flt64>? = null

    override fun generate(input: Csp1dPricingInput<Flt64>): List<CuttingPlan<Flt64>> {
        lastInput = input
        return emptyList()
    }
}



/**
 * 解包 Ret<MilpResult<V>?> 结果，测试中使用
 */
private fun <V : RealNumber<V>> unwrapMilpResult(result: Ret<Csp1dMilpSolver.MilpResult<V>?>): Csp1dMilpSolver.MilpResult<V> {
    return when (result) {
        is Ok -> result.value ?: throw AssertionError("MILP result should not be null")
        is Failed -> throw AssertionError("MILP solve failed: ${result.error}")
        is Fatal -> throw AssertionError("MILP solve fatal: ${result.errors}")
    }
}
class Csp1dApplicationAcceptanceTest {
    private val fakeSolver = Csp1dFakeSolver()

    @Test
    fun milpShouldSolveRollDemandWithoutDownstreamDependency(): Unit = runBlocking {
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
                maxInitialPlans = Int64(32),
                maxPricingPlans = Int64(8),
                iterationLimit = Int64(4)
            )
        )

        val solution = Csp1dMilp<Flt64>(fakeSolver).solve(problem)

        assertTrue(solution.produce.cuttingPlans.isNotEmpty())
        assertTrue(solution.produce.unmetDemands.isEmpty())
        assertEquals("p-roll", solution.produce.cuttingPlans.first().plan.demandContributions.first().product.id)
    }

    /**
     * 验证普通 MILP 入口读取 problem.solveConfig 并回填增强 KPI / Verify plain MILP reads problem.solveConfig and fills enhanced KPI
     */
    @Test
    fun milpShouldReadProblemSolveConfigForLengthAndTopK(): Unit = runBlocking {
        val product = dynamicProduct(
            id = "p-milp-config",
            width = 0.8
        )
        val material = material(
            id = "m-milp-config",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(product.id),
            overLengthPenalty = mapOf(product.id to Flt64(2.0))
        )
        val problem = csp1dProblem<Flt64> {
            products(listOf(product))
            material(material)
            demands(
                listOf(
                    ProductDemand.legacyRoll(
                        product = product,
                        rollAmount = Flt64(2.0)
                    )
                )
            )
            configuration(
                Csp1dConfiguration(
                    maxInitialPlans = Int64(1),
                    maxPricingPlans = Int64(1),
                    iterationLimit = Int64(1)
                )
            )
            solveConfig {
                columnGeneration(
                    maxInitialPlans = Int64(8),
                    maxPricingPlans = Int64(1),
                    iterationLimit = Int64(1)
                )
                lengthConfig(lengthConfig)
                topKPlanLimit(Int64(1))
            }
        }

        val solution = Csp1dMilp<Flt64>(fakeSolver).solve(problem)

        assertEquals(Csp1dSolutionStatus.Feasible, solution.status)
        assertNotNull(solution.lengthResult, "Length result should be filled from problem.solveConfig")
        assertEquals(UInt64.one, solution.kpi.topPlanCount)
        assertEquals(UInt64(2), solution.kpi.lengthMetricCount)
        assertEquals("Feasible", solution.render.kpi[Csp1dKpiKeys.SolutionStatus])
        assertEquals("1", solution.render.kpi[Csp1dKpiKeys.TopPlanCount])
        assertEquals("2", solution.render.kpi[Csp1dKpiKeys.LengthMetricCount])
    }

    @Test
    fun milpShouldCoverCostarRestWidthWithoutMisreportingMachineCapacity(): Unit = runBlocking {
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
                maxInitialPlans = Int64(32),
                maxPricingPlans = Int64(8),
                iterationLimit = Int64(4)
            )
        )

        val solution = Csp1dMilp<Flt64>(fakeSolver).solve(problem)
        val selectedPlan = solution.produce.cuttingPlans.first().plan
        val restWidth = selectedPlan.restWidth

        assertNotNull(restWidth)
        assertTrue(restWidth eq Quantity(Flt64(0.7), Meter))
        assertTrue(
            solution.produce.machineUsages.isEmpty(),
            "Machine capacity usage should not be reported without plan capacity consumption"
        )
    }

    /**
     * 验证设备批次数和业务产能约束可同时建模，并按解回填实际产能使用量 /
     * Verify machine batch and business capacity constraints can coexist and actual capacity usage is extracted
     */
    @Test
    fun milpWithMachineCapacityConsumptionShouldConstrainAndBackfillUsage(): Unit = runBlocking {
        val product = product(
            id = "p-machine-capacity",
            width = 0.8
        )
        val material = material(
            id = "m-machine-capacity",
            lowerWidth = 0.5,
            upperWidth = 1.5,
            machineId = "machine-capacity-modeled"
        )
        val machine = machine(
            id = "machine-capacity-modeled",
            capacity = 120.0,
            maxBatchCount = UInt64(3UL)
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(1.0)
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                simpleCuttingPlan(
                    product = product,
                    material = material,
                    rollContribution = Flt64(1.0),
                    machineId = machine.id,
                    capacityConsumption = 80.0
                )
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = listOf(machine)
        )

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(input))
        val machineCapacityUsage = milpResult.produce.machineUsages.firstOrNull {
            it.machine.id == machine.id
        }?.used
        assertNotNull(machineCapacityUsage, "Machine capacity usage should be extracted")
        assertTrue(machineCapacityUsage eq Quantity(Flt64(80.0), Kilogram))

        @Suppress("UNCHECKED_CAST")
        val constraintNames = milpResult.model.constraints.mapNotNull { constraint ->
            (constraint as? LinearInequalityConstraint<Flt64>)?.name
        }.toSet()
        assertTrue("machine_batch_0" in constraintNames)
        assertTrue("machine_capacity_0" in constraintNames)
    }

    /**
     * 验证缺少方案产能消耗时不添加设备业务产能约束，也不误报使用量 /
     * Verify machine business capacity is not constrained or reported without plan capacity consumption
     */
    @Test
    fun milpWithMachineCapacityButNoPlanConsumptionShouldSkipCapacityUsage(): Unit = runBlocking {
        val product = product(
            id = "p-machine-no-consumption",
            width = 0.8
        )
        val material = material(
            id = "m-machine-no-consumption",
            lowerWidth = 0.5,
            upperWidth = 1.5,
            machineId = "machine-no-consumption"
        )
        val machine = machine(
            id = "machine-no-consumption",
            capacity = 120.0
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(1.0)
        )
        val input = ProduceInput(
            cuttingPlans = listOf(
                simpleCuttingPlan(
                    product = product,
                    material = material,
                    rollContribution = Flt64(1.0),
                    machineId = machine.id
                )
            ),
            demands = listOf(demand),
            materials = listOf(material),
            machines = listOf(machine)
        )

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(input))
        assertTrue(
            milpResult.produce.machineUsages.isEmpty(),
            "Machine capacity usage should be empty when no plan consumption is provided"
        )

        @Suppress("UNCHECKED_CAST")
        val constraintNames = milpResult.model.constraints.mapNotNull { constraint ->
            (constraint as? LinearInequalityConstraint<Flt64>)?.name
        }.toSet()
        assertTrue("machine_capacity_0" !in constraintNames)
    }

    /**
     * 验证方案产能消耗与设备产能单位不一致时不添加约束，也不回填使用量 /
     * Verify capacity consumption with mismatched unit skips constraint and usage extraction
     */
    @Test
    fun milpWithMismatchedMachineCapacityConsumptionUnitShouldSkipCapacityUsage(): Unit = runBlocking {
        val product = product(
            id = "p-machine-capacity-unit",
            width = 0.8
        )
        val material = material(
            id = "m-machine-capacity-unit",
            lowerWidth = 0.5,
            upperWidth = 1.5,
            machineId = "machine-capacity-unit"
        )
        val machine = machine(
            id = "machine-capacity-unit",
            capacity = 120.0
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(1.0)
        )
        val plan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64(1.0),
            machineId = machine.id
        ).copy(
            capacityConsumption = Quantity(Flt64(80.0), Meter)
        )
        val input = ProduceInput(
            cuttingPlans = listOf(plan),
            demands = listOf(demand),
            materials = listOf(material),
            machines = listOf(machine)
        )

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(input))
        assertTrue(
            milpResult.produce.machineUsages.isEmpty(),
            "Machine capacity usage should be empty when consumption unit mismatches capacity unit"
        )

        @Suppress("UNCHECKED_CAST")
        val constraintNames = milpResult.model.constraints.mapNotNull { constraint ->
            (constraint as? LinearInequalityConstraint<Flt64>)?.name
        }.toSet()
        assertTrue("machine_capacity_0" !in constraintNames)
    }

    @Test
    fun columnGenerationShouldSolveInitialPlans(): Unit = runBlocking {
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
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(8),
                iterationLimit = Int64(4)
            )
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            solver = fakeSolver
        )

        val (solution, trace) = columnGeneration.solveWithTrace(problem)

        // C3 仅求解初始方案池，C4 再接入 shadow price/pricing / C3 only solves initial plans; C4 adds shadow price/pricing
        assertEquals(UInt64(trace.initialPlanCount.toULong()), trace.finalPlanCount)
        // pricedPlanCount 与 iterations 一一对应，收敛轮次记录 0 / pricedPlanCount maps 1:1 to iterations; convergence iteration records 0
        assertTrue(trace.pricedPlanCount.all { it == UInt64.zero })
        assertTrue(solution.generatedPlans.isNotEmpty())
        // 验证 trace 新字段 / Verify new trace fields
        assertNotNull(trace.terminationReason)
        assertNotNull(trace.iterations)
    }

    /**
     * 验证列生成入口透传 lengthConfig 并回填 lengthResult / Verify column generation passes lengthConfig through and fills lengthResult
     */
    @Test
    fun columnGenerationShouldPassLengthConfigToFinalMilp(): Unit = runBlocking {
        val product = dynamicProduct(
            id = "p-cg-length",
            width = 1.0
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(
                material(
                    id = "m-cg-length",
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
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(8),
                iterationLimit = Int64(4)
            )
        )
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(product.id),
            overLengthPenalty = mapOf(product.id to Flt64(3.0))
        )
        val columnGeneration = Csp1dColumnGeneration(
            solver = fakeSolver,
            lengthConfig = lengthConfig
        )

        val result = columnGeneration.solveWithTrace(problem)

        assertNotNull(result.solution.lengthResult, "Length result should be filled when lengthConfig is provided")
        assertEquals(product.id, result.solution.lengthResult!!.overLengths.first().productId)
        assertEquals(Flt64.one, result.solution.lengthResult!!.overLengths.first().overLength)
    }

    /**
     * 验证列生成入口向 pricing 透传方案级目标提示 / Verify column generation passes plan-level objective hints to pricing
     */
    @Test
    fun columnGenerationShouldPassObjectiveHintsToPricing(): Unit = runBlocking {
        val product = product(
            id = "p-cg-pricing-objective",
            width = 1.0
        )
        val material = material(
            id = "m-cg-pricing-objective",
            lowerWidth = 0.8,
            upperWidth = 1.5,
            length = 100.0
        )
        val initialPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        )
        val pricingGenerator = CapturingPricingGenerator()
        val wasteConfig = WasteMinimizationConfig<Flt64>(
            trimWidthPenalty = Flt64(0.2),
            restMaterialPenalty = Flt64(0.3),
            materialCostPenalty = mapOf(material.id to Flt64(0.4))
        )
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            batchMinPenalty = Flt64(0.5)
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64(2.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(8),
                iterationLimit = Int64(1)
            )
        )
        val columnGeneration = Csp1dColumnGeneration(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator { listOf(initialPlan) },
            pricingGenerator = pricingGenerator,
            wasteConfig = wasteConfig,
            lengthConfig = lengthConfig
        )

        columnGeneration.solveWithTrace(problem)

        val pricingInput = assertNotNull(
            pricingGenerator.lastInput,
            "Pricing input should be captured"
        )
        val objectiveConfig = pricingInput.objectiveConfig
        assertEquals(Flt64(0.5), objectiveConfig.planUsagePenalty)
        assertEquals(Flt64(0.2), objectiveConfig.trimWidthPenalty)
        assertEquals(Flt64(0.3), objectiveConfig.restMaterialPenalty)
        assertEquals(Flt64(0.4), objectiveConfig.materialCostPenalty[material.id])
    }

    /**
     * 验证列生成按 canonical key 过滤不同 ID 的重复列 / Verify column generation filters duplicate columns by canonical key even with different IDs
     */
    @Test
    fun columnGenerationShouldFilterDuplicatePricingPlansByCanonicalKey(): Unit = runBlocking {
        val product = product(
            id = "p-cg-duplicate",
            width = 1.0
        )
        val material = material(
            id = "m-cg-duplicate",
            lowerWidth = 0.8,
            upperWidth = 1.5
        )
        val initialPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        )
        val pricingGenerator = Csp1dPricingGenerator<Flt64> {
            listOf(initialPlan.copy(id = "pricing-duplicate"))
        }
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64(2.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(8),
                iterationLimit = Int64(1)
            )
        )
        val columnGeneration = Csp1dColumnGeneration(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator { listOf(initialPlan) },
            pricingGenerator = pricingGenerator
        )

        val result = columnGeneration.solveWithTrace(problem)

        assertEquals(Csp1dTerminationReason.AllDuplicates, result.trace.terminationReason)
        assertEquals(UInt64.one, result.trace.initialPlanCount)
        assertEquals(UInt64.one, result.trace.finalPlanCount)
        val pricingStatistics = assertNotNull(result.trace.pricingGenerationStatistics)
        assertEquals(Int64.one, pricingStatistics.generatedCandidates)
        assertEquals(Int64.one, pricingStatistics.acceptedPlans)
    }

    /**
     * 验证初始方案池按 canonical key 去重并应用配置上限 / Verify initial plans are deduplicated by canonical key and capped by configuration
     */
    @Test
    fun columnGenerationShouldCapInitialPlansAfterCanonicalDeduplication(): Unit = runBlocking {
        val p1 = product(
            id = "p-cg-initial-1",
            width = 0.8
        )
        val p2 = product(
            id = "p-cg-initial-2",
            width = 1.0
        )
        val material = material(
            id = "m-cg-initial-cap",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val firstPlan = simpleCuttingPlan(
            product = p1,
            material = material,
            rollContribution = Flt64.one
        )
        val secondPlan = simpleCuttingPlan(
            product = p2,
            material = material,
            rollContribution = Flt64.one
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(p1, p2),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = p1,
                    rollAmount = Flt64.one
                ),
                ProductDemand.legacyRoll(
                    product = p2,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(1),
                maxPricingPlans = Int64(8),
                iterationLimit = Int64(1)
            )
        )
        val columnGeneration = Csp1dColumnGeneration(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator<Flt64> {
                listOf(
                    firstPlan,
                    firstPlan.copy(id = "initial-duplicate"),
                    secondPlan
                )
            },
            pricingGenerator = Csp1dPricingGenerator<Flt64> { emptyList() }
        )

        val result = columnGeneration.solveWithTrace(problem)

        assertEquals(UInt64.one, result.trace.initialPlanCount)
        assertEquals(UInt64.one, result.trace.finalPlanCount)
        assertEquals(Csp1dTerminationReason.PricingConverged, result.trace.terminationReason)
        val statistics = result.trace.initialGenerationStatistics
        assertNotNull(statistics)
        assertEquals(Int64(3), statistics.generatedCandidates)
        assertEquals(Int64(3), statistics.acceptedPlans)
    }

    /**
     * 验证显式 solveConfig 覆盖 problem.configuration 并回填 Top-K 与 trace 状态 / Verify explicit solveConfig overrides problem.configuration and fills Top-K plus trace status
     */
    @Test
    fun columnGenerationShouldUseExplicitSolveConfigForLimitsAndTopK(): Unit = runBlocking {
        val p1 = product(
            id = "p-cg-config-1",
            width = 0.8
        )
        val p2 = product(
            id = "p-cg-config-2",
            width = 1.0
        )
        val material = material(
            id = "m-cg-config",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val firstPlan = simpleCuttingPlan(
            product = p1,
            material = material,
            rollContribution = Flt64.one
        )
        val secondPlan = simpleCuttingPlan(
            product = p2,
            material = material,
            rollContribution = Flt64.one
        )
        val pricingGenerator = CapturingPricingGenerator()
        val problem = Csp1dProblem<Flt64>(
            products = listOf(p1, p2),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = p1,
                    rollAmount = Flt64.one
                ),
                ProductDemand.legacyRoll(
                    product = p2,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(1),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val solveConfig = Csp1dSolveConfig<Flt64>(
            columnGeneration = Csp1dConfiguration(
                maxInitialPlans = Int64(2),
                maxPricingPlans = Int64(3),
                iterationLimit = Int64(1)
            ),
            topKPlanLimit = Int64(1)
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator<Flt64> {
                listOf(
                    firstPlan,
                    secondPlan
                )
            },
            pricingGenerator = pricingGenerator
        )

        val result = columnGeneration.solveWithTrace(
            problem = problem,
            solveConfig = solveConfig
        )

        assertEquals(UInt64(2), result.trace.initialPlanCount)
        assertEquals(UInt64(2), result.trace.finalPlanCount)
        val pricingInput = assertNotNull(
            pricingGenerator.lastInput,
            "Pricing input should be captured"
        )
        assertEquals(UInt64(3), pricingInput.maxGeneratedPlans)
        assertEquals(Csp1dFinalMilpStatus.Solved, result.trace.finalMilpStatus)
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
        assertEquals(1, result.solution.topPlans.size)
        assertEquals(UInt64.one, result.solution.kpi.topPlanCount)
        assertEquals("Feasible", result.solution.render.kpi[Csp1dKpiKeys.SolutionStatus])
        assertEquals("Solved", result.solution.render.kpi[Csp1dKpiKeys.FinalMilpStatus])
        assertEquals("1", result.solution.render.kpi[Csp1dKpiKeys.TopPlanCount])
        assertEquals("2", result.solution.render.kpi[Csp1dKpiKeys.InitialGeneratedCandidates])
        assertEquals("2", result.solution.render.kpi[Csp1dKpiKeys.InitialAcceptedPlans])
        assertEquals("0", result.solution.render.kpi[Csp1dKpiKeys.InitialKnifeBoundPrunedNodes])
        assertEquals("0", result.solution.render.kpi[Csp1dKpiKeys.InitialLengthBoundPrunedEntries])
        assertEquals("0", result.solution.render.kpi[Csp1dKpiKeys.InitialMaterialWidthIndexCacheHits])
        assertEquals("0", result.solution.render.kpi[Csp1dKpiKeys.InitialMaterialSliceTemplateCacheHits])
        assertEquals("Exhausted", result.solution.render.kpi[Csp1dKpiKeys.InitialGenerationStopReasonRender])
        assertEquals("1", result.solution.kpi.details[Csp1dKpiKeys.ColumnGenerationIterationCount])
        assertEquals("0", result.solution.kpi.details[Csp1dKpiKeys.ColumnGenerationPricedPlanCount])
        assertEquals("0", result.solution.kpi.details[Csp1dKpiKeys.InitialGenerationKnifeBoundPrunedNodes])
        assertEquals("0", result.solution.kpi.details[Csp1dKpiKeys.InitialGenerationLengthBoundPrunedEntries])
        assertEquals("0", result.solution.kpi.details[Csp1dKpiKeys.InitialGenerationMaterialWidthIndexCacheHits])
        assertEquals("0", result.solution.kpi.details[Csp1dKpiKeys.InitialGenerationMaterialSliceTemplateCacheHits])
        assertEquals("2", result.solution.kpi.details[Csp1dKpiKeys.materialUsageBatchCount("m-cg-config")])
        assertEquals("1", result.solution.render.kpi[Csp1dKpiKeys.ColumnGenerationIterationCount])
    }

    /**
     * 验证最终 MILP 失败时列生成返回可解释的部分解 / Verify column generation returns explainable partial solution when final MILP fails
     */
    @Test
    fun columnGenerationShouldReturnPartialSolutionWhenFinalMilpFails(): Unit = runBlocking {
        val product = product(
            id = "p-cg-partial",
            width = 0.8
        )
        val material = material(
            id = "m-cg-partial",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val initialPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            solver = Csp1dFailingMilpSolver(),
            initialGenerator = Csp1dInitialCuttingPlanGenerator { listOf(initialPlan) },
            pricingGenerator = Csp1dPricingGenerator<Flt64> { emptyList() }
        )

        val result = columnGeneration.solveWithTrace(
            problem = problem,
            solveConfig = Csp1dSolveConfig<Flt64>(
                columnGeneration = Csp1dConfiguration(
                    maxInitialPlans = Int64(8),
                    maxPricingPlans = Int64(1),
                    iterationLimit = Int64(1)
                ),
                topKPlanLimit = Int64(1),
                allowPartialSolution = true
            )
        )

        assertEquals(Csp1dFinalMilpStatus.Failed, result.trace.finalMilpStatus)
        assertTrue(result.trace.partialSolutionAvailable)
        assertTrue(result.trace.failureMessage?.contains("MILP") == true)
        assertEquals(Csp1dSolutionStatus.Partial, result.solution.status)
        assertTrue(result.solution.failureMessage?.contains("MILP") == true)
        assertTrue(result.solution.generatedPlans.isNotEmpty())
        assertTrue(result.solution.produce.cuttingPlans.isEmpty())
        assertEquals(problem.demands, result.solution.produce.unmetDemands)
        assertEquals(UInt64.one, result.solution.kpi.topPlanCount)
        assertEquals("Partial", result.solution.render.kpi[Csp1dKpiKeys.SolutionStatus])
        assertEquals("Failed", result.solution.render.kpi[Csp1dKpiKeys.FinalMilpStatus])
        assertEquals("true", result.solution.render.kpi[Csp1dKpiKeys.PartialSolutionAvailable])
    }

    /**
     * 验证禁用部分解后最终 MILP 失败返回 Failed 状态 / Verify final MILP failure returns Failed status when partial solution is disabled
     */
    @Test
    fun columnGenerationShouldReturnFailedStatusWhenPartialDisabled(): Unit = runBlocking {
        val product = product(
            id = "p-cg-no-partial",
            width = 0.8
        )
        val material = material(
            id = "m-cg-no-partial",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val initialPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            solver = Csp1dFailingMilpSolver(),
            initialGenerator = Csp1dInitialCuttingPlanGenerator { listOf(initialPlan) },
            pricingGenerator = Csp1dPricingGenerator<Flt64> { emptyList() }
        )

        val result = columnGeneration.solveWithTrace(
            problem = problem,
            solveConfig = Csp1dSolveConfig<Flt64>(
                columnGeneration = Csp1dConfiguration(
                    maxInitialPlans = Int64(8),
                    maxPricingPlans = Int64(1),
                    iterationLimit = Int64(1)
                ),
                allowPartialSolution = false
            )
        )

        assertEquals(Csp1dSolutionStatus.Failed, result.solution.status)
        assertEquals(Csp1dFinalMilpStatus.Failed, result.trace.finalMilpStatus)
        assertTrue(result.solution.failureMessage?.contains("MILP") == true)
    }

    /**
     * 验证恢复入口显式记录当前 adapter 暂不支持 warm start 并退回普通求解 /
     * Verify recovery records adapter-unsupported warm start and falls back to normal solve
     */
    @Test
    fun recoveryShouldTraceAdapterUnsupportedWarmStart(): Unit = runBlocking {
        val product = product(
            id = "p-recovery-warm",
            width = 0.8
        )
        val material = material(
            id = "m-recovery-warm",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val warmStartPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )

        val result = Csp1dRecovery<Flt64>(fakeSolver).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                warmStart = Csp1dWarmStart(
                    cuttingPlans = listOf(warmStartPlan)
                )
            )
        )

        assertEquals(Csp1dRecoveryStatus.RetriedWithoutWarmStart, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.AdapterUnsupported, result.trace.warmStartStatus)
        assertEquals(Int64.one, result.trace.attemptCount)
        assertEquals(Int64.one, result.trace.warmStartPlanCount)
        assertEquals(Int64.zero, result.trace.appliedWarmStartPlanCount)
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
    }

    /**
     * 验证显式方案池 adapter 会消费 warm start 方案作为初始方案池 /
     * Verify explicit plan-pool adapter consumes warm-start plans as the initial plan pool
     */
    @Test
    fun recoveryShouldApplyWarmStartPlanPoolAdapter(): Unit = runBlocking {
        val product = product(
            id = "p-recovery-apply",
            width = 0.8
        )
        val material = material(
            id = "m-recovery-apply",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val warmStartPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        ).copy(id = "warm-start-plan")
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )

        val result = Csp1dRecovery<Flt64>(
            solver = fakeSolver,
            warmStartAdapter = Csp1dWarmStartPlanPoolAdapter(
                appendFallbackPlans = false
            )
        ).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                warmStart = Csp1dWarmStart(
                    cuttingPlans = listOf(warmStartPlan)
                )
            )
        )

        assertEquals(Csp1dRecoveryStatus.Solved, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.Applied, result.trace.warmStartStatus)
        assertEquals(Int64.one, result.trace.warmStartPlanCount)
        assertEquals(Int64.one, result.trace.appliedWarmStartPlanCount)
        assertEquals(Int64.zero, result.trace.appliedWarmStartUsageCount)
        assertTrue(result.solution.generatedPlans.any { it.id == warmStartPlan.id })
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
    }

    /**
     * 验证 MILP 会把 warm start 使用量写入 assignment 初始解 /
     * Verify MILP writes warm-start usages into assignment initial values
     */
    @Test
    fun milpSolverShouldInjectWarmStartPlanUsagesAsInitialValues(): Unit = runBlocking {
        val product = product(
            id = "p-native-warm",
            width = 0.8
        )
        val firstMaterial = material(
            id = "m-native-warm-1",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val secondMaterial = material(
            id = "m-native-warm-2",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val firstPlan = simpleCuttingPlan(
            product = product,
            material = firstMaterial,
            rollContribution = Flt64.one
        )
        val secondPlan = simpleCuttingPlan(
            product = product,
            material = secondMaterial,
            rollContribution = Flt64.one
        )
        val input = ProduceInput(
            cuttingPlans = listOf(firstPlan, secondPlan),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            materials = listOf(firstMaterial, secondMaterial),
            warmStartPlanUsages = listOf(
                CuttingPlanUsage(
                    plan = secondPlan,
                    amount = UInt64(3UL)
                )
            )
        )
        val solver = Csp1dInitialResultCapturingSolver()

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(solver).solve(input))

        assertNotNull(milpResult)
        assertTrue("x_0_0" !in solver.lastInitialResults)
        assertTrue(Flt64(3.0) eq solver.lastInitialResults["x_0_1"]!!)
    }

    /**
     * 验证 previousSolution 的选中方案使用量会经 recovery 注入 native warm start /
     * Verify selected usages from previousSolution are injected as native warm start through recovery
     */
    @Test
    fun recoveryShouldApplyPreviousSolutionUsagesAsNativeWarmStart(): Unit = runBlocking {
        val product = product(
            id = "p-recovery-native",
            width = 0.8
        )
        val material = material(
            id = "m-recovery-native",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val warmStartPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        ).copy(id = "native-warm-start-plan")
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val previousSolution = Csp1dMilp<Flt64>(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator { listOf(warmStartPlan) }
        ).solve(problem)
        val solver = Csp1dInitialResultCapturingSolver()

        val result = Csp1dRecovery<Flt64>(
            solver = solver,
            warmStartAdapter = Csp1dWarmStartPlanPoolAdapter(
                appendFallbackPlans = false
            )
        ).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                warmStart = Csp1dWarmStart(
                    previousSolution = previousSolution
                )
            )
        )

        assertEquals(Csp1dRecoveryStatus.Solved, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.Applied, result.trace.warmStartStatus)
        assertEquals(Int64.one, result.trace.appliedWarmStartPlanCount)
        assertEquals(Int64.one, result.trace.appliedWarmStartUsageCount)
        assertTrue(Flt64.one eq solver.lastInitialResults["x_0_0"]!!)
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
    }

    /**
     * 验证 previousSolution warm start 与设备产能和增强配置组合时仍会写入初始解并回填 KPI /
     * Verify previousSolution warm start writes initial values and fills KPI with machine capacity and enhanced configs
     */
    @Test
    fun recoveryShouldApplyPreviousSolutionWarmStartWithMachineCapacityAndEnhancements(): Unit = runBlocking {
        val product = dynamicProduct(
            id = "p-recovery-enhanced",
            width = 0.8,
            maxOverProduceLength = 2.0
        )
        val material = material(
            id = "m-recovery-enhanced",
            lowerWidth = 0.5,
            upperWidth = 1.5,
            machineId = "machine-recovery-enhanced",
            length = 100.0
        )
        val machine = machine(
            id = "machine-recovery-enhanced",
            capacity = 120.0,
            maxBatchCount = UInt64(3UL)
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val demandKey = ProductDemandShadowPriceKey(
            productId = product.id,
            unitSymbol = demand.quantity.unit.symbol ?: demand.quantity.unit.name ?: demand.quantity.unit.toString()
        )
        val warmStartPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one,
            machineId = machine.id,
            capacityConsumption = 80.0
        ).copy(id = "enhanced-warm-start-plan")
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = listOf(machine),
            demands = listOf(demand),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val solveConfig = Csp1dSolveConfig<Flt64>(
            columnGeneration = problem.configuration,
            yieldConfig = YieldModelingConfig(
                underProductionPenalty = mapOf(demandKey to Flt64(10.0)),
                overProductionPenalty = mapOf(demandKey to Flt64(5.0))
            ),
            wasteConfig = WasteMinimizationConfig(
                trimWidthPenalty = Flt64(2.0),
                overProductionAreaPenalty = Flt64.one
            ),
            lengthConfig = LengthAssignmentModelingConfig(
                dynamicProductIds = setOf(product.id),
                overLengthPenalty = mapOf(product.id to Flt64(3.0))
            ),
            topKPlanLimit = Int64(1)
        )
        val previousSolution = Csp1dMilp<Flt64>(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator {
                listOf(warmStartPlan)
            }
        ).solve(
            problem = problem,
            solveConfig = solveConfig
        )
        val solver = Csp1dInitialResultCapturingSolver()

        val result = Csp1dRecovery<Flt64>(
            solver = solver,
            warmStartAdapter = Csp1dWarmStartPlanPoolAdapter(
                appendFallbackPlans = false
            )
        ).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                solveConfig = solveConfig,
                warmStart = Csp1dWarmStart(
                    previousSolution = previousSolution
                )
            )
        )

        assertEquals(Csp1dRecoveryStatus.Solved, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.Applied, result.trace.warmStartStatus)
        assertEquals(Int64.one, result.trace.appliedWarmStartPlanCount)
        assertEquals(Int64.one, result.trace.appliedWarmStartUsageCount)
        assertTrue(Flt64.one eq solver.lastInitialResults["x_0_0"]!!)
        val machineUsage = assertNotNull(
            result.solution.produce.machineUsages.firstOrNull { usage -> usage.machine.id == machine.id }?.used,
            "Machine capacity usage should be backfilled"
        )
        assertTrue(machineUsage eq Quantity(Flt64(80.0), Kilogram))
        assertNotNull(result.solution.yieldResult, "Yield result should be backfilled")
        assertNotNull(result.solution.wasteResult, "Waste result should be backfilled")
        assertNotNull(result.solution.lengthResult, "Length result should be backfilled")
        assertEquals(UInt64.one, result.solution.kpi.topPlanCount)
        assertTrue(Csp1dKpiKeys.machineCapacityUsed(machine.id) in result.solution.kpi.details)
        assertTrue(Csp1dKpiKeys.underProduction(product.id, demandKey.unitSymbol) in result.solution.kpi.details)
        assertTrue(Csp1dKpiKeys.assignedLength(product.id) in result.solution.kpi.details)
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
    }

    /**
     * 验证 previousSolution 只复用当前问题兼容的方案和使用量 /
     * Verify previousSolution reuses only plans and usages compatible with the current problem
     */
    @Test
    fun recoveryShouldFilterPreviousSolutionPlansBeforeNativeWarmStart(): Unit = runBlocking {
        val rollProduct = product(
            id = "p-recovery-filter-roll",
            width = 0.5
        )
        val sheetProduct = product(
            id = "p-recovery-filter-sheet",
            width = 0.7
        )
        val staleProduct = product(
            id = "p-recovery-filter-stale",
            width = 0.9
        )
        val rollMaterial = material(
            id = "m-recovery-filter-roll",
            lowerWidth = 0.5,
            upperWidth = 1.2
        )
        val sheetMaterial = material(
            id = "m-recovery-filter-sheet",
            lowerWidth = 0.5,
            upperWidth = 1.4
        )
        val staleMaterial = material(
            id = "m-recovery-filter-stale",
            lowerWidth = 0.5,
            upperWidth = 1.6
        )
        val rollPlan = simpleCuttingPlan(
            product = rollProduct,
            material = rollMaterial,
            rollContribution = Flt64(2.0)
        ).copy(id = "warm-start-roll-plan")
        val sheetPlan = sheetCuttingPlan(
            id = "warm-start-sheet-plan",
            product = sheetProduct,
            material = sheetMaterial,
            sheetContribution = Flt64(3.0)
        )
        val stalePlan = simpleCuttingPlan(
            product = staleProduct,
            material = staleMaterial,
            rollContribution = Flt64.one
        ).copy(id = "warm-start-stale-plan")
        val previousProblem = Csp1dProblem<Flt64>(
            products = listOf(rollProduct, sheetProduct, staleProduct),
            materials = listOf(rollMaterial, sheetMaterial, staleMaterial),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = rollProduct,
                    rollAmount = Flt64(2.0)
                ),
                ProductDemand.legacySheet(
                    product = sheetProduct,
                    sheetAmount = Flt64(3.0)
                ),
                ProductDemand.legacyRoll(
                    product = staleProduct,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val previousSolution = Csp1dMilp<Flt64>(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator {
                listOf(rollPlan, sheetPlan, stalePlan)
            }
        ).solve(previousProblem)
        val currentProblem = Csp1dProblem<Flt64>(
            products = listOf(rollProduct, sheetProduct),
            materials = listOf(rollMaterial, sheetMaterial),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = rollProduct,
                    rollAmount = Flt64(2.0)
                ),
                ProductDemand.legacySheet(
                    product = sheetProduct,
                    sheetAmount = Flt64(3.0)
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val solver = Csp1dInitialResultCapturingSolver()

        val result = Csp1dRecovery<Flt64>(
            solver = solver,
            warmStartAdapter = Csp1dWarmStartPlanPoolAdapter(
                appendFallbackPlans = false
            )
        ).solveWithTrace(
            Csp1dRecoveryInput(
                problem = currentProblem,
                warmStart = Csp1dWarmStart(
                    previousSolution = previousSolution
                )
            )
        )

        assertEquals(Csp1dRecoveryStatus.Solved, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.Applied, result.trace.warmStartStatus)
        assertEquals(Int64(2), result.trace.warmStartPlanCount)
        assertEquals(Int64(2), result.trace.appliedWarmStartPlanCount)
        assertEquals(Int64(2), result.trace.appliedWarmStartUsageCount)
        assertTrue(Flt64.one eq solver.lastInitialResults["x_0_0"]!!)
        assertTrue(Flt64.one eq solver.lastInitialResults["x_0_1"]!!)
        assertTrue(result.solution.generatedPlans.none { it.id == stalePlan.id })
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
    }

    /**
     * 验证列生成最终 MILP 会消费 warm start adapter 提供的使用量 /
     * Verify column generation final MILP consumes usages provided by warm-start adapter
     */
    @Test
    fun columnGenerationShouldApplyWarmStartUsagesToFinalMilp(): Unit = runBlocking {
        val product = product(
            id = "p-cg-native-warm",
            width = 0.5
        )
        val material = material(
            id = "m-cg-native-warm",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val singlePlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        ).copy(id = "cg-native-warm-single")
        val packedPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64(3.0)
        ).copy(id = "cg-native-warm-packed")
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
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val previousSolution = Csp1dMilp<Flt64>(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator {
                listOf(singlePlan, packedPlan)
            }
        ).solve(problem)
        val adapterResult = Csp1dWarmStartPlanPoolAdapter<Flt64>(
            appendFallbackPlans = false
        ).apply(
            Csp1dWarmStartAdapterInput(
                problem = problem,
                solveConfig = null,
                warmStart = Csp1dWarmStart(
                    previousSolution = previousSolution
                ),
                cuttingPlans = previousSolution.generatedPlans
            )
        )
        val solver = Csp1dInitialResultCapturingSolver()
        val initialGenerator = assertNotNull(
            adapterResult.initialGenerator,
            "Warm-start adapter should provide an initial generator"
        )

        val result = Csp1dColumnGeneration(
            solver = solver,
            initialGenerator = initialGenerator,
            pricingGenerator = Csp1dPricingGenerator<Flt64> { emptyList() },
            warmStartPlanUsages = adapterResult.initialPlanUsages
        ).solveWithTrace(problem)

        assertEquals(Csp1dFinalMilpStatus.Solved, result.trace.finalMilpStatus)
        assertTrue(Flt64.one eq solver.lastInitialResults["x_0_0"]!!)
        assertTrue(Flt64.one eq solver.lastInitialResults["x_0_1"]!!)
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
    }

    /**
     * 验证列生成恢复入口会从上一轮解消费 warm start 使用量 /
     * Verify column-generation recovery consumes warm-start usages from previous solution
     */
    @Test
    fun columnGenerationRecoveryShouldApplyPreviousSolutionWarmStart(): Unit = runBlocking {
        val product = product(
            id = "p-cg-recovery-native",
            width = 0.5
        )
        val material = material(
            id = "m-cg-recovery-native",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val singlePlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        ).copy(id = "cg-recovery-single")
        val packedPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64(3.0)
        ).copy(id = "cg-recovery-packed")
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
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val previousSolution = Csp1dMilp<Flt64>(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator {
                listOf(singlePlan, packedPlan)
            }
        ).solve(problem)
        val solver = Csp1dInitialResultCapturingSolver()

        val result = Csp1dColumnGenerationRecovery<Flt64>(
            solver = solver,
            pricingGenerator = Csp1dPricingGenerator<Flt64> { emptyList() },
            warmStartAdapter = Csp1dWarmStartPlanPoolAdapter(
                appendFallbackPlans = false
            )
        ).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                warmStart = Csp1dWarmStart(
                    previousSolution = previousSolution
                )
            )
        )

        assertEquals(Csp1dRecoveryStatus.Solved, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.Applied, result.trace.warmStartStatus)
        assertEquals(Int64(2), result.trace.warmStartPlanCount)
        assertEquals(Int64(2), result.trace.appliedWarmStartPlanCount)
        assertEquals(Int64(2), result.trace.appliedWarmStartUsageCount)
        assertTrue(Flt64.one eq solver.lastInitialResults["x_0_0"]!!)
        assertTrue(Flt64.one eq solver.lastInitialResults["x_0_1"]!!)
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
    }

    /**
     * 验证列生成恢复结果可以再次作为 previousSolution 进入下一轮 warm start /
     * Verify column-generation recovery result can be reused as next-round previousSolution warm start
     */
    @Test
    fun columnGenerationRecoveryShouldReuseRecoveredSolutionAsNextWarmStart(): Unit = runBlocking {
        val product = product(
            id = "p-cg-recovery-round",
            width = 0.8
        )
        val material = material(
            id = "m-cg-recovery-round",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val warmStartPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        ).copy(id = "cg-recovery-round-plan")
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val seedSolution = Csp1dMilp<Flt64>(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator {
                listOf(warmStartPlan)
            }
        ).solve(problem)
        val firstResult = Csp1dColumnGenerationRecovery<Flt64>(
            solver = fakeSolver,
            pricingGenerator = Csp1dPricingGenerator<Flt64> { emptyList() },
            warmStartAdapter = Csp1dWarmStartPlanPoolAdapter(
                appendFallbackPlans = false
            )
        ).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                warmStart = Csp1dWarmStart(
                    previousSolution = seedSolution
                )
            )
        )
        val solver = Csp1dInitialResultCapturingSolver()

        val secondResult = Csp1dColumnGenerationRecovery<Flt64>(
            solver = solver,
            pricingGenerator = Csp1dPricingGenerator<Flt64> { emptyList() },
            warmStartAdapter = Csp1dWarmStartPlanPoolAdapter(
                appendFallbackPlans = false
            )
        ).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                warmStart = Csp1dWarmStart(
                    previousSolution = firstResult.solution
                )
            )
        )

        assertEquals(Csp1dRecoveryStatus.Solved, firstResult.trace.status)
        assertEquals(Csp1dWarmStartStatus.Applied, firstResult.trace.warmStartStatus)
        assertEquals(Csp1dRecoveryStatus.Solved, secondResult.trace.status)
        assertEquals(Csp1dWarmStartStatus.Applied, secondResult.trace.warmStartStatus)
        assertEquals(Int64.one, secondResult.trace.appliedWarmStartPlanCount)
        assertEquals(Int64.one, secondResult.trace.appliedWarmStartUsageCount)
        assertTrue(Flt64.one eq solver.lastInitialResults["x_0_0"]!!)
        assertEquals(Csp1dSolutionStatus.Feasible, secondResult.solution.status)
    }

    /**
     * 验证列生成恢复在最终 MILP 失败时保留部分解语义 /
     * Verify column-generation recovery keeps partial-solution semantics when final MILP fails
     */
    @Test
    fun columnGenerationRecoveryShouldReturnPartialSolutionWhenFinalMilpFails(): Unit = runBlocking {
        val product = product(
            id = "p-cg-recovery-partial",
            width = 0.8
        )
        val material = material(
            id = "m-cg-recovery-partial",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val warmStartPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        ).copy(id = "cg-recovery-partial-plan")
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )
        val previousSolution = Csp1dMilp<Flt64>(
            solver = fakeSolver,
            initialGenerator = Csp1dInitialCuttingPlanGenerator {
                listOf(warmStartPlan)
            }
        ).solve(problem)

        val result = Csp1dColumnGenerationRecovery<Flt64>(
            solver = Csp1dFailingMilpSolver(),
            pricingGenerator = Csp1dPricingGenerator<Flt64> { emptyList() },
            warmStartAdapter = Csp1dWarmStartPlanPoolAdapter(
                appendFallbackPlans = false
            )
        ).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                solveConfig = Csp1dSolveConfig(
                    columnGeneration = problem.configuration,
                    topKPlanLimit = Int64(1),
                    allowPartialSolution = true
                ),
                warmStart = Csp1dWarmStart(
                    previousSolution = previousSolution
                )
            )
        )

        assertEquals(Csp1dRecoveryStatus.Solved, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.Applied, result.trace.warmStartStatus)
        assertEquals(Int64.one, result.trace.appliedWarmStartPlanCount)
        assertEquals(Int64.one, result.trace.appliedWarmStartUsageCount)
        assertEquals(Csp1dSolutionStatus.Partial, result.solution.status)
        assertTrue(result.solution.failureMessage?.contains("MILP") == true)
        assertTrue(result.solution.generatedPlans.any { it.id == warmStartPlan.id })
        assertTrue(result.solution.produce.cuttingPlans.isEmpty())
        assertEquals("Partial", result.solution.render.kpi[Csp1dKpiKeys.SolutionStatus])
        assertEquals("Failed", result.solution.render.kpi[Csp1dKpiKeys.FinalMilpStatus])
        assertEquals("true", result.solution.render.kpi[Csp1dKpiKeys.PartialSolutionAvailable])
    }

    /**
     * 验证空 warm start 输入会被记录为 ignored 并完成普通求解 /
     * Verify empty warm start input is recorded as ignored and normal solve completes
     */
    @Test
    fun recoveryShouldTraceIgnoredEmptyWarmStart(): Unit = runBlocking {
        val product = product(
            id = "p-recovery-empty",
            width = 0.8
        )
        val material = material(
            id = "m-recovery-empty",
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
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )

        val result = Csp1dRecovery<Flt64>(fakeSolver).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                warmStart = Csp1dWarmStart()
            )
        )

        assertEquals(Csp1dRecoveryStatus.Solved, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.Ignored, result.trace.warmStartStatus)
        assertEquals(Int64.one, result.trace.attemptCount)
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
    }

    /**
     * 验证恢复入口识别无效 warm start 并退回普通求解 /
     * Verify recovery detects invalid warm start and falls back to normal solve
     */
    @Test
    fun recoveryShouldRetryWithoutInvalidWarmStart(): Unit = runBlocking {
        val product = product(
            id = "p-recovery-invalid",
            width = 0.8
        )
        val material = material(
            id = "m-recovery-valid",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val invalidMaterial = material(
            id = "m-recovery-invalid",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val invalidPlan = simpleCuttingPlan(
            product = product,
            material = invalidMaterial,
            rollContribution = Flt64.one
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )

        val result = Csp1dRecovery<Flt64>(fakeSolver).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                warmStart = Csp1dWarmStart(
                    cuttingPlans = listOf(invalidPlan)
                )
            )
        )

        assertEquals(Csp1dRecoveryStatus.RetriedWithoutWarmStart, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.Invalid, result.trace.warmStartStatus)
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
    }

    /**
     * 验证禁用 fallback 后无效 warm start 会直接失败 /
     * Verify invalid warm start fails when fallback is disabled
     */
    @Test
    fun recoveryShouldRejectInvalidWarmStartWhenFallbackDisabled() {
        val product = product(
            id = "p-recovery-reject",
            width = 0.8
        )
        val material = material(
            id = "m-recovery-reject-valid",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val invalidMaterial = material(
            id = "m-recovery-reject-invalid",
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
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )

        val result = runBlocking {
            Csp1dRecovery<Flt64>(fakeSolver).solveWithTrace(
                Csp1dRecoveryInput(
                    problem = problem,
                    warmStart = Csp1dWarmStart(
                        cuttingPlans = listOf(
                            simpleCuttingPlan(
                                product = product,
                                material = invalidMaterial,
                                rollContribution = Flt64.one
                            )
                        )
                    ),
                    options = Csp1dRecoveryOptions(
                        retryWithoutWarmStart = false
                    )
                )
            )
        }

        assertTrue(result is Failed)
        assertEquals("Warm start cannot be applied and fallback is disabled: Invalid", (result as Failed).error.message)
    }

    /**
     * 验证兼容 warm start 在 adapter 未配置且禁用 fallback 时会直接失败 /
     * Verify compatible warm start fails when adapter is unsupported and fallback is disabled
     */
    @Test
    fun recoveryShouldRejectCompatibleWarmStartWhenAdapterUnsupportedAndFallbackDisabled() {
        val product = product(
            id = "p-recovery-unsupported",
            width = 0.8
        )
        val material = material(
            id = "m-recovery-unsupported",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val warmStartPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )

        val result = runBlocking {
            Csp1dRecovery<Flt64>(fakeSolver).solveWithTrace(
                Csp1dRecoveryInput(
                    problem = problem,
                    warmStart = Csp1dWarmStart(
                        cuttingPlans = listOf(warmStartPlan)
                    ),
                    options = Csp1dRecoveryOptions(
                        retryWithoutWarmStart = false
                    )
                )
            )
        }

        assertTrue(result is Failed)
        assertEquals("Warm start cannot be applied and fallback is disabled: AdapterUnsupported", (result as Failed).error.message)
    }

    /**
     * 验证普通求解失败时 recovery 用 trace 包装异常 /
     * Verify recovery wraps normal solve failure with trace
     */
    @Test
    fun recoveryShouldReturnFailedSolutionWhenMilpFails(): Unit = runBlocking {
        val product = product(
            id = "p-recovery-failure",
            width = 0.8
        )
        val material = material(
            id = "m-recovery-failure",
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
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(1)
            )
        )

        val result = Csp1dRecovery<Flt64>(Csp1dFailingMilpSolver()).solveWithTrace(
            Csp1dRecoveryInput(
                problem = problem,
                solveConfig = Csp1dSolveConfig(
                    columnGeneration = problem.configuration,
                    allowPartialSolution = false
                )
            )
        )

        assertEquals(Csp1dRecoveryStatus.Solved, result.trace.status)
        assertEquals(Csp1dWarmStartStatus.NotProvided, result.trace.warmStartStatus)
        assertEquals(Int64.one, result.trace.attemptCount)
        assertEquals(Csp1dSolutionStatus.Failed, result.solution.status)
    }

    /**
     * 验证首次 LP 求解失败时列生成返回 LpInfeasible 终止原因 /
     * Verify column generation returns LpInfeasible when first LP solve fails
     */
    @Test
    fun columnGenerationShouldReturnLpInfeasibleWhenFirstLpFails(): Unit = runBlocking {
        val product = product(
            id = "p-lp-infeasible",
            width = 0.8
        )
        val material = material(
            id = "m-lp-infeasible",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val initialPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8),
                maxPricingPlans = Int64(1),
                iterationLimit = Int64(3)
            )
        )
        val columnGeneration = Csp1dColumnGeneration<Flt64>(
            solver = Csp1dFailingLpSolver(),
            initialGenerator = Csp1dInitialCuttingPlanGenerator { listOf(initialPlan) },
            pricingGenerator = Csp1dPricingGenerator<Flt64> { emptyList() }
        )

        val result = columnGeneration.solveWithTrace(problem)

        assertEquals(Csp1dTerminationReason.LpInfeasible, result.trace.terminationReason)
        assertTrue(result.trace.lpFailureMessage?.contains("LP solve returned null") == true)
        // LP 失败但最终 MILP 使用初始方案仍然成功，所以 solutionStatus 为 Feasible
        assertEquals(Csp1dSolutionStatus.Feasible, result.solution.status)
        // LP failure message 应合并到 solution.failureMessage
        assertTrue(result.solution.failureMessage?.contains("LP solve returned null") == true)
    }

    /**
     * 验证 MILP 失败且禁用部分解时返回 Failed 状态 /
     * Verify MILP failure with allowPartialSolution=false returns Failed status
     */
    @Test
    fun milpShouldReturnFailedStatusWhenPartialDisabledAndMilpFails(): Unit = runBlocking {
        val product = product(
            id = "p-milp-failed",
            width = 0.8
        )
        val material = material(
            id = "m-milp-failed",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val initialPlan = simpleCuttingPlan(
            product = product,
            material = material,
            rollContribution = Flt64.one
        )
        val problem = Csp1dProblem<Flt64>(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(
                ProductDemand.legacyRoll(
                    product = product,
                    rollAmount = Flt64.one
                )
            ),
            configuration = Csp1dConfiguration(
                maxInitialPlans = Int64(8)
            )
        )
        val milp = Csp1dMilp<Flt64>(
            solver = Csp1dFailingMilpSolver(),
            initialGenerator = Csp1dInitialCuttingPlanGenerator { listOf(initialPlan) }
        )

        val solution = milp.solve(
            problem = problem,
            solveConfig = Csp1dSolveConfig<Flt64>(
                columnGeneration = problem.configuration,
                allowPartialSolution = false
            )
        )

        assertEquals(Csp1dSolutionStatus.Failed, solution.status)
        assertTrue(solution.failureMessage?.contains("MILP") == true)
    }

    private fun product(
        id: String,
        width: Double
    ): Product<Flt64> {
        return product(
            id = id,
            widths = listOf(width)
        )
    }

    private fun product(
        id: String,
        widths: List<Double>
    ): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = widths.map { Quantity(Flt64(it), Meter) }
        )
    }

    private fun dynamicProduct(
        id: String,
        width: Double,
        maxOverProduceLength: Double? = null
    ): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = listOf(
                Quantity(Flt64(width), Meter)
            ),
            maxOverProduceLength = maxOverProduceLength?.let { Quantity(Flt64(it), Meter) },
            dynamicLength = true
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
        capacity: Double,
        maxBatchCount: UInt64? = null
    ): Machine<Flt64> {
        return Machine(
            id = id,
            name = "machine-$id",
            maxBatchCount = maxBatchCount,
            capacity = Quantity(Flt64(capacity), Kilogram)
        )
    }

    /**
     * 验证 yield 建模在 MILP solver 上正确注册欠产/超产松弛变量并影响求解结果 / Verify yield modeling registers under/over production slack variables correctly and affects solve results
     */
    @Test
    fun milpWithYieldConfigShouldProduceYieldResult(): Unit = runBlocking {
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            yieldConfig = yieldConfig
        ))
        assertNotNull(milpResult.yieldResult, "Yield result should not be null when yieldConfig is provided")
        assertTrue(milpResult.produce.cuttingPlans.isNotEmpty(), "Should have cutting plan usage")
    }

    /**
     * 验证 yieldConfig 为 null 时 MILP solver 无 yield 建模，向后兼容 / Verify no yield modeling when yieldConfig is null, backward compatibility
     */
    @Test
    fun milpWithoutYieldConfigShouldNotProduceYieldResult(): Unit = runBlocking {
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(input))
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
     * 验证 lengthConfig 为 null 时 MILP solver 无长度分配建模，向后兼容 / Verify no length assignment modeling when lengthConfig is null, backward compatibility
     */
    @Test
    fun milpWithoutLengthConfigShouldNotProduceLengthResult(): Unit = runBlocking {
        val product = dynamicProduct(
            id = "p-no-length",
            width = 0.8
        )
        val material = material(
            id = "m-no-length",
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(input))
        assertEquals(null, milpResult.lengthResult, "Length result should be null when lengthConfig is not provided")
    }

    /**
     * 验证 lengthConfig 带超长惩罚时 MILP solver 回填超长结果 / Verify length modeling extracts over-length when over-length penalty is provided
     */
    @Test
    fun milpWithOverLengthPenaltyShouldProduceLengthResult(): Unit = runBlocking {
        val product = dynamicProduct(
            id = "p-length",
            width = 0.8
        )
        val material = material(
            id = "m-length",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(product.id),
            overLengthPenalty = mapOf(product.id to Flt64(4.0))
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            lengthConfig = lengthConfig
        ))
        assertNotNull(milpResult.lengthResult, "Length result should not be null when lengthConfig is provided")
        assertEquals(product.id, milpResult.lengthResult!!.overLengths.first().productId)
        assertEquals(Flt64.one, milpResult.lengthResult!!.overLengths.first().overLength)
    }

    /**
     * 验证仅配置超长上限时也会注册超长松弛变量 / Verify over-length slack is registered when only upper bound is configured
     */
    @Test
    fun milpWithOnlyOverLengthUpperBoundShouldProduceLengthResult(): Unit = runBlocking {
        val product = dynamicProduct(
            id = "p-length-bound",
            width = 0.8
        )
        val material = material(
            id = "m-length-bound",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(product.id),
            overLengthUpperBound = mapOf(product.id to Flt64(2.0))
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            lengthConfig = lengthConfig
        ))
        assertNotNull(milpResult.lengthResult, "Length result should not be null when lengthConfig is provided")
        assertEquals(Flt64.one, milpResult.lengthResult!!.overLengths.first().overLength)
    }

    /**
     * 验证已分配卷长变量、边界与超长联动约束会被注册并回填 / Verify assigned-length variable, bounds, and over-length link are registered and extracted
     */
    @Test
    fun milpWithAssignedLengthBoundsShouldProduceAssignedLengthResult(): Unit = runBlocking {
        val product = dynamicProduct(
            id = "p-assigned-length",
            width = 0.8,
            maxOverProduceLength = 0.5
        )
        val material = material(
            id = "m-assigned-length",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(product.id),
            assignedLengthLowerBound = mapOf(product.id to Flt64(0.5)),
            assignedLengthUpperBound = mapOf(product.id to Flt64(2.0)),
            overLengthUpperBound = mapOf(product.id to Flt64(2.0)),
            totalLengthPenalty = Flt64(0.1)
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            lengthConfig = lengthConfig
        ))
        val lengthResult = milpResult.lengthResult
        assertNotNull(lengthResult, "Length result should not be null when assigned length is modeled")
        assertEquals(product.id, lengthResult.assignedLengths.first().productId)
        assertEquals(Flt64.one, lengthResult.assignedLengths.first().assignedLength)
        assertEquals(Flt64.one, lengthResult.overLengths.first().overLength)

        @Suppress("UNCHECKED_CAST")
        val constraintNames = milpResult.model.constraints.mapNotNull { constraint ->
            (constraint as? LinearInequalityConstraint<Flt64>)?.name
        }.toSet()
        assertTrue("assigned_length_lower_bound_0" in constraintNames)
        assertTrue("assigned_length_upper_bound_0" in constraintNames)
        assertTrue("over_length_bound_0" in constraintNames)
        assertTrue("assigned_over_length_link_0" in constraintNames)
    }

    /**
     * 验证 wasteConfig 为 null 时 MILP solver 无废弃建模，向后兼容 / Verify no waste modeling when wasteConfig is null, backward compatibility
     */
    @Test
    fun milpWithoutWasteConfigShouldNotProduceWasteResult(): Unit = runBlocking {
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(input))
        assertEquals(null, milpResult.wasteResult, "Waste result should be null when wasteConfig is not provided")
    }

    /**
     * 验证 wasteConfig 带 trimWidthPenalty 时 MILP solver 正确提取余宽浪费 / Verify waste modeling extracts trim width when wasteConfig is provided
     */
    @Test
    fun milpWithTrimWidthPenaltyShouldProduceWasteResult(): Unit = runBlocking {
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            wasteConfig = wasteConfig
        ))
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertEquals(
            Flt64(0.7),
            milpResult.wasteResult!!.totalTrimWidth,
            "Total trim width should match rest width times selected batch count"
        )
    }

    /**
     * 验证 wasteConfig 带 restMaterialPenalty 时 MILP solver 正确提取余料面积代理 / Verify waste modeling extracts rest material area proxy when restMaterialPenalty is provided
     */
    @Test
    fun milpWithRestMaterialPenaltyShouldProduceWasteResult(): Unit = runBlocking {
        val product = product(
            id = "p-rest-material",
            width = 0.8
        )
        val material = material(
            id = "m-rest-material",
            lowerWidth = 0.5,
            upperWidth = 1.5,
            length = 100.0
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )

        val wasteConfig = WasteMinimizationConfig<Flt64>(
            restMaterialPenalty = Flt64(0.5)
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            wasteConfig = wasteConfig
        ))
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertEquals(
            Flt64(70.0),
            milpResult.wasteResult!!.totalRestMaterial,
            "Total rest material should match rest width times material length times selected batch count"
        )
        assertEquals(
            RestMaterialMeasure.RestWidthByMaterialLengthProxy,
            milpResult.wasteResult!!.restMaterialMeasure
        )
    }

    /**
     * 验证缺少物料长度时余料面积代理不产生结果 / Verify rest material area proxy is skipped without material length
     */
    @Test
    fun milpWithRestMaterialPenaltyButNoMaterialLengthShouldSkipRestMaterial(): Unit = runBlocking {
        val product = product(
            id = "p-rest-material-no-length",
            width = 0.8
        )
        val material = material(
            id = "m-rest-material-no-length",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )

        val wasteConfig = WasteMinimizationConfig<Flt64>(
            restMaterialPenalty = Flt64(0.5)
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            wasteConfig = wasteConfig
        ))
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertEquals(null, milpResult.wasteResult!!.totalRestMaterial)
        assertEquals(
            RestMaterialMeasure.RestWidthByMaterialLengthProxy,
            milpResult.wasteResult!!.restMaterialMeasure
        )
    }

    /**
     * 验证 wasteConfig 带 materialCostPenalty 时 MILP solver 正确提取物料成本 / Verify waste modeling extracts material costs when materialCostPenalty is provided
     */
    @Test
    fun milpWithMaterialCostPenaltyShouldProduceWasteResult(): Unit = runBlocking {
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            wasteConfig = wasteConfig
        ))
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertTrue(milpResult.wasteResult!!.materialCosts.isNotEmpty(), "Material costs should not be empty when materialCostPenalty is provided")
        assertEquals("m-cost", milpResult.wasteResult!!.materialCosts.first().materialId)
        assertEquals(Flt64(5.0), milpResult.wasteResult!!.materialCosts.first().cost)
    }

    /**
     * 验证仅配置超产面积惩罚时也会注册超产松弛变量 / Verify over-production slack is registered when only over-production area penalty is configured
     */
    @Test
    fun milpWithOnlyOverProductionAreaPenaltyShouldProduceArea(): Unit = runBlocking {
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            yieldConfig = yieldConfig,
            wasteConfig = wasteConfig
        ))
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertEquals(
            Flt64(0.8),
            milpResult.wasteResult!!.overProductionArea,
            "Over-production area should match over slack times max product width"
        )
        assertEquals(
            OverProductionAreaMeasure.ProductMaxWidthProxy,
            milpResult.wasteResult!!.overProductionAreaMeasure
        )
    }

    /**
     * 验证多宽度产品的超产面积使用最大宽度，避免依赖 width 列表顺序 / Verify multi-width over-production area uses max width, independent of width list order
     */
    @Test
    fun milpWithMultiWidthProductOverProductionAreaShouldUseMaxWidth(): Unit = runBlocking {
        val product = product(
            id = "p-area-multi-width",
            widths = listOf(0.8, 1.2)
        )
        val material = material(
            id = "m-area-multi-width",
            lowerWidth = 0.5,
            upperWidth = 2.0
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            yieldConfig = yieldConfig,
            wasteConfig = wasteConfig
        ))
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertEquals(
            Flt64(1.2),
            milpResult.wasteResult!!.overProductionArea,
            "Over-production area should use max product width for multi-width products"
        )
    }

    /**
     * 验证仅配置超产上限时也会注册超产松弛变量 / Verify over-production slack is registered when only upper bound is configured
     */
    @Test
    fun milpWithOnlyOverProductionUpperBoundShouldProduceOverYieldResult(): Unit = runBlocking {
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            yieldConfig = yieldConfig
        ))
        assertNotNull(milpResult.yieldResult, "Yield result should not be null when yieldConfig is provided")
        assertEquals(Flt64.one, milpResult.yieldResult!!.overProductions.first().amount)
    }

    /**
     * 验证 yield 和 waste 联合建模时 MILP solver 正确提取两个结果 / Verify both yield and waste results are extracted when both configs are provided
     */
    @Test
    fun milpWithYieldAndWasteConfigShouldProduceBothResults(): Unit = runBlocking {
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            yieldConfig = yieldConfig,
            wasteConfig = wasteConfig
        ))
        assertNotNull(milpResult.yieldResult, "Yield result should not be null when yieldConfig is provided")
        assertNotNull(milpResult.wasteResult, "Waste result should not be null when wasteConfig is provided")
        assertEquals(Flt64(0.7), milpResult.wasteResult!!.totalTrimWidth)
        assertEquals(Flt64(0.8), milpResult.wasteResult!!.overProductionArea)
    }

    /**
     * 验证 batchMinPenalty 接入目标函数后求解正常完成 / Verify batchMinPenalty is accepted and solve completes normally
     */
    @Test
    fun milpWithBatchMinPenaltyShouldCompleteSuccessfully(): Unit = runBlocking {
        val product = dynamicProduct(
            id = "p-batch-min",
            width = 0.8
        )
        val material = material(
            id = "m-batch-min",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(product.id),
            batchMinPenalty = Flt64(2.0)
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            lengthConfig = lengthConfig
        ))
        assertTrue(milpResult.produce.cuttingPlans.isNotEmpty(), "Should have cutting plan usage")
    }

    /**
     * 验证 batchMinPenalty 可与 totalLengthPenalty 和 overLengthPenalty 联合使用 / Verify batchMinPenalty works together with totalLengthPenalty and overLengthPenalty
     */
    @Test
    fun milpWithBatchMinPenaltyAndOtherLengthConfigShouldComplete(): Unit = runBlocking {
        val product = dynamicProduct(
            id = "p-batch-full",
            width = 0.8,
            maxOverProduceLength = 0.5
        )
        val material = material(
            id = "m-batch-full",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(product.id),
            assignedLengthLowerBound = mapOf(product.id to Flt64(0.5)),
            assignedLengthUpperBound = mapOf(product.id to Flt64(2.0)),
            overLengthPenalty = mapOf(product.id to Flt64(4.0)),
            totalLengthPenalty = Flt64(0.1),
            batchMinPenalty = Flt64(1.5)
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            lengthConfig = lengthConfig
        ))
        val lengthResult = milpResult.lengthResult
        assertNotNull(lengthResult, "Length result should not be null when full lengthConfig is provided")
        assertEquals(product.id, lengthResult.assignedLengths.first().productId)
        assertEquals(Flt64.one, lengthResult.assignedLengths.first().assignedLength)
        assertEquals(Flt64.one, lengthResult.overLengths.first().overLength)
    }

    /**
     * 验证仅配置 dynamicProductIds 不配置 assignedLengthLowerBound/UpperBound 时自动推导并注册 assignedLength 变量 / Verify assignedLength is registered when only dynamicProductIds is configured
     */
    @Test
    fun milpWithOnlyDynamicProductIdsShouldDeriveDefaultLengthBounds(): Unit = runBlocking {
        val product = dynamicProduct(
            id = "p-default-derive",
            width = 0.8,
            maxOverProduceLength = 2.0
        )
        val material = material(
            id = "m-default-derive",
            lowerWidth = 0.5,
            upperWidth = 1.5
        )
        val demand = ProductDemand.legacyRoll(
            product = product,
            rollAmount = Flt64(3.0)
        )
        val lengthConfig = LengthAssignmentModelingConfig<Flt64>(
            dynamicProductIds = setOf(product.id)
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

        val milpResult = unwrapMilpResult(Csp1dMilpSolver(fakeSolver).solve(
            input = input,
            lengthConfig = lengthConfig
        ))
        val lengthResult = milpResult.lengthResult
        assertNotNull(lengthResult, "Length result should not be null when dynamicProductIds is configured")
        // assignedLength 应被注册并回填 / assignedLength should be registered and extracted
        assertTrue(lengthResult.assignedLengths.isNotEmpty(), "Should have assigned length result")
        assertEquals(product.id, lengthResult.assignedLengths.first().productId)
    }

    private fun simpleCuttingPlan(
        product: Product<Flt64>,
        material: Material<Flt64>,
        rollContribution: Flt64,
        machineId: String? = null,
        capacityConsumption: Double? = null
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
            machineId = machineId,
            capacityConsumption = capacityConsumption?.let { Quantity(Flt64(it), Kilogram) }
        )
    }

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
                    amount = UInt64.one,
                    width = product.width.first()
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
