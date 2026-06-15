package fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.service.pipeline

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.LengthAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation
import fuookami.ospf.kotlin.framework.model.Pipeline

/**
 * 长度分配目标管线 / Length assignment objective pipeline
 *
 * 在目标函数中添加长度相关惩罚项：
 * - 总卷长惩罚: sum(totalLengthPenalty * assigned_length_i)
 * - 超长惩罚: sum(overLengthPenalty * over_length_i)
 * - 批次最小惩罚: 对 Σx_j 施加额外加权
 *
 * Add length-related penalty terms to the objective function.
 *
 * @param V 数值类型 / Numeric value type
 * @property produce 产出聚合 / Produce aggregation
 * @property length 长度分配聚合 / Length assignment aggregation
 * @property config 长度分配建模配置 / Length assignment modeling configuration
 */
class LengthObjectivePipeline<V : RealNumber<V>>(
    private val produce: ProduceAggregation<V>,
    private val length: LengthAggregation<V>,
    private val config: LengthAssignmentModelingConfig<V>
) : Pipeline<LinearMetaModel<Flt64>> {

    override val name: String = "length_objective"

    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        // length 目标项由 Csp1dProduceContext 统一组装到目标函数中
        return ok
    }

    /**
     * 生成目标项单项式 / Generate objective monomials
     */
    fun objectiveMonomials(): List<LinearMonomial<Flt64>> {
        val monomials = ArrayList<LinearMonomial<Flt64>>()

        // 总卷长惩罚: sum(totalLengthPenalty * assigned_length_i)
        val totalLengthPenalty = config.totalLengthPenalty
        if (totalLengthPenalty != null) {
            for ((demandIndex, demand) in length.demands.withIndex()) {
                val assignedVar = length.assignedLength.getOrNull(demandIndex) ?: continue
                monomials.add(LinearMonomial(totalLengthPenalty.toFlt64(), assignedVar))
            }
        }

        // 超长惩罚: sum(overLengthPenalty[productId] * over_length_i)
        if (config.overLengthPenalty.isNotEmpty()) {
            for ((demandIndex, demand) in length.demands.withIndex()) {
                val overVar = length.overLength.getOrNull(demandIndex) ?: continue
                val penalty = config.overLengthPenalty[demand.product.id] ?: continue
                monomials.add(LinearMonomial(penalty.toFlt64(), overVar))
            }
        }

        return monomials
    }

    /**
     * 获取批次系数 / Get batch coefficient
     *
     * 当 batchMinPenalty 非空时，对 Σx_j 施加额外加权。
     * Apply extra weight to Σx_j when batchMinPenalty is present.
     */
    fun batchCoefficient(): Flt64 {
        val batchMinPenalty = config.batchMinPenalty ?: return Flt64.one
        return Flt64.one + batchMinPenalty.toFlt64()
    }
}
