package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 可以通过网络路由的服务（具有容量限制和每次使用成本）。A service that can be routed through the network, with a capacity limit and per-use cost.
 *
 * @property id 参数。
 * @property capacity 参数。
 * @property cost 参数。
 */
class Service(
    val id: UInt64,
    val capacity: UInt64,
    val cost: UInt64
) : AutoIndexed(Service::class) {
    override fun toString() = "S$id"
}
