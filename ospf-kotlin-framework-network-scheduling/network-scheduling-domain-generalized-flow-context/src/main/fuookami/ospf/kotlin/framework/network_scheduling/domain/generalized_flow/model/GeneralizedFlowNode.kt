package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 带计量单位和硬净供需的广义流节点。 / Generalized-flow node with a measurement unit and hard net supply.
 *
 * @property networkNode 通用网络节点 / Generic network node
 * @property unit 节点投入/产出计量单位 / Measurement unit for node inputs and outputs
 * @property netSupply 正值供给、负值需求 / Positive supply and negative demand
 * @property id 稳定节点标识 / Stable node identity
 */
class GeneralizedFlowNode<V : RealNumber<V>> private constructor(
    val networkNode: NetworkNode<V>,
    val unit: PhysicalUnit,
    val netSupply: Quantity<V>
) {
    /** 稳定节点标识。 / Stable node identity. */
    val id: NetworkNodeId get() = networkNode.id

    companion object {
        /** 创建广义流节点。 / Create a generalized-flow node.
         *
         * @param networkNode 网络节点 / Network node
         * @param unit 节点计量单位 / Node measurement unit
         * @param netSupply 净供需量，正值供给、负值需求 / Net balance; positive is supply and negative is demand
         * @return 节点或量纲错误 / Node or dimension error
         */
        operator fun <V : RealNumber<V>> invoke(
            networkNode: NetworkNode<V>,
            unit: PhysicalUnit,
            netSupply: Quantity<V>
        ): Ret<GeneralizedFlowNode<V>> {
            if (!netSupply.unit.sameDimension(unit)) {
                return networkSchedulingFailure(
                    "广义流节点净供需与计量单位量纲不一致 / Generalized-flow node balance dimension does not match its measurement unit"
                )
            }
            return ok(GeneralizedFlowNode(
                networkNode = networkNode,
                unit = unit,
                netSupply = netSupply
            ))
        }
    }
}
