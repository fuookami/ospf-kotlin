package fuookami.ospf.kotlin.framework.csp1d.application.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.toFltX
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto.RenderCuttingPlanDTO
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto.RenderCuttingPlanProductionDTO
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto.RenderProductionType
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto.RenderSchemaDTO
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.toRenderDto
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.csp1d.application.service.WasteMinimizationResult

/**
 * CSP1D 解状态 / CSP1D solution status
 */
enum class Csp1dSolutionStatus {
    /** 已得到最终 MILP 解 / Final MILP solution is available */
    Feasible,
    /** 只有方案池或中间结果可用 / Only plan pool or intermediate result is available */
    Partial,
    /** 无初始方案 / No initial plans */
    NoInitialPlans,
    /** 求解失败且无可用部分结果 / Solve failed without usable partial result */
    Failed
}

/**
 * CSP1D KPI 稳定字段名 / Stable CSP1D KPI keys
 */
object Csp1dKpiKeys {
    const val SelectedPlanCount = "selectedPlanCount"
    const val SelectedBatchCount = "selectedBatchCount"
    const val SatisfiedDemandCount = "satisfiedDemandCount"
    const val UnmetDemandCount = "unmetDemandCount"
    const val MaterialUsageCount = "materialUsageCount"
    const val MachineUsageCount = "machineUsageCount"
    const val GeneratedPlanCount = "generatedPlanCount"
    const val TopPlanCount = "topPlanCount"
    const val YieldMetricCount = "yieldMetricCount"
    const val WasteMetricCount = "wasteMetricCount"
    const val LengthMetricCount = "lengthMetricCount"
    const val SolutionStatus = "solutionStatus"
    const val TerminationReason = "terminationReason"
    const val FinalMilpStatus = "finalMilpStatus"
    const val PartialSolutionAvailable = "partialSolutionAvailable"
    const val FailureMessage = "failureMessage"
    const val ColumnGenerationTerminationReason = "columnGeneration.terminationReason"
    const val ColumnGenerationIterationCount = "columnGeneration.iterationCount"
    const val ColumnGenerationPricedPlanCount = "columnGeneration.pricedPlanCount"
    const val ColumnGenerationLastLpObjective = "columnGeneration.lastLpObjective"
    const val ColumnGenerationLastPlanCount = "columnGeneration.lastPlanCount"
    const val InitialGenerationVisitedNodes = "initialGeneration.visitedNodes"
    const val InitialGenerationGeneratedCandidates = "initialGeneration.generatedCandidates"
    const val InitialGenerationAcceptedPlans = "initialGeneration.acceptedPlans"
    const val InitialGenerationInfeasibleCandidates = "initialGeneration.infeasibleCandidates"
    const val InitialGenerationDuplicateCandidates = "initialGeneration.duplicateCandidates"
    const val InitialGenerationDominatedCandidates = "initialGeneration.dominatedCandidates"
    const val InitialGenerationWidthBoundPrunedNodes = "initialGeneration.widthBoundPrunedNodes"
    const val InitialGenerationLengthBoundPrunedEntries = "initialGeneration.lengthBoundPrunedEntries"
    const val InitialGenerationMaterialWidthIndexCacheHits = "initialGeneration.materialWidthIndexCacheHits"
    const val InitialGenerationElapsedMilliseconds = "initialGeneration.elapsedMilliseconds"
    const val InitialGenerationStopReason = "initialGeneration.stopReason"
    const val InitialVisitedNodes = "initialVisitedNodes"
    const val InitialGeneratedCandidates = "initialGeneratedCandidates"
    const val InitialAcceptedPlans = "initialAcceptedPlans"
    const val InitialInfeasibleCandidates = "initialInfeasibleCandidates"
    const val InitialDuplicateCandidates = "initialDuplicateCandidates"
    const val InitialDominatedCandidates = "initialDominatedCandidates"
    const val InitialWidthBoundPrunedNodes = "initialWidthBoundPrunedNodes"
    const val InitialLengthBoundPrunedEntries = "initialLengthBoundPrunedEntries"
    const val InitialMaterialWidthIndexCacheHits = "initialMaterialWidthIndexCacheHits"
    const val InitialGenerationElapsedMillisecondsRender = "initialGenerationElapsedMilliseconds"
    const val InitialGenerationStopReasonRender = "initialGenerationStopReason"
    const val TotalTrimWidth = "totalTrimWidth"
    const val TotalRestMaterial = "totalRestMaterial"
    const val OverProductionArea = "overProductionArea"
    const val OverProductionAreaMeasure = "overProductionAreaMeasure"
    const val RestMaterialMeasure = "restMaterialMeasure"

    /**
     * 物料使用批次数 key / Material usage batch-count key
     *
     * @param materialId 物料 ID / Material id
     * @return KPI key / KPI key
     */
    fun materialUsageBatchCount(materialId: String): String {
        return "materialUsage.$materialId.batchCount"
    }

