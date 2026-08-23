package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.ServiceTimeWindow

/** 客户稳定标识。 / Stable customer identifier. */
@JvmInline
value class CustomerId(val value: String)

/**
 * VRPTW 客户。 / VRPTW customer.
 *
 * @property id 客户稳定标识 / Stable customer identifier
 * @property node 网络节点 / Network node
 * @property demand 需求 / Demand
 * @property timeWindow 服务时间窗 / Service time window
 * @property serviceTime 服务时长 / Service duration
 */
class Customer<V : RealNumber<V>> private constructor(
    val id: CustomerId,
    val node: NetworkNode<V>,
    val demand: Quantity<V>,
    val timeWindow: ServiceTimeWindow,
    val serviceTime: Duration
) {
    companion object {
        /**
         * 创建客户。 / Create a customer.
         *
         * @param id 客户稳定标识 / Stable customer identifier
         * @param node 网络节点 / Network node
         * @param demand 非负需求 / Non-negative demand
         * @param timeWindow 服务时间窗 / Service time window
         * @param serviceTime 非负服务时长 / Non-negative service duration
         * @return 客户或校验失败 / Customer or validation failure
         */
        operator fun <V : RealNumber<V>> invoke(
            id: CustomerId,
            node: NetworkNode<V>,
            demand: Quantity<V>,
            timeWindow: ServiceTimeWindow,
            serviceTime: Duration
        ): Ret<Customer<V>> {
            if (id.value.isBlank()) {
                return networkSchedulingFailure(
                    "创建客户失败：客户 ID 不能为空 / Failed to create customer: customer ID cannot be blank"
                )
            }
            if (demand.value ls demand.value.constants.zero) {
                return networkSchedulingFailure(
                    "创建客户失败：客户 ${id.value} 的需求不能为负 / " +
                            "Failed to create customer: demand of customer ${id.value} cannot be negative"
                )
            }
            if (serviceTime.isNegative()) {
                return networkSchedulingFailure(
                    "创建客户失败：客户 ${id.value} 的服务时间不能为负 / " +
                            "Failed to create customer: service time of customer ${id.value} cannot be negative"
                )
            }
            return ok(
                Customer(
                    id = id,
                    node = node,
                    demand = demand,
                    timeWindow = timeWindow,
                    serviceTime = serviceTime
                )
            )
        }
    }
}
