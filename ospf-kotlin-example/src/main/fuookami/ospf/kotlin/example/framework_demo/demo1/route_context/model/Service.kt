package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * A service that can be routed through the network, with a capacity limit and per-use cost.
 * 可以通过网络路由的服务（具有容量限制和每次使用成本）。
 *
 * @property id the unique service identifier / 唯一服务标识符
 * @property capacity the bandwidth capacity of this service / 该服务的带宽容量
 * @property cost the cost per use of this service / 该服务的每次使用成本
*/
class Service(
    val id: UInt64,
    val capacity: UInt64,
    val cost: UInt64
) : AutoIndexed(Service::class) {
    override fun toString() = "S$id"
}
