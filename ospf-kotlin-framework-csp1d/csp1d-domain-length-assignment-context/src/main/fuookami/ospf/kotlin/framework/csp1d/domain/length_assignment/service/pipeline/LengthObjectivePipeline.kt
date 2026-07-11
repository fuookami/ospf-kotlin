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
 * Length assignment objective pipeline.
 * 长度分配目标管线
 *
 * Add length-related penalty terms to the objective function:
 * - Total length penalty: sum(totalLengthPenalty * assigned_length_i)
 * - Over-length penalty: sum(overLengthPenalty * over_length_i)
 * - Batch minimization penalty: extra weight on Σx_j
 *
 * 在目标函数中添加长度相关惩罚项：
 * - 总卷长惩罚: sum(totalLengthPenalty * assigned_length_i)
 * - 超长惩罚: sum(overLengthPenalty * over_length_i)
 * - 批次最小惩罚: 对 Σx_j 施加额外加权
 *
 * @param V Numeric value type / 数值类型
 * @property produce Produce aggregation / 产出聚合
 * @property length Length assignment aggregation / 长度分配聚合
 * @property config Length assignment modeling configuration / 长度分配建模配置
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
     * Generate objective monomials.
     * 生成目标项单项式
     *
     * @return List of linear monomials representing objective penalty terms / 表示目标惩罚项的线性单项式列表
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
     * Get batch coefficient for the objective function.
     * 获取目标函数的批次系数
     *
     * When batchMinPenalty is present, extra weight is applied to Σx_j.
     * 当 batchMinPenalty 非空时，对 Σx_j 施加额外加权。
     *
     * @return Batch coefficient for Σx_j / Σx_j 的批次系数
    */
    fun batchCoefficient(): Flt64 {
        val batchMinPenalty = config.batchMinPenalty ?: return Flt64.one
        return Flt64.one + batchMinPenalty.toFlt64()
    }
}
