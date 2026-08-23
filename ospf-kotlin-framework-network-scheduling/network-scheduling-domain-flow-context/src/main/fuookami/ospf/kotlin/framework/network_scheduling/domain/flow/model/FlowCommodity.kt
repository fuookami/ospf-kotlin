package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * 网络流商品及其节点供需。 / Flow commodity and its node balances.
 *
 * @property id 商品标识 / Commodity identifier
 * @property supplyDemands 商品节点供需 / Commodity node balances
 */
class FlowCommodity<V : RealNumber<V>> private constructor(
    val id: String,
    val supplyDemands: List<SupplyDemand<V>>
) {
    companion object {
        /** 创建网络流商品。 / Create a flow commodity. */
        operator fun <V : RealNumber<V>> invoke(
            id: String,
            supplyDemands: List<SupplyDemand<V>> = emptyList()
        ): Ret<FlowCommodity<V>> {
            if (id.isBlank()) {
                return networkSchedulingFailure(
                    "创建流商品失败：商品 ID 不能为空 / Failed to create flow commodity: commodity ID cannot be blank"
                )
            }
            if (supplyDemands.groupingBy { it.nodeId }.eachCount().any { it.value > 1 }) {
                return networkSchedulingFailure(
                    "创建流商品失败：节点供需重复 / Failed to create flow commodity: duplicate node balance"
                )
            }
            return ok(FlowCommodity(id, supplyDemands.toList()))
        }
    }
}
