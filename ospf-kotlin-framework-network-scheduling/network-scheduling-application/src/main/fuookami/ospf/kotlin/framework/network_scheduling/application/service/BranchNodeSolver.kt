@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.network_scheduling.application.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.BranchMask
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.service.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.BranchNode

/**
 * 单分支节点求解器。 / Single branch node solver.
 *
 * 负责单个分支节点的完整求解流程：Phase I → Phase II 列生成循环、ESPPRC 定价、解提取。
 * 每次调用创建新的 model 和 context，不跨节点复用可变建模状态。 / Handles the complete solving flow for a single branch node: Phase I -> Phase II column
 * generation loop, ESPPRC pricing, and solution extraction. Each call creates a fresh model
 * and context; mutable modeling state is never shared across nodes.
 */
class BranchNodeSolver<V : RealNumber<V>>(
    private val instance: VrptwInstance<V>,
    private val solver: ColumnGenerationSolver,
    private val valueAdapter: NetworkSchedulingSolverValueAdapter<V>,
    private val distanceCalculator: DistanceCalculator<V>,
    private val travelTimeCalculator: TravelTimeCalculator<V>,
    private val arcCostCalculator: ArcCostCalculator<V>,
    private val routeCostPolicy: RouteCostPolicy<V>,
    private val arcFeasibilityPolicy: ArcFeasibilityPolicy<V> = DefaultArcFeasibilityPolicy(),
    private val labelDominancePolicy: LabelDominancePolicy = LabelDominancePolicy.Default,
    private val pricingColumnSelector: PricingColumnSelector<V> = PricingColumnSelector.default(),
    private val extraPipelines: List<CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap>> = emptyList(),
    private val maxColumnsPerPricing: Int = Int.MAX_VALUE,
    private val maxCGIterations: Int = 1000,
    private val interruptionChecker: PricingInterruptionChecker = PricingInterruptionChecker.Never,
    private val inheritedColumns: List<Route<V>> = emptyList()
) {
    private data class ColumnGenerationResult(
        val pricingComplete: Boolean,
        val iterations: Int,
        val interrupted: Boolean,
        val solverStatus: SolverStatus? = null
    )

    /**
     * 单节点求解结果。 / Single node solve result.
     *
     * @property lowerBound 节点 LP 下界（定价收敛后的 LP 目标值） / Node LP lower bound (LP objective after pricing convergence)
     * @property routeValues 路线变量值 / Route variable values
     * @property isInteger 解是否满足整数容差 / Whether solution satisfies integrality tolerance
     * @property isFeasible 节点是否可行 / Whether node is feasible
     * @property pricingComplete 定价是否完整收敛 / Whether pricing has fully converged
     * @property lpObjective LP 目标值 / LP objective value
     * @property iterations 本节点消耗的 CG 迭代次数（Phase I + Phase II） / CG iterations consumed by this node (Phase I + Phase II)
     * @property timeLimitReached 是否因时间上限中断 / Whether interrupted by the time limit
     * @property solverStatus 最后一次 LP 的求解状态 / Last LP solver status
     * @property columns 本节点生成的完整列池快照 / Complete column-pool snapshot generated at this node
     */
    data class NodeSolveResult<V : RealNumber<V>>(
        val lowerBound: Flt64,
        val routeValues: List<Pair<Route<V>, Flt64>>,
        val isInteger: Boolean,
        val isFeasible: Boolean,
        val pricingComplete: Boolean,
        val lpObjective: Flt64,
        val iterations: Int,
        val timeLimitReached: Boolean = false,
        val solverStatus: SolverStatus = SolverStatus.Optimal,
        val columns: List<Route<V>> = emptyList()
    )

    /**
     * 求解分支节点。 / Solve a branch node.
     *
     * 完整流程：
     * 1. 创建新的 model 和 context
     * 2. 注册并添加初始路线
     * 3. Phase I 列生成循环
     * 4. Phase II 列生成循环
     * 5. 提取 LP 下界和路线变量值
     *
     * @param node 分支节点 / Branch node
     * @return 节点求解结果 / Node solve result
     */
    suspend fun solve(node: BranchNode): Ret<NodeSolveResult<V>> {
        val converter: IntoValue<Flt64> = IntoValue.fromConverter(Flt64)
        val model = LinearMetaModel<Flt64>(
            "node_${node.id}",
            converter = converter
        )
        val context = RouteCompilationContext(
            instance = instance,
            valueAdapter = valueAdapter,
            extraPipelines = extraPipelines
        )

        // 1. 注册 / Register
        when (val result = context.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 2. 生成并添加初始路线 / Generate and add initial routes
        val initialRouteGenerator = InitialRouteGenerator(
            instance = instance,
            valueAdapter = valueAdapter,
            distanceCalculator = distanceCalculator,
            travelTimeCalculator = travelTimeCalculator,
            arcCostCalculator = arcCostCalculator,
            routeCostPolicy = routeCostPolicy,
            arcFeasibilityPolicy = arcFeasibilityPolicy
        )
        val compatibleInheritedColumns = filterRouteColumnsForBranch(node.branchMask, inheritedColumns)
        val initialRoutes = initialRouteGenerator.generate(node.branchMask)
        val routesToAdd = compatibleInheritedColumns + initialRoutes
        if (routesToAdd.isNotEmpty()) {
            when (val result = context.addColumns(UInt64.zero, routesToAdd, model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        // 3. Phase I 列生成循环 / Phase I column generation loop
        val phaseOne = when (val phaseOneResult = runColumnGeneration(node, context, model, PricingPhase.PhaseOne)) {
            is Ok -> phaseOneResult.value
            is Failed -> return Failed(phaseOneResult.error)
            is Fatal -> return Fatal(phaseOneResult.errors)
        }
        if (phaseOne.solverStatus != null) {
            return Ok(NodeSolveResult(
                lowerBound = node.inheritedLowerBound,
                routeValues = emptyList(),
                isInteger = false,
                isFeasible = phaseOne.solverStatus.succeeded,
                pricingComplete = false,
                lpObjective = Flt64.infinity,
                iterations = phaseOne.iterations,
                solverStatus = phaseOne.solverStatus,
                columns = context.compilation.routes
            ))
        }
        if (!phaseOne.pricingComplete) {
            if (!phaseOne.interrupted) {
                return networkSchedulingFailure(
                    "节点 ${node.id} Phase I 定价未在 $maxCGIterations 轮内收敛 / " +
                        "Phase I pricing at node ${node.id} did not converge within $maxCGIterations iterations"
                )
            }
            return Ok(NodeSolveResult(
                lowerBound = node.inheritedLowerBound,
                routeValues = emptyList(),
                isInteger = false,
                isFeasible = true,
                pricingComplete = false,
                lpObjective = Flt64.infinity,
                iterations = phaseOne.iterations,
                timeLimitReached = true,
                columns = context.compilation.routes
            ))
        }

        // 检查 Phase I 是否收敛（无可行解） / Check Phase I convergence (infeasible?)
        val phaseOneConverged = when (val result = context.isPhaseOneConverged(model, instance.tolerances.feasibility)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (!phaseOneConverged) {
            node.markInfeasible()
            return Ok(NodeSolveResult(
                lowerBound = Flt64.infinity,
                routeValues = emptyList(),
                isInteger = false,
                isFeasible = false,
                pricingComplete = true,
                lpObjective = Flt64.infinity,
                iterations = phaseOne.iterations,
                columns = context.compilation.routes
            ))
        }

        // 4. 切换到 Phase II / Switch to Phase II
        when (val result = context.switchToPhaseTwo(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 5. Phase II 列生成循环 / Phase II column generation loop
        val phaseTwo = when (val phaseTwoResult = runColumnGeneration(node, context, model, PricingPhase.PhaseTwo)) {
            is Ok -> phaseTwoResult.value
            is Failed -> return Failed(phaseTwoResult.error)
            is Fatal -> return Fatal(phaseTwoResult.errors)
        }
        val totalNodeIterations = phaseOne.iterations + phaseTwo.iterations
        if (phaseTwo.solverStatus != null) {
            return Ok(NodeSolveResult(
                lowerBound = node.inheritedLowerBound,
                routeValues = emptyList(),
                isInteger = false,
                isFeasible = phaseTwo.solverStatus.succeeded,
                pricingComplete = false,
                lpObjective = Flt64.infinity,
                iterations = totalNodeIterations,
                solverStatus = phaseTwo.solverStatus,
                columns = context.compilation.routes
            ))
        }
        if (!phaseTwo.pricingComplete) {
            if (!phaseTwo.interrupted) {
                return networkSchedulingFailure(
                    "节点 ${node.id} Phase II 定价未在 $maxCGIterations 轮内收敛 / " +
                        "Phase II pricing at node ${node.id} did not converge within $maxCGIterations iterations"
                )
            }
            return Ok(NodeSolveResult(
                lowerBound = node.inheritedLowerBound,
                routeValues = emptyList(),
                isInteger = false,
                isFeasible = true,
                pricingComplete = false,
                lpObjective = Flt64.infinity,
                iterations = totalNodeIterations,
                timeLimitReached = true,
                columns = context.compilation.routes
            ))
        }

        // 6. 最终 LP 求解获取下界 / Final LP solve to get lower bound
        val finalLp = when (val result = solver.solveLPWithStatus("node_${node.id}_final", model)) {
            is Ok -> when (val output = result.value) {
                is ColumnGenerationSolver.LPResultWithStatus.Feasible -> output.result
                is ColumnGenerationSolver.LPResultWithStatus.Infeasible -> {
                    return Ok(NodeSolveResult(
                        lowerBound = Flt64.infinity,
                        routeValues = emptyList(),
                        isInteger = false,
                        isFeasible = false,
                        pricingComplete = true,
                        lpObjective = Flt64.infinity,
                        iterations = totalNodeIterations,
                        solverStatus = SolverStatus.Infeasible,
                        columns = context.compilation.routes
                    ))
                }
            }
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (finalLp.status != SolverStatus.Optimal) {
            return Ok(NodeSolveResult(
                lowerBound = node.inheritedLowerBound,
                routeValues = emptyList(),
                isInteger = false,
                isFeasible = finalLp.status.succeeded,
                pricingComplete = false,
                lpObjective = Flt64.infinity,
                iterations = totalNodeIterations,
                solverStatus = finalLp.status,
                columns = context.compilation.routes
            ))
        }
        model.setSolution(finalLp.solution)
        val lpObjective = finalLp.obj

        val nodeLowerBound = lpObjective

        // 标记节点已求解 / Mark node as solved
        node.solve(nodeLowerBound)

        // 7. 提取路线变量值 / Extract route variable values
        val routeValues = when (val result = context.extractRouteValues(model, instance.tolerances.integrality)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 8. 检查整数性 / Check integrality
        val isInteger = routeValues.all { (_, value) ->
            value.leq(instance.tolerances.integrality) ||
                value.geq(Flt64.one - instance.tolerances.integrality)
        }

        return Ok(NodeSolveResult(
            lowerBound = nodeLowerBound,
            routeValues = routeValues,
            isInteger = isInteger,
            isFeasible = true,
            pricingComplete = true,
            lpObjective = lpObjective,
            iterations = totalNodeIterations,
            columns = context.compilation.routes
        ))
    }

    /**
     * 执行列生成循环。 / Run column generation loop.
     *
     * @param node 分支节点 / Branch node
     * @param context 路线编译上下文 / Route compilation context
     * @param model 线性元模型 / Linear meta model
     * @param phase 当前阶段 / Current phase
     * @return 列生成结果 / Column-generation result
     */
    private suspend fun runColumnGeneration(
        node: BranchNode,
        context: RouteCompilationContext<V>,
        model: LinearMetaModel<Flt64>,
        phase: PricingPhase
    ): Ret<ColumnGenerationResult> {
        val graphBuilder = RouteGraphBuilder(
            instance = instance,
            valueAdapter = valueAdapter,
            distanceCalculator = distanceCalculator,
            travelTimeCalculator = travelTimeCalculator,
            arcCostCalculator = arcCostCalculator,
            arcFeasibilityPolicy = arcFeasibilityPolicy
        )
        val pricer = EspprcPricer(
            instance = instance,
            valueAdapter = valueAdapter,
            dominancePolicy = labelDominancePolicy
        )

        var iteration = UInt64.one
        val phaseLabel = if (phase == PricingPhase.PhaseOne) "phase1" else "phase2"

        while (iteration.toInt() <= maxCGIterations) {
            if (interruptionChecker.shouldStop()) {
                return ok(ColumnGenerationResult(false, iteration.toInt() - 1, true))
            }
            // 求解 LP / Solve LP
            val lpResult = when (val result = solver.solveLPWithStatus("node_${node.id}_${phaseLabel}_$iteration", model)) {
                is Ok -> when (val output = result.value) {
                    is ColumnGenerationSolver.LPResultWithStatus.Feasible -> output.result
                    is ColumnGenerationSolver.LPResultWithStatus.Infeasible -> {
                        return ok(ColumnGenerationResult(
                            pricingComplete = false,
                            iterations = iteration.toInt() - 1,
                            interrupted = false,
                            solverStatus = SolverStatus.Infeasible
                        ))
                    }
                }
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (lpResult.status != SolverStatus.Optimal) {
                return ok(ColumnGenerationResult(
                    pricingComplete = false,
                    iterations = iteration.toInt() - 1,
                    interrupted = false,
                    solverStatus = lpResult.status
                ))
            }
            model.setSolution(lpResult.solution)

            // Phase I 的目标是非负人工变量之和；目标达到 0 时已取得全局最优，
            // 无需继续定价证明不存在更低目标的路线列。
            // The Phase-I objective is the sum of non-negative artificial variables.
            // Once it reaches zero, global optimality is proven without further pricing.
            if (phase == PricingPhase.PhaseOne &&
                lpResult.obj.leq(instance.tolerances.feasibility)
            ) {
                return ok(ColumnGenerationResult(true, iteration.toInt(), false))
            }

            // 提取对偶 / Extract duals
            val metaDual = lpResult.dualSolution.toMeta()
            val duals = when (val result = context.extractPricingDuals(model, metaDual)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            // 对每个车辆类型执行定价 / Price for each vehicle type
            val newRoutes = mutableListOf<Route<V>>()
            var allComplete = true

            for (vehicleType in instance.vehicleTypes) {
                val graph = when (val result = graphBuilder.build(
                    vehicleTypeId = vehicleType.id,
                    duals = duals,
                    branchMask = node.branchMask
                )) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                val request = PricingRequest(
                    instance = instance,
                    duals = duals,
                    branchMask = node.branchMask,
                    pricingTolerance = instance.tolerances.pricing,
                    maxColumnsPerPricing = maxColumnsPerPricing,
                    vehicleTypeId = vehicleType.id,
                    interruptionChecker = interruptionChecker
                )

                val pricingResult = when (val result = pricer.price(graph, request)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                newRoutes.addAll(pricingColumnSelector.select(
                    columns = pricingResult.routes,
                    maxColumns = maxColumnsPerPricing
                ))
                if (pricingResult.interrupted) {
                    return ok(ColumnGenerationResult(false, iteration.toInt(), true))
                }
                if (!pricingResult.exactPricingComplete) {
                    allComplete = false
                }
            }

            // 若无新列且所有车辆类型定价完成 → 收敛 / No new columns and all complete -> converged
            if (newRoutes.isEmpty() && allComplete) {
                return ok(ColumnGenerationResult(true, iteration.toInt(), false))
            }

            // 添加新列 / Add new columns
            if (newRoutes.isNotEmpty()) {
                when (val result = context.addColumns(iteration, newRoutes, model)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }

            iteration++
        }

        // 达到最大迭代次数，定价未完成（中断） / Max iterations reached, pricing incomplete (interrupted)
        return ok(ColumnGenerationResult(false, maxCGIterations, false))
    }
}

/**
 * 过滤与分支遮罩兼容的既有路线列。 / Filter existing route columns compatible with a branch mask.
 */
internal fun <V : RealNumber<V>> filterRouteColumnsForBranch(
    branchMask: BranchMask<VehicleTypeId>,
    columns: List<Route<V>>
): List<Route<V>> {
    return columns.filter { route ->
        branchMask.isRouteCompatible(
            resourceKey = route.vehicleTypeId,
            path = route.stops.map { it.nodeId }
        )
    }
}
