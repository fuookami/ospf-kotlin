package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline

/** 使用已注册的广义流表达式创建业务扩展管线。 / Create a business extension pipeline over registered generalized-flow expressions.
 *
 * @param V 领域数值类型 / Domain numeric type
 */
fun interface GeneralizedFlowPipelineFactory<V : RealNumber<V>> {
    /** 创建下游扩展管线。 / Create a downstream extension pipeline.
     *
     * @param expressions 只读投入/产出表达式 / Read-only input/output expressions
     * @return 扩展约束或目标管线 / Constraint or objective extension pipeline
     */
    operator fun invoke(
        expressions: GeneralizedFlowExpressions
    ): Pipeline<AbstractLinearMetaModel<Flt64>>
}
