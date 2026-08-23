package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/** 网络节点稳定标识。 / Stable network node identifier. */
@JvmInline
value class NetworkNodeId(val value: String)

/**
 * 不依赖全局索引的网络节点。 / Network node independent of global indices.
 *
 * @property id 稳定标识 / Stable identifier
 * @property attributes 通用节点属性 / Generic node attributes
 */
class NetworkNode<V : RealNumber<V>> private constructor(
    val id: NetworkNodeId,
    attributes: Map<String, Quantity<V>>
) {
    val attributes: Map<String, Quantity<V>> = attributes.toMap()

    companion object {
        /**
         * 创建网络节点。 / Create a network node.
         *
         * @param id 稳定标识 / Stable identifier
         * @param attributes 通用节点属性 / Generic node attributes
         * @return 节点或校验失败 / Node or validation failure
         */
        operator fun <V : RealNumber<V>> invoke(
            id: NetworkNodeId,
            attributes: Map<String, Quantity<V>> = emptyMap()
        ): Ret<NetworkNode<V>> {
            if (id.value.isBlank()) {
                return networkSchedulingFailure(
                    "创建网络节点失败：节点 ID 不能为空 / Failed to create network node: node ID cannot be blank"
                )
            }
            return ok(NetworkNode(id, attributes))
        }
    }
}