    /**
     * 设备产能使用 key / Machine capacity usage key
     *
     * @param machineId 设备 ID / Machine id
     * @return KPI key / KPI key
     */
    fun machineCapacityUsed(machineId: String): String {
        return "machineCapacityUsed.$machineId"
    }

    /**
     * 欠产 key / Under-production key
     *
     * @param productId 产品 ID / Product id
     * @param unitSymbol 需求单位符号 / Demand unit symbol
     * @return KPI key / KPI key
     */
    fun underProduction(productId: String, unitSymbol: String): String {
        return "underProduction.$productId.$unitSymbol"
    }

    /**
     * 超产 key / Over-production key
     *
     * @param productId 产品 ID / Product id
     * @param unitSymbol 需求单位符号 / Demand unit symbol
     * @return KPI key / KPI key
     */
    fun overProduction(productId: String, unitSymbol: String): String {
        return "overProduction.$productId.$unitSymbol"
    }

    /**
     * 物料成本 key / Material cost key
     *
     * @param materialId 物料 ID / Material id
     * @return KPI key / KPI key
     */
    fun materialCost(materialId: String): String {
        return "materialCost.$materialId"
    }

    /**
     * 分配长度 key / Assigned length key
     *
     * @param productId 产品 ID / Product id
     * @return KPI key / KPI key
     */
    fun assignedLength(productId: String): String {
        return "assignedLength.$productId"
    }

    /**
     * 超长 key / Over-length key
     *
     * @param productId 产品 ID / Product id
     * @return KPI key / KPI key
     */
    fun overLength(productId: String): String {
        return "overLength.$productId"
    }
}

/**
 * CSP1D KPI / CSP1D KPI
 *
 * @property selectedPlanCount 选中方案数量 / Selected plan count
 * @property selectedBatchCount 选中车次数量 / Selected batch count
 * @property satisfiedDemandCount 已满足需求数量 / Satisfied demand count
 * @property unmetDemandCount 未满足需求数量 / Unmet demand count
 * @property materialUsageCount 物料使用条目数 / Material usage entry count
 * @property machineUsageCount 设备使用条目数 / Machine usage entry count
 * @property generatedPlanCount 本轮生成方案总数 / Generated plan count
 * @property topPlanCount Top-K 方案数量 / Top-K plan count
 * @property yieldMetricCount yield 回填指标数 / Yield metric count
 * @property wasteMetricCount waste 回填指标数 / Waste metric count
 * @property lengthMetricCount length 回填指标数 / Length metric count
 * @property details 可序列化 KPI 明细 / Serializable KPI details
 */
data class Csp1dKpi(
    val selectedPlanCount: UInt64,
    val selectedBatchCount: UInt64,
    val satisfiedDemandCount: UInt64,
    val unmetDemandCount: UInt64,
    val materialUsageCount: UInt64,
    val machineUsageCount: UInt64,
    val generatedPlanCount: UInt64,
    val topPlanCount: UInt64 = UInt64.zero,
    val yieldMetricCount: UInt64 = UInt64.zero,
    val wasteMetricCount: UInt64 = UInt64.zero,
    val lengthMetricCount: UInt64 = UInt64.zero,
    val details: Map<String, String> = emptyMap()
)

/**
 * CSP1D 求解结果 / CSP1D solution
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 主问题结果 / Master problem output
 * @property generatedPlans 切割方案池 / Generated cutting plans
 * @property kpi KPI / KPI
 * @property render 渲染输出 / Render output
 * @property status 解状态 / Solution status
 * @property failureMessage 失败信息 / Failure message
 * @property topPlans Top-K 方案 / Top-K plans
 */
data class Csp1dSolution<V : RealNumber<V>>(
    val produce: Produce<V>,
    val yieldResult: YieldModelingResult<V>? = null,
    val wasteResult: WasteMinimizationResult<V>? = null,
    val lengthResult: LengthAssignmentModelingResult<V>? = null,
    val generatedPlans: List<CuttingPlan<V>>,
    val kpi: Csp1dKpi,
    val render: RenderSchemaDTO,
    val status: Csp1dSolutionStatus = Csp1dSolutionStatus.Feasible,
    val failureMessage: String? = null,
    val topPlans: List<CuttingPlan<V>> = emptyList()
)

/**
 * CSP1D 解分析器 / CSP1D solution analyzer
 *
 * @param V 数值类型 / Numeric value type
 */
fun interface Csp1dSolutionAnalyzer<V : RealNumber<V>> {
    /**
     * 分析并组装解 / Analyze and build solution
     *
     * @param problem 原问题 / Original problem
     * @param produce 主问题结果 / Master problem output
     * @param generatedPlans 切割方案池 / Generated cutting plans
     * @return CSP1D 解 / CSP1D solution
     */
    fun analyze(
        problem: Csp1dProblem<V>,
        produce: Produce<V>,
        generatedPlans: List<CuttingPlan<V>>
    ): Csp1dSolution<V>
}

