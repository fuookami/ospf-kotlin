package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkArcId
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/** 已注册广义流弧表达式的只读视图。 / Read-only view of registered generalized-flow arc expressions.
 *
 * @property inputExpressions 起点单位下的弧投入表达式 / Arc input expressions in origin-node units
 * @property outputExpressions 终点单位下的到达产出表达式 / Arrival-output expressions in destination-node units
 */
class GeneralizedFlowExpressions internal constructor(
    inputExpressions: Map<NetworkArcId, LinearPolynomial<Flt64>>,
    outputExpressions: Map<NetworkArcId, LinearPolynomial<Flt64>>
) {
    private val inputExpressions = inputExpressions.toMap()
    private val outputExpressions = outputExpressions.toMap()

    /** 读取弧投入表达式。 / Get an arc input expression.
     *
     * @param arcId 稳定弧标识 / Stable arc identity
     * @return 起点单位下的投入表达式或缺失错误 / Origin-unit input expression or a missing-arc error
     */
    fun input(arcId: NetworkArcId): Ret<LinearPolynomial<Flt64>> {
        return inputExpressions[arcId]?.let(::ok)
            ?: networkSchedulingFailure("广义流投入弧未注册：$arcId / Generalized-flow input arc is not registered: $arcId")
    }

    /** 读取弧到达产出表达式。 / Get an arc arrival-output expression.
     *
     * @param arcId 稳定弧标识 / Stable arc identity
     * @return 终点单位下的产出表达式或缺失错误 / Destination-unit output expression or a missing-arc error
     */
    fun output(arcId: NetworkArcId): Ret<LinearPolynomial<Flt64>> {
        return outputExpressions[arcId]?.let(::ok)
            ?: networkSchedulingFailure("广义流产出弧未注册：$arcId / Generalized-flow output arc is not registered: $arcId")
    }
}
