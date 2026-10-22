package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 网络流节点。
 * / Network-flow node.
 * @property networkNode 通用网络节点 / Generic network node
 * @property balances 按商品标识索引的供需 / Balances indexed by commodity identity
 * @property id 稳定节点标识 / Stable node identity
 * @property attributes 节点属性 / Node attributes
 */
class FlowNode<V : RealNumber<V>> private constructor(
    val networkNode: NetworkNode<V>,
    balances: Map<String, NodeBalance<V>>
) {
    /** 稳定节点标识。 / Stable node identifier. */
    val id: NetworkNodeId get() = networkNode.id

    /** 节点属性。 / Node attributes. */
    val attributes: Map<String, Quantity<V>> get() = networkNode.attributes

    /** 按商品 ID 索引的节点供需快照。 / Node-balance snapshot indexed by commodity ID. */
    val balances: Map<String, NodeBalance<V>> = balances.toMap()

    /** 获取指定商品的节点供需。 / Get the node balance for a commodity.
     * @param commodityId 商品标识 / Commodity identity
     * @return 节点供需，未声明时为空 / Node balance, or null when absent
     */
    fun balanceOf(commodityId: String): NodeBalance<V>? = balances[commodityId]

    companion object {
        /** 包装通用网络节点。 / Wrap a generic network node.
         * @param networkNode 通用网络节点 / Generic network node
         * @param balances 按商品标识索引的供需 / Balances indexed by commodity identity
         * @return 流节点 / Flow node
         */
        operator fun <V : RealNumber<V>> invoke(
            networkNode: NetworkNode<V>,
            balances: Map<String, NodeBalance<V>> = emptyMap()
        ): Ret<FlowNode<V>> {
            return ok(FlowNode(networkNode, balances))
        }

        internal fun <V : RealNumber<V>> wrap(
            networkNode: NetworkNode<V>,
            balances: Map<String, NodeBalance<V>> = emptyMap()
        ): FlowNode<V> {
            return FlowNode(networkNode, balances)
        }
    }
}