/**
 * 默认解分析器 / Default solution analyzer
 *
 * @param V 数值类型 / Numeric value type
 */
class DefaultCsp1dSolutionAnalyzer<V : RealNumber<V>> : Csp1dSolutionAnalyzer<V> {
    override fun analyze(
        problem: Csp1dProblem<V>,
        produce: Produce<V>,
        generatedPlans: List<CuttingPlan<V>>
    ): Csp1dSolution<V> {
        val selectedBatchCount = produce.cuttingPlans.fold(UInt64.zero) { acc, usage ->
            acc + usage.amount
        }
        val satisfiedDemandCount = UInt64(problem.demands.size - produce.unmetDemands.size)
        val kpi = Csp1dKpi(
            selectedPlanCount = UInt64(produce.cuttingPlans.size),
            selectedBatchCount = selectedBatchCount,
            satisfiedDemandCount = satisfiedDemandCount,
            unmetDemandCount = UInt64(produce.unmetDemands.size),
            materialUsageCount = UInt64(produce.materialUsages.size),
            machineUsageCount = UInt64(produce.machineUsages.size),
            generatedPlanCount = UInt64(generatedPlans.size)
        )
        val render = RenderSchemaDTO(
            kpi = mapOf(
                Csp1dKpiKeys.SelectedPlanCount to kpi.selectedPlanCount.toString(),
                Csp1dKpiKeys.SelectedBatchCount to kpi.selectedBatchCount.toString(),
                Csp1dKpiKeys.SatisfiedDemandCount to kpi.satisfiedDemandCount.toString(),
                Csp1dKpiKeys.UnmetDemandCount to kpi.unmetDemandCount.toString(),
                Csp1dKpiKeys.MaterialUsageCount to kpi.materialUsageCount.toString(),
                Csp1dKpiKeys.MachineUsageCount to kpi.machineUsageCount.toString(),
                Csp1dKpiKeys.GeneratedPlanCount to kpi.generatedPlanCount.toString(),
                Csp1dKpiKeys.TopPlanCount to kpi.topPlanCount.toString(),
                Csp1dKpiKeys.YieldMetricCount to kpi.yieldMetricCount.toString(),
                Csp1dKpiKeys.WasteMetricCount to kpi.wasteMetricCount.toString(),
                Csp1dKpiKeys.LengthMetricCount to kpi.lengthMetricCount.toString()
            ),
            cuttingPlans = produce.cuttingPlans.map { usage ->
                renderCuttingPlan(
                    plan = usage.plan,
                    amount = usage.amount
                )
            }
        )
        return Csp1dSolution(
            produce = produce,
            generatedPlans = generatedPlans,
            kpi = kpi,
            render = render
        )
    }

    private fun renderCuttingPlan(
        plan: CuttingPlan<V>,
        amount: UInt64
    ): RenderCuttingPlanDTO {
        val productions = ArrayList<RenderCuttingPlanProductionDTO>()
        var cursor = FltX.zero
        for (slice in plan.slices) {
            val width = slice.width.toFltX().value
            val productionDto = toRenderProductionDto(
                production = slice.production,
                x = cursor,
                width = width,
                amount = slice.amount
            )
            productions.add(productionDto)
            cursor += width
        }
        return RenderCuttingPlanDTO(
            group = listOf(
                plan.material.name,
                plan.machineId ?: "unassigned-machine"
            ),
            productions = productions,
            width = plan.usedWidth?.toFltX()?.value ?: FltX.zero,
            standardWidth = plan.material.widthRange.upperBound.toFltX().value,
            amount = amount,
            info = mapOf(
                "planId" to plan.id
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun toRenderProductionDto(
        production: Any,
        x: FltX,
        width: FltX,
        amount: UInt64
    ): RenderCuttingPlanProductionDTO {
        return when (production) {
            is Product<*> -> {
                val product = production as Product<V>
                product.toRenderDto(
                    x = x,
                    productionType = RenderProductionType.Product,
                    info = mapOf(
                        "amount" to amount.toString()
                    )
                )
            }

            is Costar<*> -> {
                val costar = production as Costar<V>
                RenderCuttingPlanProductionDTO(
                    name = costar.name,
                    x = x,
                    width = width,
                    unitLength = costar.length?.toFltX()?.value,
                    productionType = RenderProductionType.Costar,
                    info = mapOf(
                        "amount" to amount.toString()
                    )
                )
            }

            else -> {
                RenderCuttingPlanProductionDTO(
                    name = "unknown-production",
                    x = x,
                    width = width,
                    unitLength = null,
                    productionType = RenderProductionType.Product,
                    info = mapOf(
                        "amount" to amount.toString()
                    )
                )
            }
        }
    }
}

