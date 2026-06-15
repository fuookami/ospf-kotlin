package fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.service.pipeline

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.wasting_minimization.WasteAggregation
import fuookami.ospf.kotlin.framework.model.Pipeline

/**
 * 浪费最小化目标管线 / Waste minimization objective pipeline
 *
 * 在目标函数中添加浪费相关惩罚项：
 * - 余宽惩罚: sum(restWidth * trimPenalty * x_j)
 * - 余料面积代理惩罚: sum(restMaterialValue * restPenalty * x_j)
 * - 物料成本惩罚: sum(materialCostPenalty[materialId] * x_j)
 * - 超产面积代理惩罚: sum(overArea * overAreaPenalty)
 *
 * Add waste-related penalty terms to the objective function.
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 产出聚合 / Produce aggregation
 * @property waste 浪费聚合 / Waste aggregation
 * @property demands 需求列表 / Demand list
 * @property overProductionVars 超产松弛变量列表（超产面积惩罚需要）/ Over-production slack variables (needed for over-production area penalty)
 * @property overProductionAreaMeasure 超产面积度量口径 / Over-production area measure policy
 * @property restMaterialMeasure 余料度量口径 / Rest material measure policy
 */
class WasteObjectivePipeline<V : RealNumber<V>>(
    private val produce: ProduceAggregation<V>,
    private val waste: WasteAggregation<V>,
    private val demands: List<ProductDemand<V>>,
    private val overProductionVars: List<URealVar?> = emptyList(),
    private val overProductionAreaMeasure: OverProductionAreaMeasure = OverProductionAreaMeasure.ProductMaxWidthProxy,
    private val restMaterialMeasure: RestMaterialMeasure = RestMaterialMeasure.RestWidthByMaterialLengthProxy
) : Pipeline<LinearMetaModel<Flt64>> {

    override val name: String = "waste_objective"

    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        // waste 目标项由 Csp1dProduceContext 统一组装到目标函数中
        return ok
    }

    /**
     * 生成目标项单项式 / Generate objective monomials
     */
    fun objectiveMonomials(): List<LinearMonomial<Flt64>> {
        val monomials = ArrayList<LinearMonomial<Flt64>>()

        // 余宽惩罚: sum(restWidth * trimPenalty * x_j)
        val trimPenalty = waste.trimWidthPenalty
        if (trimPenalty != null) {
            for (index in 0 until produce.planCount) {
                val plan = produce.cuttingPlans[index]
                val restWidthValue = plan.restWidth?.value ?: continue
                if (restWidthValue > restWidthValue.constants.zero) {
                    val coeff = restWidthValue.toFlt64() * trimPenalty.toFlt64()
                    monomials.add(LinearMonomial(coeff, produce[index]))
                }
            }
        }

        // 余料面积代理惩罚: sum(restMaterialValue * restPenalty * x_j)
        val restMaterialPenalty = waste.restMaterialPenalty
        if (restMaterialPenalty != null) {
            for (index in 0 until produce.planCount) {
                val plan = produce.cuttingPlans[index]
                val restMaterialValue = restMaterialValue(plan, restMaterialMeasure) ?: continue
                val coeff = restMaterialValue.toFlt64() * restMaterialPenalty.toFlt64()
                monomials.add(LinearMonomial(coeff, produce[index]))
            }
        }

        // 物料成本惩罚: sum(materialCostPenalty[materialId] * x_j)
        if (waste.materialCostPenalty.isNotEmpty()) {
            for (index in 0 until produce.planCount) {
                val plan = produce.cuttingPlans[index]
                val costPenalty = waste.materialCostPenalty[plan.material.id]
                if (costPenalty != null) {
                    monomials.add(LinearMonomial(costPenalty.toFlt64(), produce[index]))
                }
            }
        }

        // 超产面积代理惩罚: sum(overArea * overAreaPenalty)
        val overAreaPenalty = waste.overProductionAreaPenalty
        if (overAreaPenalty != null) {
            for ((demandIndex, demand) in demands.withIndex()) {
                val overVar = overProductionVars.getOrNull(demandIndex) ?: continue
                val productWidthValue = overProductionAreaWidthValue(demand, overProductionAreaMeasure) ?: continue
                val coeff = productWidthValue.toFlt64() * overAreaPenalty.toFlt64()
                monomials.add(LinearMonomial(coeff, overVar))
            }
        }

        return monomials
    }

    private fun restMaterialValue(
        plan: CuttingPlan<V>,
        measure: RestMaterialMeasure
    ): V? {
        val restWidth = plan.restWidth ?: return null
        return when (measure) {
            RestMaterialMeasure.RestWidthByMaterialLengthProxy -> {
                val materialLength = plan.material.length ?: return null
                (restWidth.value * materialLength.value)
            }
        }
    }

    private fun overProductionAreaWidthValue(
        demand: ProductDemand<V>,
        measure: OverProductionAreaMeasure
    ): V? {
        return when (measure) {
            OverProductionAreaMeasure.ProductMaxWidthProxy -> {
                demand.product.maxWidth()?.value
            }
        }
    }
}

/**
 * 超产面积度量口径 / Over-production area measure policy
 */
enum class OverProductionAreaMeasure {
    /** 使用产品最大宽度代理 / Use product max width proxy */
    ProductMaxWidthProxy
}

/**
 * 余料度量口径 / Rest material measure policy
 */
enum class RestMaterialMeasure {
    /** 使用余宽乘物料长度代理 / Use rest width by material length proxy */
    RestWidthByMaterialLengthProxy
}
