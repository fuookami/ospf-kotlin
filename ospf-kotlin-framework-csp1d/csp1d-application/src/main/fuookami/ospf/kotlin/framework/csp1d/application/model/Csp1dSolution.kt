package fuookami.ospf.kotlin.framework.csp1d.application.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.toRenderDto
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingResult
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingResult
import fuookami.ospf.kotlin.framework.csp1d.application.service.WasteMinimizationResult
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto.RenderCuttingPlanDTO
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto.RenderCuttingPlanProductionDTO
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto.RenderProductionType
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto.RenderSchemaDTO
import fuookami.ospf.kotlin.quantities.quantity.toFltX

/**
 * CSP1D KPI / CSP1D KPI
 *
 * @property selectedPlanCount 选中方案数量 / Selected plan count
 * @property selectedBatchCount 选中车次数量 / Selected batch count
 * @property unmetDemandCount 未满足需求数量 / Unmet demand count
 * @property materialUsageCount 物料使用条目数 / Material usage entry count
 * @property machineUsageCount 设备使用条目数 / Machine usage entry count
 * @property generatedPlanCount 本轮生成方案总数 / Generated plan count
 */
data class Csp1dKpi(
    val selectedPlanCount: UInt64,
    val selectedBatchCount: UInt64,
    val unmetDemandCount: UInt64,
    val materialUsageCount: UInt64,
    val machineUsageCount: UInt64,
    val generatedPlanCount: UInt64
)

/**
 * CSP1D 求解结果 / CSP1D solution
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 主问题结果 / Master problem output
 * @property generatedPlans 切割方案池 / Generated cutting plans
 * @property kpi KPI / KPI
 * @property render 渲染输出 / Render output
 */
data class Csp1dSolution<V : RealNumber<V>>(
    val produce: Produce<V>,
    val yieldResult: YieldModelingResult<V>? = null,
    val wasteResult: WasteMinimizationResult<V>? = null,
    val lengthResult: LengthAssignmentModelingResult<V>? = null,
    val generatedPlans: List<CuttingPlan<V>>,
    val kpi: Csp1dKpi,
    val render: RenderSchemaDTO
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
        val kpi = Csp1dKpi(
            selectedPlanCount = UInt64(produce.cuttingPlans.size),
            selectedBatchCount = selectedBatchCount,
            unmetDemandCount = UInt64(produce.unmetDemands.size),
            materialUsageCount = UInt64(produce.materialUsages.size),
            machineUsageCount = UInt64(produce.machineUsages.size),
            generatedPlanCount = UInt64(generatedPlans.size)
        )
        val render = RenderSchemaDTO(
            kpi = mapOf(
                "selectedPlanCount" to kpi.selectedPlanCount.toString(),
                "selectedBatchCount" to kpi.selectedBatchCount.toString(),
                "unmetDemandCount" to kpi.unmetDemandCount.toString(),
                "materialUsageCount" to kpi.materialUsageCount.toString(),
                "machineUsageCount" to kpi.machineUsageCount.toString(),
                "generatedPlanCount" to kpi.generatedPlanCount.toString()
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

